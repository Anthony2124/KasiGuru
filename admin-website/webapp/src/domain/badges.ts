/**
 * The 11 badge families with six permanent tiers each, the archive of the original achievements,
 * and profile scenery. Port of BadgeCatalog.kt, LegacyBadgeCatalog.kt, BadgeShowcase.kt and
 * ProfileBackgroundCatalog.kt. Ids are shared with Android through sync and public profiles:
 * never rename one.
 */

import { plural } from './plural';

export const BADGE_PREFIX = 'badge:';

export interface BadgeTier {
  index: number;
  label: string;
  bonus: number;
}

export const BADGE_TIERS: readonly BadgeTier[] = [
  { index: 0, label: 'Beginner', bonus: 20 },
  { index: 1, label: 'Learner', bonus: 40 },
  { index: 2, label: 'Achiever', bonus: 80 },
  { index: 3, label: 'Expert', bonus: 120 },
  { index: 4, label: 'Master', bonus: 160 },
  { index: 5, label: 'Legend', bonus: 200 },
];

export type BadgeSection = 'Learning' | 'Practice' | 'Games' | 'Community';

export interface BadgeFamily {
  id: string;
  name: string;
  metric: string;
  /** What the metric counts, singular ("day"); `units` is its plural. */
  unit: string;
  units: string;
  thresholds: readonly number[];
  section: BadgeSection;
  action: string;
  bonus: boolean;
}

const family = (
  id: string,
  name: string,
  metric: string,
  unit: string,
  thresholds: number[],
  section: BadgeSection,
  action: string,
  bonus = true,
  units = `${unit}s`
): BadgeFamily => ({ id, name, metric, unit, units, thresholds, section, action, bonus });

/** A count in this family's unit: "1 day", "30 days". The level family reads as a rank: "Level 6". */
export const badgeAmount = (f: BadgeFamily, n: number) => (f.metric === 'level' ? `Level ${n}` : plural(n, f.unit, f.units));

/** How far toward `target`: "0 / 1 day", "12 / 30 days", "Level 3 of 6". */
export const badgeProgress = (f: BadgeFamily, value: number, target: number) =>
  f.metric === 'level' ? `Level ${value} of ${target}` : `${value} / ${badgeAmount(f, target)}`;

/** Stable identities; thresholds count distinct evidence, never screen visits. */
export const BADGE_FAMILIES: readonly BadgeFamily[] = [
  family('word_explorer', 'Word Explorer', 'verifiedWords', 'word', [1, 10, 50, 150, 400, 800], 'Learning', 'Review words'),
  family('lesson_pathfinder', 'Lesson Pathfinder', 'distinctLessons', 'lesson', [1, 5, 10, 30, 75, 150], 'Learning', 'Continue learning'),
  family('review_keeper', 'Review Keeper', 'scheduledReviews', 'review', [10, 20, 100, 300, 1000, 3000], 'Learning', 'Review words'),
  family('consistent_learner', 'Consistent Learner', 'streak', 'day', [1, 3, 7, 30, 90, 180], 'Practice', "View today's tasks"),
  family('game_adventurer', 'Game Adventurer', 'distinctGameLevels', 'level', [1, 5, 10, 50, 150, 400], 'Games', 'Play a game'),
  family('precision_player', 'Precision Player', 'perfectLevels', 'perfect level', [1, 3, 5, 20, 75, 150], 'Games', 'Play a game'),
  family('mode_explorer', 'Mode Explorer', 'gameModesPlayed', 'mode', [1, 2, 3, 4, 6, 8], 'Games', 'Explore games'),
  family('story_reader', 'Story Reader', 'storiesCompleted', 'story', [1, 2, 3, 4, 7, 10], 'Learning', 'Read a story', true, 'stories'),
  family('category_scholar', 'Category Scholar', 'verifiedCategories', 'category', [1, 2, 3, 6, 9, 12], 'Learning', 'Explore vocabulary', true, 'categories'),
  family('community_contributor', 'Community Contributor', 'submissionsApproved', 'approval', [1, 3, 5, 10, 25, 50], 'Community', 'Contribute a word'),
  family('journey_rank', 'Journey Rank', 'level', 'account level', [2, 3, 6, 10, 20, 30], 'Practice', 'Continue learning', false),
];

export const badgeId = (familyId: string, tierIndex: number) => `${BADGE_PREFIX}${familyId}:${tierIndex + 1}`;

export function tierFor(id: string): BadgeTier | undefined {
  const tail = id.slice(id.lastIndexOf(':') + 1);
  if (!/^-?\d+$/.test(tail)) return undefined;
  return BADGE_TIERS[Number(tail) - 1];
}

export function familyFor(id: string): BadgeFamily | undefined {
  const tier = tierFor(id);
  if (!tier) return undefined;
  return BADGE_FAMILIES.find((f) => id === badgeId(f.id, tier.index));
}

export interface BadgeRow {
  id: string;
  family: BadgeFamily;
  tier: BadgeTier;
  requiredValue: number;
  xpReward: number;
}

/** Every one of the 66 milestones, family by family, Beginner to Legend. */
export const BADGE_ROWS: readonly BadgeRow[] = BADGE_FAMILIES.flatMap((f) =>
  BADGE_TIERS.map((tier) => ({
    id: badgeId(f.id, tier.index),
    family: f,
    tier,
    requiredValue: f.thresholds[tier.index],
    xpReward: f.bonus ? tier.bonus : 0,
  }))
);

export const isBadgeId = (id: string) => id.startsWith(BADGE_PREFIX);

