/**
 * Word Wheel: domain/wordwheel/WordWheelGenerator.kt.
 *
 * Levels are chosen by a hash of each base word's letters, not by row id, so the same dictionary
 * yields the same thirty puzzles on every device - including this one. Every board word and bonus
 * word is a real dictionary entry; nothing is spelled letter by letter.
 */
import { KotlinRandom } from './kotlinRandom';
import type { Word } from './types';
import { tilesOf } from './wordSearch';

export const WORD_WHEEL_MAX_LEVEL = 30;
export const MIN_WORD_LENGTH = 3;
export const MAX_BOARD_SPAN = 10;
export const MAX_HINTS = 3;

export function wordWheelTier(level: number) {
  if (level <= 10) return { name: 'Easy', letterCount: 5, minBoardWords: 4, maxBoardWords: 5 };
  if (level <= 20) return { name: 'Medium', letterCount: 6, minBoardWords: 5, maxBoardWords: 6 };
  return { name: 'Hard', letterCount: 7, minBoardWords: 6, maxBoardWords: 8 };
}

export interface WheelWord {
  id: string;
  word: string;
  letters: string[];
  key: string;
}

export interface BoardSlot {
  word: WheelWord;
  row: number;
  col: number;
  across: boolean;
}

export const slotCells = (s: BoardSlot): [number, number][] =>
  s.word.letters.map((_, i) => (s.across ? [s.row, s.col + i] : [s.row + i, s.col]));

export interface WordWheelPuzzle {
  wheel: string[];
  slots: BoardSlot[];
  rows: number;
  cols: number;
  bonusWords: WheelWord[];
  /** "r,c" -> letter */
  letterAt: Map<string, string>;
}

const ck = (r: number, c: number) => `${r},${c}`;
const codeUnitCompare = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

