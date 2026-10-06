/**
 * The dictionary as an Android phone holds it: the shipping corpus (DatabaseSeeder, carried as
 * public/content/corpus.json by scripts/web/sync-from-app.js) with the cloud's words laid over it.
 *
 * Without the corpus the web app had only the cloud's copy, which lacks about eighty sourced senses
 * the APK ships, so its word counts, word lists and word of the day differed from the phone's.
 */
import type { WordContent } from './types';

/** One seeded sense, as the sync script writes it. */
export interface CorpusWord {
  kasiguranin: string;
  tagalog: string;
  english: string;
  rootForm: string;
  category: string;
  meaningEnglish: string;
  meaningTagalog: string;
  ipaNotation: string;
  phoneticGlottal: boolean;
  phoneticVowelLength: boolean;
}

/** FirestoreSyncManager.senseKey: one sense of a word is its headword and its English gloss. */
export const senseKey = (w: { kasiguranin: string; english: string }) =>
  `${w.kasiguranin.trim().toLowerCase()}\u0000${w.english.trim().toLowerCase()}`;

function fromCorpus(w: CorpusWord, index: number): WordContent {
  return {
    id: `seed:${index + 1}`,
    order: index + 1,
    kasiguranin: w.kasiguranin,
    tagalog: w.tagalog,
    english: w.english,
    rootForm: w.rootForm,
    neutralForm: '',
    imperfectiveForm: '',
    perfectiveForm: '',
    contemplativeForm: '',
    category: w.category || 'General',
    theme: '',
    partOfSpeech: '',
    meaningEnglish: w.meaningEnglish,
    meaningTagalog: w.meaningTagalog,
    audioFileName: '',
    audioUpdatedAt: 0,
    exampleSentence: '',
    exampleTranslation: '',
    exampleSentence2: '',
    exampleTranslation2: '',
    phoneticGlottal: w.phoneticGlottal,
    phoneticVowelLength: w.phoneticVowelLength,
    ipaNotation: w.ipaNotation,
    updatedAt: 0,
  };
}

/**
 * VocabularyContentMerge.merge: the cloud owns what a word is, except the fields no portal control
 * writes (root form, phonetic flags) and the ones older documents omit (theme, meanings, IPA), which
 * fall back to the local value rather than being blanked. Audio moves only when the portal stamped it.
 * The result keeps the local row's place in the list and takes the cloud document's id.
 */
export function mergeWord(local: WordContent | undefined, cloud: WordContent): WordContent {
  if (!local) return cloud;
  return {
    ...local,
    id: cloud.id,
    kasiguranin: cloud.kasiguranin,
    tagalog: cloud.tagalog,
    english: cloud.english,
    rootForm: cloud.rootForm || local.rootForm,
    neutralForm: cloud.neutralForm,
    imperfectiveForm: cloud.imperfectiveForm,
    perfectiveForm: cloud.perfectiveForm,
    contemplativeForm: cloud.contemplativeForm,
    category: cloud.category,
    theme: cloud.theme || local.theme,
    partOfSpeech: cloud.partOfSpeech,
    meaningEnglish: cloud.meaningEnglish || local.meaningEnglish,
    meaningTagalog: cloud.meaningTagalog || local.meaningTagalog,
    audioFileName: cloud.audioUpdatedAt > 0 ? cloud.audioFileName : local.audioFileName,
    audioUpdatedAt: cloud.audioUpdatedAt > 0 ? cloud.audioUpdatedAt : local.audioUpdatedAt,
    exampleSentence: cloud.exampleSentence,
    exampleTranslation: cloud.exampleTranslation,
    exampleSentence2: cloud.exampleSentence2,
    exampleTranslation2: cloud.exampleTranslation2,
    phoneticGlottal: cloud.phoneticGlottal || local.phoneticGlottal,
    phoneticVowelLength: cloud.phoneticVowelLength || local.phoneticVowelLength,
    ipaNotation: cloud.ipaNotation || local.ipaNotation,
    updatedAt: cloud.updatedAt,
  };
}

/**
 * Lays [cloud] over [base] the way a phone's sync does: a cloud word replaces the row with its id or,
 * failing that, its sense, keeping that row's place; a word the list does not have is appended.
 */
export function layOver(base: WordContent[], cloud: WordContent[]): WordContent[] {
  const rows = base.slice();
  const byId = new Map(rows.map((w, i) => [w.id, i]));
  const bySense = new Map(rows.map((w, i) => [senseKey(w), i]));
  let next = rows.reduce((m, w) => Math.max(m, w.order ?? 0), 0);
  for (const w of cloud) {
    const at = byId.get(w.id) ?? bySense.get(senseKey(w));
    if (at === undefined) {
      const row = { ...w, order: w.order ?? ++next };
      byId.set(row.id, rows.length);
      bySense.set(senseKey(row), rows.length);
      rows.push(row);
      continue;
    }
    const merged = mergeWord(rows[at], w);
    byId.delete(rows[at].id);
    bySense.delete(senseKey(rows[at]));
    rows[at] = merged;
    byId.set(merged.id, at);
    bySense.set(senseKey(merged), at);
  }
  return rows;
}

/** The dictionary a phone ends up with: the corpus first, in seeding order, then the cloud over it. */
export function buildDictionary(corpus: CorpusWord[], cloud: WordContent[]): WordContent[] {
  return layOver(corpus.filter((w) => w.kasiguranin).map(fromCorpus), cloud);
}
