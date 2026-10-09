import { describe, expect, it } from 'vitest';
import { exampleSentenceProblem } from '../src/domain/exampleSentence';

const forms = { rootForm: '', neutralForm: '', imperfectiveForm: '', perfectiveForm: '', contemplativeForm: '' };
const word = { kasiguranin: 'bëbbi', ...forms };
const verb = { kasiguranin: 'kain', ...forms, perfectiveForm: 'kinain' };

describe('exampleSentenceProblem (same rules as Android)', () => {
  it('accepts a sentence that uses the word, with a translation', () => {
    expect(exampleSentenceProblem(word, 'Mapiya i bëbbi ya.', 'The woman is kind.')).toBeNull();
    expect(exampleSentenceProblem(word, 'Mapiya i bebbi ya', 'The woman is kind.')).toBeNull();
  });

  it('counts a recorded or affixed verb form as the word', () => {
    expect(exampleSentenceProblem(verb, 'Kinain ko i saging', 'I ate the banana.')).toBeNull();
    expect(exampleSentenceProblem(verb, 'Kumakain kami ngayon', 'We are eating now.')).toBeNull();
  });

  it('refuses a sentence without the word, a short one, or no translation', () => {
    expect(exampleSentenceProblem(word, 'Mapiya i lalëkke ya', 'The man is kind.')).toBe('Use "bëbbi" (or one of its forms) in the sentence.');
    expect(exampleSentenceProblem(word, 'bëbbi ya', 'That woman.')).toBe('Write a sentence of at least 3 words.');
    expect(exampleSentenceProblem(word, 'Mapiya i bëbbi ya', ' ')).toBe('Add what the sentence means in English.');
  });
});
