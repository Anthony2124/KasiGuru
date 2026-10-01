/**
 * The dictionary joined with the learner's review state, and the queries Android runs against Room
 * (VocabularyDao / VocabularyRepository), done in memory. The corpus is ~1,200 rows, so a linear
 * pass is well under a millisecond and needs no index.
 */
import { today as todayIso } from './dates';
import { distinctBy, shuffled } from './random';
import { answerCandidates } from './recall';
import { LEECH_LAPSES } from './sm2';
import type { Word, WordContent, WordState } from './types';
import { DEFAULT_WORD_STATE } from './types';
import { wordKey } from './merge';

export type DistractorDifficulty = 'GENTLE' | 'STANDARD' | 'TIGHT';

/** DistractorSelector.difficultyFor. */
export function difficultyFor(word: WordState): DistractorDifficulty {
  if (word.timesReviewed < 2 || word.easinessFactor < 2.0) return 'GENTLE';
  if (word.isLearned || word.easinessFactor >= 2.5) return 'TIGHT';
  return 'STANDARD';
}

function shapeSimilarity(target: Word, other: Word): number {
  const a = target.kasiguranin.toLowerCase();
  const b = other.kasiguranin.toLowerCase();
  let score = 0;
  if (a[0] === b[0]) score += 2;
  if (Math.abs(a.length - b.length) <= 1) score += 2;
  if (a.length >= 2 && b.length >= 2 && a.slice(-2) === b.slice(-2)) score += 1;
  return score;
}

/** DistractorSelector.choose. */
export function chooseDistractors(
  target: Word,
  sameCategory: Word[],
  otherCategory: Word[],
  difficulty: DistractorDifficulty,
  count: number
): Word[] {
  const near = sameCategory.filter((w) => w.id !== target.id && w.kasiguranin);
  const far = otherCategory.filter((w) => w.id !== target.id && w.kasiguranin);
  let ordered: Word[];
  if (difficulty === 'TIGHT') {
    ordered = [...near.slice().sort((x, y) => shapeSimilarity(target, y) - shapeSimilarity(target, x)), ...far];
  } else if (difficulty === 'STANDARD') {
    ordered = [...shuffled(near), ...shuffled(far)];
  } else {
    ordered = [...far.slice().sort((x, y) => shapeSimilarity(target, x) - shapeSimilarity(target, y)), ...shuffled(near)];
  }
  return distinctBy(ordered, (w) => w.id).slice(0, count);
}

/** buildPracticeRound: review-first, but never more than 70% review. */
export function buildPracticeRound(due: Word[], fresh: Word[], count: number): Word[] {
  if (count <= 0) return [];
  const dueBudget = Math.ceil(count * 0.7);
  const chosen = new Map<string, Word>();
  for (const w of due.slice(0, dueBudget)) chosen.set(w.id, w);
  for (const w of fresh) {
    if (chosen.size >= count) break;
    if (!chosen.has(w.id)) chosen.set(w.id, w);
  }
  for (const w of due) {
    if (chosen.size >= count) break;
    if (!chosen.has(w.id)) chosen.set(w.id, w);
  }
  return shuffled([...chosen.values()]);
}

export function buildPracticeRoundFromPool(pool: Word[], today: string, count: number): Word[] {
  const due = pool.filter((w) => w.nextReviewDate && w.nextReviewDate <= today).sort((a, b) => a.nextReviewDate.localeCompare(b.nextReviewDate));
  const rest = pool.filter((w) => !(w.nextReviewDate && w.nextReviewDate <= today)).sort((a, b) => a.timesReviewed - b.timesReviewed);
  return buildPracticeRound(due, rest, count);
}

export class Corpus {
  readonly all: Word[];
  private readonly byIdMap: Map<string, Word>;

