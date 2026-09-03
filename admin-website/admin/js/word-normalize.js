// Kasiguranin word normalisation for duplicate comparison.
//
// A direct port of RecallAnswerMatcher.normalise in the Android app
// (app/src/main/java/com/kasiguru/util/RecallAnswerMatcher.kt). The two must stay in step: the app
// warns a contributor that "singët" already exists as "singet", and if this portal then compares with
// a bare toLowerCase() it sees two different strings and approves the duplicate the app just warned
// about. The contributor-facing check and the moderator-facing check have to agree on what "the same
// word" means, or the guard on one side is undone by the other.
//
// Keep this file and RecallAnswerMatcher.normalise identical in behaviour. If the linguists add
// another schwa form, both change.

/** The three codepoints the corpus uses for schwa. All fold to a plain "e". */
const SCHWA_FORMS = new Set(['ë', 'ə', 'ǝ']); // ë ə ǝ

/**
 * Folds a Kasiguranin headword to its comparison form.
 *
 * Trims, lowercases, collapses runs of whitespace to one space, drops hyphens, folds schwa, and
 * decomposes away every remaining diacritic.
 *
 * Hyphens go because the corpus is inconsistent about them - "tël-lën" and "tëllën" are both
 * recorded. Accents go because they mark stress, which is not what "is this the same word" is asking.
 * Marks are stripped generically rather than from a list of accented vowels, because the corpus
 * carries á â é ë í ó ý ś and a hand-written list is always one letter behind the linguists.
 *
 * @param {string} raw
 * @returns {string}
 */
export function normaliseWord(raw) {
  if (typeof raw !== 'string') return '';

  let folded = '';
  for (const ch of raw.trim().toLowerCase()) {
    if (ch === '-') continue;
    if (/\s/.test(ch)) {
      if (folded.length > 0 && folded[folded.length - 1] !== ' ') folded += ' ';
      continue;
    }
    folded += SCHWA_FORMS.has(ch) ? 'e' : ch;
  }

  // NFD splits "é" into "e" + a combining acute, which \p{Mn} then removes. Unicode property escapes
  // need the /u flag; every browser this portal supports has had them since 2018.
  return folded.normalize('NFD').replace(/\p{Mn}+/gu, '').trim();
}

/**
 * True when two headwords are the same word for duplicate purposes.
 *
 * @param {string} a
 * @param {string} b
 * @returns {boolean}
 */
export function isSameWord(a, b) {
  const left = normaliseWord(a);
  return left.length > 0 && left === normaliseWord(b);
}

/**
 * Every entry in `corpus` whose Kasiguranin form matches `word`.
 *
 * Returns all matches rather than the first, because a headword with two senses is a legitimate
 * homonym and the moderator needs to see what is already there before deciding.
 *
 * @param {string} word
 * @param {Array<{kasiguranin?: string}>} corpus
 * @returns {Array<object>}
 */
export function findExistingWord(word, corpus) {
  const target = normaliseWord(word);
  if (!target) return [];
  return (corpus || []).filter((entry) => normaliseWord(entry && entry.kasiguranin) === target);
}
