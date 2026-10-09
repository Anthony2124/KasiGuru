#!/usr/bin/env node
'use strict';

/**
 * Copies every word pronunciation clip out of Firestore into the APK, so a recording plays offline
 * and does not depend on the Spark plan's daily read quota.
 *
 *   node scripts/audio/export-word-audio.js            download new and changed clips
 *   node scripts/audio/export-word-audio.js --dry-run  report what would change, write nothing
 *
 * Firestore stays the source: the admin portal uploads to `word_audio/{key}` and stamps the word's
 * `audioResName` / `audioUpdatedAt`. This script reads those pointers from `vocabulary`, then fetches
 * only the clips whose version differs from the bundled copy, so a re-run costs one read per
 * vocabulary document plus one per changed clip. Both collections are `allow read: if true`, so the
 * public web API key is enough. Nothing is written to Firestore.
 *
 * Output, in app/src/main/assets/word_audio/:
 *   <key>.<audioUpdatedAt>.<ext>   one file per clip; the version in the name lets the web app cache
 *                                  it forever and makes a re-recording a new file
 *   manifest.json                  { generatedAt, clips: { <key>: { v, file, bytes } } }
 *
 * `npm run sync:web` then copies the folder to the web app. A word re-recorded after the export
 * still plays everywhere: both apps fetch the newer clip from Firestore and fall back to the
 * bundled one when that fails.
 */

const fs = require('node:fs');
const path = require('node:path');
const { WORD_AUDIO_ASSETS } = require('../lib/project-paths');

const PROJECT = 'kasiguru-86042';
const API_KEY = 'AIzaSyBIADrpzbQZpE4SoHRp9xKsh9A03RLZDlg';
const ROOT = `projects/${PROJECT}/databases/(default)/documents`;
const BASE = `https://firestore.googleapis.com/v1/${ROOT}`;
const MANIFEST = path.join(WORD_AUDIO_ASSETS, 'manifest.json');
const BATCH = 50;

/** A key becomes a file name and a URL path segment, so only the admin portal's slug characters. */
const SAFE_KEY = /^[A-Za-z0-9_-]+$/;

const EXT = [
  [/mp4|m4a|aac/, 'm4a'],
  [/mpeg|mp3/, 'mp3'],
  [/ogg|opus/, 'ogg'],
  [/wav/, 'wav'],
  [/webm/, 'webm'],
  [/3gp/, '3gp'],
];
const extFor = (mime) => EXT.find(([re]) => re.test(String(mime).toLowerCase()))?.[1] ?? 'm4a';

async function getJson(url, init) {
  const res = await fetch(url, { ...init, signal: AbortSignal.timeout(60000) });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${body.error?.message ?? 'request failed'}`);
  return body;
}

/** The newest clip version each `word_audio` key is pointed at by a vocabulary document. */
async function audioPointers() {
  const pointers = new Map();
  let token = '';
  let read = 0;
  do {
    const mask = 'mask.fieldPaths=audioResName&mask.fieldPaths=audioUpdatedAt';
    const body = await getJson(`${BASE}/vocabulary?pageSize=300&${mask}&key=${API_KEY}${token ? `&pageToken=${token}` : ''}`);
    for (const d of body.documents || []) {
      read++;
      const key = (d.fields?.audioResName?.stringValue || '').trim();
      const v = Number(d.fields?.audioUpdatedAt?.integerValue ?? d.fields?.audioUpdatedAt?.doubleValue ?? 0) || 0;
      if (!key) continue;
      if (!SAFE_KEY.test(key)) {
        console.warn(`skipped "${key}": not a file-safe key; it will keep playing from Firestore`);
        continue;
      }
      pointers.set(key, Math.max(v, pointers.get(key) ?? 0));
    }
    token = body.nextPageToken || '';
  } while (token);
  return { pointers, read };
}

/** `word_audio` documents by key; a missing document maps to null. */
async function fetchClips(keys) {
  const out = new Map();
  for (let i = 0; i < keys.length; i += BATCH) {
    const chunk = keys.slice(i, i + BATCH);
    const rows = await getJson(`${BASE}:batchGet?key=${API_KEY}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ documents: chunk.map((k) => `${ROOT}/word_audio/${k}`) }),
    });
    for (const row of rows) {
      if (row.missing) { out.set(row.missing.split('/').pop(), null); continue; }
      const d = row.found;
      const key = d.name.split('/').pop();
      const b64 = d.fields?.data?.bytesValue;
      out.set(key, b64 ? { bytes: Buffer.from(b64, 'base64'), mime: d.fields?.mimeType?.stringValue || 'audio/mp4' } : null);
    }
    process.stdout.write(`\rfetched ${Math.min(i + BATCH, keys.length)}/${keys.length} clips`);
  }
  if (keys.length) process.stdout.write('\n');
  return out;
}

function readManifest() {
  try {
    return JSON.parse(fs.readFileSync(MANIFEST, 'utf8')).clips || {};
  } catch {
    return {};
  }
}

async function main() {
  const dryRun = process.argv.includes('--dry-run');
  const previous = readManifest();
  const { pointers, read } = await audioPointers();

  const changed = [...pointers].filter(([key, v]) => {
    const had = previous[key];
    return !had || had.v !== v || !fs.existsSync(path.join(WORD_AUDIO_ASSETS, had.file));
  }).map(([key]) => key);
  const removed = Object.keys(previous).filter((key) => !pointers.has(key));

  console.log(`${read} vocabulary documents read; ${pointers.size} words have a clip.`);
  console.log(`${changed.length} clip(s) to download, ${removed.length} to remove.`);
  if (dryRun) return;

  fs.mkdirSync(WORD_AUDIO_ASSETS, { recursive: true });
  const fetched = await fetchClips(changed);
  const clips = { ...previous };
  const drop = (key) => {
    const had = clips[key];
    if (had) fs.rmSync(path.join(WORD_AUDIO_ASSETS, had.file), { force: true });
    delete clips[key];
  };
  for (const key of removed) drop(key);
  let missing = 0;
  for (const key of changed) {
    const clip = fetched.get(key);
    if (!clip || !clip.bytes.length) {
      // The word points at a clip that is not there; leave whatever was bundled before.
      missing++;
      continue;
    }
    drop(key);
    const v = pointers.get(key);
    const file = `${key}.${v}.${extFor(clip.mime)}`;
    fs.writeFileSync(path.join(WORD_AUDIO_ASSETS, file), clip.bytes);
    clips[key] = { v, file, bytes: clip.bytes.length };
  }

  const sorted = Object.fromEntries(Object.keys(clips).sort().map((k) => [k, clips[k]]));
  fs.writeFileSync(MANIFEST, JSON.stringify({ generatedAt: Date.now(), clips: sorted }, null, 1) + '\n');
  const total = Object.values(sorted).reduce((n, c) => n + c.bytes, 0);
  console.log(`${Object.keys(sorted).length} clips bundled (${(total / 1048576).toFixed(1)} MB).` +
    (missing ? ` ${missing} word(s) point at a clip that does not exist in word_audio.` : ''));
  console.log('Next: npm run sync:web, so the web app serves the same clips.');
}

main().catch((e) => {
  console.error(e.message || e);
  process.exit(1);
});
