/**
 * Everything the learner has done, and every rule that changes it.
 *
 * Port of GamificationRepository (XP policy 2: reward receipts, projection, tiers, normalization),
 * UserProgressRepository (streak quota, onboarding, profile), VocabularyRepository.processWordReview
 * / markAsLearned, LessonRepository.completeLesson and the games' and story reader's reward calls.
 *
 * XP is never added directly. An action records receipts, and settle() projects them into progress:
 * total XP, level, today's XP, badge tiers and their bonus. Each action mutates a draft and records
 * the celebration Android would show, so the store can apply a whole action atomically.
 */
import {
  BADGE_ROWS,
  backgroundUnlocked,
  BADGE_FAMILIES,
  familyFor,
  findBackground,
  isBadgeId,
} from './badges';
import { LEVELS_PER_GAME } from './constants';
import { daysBetween, plusDays, today as todayIso } from './dates';
import { lessonKey, levelKey, wordKey } from './merge';
import { calculateNextReview, MIN_LEARNED_INTERVAL_DAYS, MIN_LEARNED_REVIEWS, ReviewRating } from './sm2';
import type { LessonRef, TreeAccess } from './lesson';
import type {
  AchievementState,
  GameLevelState,
  GameScore,
  LessonState,
  Story,
  StoryProgress,
  UserProgress,
  WordContent,
  WordState,
} from './types';
import { DEFAULT_WORD_STATE, initialProgress } from './types';
import {
  decodeReceipt,
  encodeReceipt,
  gameXp,
  lessonXp,
  levelFor,
  mergeRecord,
  receipt,
  totals,
  XP_POLICY_VERSION,
  type RewardRecord,
} from './xp';

/** The recalculation report shown once on the badges page (ProgressNormalizationEntity). */
export interface Normalization {
  originalXp: number;
  originalLevel: number;
  normalizedXp: number;
  unsupportedHistory: string;
  acknowledged: boolean;
}

export interface LearnerData {
  progress: UserProgress;
  wordStates: Record<string, WordState>;
  lessons: Record<string, LessonState>;
  gameLevels: Record<string, GameLevelState>;
  /** Archived original achievements, plus the 66 tier rows projected from receipts. */
  achievements: Record<string, AchievementState>;
  stories: Record<string, StoryProgress>;
  gameScores: GameScore[];
  /** Reward receipts by their stable id. The source of every XP figure. */
  receipts: Record<string, RewardRecord>;
  normalization: Normalization | null;
}

export type LearnerEvent =
  | { type: 'reward'; activityXp: number; bonusXp: number; level: number; levelChanged: boolean; badgeIds: string[] }
  | { type: 'streak'; days: number };

export interface RewardOutcome {
  activityXp: number;
  bonusXp: number;
  level: number;
  promotions: string[];
}

/** Content the rules need: every word for category mastery, every story for unlocks. */
export interface LearnerContent {
  words: Pick<WordContent, 'kasiguranin' | 'category'>[];
  stories: Pick<Story, 'id' | 'requiredXp'>[];
}

const NO_CONTENT: LearnerContent = { words: [], stories: [] };

export function initialLearner(): LearnerData {
  return {
    progress: initialProgress(),
    wordStates: {},
    lessons: {},
    gameLevels: {},
    achievements: {},
    stories: {},
    gameScores: [],
    receipts: {},
    normalization: null,
  };
}

const UNSUPPORTED_HISTORY =
  'Older review ratings, manual word mastery, game accuracy/hints and replay dates could not be reconstructed. Original XP and badges are archived.';

const wordSource = (text: string) => `word:${text.trim().toLowerCase()}`;

const sameRecord = (a: RewardRecord, b: RewardRecord) =>
  a.day === b.day && a.xp === b.xp && a.value === b.value && a.imported === b.imported;

const sameAchievement = (a: AchievementState | undefined, b: AchievementState) =>
  !!a && a.isUnlocked === b.isUnlocked && a.currentValue === b.currentValue && a.unlockedDate === b.unlockedDate;

