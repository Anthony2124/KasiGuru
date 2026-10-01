/**
 * Cross-device merge rules and the Firestore payload shapes.
 *
 * Port of LearningStateMerge.kt and the mergeProgress / toMap / toEntity functions in
 * ProgressSyncManager.kt. These run against the same users/{uid}/progress documents the Android app
 * writes, so they must agree with it exactly: every rule is additive, and main progress uses the
 * date-aware streak merge. firestore.rules' isValidMainProgress() rejects a document carrying any key
 * outside MAIN_PROGRESS_KEYS, so toMap() must never grow a field the rules do not list.
 */
import { daysBetween, today as todayIso } from './dates';
import type { AchievementState, GameLevelState, LessonState, UserProgress, WordState } from './types';

type Map_<T> = Record<string, T>;

function mergeKeys<T>(local: Map_<T>, remote: Map_<T>, both: (l: T, r: T) => T): Map_<T> {
  const out: Map_<T> = {};
  for (const k of new Set([...Object.keys(local), ...Object.keys(remote)])) {
    const l = local[k];
    const r = remote[k];
    out[k] = l === undefined ? r : r === undefined ? l : both(l, r);
  }
  return out;
}

export const mergeAchievements = (l: Map_<AchievementState>, r: Map_<AchievementState>) =>
  mergeKeys(l, r, (a, b) => {
    const dates = [a.unlockedDate, b.unlockedDate].filter((d): d is string => !!d).sort();
    return {
      isUnlocked: a.isUnlocked || b.isUnlocked,
      currentValue: Math.max(a.currentValue, b.currentValue),
      unlockedDate: dates[0] ?? null,
    };
  });

export const mergeGameLevels = (l: Map_<GameLevelState>, r: Map_<GameLevelState>) =>
  mergeKeys(l, r, (a, b) => ({
    starsEarned: Math.max(a.starsEarned, b.starsEarned),
    isUnlocked: a.isUnlocked || b.isUnlocked,
  }));

export const mergeLessonProgress = (l: Map_<LessonState>, r: Map_<LessonState>) =>
  mergeKeys(l, r, (a, b) => ({
    isComplete: a.isComplete || b.isComplete,
    bestAccuracy: Math.max(a.bestAccuracy, b.bestAccuracy),
    timesCompleted: Math.max(a.timesCompleted, b.timesCompleted),
    lastCompletedAt: Math.max(a.lastCompletedAt, b.lastCompletedAt),
  }));

export const mergeWordStates = (l: Map_<WordState>, r: Map_<WordState>) =>
  mergeKeys(l, r, (a, b) => {
    const authority = b.timesReviewed >= a.timesReviewed ? b : a;
    return {
      isLearned: a.isLearned || b.isLearned,
      timesReviewed: Math.max(a.timesReviewed, b.timesReviewed),
      easinessFactor: authority.easinessFactor,
      intervalDays: Math.max(a.intervalDays, b.intervalDays),
      nextReviewDate: authority.nextReviewDate,
      lapses: Math.max(a.lapses, b.lapses),
      relearningStep: authority.relearningStep,
    };
  });

const pick = (local: string, remote: string, remoteNewer: boolean) =>
  remoteNewer ? remote || local : local || remote;

/** ISO dates compare lexicographically; the later one wins, a shared one keeps the higher count. */
function ledger(lDate: string, lCount: number, rDate: string, rCount: number): [string, number] {
  if (lDate === rDate) return [lDate, Math.max(lCount, rCount)];
  return lDate > rDate ? [lDate, lCount] : [rDate, rCount];
}

