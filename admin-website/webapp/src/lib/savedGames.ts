/**
 * The game each level was left in, one per level, kept on this device only (SavedGameRepository on
 * Android): see domain/savedGame.ts.
 *
 * Read once at boot into memory, so a game finds its save as it opens and the level picker can mark
 * levels without waiting. Every change rewrites the whole set under one IndexedDB key. idb-keyval
 * runs each write in its own transaction, in the order they were made, so a save can never land after
 * the clear that follows it, which would bring a finished round back to be rewarded a second time.
 *
 * Levels are keyed by the same game type as their game_levels row, so Word Search keeps one game per
 * category level.
 */
import { useEffect, useState } from 'preact/hooks';
import { parseSavedGame, type SavedGame } from '../domain/savedGame';
import { load, save } from './persist';

const STORAGE_KEY = 'saved-games';

let games: Record<string, SavedGame> = {};
const listeners = new Set<() => void>();

const keyFor = (gameType: string, level: number) => `${gameType}/${level}`;

function changed() {
  void save(STORAGE_KEY, games);
  listeners.forEach((fn) => fn());
}

/** Loads what this device kept. Anything this version cannot read is dropped, not resumed. */
export async function hydrateSavedGames() {
  const stored = await load<Record<string, unknown>>(STORAGE_KEY);
  const next: Record<string, SavedGame> = {};
  for (const [key, value] of Object.entries(stored ?? {})) {
    const game = parseSavedGame(value);
    if (game) next[key] = game;
  }
  games = next;
}

/** The game [level] of [gameType] was left in, or null to deal a fresh one. */
export function loadSavedGame(gameType: string, level: number): SavedGame | null {
  return games[keyFor(gameType, level)] ?? null;
}

/** Keeps [game] as the one to resume. */
export function saveGame(gameType: string, level: number, game: SavedGame) {
  games = { ...games, [keyFor(gameType, level)]: game };
  changed();
}

/** Forgets the saved game, as its round finishes and before its reward is given. */
export function clearSavedGame(gameType: string, level: number) {
  const key = keyFor(gameType, level);
  if (!(key in games)) return;
  const rest = { ...games };
  delete rest[key];
  games = rest;
  changed();
}

/** Forgets every saved game, for sign-out: the next learner on this device starts their own. */
export async function clearAllSavedGames() {
  games = {};
  listeners.forEach((fn) => fn());
  await save(STORAGE_KEY, games);
}

/** The levels of [gameType] that have a game to resume, for the level picker. */
export function savedLevels(gameType: string): Set<number> {
  const prefix = `${gameType}/`;
  const levels = new Set<number>();
  for (const key of Object.keys(games)) {
    if (!key.startsWith(prefix)) continue;
    const n = Number(key.slice(prefix.length));
    if (Number.isInteger(n)) levels.add(n);
  }
  return levels;
}

/** [savedLevels], kept current while the level picker is open. */
export function useSavedLevels(gameType: string): Set<number> {
  const [levels, setLevels] = useState(() => savedLevels(gameType));
  useEffect(() => {
    const update = () => setLevels(savedLevels(gameType));
    update();
    listeners.add(update);
    return () => {
      listeners.delete(update);
    };
  }, [gameType]);
  return levels;
}
