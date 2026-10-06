/**
 * XP policy 2: ports of RewardLedgerTest, RewardReceiptCodecTest, NormalizedProgressSyncTest,
 * ProfilePresentationTest, PublicProfileDtoTest, LevelConsistencyTest, XpSummaryTest and
 * BadgeSummaryTest, plus the web learner's normalization and grants. Receipts sync with Android, so
 * these must agree with the Kotlin exactly.
 */
import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import {
  decodeReceipt,
  encodeReceipt,
  gameXp,
  lastSevenDays,
  lessonXp,
  LEVEL_THRESHOLDS,
  levelFor,
  mergeRecord,
  receiptDocumentId,
  totals,
  xpBySource,
  type RewardRecord,
} from '../src/domain/xp';
import {
  BADGE_FAMILIES,
  BADGE_ROWS,
  BADGE_TIERS,
  backgroundUnlocked,
  badgeSummaries,
  chooseShowcase,
  familyFor,
  findBackground,
  nextUpBadge,
  PROFILE_BACKGROUNDS,
  showcaseCandidates,
} from '../src/domain/badges';
import { GAME_LEVEL_COUNT } from '../src/domain/gamification';
import type { AchievementState } from '../src/domain/types';
import { fromMainDoc, MAIN_PROGRESS_KEYS, mergeLegacyAchievements, mergeProgress, toMainDoc } from '../src/domain/merge';
import { Draft, initialLearner, storyUnlocked, weeklyXp } from '../src/domain/learner';
import { initialProgress, type UserProgress } from '../src/domain/types';
import { parsePublicProfile, publicDisplayName } from '../src/domain/publicProfile';
import { ReviewRating } from '../src/domain/sm2';
import { levelProgress, levelTitle, xpToNextLevel } from '../src/domain/constants';

const r = (
  id: string,
  kind: RewardRecord['kind'],
  o: Partial<RewardRecord> & { xp: number }
): RewardRecord => ({ id, kind, source: 'game:word_match#1', day: '2026-10-01', value: 1, imported: false, ...o });