/** `word_search_food_dining_3` -> [`word_search_food_dining`, 3]. */
function splitLevelKey(key: string): [string, number] | null {
  const at = key.lastIndexOf('_');
  const n = Number(key.slice(at + 1));
  return at > 0 && Number.isInteger(n) && n > 0 ? [key.slice(0, at), n] : null;
}

/** A draft being changed by one action, plus what the learner should be told about. */
export class Draft {
  readonly events: LearnerEvent[] = [];
  constructor(
    public d: LearnerData,
    readonly today: string = todayIso(),
    readonly content: LearnerContent = NO_CONTENT
  ) {}

  private touch() {
    this.d.progress.updatedAt = Date.now();
  }

  // ── Receipts ────────────────────────────────────────────────────────────────

  private records(): RewardRecord[] {
    return Object.values(this.d.receipts);
  }

  private put(row: RewardRecord) {
    const old = this.d.receipts[row.id];
    const merged = old ? mergeRecord(old, row) : row;
    if (!old || !sameRecord(old, merged)) this.d.receipts[row.id] = merged;
  }

  /** Normalizes once (idempotent): imports what history can prove and archives the old totals. */
  ensureNormalized() {
    if (this.d.normalization) return;
    const progress = { ...this.d.progress };
    for (const s of this.content.stories) {
      if (s.requiredXp <= progress.totalXp) this.put(receipt(`access:story:${s.id}`, 'access', `story:${s.id}`, '', 0, 1, true));
    }
    this.importSupportedHistory();
    this.settle(true);
    const normalized = this.d.progress;
    this.d.normalization = {
      originalXp: progress.totalXp,
      originalLevel: progress.level,
      normalizedXp: normalized.totalXp,
      unsupportedHistory: UNSUPPORTED_HISTORY,
      acknowledged: progress.totalXp === 0 || progress.xpPolicyVersion >= XP_POLICY_VERSION,
    };
    // Old daily goals were effort bands of 50/100/150/200 XP; the new choices are 30/50/80/100.
    if (progress.xpPolicyVersion < XP_POLICY_VERSION && progress.isOnboardingCompleted) {
      const g = progress.dailyGoalXp;
      normalized.dailyGoalXp = g <= 50 ? 30 : g <= 100 ? 50 : g <= 150 ? 80 : 100;
    }
  }

  private importSupportedHistory() {
    const receipts = this.records();
    const has = (source: string, kind: string) => receipts.some((r) => r.source === source && r.kind === kind);
    for (const [key, row] of Object.entries(this.d.lessons)) {
      if (!row.isComplete) continue;
      const source = `lesson:${key}`;
      if (!has(source, 'lesson')) {
        const unit = key.slice(0, key.lastIndexOf('#'));
        this.put(receipt(`${source}:import`, 'lesson', source, '', lessonXp(row.bestAccuracy >= 1), unit.startsWith('mastery:') ? 0 : 1, true));
      }
    }
    for (const [key, row] of Object.entries(this.d.gameLevels)) {
      const parts = splitLevelKey(key);
      if (!parts || row.starsEarned < 1) continue;
      const source = `game:${parts[0]}#${parts[1]}`;
      if (!has(source, 'game')) this.put(receipt(`${source}:import`, 'game', source, '', 5, 1, true));
    }
    for (const [id, row] of Object.entries(this.d.stories)) {
      const source = `story:${id}`;
      if (row.isCompleted && !has(source, 'story')) this.put(receipt(source, 'story', source, '', 20, 1, true));
    }
  }

  /** Restoring version-1 learning records from the cloud; never guesses absent activity. */
  importLegacyLearningState() {
    this.ensureNormalized();
    this.importSupportedHistory();
    this.settle(true);
    this.d.normalization = { ...this.d.normalization!, normalizedXp: this.d.progress.totalXp };
  }

