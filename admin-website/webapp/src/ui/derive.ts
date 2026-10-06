/**
 * What LearnViewModel derives for Home: today's one next action, the day goal, the week strip and
 * Jepjep's line. Pure functions of learner state, so Home always says something true.
 */
import type { Corpus } from '../domain/corpus';
import { daysBetween, epochDay, isoDate, now, today as todayIso } from '../domain/dates';
import { dailyXpEarned, storyUnlocked } from '../domain/learner';
import { nextLesson, sectionForUnit, wordsFor, type LessonRef } from '../domain/lesson';
import { plural } from '../domain/plural';
import { meaningFor } from '../domain/recall';
import type { LearnerData } from '../domain/learner';
import type { Story, UserProgress } from '../domain/types';
import type { Pose } from './kit';
import { STORIES_ENABLED } from '../domain/constants';

export const wordsToReview = (n: number) => plural(n, 'word');

/** Word of the day: seeded by the day, so it is the same for everyone and rotates tomorrow. */
export function wordOfTheDay(corpus: Corpus) {
  // WordOfDay.eligible sorts by Room id, which `order` mirrors, so a phone and the web pick one word.
  const eligible = corpus.all
    .filter((w) => w.kasiguranin && w.tagalog)
    .sort((a, b) => (a.order ?? Infinity) - (b.order ?? Infinity) || (a.id < b.id ? -1 : 1));
  if (!eligible.length) return null;
  return eligible[(epochDay(todayIso()) ?? 0) % eligible.length];
}

export interface ContinueCardData {
  heroWord: string;
  heroMeaning: string;
  sectionTitle: string;
  journeyLine: string;
  lessonLabel: string;
  ref: LessonRef;
}

export function continueCard(corpus: Corpus, learner: LearnerData): ContinueCardData | null {
  const ref = nextLesson(corpus, learner.lessons);
  if (!ref) return null;
  const words = wordsFor(corpus, ref);
  const hero = words[0];
  if (!hero) return null;
  const section = sectionForUnit(ref.unitId);
  return {
    heroWord: hero.kasiguranin,
    heroMeaning: meaningFor(hero.kasiguranin, hero.tagalog, hero.english) ?? hero.english,
    sectionTitle: section?.title ?? ref.unitId,
    journeyLine: section?.journeyLine ?? '',
    lessonLabel: `Lesson ${ref.lessonIndex + 1} · ${plural(words.length, 'word')}`,
    ref,
  };
}

export type ActivityKind = 'lesson' | 'review' | 'game' | 'story';
export interface Activity {
  kind: ActivityKind;
  title: string;
  subtitle: string;
  isDone: boolean;
  ref?: LessonRef;
}

export function activities(corpus: Corpus, learner: LearnerData, stories: Story[]): Activity[] {
  const out: Activity[] = [];
  const next = nextLesson(corpus, learner.lessons);
  if (next) {
    out.push({
      kind: 'lesson',
      title: sectionForUnit(next.unitId)?.title ?? next.unitId,
      subtitle: `Lesson ${next.lessonIndex + 1} · ${plural(wordsFor(corpus, next).length, 'word')}`,
      isDone: false,
      ref: next,
    });
  } else {
    out.push({ kind: 'lesson', title: 'Every lesson complete', subtitle: 'Keep them sharp with review', isDone: true });
  }
  const due = corpus.countScheduledDue();
  const review: Activity = {
    kind: 'review',
    title: 'Review',
    subtitle: due === 0 ? 'Nothing due today' : `${wordsToReview(Math.min(due, 20))} due`,
    isDone: due === 0,
  };
  if (due === 0) out.push(review);
  else out.unshift(review);
  out.push({ kind: 'game', title: 'Practice game', subtitle: 'Earn stars and XP', isDone: false });
  // No story on today's path while they are switched off (not narrated yet).
  const unlocked = STORIES_ENABLED ? stories.filter((s) => storyUnlocked(learner, s)) : [];
  if (unlocked.length) {
    const unread = unlocked.find((s) => !learner.stories[String(s.id)]?.isCompleted);
    out.push({ kind: 'story', title: unread?.title ?? 'Folk tales', subtitle: unread ? 'Read and listen' : 'All stories read', isDone: !unread });
  }
  return out;
}

export interface DayMark {
  label: string;
  dayOfMonth: number;
  practised: boolean;
  isToday: boolean;
}

/** The week is derived from the streak, the way Android does: a run ending on lastActiveDate. */
export function buildWeek(p: UserProgress): DayMark[] {
  const todayDate = now();
  const last = epochDay(p.lastActiveDate);
  const labels = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];
  return Array.from({ length: 7 }, (_, k) => {
    const daysAgo = 6 - k;
    const d = new Date(todayDate.getFullYear(), todayDate.getMonth(), todayDate.getDate() - daysAgo);
    const iso = isoDate(d);
    const before = last != null ? daysBetween(iso, p.lastActiveDate) : null;
    const practised = last != null && p.currentStreak > 0 && before != null && before >= 0 && before < p.currentStreak;
    return { label: labels[d.getDay()], dayOfMonth: d.getDate(), practised, isToday: daysAgo === 0 };
  });
}

export interface DayGoal {
  earned: number;
  goal: number;
  fraction: number;
  met: boolean;
  remainder: string;
  wordsDue: number;
}

export function dayGoal(learner: LearnerData, wordsDue: number): DayGoal {
  const p = learner.progress;
  const earned = dailyXpEarned(learner);
  const goal = p.dailyGoalXp;
  const met = earned >= goal && wordsDue === 0;
  let remainder: string;
  if (met) remainder = 'goal met';
  else if (earned < goal && wordsDue > 0) remainder = `${goal - earned} XP to go and ${wordsToReview(wordsDue)} to review`;
  else if (earned < goal) remainder = `${goal - earned} XP to go`;
  else remainder = `${wordsToReview(wordsDue)} still to review`;
  return { earned, goal, fraction: goal <= 0 ? 0 : Math.min(1, earned / goal), met, remainder, wordsDue };
}

export function jepjepLine(p: UserProgress, wordsDue: number, goalMet: boolean): { pose: Pose; text: string } {
  const practisedToday = p.lastActiveDate === todayIso();
  if (p.currentStreak > 0 && !practisedToday) return { pose: 'worried', text: `Keep your ${p.currentStreak}-day streak alive today.` };
  if (wordsDue > 0) return { pose: 'speaking', text: wordsDue === 1 ? '1 word is due for review.' : `${wordsDue} words are due for review.` };
  if (goalMet) return { pose: 'celebrating', text: 'All done for today. See you tomorrow!' };
  if (p.totalXp === 0) return { pose: 'waving', text: "Let's learn your first Kasiguranin words." };
  return { pose: 'waving', text: "Ready for today's lesson?" };
}

/** Open once its XP is reached, or for good once an access receipt says it was (XP policy 2). */
export const isStoryUnlocked = (s: Story, learner: LearnerData) => storyUnlocked(learner, s);
