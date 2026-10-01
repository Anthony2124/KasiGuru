#!/usr/bin/env node
/**
 * Snapshots the public dictionary and stories into public/content/*.json.
 *
 *   node scripts/export-content.mjs            # fail loudly if Firestore cannot be reached
 *   node scripts/export-content.mjs --if-online # keep the committed snapshot when offline (the build uses this)
 *
 * Why a snapshot at all: the Android app ships its corpus inside the APK and only asks Firestore for
 * what changed since (FirestoreSyncManager). A web visitor has no APK, so without this every first
 * visit would read all ~1,250 vocabulary documents, and roughly forty visitors a day would exhaust the
 * Spark plan's 50,000 reads for the whole project - Android users included. With it, a visit costs one
 * query that usually returns nothing: `updatedAt > snapshot`.
 *
 * Reads go through the public REST API with the web API key. `vocabulary` and `stories` are
 * `allow read: if true` in firestore.rules, so no credentials are needed.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const PROJECT = 'kasiguru-86042';
const API_KEY = 'AIzaSyBIADrpzbQZpE4SoHRp9xKsh9A03RLZDlg';
const BASE = `https://firestore.googleapis.com/v1/projects/${PROJECT}/databases/(default)/documents`;

const here = path.dirname(fileURLToPath(import.meta.url));
const outDir = path.resolve(here, '..', 'public', 'content');
const ifOnline = process.argv.includes('--if-online');

// Each export reads every vocabulary document (~1,200 reads). Vercel builds a preview for every
// branch push, so only production builds refresh the snapshot; previews use the committed one.
if (ifOnline && process.env.VERCEL_ENV && process.env.VERCEL_ENV !== 'production') {
  console.log(`content export skipped for a ${process.env.VERCEL_ENV} build; using the committed snapshot`);
  process.exit(0);
}

/** Firestore REST value -> plain JS. */
function plain(v) {
  if (v == null) return null;
  if ('stringValue' in v) return v.stringValue;
  if ('integerValue' in v) return Number(v.integerValue);
  if ('doubleValue' in v) return v.doubleValue;
  if ('booleanValue' in v) return v.booleanValue;
  if ('nullValue' in v) return null;
  if ('timestampValue' in v) return Date.parse(v.timestampValue);
  if ('arrayValue' in v) return (v.arrayValue.values || []).map(plain);
  if ('mapValue' in v) return fields(v.mapValue.fields || {});
  return null;
}
const fields = (f) => Object.fromEntries(Object.entries(f).map(([k, v]) => [k, plain(v)]));

async function readAll(collection) {
  const docs = [];
  let token = '';
  do {
    const url = `${BASE}/${collection}?pageSize=300&key=${API_KEY}${token ? `&pageToken=${token}` : ''}`;
    const res = await fetch(url, { signal: AbortSignal.timeout(30000) });
    if (!res.ok) throw new Error(`${collection}: HTTP ${res.status}`);
    const body = await res.json();
    for (const d of body.documents || []) {
      docs.push({ id: d.name.split('/').pop(), ...fields(d.fields || {}) });
    }
    token = body.nextPageToken || '';
  } while (token);
  return docs;
}

const str = (v) => (typeof v === 'string' ? v : v == null ? '' : String(v)).trim();