  /** A version-1 cloud document: archive its larger totals and keep the stories they had opened. */
  archiveLegacy(remote: UserProgress) {
    this.ensureNormalized();
    const report = this.d.normalization!;
    if (remote.totalXp > report.originalXp) {
      this.d.normalization = { ...report, originalXp: remote.totalXp, originalLevel: remote.level, acknowledged: false };
    }
    for (const s of this.content.stories) {
      if (s.requiredXp <= remote.totalXp) this.put(receipt(`access:story:${s.id}`, 'access', `story:${s.id}`, '', 0, 1, true));
    }
  }

  /** Receipts from another device. Anything the codec would refuse is dropped, never trusted. */
  mergeRewards(remote: RewardRecord[]) {
    this.ensureNormalized();
    for (const row of remote) if (decodeReceipt(encodeReceipt(row))) this.put(row);
    this.settle(true);
  }

  private transact(action: () => void): RewardOutcome {
    this.ensureNormalized();
    const before = totals(this.records());
    action();
    const promotions = this.settle(false);
    const after = totals(this.records());
    const levelBefore = levelFor(before.total);
    const outcome: RewardOutcome = {
      activityXp: Math.max(0, after.activity - before.activity),
      bonusXp: Math.max(0, after.bonus - before.bonus),
      level: levelFor(after.total),
      promotions,
    };
    if (promotions.length || outcome.level > levelBefore) {
      this.events.push({
        type: 'reward',
        activityXp: outcome.activityXp,
        bonusXp: outcome.bonusXp,
        level: outcome.level,
        levelChanged: outcome.level > levelBefore,
        badgeIds: promotions,
      });
    }
    return outcome;
  }

  /** settleLocked: receipts become metrics, metrics become tiers, and the ledger becomes progress. */
  private settle(imported: boolean): string[] {
    const today = this.today;
    const mastered = new Set(this.records().filter((r) => r.kind === 'mastery').map((r) => r.source));
    if (mastered.size) {
      const byCategory = new Map<string, string[]>();
      for (const w of this.content.words) {
        const list = byCategory.get(w.category) ?? [];
        list.push(wordSource(w.kasiguranin));
        byCategory.set(w.category, list);
      }
      for (const [category, sources] of byCategory) {
        if (sources.length && sources.every((s) => mastered.has(s))) {
          this.put(receipt(`category:${category}`, 'category', category, '', 0));
        }
      }
    }
    const records = this.records();
    const progress = this.d.progress;
    const count = (kind: string, test: (r: RewardRecord) => boolean = () => true) =>
      records.filter((r) => r.kind === kind && test(r)).length;
    const distinct = (rows: RewardRecord[], key: (r: RewardRecord) => string) => new Set(rows.map(key)).size;
    const games = records.filter((r) => r.kind === 'game');
    const metrics: Record<string, number> = {
      verifiedWords: count('mastery'),
      distinctLessons: distinct(records.filter((r) => r.kind === 'lesson' && r.value > 0), (r) => r.source),
      scheduledReviews: count('review', (r) => r.value > 0),
      streak: progress.longestStreak,
      distinctGameLevels: distinct(games, (r) => r.source),
      perfectLevels: count('perfect'),
      gameModesPlayed: distinct(games, (r) =>
        r.source.startsWith('game:word_search_') ? 'word_search' : r.source.slice(5).split('#')[0]
      ),
      storiesCompleted: count('story'),
      verifiedCategories: count('category'),
      submissionsApproved: count('approved'),
    };
    const promotions: string[] = [];
    const setRow = (id: string, next: AchievementState) => {
      if (!sameAchievement(this.d.achievements[id], next)) this.d.achievements[id] = next;
    };
    for (const row of BADGE_ROWS) {
      if (row.family.metric === 'level') continue;
      const value = metrics[row.family.metric] ?? 0;
      const state = this.d.achievements[row.id];
      // A tier earned on another device arrives as its receipt; earned tiers are permanent.
      const wasUnlocked = state?.isUnlocked === true || !!this.d.receipts[row.id];
      const achieved = wasUnlocked || value >= row.requiredValue;
      if (achieved && !this.d.receipts[row.id]) {
        this.put(receipt(row.id, 'badge', row.id, today, row.xpReward, 1, imported));
        if (!imported && !wasUnlocked) promotions.push(row.id);
      }
      setRow(row.id, {
        isUnlocked: achieved,
        currentValue: Math.max(state?.currentValue ?? 0, value),
        unlockedDate: state?.unlockedDate ?? (achieved ? today : null),
      });
    }
    const final = totals(this.records());
    const level = levelFor(final.total);
    for (const row of BADGE_ROWS) {
      if (row.family.metric !== 'level') continue;
      const state = this.d.achievements[row.id];
      const wasUnlocked = state?.isUnlocked === true;
      const achieved = wasUnlocked || level >= row.requiredValue;
      setRow(row.id, { isUnlocked: achieved, currentValue: level, unlockedDate: state?.unlockedDate ?? (achieved ? today : null) });
      if (!imported && achieved && !wasUnlocked) promotions.push(row.id);
    }
    const projected: UserProgress = {
      ...progress,
      totalXp: final.total,
      level,
      activityXp: final.activity,
      badgeBonusXp: final.bonus,
      xpPolicyVersion: XP_POLICY_VERSION,
      dailyXpDate: today,
      dailyXpEarned: final.byDay[today] ?? 0,
      wordsLearned: Math.max(progress.wordsLearned, metrics.verifiedWords),
      storiesCompleted: metrics.storiesCompleted,
      lessonsCompleted: metrics.distinctLessons,
    };
    if ((Object.keys(projected) as (keyof UserProgress)[]).some((k) => projected[k] !== progress[k])) {
      this.d.progress = { ...projected, updatedAt: Date.now() };
    }
    for (const r of records) {
      if (r.kind !== 'story') continue;
      const id = r.source.slice(6);
      const s = this.d.stories[id];
      if (!s?.isCompleted) this.d.stories[id] = { isCompleted: true, currentPage: s?.currentPage ?? 0 };
    }
    return promotions;
  }

