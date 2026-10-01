/**
 * Ports of the Android unit tests that pin the shared rules. The web app writes the same synced
 * documents the Android app reads, so these rules have to agree with the Kotlin to the day.
 */
import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { calculateNextReview, ReviewRating } from '../src/domain/sm2';
import { DEFAULT_WORD_STATE, initialProgress, type UserProgress, type WordContent, type WordState } from '../src/domain/types';
import { fromMainDoc, MAIN_PROGRESS_KEYS, mergeProgress, mergeWordStates, toMainDoc } from '../src/domain/merge';
import { matchRecall, meaningFor } from '../src/domain/recall';
import { buildExercises, buildTree, nextLesson, openLessons, teachingOrder, wordsFor } from '../src/domain/lesson';
import { Corpus } from '../src/domain/corpus';
import { Draft, initialLearner } from '../src/domain/learner';
import { isoWeekId, plusDays } from '../src/domain/dates';
import { calculateLevel } from '../src/domain/constants';
import { generateWordSearch } from '../src/domain/wordSearch';
import { generateWordWheel } from '../src/domain/wordWheel';
import { KotlinRandom } from '../src/domain/kotlinRandom';

const TODAY = '2026-09-30';
const card = (o: Partial<WordState> = {}): WordState => ({ ...DEFAULT_WORD_STATE, ...o });

describe('SM-2 (Sm2AlgorithmTest, Sm2RelearningTest)', () => {
  it('failed recall resets the interval and never learns', () => {
    const r = calculateNextReview(card({ intervalDays: 6, timesReviewed: 5 }), ReviewRating.AGAIN, TODAY);
    expect(r.intervalDays).toBe(1);
    expect(r.isLearned).toBe(false);
    expect(r.nextReviewDate).toBe(plusDays(TODAY, 1));
  });
  it('second recall moves to six days but is not yet learned', () => {
    const r = calculateNextReview(card({ intervalDays: 1, timesReviewed: 1 }), ReviewRating.GOOD, TODAY);
    expect(r.intervalDays).toBe(6);
    expect(r.isLearned).toBe(false);
  });
  it('third recall across a real gap learns', () => {
    const r = calculateNextReview(card({ easinessFactor: 2.5, intervalDays: 6, timesReviewed: 2 }), ReviewRating.GOOD, TODAY);
    expect(r.intervalDays).toBe(15);
    expect(r.isLearned).toBe(true);
    expect(r.nextReviewDate).toBe('2026-10-15');
  });
  it('same-day repetition never certifies a word', () => {
    let c = card();
    for (let i = 0; i < 2; i++) {
      c = calculateNextReview(c, ReviewRating.EASY, TODAY);
      expect(c.isLearned).toBe(false);
    }
  });
  it('the way back is a 1-2-4 ladder', () => {
    let c = card({ intervalDays: 30, timesReviewed: 8, isLearned: true });
    c = calculateNextReview(c, ReviewRating.AGAIN, TODAY);
    expect([c.intervalDays, c.lapses, c.relearningStep]).toEqual([1, 1, 1]);
    c = calculateNextReview(c, ReviewRating.GOOD, TODAY);
    expect([c.intervalDays, c.relearningStep]).toEqual([2, 2]);
    c = calculateNextReview(c, ReviewRating.GOOD, TODAY);
    expect([c.intervalDays, c.relearningStep]).toEqual([4, 3]);
    c = calculateNextReview(c, ReviewRating.GOOD, TODAY);
    expect(c.relearningStep).toBe(0);
    expect(c.intervalDays).toBeGreaterThan(4);
  });
  it('easiness factor floors at 1.3 and rises on EASY', () => {
    expect(calculateNextReview(card({ easinessFactor: 1.3 }), ReviewRating.AGAIN, TODAY).easinessFactor).toBeCloseTo(1.3);
    expect(calculateNextReview(card({ easinessFactor: 2.5 }), ReviewRating.EASY, TODAY).easinessFactor).toBeCloseTo(2.6);
  });
});