// ── Legacy (the catalogue before policy version 2) ──────────────────────────

const LEGACY: Record<string, [string, string]> = {
  level_1: ['Novice Explorer', 'Began your Kasiguranin learning journey'],
  level_2: ['Vocab Apprentice', 'Reached Level 2 & unlocked Fill in the Blank'],
  level_3: ['Linguistic Scholar', 'Reached Level 3 & unlocked Audio Listening Quiz'],
  level_4: ['Grammar Specialist', 'Reached Level 4 & unlocked Verb Aspect Builder'],
  level_5: ['Kasiguranin Legend', 'Reached Level 5 & unlocked Sentence Construction'],
  first_word: ['Unáng Salitâ', 'Learn your first Kasiguranin word'],
  ten_words: ['Sampûng Salitâ', 'Learn 10 Kasiguranin words'],
  fifty_words: ['Limampûng Salitâ', 'Learn 50 Kasiguranin words'],
  first_story: ['Mambábasa', 'Complete your first story'],
  first_game: ['Mánlalaro', 'Play your first mini-game'],
  perfect_game: ['Perpekto!', 'Get a perfect score in any mini-game'],
  three_day_streak: ['Tatlong Aldaw', 'Maintain a 3-day learning streak'],
  seven_day_streak: ['Isáng Linggo', 'Maintain a 7-day learning streak'],
  level_five: ['Sumusulong', 'Reach Level 5'],
  level_ten: ['Mæstro', 'Reach Level 10 — Master of Kasiguranin!'],
  all_stories: ['Tagapagsalaysay', 'Complete 3 stories'],
  first_contribution: ['First Contribution', 'Submit your first word, story, or poem'],
  trusted_voice: ['Trusted Voice', 'Have 5 submissions approved into the dictionary or Stories'],
  corpus_builder: ['Corpus Builder', 'Have 25 submissions approved into the dictionary or Stories'],
  moon_cycle: ['Moon Cycle', 'Maintain a 30-day learning streak'],
  centurion: ['Centurion', 'Maintain a 100-day learning streak'],
  category_master: ['Category Master', 'Learn every word in one dictionary category'],
  perfect_six: ['Perfect Six', 'Score perfectly in all six game modes in one sitting'],
  top_of_the_week: ['Top of the Week', 'Reach the top 10 of the weekly leaderboard'],
  six_for_six: ['Six for Six', 'Play every one of the six mini-game modes at least once'],
};

/** Archived display metadata, only for badges already earned. Grants no XP and no active tier. */
export function legacyDefinition(id: string): { name: string; description: string } {
  const d = LEGACY[id];
  return { name: d?.[0] ?? id, description: d?.[1] ?? 'Previously earned achievement' };
}

// ── Showcase ────────────────────────────────────────────────────────────────

/** The highest earned tier of each family. */
export function showcaseCandidates(ids: string[]): BadgeRow[] {
  const best = new Map<string, BadgeRow>();
  for (const row of BADGE_ROWS) {
    if (!ids.includes(row.id)) continue;
    const current = best.get(row.family.id);
    if (!current || row.tier.index > current.tier.index) best.set(row.family.id, row);
  }
  return [...best.values()];
}

/** Three badges: highest tier first, then the rarest among players, then by id. */
export function chooseShowcase(ids: string[], earnedByPlayers: Record<string, number>): BadgeRow[] {
  return showcaseCandidates(ids)
    .sort(
      (a, b) =>
        b.tier.index - a.tier.index ||
        (earnedByPlayers[a.id] ?? Number.MAX_SAFE_INTEGER) - (earnedByPlayers[b.id] ?? Number.MAX_SAFE_INTEGER) ||
        (a.id < b.id ? -1 : a.id > b.id ? 1 : 0)
    )
    .slice(0, 3);
}

// ── Profile scenery ─────────────────────────────────────────────────────────

export interface ProfileBackground {
  id: string;
  title: string;
  level: number;
  streak: number;
}

const NEVER = Number.MAX_SAFE_INTEGER;

export const PROFILE_BACKGROUNDS: readonly ProfileBackground[] = [
  { id: 'forest', title: 'Forest', level: 1, streak: 0 },
  { id: 'casapsapan', title: 'Casapsapan Beach', level: 1, streak: 0 },
  { id: 'farm', title: 'Casiguran Farm', level: 2, streak: 0 },
  { id: 'river', title: 'River', level: 3, streak: 0 },
  { id: 'ermita_hill', title: 'Ermita Hill', level: 4, streak: 0 },
  { id: 'ontok_lighthouse', title: 'Ontok Lighthouse', level: 5, streak: 7 },
  { id: 'tibu_tidal_pool', title: 'Tibu Tidal Pool', level: NEVER, streak: 30 },
];

export const DEFAULT_BACKGROUND = 'forest';

export const backgroundUnlocked = (b: ProfileBackground, accountLevel: number, longestStreak: number) =>
  accountLevel >= b.level || (b.streak > 0 && longestStreak >= b.streak);

export function backgroundRequirement(b: ProfileBackground): string {
  if (b.level === 1) return 'Free';
  if (b.level === NEVER) return `${b.streak}-day streak`;
  if (b.streak > 0) return `Level ${b.level} or ${b.streak}-day streak`;
  return `Level ${b.level}`;
}

export const findBackground = (id: string) => PROFILE_BACKGROUNDS.find((b) => b.id === id);
export const normalizeBackground = (id: string) => findBackground(id)?.id ?? DEFAULT_BACKGROUND;