  // ── Grants (one per activity, as GamificationRepository) ────────────────────

  lesson(unit: string, index: number, accuracy: number): RewardOutcome {
    return this.transact(() => {
      if (!(Number.isFinite(accuracy) && accuracy >= 0 && accuracy <= 1)) throw new Error('Accuracy must be 0..1');
      const source = `lesson:${unit}#${index}`;
      const day = this.today;
      const previous = this.records().filter((r) => r.source === source && r.kind === 'lesson');
      this.put(receipt(`${source}:${day}`, 'lesson', source, day, lessonXp(accuracy >= 1), unit.startsWith('mastery:') ? 0 : 1));
      if (previous.length && !previous.some((r) => r.day === day && !r.imported)) {
        this.put(receipt(`replay:${source}:${day}`, 'replay', source, day, 5));
      }
      const key = lessonKey(unit, index);
      const existing = this.d.lessons[key];
      this.d.lessons[key] = {
        isComplete: true,
        bestAccuracy: Math.max(existing?.bestAccuracy ?? 0, accuracy),
        timesCompleted: (existing?.timesCompleted ?? 0) + 1,
        lastCompletedAt: Date.now(),
      };
    });
  }

  /** A finished round. Under one star there is no reward and the level does not move. */
  game(mode: string, gameLevelKey: string, n: number, correct: number, total: number, stars: number, perfect: boolean): RewardOutcome {
    return this.transact(() => {
      if (stars < 1) return;
      if (perfect && correct !== total) throw new Error('A perfect round answers everything');
      const source = `game:${gameLevelKey}#${n}`;
      const day = this.today;
      const reward = gameXp(mode, correct, total, perfect);
      const previous = this.records().filter((r) => r.source === source && r.kind === 'game');
      this.put(receipt(`${source}:${day}`, 'game', source, day, reward));
      if (previous.length && !previous.some((r) => r.day === day && !r.imported)) {
        this.put(receipt(`replay:${source}:${day}`, 'replay', source, day, Math.floor(reward / 4)));
      }
      if (perfect) this.put(receipt(`perfect:${source}`, 'perfect', source, '', 0));
      this.saveLevelResult(gameLevelKey, n, Math.min(3, Math.max(1, stars)));
    });
  }

