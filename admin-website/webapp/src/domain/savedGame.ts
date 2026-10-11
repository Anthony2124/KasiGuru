/**
 * A game the learner left before its end (domain/games/SavedGame.kt on Android), kept on this device
 * so that opening the same level again carries on where they stopped instead of dealing a fresh one.
 *
 * Saved after every answer rather than only on the way out, so a closed tab or a reload keeps the
 * round too. Cleared as the round finishes, before its reward is given, so no round can be resumed,
 * and rewarded, twice. Storage is lib/savedGames.ts; this file is the shapes and the checks.
 */

/** One answer already given, as the end-of-round review lists it. */
export interface SavedAnswer {
  prompt: string;
  subPrompt?: string;
  userAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
}

/**
 * A question round partway through (Word Match on the web). Answered questions live on only in
 * [answers]; the next question is rebuilt from its word, so one the learner had open but not
 * answered comes back with its options in a different order.
 */
export interface SavedRound {
  kind: 'round';
  /** The round's words in order, by dictionary id. */
  questionKeys: string[];
  answers: SavedAnswer[];
  score: number;
  /** What the round is scored out of. Can exceed [questionKeys] when the dictionary is small. */
  totalQuestions: number;
  /** A hint was shown at some point in the round, which costs the perfect bonus. */
  usedHint: boolean;
  /** The next question's hint was already showing, so it is shown, and graded as hinted, again. */
  hintRevealed: boolean;
}

/** A Word Search grid partway through. The grid itself is rebuilt from its seed. */
export interface SavedWordSearch {
  kind: 'word_search';
  /** The words hidden in the grid, to tell whether a dictionary update has since changed it. */
  puzzleWordIds: string[];
  /** In the order found. */
  foundIds: string[];
  misses: number;
  hintRevealed: boolean;
}

/** A Word Wheel board partway through. The board itself is rebuilt from the dictionary. */
export interface SavedWordWheel {
  kind: 'word_wheel';
  /** The board's words in slot order, to tell whether a dictionary update has since changed it. */
  boardWords: string[];
  /** Board slots found, in the order found. */
  foundSlots: number[];
  /** Bonus words found, in the order found. */
  bonusWords: string[];
  hintsUsed: number;
  /** The board slot whose clue was showing, or null. */
  cluedSlot: number | null;
}

export type SavedGame = SavedRound | SavedWordSearch | SavedWordWheel;

const isInt = (v: unknown): v is number => Number.isInteger(v);
const isIntIn = (v: unknown, min: number, max = Infinity): v is number => isInt(v) && v >= min && v <= max;
const strings = (v: unknown): v is string[] => Array.isArray(v) && v.every((s) => typeof s === 'string');
const ints = (v: unknown): v is number[] => Array.isArray(v) && v.every(isInt);

function isAnswer(v: unknown): v is SavedAnswer {
  if (!v || typeof v !== 'object') return false;
  const a = v as Record<string, unknown>;
  return (
    typeof a.prompt === 'string' &&
    typeof a.userAnswer === 'string' &&
    typeof a.correctAnswer === 'string' &&
    typeof a.isCorrect === 'boolean' &&
    (a.subPrompt === undefined || typeof a.subPrompt === 'string')
  );
}

/**
 * What device storage handed back, if it is a saved game this version can resume; otherwise null, and
 * the level deals a fresh game. As SavedGameCodec.decode: a round that contradicts itself (more
 * answers than questions, a score above its answers, a total below its questions) is not resumed.
 */
