/**
 * Typed-answer grading and the meaning shown as a prompt: RecallAnswerMatcher, RecallPrompt,
 * RecallGrading and AnswerLabel from util/.
 */
import { XP_PER_GAME_CORRECT } from './constants';
import { ReviewRating } from './sm2';

export type RecallMatch = 'Exact' | 'Close' | 'Wrong';

/** The schwa in every spelling the corpus uses: ë (most), ǝ (turned e), ə (IPA). */
const SCHWA = new Set(['ë', 'ə', 'ǝ']);
const ALTERNATE_SEPARATORS = /[/,]/;

export function normalise(raw: string): string {
  let folded = '';
  for (const ch of raw.trim().toLowerCase()) {
    if (ch === '-') continue;
    if (/\s/.test(ch)) {
      if (folded.length > 0 && !folded.endsWith(' ')) folded += ' ';
    } else if (SCHWA.has(ch)) {
      folded += 'e';
    } else {
      folded += ch;
    }
  }
  return folded.normalize('NFD').replace(/\p{Mn}+/gu, '').trim();
}

function allowedSlips(expected: string): number {
  if (expected.length <= 3) return 0;
  if (expected.length <= 7) return 1;
  return 2;
}

export function levenshtein(a: string, b: string): number {
  if (a === b) return 0;
  if (!a.length) return b.length;
  if (!b.length) return a.length;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  let cur = new Array<number>(b.length + 1).fill(0);
  for (let i = 1; i <= a.length; i++) {
    cur[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const sub = prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1);
      cur[j] = Math.min(cur[j - 1] + 1, prev[j] + 1, sub);
    }
    [prev, cur] = [cur, prev];
  }
  return prev[b.length];
}

export function matchRecall(typed: string, expected: string): RecallMatch {
  const a = normalise(typed);
  if (!a) return 'Wrong';
  const alternatives = expected
    .split(ALTERNATE_SEPARATORS)
    .map(normalise)
    .filter(Boolean);
  let best: RecallMatch = 'Wrong';
  for (const b of alternatives) {
    if (a === b) return 'Exact';
    if (levenshtein(a, b) <= allowedSlips(b)) best = 'Close';
  }
  return best;
}

/** RecallPrompt.meaningFor: the first gloss that does not hand over the answer, or null. */
export function meaningFor(kasiguranin: string, tagalog: string, english: string): string | null {
  const headword = normalise(kasiguranin);
  for (const g of [tagalog, english]) {
    if (g.trim() && normalise(g) !== headword) return g;
  }
  return null;
}

export const recallIsCorrect = (m: RecallMatch) => m !== 'Wrong';

export function recallRating(m: RecallMatch, expected: string, responseTimeMs: number): ReviewRating {
  if (m === 'Exact') {
    const fast = 1_500 + 400 * expected.trim().length;
    return responseTimeMs < fast ? ReviewRating.EASY : ReviewRating.GOOD;
  }
  return m === 'Close' ? ReviewRating.HARD : ReviewRating.AGAIN;
}

export function recallXp(m: RecallMatch): number {
  return m === 'Exact' ? XP_PER_GAME_CORRECT : m === 'Close' ? XP_PER_GAME_CORRECT / 2 : 0;
}

/** AnswerLabel.candidates: a "tagalog · english" button read back into its parts. */
export function answerCandidates(label: string): string[] {
  const trimmed = label.trim();
  if (!trimmed) return [];
  const parts = trimmed
    .split('·')
    .map((p) => p.trim())
    .filter(Boolean);
  return [...new Set([trimmed, ...parts])];
}
