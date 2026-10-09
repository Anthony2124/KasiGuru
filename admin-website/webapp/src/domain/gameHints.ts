/**
 * The meaning hints of Word Search and Word Wheel (domain/games/GameHints.kt on Android): a word's
 * simple meaning, one per line. Null when no word has a meaning written, so the hint stays hidden
 * rather than opening onto nothing.
 */

const firstLine = (meaning: string | null | undefined) => (meaning ?? '').split('\n')[0].trim();

/** Word Search: each word still to find, with its meaning. */
export function wordSearchHint(words: [word: string, meaning: string | null | undefined][]): string | null {
  const lines = words.flatMap(([word, meaning]) => (firstLine(meaning) ? [`${word} — ${firstLine(meaning)}`] : []));
  return lines.length ? lines.join('\n') : null;
}

/** Word Wheel: each hidden board word's meaning and length, never the word itself, which is the answer. */
export function wordWheelHint(words: [letters: number, meaning: string | null | undefined][]): string | null {
  const lines = words.flatMap(([letters, meaning]) => (firstLine(meaning) ? [`${letters} letters — ${firstLine(meaning)}`] : []));
  return lines.length ? lines.join('\n') : null;
}
