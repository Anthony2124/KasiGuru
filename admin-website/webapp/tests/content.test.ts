import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { buildDictionary, layOver, mergeWord, senseKey, withdrawnSenses, withoutRemoved, type CorpusWord } from '../src/domain/contentMerge';
import type { WordContent } from '../src/domain/types';
import { Corpus } from '../src/domain/corpus';

const seed = (kasiguranin: string, english: string, extra: Partial<CorpusWord> = {}): CorpusWord => ({
  kasiguranin, tagalog: 't', english, rootForm: kasiguranin, category: 'Food & Dining', meaningEnglish: 'seeded meaning',
  meaningTagalog: '', ipaNotation: '', phoneticGlottal: false, phoneticVowelLength: false, ...extra,
});
const cloud = (id: string, kasiguranin: string, english: string, extra: Partial<WordContent> = {}): WordContent => ({
  id, kasiguranin, tagalog: 't', english, rootForm: '', neutralForm: '', imperfectiveForm: '', perfectiveForm: '',
  contemplativeForm: '', category: 'Food & Dining', theme: '', partOfSpeech: '', meaningEnglish: '', meaningTagalog: '',
  audioFileName: '', audioUpdatedAt: 0, exampleSentence: '', exampleTranslation: '', exampleSentence2: '',
  exampleTranslation2: '', phoneticGlottal: false, phoneticVowelLength: false, ipaNotation: '', updatedAt: 5, ...extra,
});

describe('dictionary as a phone holds it (VocabularyContentMerge)', () => {
  it('keeps the corpus in seeding order and appends cloud-only words after it', () => {
    const d = buildDictionary([seed('asen', 'salt'), seed('atong', 'vegetable')], [cloud('c9', 'bago', 'new')]);
    expect(d.map((w) => [w.kasiguranin, w.order])).toEqual([['asen', 1], ['atong', 2], ['bago', 3]]);
  });
  it('a cloud word for a seeded sense merges into that row: its place, the cloud id, guarded fields kept', () => {
    const d = buildDictionary(
      [seed('asen', 'salt', { phoneticGlottal: true })],
      [cloud('c1', 'asen', 'Salt ', { theme: 'pagkain' })]
    );
    expect(d).toHaveLength(1);
    expect(d[0]).toMatchObject({ id: 'c1', order: 1, theme: 'pagkain', rootForm: 'asen', meaningEnglish: 'seeded meaning', phoneticGlottal: true });
  });
  it('homonyms stay separate senses (lima "five" and lima "hand")', () => {
    const d = buildDictionary([seed('lima', 'hand')], [cloud('c2', 'lima', 'five')]);
    expect(d.map((w) => w.english)).toEqual(['hand', 'five']);
  });
  it('audio moves only when the portal stamped it', () => {
    const local = mergeWord(undefined, cloud('c3', 'apog', 'lime', { audioFileName: 'a.m4a', audioUpdatedAt: 9 }));
    expect(mergeWord(local, cloud('c3', 'apog', 'lime')).audioFileName).toBe('a.m4a');
    expect(mergeWord(local, cloud('c3', 'apog', 'lime', { audioUpdatedAt: 10 })).audioFileName).toBe('');
  });
  it('a later edit replaces by id and keeps the row in place', () => {
    const d = buildDictionary([seed('asen', 'salt'), seed('atong', 'vegetable')], [cloud('c1', 'asen', 'salt')]);
    const next = layOver(d, [cloud('c1', 'asen', 'salt', { tagalog: 'asin' })]);
    expect(next.map((w) => [w.id, w.tagalog])).toEqual([['c1', 'asin'], ['seed:2', 't']]);
    expect(senseKey(next[0])).toBe(senseKey({ kasiguranin: 'ASEN ', english: 'salt' }));
  });
});

