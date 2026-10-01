/**
 * Everything the learner has done, and every rule that changes it.
 *
 * Port of UserProgressRepository, VocabularyRepository.processWordReview / markAsLearned,
 * LessonRepository.completeLesson, GameLevelRepository.saveLevelResult, GameRepository.saveGameScore
 * and the story reader's XP. Each action mutates a draft and records the celebratory events Android
 * emits (level up, streak day, badge), so the store can apply a whole action atomically and show the
 * right dialog afterwards.
 *
 * A few Android quirks are kept on purpose because they are observable through sync: a badge's XP
 * reward and the onboarding welcome bonus go straight to totalXp without touching the daily ledger or
 * recomputing the level (the DAO does a bare `totalXp = totalXp + x`), and lessonsCompleted is never
 * incremented (Profile counts lesson rows instead).
 */
import { calculateLevel, LEVELS_PER_GAME, XP_PER_STORY_COMPLETE, XP_PER_STORY_PAGE, XP_PER_WORD_LEARNED } from './constants';
import { daysBetween, today as todayIso } from './dates';
import { ACHIEVEMENTS, METRIC } from './gamification';
import { lessonKey, levelKey, wordKey } from './merge';
import { calculateNextReview, MIN_LEARNED_INTERVAL_DAYS, MIN_LEARNED_REVIEWS, ReviewRating } from './sm2';
import { plusDays } from './dates';
import { xpForLesson, type LessonRef } from './lesson';
import type {
  AchievementState,
  GameLevelState,
  GameScore,
  LessonState,
  StoryProgress,
  UserProgress,
  WordContent,
  WordState,
} from './types';
import { DEFAULT_WORD_STATE, initialProgress } from './types';

export interface LearnerData {
  progress: UserProgress;
  wordStates: Record<string, WordState>;
  lessons: Record<string, LessonState>;
  gameLevels: Record<string, GameLevelState>;
  achievements: Record<string, AchievementState>;
  stories: Record<string, StoryProgress>;
  gameScores: GameScore[];
}

export type LearnerEvent =
  | { type: 'levelUp'; level: number }
  | { type: 'streak'; days: number }
  | { type: 'badge'; id: string };

export function initialAchievements(): Record<string, AchievementState> {
  const out: Record<string, AchievementState> = {};
  for (const a of ACHIEVEMENTS) out[a.id] = { isUnlocked: !!a.startsUnlocked, currentValue: 0, unlockedDate: null };
  return out;
}

export function initialLearner(): LearnerData {
  return {
    progress: initialProgress(),
    wordStates: {},
    lessons: {},
    gameLevels: {},
    achievements: initialAchievements(),
    stories: {},
    gameScores: [],
  };
}

/** A draft being changed by one action, plus what the learner should be told about. */
export class Draft {
  readonly events: LearnerEvent[] = [];
  constructor(public d: LearnerData, readonly today: string = todayIso()) {}

  private touch() {
    this.d.progress.updatedAt = Date.now();
  }

  // ── XP, level and badges ────────────────────────────────────────────────────

  addXp(xp: number) {
    const p = this.d.progress;
    p.totalXp += xp;
    if (p.dailyXpDate === this.today) p.dailyXpEarned += xp;
    else p.dailyXpEarned = xp;
    p.dailyXpDate = this.today;
    const newLevel = calculateLevel(p.totalXp);
    if (newLevel !== p.level) {
      p.level = newLevel;
      this.checkAchievements(METRIC.LEVEL, newLevel);
      this.events.push({ type: 'levelUp', level: newLevel });
    }
    this.touch();
  }

  checkAchievements(metricType: string, value: number) {
    let rewarded = false;
    for (const def of ACHIEVEMENTS) {
      if (def.metricType !== metricType) continue;
      const state = this.d.achievements[def.id] ?? { isUnlocked: false, currentValue: 0, unlockedDate: null };
      const next = { ...state, currentValue: Math.max(state.currentValue, value) };
      if (!state.isUnlocked && value >= def.requiredValue) {
        next.isUnlocked = true;
        next.unlockedDate = this.today;
        this.d.progress.totalXp += def.xpReward;
        this.events.push({ type: 'badge', id: def.id });
        rewarded = true;
      }
      this.d.achievements[def.id] = next;
    }
    // A badge's reward is XP like any other, so it can cross a level threshold. Android leaves the
    // stored level behind until the next ordinary XP award; recomputing here keeps the rank and the
    // level badge in agreement, and the cross-device merge takes the higher level either way.
    if (rewarded) {
      const level = calculateLevel(this.d.progress.totalXp);
      if (level > this.d.progress.level) {
        this.d.progress.level = level;
        this.events.push({ type: 'levelUp', level });
        this.checkAchievements(METRIC.LEVEL, level);
      }
    }
    this.touch();
  }

  // ── Streak ──────────────────────────────────────────────────────────────────