describe('RewardLedgerTest', () => {
  it('rewards follow difficulty and cap without using speed', () => {
    expect(gameXp('word_match', 5, 5, true)).toBe(20);
    expect(gameXp('audio_quiz', 5, 5, true)).toBe(25);
    expect(gameXp('sentence_order', 15, 15, true)).toBe(40);
    expect(gameXp('word_search', 5, 5, true)).toBe(15);
    expect(lessonXp(false)).toBe(20);
    expect(lessonXp(true)).toBe(25);
  });
  it('a score measured in XP cannot be used as correctness', () => {
    expect(() => gameXp('sentence_order', 50, 5, true)).toThrow();
  });
  it('replay and improvement are compared without double payment', () => {
    const rows = [
      r('first', 'game', { xp: 13 }),
      r('better', 'game', { day: '2026-10-02', xp: 20 }),
      r('replay', 'replay', { day: '2026-10-02', xp: 5 }),
    ];
    expect(totals(rows).activity).toBe(20);
    expect(totals(rows).byDay['2026-10-02']).toBe(7);
    expect(totals([...rows, r('third', 'replay', { day: '2026-10-03', xp: 5 })]).activity).toBe(25);
  });
  it('imported completion does not count toward daily goals', () => {
    const t = totals([r('old', 'game', { day: '', xp: 5, imported: true }), r('new', 'game', { xp: 20 }), r('repeat', 'replay', { xp: 5 })]);
    expect(t.activity).toBe(20);
    expect(t.byDay['2026-10-01']).toBe(15);
  });
  it('the review cap applies to the combined evidence of both devices', () => {
    const rows = Array.from({ length: 40 }, (_, i) => r(`review:${i}`, 'review', { source: `word:${i}`, xp: 3 }));
    expect(totals(rows).activity).toBe(60);
    expect(totals(rows).byDay['2026-10-01']).toBe(60);
  });
  it('badge bonuses affect level but not daily activity', () => {
    const t = totals([r('lesson', 'lesson', { xp: 20 }), r('badge', 'badge', { xp: 120 })]);
    expect(t.total).toBe(140);
    expect(t.byDay['2026-10-01']).toBe(20);
    expect(levelFor(t.total)).toBe(2);
  });
  it('receipt merge is idempotent, commutative and keeps the best evidence', () => {
    const a = r('one', 'game', { xp: 13 });
    const b = { ...a, xp: 20 };
    expect(mergeRecord(a, b)).toEqual(b);
    expect(mergeRecord(a, b)).toEqual(mergeRecord(b, a));
    expect(mergeRecord(b, b)).toEqual(b);
  });
  it('single lifetime milestones use the earliest day and imported history stays imported', () => {
    const a = r('mastery', 'mastery', { day: '2026-09-30', xp: 5, imported: true });
    const merged = mergeRecord(a, { ...a, day: '2026-10-01', imported: false });
    expect(merged.day).toBe('2026-09-30');
    expect(merged.imported).toBe(true);
    expect(totals([merged]).byDay).toEqual({});
  });
  it('all thirty level boundaries agree with display and storage', () => {
    expect(LEVEL_THRESHOLDS).toHaveLength(30);
    expect(LEVEL_THRESHOLDS[LEVEL_THRESHOLDS.length - 1]).toBe(15620);
    LEVEL_THRESHOLDS.forEach((xp, i) => {
      expect(levelFor(xp)).toBe(i + 1);
      if (i > 0) expect(levelFor(xp - 1)).toBe(i);
    });
  });
  it('the threshold table is the one the JVM computes', () => {
    // From XpPolicy.thresholds, printed by the JDK (floor(100 * (n - 1)^1.5 / 10 + 0.5) * 10).
    expect(LEVEL_THRESHOLDS.join(',')).toBe(
      '0,100,280,520,800,1120,1470,1850,2260,2700,3160,3650,4160,4690,5240,5810,6400,7010,7640,8280,8940,9620,10320,11030,11760,12500,13260,14030,14820,15620'
    );
  });
  it('all eleven families have six strictly increasing milestones and stable identities', () => {
    expect(BADGE_FAMILIES).toHaveLength(11);
    expect(new Set(BADGE_ROWS.map((x) => x.id)).size).toBe(66);
    for (const f of BADGE_FAMILIES) {
      expect(f.thresholds).toHaveLength(6);
      expect(f.thresholds.every((v, i) => i === 0 || v > f.thresholds[i - 1])).toBe(true);
    }
    expect(BADGE_ROWS.filter((x) => x.family.metric === 'level').every((x) => x.xpReward === 0)).toBe(true);
    expect(familyFor('badge:word_explorer:7')).toBeUndefined();
  });
});

describe('LevelConsistencyTest', () => {
  it('levels cap at thirty for high XP, with no next target', () => {
    for (const xp of [15620, 100000, Number.MAX_SAFE_INTEGER]) expect(levelFor(xp)).toBe(30);
    expect(xpToNextLevel(15620)).toBeNull();
    expect(levelProgress(20000)).toBe(1);
    expect(levelTitle(31)).toBe('Level 30');
    expect(xpToNextLevel(0)).toBe(100);
  });
});

