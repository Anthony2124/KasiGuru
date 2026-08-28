// fix_firestore_twins.js — bring Firestore's vocabulary documents into line with the
// repaired corpus, without deleting anything.
//
// Usage:
//   node fix_firestore_twins.js <service-account.json> [--dry-run]
//
// Firestore received the same bad import the corpus did, so it still holds rows the corpus has
// already fixed ("kulot kulot / curly") and rows whose headword packs several forms
// ("diget,sabeng"). Neither matches a corpus sense, which means backfill_meanings.js skips them —
// and running that script with --create-missing while they exist would add the *corrected* sense
// alongside the stale one and show a learner both.
//
// This corrects those documents in place rather than deleting them:
//
//   1. Documents matching a row in scripts/repair_dictionary.js get exactly the correction the
//      corpus got. One table, so the two can never drift apart.
//   2. Documents whose headword holds several forms are normalised the way the importer normalises
//      them -- first form, parentheticals stripped -- but only when the result matches a real
//      corpus sense. The alternates are already preserved in scripts/wordlist_notes.json.
//
// Nothing is deleted, nothing is created, and a rewrite that would collide with a document that
// already holds that sense is skipped and reported instead. Every skip is printed, so a document
// this cannot resolve is visible rather than silently left behind.

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

const args = process.argv.slice(2);
const keyPath = args.find((a) => !a.startsWith('--'));
const dryRun = args.includes('--dry-run');

if (!keyPath) {
  console.error('Usage: node fix_firestore_twins.js <service-account.json> [--dry-run]');
  process.exit(1);
}

const { REPAIRS } = require('../scripts/repair_dictionary.js');

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

/** The importer's rule: first form, parentheticals stripped, whitespace collapsed. */
function primaryForm(cell) {
  const parts = String(cell || '').split(/[,;/\n]/).map((p) => p.trim()).filter(Boolean);
  for (const part of parts) {
    const clean = part.replace(/\s*\([^)]*\)\s*/g, ' ').replace(/\s+/g, ' ').trim();
    if (clean) return clean;
  }
  return '';
}

async function main() {
  admin.initializeApp({ credential: admin.credential.cert(keyPath) });
  const db = admin.firestore();

  const corpus = readCorpus();
  const corpusByKey = new Map(corpus.map((r) => [senseKey(r.kasiguranin, r.english), r]));

  const snap = await db.collection('vocabulary').get();
  // Sense keys already taken, so a correction can never land on top of another document.
  const taken = new Map();
  snap.docs.forEach((d) => {
    const x = d.data();
    taken.set(senseKey(x.kasiguranin, x.english), d.id);
  });

  const repairByKey = new Map(REPAIRS.map((r) => [senseKey(r.from[0], r.from[1]), r]));

  const planned = [];
  const collisions = [];
  const unresolved = [];

  for (const doc of snap.docs) {
    const d = doc.data();
    const key = senseKey(d.kasiguranin, d.english);
    if (corpusByKey.has(key)) continue; // already matches the corpus

    let target = null;
    let why = '';

    const repair = repairByKey.get(key);
    if (repair) {
      target = {
        kasiguranin: repair.set.kasiguranin !== undefined ? repair.set.kasiguranin : d.kasiguranin,
        tagalog: repair.set.tagalog !== undefined ? repair.set.tagalog : d.tagalog,
        english: repair.set.english !== undefined ? repair.set.english : d.english,
      };
      if (repair.set.rootForm !== undefined) target.rootForm = repair.set.rootForm;
      if (repair.set.category !== undefined) target.category = repair.set.category;
      why = 'corpus repair';
    } else if (/[,;/\n]|\(/.test(d.kasiguranin || '')) {
      const head = primaryForm(d.kasiguranin);
      const candidate = senseKey(head, d.english);
      if (head && corpusByKey.has(candidate)) {
        const row = corpusByKey.get(candidate);
        target = { kasiguranin: head, tagalog: row.tagalog, english: row.english };
        why = 'multi-form headword normalised';
      }
    }

    if (!target) { unresolved.push({ id: doc.id, k: d.kasiguranin, e: d.english, t: d.tagalog }); continue; }

    const newKey = senseKey(target.kasiguranin, target.english);
    if (newKey === key) continue;
    const holder = taken.get(newKey);
    if (holder && holder !== doc.id) {
      collisions.push({ id: doc.id, from: key, to: newKey });
      continue;
    }

    taken.delete(key);
    taken.set(newKey, doc.id);
    planned.push({ ref: doc.ref, id: doc.id, from: key, target, why });
  }

  console.log('Firestore docs: ' + snap.size);
  console.log('Documents to correct: ' + planned.length);
  planned.forEach((p) => console.log('   ' + p.from + '  ->  ' + senseKey(p.target.kasiguranin, p.target.english) + '   (' + p.why + ')'));

  if (collisions.length) {
    console.log('\nSkipped, another document already holds the corrected sense (' + collisions.length + '):');
    collisions.forEach((c) => console.log('   ' + c.from + '  ->  ' + c.to));
  }
  if (unresolved.length) {
    console.log('\nLeft alone, not a corpus sense - community additions and anything this cannot resolve (' + unresolved.length + '):');
    unresolved.forEach((u) => console.log('   ' + u.k + '  |  ' + u.t + '  |  ' + u.e));
  }

  if (dryRun) { console.log('\nDry run, nothing written.'); return; }
  if (!planned.length) { console.log('\nNothing to do.'); return; }

  let done = 0;
  for (let i = 0; i < planned.length; i += BATCH_LIMIT) {
    const batch = db.batch();
    planned.slice(i, i + BATCH_LIMIT).forEach((p) => {
      // A document that never had a tagalog or english field yields undefined here, which the
      // client rejects outright. Absent stays absent rather than being written as a blank.
      const patch = { updatedAt: Date.now() };
      for (const [k, v] of Object.entries(p.target)) if (v !== undefined) patch[k] = v;
      batch.update(p.ref, patch);
    });
    await batch.commit();
    done += Math.min(BATCH_LIMIT, planned.length - i);
    console.log('  corrected ' + done + '/' + planned.length);
  }
  console.log('\nDone. Re-run audit_firestore_corpus.js to confirm, then backfill_meanings.js.');
}

main().catch((err) => { console.error(err); process.exit(1); });
