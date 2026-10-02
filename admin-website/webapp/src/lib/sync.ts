/**
 * Cross-device progress sync: ProgressSyncManager (XP policy 2), for the browser.
 *
 * On every account change: pull users/{uid}/progress/main, then every reward receipt under
 * users/{uid}/rewardReceipts, then the learning documents (achievements, gameLevels, lessonProgress,
 * wordStates); merge each with this device (domain/merge.ts, domain/learner.ts) and push the result
 * back. Receipts go first and in transactions, so a device can never overwrite stronger evidence,
 * and main progress is only written once they are in. After that, every local change is uploaded
 * five seconds after it settles. An Android learner and a web learner signed into the same account
 * converge on the same state, whichever device they used last.
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
import { act, getCorpus, getState, onLearnerChanged, persistLearnerNow, replaceLearner, setState } from './store';

export const PROGRESS_DOCS = ['main', 'achievements', 'gameLevels', 'lessonProgress', 'wordStates'] as const;
type LearningDoc = Exclude<(typeof PROGRESS_DOCS)[number], 'main'>;

const DEBOUNCE_MS = 5_000;
const PAGE = 400;

let activeUid: string | null = null;
let session = 0;
let timer: ReturnType<typeof setTimeout> | undefined;
/** Last payload uploaded per `${uid}/${doc}`, so an unchanged document is never rewritten. */
const lastUploaded = new Map<string, string>();
/** Main progress is written only after a full pull: before that it could carry another device's past. */
let readyForUpload = false;
/** Receipts already in the cloud as they are now, by `${uid}/${id}`. */
const uploadedRewards = new Map<string, string>();
/** Newest server `updatedAt` seen; the next pull asks only for receipts at or after it. */
let rewardCursor: Timestamp | null = null;
let rewardLock: Promise<unknown> = Promise.resolve();

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
  uploadedRewards.clear();
  rewardCursor = null;
  readyForUpload = false;
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

/** Merges main progress. Returns whether the cloud copy predates XP policy 2. */
async function pullMain(uid: string, mine: number): Promise<boolean> {
  const snap = await getDoc(progressRef(uid, 'main'));
  if (!stillActive(uid, mine) || !snap.exists()) return false;
  const remote = fromMainDoc(snap.data());
  act(
    (d) => {
      d.ensureNormalized();
      d.d.progress = mergeProgress(d.d.progress, remote);
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
  act(
    (d) => {
      const local = d.d;
      const tiers = Object.fromEntries(Object.entries(local.achievements).filter(([id]) => isBadgeId(id)));
      local.achievements = { ...tiers, ...mergeLegacyAchievements(local.achievements, parseAchievements(ach)) };
      local.gameLevels = mergeGameLevels(local.gameLevels, parseGameLevels(levels));
      local.lessons = mergeLessonProgress(local.lessons, parseLessons(lessons));
      local.wordStates = mergeWordStates(local.wordStates, parseWordStates(words));
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

/** Serialises reward uploads: two overlapping ones would each read the other's stale state. */
function withRewardLock<T>(fn: () => Promise<T>): Promise<T> {
  const run = rewardLock.then(fn, fn);
  rewardLock = run.catch(() => undefined);
  return run;
}

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

function learningPayload(d: LearnerData, name: LearningDoc): Record<string, unknown> | null {
  let entries: Record<string, unknown>;
  switch (name) {
    case 'achievements':
      entries = d.achievements;
      break;
    case 'gameLevels':
      entries = d.gameLevels;
      break;
    case 'lessonProgress':
      entries = d.lessons;
      break;
    case 'wordStates':
      // Only words actually touched, as on Android: the document stays small.
      entries = Object.fromEntries(Object.entries(d.wordStates).filter(([, s]) => s.isLearned || s.timesReviewed > 0));
      break;
  }
  if (!Object.keys(entries).length) return null;
  return { entries };
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
  for (const name of ['achievements', 'gameLevels', 'lessonProgress', 'wordStates'] as LearningDoc[]) {
    const payload = learningPayload(d, name);
    if (!payload) continue;
    const fp = JSON.stringify(payload.entries);
    await uploadIfChanged(uid, name, { ...payload, updatedAt: Date.now() }, fp).catch((e) => {
      console.warn(`${name} upload failed`, e);
      failure = e;
    });
    if (!stillActive(uid, mine)) return;
  }
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
    if (!readyForUpload) await syncNow(uid, mine);
    else await uploadAll(uid, mine);
    if (mine === session) setState({ syncStatus: 'synced', lastSyncedAt: Date.now() });
    return mine === session;
  } catch {
    if (mine === session) setState({ syncStatus: 'error' });
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
    const saved = await flush();
    if (requireSaved && !saved) throw new Error('Connect to the internet before signing out so your progress can be saved.');
  }
  endSession();
  replaceLearner(initialLearner());
  await persistLearnerNow();
  await save('leaderboard-week', null);
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
  const hide = () => {
    if (document.visibilityState === 'hidden') void flush();
  };
  document.addEventListener('visibilitychange', hide);
  window.addEventListener('pagehide', () => void flush());
  window.addEventListener('online', () => {
    setState({ online: true });
    schedule();
  });
  window.addEventListener('offline', () => setState({ online: false, syncStatus: 'offline' }));
}
