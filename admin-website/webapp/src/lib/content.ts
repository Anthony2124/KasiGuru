/**
 * The dictionary and stories.
 *
 * Starts from the snapshot baked in at build time (public/content/*.json, see
 * scripts/export-content.mjs) and asks Firestore only for documents stamped after it - the same
 * `updatedAt > lastSync` pull FirestoreSyncManager does, throttled to once every six hours for the
 * same reason: the Spark plan's 50,000 reads a day are shared by every learner on both apps.
 */
import { collection, getDocs, query, where } from 'firebase/firestore/lite';
import { buildDictionary, layOver, type CorpusWord } from '../domain/contentMerge';
import type { Story, WordContent } from '../domain/types';
import { db } from './firebase';
import { load, save } from './persist';
import { getState, setState } from './store';

interface Snapshot<T> {
  meta: { generatedAt: number; vocabularyUpdatedAt: number; storiesUpdatedAt: number };
  words?: T[];
  stories?: T[];
}

interface Delta {
  words: WordContent[];
  stories: Story[];
  vocabSince: number;
  storiesSince: number;
  checkedAt: number;
}

const DELTA_KEY = 'content-delta:v1';
const MIN_SYNC_INTERVAL_MS = 6 * 60 * 60 * 1000;

let snapshotMeta = { vocabularyUpdatedAt: 0, storiesUpdatedAt: 0 };

const str = (v: unknown) => (typeof v === 'string' ? v : v == null ? '' : String(v)).trim();

/** FirestoreSyncManager's field mapping, which is also what the export script writes. */
export function wordFromDoc(id: string, d: Record<string, unknown>): WordContent | null {
  const kasiguranin = str(d.kasiguranin);
  if (!kasiguranin) return null;
  const theme = str(d.theme);
  return {
    id,
    kasiguranin,
    tagalog: str(d.tagalog),
    english: str(d.english),
    rootForm: str(d.rootForm),
    neutralForm: str(d.neutralForm),
    imperfectiveForm: str(d.imperfectiveForm),
    perfectiveForm: str(d.perfectiveForm),
    contemplativeForm: str(d.contemplativeForm),
    category: str(d.category) || 'General',
    theme: theme === 'nan' ? '' : theme,
    partOfSpeech: str(d.partOfSpeech),
    meaningEnglish: str(d.meaningEnglish),
    meaningTagalog: str(d.meaningTagalog),
    audioFileName: str(d.audioResName),
    audioUpdatedAt: Number(d.audioUpdatedAt) || 0,
    exampleSentence: str(d.exampleSentence),
    exampleTranslation: str(d.exampleTranslation),
    exampleSentence2: str(d.exampleSentence2),
    exampleTranslation2: str(d.exampleTranslation2),
    phoneticGlottal: d.phoneticGlottal === true,
    phoneticVowelLength: d.phoneticVowelLength === true,
    ipaNotation: str(d.ipaNotation),
    updatedAt: Number(d.updatedAt) || 0,
  };
}

function storyFromDoc(d: Record<string, unknown>): Story | null {
  const id = Number(d.id);
  const title = str(d.title);
  if (!Number.isInteger(id) || id <= 0 || !title) return null;
  return {
    id,
    title,
    titleKasiguranin: str(d.titleKasiguranin),
    description: str(d.description),
    category: str(d.category),
    pagesJson: str(d.pagesJson) || '[]',
    totalPages: Number(d.totalPages) || 0,
    requiredXp: Number(d.requiredXp) || 0,
    updatedAt: Number(d.updatedAt) || 0,
  };
}

/**
 * Lays changed documents over the dictionary the way a phone's sync does (contentMerge.layOver): a
 * changed word is merged into the row with its id or sense, keeping that row's place, or is added.
 */
function overlay(words: WordContent[], stories: Story[], delta: Delta | undefined) {
  if (!delta) return { words, stories };
  const storiesById = new Map(stories.map((s) => [s.id, s]));
  for (const s of delta.stories) storiesById.set(s.id, s);
  return { words: layOver(words, delta.words), stories: [...storiesById.values()].sort((a, b) => a.id - b.id) };
}

