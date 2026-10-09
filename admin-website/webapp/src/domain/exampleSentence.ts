/**
 * Whether a learner's example sentence for a word is fit to send for review, and why not
 * (domain/contribute/ExampleSentence.kt on Android, same messages).
 *
 * Lessons and Sentence Order refuse a sentence under three words, every reader shows the English
 * translation as the prompt, and Fill in the Blank blanks out the word, so it has to be in there. Any
 * recorded form of the word counts, and so does a word built on it. Kasiguranin is judged by a
 * verifier, not here.
 */
import type { WordContent } from './types';

export const EXAMPLE_MIN_WORDS = 3;
export const EXAMPLE_MAX_LENGTH = 300;

/** Lower case, punctuation and accents dropped, so "Bëbbi," and "bebbi" compare equal. */
const plain = (text: string) =>
  text
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Mn}+/gu, '')
    .replace(/[^\p{L}-]/gu, '')
    .replace(/^-+|-+$/g, '');

type FormsOf = Pick<WordContent, 'kasiguranin' | 'rootForm' | 'neutralForm' | 'imperfectiveForm' | 'perfectiveForm' | 'contemplativeForm'>;

function usesWord(word: FormsOf, tokens: string[]): boolean {
  const forms = new Set(
    [word.kasiguranin, word.rootForm, word.neutralForm, word.imperfectiveForm, word.perfectiveForm, word.contemplativeForm]
      .map((f) => plain(f ?? ''))
      .filter((f) => f.length >= 2)
  );
  const head = plain(word.kasiguranin);
  return tokens.map(plain).some((t) => forms.has(t) || (head.length >= 3 && t.includes(head)));
}

export function exampleSentenceProblem(word: FormsOf, sentence: string, translation: string): string | null {
  const tokens = sentence.trim().split(/\s+/).filter(Boolean);
  if (tokens.length < EXAMPLE_MIN_WORDS) return `Write a sentence of at least ${EXAMPLE_MIN_WORDS} words.`;
  if (sentence.trim().length > EXAMPLE_MAX_LENGTH) return `Keep the sentence under ${EXAMPLE_MAX_LENGTH} characters.`;
  if (!usesWord(word, tokens)) return `Use "${word.kasiguranin}" (or one of its forms) in the sentence.`;
  if (translation.trim().length < 2) return 'Add what the sentence means in English.';
  if (translation.trim().length > EXAMPLE_MAX_LENGTH) return `Keep the translation under ${EXAMPLE_MAX_LENGTH} characters.`;
  return null;
}