/** FNV-1a over UTF-16 code units, as a signed 32-bit int (WordWheelGenerator.stableHash). */
function stableHash(text: string): number {
  let hash = 0x811c9dc5 | 0;
  for (let i = 0; i < text.length; i++) {
    hash ^= text.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return hash | 0;
}

export function dictionaryOf(words: Word[]): WheelWord[] {
  const seen = new Set<string>();
  const out: WheelWord[] = [];
  for (const w of words.slice().sort((a, b) => codeUnitCompare(a.id, b.id))) {
    const letters = tilesOf(w.kasiguranin);
    if (!letters || letters.length < MIN_WORD_LENGTH) continue;
    const key = letters.join('');
    if (seen.has(key)) continue;
    seen.add(key);
    out.push({ id: w.id, word: w.kasiguranin.trim(), letters, key });
  }
  return out;
}

export function spells(wheel: string[], word: string[]): boolean {
  if (word.length > wheel.length) return false;
  const available = new Map<string, number>();
  for (const l of wheel) available.set(l, (available.get(l) ?? 0) + 1);
  for (const l of word) {
    const left = available.get(l) ?? 0;
    if (left === 0) return false;
    available.set(l, left - 1);
  }
  return true;
}

export function generateWordWheel(words: Word[], level: number): WordWheelPuzzle | null {
  const tier = wordWheelTier(level);
  const dictionary = dictionaryOf(words);
  const seenAnagram = new Set<string>();
  const bases = dictionary
    .filter((w) => w.letters.length === tier.letterCount)
    .filter((w) => {
      const sorted = w.letters.slice().sort(codeUnitCompare).join('');
      if (seenAnagram.has(sorted)) return false;
      seenAnagram.add(sorted);
      return true;
    })
    .sort((a, b) => stableHash(a.key) - stableHash(b.key) || codeUnitCompare(a.key, b.key));

  const wanted = (level - 1) % 10;
  let valid = 0;
  for (const base of bases) {
    const spelled = dictionary.filter((w) => spells(base.letters, w.letters));
    if (spelled.length < tier.minBoardWords) continue;
    const slots = layout(base, spelled, tier);
    if (!slots) continue;
    if (valid++ < wanted) continue;

    const placedKeys = new Set(slots.map((s) => s.word.key));
    const letterAt = new Map<string, string>();
    for (const s of slots) slotCells(s).forEach(([r, c], i) => letterAt.set(ck(r, c), s.word.letters[i]));
    return {
      wheel: shuffledWheel(base),
      slots,
      rows: Math.max(...slots.map((s) => Math.max(...slotCells(s).map((c) => c[0])))) + 1,
      cols: Math.max(...slots.map((s) => Math.max(...slotCells(s).map((c) => c[1])))) + 1,
      bonusWords: spelled.filter((w) => !placedKeys.has(w.key)),
      letterAt,
    };
  }
  return null;
}

const ACROSS = 1;
const DOWN = 2;

function layout(base: WheelWord, spelled: WheelWord[], tier: ReturnType<typeof wordWheelTier>): BoardSlot[] | null {
  const letters = new Map<string, string>();
  const directions = new Map<string, number>();
  const cellsOf = new Map<string, [number, number]>();
  const placed: BoardSlot[] = [];

  const put = (slot: BoardSlot) => {
    const bit = slot.across ? ACROSS : DOWN;
    slotCells(slot).forEach(([r, c], i) => {
      const k = ck(r, c);
      letters.set(k, slot.word.letters[i]);
      directions.set(k, (directions.get(k) ?? 0) | bit);
      cellsOf.set(k, [r, c]);
    });
    placed.push(slot);
  };

  const bounds = (extra: [number, number][]) => {
    const all = [...cellsOf.values(), ...extra];
    const rows = all.map((c) => c[0]);
    const cols = all.map((c) => c[1]);
    return { minR: Math.min(...rows), maxR: Math.max(...rows), minC: Math.min(...cols), maxC: Math.max(...cols) };
  };

  const fits = (slot: BoardSlot): boolean => {
    const bit = slot.across ? ACROSS : DOWN;
    const cells = slotCells(slot);
    const before = slot.across ? ck(slot.row, slot.col - 1) : ck(slot.row - 1, slot.col);
    const after = slot.across ? ck(slot.row, slot.col + cells.length) : ck(slot.row + cells.length, slot.col);
    if (letters.has(before) || letters.has(after)) return false;
    let crossings = 0;
    for (let i = 0; i < cells.length; i++) {
      const [r, c] = cells[i];
      const existing = letters.get(ck(r, c));
      if (existing != null) {
        if (existing !== slot.word.letters[i]) return false;
        if (((directions.get(ck(r, c)) ?? 0) & bit) !== 0) return false;
        crossings++;
      } else {
        const sides = slot.across ? [ck(r - 1, c), ck(r + 1, c)] : [ck(r, c - 1), ck(r, c + 1)];
        if (sides.some((s) => letters.has(s))) return false;
      }
    }
    if (crossings === 0) return false;
    const b = bounds(cells);
    return b.maxR - b.minR < MAX_BOARD_SPAN && b.maxC - b.minC < MAX_BOARD_SPAN;
  };

  const area = (slot: BoardSlot) => {
    const b = bounds(slotCells(slot));
    return (b.maxR - b.minR + 1) * (b.maxC - b.minC + 1);
  };

  put({ word: base, row: 0, col: 0, across: true });
  const rest = spelled
    .filter((w) => w.key !== base.key)
    .sort((a, b) => b.letters.length - a.letters.length || codeUnitCompare(a.key, b.key));

  for (const word of rest) {
    if (placed.length >= tier.maxBoardWords) break;
    const options: BoardSlot[] = [];
    // Kotlin iterates a HashMap here, whose order is not insertion order; the final pick is a
    // total order (area, row, col, across), so iteration order does not change the result.
    for (const [k, letter] of letters) {
      const [r, c] = cellsOf.get(k)!;
      const dirs = directions.get(k) ?? 0;
      word.letters.forEach((l, i) => {
        if (l !== letter) return;
        if (dirs === ACROSS) options.push({ word, row: r - i, col: c, across: false });
        if (dirs === DOWN) options.push({ word, row: r, col: c - i, across: true });
      });
    }
    const best = options
      .filter(fits)
      .sort((a, b) => area(a) - area(b) || a.row - b.row || a.col - b.col || Number(a.across) - Number(b.across))[0];
    if (best) put(best);
  }
  if (placed.length < tier.minBoardWords) return null;

  const minRow = Math.min(...placed.flatMap((s) => slotCells(s).map((c) => c[0])));
  const minCol = Math.min(...placed.flatMap((s) => slotCells(s).map((c) => c[1])));
  return placed.map((s) => ({ ...s, row: s.row - minRow, col: s.col - minCol }));
}

function shuffledWheel(base: WheelWord): string[] {
  const shuffled = new KotlinRandom(stableHash(base.key)).shuffled(base.letters);
  const same = shuffled.every((l, i) => l === base.letters[i]);
  return same ? [...base.letters.slice(1), base.letters[0]] : shuffled;
}

export const cellKey = ck;