describe('RewardReceiptCodecTest', () => {
  it('stable ids with spaces and slashes are safe Firestore document ids, same hash as the JVM', async () => {
    const hash = await receiptDocumentId('review:word:example/entry:2026-10-01');
    expect(hash).toHaveLength(64);
    expect(hash).not.toContain('/');
    // MessageDigest SHA-256 of the same id, printed by the JDK.
    expect(hash).toBe('3adc9c4f2a78921e52618b9a9ca0cbff1596238d55b216dac8956f674d150c2b');
  });
  it('receipts round-trip with every field', () => {
    const row: RewardRecord = { id: 'game:word_match#1:2026-10-01', kind: 'game', source: 'game:word_match#1', day: '2026-10-01', xp: 20, value: 1, imported: false };
    expect(decodeReceipt(encodeReceipt(row))).toEqual(row);
  });
  it('malformed and unknown receipts cannot mint XP', () => {
    const row: RewardRecord = { id: 'review:word:x:2026-10-01', kind: 'review', source: 'word:x', day: '2026-10-01', xp: 3, value: 1, imported: false };
    const payload = encodeReceipt(row);
    expect(decodeReceipt(payload)).not.toBeNull();
    expect(decodeReceipt({ ...payload, xp: 200 })).toBeNull();
    expect(decodeReceipt({ ...payload, day: '2026-02-30' })).toBeNull();
    expect(decodeReceipt({ ...payload, kind: 'welcome' })).toBeNull();
    expect(decodeReceipt({ ...payload, policyVersion: 1 })).toBeNull();
    expect(decodeReceipt({ ...payload, id: 'review:word:y:2026-10-01' })).toBeNull();
  });
  it('badge receipts must carry exactly their tier bonus', () => {
    const badge = (xp: number) => ({ policyVersion: 2, id: 'badge:story_reader:3', kind: 'badge', source: 'badge:story_reader:3', day: '2026-10-01', xp, value: 1, imported: false });
    expect(decodeReceipt(badge(80))).not.toBeNull();
    expect(decodeReceipt(badge(200))).toBeNull();
    const rank = { ...badge(0), id: 'badge:journey_rank:2', source: 'badge:journey_rank:2' };
    expect(decodeReceipt(rank)).not.toBeNull();
  });
});

describe('NormalizedProgressSyncTest', () => {
  it('a fresh install restores an earned legacy badge with its original date', () => {
    const restored = mergeLegacyAchievements({}, { first_word: { isUnlocked: true, currentValue: 1, unlockedDate: '2025-01-05' } });
    expect(Object.keys(restored)).toEqual(['first_word']);
    expect(restored.first_word.unlockedDate).toBe('2025-01-05');
  });
  it('cloud badge flags cannot grant active tiers or create unearned legacy rows', () => {
    const restored = mergeLegacyAchievements(
      {},
      {
        'badge:word_explorer:6': { isUnlocked: true, currentValue: 800, unlockedDate: '2025-01-05' },
        ten_words: { isUnlocked: false, currentValue: 4, unlockedDate: null },
      }
    );
    expect(restored).toEqual({});
  });
  it('legacy restore keeps the earliest earned date', () => {
    const restored = mergeLegacyAchievements(
      { custom_badge: { isUnlocked: true, currentValue: 1, unlockedDate: '2025-01-05' } },
      {
        custom_badge: { isUnlocked: true, currentValue: 5, unlockedDate: '2025-02-05' },
        older_unknown_badge: { isUnlocked: true, currentValue: 1, unlockedDate: '2025-03-05' },
      }
    );
    expect(restored.custom_badge.unlockedDate).toBe('2025-01-05');
    expect(restored.older_unknown_badge.unlockedDate).toBe('2025-03-05');
  });
  it('newer legacy cloud totals cannot undo normalization', () => {
    const local: UserProgress = { ...initialProgress(), totalXp: 140, level: 2, xpPolicyVersion: 2, activityXp: 20, badgeBonusXp: 120, dailyXpDate: '2026-10-01', dailyXpEarned: 20 };
    const remote: UserProgress = { ...initialProgress(), totalXp: 50000, level: 10, updatedAt: 9999999999999, dailyXpDate: '2026-10-01', dailyXpEarned: 2000 };
    const merged = mergeProgress(local, remote, '2026-10-01');
    expect(merged.totalXp).toBe(140);
    expect(merged.level).toBe(2);
    expect(merged.dailyXpEarned).toBe(20);
    expect(merged.xpPolicyVersion).toBe(2);
  });
  it('normalized fields round-trip, including profile pins and scenery', () => {
    const row: UserProgress = { ...initialProgress(), xpPolicyVersion: 2, activityXp: 60, badgeBonusXp: 40, pinnedBadgeIds: 'word_explorer,lesson_pathfinder', profileBackgroundId: 'river' };
    const back = fromMainDoc(toMainDoc(row, 1));
    expect([back.xpPolicyVersion, back.activityXp, back.badgeBonusXp, back.pinnedBadgeIds, back.profileBackgroundId]).toEqual([2, 60, 40, 'word_explorer,lesson_pathfinder', 'river']);
  });
  it('a blank or unknown background is written as forest, which the rules accept', () => {
    expect(toMainDoc({ ...initialProgress(), profileBackgroundId: '' }, 1).profileBackgroundId).toBe('forest');
  });
  it('main progress carries exactly the keys firestore.rules allows', () => {
    const rules = fs.readFileSync(path.resolve(__dirname, '../../../firestore.rules'), 'utf8');
    const block = rules.slice(rules.indexOf('function isValidMainProgress'));
    const list = block.slice(block.indexOf('hasOnly(['), block.indexOf('])'));
    const allowed = [...list.matchAll(/'([A-Za-z]+)'/g)].map((m) => m[1]).sort();
    expect([...MAIN_PROGRESS_KEYS].sort()).toEqual(allowed);
  });
});