describe('recall matching (RecallAnswerMatcherTest)', () => {
  const cases: [string, string, string][] = [
    ['abben', 'abben', 'Exact'],
    ['  AbBeN ', 'abben', 'Exact'],
    ['adeg', 'adëg', 'Exact'],
    ['telompulu', 'tǝlompulu', 'Exact'],
    ['telompulu', 'təlompulu', 'Exact'],
    ['ulo', 'uló', 'Exact'],
    ['saan', 'saân', 'Exact'],
    ['kasi', 'kaśi', 'Exact'],
    ['magaral', 'mag-aral', 'Exact'],
    ['abbem', 'abben', 'Close'],
    ['kasinungalingam', 'kasinungalingan', 'Close'],
    ['ako', 'aso', 'Wrong'],
    ['bungaw', 'buto/bungaw', 'Exact'],
    ['balimbing', 'koloran, balimbing', 'Exact'],
    ['bungow', 'buto/bungaw', 'Close'],
    ['', 'ay', 'Wrong'],
    ['anak   anak', 'anak anak', 'Exact'],
  ];
  it.each(cases)('%s vs %s is %s', (typed, expected, result) => {
    expect(matchRecall(typed, expected)).toBe(result);
  });
  it('never prompts with a gloss identical to the headword', () => {
    expect(meaningFor('buhay', 'buhay', 'life')).toBe('life');
    expect(meaningFor('sayaw', 'Sayaw', '')).toBeNull();
  });
});

describe('progress merge (ProgressSyncTest)', () => {
  const p = (o: Partial<UserProgress>): UserProgress => ({ ...initialProgress(), ...o });
  it('lifetime counters take the max whichever side is newer', () => {
    expect(mergeProgress(p({ submissionsMade: 7, updatedAt: 1 }), p({ submissionsMade: 3, updatedAt: 2 }), TODAY).submissionsMade).toBe(7);
    const m = mergeProgress(p({ totalXp: 100, wordsLearned: 5, updatedAt: 1 }), p({ totalXp: 250, wordsLearned: 3, updatedAt: 2 }), TODAY);
    expect([m.totalXp, m.wordsLearned, m.updatedAt]).toEqual([250, 5, 2]);
  });
  it('profile comes from the newer side, email never syncs', () => {
    expect(mergeProgress(p({ fullName: 'Adrian', updatedAt: 1 }), p({ fullName: 'Anthony', updatedAt: 5 }), TODAY).fullName).toBe('Anthony');
    expect(mergeProgress(p({ fullName: 'Adrian', updatedAt: 9 }), p({ fullName: 'Anthony', updatedAt: 5 }), TODAY).fullName).toBe('Adrian');
    expect(mergeProgress(p({ email: 'a@b.c' }), p({ updatedAt: 999 }), TODAY).email).toBe('a@b.c');
  });
  it('daily ledger: later date wins outright, same date takes the higher count', () => {
    expect(mergeProgress(p({ dailyXpDate: '2026-08-17', dailyXpEarned: 20 }), p({ dailyXpDate: '2026-08-16', dailyXpEarned: 90 }), TODAY).dailyXpEarned).toBe(20);
    expect(mergeProgress(p({ dailyXpDate: '2026-08-17', dailyXpEarned: 30 }), p({ dailyXpDate: '2026-08-17', dailyXpEarned: 75 }), TODAY).dailyXpEarned).toBe(75);
  });
  it('a stale streak expires on merge', () => {
    const m = mergeProgress(p({ currentStreak: 4, lastActiveDate: '2026-09-20' }), p({ currentStreak: 2, lastActiveDate: '2026-09-19' }), TODAY);
    expect(m.currentStreak).toBe(0);
    const live = mergeProgress(p({ currentStreak: 4, lastActiveDate: '2026-09-29' }), p({}), TODAY);
    expect(live.currentStreak).toBe(4);
  });
  it('round-trips through the cloud payload with exactly the keys the rules allow', () => {
    const original = p({ submissionsMade: 12, totalXp: 340, dailyGamesPlayedCount: 2, updatedAt: 5 });
    const doc = toMainDoc(original, 123);
    expect(Object.keys(doc).sort()).toEqual([...MAIN_PROGRESS_KEYS].sort());
    expect(fromMainDoc(doc).submissionsMade).toBe(12);
    expect(fromMainDoc(doc).updatedAt).toBe(123);
  });
  it('word state authority is the side that reviewed more', () => {
    const m = mergeWordStates(
      { a: card({ timesReviewed: 5, intervalDays: 15, nextReviewDate: '2026-10-10', easinessFactor: 2.6 }) },
      { a: card({ timesReviewed: 1, intervalDays: 1, nextReviewDate: '2026-10-01', lapses: 2 }) }
    );
    expect(m.a.nextReviewDate).toBe('2026-10-10');
    expect(m.a.lapses).toBe(2);
  });
});