/** The same field mapping FirestoreSyncManager / FirestoreSyncRepository apply on Android. */
function toWord(d) {
  const theme = str(d.theme);
  return {
    id: d.id,
    kasiguranin: str(d.kasiguranin),
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

function toStory(d) {
  // readAll spreads the fields over the document name, so a numeric `id` field wins when present.
  const id = Number(d.id);
  return {
    id: Number.isInteger(id) && id > 0 ? id : 0,
    title: str(d.title),
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
 * The stories the APK ships, read out of DatabaseSeeder.getInitialStories().
 *
 * Android shows the seeded set with Firestore laid over it by id (FirestoreSyncManager.syncStories
 * inserts cloud rows with REPLACE and never prunes a seeded id), so the web app has to start from the
 * same ten or it would show only the handful that happen to exist upstream. Parsed from the Kotlin so
 * there is one copy of the text; the result is cached next to this script for builds that only see
 * this folder.
 */
function loadSeedStories() {
  const cache = path.join(here, 'seed-stories.json');
  const kotlin = path.resolve(here, '..', '..', '..', 'app', 'src', 'main', 'java', 'com', 'kasiguru', 'data', 'local', 'DatabaseSeeder.kt');
  if (!fs.existsSync(kotlin)) {
    return fs.existsSync(cache) ? JSON.parse(fs.readFileSync(cache, 'utf8')) : [];
  }
  const src = fs.readFileSync(kotlin, 'utf8');
  const start = src.indexOf('fun getInitialStories()');
  const end = src.indexOf('fun getInitialAchievements()');
  const body = src.slice(start, end);

  const pages = {};
  for (const m of body.matchAll(/val (story\d+Pages) = """([\s\S]*?)"""/g)) {
    pages[m[1]] = JSON.stringify(JSON.parse(m[2]));
  }
  const field = (block, name) => {
    const s = block.match(new RegExp(`${name} = "((?:[^"\\\\]|\\\\.)*)"`));
    if (s) return JSON.parse(`"${s[1]}"`);
    const n = block.match(new RegExp(`${name} = (\\w+)`));
    return n ? n[1] : '';
  };
  const stories = [];
  for (const m of body.matchAll(/StoryEntity\(([\s\S]*?)\n\s*\)/g)) {
    const b = m[1];
    const pagesVar = field(b, 'pagesJson');
    stories.push({
      id: Number(field(b, 'id')),
      title: field(b, 'title'),
      titleKasiguranin: field(b, 'titleKasiguranin'),
      description: field(b, 'description'),
      category: field(b, 'category'),
      pagesJson: pages[pagesVar] || '[]',
      totalPages: Number(field(b, 'totalPages')) || 0,
      requiredXp: Number(field(b, 'requiredXp')) || 0,
      updatedAt: 0,
    });
  }
  fs.writeFileSync(cache, JSON.stringify(stories, null, 1));
  return stories;
}

async function main() {
  const [vocabDocs, storyDocs] = await Promise.all([readAll('vocabulary'), readAll('stories')]);
  const seedStories = loadSeedStories();

  // One row per sense, keyed exactly like VocabularyDao.deleteDuplicateWords: the first document to
  // carry a (headword, English gloss) pair wins, the way the lowest Room id wins on Android.
  const seen = new Set();
  const words = [];
  for (const d of vocabDocs) {
    const w = toWord(d);
    if (!w.kasiguranin) continue;
    const key = `${w.kasiguranin.toLowerCase()} ${w.english.toLowerCase()}`;
    if (seen.has(key)) continue;
    seen.add(key);
    words.push(w);
  }

  // Story documents carry their numeric id in an `id` field (and usually as the document name).
  // Cloud rows replace seeded rows with the same id, exactly as on Android.
  const cloudStories = storyDocs.map(toStory).filter((s) => s.title && s.id > 0);
  const byId = new Map(seedStories.map((s) => [s.id, s]));
  for (const s of cloudStories) byId.set(s.id, s);
  const stories = [...byId.values()].sort((a, b) => a.id - b.id);

  const maxUpdated = (list) => list.reduce((m, x) => Math.max(m, x.updatedAt || 0), 0);
  const meta = {
    generatedAt: Date.now(),
    vocabularyUpdatedAt: maxUpdated(words),
    storiesUpdatedAt: maxUpdated(stories),
    wordCount: words.length,
    storyCount: stories.length,
  };

  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, 'vocabulary.json'), JSON.stringify({ meta, words }));
  fs.writeFileSync(path.join(outDir, 'stories.json'), JSON.stringify({ meta, stories }));
  console.log(`content: ${words.length} words (${vocabDocs.length} docs), ${stories.length} stories`);
}

main().catch((e) => {
  const existing = fs.existsSync(path.join(outDir, 'vocabulary.json'));
  if (ifOnline && existing) {
    console.warn(`content export skipped (${e.message}); keeping the committed snapshot`);
    process.exit(0);
  }
  console.error(`content export failed: ${e.message}`);
  process.exit(1);
});
