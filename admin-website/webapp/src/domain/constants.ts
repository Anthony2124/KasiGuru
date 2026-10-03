/** util/Constants.kt, the parts the learner app reads. */
import { LEVEL_THRESHOLDS, levelFor, MAX_LEVEL } from './xp';

// Values from Constants.kt under XP policy 2. Rewards themselves are granted by the reward ledger
// (./xp.ts); these remain for the few places that describe them.
export const XP_PER_WORD_LEARNED = 5;

/**
 * Constants.STORIES_ENABLED in the Android app. The stories are written but not yet narrated - there
 * is no voice actor - so while this is false every way into stories says they are coming soon. The
 * stories, their rewards and any progress are untouched; switching back is this one line.
 */
export const STORIES_ENABLED = false;
export const XP_PER_STORY_COMPLETE = 20;
export const XP_PER_GAME_CORRECT = 2;
export const XP_BONUS_PERFECT_GAME = 5;

export { LEVEL_THRESHOLDS, MAX_LEVEL } from './xp';

/** Must stay identical to CategoryRegistry on Android. */
export const CATEGORIES = [
  'Greetings & Essentials',
  'Body Parts & Health',
  'Animals & Wildlife',
  'Food & Dining',
  'Numbers & Time',
  'Weather & Climate',
  'Nature & Environment',
  'House & Daily Life',
  'Family & People',
  'Emotions & Feelings',
  'Colors & Shapes',
  'Occupations & Tools',
];

export const GAMES = {
  WORD_MATCH: 'word_match',
  REVERSE_MATCH: 'reverse_match',
  FILL_BLANK: 'fill_blank',
  ASPECT_BUILDER: 'aspect_builder',
  SENTENCE_ORDER: 'sentence_order',
  /** Word Recall kept Audio Quiz's key so its levels and stars keep counting. */
  RECALL: 'audio_quiz',
  WORD_SEARCH: 'word_search',
  WORD_WHEEL: 'word_wheel',
} as const;

/** The `gameLevels` key prefix for one category's Word Search track, e.g. `word_search_food_dining`. */
export function wordSearchLevelKey(category: string): string {
  return (
    GAMES.WORD_SEARCH +
    '_' +
    category
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '')
  );
}

export function categoryForWordSearchKey(key: string): string | undefined {
  return CATEGORIES.find((c) => wordSearchLevelKey(c) === key);
}

export const GAME_UNLOCK_STARS: Record<string, number> = {
  word_match: 0,
  word_search: 0,
  word_wheel: 0,
  fill_blank: 45,
  audio_quiz: 90,
  aspect_builder: 135,
  sentence_order: 180,
  reverse_match: 225,
};

export const LEVELS_PER_GAME = 30;

export const calculateLevel = (totalXp: number) => levelFor(totalXp);

/** Progress through the current level, 0 to 1; 1 at the cap, where lifetime XP keeps counting. */
export function levelProgress(totalXp: number): number {
  const level = levelFor(totalXp);
  if (level >= MAX_LEVEL) return 1;
  const current = LEVEL_THRESHOLDS[level - 1];
  const next = LEVEL_THRESHOLDS[level];
  return Math.min(1, Math.max(0, (totalXp - current) / (next - current)));
}

/** getLevelTitle: account levels describe participation, so the title is just the level. */
export function levelTitle(level: number): string {
  return `Level ${Math.min(MAX_LEVEL, Math.max(1, level))}`;
}

/** XP still needed for the next level, or null at level 30 (there is no next target). */
export function xpToNextLevel(totalXp: number): number | null {
  const level = levelFor(totalXp);
  if (level >= MAX_LEVEL) return null;
  return LEVEL_THRESHOLDS[level] - totalXp;
}