  constructor(content: WordContent[], states: Record<string, WordState>) {
    this.all = content.map((c) => ({ ...c, ...(states[wordKey(c.kasiguranin)] ?? DEFAULT_WORD_STATE) }));
    this.byIdMap = new Map(this.all.map((w) => [w.id, w]));
  }

  get size() {
    return this.all.length;
  }

  byId(id: string): Word | undefined {
    return this.byIdMap.get(id);
  }

  /** getVocabularyByCategory: ORDER BY kasiguranin. */
  inCategory(category: string): Word[] {
    return this.all
      .filter((w) => w.category === category)
      .sort((a, b) => a.kasiguranin.localeCompare(b.kasiguranin));
  }

  categories(): string[] {
    return [...new Set(this.all.map((w) => w.category))].sort();
  }

  learnedCount(): number {
    return this.all.filter((w) => w.isLearned).length;
  }

  /** getScheduledDueWords: a real review date that has arrived, soonest first. */
  scheduledDue(limit = 20, today = todayIso()): Word[] {
    return this.all
      .filter((w) => w.nextReviewDate !== '' && w.nextReviewDate <= today)
      .sort((a, b) => a.nextReviewDate.localeCompare(b.nextReviewDate))
      .slice(0, limit);
  }

  countScheduledDue(today = todayIso()): number {
    let n = 0;
    for (const w of this.all) if (w.nextReviewDate !== '' && w.nextReviewDate <= today) n++;
    return n;
  }

  /** getDueReviewWords: due or never scheduled, soonest first then random; falls back to random. */
  dueForDeck(count = 10, today = todayIso()): Word[] {
    const due = shuffled(this.all.filter((w) => w.nextReviewDate <= today || w.nextReviewDate === ''))
      .sort((a, b) => a.nextReviewDate.localeCompare(b.nextReviewDate))
      .slice(0, count);
    return due.length ? due : this.random(count);
  }

  /** getFreshWords: least reviewed first, random within a tie. */
  fresh(count: number): Word[] {
    return shuffled(this.all)
      .sort((a, b) => a.timesReviewed - b.timesReviewed)
      .slice(0, count);
  }

  random(count: number): Word[] {
    return shuffled(this.all).slice(0, count);
  }

  leeches(limit: number): Word[] {
    return this.all
      .filter((w) => w.lapses >= LEECH_LAPSES)
      .sort((a, b) => b.lapses - a.lapses)
      .slice(0, limit);
  }

  /** getPracticeWords: everything due first, then unseen material. */
  practiceWords(count: number, today = todayIso()): Word[] {
    return buildPracticeRound(this.scheduledDue(count, today), this.fresh(count * 2), count);
  }

  /** VocabularyRepository.getDistractorsForWord. */
  distractorsFor(target: Word, count = 3): Word[] {
    const pool = count * 4;
    const same = shuffled(this.all.filter((w) => w.id !== target.id && w.category === target.category)).slice(0, pool);
    const other = shuffled(this.all.filter((w) => w.id !== target.id && w.category !== target.category)).slice(0, pool);
    return chooseDistractors(target, same, other, difficultyFor(target), count);
  }

  /** findByWrittenForm: resolves an option label back to the word it came from. */
  findByWrittenForm(text: string): Word | undefined {
    for (const c of answerCandidates(text)) {
      const lc = c.toLowerCase();
      const hit = this.all.find(
        (w) => w.kasiguranin.toLowerCase() === lc || w.tagalog.toLowerCase() === lc || w.english.toLowerCase() === lc
      );
      if (hit) return hit;
    }
    return undefined;
  }

  /** searchVocabulary: substring on any of the three languages, ordered by headword. */
  search(query: string, limit = 200): Word[] {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    return this.all
      .filter(
        (w) =>
          w.kasiguranin.toLowerCase().includes(q) || w.tagalog.toLowerCase().includes(q) || w.english.toLowerCase().includes(q)
      )
      .sort((a, b) => a.kasiguranin.localeCompare(b.kasiguranin))
      .slice(0, limit);
  }
}
