/**
 * Cross-device progress sync: ProgressSyncManager, for the browser.
 *
 * On every account change, pull users/{uid}/progress/{main,achievements,gameLevels,lessonProgress,
 * wordStates}, merge each with what this device holds (additively - see domain/merge.ts), keep the
 * result, and push it back. After that, every local change is uploaded five seconds after it settles.
 * An Android learner and a web learner signed into the same account therefore converge on the same
 * state, whichever device they used last.
 */
import { deleteDoc, doc, getDoc, setDoc } from 'firebase/firestore/lite';
import { isoWeekId } from '../domain/dates';
import { initialLearner, type LearnerData } from '../domain/learner';
import {
  fromMainDoc,
  mergeAchievements,
  mergeGameLevels,
  mergeLessonProgress,
  mergeProgress,
  mergeWordStates,
  parseAchievements,
  parseGameLevels,
  parseLessons,
  parseWordStates,
  toMainDoc,
} from '../domain/merge';
import { db } from './firebase';
import { load, save } from './persist';
import { getState, onLearnerChanged, persistLearnerNow, replaceLearner, setState } from './store';

export const PROGRESS_DOCS = ['main', 'achievements', 'gameLevels', 'lessonProgress', 'wordStates'] as const;
type LearningDoc = Exclude<(typeof PROGRESS_DOCS)[number], 'main'>;

const DEBOUNCE_MS = 5_000;

let activeUid: string | null = null;
let session = 0;
let timer: ReturnType<typeof setTimeout> | undefined;
/** Last payload uploaded per `${uid}/${doc}`, so an unchanged document is never rewritten. */
const lastUploaded = new Map<string, string>();

const progressRef = (uid: string, name: string) => doc(db, 'users', uid, 'progress', name);

