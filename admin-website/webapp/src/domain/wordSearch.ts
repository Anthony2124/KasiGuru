/**
 * Word Search: domain/wordsearch/WordSearchGenerator.kt.
 *
 * Seeded by category and level with the same arithmetic as WordSearchViewModel, so replaying a level
 * for more stars replays the same grid.
 */
import { KotlinRandom } from './kotlinRandom';
import type { Word } from './types';

export type Cell = [number, number];

const DIRS = {
  EAST: [0, 1],
  SOUTH: [1, 0],
  SOUTH_EAST: [1, 1],
  NORTH_EAST: [-1, 1],
  WEST: [0, -1],
  NORTH: [-1, 0],
  NORTH_WEST: [-1, -1],
  SOUTH_WEST: [1, -1],
} as const;

export interface WordSearchTier {
  name: 'Easy' | 'Medium' | 'Hard';
  gridSize: number;
  wordCount: number;
  directions: (readonly [number, number])[];
}

export const WORD_SEARCH_MAX_LEVEL = 30;
export const WORD_SEARCH_MIN_WORDS = 3;

export function wordSearchTier(level: number): WordSearchTier {
  if (level <= 10) return { name: 'Easy', gridSize: 6, wordCount: 4, directions: [DIRS.EAST, DIRS.SOUTH] };
  if (level <= 20)
    return { name: 'Medium', gridSize: 8, wordCount: 6, directions: [DIRS.EAST, DIRS.SOUTH, DIRS.SOUTH_EAST, DIRS.NORTH_EAST] };
  return { name: 'Hard', gridSize: 10, wordCount: 8, directions: Object.values(DIRS) };
}

/** One tile per letter: NFC, hyphens dropped, and null for anything that is not all letters. */
export function tilesOf(word: string): string[] | null {
  const normalized = word.trim().normalize('NFC').replace(/-/g, '');
  if (!normalized) return null;
  const tiles: string[] = [];
  for (let i = 0; i < normalized.length; i++) {
    const ch = normalized[i];
    if (!/\p{L}/u.test(ch)) return null;
    tiles.push(ch.toUpperCase());
  }
  return tiles;
}

export interface PlacedWord {
  id: string;
  word: string;
  cells: Cell[];
  letters: string;
  tiles: string[];
}

export interface WordSearchBoard {
  size: number;
  grid: string[][];
  placements: PlacedWord[];
}

export function lineBetween(from: Cell, to: Cell): Cell[] | null {
  const dr = to[0] - from[0];
  const dc = to[1] - from[1];
  if (dr !== 0 && dc !== 0 && Math.abs(dr) !== Math.abs(dc)) return null;
  const steps = Math.max(Math.abs(dr), Math.abs(dc));
  const sr = Math.sign(dr);
  const sc = Math.sign(dc);
  return Array.from({ length: steps + 1 }, (_, i) => [from[0] + sr * i, from[1] + sc * i] as Cell);
}

/** The unfound word a straight line spells, read either way. */
export function matchLine(board: WordSearchBoard, from: Cell, to: Cell, found: Set<string>): PlacedWord | null {
  const line = lineBetween(from, to);
  if (!line) return null;
  const spelled = line.map(([r, c]) => board.grid[r][c]).join('\u0000');
  const reversed = line
    .map(([r, c]) => board.grid[r][c])
    .reverse()
    .join('\u0000');
  return (
    board.placements.find((p) => !found.has(p.id) && (p.tiles.join('\u0000') === spelled || p.tiles.join('\u0000') === reversed)) ?? null
  );
}

/** Stable seed from a string (the level key) and a level number. */
export function wordSearchSeed(levelKey: string, level: number): number {
  let h = 0;
  for (let i = 0; i < levelKey.length; i++) h = (Math.imul(31, h) + levelKey.charCodeAt(i)) | 0;
  return h * 31 + level;
}

export function generateWordSearch(words: Word[], level: number, seed = level): WordSearchBoard | null {
  const tier = wordSearchTier(level);
  const random = new KotlinRandom(seed);

  const seen = new Set<string>();
  const pool = random.shuffled(
    words
      .slice()
      .sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0))
      .map((w) => ({ w, tiles: tilesOf(w.kasiguranin) }))
      .filter((c): c is { w: Word; tiles: string[] } => !!c.tiles && c.tiles.length >= 3 && c.tiles.length <= tier.gridSize)
      .filter((c) => {
        const key = c.tiles.join('');
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      })
  );

  const n = tier.gridSize;
  const grid: (string | null)[][] = Array.from({ length: n }, () => Array<string | null>(n).fill(null));
  const placed: PlacedWord[] = [];

  for (const { w, tiles } of pool) {
    if (placed.length >= tier.wordCount) break;
    const text = tiles.join('');
    if (placed.some((p) => p.letters.includes(text) || text.includes(p.letters))) continue;
    const cells = place(grid, tiles, tier, random);
    if (cells) placed.push({ id: w.id, word: w.kasiguranin.trim(), cells, letters: text, tiles });
  }
  if (placed.length < WORD_SEARCH_MIN_WORDS) return null;

  const fillers = placed.flatMap((p) => p.tiles);
  const rows = grid.map((row) => row.map((cell) => cell ?? fillers[random.nextIntUntil(fillers.length)]));
  return { size: n, grid: rows, placements: placed };
}

function place(grid: (string | null)[][], tiles: string[], tier: WordSearchTier, random: KotlinRandom): Cell[] | null {
  const n = tier.gridSize;
  for (let attempt = 0; attempt < 250; attempt++) {
    const [dr, dc] = tier.directions[random.nextIntUntil(tier.directions.length)];
    const row = random.nextIntUntil(n);
    const col = random.nextIntUntil(n);
    const endRow = row + dr * (tiles.length - 1);
    const endCol = col + dc * (tiles.length - 1);
    if (endRow < 0 || endRow >= n || endCol < 0 || endCol >= n) continue;
    const cells = tiles.map((_, i) => [row + dr * i, col + dc * i] as Cell);
    if (cells.some(([r, c], i) => grid[r][c] != null && grid[r][c] !== tiles[i])) continue;
    cells.forEach(([r, c], i) => (grid[r][c] = tiles[i]));
    return cells;
  }
  return null;
}

/** Whether a category has enough short words for its first level (the category picker's rule). */
export function categoryPlayable(words: Word[]): boolean {
  const easy = wordSearchTier(1);
  const keys = new Set<string>();
  for (const w of words) {
    const t = tilesOf(w.kasiguranin);
    if (t && t.length >= 3 && t.length <= easy.gridSize) keys.add(t.join(''));
  }
  return keys.size >= easy.wordCount;
}