  story(id: number): RewardOutcome {
    return this.transact(() => {
      const source = `story:${id}`;
      if (!this.d.receipts[source]) this.put(receipt(source, 'story', source, this.today, 20));
      const s = this.storyProgress(id);
      this.d.stories[String(id)] = { ...s, isCompleted: true };
    });
  }

  /** One retrieval. `due` is a scheduled review; `mastered` is SM-2's verdict after this answer. */
  review(kasiguranin: string, rating: ReviewRating, due: boolean, mastered: boolean): RewardOutcome {
    return this.transact(() => {
      const day = this.today;
      const source = wordSource(kasiguranin);
      const positive = rating !== ReviewRating.AGAIN;
      if (positive) this.put(receipt(`retrieval:${source}:${day}`, 'retrieval', source, day, 0));
      if (due) {
        const id = `review:${source}:${day}`;
        const xp = this.d.receipts[id]?.xp ?? (rating === ReviewRating.AGAIN ? 1 : rating === ReviewRating.HARD ? 2 : 3);
        this.put(receipt(id, 'review', source, day, xp, positive ? 1 : 0));
      }
      const retrievals = this.records().filter((r) => r.kind === 'retrieval' && r.source === source).length;
      if (mastered && positive && retrievals >= 3 && !this.d.receipts[`mastery:${source}`]) {
        this.put(receipt(`mastery:${source}`, 'mastery', source, day, 5));
      }
    });
  }

  /** Approved submissions, `word:<id>` or `literature:<id>`, with the day each was approved if known. */
  approved(contributions: { id: string; approvedDay: string | null }[]): RewardOutcome {
    return this.transact(() => {
      for (const c of contributions) {
        const key = `approved:${c.id}`;
        const day = c.approvedDay ?? '';
        if (!this.d.receipts[key]) this.put(receipt(key, 'approved', c.id, day, 10, 1, day !== this.today));
      }
    });
  }

  updateMetrics(): RewardOutcome {
    return this.transact(() => {});
  }

  /** Sections a learner has seen open stay open, whatever the XP gate says later. */
  preserveAccess(keys: string[]) {
    for (const k of keys) {
      if (!this.d.receipts[`access:${k}`]) this.put(receipt(`access:${k}`, 'access', k, '', 0, 1, true));
    }
  }

  /** Pins or unpins a badge family on the profile; at most three, and only an earned one. */
  pin(familyId: string) {
    const fam = BADGE_FAMILIES.find((f) => f.id === familyId);
    if (!fam) return;
    const earned = Object.entries(this.d.achievements).some(([id, s]) => s.isUnlocked && familyFor(id)?.id === familyId);
    if (!earned) return;
    const pins = this.d.progress.pinnedBadgeIds.split(',').filter((s) => s.trim());
    const i = pins.indexOf(familyId);
    if (i >= 0) pins.splice(i, 1);
    else if (pins.length < 3) pins.push(familyId);
    this.d.progress.pinnedBadgeIds = pins.join(',');
    this.touch();
  }