export function mergeProgress(local: UserProgress, remote: UserProgress, today: string = todayIso()): UserProgress {
  const remoteNewer = remote.updatedAt >= local.updatedAt;

  const [ledgerDate, ledgerXp] = ledger(local.dailyXpDate, local.dailyXpEarned, remote.dailyXpDate, remote.dailyXpEarned);
  const mergedReviewDate =
    local.dailyReviewCompletedDate >= remote.dailyReviewCompletedDate
      ? local.dailyReviewCompletedDate
      : remote.dailyReviewCompletedDate;
  const [gamesDate, gamesCount] = ledger(
    local.dailyGamesDate,
    local.dailyGamesPlayedCount,
    remote.dailyGamesDate,
    remote.dailyGamesPlayedCount
  );

  let rawStreak: number;
  let lastActive: string;
  if (local.lastActiveDate === remote.lastActiveDate) {
    rawStreak = Math.max(local.currentStreak, remote.currentStreak);
    lastActive = local.lastActiveDate;
  } else if (local.lastActiveDate > remote.lastActiveDate) {
    rawStreak = local.currentStreak;
    lastActive = local.lastActiveDate;
  } else {
    rawStreak = remote.currentStreak;
    lastActive = remote.lastActiveDate;
  }
  let streak = rawStreak;
  if (rawStreak > 0 && lastActive) {
    const gap = daysBetween(lastActive, today);
    if (gap != null && gap > 1) streak = 0;
  }

  return {
    id: 1,
    userName: pick(local.userName, remote.userName, remoteNewer),
    email: local.email,
    fullName: pick(local.fullName, remote.fullName, remoteNewer),
    age: remoteNewer ? remote.age ?? local.age : local.age ?? remote.age,
    address: pick(local.address, remote.address, remoteNewer),
    profileIconId: remoteNewer ? remote.profileIconId : local.profileIconId,
    totalXp: Math.max(local.totalXp, remote.totalXp),
    level: Math.max(local.level, remote.level),
    currentStreak: streak,
    longestStreak: Math.max(local.longestStreak, remote.longestStreak),
    lastActiveDate: lastActive,
    wordsLearned: Math.max(local.wordsLearned, remote.wordsLearned),
    storiesCompleted: Math.max(local.storiesCompleted, remote.storiesCompleted),
    gamesPlayed: Math.max(local.gamesPlayed, remote.gamesPlayed),
    totalCorrectAnswers: Math.max(local.totalCorrectAnswers, remote.totalCorrectAnswers),
    totalQuestionsAnswered: Math.max(local.totalQuestionsAnswered, remote.totalQuestionsAnswered),
    lessonsCompleted: Math.max(local.lessonsCompleted, remote.lessonsCompleted),
    isOnboardingCompleted: local.isOnboardingCompleted || remote.isOnboardingCompleted,
    dailyGoalXp: remoteNewer ? remote.dailyGoalXp : local.dailyGoalXp,
    dailyXpEarned: ledgerXp,
    dailyXpDate: ledgerDate,
    titleBadge: pick(local.titleBadge, remote.titleBadge, remoteNewer),
    submissionsMade: Math.max(local.submissionsMade, remote.submissionsMade),
    dailyReviewCompletedDate: mergedReviewDate,
    dailyGamesDate: gamesDate,
    dailyGamesPlayedCount: gamesCount,
    updatedAt: Math.max(local.updatedAt, remote.updatedAt),
  };
}

/** The keys firestore.rules' isValidMainProgress() allows, in toMap() order. */
export const MAIN_PROGRESS_KEYS = [
  'id', 'userName', 'email', 'fullName', 'age', 'address', 'profileIconId',
  'totalXp', 'level', 'currentStreak', 'longestStreak', 'lastActiveDate',
  'wordsLearned', 'storiesCompleted', 'gamesPlayed', 'totalCorrectAnswers',
  'totalQuestionsAnswered', 'lessonsCompleted', 'isOnboardingCompleted',
  'dailyGoalXp', 'dailyXpEarned', 'dailyXpDate', 'titleBadge', 'submissionsMade',
  'dailyReviewCompletedDate', 'dailyGamesDate', 'dailyGamesPlayedCount',
  'updatedAt',
] as const;

/** ProgressSyncManager.toMap(): the payload for users/{uid}/progress/main. */
export function toMainDoc(p: UserProgress, stampedAt: number): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const k of MAIN_PROGRESS_KEYS) out[k] = k === 'updatedAt' ? stampedAt : p[k];
  // The rules type-check these as ints; guard against a fractional value ever slipping through.
  for (const k of ['totalXp', 'level', 'currentStreak', 'longestStreak', 'wordsLearned', 'dailyGamesPlayedCount'] as const) {
    out[k] = Math.max(0, Math.trunc(Number(out[k]) || 0));
  }
  return out;
}