export function parseSavedGame(value: unknown): SavedGame | null {
  if (!value || typeof value !== 'object') return null;
  const v = value as Record<string, unknown>;
  switch (v.kind) {
    case 'round':
      if (!strings(v.questionKeys) || !Array.isArray(v.answers) || !v.answers.every(isAnswer)) return null;
      if (typeof v.usedHint !== 'boolean' || typeof v.hintRevealed !== 'boolean') return null;
      if (!isIntIn(v.score, 0, v.answers.length) || v.answers.length > v.questionKeys.length) return null;
      if (!isIntIn(v.totalQuestions, v.questionKeys.length)) return null;
      return {
        kind: 'round',
        questionKeys: v.questionKeys,
        answers: v.answers as SavedAnswer[],
        score: v.score,
        totalQuestions: v.totalQuestions,
        usedHint: v.usedHint,
        hintRevealed: v.hintRevealed,
      };
    case 'word_search':
      if (!strings(v.puzzleWordIds) || !strings(v.foundIds) || !isIntIn(v.misses, 0) || typeof v.hintRevealed !== 'boolean') return null;
      return { kind: 'word_search', puzzleWordIds: v.puzzleWordIds, foundIds: v.foundIds, misses: v.misses, hintRevealed: v.hintRevealed };
    case 'word_wheel':
      if (!strings(v.boardWords) || !ints(v.foundSlots) || !strings(v.bonusWords) || !isIntIn(v.hintsUsed, 0)) return null;
      if (v.cluedSlot !== null && !isInt(v.cluedSlot)) return null;
      return {
        kind: 'word_wheel',
        boardWords: v.boardWords,
        foundSlots: v.foundSlots,
        bonusWords: v.bonusWords,
        hintsUsed: v.hintsUsed,
        cluedSlot: v.cluedSlot as number | null,
      };
    default:
      return null;
  }
}

/** Where a saved round picks up: the first question not yet answered. */
export const nextIndexOf = (saved: SavedRound) => saved.answers.length;

/**
 * The dictionary words [saved] was dealt, in order, or null when any has since left the dictionary
 * or the level now asks a different number of questions: the round can then not be rebuilt, and the
 * game deals a fresh one.
 */
export function wordsOfRound<W>(saved: SavedRound, byId: (id: string) => W | undefined, totalQuestions: number): W[] | null {
  if (saved.totalQuestions !== totalQuestions) return null;
  const words: W[] = [];
  for (const id of saved.questionKeys) {
    const word = byId(id);
    if (word === undefined) return null;
    words.push(word);
  }
  return words;
}

/**
 * [saved] if it was left on the grid this level deals now. The same seed deals the same grid, so a
 * save only holds what was found on it; a dictionary update can still change the grid, and its words
 * then no longer match and the level starts over.
 */
export function resumableWordSearch(saved: SavedWordSearch | null, puzzleWordIds: string[]): SavedWordSearch | null {
  if (!saved) return null;
  if (saved.puzzleWordIds.length !== puzzleWordIds.length || saved.puzzleWordIds.some((id, i) => id !== puzzleWordIds[i])) return null;
  if (!saved.foundIds.every((id) => puzzleWordIds.includes(id))) return null;
  return saved;
}

/**
 * [saved]'s progress, fitted to the board this level builds now, or null to start over: the board is
 * rebuilt from the dictionary, so when an update has since changed it the saved words no longer match.
 * Hints are capped at [maxHints], bonus words no longer on the board are dropped, and a clue whose
 * word is already found is not shown again.
 */
export function resumableWordWheel(
  saved: SavedWordWheel | null,
  boardWords: string[],
  bonusKeys: string[],
  maxHints: number
): SavedWordWheel | null {
  if (!saved) return null;
  if (saved.boardWords.length !== boardWords.length || saved.boardWords.some((w, i) => w !== boardWords[i])) return null;
  if (saved.foundSlots.some((s) => s < 0 || s >= boardWords.length)) return null;
  const clued = saved.cluedSlot;
  return {
    ...saved,
    bonusWords: saved.bonusWords.filter((k) => bonusKeys.includes(k)),
    hintsUsed: Math.min(saved.hintsUsed, maxHints),
    cluedSlot: clued !== null && clued >= 0 && clued < boardWords.length && !saved.foundSlots.includes(clued) ? clued : null,
  };
}