  get quota() {
    const p = this.d.progress;
    const reviewCompleted = p.dailyReviewCompletedDate === this.today;
    const gamesPlayed = p.dailyGamesDate === this.today ? p.dailyGamesPlayedCount : 0;
    return { reviewCompleted, gamesPlayed, requiredGames: 3, isMet: reviewCompleted && gamesPlayed >= 3 };
  }

  validateStreak() {
    const p = this.d.progress;
    if (p.currentStreak > 0 && p.lastActiveDate) {
      const gap = daysBetween(p.lastActiveDate, this.today);
      if (gap != null && gap > 1) {
        p.currentStreak = 0;
        this.touch();
      }
    }
  }

  /** checkStreakQuotaAndAdvance: the streak moves only once the day's quota is met. */
  recordLearningActivity() {
    if (this.quota.isMet) this.updateStreak();
  }

  private updateStreak() {
    const p = this.d.progress;
    if (p.lastActiveDate === this.today) return;
    let streak = 1;
    if (p.lastActiveDate) {
      const gap = daysBetween(p.lastActiveDate, this.today);
      streak = gap != null && gap <= 1 ? p.currentStreak + 1 : 1;
    }
    p.currentStreak = streak;
    p.longestStreak = Math.max(p.longestStreak, streak);
    p.lastActiveDate = this.today;
    this.checkAchievements(METRIC.STREAK, streak);
    this.events.push({ type: 'streak', days: streak });
    this.touch();
  }

  recordDailyReviewCompleted() {
    this.d.progress.dailyReviewCompletedDate = this.today;
    this.touch();
    this.recordLearningActivity();
  }

  incrementGamesPlayed() {
    const p = this.d.progress;
    p.gamesPlayed += 1;
    if (p.dailyGamesDate === this.today) p.dailyGamesPlayedCount += 1;
    else p.dailyGamesPlayedCount = 1;
    p.dailyGamesDate = this.today;
    this.touch();
    this.recordLearningActivity();
    this.checkAchievements(METRIC.GAMES_PLAYED, p.gamesPlayed);
  }

  updateGameStats(correct: number, total: number) {
    this.d.progress.totalCorrectAnswers += correct;
    this.d.progress.totalQuestionsAnswered += total;
    this.touch();
  }

  // ── Words ───────────────────────────────────────────────────────────────────

  wordState(kasiguranin: string): WordState {
    return this.d.wordStates[wordKey(kasiguranin)] ?? { ...DEFAULT_WORD_STATE };
  }

  /** processWordReview. [corpus] is needed only to test whether a whole category is now learned. */
  reviewWord(word: WordContent, rating: ReviewRating, corpus: WordContent[]) {
    const before = this.wordState(word.kasiguranin);
    const after = calculateNextReview(before, rating, this.today);
    this.d.wordStates[wordKey(word.kasiguranin)] = after;
    this.touch();
    this.recordLearningActivity();
    if (!before.isLearned && after.isLearned) {
      this.d.progress.wordsLearned += 1;
      this.checkAchievements(METRIC.WORDS_LEARNED, this.d.progress.wordsLearned);
      this.addXp(XP_PER_WORD_LEARNED);
      this.checkCategoryMastery(word.category, corpus);
    }
    return after;
  }

  private checkCategoryMastery(category: string, corpus: WordContent[]) {
    const words = corpus.filter((w) => w.category === category);
    if (words.length && words.every((w) => this.wordState(w.kasiguranin).isLearned)) {
      this.checkAchievements(METRIC.CATEGORY_MASTERED, 1);
    }
  }

  /** "I already know this one": seeds SM-2 to a state that genuinely meets the mastery bar. */
  markAsLearned(word: WordContent, corpus: WordContent[]) {
    const before = this.wordState(word.kasiguranin);
    if (before.isLearned) return;
    const r = calculateNextReview(before, ReviewRating.GOOD, this.today);
    const interval = Math.max(MIN_LEARNED_INTERVAL_DAYS, r.intervalDays);
    this.d.wordStates[wordKey(word.kasiguranin)] = {
      ...before,
      easinessFactor: r.easinessFactor,
      intervalDays: interval,
      nextReviewDate: plusDays(this.today, interval),
      timesReviewed: Math.max(MIN_LEARNED_REVIEWS, r.timesReviewed),
      isLearned: true,
    };
    this.d.progress.wordsLearned += 1;
    this.checkAchievements(METRIC.WORDS_LEARNED, this.d.progress.wordsLearned);
    this.addXp(XP_PER_WORD_LEARNED);
    this.checkCategoryMastery(word.category, corpus);
  }

  unmarkAsLearned(word: WordContent) {
    const before = this.wordState(word.kasiguranin);
    this.d.wordStates[wordKey(word.kasiguranin)] = {
      ...before,
      isLearned: false,
      timesReviewed: 0,
      intervalDays: 0,
      nextReviewDate: '',
      relearningStep: 0,
    };
    this.touch();
  }

  // ── Lessons ─────────────────────────────────────────────────────────────────