const num = (v: unknown, d: number) => (typeof v === 'number' && Number.isFinite(v) ? v : d);
const str = (v: unknown, d = '') => (typeof v === 'string' ? v : d);

/** ProgressSyncManager.toEntity(). */
export function fromMainDoc(d: Record<string, unknown>): UserProgress {
  return {
    id: num(d.id, 1),
    userName: str(d.userName),
    email: str(d.email),
    fullName: str(d.fullName),
    age: typeof d.age === 'number' ? d.age : null,
    address: str(d.address),
    profileIconId: num(d.profileIconId, 1),
    totalXp: num(d.totalXp, 0),
    level: num(d.level, 1),
    currentStreak: num(d.currentStreak, 0),
    longestStreak: num(d.longestStreak, 0),
    lastActiveDate: str(d.lastActiveDate),
    wordsLearned: num(d.wordsLearned, 0),
    storiesCompleted: num(d.storiesCompleted, 0),
    gamesPlayed: num(d.gamesPlayed, 0),
    totalCorrectAnswers: num(d.totalCorrectAnswers, 0),
    totalQuestionsAnswered: num(d.totalQuestionsAnswered, 0),
    lessonsCompleted: num(d.lessonsCompleted, 0),
    isOnboardingCompleted: d.isOnboardingCompleted === true,
    dailyGoalXp: num(d.dailyGoalXp, 100),
    dailyXpEarned: num(d.dailyXpEarned, 0),
    dailyXpDate: str(d.dailyXpDate),
    titleBadge: str(d.titleBadge, 'Kasiguranin Apprentice'),
    submissionsMade: num(d.submissionsMade, 0),
    dailyReviewCompletedDate: str(d.dailyReviewCompletedDate),
    dailyGamesDate: str(d.dailyGamesDate),
    dailyGamesPlayedCount: num(d.dailyGamesPlayedCount, 0),
    updatedAt: num(d.updatedAt, 0),
  };
}

export function parseWordStates(entries: Record<string, Record<string, unknown>>): Map_<WordState> {
  const out: Map_<WordState> = {};
  for (const [k, v] of Object.entries(entries)) {
    out[k] = {
      isLearned: v.isLearned === true,
      timesReviewed: num(v.timesReviewed, 0),
      easinessFactor: num(v.easinessFactor, 2.5),
      intervalDays: num(v.intervalDays, 0),
      nextReviewDate: str(v.nextReviewDate),
      lapses: num(v.lapses, 0),
      relearningStep: num(v.relearningStep, 0),
    };
  }
  return out;
}

export function parseLessons(entries: Record<string, Record<string, unknown>>): Map_<LessonState> {
  const out: Map_<LessonState> = {};
  for (const [k, v] of Object.entries(entries)) {
    out[k] = {
      isComplete: v.isComplete === true,
      bestAccuracy: num(v.bestAccuracy, 0),
      timesCompleted: num(v.timesCompleted, 0),
      lastCompletedAt: num(v.lastCompletedAt, 0),
    };
  }
  return out;
}

export function parseGameLevels(entries: Record<string, Record<string, unknown>>): Map_<GameLevelState> {
  const out: Map_<GameLevelState> = {};
  for (const [k, v] of Object.entries(entries)) {
    out[k] = { starsEarned: num(v.starsEarned, 0), isUnlocked: v.isUnlocked === true };
  }
  return out;
}

export function parseAchievements(entries: Record<string, Record<string, unknown>>): Map_<AchievementState> {
  const out: Map_<AchievementState> = {};
  for (const [k, v] of Object.entries(entries)) {
    out[k] = {
      isUnlocked: v.isUnlocked === true,
      currentValue: num(v.currentValue, 0),
      unlockedDate: typeof v.unlockedDate === 'string' ? v.unlockedDate : null,
    };
  }
  return out;
}

/** Words are keyed by their Kasiguranin headword, lowercased, as ProgressSyncManager.wordKey. */
export const wordKey = (kasiguranin: string) => kasiguranin.toLowerCase();

/** Lesson rows are keyed `${unitId}#${lessonIndex}`; '#' never appears in a unit key. */
export const lessonKey = (unitId: string, lessonIndex: number) => `${unitId}#${lessonIndex}`;

export const levelKey = (gameType: string, levelNumber: number) => `${gameType}_${levelNumber}`;