describe('learning tree (LearningTreeTest)', () => {
  it('opens finished lessons and the next one per tier', () => {
    expect(openLessons([false, false, false], 6)).toEqual([true, false, false]);
    expect(openLessons([true, false, false], 6)).toEqual([true, true, false]);
    expect(openLessons([true, true, false, false], 2)).toEqual([true, true, true, false]);
    expect(openLessons([false, false, true], 2)).toEqual([true, false, true]);
  });

  const snapshot = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'public', 'content', 'vocabulary.json'), 'utf8'));
  const words: WordContent[] = snapshot.words;
  const corpus = new Corpus(words, {});

  it('builds sections from the real corpus, first one open, the rest gated', () => {
    const tree = buildTree(corpus, {});
    expect(tree.length).toBeGreaterThan(3);
    expect(tree[0].isUnlocked).toBe(true);
    expect(tree[1].isUnlocked).toBe(false);
    expect(tree.every((s) => s.wordCount >= 35)).toBe(true);
    expect(tree[tree.length - 1].definition.id).toBe('araw_araw');
    const first = tree[0].nodes[0];
    expect(first.isCurrent && first.isUnlocked).toBe(true);
    expect(tree[0].nodes[1].isUnlocked).toBe(false);
  });

  it('teaches nouns before verbs and slices lessons of seven', () => {
    const ref = nextLesson(corpus, {})!;
    const lesson = wordsFor(corpus, ref);
    expect(lesson.length).toBe(7);
    const ordered = teachingOrder(lesson);
    expect(ordered.map((w) => w.id)).toEqual(lesson.map((w) => w.id));
  });

  it('builds a full run of exercises for a lesson', () => {
    const ref = nextLesson(corpus, {})!;
    const ex = buildExercises(corpus, wordsFor(corpus, ref));
    expect(ex.length).toBeGreaterThanOrEqual(9);
    for (const e of ex) {
      if (e.type === 'match' || e.type === 'type') continue;
      expect(e.options).toContain(e.answer);
    }
  });
});