describe('ProfilePresentationTest', () => {
  it('backgrounds unlock on the exact level or permanent streak milestone', () => {
    expect(PROFILE_BACKGROUNDS.filter((b) => backgroundUnlocked(b, 1, 0))).toHaveLength(2);
    expect(backgroundUnlocked(findBackground('farm')!, 1, 0)).toBe(false);
    expect(backgroundUnlocked(findBackground('farm')!, 2, 0)).toBe(true);
    const lighthouse = findBackground('ontok_lighthouse')!;
    expect(backgroundUnlocked(lighthouse, 4, 6)).toBe(false);
    expect(backgroundUnlocked(lighthouse, 5, 0)).toBe(true);
    expect(backgroundUnlocked(lighthouse, 1, 7)).toBe(true);
    const tidal = findBackground('tibu_tidal_pool')!;
    expect(backgroundUnlocked(tidal, 30, 29)).toBe(false);
    expect(backgroundUnlocked(tidal, 1, 30)).toBe(true);
  });
  it('the showcase prefers tier, then rarity, and shows one badge per family', () => {
    const ids = ['badge:word_explorer:1', 'badge:word_explorer:6', 'badge:review_keeper:6', 'badge:lesson_pathfinder:6', 'badge:consistent_learner:6', 'badge:precision_player:5'];
    const rarity = { 'badge:word_explorer:6': 10, 'badge:review_keeper:6': 5, 'badge:lesson_pathfinder:6': 1, 'badge:consistent_learner:6': 20, 'badge:precision_player:5': 0 };
    expect(chooseShowcase(ids, rarity).map((x) => x.id)).toEqual(['badge:lesson_pathfinder:6', 'badge:review_keeper:6', 'badge:word_explorer:6']);
    expect(showcaseCandidates(ids)).toHaveLength(5);
  });
});

describe('PublicProfileDtoTest', () => {
  it('private fields and unknown achievements cannot round-trip', () => {
    const result = parsePublicProfile({
      displayName: 'Learner name', email: 'private@example.test', fullName: 'Private name', age: 20,
      address: 'Private address', profileBackgroundId: 'invalid', level: 900,
      badgeIds: ['badge:word_explorer:1', 'private@example.test'], sections: { pagbati: 2, 'private@example.test': 3 },
    }) as unknown as Record<string, unknown>;
    expect(result.profileBackgroundId).toBe('forest');
    expect(result.level).toBe(30);
    expect(result.badgeIds).toEqual(['badge:word_explorer:1']);
    expect(result.sections).toEqual({ pagbati: 2 });
    for (const k of ['email', 'fullName', 'age', 'address']) expect(k in result).toBe(false);
    expect(JSON.stringify(result)).not.toContain('private@example.test');
  });
  it('an email is never used as the public display name', () => {
    expect(publicDisplayName('account@example.test')).toBe('Learner');
    expect(publicDisplayName('  ')).toBe('Learner');
    expect(publicDisplayName('  Kiko  ')).toBe('Kiko');
  });
  it('a learner who never set a nickname is ranked by full name', () => {
    expect(publicDisplayName('Learner', 'Ana Cruz')).toBe('Ana Cruz');
    expect(publicDisplayName('', '  Ana Cruz ')).toBe('Ana Cruz');
    expect(publicDisplayName('Kasiguranin Learner', 'vre')).toBe('vre');
    expect(publicDisplayName('Kiko', 'Francisco Reyes')).toBe('Kiko');
    expect(publicDisplayName('Learner', 'ana@example.test')).toBe('Learner');
    expect(publicDisplayName('Learner', '')).toBe('Learner');
  });
});

