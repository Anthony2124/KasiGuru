/**
 * SuperMemo-2 with lapse counting and a relearning ladder: util/srs/Sm2Algorithm.kt, line for line.
 * The learning model is a thesis claim, and the schedule a word carries is shared with the Android
 * app through the synced wordStates document, so both apps must compute the same next date.
 */
import { plusDays, today } from './dates';
import type { WordState } from './types';

export enum ReviewRating {
  AGAIN = 0,
  HARD = 3,
  GOOD = 4,
  EASY = 5,
}

export const MIN_LEARNED_REVIEWS = 3;
export const MIN_LEARNED_INTERVAL_DAYS = 6;
export const RELEARNING_STEPS = [1, 2, 4];
export const LEECH_LAPSES = 5;

export const isLeech = (w: Pick<WordState, 'lapses'>) => w.lapses >= LEECH_LAPSES;

export function calculateNextReview(card: WordState, rating: ReviewRating, currentDate: string = today()): WordState {
  const q = rating as number;

  let newEf = card.easinessFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
  if (newEf < 1.3) newEf = 1.3;

  const failed = q < 3;
  const hadBeenRetained = card.isLearned || card.intervalDays >= MIN_LEARNED_INTERVAL_DAYS;
  const lapses = card.lapses + (failed && hadBeenRetained ? 1 : 0);

  let relearningStep: number;
  let newInterval: number;
  if (failed) {
    relearningStep = 1;
    newInterval = RELEARNING_STEPS[0];
  } else if (card.relearningStep > 0) {
    const nextStep = card.relearningStep + 1;
    if (nextStep <= RELEARNING_STEPS.length) {
      relearningStep = nextStep;
      newInterval = RELEARNING_STEPS[nextStep - 1];
    } else {
      relearningStep = 0;
      newInterval = Math.max(Math.trunc(card.intervalDays * newEf), RELEARNING_STEPS[RELEARNING_STEPS.length - 1] + 1);
    }
  } else if (card.intervalDays <= 0) {
    relearningStep = 0;
    newInterval = 1;
  } else if (card.intervalDays === 1) {
    relearningStep = 0;
    newInterval = 6;
  } else {
    relearningStep = 0;
    newInterval = Math.trunc(card.intervalDays * newEf);
  }

  const timesReviewed = card.timesReviewed + 1;
  const isLearned =
    q >= 3 && relearningStep === 0 && timesReviewed >= MIN_LEARNED_REVIEWS && newInterval >= MIN_LEARNED_INTERVAL_DAYS;

  return {
    easinessFactor: newEf,
    intervalDays: newInterval,
    nextReviewDate: plusDays(currentDate, newInterval),
    timesReviewed,
    isLearned,
    lapses,
    relearningStep,
  };
}

/** ReviewRatingMapper: a tap's speed read as fluency. */
export function ratingForAnswer(isCorrect: boolean, responseTimeMs: number, usedHint = false): ReviewRating {
  if (!isCorrect) return ReviewRating.AGAIN;
  // A hinted answer was recognised, not recalled.
  if (usedHint) return ReviewRating.HARD;
  if (responseTimeMs < 1_500) return ReviewRating.EASY;
  if (responseTimeMs < 5_000) return ReviewRating.GOOD;
  return ReviewRating.HARD;
}
