/**
 * The web app's data model. Field names match the Android entities and the Firestore documents
 * one for one, because the synced progress documents are shared between the two apps: an Android
 * learner who signs in here must see exactly what they left, and vice versa.
 */

/** Dictionary content for one sense of a word, as the admin portal writes it. */
export interface WordContent {
  /** Firestore document id. Stable, unlike Android's Room ids, so it doubles as the local id. */
  id: string;
  kasiguranin: string;
  tagalog: string;
  english: string;
  rootForm: string;
  neutralForm: string;
  imperfectiveForm: string;
  perfectiveForm: string;
  contemplativeForm: string;
  category: string;
  /** Learning-tree section tag, or blank. See LearningTree. */
  theme: string;
  partOfSpeech: string;
  meaningEnglish: string;
  meaningTagalog: string;
  /** `word_audio/{audioFileName}` holds the clip; blank means none. */
  audioFileName: string;
  audioUpdatedAt: number;
  exampleSentence: string;
  exampleTranslation: string;
  exampleSentence2: string;
  exampleTranslation2: string;
  phoneticGlottal: boolean;
  phoneticVowelLength: boolean;
  ipaNotation: string;
  updatedAt: number;
}

/** A learner's SM-2 state for one headword. Mirrors ProgressSyncManager's WordState. */
export interface WordState {
  isLearned: boolean;
  timesReviewed: number;
  easinessFactor: number;
  intervalDays: number;
  /** ISO yyyy-MM-dd, or blank when never scheduled. */
  nextReviewDate: string;
  lapses: number;
  relearningStep: number;
}

/** Content joined with the learner's state: the shape Android's VocabularyEntity has. */
export type Word = WordContent & WordState;

export interface LessonState {
  isComplete: boolean;
  bestAccuracy: number;
  timesCompleted: number;
  lastCompletedAt: number;
}

export interface GameLevelState {
  starsEarned: number;
  isUnlocked: boolean;
}

export interface AchievementState {
  isUnlocked: boolean;
  currentValue: number;
  unlockedDate: string | null;
}

/** users/{uid}/progress/main, field for field (password excepted, as on Android). */
export interface UserProgress {
  id: number;
  userName: string;
  email: string;
  fullName: string;
  age: number | null;
  address: string;
  profileIconId: number;
  /** A ProfileBackgroundCatalog id; the rules accept only those seven. */
  profileBackgroundId: string;
  totalXp: number;
  level: number;
  currentStreak: number;
  longestStreak: number;
  lastActiveDate: string;
  wordsLearned: number;
  storiesCompleted: number;
  gamesPlayed: number;
  totalCorrectAnswers: number;
  totalQuestionsAnswered: number;
  lessonsCompleted: number;
  isOnboardingCompleted: boolean;
  dailyGoalXp: number;
  dailyXpEarned: number;
  dailyXpDate: string;
  titleBadge: string;
  submissionsMade: number;
  dailyReviewCompletedDate: string;
  dailyGamesDate: string;
  dailyGamesPlayedCount: number;
  /** 2 once totals are projected from reward receipts; the rules refuse any later write below it. */
  xpPolicyVersion: number;
  /** Dated and imported activity XP. Under policy 2, totalXp is activityXp + badgeBonusXp. */
  activityXp: number;
  badgeBonusXp: number;
  /** Up to three badge family ids, comma-separated, in the order they were pinned. */
  pinnedBadgeIds: string;
  updatedAt: number;
}

export interface Story {
  id: number;
  title: string;
  titleKasiguranin: string;
  description: string;
  category: string;
  pagesJson: string;
  totalPages: number;
  requiredXp: number;
  updatedAt: number;
}

export interface StoryPage {
  pageNumber: number;
  kasiguranin: string;
  tagalog: string;
  english: string;
  audioFileName?: string;
  illustrationDesc?: string;
  imageId?: string;
}

/** Local-only reading position, as StoryEntity keeps it on the device. */
export interface StoryProgress {
  isCompleted: boolean;
  currentPage: number;
}

/** One finished mini-game round. Local only, like Android's game_scores table. */
export interface GameScore {
  gameType: string;
  score: number;
  totalQuestions: number;
  xpEarned: number;
  playedAt: number;
}

export interface AnnouncementDto {
  id: string;
  title: string;
  message: string;
  active: boolean;
  createdAt: number;
}

export const DEFAULT_WORD_STATE: WordState = {
  isLearned: false,
  timesReviewed: 0,
  easinessFactor: 2.5,
  intervalDays: 0,
  nextReviewDate: '',
  lapses: 0,
  relearningStep: 0,
};

/** The nickname every progress row starts with, before onboarding asks for one. */
export const PLACEHOLDER_NAME = 'Learner';

export function initialProgress(): UserProgress {
  return {
    id: 1,
    userName: PLACEHOLDER_NAME,
    email: '',
    fullName: '',
    age: null,
    address: '',
    profileIconId: 1,
    profileBackgroundId: 'forest',
    totalXp: 0,
    level: 1,
    currentStreak: 0,
    longestStreak: 0,
    lastActiveDate: '',
    wordsLearned: 0,
    storiesCompleted: 0,
    gamesPlayed: 0,
    totalCorrectAnswers: 0,
    totalQuestionsAnswered: 0,
    lessonsCompleted: 0,
    isOnboardingCompleted: false,
    dailyGoalXp: 50,
    dailyXpEarned: 0,
    dailyXpDate: '',
    titleBadge: 'Kasiguranin Apprentice',
    submissionsMade: 0,
    dailyReviewCompletedDate: '',
    dailyGamesDate: '',
    dailyGamesPlayedCount: 0,
    xpPolicyVersion: 0,
    activityXp: 0,
    badgeBonusXp: 0,
    pinnedBadgeIds: '',
    updatedAt: 0,
  };
}