describe('the learner under policy 2', () => {
  const TODAY = '2026-10-01';
  const content = { words: [], stories: [{ id: 1, requiredXp: 0 }, { id: 2, requiredXp: 300 }, { id: 3, requiredXp: 900 }] };

  it('normalizes old progress once: provable history imported, the rest archived', () => {
    const data = initialLearner();
    Object.assign(data.progress, { totalXp: 640, level: 4, isOnboardingCompleted: true, dailyGoalXp: 150 });
    data.lessons['theme:pamilya#0'] = { isComplete: true, bestAccuracy: 1, timesCompleted: 2, lastCompletedAt: 1 };
    data.lessons['theme:pamilya#1'] = { isComplete: true, bestAccuracy: 0.6, timesCompleted: 1, lastCompletedAt: 1 };
    data.gameLevels['word_match_1'] = { starsEarned: 2, isUnlocked: true };
    data.gameLevels['word_search_food_dining_3'] = { starsEarned: 1, isUnlocked: true };
    data.stories['1'] = { isCompleted: true, currentPage: 4 };
    const d = new Draft(data, TODAY, content);
    d.ensureNormalized();
    const p = d.d.progress;
    // 25 + 20 (lessons) + 5 + 5 (levels) + 20 (story) = 75 activity; badges: lesson, game, mode (2
    // modes -> Learner too), story = 20 + 20 + 20 + 40 + 20 = 120.
    expect(p.activityXp).toBe(75);
    expect(p.badgeBonusXp).toBe(120);
    expect(p.totalXp).toBe(195);
    expect(p.level).toBe(2);
    expect(p.xpPolicyVersion).toBe(2);
    expect(p.dailyXpEarned).toBe(0);
    expect(p.dailyGoalXp).toBe(80);
    expect(d.d.normalization).toMatchObject({ originalXp: 640, originalLevel: 4, normalizedXp: 195, acknowledged: false });
    // Stories the old XP had opened stay open.
    expect(storyUnlocked(d.d, { id: 2, requiredXp: 300 })).toBe(true);
    expect(storyUnlocked(d.d, { id: 3, requiredXp: 900 })).toBe(false);
    // Imported history never celebrates.
    expect(d.events).toEqual([]);
    const again = structuredClone(d.d);
    d.ensureNormalized();
    expect(d.d).toEqual(again);
  });

  it('a perfect game pays its weighted reward once, a replay a quarter of it the next day', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    expect(d.finishGame({ mode: 'word_match', level: 1, correct: 5, total: 5, stars: 3, perfect: true })).toBe(20);
    expect(d.finishGame({ mode: 'word_match', level: 1, correct: 5, total: 5, stars: 3, perfect: true })).toBe(0);
    expect(d.level('word_match', 2).isUnlocked).toBe(true);
    const next = new Draft(d.d, '2026-10-02', content);
    expect(next.finishGame({ mode: 'word_match', level: 1, correct: 5, total: 5, stars: 3, perfect: true })).toBe(5);
    expect(next.d.receipts['perfect:game:word_match#1']).toBeDefined();
    expect(next.d.achievements['badge:precision_player:1'].isUnlocked).toBe(true);
  });

  it('a round under one star earns nothing and opens nothing', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    expect(d.finishGame({ mode: 'word_match', level: 1, correct: 1, total: 5, stars: 0, perfect: false })).toBe(0);
    expect(d.level('word_match', 2).isUnlocked).toBe(false);
    expect(d.d.progress.gamesPlayed).toBe(1);
  });

  it('only scheduled reviews pay, and mastery needs three successful days', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    expect(d.reviewWord({ kasiguranin: 'aldew' }, ReviewRating.GOOD, false).xp).toBe(0);
    expect(d.reviewWord({ kasiguranin: 'aldew' }, ReviewRating.HARD, true).xp).toBe(2);
    // The same word again today: the review receipt keeps its first rating's XP.
    expect(d.reviewWord({ kasiguranin: 'aldew' }, ReviewRating.EASY, true).xp).toBe(0);
    for (const day of ['2026-10-05', '2026-10-20']) {
      const later = new Draft(d.d, day, content);
      later.review('aldew', ReviewRating.GOOD, false, true);
      d.d = later.d;
    }
    expect(d.d.receipts['mastery:word:aldew']).toMatchObject({ xp: 5, day: '2026-10-20' });
  });

  it('a story pays 20 the first time and nothing on a reread', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    expect(d.completeStory(1)).toBe(20);
    expect(d.completeStory(1)).toBe(0);
    expect(d.d.stories['1'].isCompleted).toBe(true);
    expect(d.d.achievements['badge:story_reader:1'].isUnlocked).toBe(true);
  });

  it('approved contributions pay once; an older approval is imported and outside today', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    d.approved([{ id: 'word:abc', approvedDay: TODAY }, { id: 'literature:xyz', approvedDay: '2026-09-01' }]);
    d.approved([{ id: 'word:abc', approvedDay: TODAY }]);
    expect(d.d.progress.activityXp).toBe(20);
    expect(d.d.receipts['approved:literature:xyz'].imported).toBe(true);
    expect(weeklyXp(d.d, TODAY)).toBe(10);
  });

  it('receipts from another device settle without celebrating, and tiers they prove stay earned', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    d.mergeRewards([
      { id: 'story:1', kind: 'story', source: 'story:1', day: '2026-09-29', xp: 20, value: 1, imported: false },
      { id: 'badge:story_reader:1', kind: 'badge', source: 'badge:story_reader:1', day: '2026-09-29', xp: 20, value: 1, imported: false },
      { id: 'bogus', kind: 'story', source: 'story:2', day: '', xp: 9000, value: 1, imported: false },
    ]);
    expect(d.d.progress.totalXp).toBe(40);
    expect(d.d.achievements['badge:story_reader:1'].isUnlocked).toBe(true);
    expect(d.d.stories['1'].isCompleted).toBe(true);
    expect(d.events).toEqual([]);
  });

  it('pins at most three earned families, in order', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    d.completeStory(1);
    d.pin('word_explorer');
    expect(d.d.progress.pinnedBadgeIds).toBe('');
    d.pin('story_reader');
    expect(d.d.progress.pinnedBadgeIds).toBe('story_reader');
    d.pin('story_reader');
    expect(d.d.progress.pinnedBadgeIds).toBe('');
  });

  it('a locked background cannot be chosen', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    expect(() => d.selectBackground('river')).toThrow();
    d.selectBackground('casapsapan');
    expect(d.d.progress.profileBackgroundId).toBe('casapsapan');
  });

  it('a legacy cloud document is archived, never restored as XP', () => {
    const d = new Draft(initialLearner(), TODAY, content);
    d.ensureNormalized();
    d.archiveLegacy({ ...initialProgress(), totalXp: 5000, level: 10 });
    expect(d.d.normalization).toMatchObject({ originalXp: 5000, originalLevel: 10, acknowledged: false });
    expect(d.d.progress.totalXp).toBe(0);
    expect(storyUnlocked(d.d, { id: 3, requiredXp: 900 })).toBe(true);
  });
});