  completeLesson(ref: LessonRef, accuracy: number): number {
    const key = lessonKey(ref.unitId, ref.lessonIndex);
    const existing = this.d.lessons[key];
    this.d.lessons[key] = {
      isComplete: true,
      bestAccuracy: Math.max(existing?.bestAccuracy ?? 0, accuracy),
      timesCompleted: (existing?.timesCompleted ?? 0) + 1,
      lastCompletedAt: Date.now(),
    };
    const xp = xpForLesson(accuracy);
    this.addXp(xp);
    this.recordLearningActivity();
    return xp;
  }

  // ── Games ───────────────────────────────────────────────────────────────────

  level(gameType: string, n: number): GameLevelState {
    return this.d.gameLevels[levelKey(gameType, n)] ?? { starsEarned: 0, isUnlocked: n === 1 };
  }

  saveLevelResult(gameType: string, n: number, stars: number) {
    const current = this.level(gameType, n);
    if (stars > current.starsEarned) {
      this.d.gameLevels[levelKey(gameType, n)] = { ...current, starsEarned: stars };
    }
    if (stars >= 1 && n < LEVELS_PER_GAME) {
      const next = this.level(gameType, n + 1);
      if (!next.isUnlocked) this.d.gameLevels[levelKey(gameType, n + 1)] = { ...next, isUnlocked: true };
    }
    this.touch();
  }

  saveGameScore(score: GameScore) {
    this.d.gameScores.push(score);
    if (this.d.gameScores.length > 200) this.d.gameScores.splice(0, this.d.gameScores.length - 200);
    this.checkAchievements(METRIC.GAME_MODES_PLAYED, new Set(this.d.gameScores.map((s) => s.gameType)).size);
  }

  /**
   * The bookkeeping every mini-game does when a round ends (WordMatchViewModel.endGame and its
   * siblings): score, stars, XP, the daily game count behind the streak, and the perfect badge.
   */
  finishGame(opts: { gameType: string; levelKeyType?: string; level: number; score: number; total: number; xp: number; stars: number; perfect: boolean; statsTotal?: number }) {
    this.saveGameScore({
      gameType: opts.gameType,
      score: opts.score,
      totalQuestions: opts.total,
      xpEarned: opts.xp,
      playedAt: Date.now(),
    });
    this.saveLevelResult(opts.levelKeyType ?? opts.gameType, opts.level, opts.stars);
    this.addXp(opts.xp);
    this.incrementGamesPlayed();
    // Word Search and Word Wheel count wrong lines or hints as extra questions for accuracy only.
    this.updateGameStats(opts.score, opts.statsTotal ?? opts.total);
    if (opts.perfect) this.checkAchievements(METRIC.PERFECT_GAME, 1);
  }

  // ── Stories ─────────────────────────────────────────────────────────────────

  story(id: number): StoryProgress {
    return this.d.stories[String(id)] ?? { isCompleted: false, currentPage: 0 };
  }

  storyPageTurned(id: number, newIndex: number) {
    this.addXp(XP_PER_STORY_PAGE);
    const s = this.story(id);
    if (!s.isCompleted) this.d.stories[String(id)] = { ...s, currentPage: newIndex };
  }

  storyPageBack(id: number, newIndex: number) {
    const s = this.story(id);
    if (!s.isCompleted) this.d.stories[String(id)] = { ...s, currentPage: newIndex };
  }

  completeStory(id: number, totalPages: number) {
    const s = this.story(id);
    if (s.isCompleted) return;
    this.d.stories[String(id)] = { isCompleted: true, currentPage: totalPages };
    this.addXp(XP_PER_STORY_COMPLETE);
    this.d.progress.storiesCompleted += 1;
    this.checkAchievements(METRIC.STORIES_COMPLETED, this.d.progress.storiesCompleted);
  }

  // ── Profile ─────────────────────────────────────────────────────────────────

  completeOnboarding(userName: string, avatarId: number, dailyGoalXp: number, titleBadge: string) {
    const p = this.d.progress;
    p.isOnboardingCompleted = true;
    p.userName = userName;
    p.profileIconId = avatarId;
    p.dailyGoalXp = dailyGoalXp;
    p.titleBadge = titleBadge;
    p.totalXp += 50;
    p.currentStreak = Math.max(1, p.currentStreak);
    p.longestStreak = Math.max(1, p.longestStreak);
    p.lastActiveDate = this.today;
    this.touch();
  }

  incrementSubmissionsMade() {
    this.d.progress.submissionsMade += 1;
    this.checkAchievements(METRIC.SUBMISSIONS_MADE, this.d.progress.submissionsMade);
  }
}

/** The day goal is met when the XP target is reached and nothing is still due. */
export function dailyXpEarned(p: UserProgress, today = todayIso()) {
  return p.dailyXpDate === today ? p.dailyXpEarned : 0;
}

export function practisedToday(p: UserProgress, today = todayIso()) {
  return p.lastActiveDate === today;
}

export function streakAtRisk(p: UserProgress, today = todayIso()) {
  return p.currentStreak > 0 && !practisedToday(p, today);
}

export function totalStars(levels: Record<string, GameLevelState>): number {
  return Object.values(levels).reduce((s, l) => s + l.starsEarned, 0);
}