/** Called by the auth layer whenever the signed-in uid changes (including to a new guest). */
export async function startSession(uid: string) {
  if (uid === activeUid) return;
  clearTimeout(timer);
  activeUid = uid;
  lastUploaded.clear();
  const mine = ++session;
  setState({ syncStatus: 'syncing' });
  try {
    await pullAndMerge(uid, mine);
    if (mine !== session) return;
    await uploadAll(uid, mine);
    setState({ syncStatus: 'synced', lastSyncedAt: Date.now() });
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
}

async function readEntries(uid: string, name: string): Promise<Record<string, Record<string, unknown>> | null> {
  const snap = await getDoc(progressRef(uid, name));
  if (!snap.exists()) return {};
  const entries = snap.data().entries;
  return entries && typeof entries === 'object' ? (entries as Record<string, Record<string, unknown>>) : {};
}

async function pullAndMerge(uid: string, mine: number) {
  const [mainSnap, ach, levels, lessons, words] = await Promise.all([
    getDoc(progressRef(uid, 'main')),
    readEntries(uid, 'achievements'),
    readEntries(uid, 'gameLevels'),
    readEntries(uid, 'lessonProgress'),
    readEntries(uid, 'wordStates'),
  ]);
  if (mine !== session) return;

  const local = getState().learner;
  const merged: LearnerData = { ...local };
  if (mainSnap.exists()) merged.progress = mergeProgress(local.progress, fromMainDoc(mainSnap.data()));
  if (ach) merged.achievements = mergeAchievements(local.achievements, parseAchievements(ach));
  if (levels) merged.gameLevels = mergeGameLevels(local.gameLevels, parseGameLevels(levels));
  if (lessons) merged.lessons = mergeLessonProgress(local.lessons, parseLessons(lessons));
  if (words) merged.wordStates = mergeWordStates(local.wordStates, parseWordStates(words));
  withAccountIdentity(merged);
  replaceLearner(merged);
}

/** Fills a blank email or name from the signed-in account. Never overwrites what is there. */
function withAccountIdentity(d: LearnerData) {
  const { account } = getState();
  if (!d.progress.email && account.email) d.progress.email = account.email;
  if (!d.progress.fullName && account.displayName) d.progress.fullName = account.displayName;
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
  const d = getState().learner;
  let failure: unknown = null;
  const fingerprint = JSON.stringify({ ...d.progress, updatedAt: 0 });
  const mainChanged = await uploadIfChanged(uid, 'main', toMainDoc(d.progress, Date.now()), fingerprint).catch((e) => {
    // A rejected write here means firestore.rules refused the document - most often the per-write
    // XP cap. Nothing retries it into existence, so say so rather than failing silently.
    console.warn('main progress upload rejected', e);
    failure = e;
    return false;
  });
  if (mine !== session) return;
  // The learning documents are independent of main; one being refused must not strand the others.
  for (const name of ['achievements', 'gameLevels', 'lessonProgress', 'wordStates'] as LearningDoc[]) {
    const payload = learningPayload(d, name);
    if (!payload) continue;
    const fp = JSON.stringify(payload.entries);
    await uploadIfChanged(uid, name, { ...payload, updatedAt: Date.now() }, fp).catch((e) => {
      console.warn(`${name} upload failed`, e);
      failure = e;
    });
    if (mine !== session) return;
  }
  if (mainChanged) await publishLeaderboard(uid, d);
  if (failure) throw failure;
}

// ── Leaderboard row ───────────────────────────────────────────────────────────

interface WeekBaseline {
  uid: string;
  weekStartDate: string;
  weekStartXp: number;
}

/**
 * Publishes leaderboard_public/{uid}. Guests are not ranked (the rules refuse them), and a guest's
 * leftover row is withdrawn. The weekly baseline is read once a week and then remembered, rather
 * than read on every publish.
 */
async function publishLeaderboard(uid: string, d: LearnerData) {
  const { account } = getState();
  const ref = doc(db, 'leaderboard_public', uid);
  if (account.isAnonymous) {
    return;
  }
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
  // No email, and never an email as the name: the row is public. Same rule as Android's
  // PublicProfileDto.displayName, and firestore.rules refuses an `email` key.
  const name = p.userName.trim().slice(0, 40);
  await setDoc(ref, {
    displayName: name && !name.includes('@') ? name : 'Learner',
    isAnonymous: false,
    createdAt: created,
    registeredAt: created,
    totalXp: p.totalXp,
    level: p.level,
    currentStreak: p.currentStreak,
    profileIconId: p.profileIconId,
    titleBadge: p.titleBadge,
    weeklyXp: Math.max(0, p.totalXp - baseline.weekStartXp),
    weekStartXp: baseline.weekStartXp,
    weekStartDate: baseline.weekStartDate,
    updatedAt: Date.now(),
  }).catch((e) => console.warn('leaderboard publish failed', e));
}

// ── Scheduling ────────────────────────────────────────────────────────────────

function schedule() {
  if (!activeUid) return;
  clearTimeout(timer);
  timer = setTimeout(() => {
    void flush();
  }, DEBOUNCE_MS);
}

/** Uploads now. Used before sign-out and when the page is being hidden. */
export async function flush() {
  clearTimeout(timer);
  const uid = activeUid;
  if (!uid || !navigator.onLine) return;
  const mine = session;
  try {
    setState({ syncStatus: 'syncing' });
    await uploadAll(uid, mine);
    if (mine === session) setState({ syncStatus: 'synced', lastSyncedAt: Date.now() });
  } catch {
    if (mine === session) setState({ syncStatus: 'error' });
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

/** Signing out keeps nothing on the device: push first, then reset to a clean guest. */
export async function wipeLocal(uploadFirst: boolean) {
  if (uploadFirst) await flush();
  endSession();
  replaceLearner(initialLearner());
  await persistLearnerNow();
  await save('leaderboard-week', null);
}

export async function deleteCloudData(uid: string) {
  for (const name of PROGRESS_DOCS) await deleteDoc(progressRef(uid, name));
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