describe('XpSummaryTest', () => {
  const today = '2026-10-05';
  const day = (back: number) => `2026-10-0${5 - back}`;
  const records = [
    r(`lesson:a:${day(1)}`, 'lesson', { source: 'lesson:a', day: day(1), xp: 25 }),
    r(`replay:lesson:a:${day(0)}`, 'replay', { source: 'lesson:a', day: day(0), xp: 5 }),
    r(`game:word_match:1:${day(0)}`, 'game', { source: 'game:word_match:1', day: day(0), xp: 20 }),
    r(`replay:game:word_match:1:${day(0)}`, 'replay', { source: 'game:word_match:1', day: day(0), xp: 5 }),
    // Reviews cap at 60 XP a day.
    r(`review:x:${day(0)}`, 'review', { source: 'x', day: day(0), xp: 40 }),
    r(`review:y:${day(0)}`, 'review', { source: 'y', day: day(0), xp: 40 }),
    r('badge:word_explorer:1', 'badge', { source: 'badge:word_explorer:1', day: day(0), xp: 20 }),
    r('perfect:game:word_match:1', 'perfect', { source: 'game:word_match:1', day: '', xp: 0 }),
  ];

  it('the parts add up to the ledger total', () => {
    expect(Object.values(xpBySource(records)).reduce((s, xp) => s + xp, 0)).toBe(totals(records).total);
  });
  it('each source is settled the way the ledger settles it', () => {
    const parts = xpBySource(records);
    expect(parts.Lessons).toBe(30); // 25 for the lesson, then a 5 XP replay the next day
    expect(parts.Games).toBe(20); // a same-day replay settles to max(improvement, replay)
    expect(parts.Reviews).toBe(60); // reviews cap at 60 a day
    expect(parts.Badges).toBe(20);
  });
  it('sources with no XP are left out', () => {
    expect(Object.keys(xpBySource(records)).sort()).toEqual(['Badges', 'Games', 'Lessons', 'Reviews']);
  });
  it('the week runs oldest first and ends today, without badge bonuses', () => {
    const week = lastSevenDays(records, today);
    expect(week).toHaveLength(7);
    expect(week[6].day).toBe(today);
    expect(week[0].day).toBe('2026-09-29');
    expect(week[5].xp).toBe(25);
    expect(week[6].xp).toBe(5 + 20 + 60);
  });
  it('game stars are out of every level Android seeds', () => {
    // Seven games and twelve Word Search categories, 30 levels each (DatabaseSeeder).
    expect(GAME_LEVEL_COUNT).toBe(570);
  });
});

