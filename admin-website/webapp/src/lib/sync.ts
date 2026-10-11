/**
 * Cross-device progress sync: ProgressSyncManager (XP policy 2), for the browser.
 *
 * On every account change: pull users/{uid}/progress/main, then every reward receipt under
 * users/{uid}/rewardReceipts, then the learning documents (achievements, gameLevels, lessonProgress,
 * wordStates); merge each with this device (domain/merge.ts, domain/learner.ts) and push the result
 * back. Receipts go first and in transactions, so a device can never overwrite stronger evidence,
 * and main progress is only written once they are in. After that, every local change is uploaded
 * five seconds after it settles, and coming back to the app downloads again (refresh()), at most
 * once a minute. The learning documents are written entry by entry, merged, never replaced whole.
 * An Android learner and a web learner signed into the same account converge on the same state,
 * whichever device they used last.
 */
import {
  collection,
  deleteDoc,
  doc,
  documentId,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  startAfter,
  Timestamp,
  where,
  writeBatch,
  type QueryDocumentSnapshot,
  type QuerySnapshot,
} from 'firebase/firestore/lite';
import { publicDisplayName } from '../domain/publicProfile';
import { isoWeekId } from '../domain/dates';
import { earnedBadgeIds, initialLearner, treeAccess, weeklyXp, type LearnerData } from '../domain/learner';
import { buildTree, Mastery } from '../domain/lesson';
import { isBadgeId, normalizeBackground } from '../domain/badges';
import {
  changedEntries,
  hasEntry,
  fromMainDoc,
  mergeGameLevels,
  mergeLegacyAchievements,
  mergeLessonProgress,
  mergeProgress,
  mergeWordStates,
  parseAchievements,
  parseGameLevels,
  parseLessons,
  parseWordStates,
  toMainDoc,
} from '../domain/merge';
import { decodeReceipt, encodeReceipt, mergeRecord, receiptDocumentId, RECEIPTS_COLLECTION, XP_POLICY_VERSION, type RewardRecord } from '../domain/xp';
import { auth, db } from './firebase';
import { load, save } from './persist';
import { clearAllSavedGames } from './savedGames';
import { act, getCorpus, getState, onLearnerChanged, persistLearnerNow, replaceLearner, setState } from './store';

export const PROGRESS_DOCS = ['main', 'achievements', 'gameLevels', 'lessonProgress', 'wordStates'] as const;
type LearningDoc = Exclude<(typeof PROGRESS_DOCS)[number], 'main'>;
const LEARNING_DOCS: LearningDoc[] = ['achievements', 'gameLevels', 'lessonProgress', 'wordStates'];

const DEBOUNCE_MS = 5_000;
const PAGE = 400;
/** The shortest wait between two downloads when the app comes back (ProgressSyncManager.REFRESH_INTERVAL_MS). */
const REFRESH_MS = 60_000;

const SIGN_OUT_SAVE_TIMEOUT_MS = 20_000;

let activeUid: string | null = null;
let session = 0;
let timer: ReturnType<typeof setTimeout> | undefined;
/** Last main progress uploaded per uid, so an unchanged document is never rewritten. */
const lastUploaded = new Map<string, string>();
/**
 * What each learning document holds as far as this browser knows, by `${uid}/${doc}`: the entries it
 * last read there plus those it has written since. An upload sends only entries that differ.
 */
const cloudLearning = new Map<string, Record<string, unknown>>();
/** When this session last started downloading the account, for refresh()'s throttle. */
let lastPulledAt = 0;
/** Main progress is written only after a full pull: before that it could carry another device's past. */
let readyForUpload = false;
/** Receipts already in the cloud as they are now, by `${uid}/${id}`. */
const uploadedRewards = new Map<string, string>();
/** Newest server `updatedAt` seen; the next pull asks only for receipts at or after it. */
let rewardCursor: Timestamp | null = null;

const progressRef = (uid: string, name: string) => doc(db, 'users', uid, 'progress', name);
const rewardCollection = (uid: string) => collection(db, 'users', uid, RECEIPTS_COLLECTION);

/** True while `uid` is still the account this device is syncing. */
const stillActive = (uid: string, mine: number) => mine === session && activeUid === uid && auth.currentUser?.uid === uid;

