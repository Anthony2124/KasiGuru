import { describe, expect, it } from 'vitest';
import { wordSearchHint, wordWheelHint } from '../src/domain/gameHints';

describe('game meaning hints', () => {
  it('Word Search lists each hidden word with the first line of its meaning', () => {
    expect(wordSearchHint([['bale', 'A building people live in.\nGusali.'], ['aso', '']])).toBe('bale — A building people live in.');
  });

  it('Word Wheel gives lengths and meanings, never the word', () => {
    expect(wordWheelHint([[4, 'A building people live in.'], [3, undefined]])).toBe('4 letters — A building people live in.');
  });

  it('has no hint when no word has a meaning', () => {
    expect(wordSearchHint([['bale', null]])).toBeNull();
    expect(wordWheelHint([[4, ' ']])).toBeNull();
  });
});
