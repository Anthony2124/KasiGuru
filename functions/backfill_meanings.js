// backfill_meanings.js — push the corpus's definitions, parts of speech and any missing
// senses into the Firestore `vocabulary` collection.
//
// Usage:
//   node backfill_meanings.js <service-account.json> [--dry-run] [--create-missing]
//
// Runs as a local Node script with a service-account key, not as a Cloud Function: this
// project stays on the Firebase Spark plan.
//
// Behavior:
//   - Reads DatabaseSeeder.kt as the source of truth for the corpus.
//   - Matches Firestore docs on kasiguranin + english, the same sense key the app uses, so a
//     homonym is not written over its twin.
//   - Writes meaningEnglish, meaningTagalog and partOfSpeech, and stamps updatedAt so the
//     app's incremental sync actually picks the change up.
//   - Never blanks a field: an empty value in the corpus leaves the document alone.
//   - Reports Firestore docs the corpus does not account for, rather than skipping silently.
//   - --create-missing adds corpus senses that have no document yet. Off by default, because
//     creating a thousand documents is not something to do as a side effect of a backfill.

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

const args = process.argv.slice(2);
const keyPath = args.find((a) => !a.startsWith('--'));
const dryRun = args.includes('--dry-run');
const createMissing = args.includes('--create-missing');

if (!keyPath) {
  console.error('Usage: node backfill_meanings.js <service-account.json> [--dry-run] [--create-missing]');
  process.exit(1);
}

const SEEDER = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'kasiguru', 'data', 'local', 'DatabaseSeeder.kt');
const ENTITY = /VocabularyEntity\(([\s\S]*?)\n {8}\)/g;
const FIELD = /(\w+)\s*=\s*"([^"]*)"/g;
const BATCH_LIMIT = 450;

const norm = (s) => String(s || '').trim().toLowerCase();
const senseKey = (k, e) => norm(k) + '|' + norm(e);

function readCorpus() {
  const src = fs.readFileSync(SEEDER, 'utf8');
  const seg = src.slice(src.indexOf('fun getInitialVocabulary'), src.indexOf('fun getInitialStories'));
  const rows = [];
  for (const m of seg.matchAll(ENTITY)) {
    const f = {};
    for (const p of m[1].matchAll(FIELD)) if (f[p[1]] === undefined) f[p[1]] = p[2];
    if (f.kasiguranin) rows.push(f);
  }
  return rows;
}

async function main() {
  admin.initializeApp({ credential: admin.credential.cert(keyPath) });
  const db = admin.firestore();

  const corpus = readCorpus();
  const byKey = new Map(corpus.map((r) => [senseKey(r.kasiguranin, r.english), r]));
  console.log('Corpus senses: ' + corpus.length);

  const snapshot = await db.collection('vocabulary').get();
  console.log('Firestore docs: ' + snapshot.size);

  const seenKeys = new Set();
  const updates = [];
  const unaccounted = [];

  for (const doc of snapshot.docs) {
    const d = doc.data();
    const key = senseKey(d.kasiguranin, d.english);
    const row = byKey.get(key);
    if (!row) { unaccounted.push((d.kasiguranin || '?') + ' / ' + (d.english || '?')); continue; }
    seenKeys.add(key);

    const patch = {};
    // Only fields the corpus actually has, and only where the document differs. Writing a
    // blank would clear a definition a moderator wrote in the portal.
    for (const field of ['meaningEnglish', 'meaningTagalog', 'partOfSpeech']) {
      const value = (row[field] || '').trim();
      if (value && d[field] !== value) patch[field] = value;
    }
    if (Object.keys(patch).length === 0) continue;

    patch.updatedAt = Date.now();
    updates.push({ ref: doc.ref, patch, label: key });
  }

  const missing = [...byKey.keys()].filter((k) => !seenKeys.has(k));

  console.log('Docs to update:                  ' + updates.length);
  console.log('Corpus senses with no document:  ' + missing.length + (createMissing ? '  (will be created)' : '  (pass --create-missing to add them)'));
  console.log('Firestore docs not in the corpus: ' + unaccounted.length);
  unaccounted.slice(0, 30).forEach((u) => console.log('    ' + u));
  if (unaccounted.length > 30) console.log('    ... and ' + (unaccounted.length - 30) + ' more');

  if (dryRun) { console.log('\nDry run, nothing written.'); return; }

  let written = 0;
  for (let i = 0; i < updates.length; i += BATCH_LIMIT) {
    const batch = db.batch();
    updates.slice(i, i + BATCH_LIMIT).forEach((u) => batch.update(u.ref, u.patch));
    await batch.commit();
    written += Math.min(BATCH_LIMIT, updates.length - i);
    console.log('  updated ' + written + '/' + updates.length);
  }

  if (createMissing && missing.length) {
    let created = 0;
    for (let i = 0; i < missing.length; i += BATCH_LIMIT) {
      const batch = db.batch();
      for (const key of missing.slice(i, i + BATCH_LIMIT)) {
        const row = byKey.get(key);
        const now = Date.now();
        batch.set(db.collection('vocabulary').doc(), {
          kasiguranin: row.kasiguranin,
          tagalog: row.tagalog || null,
          english: row.english || null,
          rootForm: row.rootForm || row.kasiguranin,
          category: row.category || 'General',
          partOfSpeech: row.partOfSpeech || null,
          meaningEnglish: row.meaningEnglish || null,
          meaningTagalog: row.meaningTagalog || null,
          ipaNotation: row.ipaNotation || null,
          neutralForm: row.neutralForm || null,
          imperfectiveForm: row.imperfectiveForm || null,
          perfectiveForm: row.perfectiveForm || null,
          contemplativeForm: row.contemplativeForm || null,
          createdAt: now,
          updatedAt: now
        });
      }
      await batch.commit();
      created += Math.min(BATCH_LIMIT, missing.length - i);
      console.log('  created ' + created + '/' + missing.length);
    }
  }

  console.log('\nDone.');
}

main().catch((err) => { console.error(err); process.exit(1); });