/** Called by the auth layer whenever the signed-in uid changes (including to a new guest). */
export async function startSession(uid: string) {
  if (uid === activeUid) return;
  clearTimeout(timer);
  activeUid = uid;
  lastUploaded.clear();
  cloudLearning.clear();
  uploadedRewards.clear();
  rewardCursor = null;
  readyForUpload = false;
  lastPulledAt = Date.now();
  const mine = ++session;
  setState({ syncStatus: 'syncing' });
  try {
    await syncNow(uid, mine);
    if (mine === session) setState({ syncStatus: 'synced', lastSyncedAt: Date.now() });
  } catch (e) {
    console.warn('progress sync failed', e);
    if (mine === session) setState({ syncStatus: navigator.onLine ? 'error' : 'offline' });
  }
}

export function endSession() {
  clearTimeout(timer);
  activeUid = null;
  session++;
  lastUploaded.clear();
  cloudLearning.clear();
  uploadedRewards.clear();
  rewardCursor = null;
  readyForUpload = false;
}

/** Pull everything, then push. Throws if any read failed, so nothing is uploaded half-merged. */
async function syncNow(uid: string, mine: number) {
  const legacyCloud = await pullMain(uid, mine);
  if (!stillActive(uid, mine)) return;
  await pullRewards(uid, mine);
  if (!stillActive(uid, mine)) return;
  await pullLearning(uid, mine);
  if (!stillActive(uid, mine)) return;
  if (legacyCloud) act((d) => d.importLegacyLearningState(), { celebrate: false });
  readyForUpload = true;
  await uploadAll(uid, mine);
}

/** Set by an account switch: the next pull takes the account's progress as-is. */
let adoptCloud = false;

/**
 * Called after a sign-in to an existing account has wiped the guest. The blank local row's
 * defaults (the placeholder name, avatar 0) must not be weighed against the account's real values.
 */
export function adoptCloudOnNextSync() {
  adoptCloud = true;
}

/** Merges main progress. Returns whether the cloud copy predates XP policy 2. */
async function pullMain(uid: string, mine: number): Promise<boolean> {
  const snap = await getDoc(progressRef(uid, 'main'));
  if (!stillActive(uid, mine)) return false;
  const adopt = adoptCloud;
  adoptCloud = false;
  if (!snap.exists()) return false;
  const remote = fromMainDoc(snap.data());
  act(
    (d) => {
      d.ensureNormalized();
      d.d.progress = adopt ? { ...remote } : mergeProgress(d.d.progress, remote);
      withAccountIdentity(d.d);
      if (remote.xpPolicyVersion < XP_POLICY_VERSION) d.archiveLegacy(remote);
    },
    { celebrate: false }
  );
  return remote.xpPolicyVersion < XP_POLICY_VERSION;
}

/**
 * Reads receipts: the first scan in document-id order, later ones by server `updatedAt` from the
 * cursor (inclusive). A receipt whose document id is not the hash of its own id is ignored.
 */
async function pullRewards(uid: string, mine: number) {
  const cursor = rewardCursor;
  let newest = cursor;
  let last: QueryDocumentSnapshot | null = null;
  for (;;) {
    const base = cursor
      ? query(rewardCollection(uid), where('updatedAt', '>=', cursor), orderBy('updatedAt'), orderBy(documentId()), limit(PAGE))
      : query(rewardCollection(uid), orderBy(documentId()), limit(PAGE));
    const page: QuerySnapshot = await getDocs(last ? query(base, startAfter(last)) : base);
    if (!stillActive(uid, mine)) return;
    const rows: RewardRecord[] = [];
    for (const snap of page.docs) {
      const row = decodeReceipt(snap.data());
      if (row && (await receiptDocumentId(row.id)) === snap.id) rows.push(row);
      const at = snap.get('updatedAt');
      if (at instanceof Timestamp && (!newest || at.toMillis() > newest.toMillis())) newest = at;
    }
    // The cursor is inclusive, so the newest receipts come back every time. Only evidence that
    // changes something is applied; anything else would schedule another sync, forever.
    const local = getState().learner.receipts;
    const fresh = rows.filter((r) => {
      const mine = local[r.id];
      return !mine || JSON.stringify(mergeRecord(mine, r)) !== JSON.stringify(mine);
    });
    if (fresh.length) act((d) => d.mergeRewards(fresh), { celebrate: false });
    for (const row of rows) uploadedRewards.set(`${uid}/${row.id}`, JSON.stringify(row));
    last = page.docs[page.docs.length - 1] ?? null;
    if (page.size < PAGE) break;
  }
  // A first scan is ordered by document id, so an insert landing behind it could be missed; the
  // next scan starts from the epoch by timestamp to catch it.
  rewardCursor = cursor ? newest : new Timestamp(0, 0);
}