export async function loadContent() {
  const [corpus, vocab, stories, delta] = await Promise.all([
    fetch('/content/corpus.json').then((r) => r.json() as Promise<{ words?: CorpusWord[] }>),
    fetch('/content/vocabulary.json').then((r) => r.json() as Promise<Snapshot<WordContent>>),
    fetch('/content/stories.json').then((r) => r.json() as Promise<Snapshot<Story>>),
    load<Delta>(DELTA_KEY),
  ]);
  snapshotMeta = vocab.meta;
  // A delta older than a newer snapshot has already been folded into it.
  const usable =
    delta && delta.vocabSince >= vocab.meta.vocabularyUpdatedAt ? delta : undefined;
  // The APK's shipping corpus with the cloud's snapshot over it, as a phone holds the dictionary.
  const dictionary = buildDictionary(corpus.words ?? [], vocab.words ?? []);
  const merged = overlay(dictionary, stories.stories ?? [], usable);
  setState((s) => ({ words: merged.words, stories: merged.stories, contentReady: true, contentVersion: s.contentVersion + 1 }));
}

/** Pulls documents changed since the snapshot (or the last pull). Cheap: usually zero results. */
export async function refreshContent(force = false) {
  if (!navigator.onLine) return;
  const delta: Delta = (await load<Delta>(DELTA_KEY)) ?? {
    words: [],
    stories: [],
    vocabSince: 0,
    storiesSince: 0,
    checkedAt: 0,
  };
  if (delta.vocabSince < snapshotMeta.vocabularyUpdatedAt) {
    // The snapshot moved past this delta (a new deploy): start again from the snapshot.
    delta.words = [];
    delta.stories = [];
    delta.vocabSince = snapshotMeta.vocabularyUpdatedAt;
    delta.storiesSince = snapshotMeta.storiesUpdatedAt;
    delta.checkedAt = 0;
  }
  const now = Date.now();
  if (!force && delta.checkedAt > 0 && now - delta.checkedAt >= 0 && now - delta.checkedAt < MIN_SYNC_INTERVAL_MS) return;

  try {
    const [vSnap, sSnap] = await Promise.all([
      getDocs(query(collection(db, 'vocabulary'), where('updatedAt', '>', delta.vocabSince))),
      getDocs(query(collection(db, 'stories'), where('updatedAt', '>', delta.storiesSince))),
    ]);
    const words = vSnap.docs.map((d) => wordFromDoc(d.id, d.data())).filter((w): w is WordContent => !!w);
    const stories = sSnap.docs.map((d) => storyFromDoc(d.data())).filter((s): s is Story => !!s);

    const wordMap = new Map(delta.words.map((w) => [w.id, w]));
    for (const w of words) wordMap.set(w.id, w);
    const storyMap = new Map(delta.stories.map((s) => [s.id, s]));
    for (const s of stories) storyMap.set(s.id, s);

    const next: Delta = {
      words: [...wordMap.values()],
      stories: [...storyMap.values()],
      vocabSince: Math.max(delta.vocabSince, ...words.map((w) => w.updatedAt)),
      storiesSince: Math.max(delta.storiesSince, ...stories.map((s) => s.updatedAt)),
      checkedAt: now,
    };
    await save(DELTA_KEY, next);
    if (words.length || stories.length) {
      const st = getState();
      const merged = overlay(st.words, st.stories, { ...next, words, stories });
      setState((s) => ({ words: merged.words, stories: merged.stories, contentVersion: s.contentVersion + 1 }));
    }
  } catch (e) {
    console.warn('content refresh failed', e);
  }
}

export function parsePages(story: Story) {
  try {
    const pages = JSON.parse(story.pagesJson);
    return Array.isArray(pages) ? pages : [];
  } catch {
    return [];
  }
}