describe('learner actions', () => {
  it('onboarding preferences grant no XP and no practice-day streak', () => {
    const d = new Draft(initialLearner(), TODAY);
    d.completeOnboarding('Ana', 3, 50, 'Kasiguranin Apprentice');
    expect(d.d.progress.totalXp).toBe(0);
    expect(d.d.progress.currentStreak).toBe(0);
    expect(d.d.progress.isOnboardingCompleted).toBe(true);
    expect(d.d.progress.xpPolicyVersion).toBe(2);
  });
  it('a lesson pays 20, or 25 when perfect, and a same-day repeat pays nothing more', () => {
    const d = new Draft(initialLearner(), TODAY);
    expect(d.completeLesson({ unitId: 'theme:pamilya', lessonIndex: 0 }, 1)).toBe(25);
    expect(d.completeLesson({ unitId: 'theme:pamilya', lessonIndex: 1 }, 0.8)).toBe(20);
    expect(d.completeLesson({ unitId: 'theme:pamilya', lessonIndex: 1 }, 0.8)).toBe(0);
    // 45 activity XP plus the Lesson Pathfinder Beginner bonus of 20.
    expect(d.d.progress.totalXp).toBe(65);
    expect(d.d.progress.level).toBe(calculateLevel(65));
    expect(d.d.progress.totalXp).toBe(d.d.progress.activityXp + d.d.progress.badgeBonusXp);
    expect(d.events.some((e) => e.type === 'reward' && e.badgeIds.includes('badge:lesson_pathfinder:1'))).toBe(true);
  });
  it('the streak advances only when review and three games are done', () => {
    const d = new Draft(initialLearner(), TODAY);
    d.d.progress.currentStreak = 2;
    d.d.progress.lastActiveDate = '2026-09-29';
    d.recordDailyReviewCompleted();
    d.incrementGamesPlayed();
    d.incrementGamesPlayed();
    expect(d.d.progress.currentStreak).toBe(2);
    d.incrementGamesPlayed();
    expect(d.d.progress.currentStreak).toBe(3);
    expect(d.events).toContainEqual({ type: 'streak', days: 3 });
    expect(d.d.achievements['badge:consistent_learner:2'].isUnlocked).toBe(true);
    expect(d.d.achievements['badge:consistent_learner:3']?.isUnlocked ?? false).toBe(false);
  });
  it('a game level earning a star opens the next one', () => {
    const d = new Draft(initialLearner(), TODAY);
    d.saveLevelResult('word_match', 1, 2);
    expect(d.level('word_match', 2).isUnlocked).toBe(true);
    expect(d.level('word_match', 1).starsEarned).toBe(2);
  });
});

describe('dates', () => {
  it('ISO week ids', () => {
    expect(isoWeekId(new Date(2026, 8, 30))).toBe('2026-W40');
    expect(isoWeekId(new Date(2027, 0, 1))).toBe('2026-W53');
  });
});

describe('word games', () => {
  const snapshot = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'public', 'content', 'vocabulary.json'), 'utf8'));
  const corpus = new Corpus(snapshot.words, {});
  it('word search hides every chosen word in the grid', () => {
    const board = generateWordSearch(corpus.inCategory('Food & Dining'), 1)!;
    expect(board).not.toBeNull();
    for (const p of board.placements) {
      const letters = p.cells.map(([r, c]) => board.grid[r][c]).join('');
      expect(letters).toBe(p.letters);
    }
  });
  it('word wheel builds a crossword from one wheel of letters', () => {
    const puzzle = generateWordWheel(corpus.all, 1);
    expect(puzzle).not.toBeNull();
    expect(puzzle!.slots.length).toBeGreaterThanOrEqual(4);
    const built = Array.from({ length: 30 }, (_, i) => generateWordWheel(corpus.all, i + 1)).filter(Boolean).length;
    expect(built).toBe(30);
  });
});

describe('KotlinRandom matches kotlin.random.Random (reference values from kotlin-stdlib 2.0.21)', () => {
  const expected: [bigint, string][] = [
    [42n, '972016666 1740578880 -408207414 1 6 4 7 7 1 B,E,C,D,A,F'],
    [-1234567n, '1393624988 -803218564 -72168943 3 2 4 1 1 5 C,A,F,D,B,E'],
    [220n, '61862175 248380991 -1687762763 3 2 2 6 2 6 F,D,E,C,A,B'],
    [1n << 40n, '-54035998 1009676864 -1982007069 5 4 6 4 7 7 B,A,F,E,C,D'],
  ];
  it.each(expected)('seed %s', (seed, want) => {
    const r = new KotlinRandom(seed);
    const out: (number | string)[] = [];
    for (let i = 0; i < 3; i++) out.push(r.nextInt());
    for (let i = 0; i < 3; i++) out.push(r.nextIntUntil(7));
    for (let i = 0; i < 3; i++) out.push(r.nextIntUntil(8));
    out.push(new KotlinRandom(seed).shuffled(['A', 'B', 'C', 'D', 'E', 'F']).join(','));
    expect(out.join(' ')).toBe(want);
  });
});
