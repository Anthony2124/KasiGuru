/**
 * Badges and display ranks: DatabaseSeeder.getInitialAchievements() and GamificationEngine.
 * Badge ids are shared with Android through the synced achievements document; never rename one.
 */
import { GAMES, LEVEL_THRESHOLDS } from './constants';

export interface AchievementDef {
  id: string;
  metricType: string;
  name: string;
  description: string;
  category: string;
  requiredValue: number;
  xpReward: number;
  tier?: 'bronze' | 'silver' | 'gold';
  /** Unlocked from the start (the level-1 badge). */
  startsUnlocked?: boolean;
}

export const METRIC = {
  WORDS_LEARNED: 'wordsLearned',
  STORIES_COMPLETED: 'storiesCompleted',
  GAMES_PLAYED: 'gamesPlayed',
  PERFECT_GAME: 'perfectGame',
  STREAK: 'streak',
  LEVEL: 'level',
  SUBMISSIONS_MADE: 'submissionsMade',
  SUBMISSIONS_APPROVED: 'submissionsApproved',
  CATEGORY_MASTERED: 'categoryMastered',
  WEEKLY_TOP_TEN: 'weeklyTopTen',
  GAME_MODES_PLAYED: 'gameModesPlayed',
} as const;

export const ACHIEVEMENTS: AchievementDef[] = [
  { id: 'level_1', metricType: 'level', name: 'Novice Explorer', description: 'Began your Kasiguranin learning journey', category: 'Level', requiredValue: 1, xpReward: 10, startsUnlocked: true },
  { id: 'level_2', metricType: 'level', name: 'Vocab Apprentice', description: 'Reached Level 2 & unlocked Fill in the Blank', category: 'Level', requiredValue: 2, xpReward: 50 },
  { id: 'level_3', metricType: 'level', name: 'Linguistic Scholar', description: 'Reached Level 3 & unlocked Audio Listening Quiz', category: 'Level', requiredValue: 3, xpReward: 100 },
  { id: 'level_4', metricType: 'level', name: 'Grammar Specialist', description: 'Reached Level 4 & unlocked Verb Aspect Builder', category: 'Level', requiredValue: 4, xpReward: 150 },
  { id: 'level_5', metricType: 'level', name: 'Kasiguranin Legend', description: 'Reached Level 5 & unlocked Sentence Construction', category: 'Level', requiredValue: 5, xpReward: 300 },
  { id: 'first_word', metricType: 'wordsLearned', name: 'Unáng Salitâ', description: 'Learn your first Kasiguranin word', category: 'Progress', requiredValue: 1, xpReward: 20 },
  { id: 'ten_words', metricType: 'wordsLearned', name: 'Sampûng Salitâ', description: 'Learn 10 Kasiguranin words', category: 'Progress', requiredValue: 10, xpReward: 50 },
  { id: 'fifty_words', metricType: 'wordsLearned', tier: 'bronze', name: 'Limampûng Salitâ', description: 'Learn 50 Kasiguranin words', category: 'Progress', requiredValue: 50, xpReward: 200 },
  { id: 'first_story', metricType: 'storiesCompleted', name: 'Mambábasa', description: 'Complete your first story', category: 'Progress', requiredValue: 1, xpReward: 50 },
  { id: 'first_game', metricType: 'gamesPlayed', name: 'Mánlalaro', description: 'Play your first mini-game', category: 'Progress', requiredValue: 1, xpReward: 25 },
  { id: 'perfect_game', metricType: 'perfectGame', name: 'Perpekto!', description: 'Get a perfect score in any mini-game', category: 'Games', requiredValue: 1, xpReward: 100 },
  { id: 'three_day_streak', metricType: 'streak', name: 'Tatlong Aldaw', description: 'Maintain a 3-day learning streak', category: 'Streaks', requiredValue: 3, xpReward: 50 },
  { id: 'seven_day_streak', metricType: 'streak', tier: 'bronze', name: 'Isáng Linggo', description: 'Maintain a 7-day learning streak', category: 'Streaks', requiredValue: 7, xpReward: 150 },
  { id: 'level_five', metricType: 'level', tier: 'silver', name: 'Sumusulong', description: 'Reach Level 5', category: 'Progress', requiredValue: 5, xpReward: 100 },
  { id: 'level_ten', metricType: 'level', tier: 'gold', name: 'Mæstro', description: 'Reach Level 10 — Master of Kasiguranin!', category: 'Progress', requiredValue: 10, xpReward: 500 },
  { id: 'all_stories', metricType: 'storiesCompleted', tier: 'silver', name: 'Tagapagsalaysay', description: 'Complete 3 stories', category: 'Progress', requiredValue: 3, xpReward: 150 },
  { id: 'first_contribution', metricType: 'submissionsMade', name: 'First Contribution', description: 'Submit your first word, story, or poem', category: 'Contribution', requiredValue: 1, xpReward: 30 },
  { id: 'trusted_voice', metricType: 'submissionsApproved', tier: 'bronze', name: 'Trusted Voice', description: 'Have 5 submissions approved into the dictionary or Stories', category: 'Contribution', requiredValue: 5, xpReward: 150 },
  { id: 'corpus_builder', metricType: 'submissionsApproved', tier: 'gold', name: 'Corpus Builder', description: 'Have 25 submissions approved into the dictionary or Stories', category: 'Contribution', requiredValue: 25, xpReward: 500 },
  { id: 'moon_cycle', metricType: 'streak', tier: 'silver', name: 'Moon Cycle', description: 'Maintain a 30-day learning streak', category: 'Streaks', requiredValue: 30, xpReward: 300 },
  { id: 'centurion', metricType: 'streak', tier: 'gold', name: 'Centurion', description: 'Maintain a 100-day learning streak', category: 'Streaks', requiredValue: 100, xpReward: 1000 },
  { id: 'category_master', metricType: 'categoryMastered', tier: 'gold', name: 'Category Master', description: 'Learn every word in one dictionary category', category: 'Mastery', requiredValue: 1, xpReward: 250 },
  { id: 'perfect_six', metricType: 'perfectSixInOneSitting', tier: 'gold', name: 'Perfect Six', description: 'Score perfectly in all six game modes in one sitting', category: 'Games', requiredValue: 1, xpReward: 400 },
  { id: 'top_of_the_week', metricType: 'weeklyTopTen', tier: 'silver', name: 'Top of the Week', description: 'Reach the top 10 of the weekly leaderboard', category: 'Social', requiredValue: 1, xpReward: 200 },
  { id: 'six_for_six', metricType: 'gameModesPlayed', name: 'Six for Six', description: 'Play every one of the six mini-game modes at least once', category: 'Games', requiredValue: 6, xpReward: 150 },
];

/** GamificationEngine's five display ranks, bounded by the stored level thresholds. */
export const RANKS = ['Novice Explorer', 'Vocab Apprentice', 'Linguistic Scholar', 'Grammar Specialist', 'Kasiguranin Legend'].map(
  (title, i) => ({
    level: i + 1,
    title,
    minXp: LEVEL_THRESHOLDS[i],
    maxXp: i === 4 ? Number.MAX_SAFE_INTEGER : LEVEL_THRESHOLDS[i + 1] - 1,
  })
);

export function rankFor(totalXp: number) {
  return [...RANKS].reverse().find((r) => totalXp >= r.minXp) ?? RANKS[0];
}

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