  acknowledgeNormalization() {
    if (this.d.normalization) this.d.normalization = { ...this.d.normalization, acknowledged: true };
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
    this.touch();
    this.updateMetrics();
    this.events.push({ type: 'streak', days: streak });
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

  /**
   * processWordReview. A retrieval keeps the streak alive; [scheduled] marks a due review from the
   * deck, the only kind that earns review XP. Mastery XP needs three successful days on top of SM-2.
   */
  reviewWord(word: Pick<WordContent, 'kasiguranin'>, rating: ReviewRating, scheduled = false): { state: WordState; xp: number } {
    const after = calculateNextReview(this.wordState(word.kasiguranin), rating, this.today);
    this.d.wordStates[wordKey(word.kasiguranin)] = after;
    this.touch();
    this.recordLearningActivity();
    const outcome = this.review(word.kasiguranin, rating, scheduled, after.isLearned);
    return { state: after, xp: outcome.activityXp };
  }

  /** "I already know this one": SM-2 keeps it, but only verified retrieval earns mastery XP. */
  markAsLearned(word: Pick<WordContent, 'kasiguranin'>) {
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
    this.touch();
  }

  unmarkAsLearned(word: Pick<WordContent, 'kasiguranin'>) {
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

  /** Returns the activity XP the lesson earned (0 for a same-day repeat). */
  completeLesson(ref: LessonRef, accuracy: number): number {
    const xp = this.lesson(ref.unitId, ref.lessonIndex, accuracy).activityXp;
    this.recordLearningActivity();
    return xp;
  }

  // ── Games ───────────────────────────────────────────────────────────────────

  level(gameType: string, n: number): GameLevelState {
    return this.d.gameLevels[levelKey(gameType, n)] ?? { starsEarned: 0, isUnlocked: n === 1 };
  }

  saveLevelResult(gameType: string, n: number, stars: number) {
    const current = this.level(gameType, n);
    if (stars > current.starsEarned || !current.isUnlocked) {
      this.d.gameLevels[levelKey(gameType, n)] = { starsEarned: Math.max(current.starsEarned, stars), isUnlocked: true };
    }
    if (stars >= 1 && n < LEVELS_PER_GAME) {
      const next = this.level(gameType, n + 1);
      if (!next.isUnlocked) this.d.gameLevels[levelKey(gameType, n + 1)] = { ...next, isUnlocked: true };
    }
    this.touch();
  }

  /**
   * The bookkeeping every mini-game does when a round ends: the reward (which also records the
   * stars), the score, the daily game count behind the streak, and accuracy. Returns the XP earned.
   */
  finishGame(opts: {
    mode: string;
    levelKeyType?: string;
    level: number;
    correct: number;
    total: number;
    stars: number;
    perfect: boolean;
    statsTotal?: number;
  }): number {
    const correct = Math.min(opts.correct, opts.total);
    const xp = this.game(opts.mode, opts.levelKeyType ?? opts.mode, opts.level, correct, opts.total, opts.stars, opts.perfect).activityXp;
    this.d.gameScores.push({ gameType: opts.mode, score: correct, totalQuestions: opts.total, xpEarned: xp, playedAt: Date.now() });
    if (this.d.gameScores.length > 200) this.d.gameScores.splice(0, this.d.gameScores.length - 200);
    this.incrementGamesPlayed();
    // Word Search and Word Wheel count wrong lines or hints as extra questions for accuracy only.
    this.updateGameStats(correct, opts.statsTotal ?? opts.total);
    return xp;
  }

  // ── Stories ─────────────────────────────────────────────────────────────────

  storyProgress(id: number): StoryProgress {
    return this.d.stories[String(id)] ?? { isCompleted: false, currentPage: 0 };
  }

  /** Saves the reading position; XP comes only with the first completion. */
  storyPage(id: number, newIndex: number) {
    const s = this.storyProgress(id);
    if (!s.isCompleted) this.d.stories[String(id)] = { ...s, currentPage: newIndex };
  }

  /** Returns the XP earned: 20 the first time, nothing on a reread. */
  completeStory(id: number): number {
    if (this.storyProgress(id).isCompleted && this.d.receipts[`story:${id}`]) return 0;
    return this.story(id).activityXp;
  }

  // ── Profile ─────────────────────────────────────────────────────────────────

  /** Personal onboarding preferences grant no XP or practice-day streak. */
  completeOnboarding(userName: string, avatarId: number, dailyGoalXp: number, titleBadge: string) {
    this.ensureNormalized();
    const p = this.d.progress;
    p.isOnboardingCompleted = true;
    p.userName = userName;
    p.profileIconId = avatarId;
    p.dailyGoalXp = dailyGoalXp;
    p.titleBadge = titleBadge;
    this.touch();
  }

  /** Throws when the background is still locked, as UserProgressRepository.updateBackground. */
  selectBackground(id: string) {
    const b = findBackground(id);
    if (!b) return;
    const p = this.d.progress;
    if (!backgroundUnlocked(b, p.level, p.longestStreak)) throw new Error('This background is still locked.');
    p.profileBackgroundId = b.id;
    this.touch();
  }

  incrementSubmissionsMade() {
    this.d.progress.submissionsMade += 1;
    this.touch();
  }
}

// ── Reads ─────────────────────────────────────────────────────────────────────

const totalsCache = new WeakMap<Record<string, RewardRecord>, ReturnType<typeof totals>>();

/** The ledger's totals, cached per receipts object (the store replaces it on every change). */
export function ledgerTotals(d: Pick<LearnerData, 'receipts'>) {
  let t = totalsCache.get(d.receipts);
  if (!t) {
    t = totals(Object.values(d.receipts));
    totalsCache.set(d.receipts, t);
  }
  return t;
}

/** Activity XP earned today, from dated receipts only; badge bonuses and imports never count. */
export function dailyXpEarned(d: Pick<LearnerData, 'receipts'>, today = todayIso()) {
  return ledgerTotals(d).byDay[today] ?? 0;
}

/** This week's activity XP, Monday to today, as the weekly leaderboard counts it. */
export function weeklyXp(d: Pick<LearnerData, 'receipts'>, today = todayIso()) {
  const date = new Date(`${today}T00:00:00Z`);
  const monday = plusDays(today, -((date.getUTCDay() + 6) % 7));
  return Object.entries(ledgerTotals(d).byDay)
    .filter(([day]) => day >= monday && day <= today)
    .reduce((s, [, xp]) => s + xp, 0);
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

/** A story is open once its XP is reached, or for good once an access receipt says it was. */
export function storyUnlocked(d: LearnerData, story: Pick<Story, 'id' | 'requiredXp'>) {
  return story.requiredXp <= d.progress.totalXp || !!d.receipts[`access:story:${story.id}`];
}

/** The earned tier rows of the 66, for the profile and the public projection. */
export function earnedBadgeIds(d: Pick<LearnerData, 'achievements'>): string[] {
  return BADGE_ROWS.filter((r) => d.achievements[r.id]?.isUnlocked).map((r) => r.id);
}

/** Originals earned before policy 2, shown under Legacy. */
export function legacyAchievements(d: Pick<LearnerData, 'achievements'>) {
  return Object.entries(d.achievements).filter(([id, s]) => !isBadgeId(id) && s.isUnlocked);
}

export const pinnedFamilies = (p: UserProgress) => p.pinnedBadgeIds.split(',').filter((s) => s.trim());

const accessCache = new WeakMap<Record<string, RewardRecord>, TreeAccess>();

/** What keeps learning sections open (access receipts and imported lessons), for buildTree. */
export function treeAccess(d: Pick<LearnerData, 'receipts'>): TreeAccess {
  let a = accessCache.get(d.receipts);
  if (!a) {
    const rows = Object.values(d.receipts);
    a = {
      savedAccess: new Set(rows.filter((r) => r.kind === 'access').map((r) => r.source)),
      importedLessons: Object.fromEntries(rows.filter((r) => r.kind === 'lesson' && r.imported).map((r) => [r.source, r.xp])),
    };
    accessCache.set(d.receipts, a);
  }
  return a;
}