async function readEntries(uid: string, name: string): Promise<Record<string, Record<string, unknown>>> {
  const snap = await getDoc(progressRef(uid, name));
  if (!snap.exists()) return {};
  const entries = snap.data().entries;
  return entries && typeof entries === 'object' ? (entries as Record<string, Record<string, unknown>>) : {};
}

async function pullLearning(uid: string, mine: number) {
  const [ach, levels, lessons, words] = await Promise.all([
    readEntries(uid, 'achievements'),
    readEntries(uid, 'gameLevels'),
    readEntries(uid, 'lessonProgress'),
    readEntries(uid, 'wordStates'),
  ]);
  if (!stillActive(uid, mine)) return;
  const remote = {
    achievements: parseAchievements(ach),
    gameLevels: parseGameLevels(levels),
    lessonProgress: parseLessons(lessons),
    wordStates: parseWordStates(words),
  } satisfies Record<LearningDoc, unknown>;
  // What the documents hold now: the upload that follows writes back only what differs.
  for (const name of LEARNING_DOCS) cloudLearning.set(`${uid}/${name}`, remote[name]);
  act(
    (d) => {
      const local = d.d;
      const tiers = Object.fromEntries(Object.entries(local.achievements).filter(([id]) => isBadgeId(id)));
      local.achievements = { ...tiers, ...mergeLegacyAchievements(local.achievements, remote.achievements) };
      local.gameLevels = mergeGameLevels(local.gameLevels, remote.gameLevels);
      local.lessons = mergeLessonProgress(local.lessons, remote.lessonProgress);
      local.wordStates = mergeWordStates(local.wordStates, remote.wordStates);
      // Restored lessons and levels can complete badge criteria; project them without celebrating.
      d.mergeRewards([]);
    },
    { celebrate: false }
  );
}

/** Fills a blank email or name from the signed-in account. Never overwrites what is there. */
function withAccountIdentity(d: LearnerData) {
  const { account } = getState();
  if (!d.progress.email && account.email) d.progress.email = account.email;
  if (!d.progress.fullName && account.displayName) d.progress.fullName = account.displayName;
}

/** Runs calls one after another: two overlapping uploads would each work from the other's stale state. */
function serial() {
  let tail: Promise<unknown> = Promise.resolve();
  return <T>(fn: () => Promise<T>): Promise<T> => {
    const run = tail.then(fn, fn);
    tail = run.catch(() => undefined);
    return run;
  };
}

const withRewardLock = serial();
/** One learning upload at a time, so two cannot send the same entry out of order. */
const withLearningLock = serial();

/**
 * Writes every changed receipt in transactions that read the cloud copy first and keep the stronger
 * evidence, so concurrent devices cannot erase each other's. Returns false if anything failed.
 */
function uploadRewards(uid: string, mine: number): Promise<boolean> {
  return withRewardLock(async () => {
    if (!readyForUpload || !stillActive(uid, mine)) return false;
    try {
      await pullRewards(uid, mine);
      if (!stillActive(uid, mine)) return false;
      const changed = Object.values(getState().learner.receipts).filter(
        (r) => uploadedRewards.get(`${uid}/${r.id}`) !== JSON.stringify(r)
      );
      for (let i = 0; i < changed.length; i += 200) {
        const chunk = await Promise.all(
          changed.slice(i, i + 200).map(async (row) => ({ row, ref: doc(rewardCollection(uid), await receiptDocumentId(row.id)) }))
        );
        const merged = await runTransaction(db, async (tx) => {
          const results: { ref: (typeof chunk)[number]['ref']; row: RewardRecord }[] = [];
          for (const { row, ref } of chunk) {
            const snap = await tx.get(ref);
            const old = snap.exists() ? decodeReceipt(snap.data()) : null;
            results.push({ ref, row: old && old.id === row.id && old.kind === row.kind && old.source === row.source ? mergeRecord(row, old) : row });
          }
          for (const { ref, row } of results) tx.set(ref, { ...encodeReceipt(row), updatedAt: serverTimestamp() });
          return results.map((r) => r.row);
        });
        if (!stillActive(uid, mine)) return false;
        act((d) => d.mergeRewards(merged), { celebrate: false });
        for (const row of merged) uploadedRewards.set(`${uid}/${row.id}`, JSON.stringify(row));
      }
      return true;
    } catch (e) {
      console.warn('reward upload failed', e);
      return false;
    }
  });
}