describe('the shipped content files', () => {
  const read = (f: string) => JSON.parse(fs.readFileSync(path.resolve(__dirname, '../public/content', f), 'utf8'));
  const corpus = read('corpus.json');
  const vocab = read('vocabulary.json');
  const dictionary = buildDictionary(corpus.words, vocab.words);

  it('carries the whole shipping corpus', () => {
    expect(corpus.words).toHaveLength(corpus.meta.count);
    // A floor, not the count: 1,100 since the dictionary's withdrawn senses left the corpus.
    expect(corpus.words.length).toBeGreaterThan(1000);
  });
  it('the dictionary has every cloud sense and every corpus sense the cloud kept, once each', () => {
    const withdrawn = withdrawnSenses(corpus.words, vocab.words);
    expect(withdrawn).not.toBeNull();
    const senses = new Set([...corpus.words.map(senseKey).filter((k: string) => !withdrawn!.has(k)), ...vocab.words.map(senseKey)]);
    expect(new Set(dictionary.map(senseKey))).toEqual(senses);
    expect(dictionary).toHaveLength(senses.size);
    expect(new Corpus(dictionary, {}).all.length).toBe(dictionary.length);
  });
});

describe('words deleted or re-glossed in the portal (FirestoreSyncManager.pruneWithdrawnWords)', () => {
  const corpus = Array.from({ length: 20 }, (_, i) => seed(`word${i + 1}`, 'gloss'));
  const upstream = corpus.map((w, i) => cloud(`c${i + 1}`, w.kasiguranin, w.english));

  it('a shipped word the cloud lacks is left out, and every other word keeps its id and place', () => {
    const d = buildDictionary([...corpus.slice(0, 5), seed('abaga', 'shoulder'), ...corpus.slice(5)], upstream);
    expect(d.map((w) => w.kasiguranin)).not.toContain('abaga');
    expect(d).toHaveLength(20);
    expect(d[5]).toMatchObject({ kasiguranin: 'word6', order: 7 });
  });
  it('a re-glossed word keeps only its new sense', () => {
    const d = buildDictionary([...corpus, seed('masena', 'jealou')], [...upstream, cloud('c99', 'masena', 'jealous')]);
    expect(d.filter((w) => w.kasiguranin === 'masena').map((w) => w.english)).toEqual(['jealous']);
  });
  it('a cloud missing over a tenth of the shipped words withdraws none of them', () => {
    expect(withdrawnSenses(corpus, upstream.slice(0, 15))).toBeNull();
    expect(buildDictionary(corpus, upstream.slice(0, 15))).toHaveLength(20);
    expect(withdrawnSenses(corpus, upstream.slice(0, 18))?.size).toBe(2);
  });
  it('a word removed after the snapshot goes, unless it was saved again since', () => {
    const d = buildDictionary([...corpus, seed('abaga', 'shoulder')], [...upstream, cloud('c50', 'abaga', 'shoulder', { updatedAt: 100 })]);
    const removed = { [senseKey({ kasiguranin: 'abaga', english: 'shoulder' })]: 200 };
    expect(withoutRemoved(d, removed).map((w) => w.kasiguranin)).not.toContain('abaga');
    // Restored from the Recycle bin: a fresh updatedAt, later than the removal.
    const restored = layOver(d, [cloud('c50', 'abaga', 'shoulder', { updatedAt: 300 })]);
    expect(withoutRemoved(restored, removed).map((w) => w.kasiguranin)).toContain('abaga');
  });
});

describe('My words (WordEncounterRepository)', () => {
  it('records each word once per showing, keeping the first time and the latest source', async () => {
    const { Draft, initialLearner } = await import('../src/domain/learner');
    const d = new Draft(initialLearner());
    d.met(['w1', 'w2', 'w1'], 'lesson', 100);
    d.met(['w1'], 'word_match', 200);
    expect(d.d.encounters?.w1).toEqual({ firstSeenAt: 100, lastSeenAt: 200, timesSeen: 2, lastSource: 'word_match' });
    expect(d.d.encounters?.w2?.timesSeen).toBe(1);
  });
  it('fills from history without touching words already met', async () => {
    const { Draft, initialLearner, UNKNOWN_TIME } = await import('../src/domain/learner');
    const d = new Draft(initialLearner());
    d.met(['w1'], 'review', 50);
    d.fillMet(['w1', 'w9'], [{ at: 300, ids: ['w2'] }, { at: 400, ids: ['w2'] }]);
    expect(d.d.encounters?.w1?.lastSeenAt).toBe(50);
    expect(d.d.encounters?.w9).toMatchObject({ lastSeenAt: UNKNOWN_TIME, lastSource: 'review' });
    expect(d.d.encounters?.w2).toMatchObject({ firstSeenAt: 300, lastSeenAt: 400, timesSeen: 2, lastSource: 'lesson' });
  });
});
