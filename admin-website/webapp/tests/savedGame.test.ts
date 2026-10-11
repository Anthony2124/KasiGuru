import { describe, expect, it } from 'vitest';
import { nextClueSlot } from '../src/domain/gameHints';
import {
  nextIndexOf,
  parseSavedGame,
  resumableWordSearch,
  resumableWordWheel,
  wordsOfRound,
  type SavedRound,
  type SavedWordSearch,
  type SavedWordWheel,
} from '../src/domain/savedGame';

// Restates SavedGameCodecTest (Android): what a save holds comes back as it was, and a save that
// cannot be resumed is no saved game at all.
const round: SavedRound = {
  kind: 'round',
  questionKeys: ['12', '40', '7'],
  answers: [
    { prompt: 'Bëbbi ya "dalaga".', userAnswer: 'bëbbi\nya', correctAnswer: 'Bëbbi ya dalaga.', isCorrect: false, subPrompt: 'Tagalog: babae' },
    { prompt: 'aso', userAnswer: 'aso', correctAnswer: 'aso', isCorrect: true },
  ],
  score: 1,
  totalQuestions: 5,
  usedHint: true,
  hintRevealed: true,
};

const grid: SavedWordSearch = { kind: 'word_search', puzzleWordIds: ['3', '9', '14'], foundIds: ['14', '3'], misses: 2, hintRevealed: false };

const board: SavedWordWheel = { kind: 'word_wheel', boardWords: ['BALE', 'ALË'], foundSlots: [1], bonusWords: ['LA'], hintsUsed: 2, cluedSlot: 0 };

describe('saved games', () => {
  it('a question round comes back as it was saved', () => {
    expect(parseSavedGame(structuredClone(round))).toEqual(round);
    expect(nextIndexOf(round)).toBe(2);
  });

  it('a missing sub-prompt stays missing rather than becoming empty text', () => {
    const parsed = parseSavedGame(structuredClone(round)) as SavedRound;
    expect(parsed.answers[1].subPrompt).toBeUndefined();
  });

  it('a word search grid and a word wheel board come back as they were saved', () => {
    expect(parseSavedGame(structuredClone(grid))).toEqual(grid);
    expect(parseSavedGame(structuredClone(board))).toEqual(board);
    expect(parseSavedGame({ ...board, cluedSlot: null })).toEqual({ ...board, cluedSlot: null });
  });

  it('unreadable data is no saved game', () => {
    for (const bad of [undefined, null, '', 'not a game', 3, { kind: 'chess' }, { kind: 'word_search', puzzleWordIds: ['1'] }]) {
      expect(parseSavedGame(bad)).toBeNull();
    }
  });

  it('a round that contradicts itself is not resumed', () => {
    // More answers than questions, a score above the answers, a total below the questions.
    for (const bad of [{ ...round, questionKeys: ['12'] }, { ...round, score: 3 }, { ...round, totalQuestions: 2 }]) {
      expect(parseSavedGame(bad)).toBeNull();
    }
  });

  it('a round resumes only while every word is still in the dictionary and the level asks as many', () => {
    const dictionary = new Map([['12', 'a'], ['40', 'b'], ['7', 'c']]);
    expect(wordsOfRound(round, (id) => dictionary.get(id), 5)).toEqual(['a', 'b', 'c']);
    dictionary.delete('40');
    expect(wordsOfRound(round, (id) => dictionary.get(id), 5)).toBeNull();
    expect(wordsOfRound(round, (id) => new Map([['12', 1], ['40', 2], ['7', 3]]).get(id), 10)).toBeNull();
  });

  it('a word search grid resumes only on the grid it was left on', () => {
    expect(resumableWordSearch(grid, ['3', '9', '14'])).toEqual(grid);
    expect(resumableWordSearch(grid, ['3', '14', '9'])).toBeNull();
    expect(resumableWordSearch({ ...grid, foundIds: ['99'] }, ['3', '9', '14'])).toBeNull();
    expect(resumableWordSearch(null, ['3'])).toBeNull();
  });

  it('a word wheel board resumes only while its words match, with its clue and hints fitted', () => {
    expect(resumableWordWheel(board, ['BALE', 'ALË'], ['LA'], 3)).toEqual(board);
    expect(resumableWordWheel(board, ['BALE', 'ALE'], ['LA'], 3)).toBeNull();
    expect(resumableWordWheel({ ...board, foundSlots: [5] }, ['BALE', 'ALË'], ['LA'], 3)).toBeNull();
    // A clue for a word already found is done with; hints never exceed the cap; gone bonus words drop.
    const fitted = resumableWordWheel({ ...board, cluedSlot: 1, hintsUsed: 9 }, ['BALE', 'ALË'], [], 3);
    expect(fitted).toMatchObject({ cluedSlot: null, hintsUsed: 3, bonusWords: [] });
  });
});

describe('Word Wheel clues', () => {
  const slots: [number, string | undefined][] = [
    [5, 'A long word.'],
    [3, undefined],
    [4, 'A house.'],
    [4, 'A dog.'],
  ];

  it('go to the shortest hidden word that has a meaning, the first on the board on a tie', () => {
    expect(nextClueSlot(slots, [])).toBe(2);
    expect(nextClueSlot(slots, [2])).toBe(3);
    expect(nextClueSlot(slots, [2, 3])).toBe(0);
  });

  it('run out when no hidden word has a meaning to give', () => {
    expect(nextClueSlot(slots, [0, 2, 3])).toBeNull();
  });
});