async function uploadIfChanged(uid: string, name: string, payload: Record<string, unknown>, fingerprint: string) {
  const key = `${uid}/${name}`;
  if (lastUploaded.get(key) === fingerprint) return false;
  await setDoc(progressRef(uid, name), payload);
  lastUploaded.set(key, fingerprint);
  return true;
}

function learningEntries(d: LearnerData, name: LearningDoc, inCloud: Record<string, unknown>): Record<string, unknown> {
  switch (name) {
    case 'achievements':
      return d.achievements;
    case 'gameLevels':
      return d.gameLevels;
    case 'lessonProgress':
      return d.lessons;
    case 'wordStates':
      // Only words actually touched, as on Android: the document stays small. A word the document
      // already holds stays in even once unmarked as learned, so the reset is written over it.
      return Object.fromEntries(
        Object.entries(d.wordStates).filter(([key, s]) => s.isLearned || s.timesReviewed > 0 || hasEntry(inCloud, key))
      );
  }
}

/**
 * Writes each learning document's changed entries, merged into it. Entries this browser has not
 * changed are left alone, so one that has not downloaded lately cannot erase what a phone added;
 * older Android versions still replace the whole document, and the next download here writes back
 * whatever they dropped. Returns the last failure, if any; one document failing does not stop the rest.
 */
function uploadLearning(uid: string, mine: number): Promise<unknown> {
  return withLearningLock(async () => {
    let failure: unknown = null;
    for (const name of LEARNING_DOCS) {
      if (!stillActive(uid, mine)) return failure;
      const key = `${uid}/${name}`;
      const known = cloudLearning.get(key);
      const changed = changedEntries(learningEntries(getState().learner, name, known ?? {}), known);
      if (!Object.keys(changed).length) continue;
      try {
        await setDoc(progressRef(uid, name), { entries: changed, updatedAt: Date.now() }, { merge: true });
        cloudLearning.set(key, { ...cloudLearning.get(key), ...changed });
      } catch (e) {
        console.warn(`${name} upload failed`, e);
        failure = e;
      }
    }
    return failure;
  });
}

async function uploadAll(uid: string, mine: number) {
  if (!readyForUpload) return;
  let failure: unknown = null;
  // Receipts first: main progress is a projection of them, and the rules expect them to exist.
  const rewardsSaved = await uploadRewards(uid, mine);
  if (!stillActive(uid, mine)) return;
  const d = getState().learner;
  let mainChanged = false;
  if (rewardsSaved) {
    const fingerprint = JSON.stringify({ ...d.progress, updatedAt: 0 });
    mainChanged = await uploadIfChanged(uid, 'main', toMainDoc(d.progress, Date.now()), fingerprint).catch((e) => {
      // A rejected write means firestore.rules refused the document. Nothing retries it into
      // existence, so say so rather than failing silently.
      console.warn('main progress upload rejected', e);
      failure = e;
      return false;
    });
  } else {
    failure = new Error('Your rewards could not be saved.');
  }
  if (!stillActive(uid, mine)) return;
  // The learning documents are independent of main; one being refused must not strand the others.
  const learningFailure = await uploadLearning(uid, mine);
  if (learningFailure) failure = learningFailure;
  if (!stillActive(uid, mine)) return;
  if (mainChanged) await publishPublicRows(uid, d);
  if (failure) throw failure;
}

// ── Leaderboard row and public profile ───────────────────────────────────────

