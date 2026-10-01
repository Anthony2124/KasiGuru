/**
 * App state: one small store, the way Room + DataStore are the single source of truth on Android.
 *
 * Learner data lives here and on the device (IndexedDB); the cloud is a sync target, never read on
 * the hot path. Screens subscribe with useApp(selector) and change learner data only through act(),
 * which applies one whole action atomically, persists it, queues any celebration it produced, and
 * tells the sync layer something changed.
 */
import { useEffect, useMemo, useRef, useState } from 'preact/hooks';
import { Corpus } from '../domain/corpus';
import { today as todayIso } from '../domain/dates';
import { Draft, initialLearner, type LearnerData, type LearnerEvent } from '../domain/learner';
import type { AnnouncementDto, Story, WordContent } from '../domain/types';
import { load, save } from './persist';

export interface Account {
  uid: string | null;
  isAnonymous: boolean;
  email: string | null;
  displayName: string | null;
  providers: string[];
  creationTime: number | null;
  emailVerified: boolean;
}

export interface Prefs {
  soundEnabled: boolean;
  /** Answer vibrations in lessons, where the browser supports them (not iOS). */
  hapticsEnabled: boolean;
  /** The day of the last finished lesson, so the streak page shows after the day's first one. */
  lastLessonDate: string;
  backupPromptDismissed: boolean;
  gameRulesSeen: string[];
  /** The one-time "About you" details were collected (Android asks before the first sign-in). */
  aboutYouDone: boolean;
  installHintDismissed: boolean;
  readAnnouncements: string[];
}

export interface BanInfo {
  isBanned: boolean;
  reason: string;
  bannedAt: number;
  appealText?: string;
  appealStatus?: string;
  appealReviewNotes?: string;
}

export interface AppState {
  booted: boolean;
  contentReady: boolean;
  words: WordContent[];
  stories: Story[];
  contentVersion: number;
  learner: LearnerData;
  learnerLoaded: boolean;
  account: Account;
  authReady: boolean;
  syncStatus: 'idle' | 'syncing' | 'synced' | 'error' | 'offline';
  lastSyncedAt: number;
  prefs: Prefs;
  celebrations: LearnerEvent[];
  announcements: AnnouncementDto[];
  ban: BanInfo | null;
  online: boolean;
}

const defaultPrefs: Prefs = {
  soundEnabled: true,
  hapticsEnabled: true,
  lastLessonDate: '',
  backupPromptDismissed: false,
  gameRulesSeen: [],
  aboutYouDone: false,
  installHintDismissed: false,
  readAnnouncements: [],
};

let state: AppState = {
  booted: false,
  contentReady: false,
  words: [],
  stories: [],
  contentVersion: 0,
  learner: initialLearner(),
  learnerLoaded: false,
  account: { uid: null, isAnonymous: true, email: null, displayName: null, providers: [], creationTime: null, emailVerified: false },
  authReady: false,
  syncStatus: 'idle',
  lastSyncedAt: 0,
  prefs: defaultPrefs,
  celebrations: [],
  announcements: [],
  ban: null,
  online: typeof navigator === 'undefined' ? true : navigator.onLine,
};

const listeners = new Set<() => void>();

export const getState = () => state;

export function setState(patch: Partial<AppState> | ((s: AppState) => Partial<AppState>)) {
  const next = typeof patch === 'function' ? patch(state) : patch;
  state = { ...state, ...next };
  listeners.forEach((l) => l());
}

export function subscribe(fn: () => void) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

/** Subscribes a component to a slice of state; re-renders only when that slice changes. */
export function useApp<T>(selector: (s: AppState) => T, equal: (a: T, b: T) => boolean = Object.is): T {
  const [value, setValue] = useState(() => selector(state));
  const sel = useRef(selector);
  sel.current = selector;
  const current = useRef(value);
  current.current = value;
  useEffect(() => {
    const check = () => {
      const next = sel.current(state);
      if (!equal(current.current, next)) {
        current.current = next;
        setValue(() => next);
      }
    };
    check();
    return subscribe(check);
  }, []);
  return value;
}

// ── Learner actions ───────────────────────────────────────────────────────────

const LEARNER_KEY = 'learner:v1';
const PREFS_KEY = 'prefs:v1';

let learnerSaveTimer: ReturnType<typeof setTimeout> | undefined;
const changeHooks = new Set<() => void>();

/** The sync layer registers here to hear about every learner change. */
export function onLearnerChanged(fn: () => void) {
  changeHooks.add(fn);
  return () => changeHooks.delete(fn);
}

function persistLearnerSoon() {
  clearTimeout(learnerSaveTimer);
  learnerSaveTimer = setTimeout(() => save(LEARNER_KEY, state.learner), 250);
}

export function persistLearnerNow() {
  clearTimeout(learnerSaveTimer);
  return save(LEARNER_KEY, state.learner);
}

/**
 * Applies one learner action. The draft is a deep copy, so a throw leaves state untouched, and every
 * event the action produced (level up, streak, badge) is queued for the celebration dialogs.
 */
export function act<T>(fn: (d: Draft) => T, opts: { celebrate?: boolean } = {}): T {
  const draft = new Draft(structuredClone(state.learner), todayIso(), { words: state.words, stories: state.stories });
  const result = fn(draft);
  setState((s) => ({
    learner: draft.d,
    celebrations: opts.celebrate === false ? s.celebrations : [...s.celebrations, ...draft.events],
  }));
  persistLearnerSoon();
  changeHooks.forEach((h) => h());
  return result;
}

/** Replaces learner data wholesale (sync merge, sign-out wipe) without firing change hooks. */
export function replaceLearner(data: LearnerData, opts: { notify?: boolean } = {}) {
  setState({ learner: data });
  persistLearnerSoon();
  if (opts.notify) changeHooks.forEach((h) => h());
}

export function dismissCelebration() {
  setState((s) => ({ celebrations: s.celebrations.slice(1) }));
}

export function setPrefs(patch: Partial<Prefs>) {
  setState((s) => ({ prefs: { ...s.prefs, ...patch } }));
  save(PREFS_KEY, state.prefs);
}

export async function loadLocal() {
  const [learner, prefs] = await Promise.all([load<LearnerData>(LEARNER_KEY), load<Prefs>(PREFS_KEY)]);
  const base = initialLearner();
  setState({
    learner: learner
      ? {
          ...base,
          ...learner,
          progress: { ...base.progress, ...learner.progress },
          achievements: { ...base.achievements, ...learner.achievements },
        }
      : base,
    prefs: { ...defaultPrefs, ...(prefs ?? {}) },
    learnerLoaded: true,
  });
}

// ── Derived data ──────────────────────────────────────────────────────────────

let corpusCache: { words: WordContent[]; states: LearnerData['wordStates']; corpus: Corpus } | null = null;

/** The dictionary joined with review state, rebuilt only when either side changes. */
export function getCorpus(): Corpus {
  const { words, learner } = state;
  if (!corpusCache || corpusCache.words !== words || corpusCache.states !== learner.wordStates) {
    corpusCache = { words, states: learner.wordStates, corpus: new Corpus(words, learner.wordStates) };
  }
  return corpusCache.corpus;
}

export function useCorpus(): Corpus {
  const words = useApp((s) => s.words);
  const states = useApp((s) => s.learner.wordStates);
  return useMemo(() => getCorpus(), [words, states]);
}

export const useProgress = () => useApp((s) => s.learner.progress);
export const useLearner = () => useApp((s) => s.learner);
