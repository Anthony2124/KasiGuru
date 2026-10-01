import type { WordContent } from '../../domain/types';

/** Case, accents, the schwa and hyphens ignored, so `singët` and `singet` count as one spelling. */
const comparable = (t: string) =>
  t
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Mn}+/gu, '')
    .replace(/-/g, '');

/** GameGloss.tagalogGloss: the Tagalog meaning, or blank when it only repeats the headword. */
export function tagalogGloss(entry: Pick<WordContent, 'tagalog' | 'english' | 'kasiguranin'>): string {
  const gloss = (entry.tagalog || entry.english).trim();
  return comparable(gloss) === comparable(entry.kasiguranin) ? '' : gloss;
}