interface WeekBaseline {
  uid: string;
  weekStartDate: string;
  weekStartXp: number;
}

/**
 * Publishes leaderboard_public/{uid} and public_profiles/{uid}: allowlisted projections, never the
 * private progress document. Guests are not ranked and have no public profile (the rules refuse
 * both). Weekly XP is this week's dated activity receipts; badge bonuses never count.
 */
async function publishPublicRows(uid: string, d: LearnerData) {
  const { account } = getState();
  if (account.isAnonymous || auth.currentUser?.uid !== uid || auth.currentUser.isAnonymous) return;
  const ref = doc(db, 'leaderboard_public', uid);
  const week = isoWeekId();
  let baseline = await load<WeekBaseline>('leaderboard-week');
  if (!baseline || baseline.uid !== uid || baseline.weekStartDate !== week) {
    const existing = await getDoc(ref).catch(() => null);
    const data = existing?.exists() ? existing.data() : null;
    baseline =
      data && data.weekStartDate === week && typeof data.weekStartXp === 'number'
        ? { uid, weekStartDate: week, weekStartXp: data.weekStartXp }
        : { uid, weekStartDate: week, weekStartXp: d.progress.totalXp };
    await save('leaderboard-week', baseline);
  }
  const p = d.progress;
  const created = account.creationTime ?? Date.now();
  const displayName = publicDisplayName(p.userName, p.fullName);
  // The rules require weeklyXp <= activityXp under policy 2.
  const thisWeek = Math.min(weeklyXp(d), p.activityXp);
  await setDoc(ref, {
    displayName,
    isAnonymous: false,
    createdAt: created,
    registeredAt: created,
    totalXp: p.totalXp,
    level: p.level,
    currentStreak: p.currentStreak,
    profileIconId: p.profileIconId,
    titleBadge: p.titleBadge,
    weeklyXp: thisWeek,
    weekStartXp: baseline.weekStartXp,
    weekStartDate: baseline.weekStartDate,
    xpPolicyVersion: p.xpPolicyVersion,
    activityXp: p.activityXp,
    updatedAt: Date.now(),
  }).catch((e) => console.warn('leaderboard publish failed', e));

  try {
    const tree = buildTree(getCorpus(), d.lessons, treeAccess(d));
    const sections: Record<string, number> = {};
    const sectionTotals: Record<string, number> = {};
    for (const s of tree) {
      const core = s.nodes.filter((n) => n.node.kind === 'lesson' && !n.isDeepDive);
      sections[s.definition.id] = core.filter((n) => n.mastery >= Mastery.FAMILIAR).length;
      sectionTotals[s.definition.id] = core.length;
    }
    await setDoc(doc(db, 'public_profiles', uid), {
      displayName,
      profileIconId: Math.min(1000, Math.max(0, p.profileIconId)),
      profileBackgroundId: normalizeBackground(p.profileBackgroundId),
      level: Math.min(30, Math.max(1, p.level)),
      totalXp: p.totalXp,
      currentStreak: p.currentStreak,
      wordsLearned: Object.values(d.wordStates).filter((w) => w.isLearned).length,
      lessonsCompleted: Object.entries(d.lessons).filter(([k, s]) => s.isComplete && !k.startsWith('mastery:')).length,
      weeklyXp: thisWeek,
      weekId: week,
      createdAt: created,
      updatedAt: Date.now(),
      badgeIds: earnedBadgeIds(d),
      sections,
      sectionTotals,
      masteredSections: tree
        .filter((s) => s.nodes.some((n) => n.node.kind === 'mastery' && n.mastery >= Mastery.FAMILIAR))
        .map((s) => s.definition.id),
      unlockedSections: tree.filter((s) => s.isUnlocked).map((s) => s.definition.id),
    });
  } catch (e) {
    console.warn('public profile publish failed', e);
  }
}

// ── Scheduling ────────────────────────────────────────────────────────────────

function schedule() {
  if (!activeUid) return;
  clearTimeout(timer);
  timer = setTimeout(() => {
    void flush();
  }, DEBOUNCE_MS);
}