describe('BadgeSummaryTest', () => {
  /** The catalog with the first `tiers` of `familyId` earned and `current` counted toward the next. */
  const rows = (familyId: string, tiers: number, current = 0): Record<string, AchievementState> =>
    Object.fromEntries(
      BADGE_ROWS.filter((row) => row.family.id === familyId).map((row) => [
        row.id,
        { isUnlocked: row.tier.index + 1 <= tiers, currentValue: current, unlockedDate: null },
      ])
    );

  it('every family appears once, earned or not', () => {
    const summaries = badgeSummaries({});
    expect(summaries.map((s) => s.family.id)).toEqual(BADGE_FAMILIES.map((f) => f.id));
    summaries.forEach((s) => expect(s.highest).toBeUndefined());
  });
  it('a family shows its highest earned tier and the next one', () => {
    const wordExplorer = badgeSummaries(rows('word_explorer', 3, 38)).find((s) => s.family.id === 'word_explorer')!;
    expect(wordExplorer.highest).toBe(BADGE_TIERS[2]);
    expect(wordExplorer.earnedTiers).toBe(3);
    expect(wordExplorer.next?.row.tier).toBe(BADGE_TIERS[3]);
    expect(wordExplorer.progress).toBeCloseTo(38 / 150, 4);
  });
  it('a finished family has nothing next', () => {
    const done = badgeSummaries(rows('story_reader', 6)).find((s) => s.family.id === 'story_reader')!;
    expect(done.highest).toBe(BADGE_TIERS[5]);
    expect(done.next).toBeUndefined();
    expect(done.progress).toBe(1);
  });
  it('pinned families lead, the rest keep catalog order', () => {
    const ids = badgeSummaries({}, ['story_reader', '', 'mode_explorer']).map((s) => s.family.id);
    expect(ids.slice(0, 2)).toEqual(['story_reader', 'mode_explorer']);
    expect(ids.slice(2)).toEqual(BADGE_FAMILIES.map((f) => f.id).filter((id) => id !== 'story_reader' && id !== 'mode_explorer'));
  });
  it('next up is the unfinished tier closest to done', () => {
    const achievements: Record<string, AchievementState> = {
      'badge:review_keeper:1': { isUnlocked: false, currentValue: 9, unlockedDate: null }, // 9 of 10
      'badge:word_explorer:1': { isUnlocked: false, currentValue: 0, unlockedDate: null },
    };
    expect(nextUpBadge(badgeSummaries(achievements))?.family.id).toBe('review_keeper');
  });
});
