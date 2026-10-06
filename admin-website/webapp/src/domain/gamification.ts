/**
 * The mini-game shelf and per-level game rules (GameHubScreen, the games' view models). Badges and
 * levels moved to ./badges.ts and ./xp.ts with XP policy 2.
 */
import { CATEGORIES, GAMES, LEVELS_PER_GAME } from './constants';

/** The mini-game shelf. Only the first three are playable; the rest show "Coming soon" on Android too. */
export interface GameEntry {
  type: string;
  title: string;
  icon: 'element4' | 'search' | 'refresh' | 'repeat' | 'edit' | 'document' | 'keyboard' | 'teacher';
  tone: 'lime' | 'coral' | 'gold';
  unlockStars: number;
  comingSoon: boolean;
  blurb: string;
}

export const GAME_ENTRIES: GameEntry[] = [
  { type: GAMES.WORD_MATCH, title: 'Word Match', icon: 'element4', tone: 'lime', unlockStars: 0, comingSoon: false, blurb: 'Pick the Tagalog meaning of each Kasiguranin word.' },
  { type: GAMES.WORD_SEARCH, title: 'Word Search', icon: 'search', tone: 'coral', unlockStars: 0, comingSoon: false, blurb: 'Find hidden words from one category in a letter grid.' },
  { type: GAMES.WORD_WHEEL, title: 'Word Wheel', icon: 'refresh', tone: 'gold', unlockStars: 0, comingSoon: false, blurb: 'Spell words from one wheel of letters to fill a small crossword.' },
  { type: GAMES.REVERSE_MATCH, title: 'Reverse Match', icon: 'repeat', tone: 'lime', unlockStars: 225, comingSoon: true, blurb: '' },
  { type: GAMES.FILL_BLANK, title: 'Fill in Blank', icon: 'edit', tone: 'lime', unlockStars: 45, comingSoon: true, blurb: '' },
  { type: GAMES.SENTENCE_ORDER, title: 'Sentence Order', icon: 'document', tone: 'lime', unlockStars: 180, comingSoon: true, blurb: '' },
  { type: GAMES.RECALL, title: 'Word Recall', icon: 'keyboard', tone: 'lime', unlockStars: 90, comingSoon: true, blurb: '' },
  { type: GAMES.ASPECT_BUILDER, title: 'Aspect Builder', icon: 'teacher', tone: 'lime', unlockStars: 135, comingSoon: true, blurb: '' },
];

/**
 * Every game level there is, as rows in Android's game_levels table (DatabaseSeeder): 30 levels for
 * each of seven games, plus a 30-level Word Search track per category. The XP page counts game
 * stars out of three per level.
 */
export const GAME_LEVEL_COUNT =
  ([GAMES.WORD_MATCH, GAMES.REVERSE_MATCH, GAMES.FILL_BLANK, GAMES.RECALL, GAMES.ASPECT_BUILDER, GAMES.SENTENCE_ORDER, GAMES.WORD_WHEEL].length +
    CATEGORIES.length) *
  LEVELS_PER_GAME;

export const gameTitle = (type: string) =>
  GAME_ENTRIES.find((g) => g.type === type)?.title ?? (type.startsWith(GAMES.WORD_SEARCH) ? 'Word Search' : type);

/** Stars from a round's success rate, as every game on Android scores it. */
export function starsFor(successRate: number): number {
  if (successRate >= 1) return 3;
  if (successRate >= 0.7) return 2;
  if (successRate >= 0.4) return 1;
  return 0;
}

/** Easy 1-10 (5 questions), Medium 11-20 (10), Hard 21-30 (15). */
export function questionsForLevel(level: number): number {
  return level <= 10 ? 5 : level <= 20 ? 10 : 15;
}

export function difficultyForLevel(level: number): 'Easy' | 'Medium' | 'Hard' {
  return level <= 10 ? 'Easy' : level <= 20 ? 'Medium' : 'Hard';
}