/** Uploads now (after completing the first pull if it never finished). Used before sign-out too. */
export async function flush(): Promise<boolean> {
  clearTimeout(timer);
  const uid = activeUid;
  if (!uid || !navigator.onLine) return false;
  const mine = session;
  try {
    setState({ syncStatus: 'syncing' });
    if (!readyForUpload) {
      lastPulledAt = Date.now();
      await syncNow(uid, mine);
    } else await uploadAll(uid, mine);
    if (mine === session) setState({ syncStatus: 'synced', lastSyncedAt: Date.now() });
    return mine === session;
  } catch {
    if (mine === session) setState({ syncStatus: 'error' });
    return false;
  }
}

/**
 * Downloads what the Android app or another browser has saved since this one last looked, merges
 * it, and uploads the result (ProgressSyncManager.refreshFromCloud).
 *
 * A session downloads only when it starts, and an installed iPhone app or a background tab comes
 * back without reloading, so it went on showing the XP, lessons and words it had before the learner
 * played on their phone. Runs when the app comes back, at most every REFRESH_MS, since a refresh
 * reads about eight documents from the project's shared daily quota. `now` (the Sync button)
 * skips the wait.
 */
export async function refresh(now = false): Promise<boolean> {
  const uid = activeUid;
  if (!uid || !navigator.onLine) return false;
  if (!now && Date.now() - lastPulledAt < REFRESH_MS) return false;
  clearTimeout(timer);
  lastPulledAt = Date.now();
  const mine = session;
  try {
    setState({ syncStatus: 'syncing' });
    await syncNow(uid, mine);
    if (mine === session) setState({ syncStatus: 'synced', lastSyncedAt: Date.now() });
    return mine === session;
  } catch (e) {
    console.warn('progress refresh failed', e);
    if (mine === session) setState({ syncStatus: navigator.onLine ? 'error' : 'offline' });
    return false;
  }
}

/** Immediately after a guest links an account: the uid is unchanged, so publish the new identity. */
export async function onAccountLinked() {
  const d = structuredClone(getState().learner);
  withAccountIdentity(d);
  replaceLearner(d);
  lastUploaded.clear();
  await flush();
}

/**
 * Signing out keeps nothing on the device, so rewards must reach the cloud first. With
 * [requireSaved], an upload that fails stops the sign-out instead of losing them.
 */
export async function wipeLocal(uploadFirst: boolean, requireSaved = false) {
  if (uploadFirst) {
    // Bounded: a Firestore write with no network can wait indefinitely, which left sign-out
    // spinning with no way out.
    const timedOut = new Promise<boolean>((resolve) => setTimeout(() => resolve(false), SIGN_OUT_SAVE_TIMEOUT_MS));
    const saved = await Promise.race([flush(), timedOut]);
    if (requireSaved && !saved) throw new Error('Your progress could not be saved. Check your connection and try again.');
  }
  endSession();
  replaceLearner(initialLearner());
  await persistLearnerNow();
  await save('leaderboard-week', null);
  // Games left partway are this learner's: the next one on this device starts their own.
  await clearAllSavedGames();
}

export async function deleteCloudData(uid: string) {
  for (const name of PROGRESS_DOCS) await deleteDoc(progressRef(uid, name));
  // Receipts in bounded batches, as AuthRepository.deleteAccount.
  for (;;) {
    const page = await getDocs(query(rewardCollection(uid), limit(PAGE)));
    if (page.empty) break;
    const batch = writeBatch(db);
    page.docs.forEach((s) => batch.delete(s.ref));
    await batch.commit();
  }
  await deleteDoc(doc(db, 'public_profiles', uid));
  await deleteDoc(doc(db, 'leaderboard_public', uid));
  await deleteDoc(doc(db, 'device_tokens', uid)).catch(() => undefined);
}

export function initSync() {
  onLearnerChanged(schedule);
  // Leaving saves; coming back downloads what other devices saved meanwhile.
  const visibility = () => void (document.visibilityState === 'hidden' ? flush() : refresh());
  document.addEventListener('visibilitychange', visibility);
  window.addEventListener('pagehide', () => void flush());
  // A desktop window brought to the front was visible all along, behind another.
  window.addEventListener('focus', () => void refresh());
  window.addEventListener('online', () => {
    setState({ online: true });
    schedule();
    void refresh();
  });
  window.addEventListener('offline', () => setState({ online: false, syncStatus: 'offline' }));
}
