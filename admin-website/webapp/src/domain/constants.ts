/** util/Constants.kt, the parts the learner app reads. */

export const XP_PER_WORD_LEARNED = 100;
export const XP_PER_STORY_PAGE = 15;
export const XP_PER_STORY_COMPLETE = 50;
export const XP_PER_GAME_CORRECT = 20;
export const XP_BONUS_PERFECT_GAME = 100;

export const LEVEL_THRESHOLDS = [0, 100, 300, 600, 1000, 1500, 2200, 3000, 4000, 5000];

export const LEVEL_TITLES = [
  'Baguhan',
  'Nag-aaral',
  'Nagsisimula',
  'Lumalago',
  'Sumusulong',
  'Mahusay',
  'Dalubhasa',
  'Pantas',
  'Guro',
  'Mæstro',
];

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

export function calculateLevel(totalXp: number): number {
  for (let i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
    if (totalXp >= LEVEL_THRESHOLDS[i]) return i + 1;
  }
  return 1;
}

export function levelProgress(totalXp: number): number {
  const level = calculateLevel(totalXp);
  if (level >= LEVEL_THRESHOLDS.length) return 1;
  const current = LEVEL_THRESHOLDS[level - 1];
  const next = LEVEL_THRESHOLDS[level];
  return Math.min(1, Math.max(0, (totalXp - current) / (next - current)));
}

export function levelTitle(level: number): string {
  return LEVEL_TITLES[level - 1] ?? LEVEL_TITLES[LEVEL_TITLES.length - 1];
}

export function xpToNextLevel(totalXp: number): number | null {
  const level = calculateLevel(totalXp);
  if (level >= LEVEL_THRESHOLDS.length) return null;
  return LEVEL_THRESHOLDS[level] - totalXp;
}
