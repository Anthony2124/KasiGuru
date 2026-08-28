// audit_firestore_corpus.js — read-only. Compares the Firestore `vocabulary`
// collection against the corpus in DatabaseSeeder.kt and sorts every document
// the corpus does not account for into three buckets.
//
// Usage:
//   node audit_firestore_corpus.js <service-account.json> [out.json]
//
// The buckets matter because they need opposite treatment:
//
//   CORRUPTED TWINS  A document carrying a row the corpus has already repaired
//                    ("kulot kulot / curly" where the corpus now says
//                    "kulot / curly hair"). The corrected sense has no document,
//                    so creating one would leave the learner seeing both. These
//                    should be corrected or deleted in the portal.
//   MULTI-FORM       One document whose headword holds several Kasiguranin forms
//                    ("diget,sabeng"). The corpus splits these; the document does not.
//   GENUINE          A word the corpus simply does not have — community
//                    submissions and admin additions. These stay, and need
//                    definitions written for them in the portal.
//
// Writes nothing to Firestore.

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

const [keyPath, outPath] = process.argv.slice(2);
if (!keyPath) {
  console.error('Usage: node audit_firestore_corpus.js <service-account.json> [out.json]');
  process.exit(1);
}

const SEEDER = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'kasiguru', 'data', 'local', 'DatabaseSeeder.kt');
const ENTITY = /VocabularyEntity\(([\s\S]*?)\n {8}\)/g;
const FIELD = /(\w+)\s*=\s*"([^"]*)"/g;

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
  const corpusHeads = new Set(corpus.map((r) => norm(r.kasiguranin)));
  const corpusKeys = new Set(corpus.map((r) => senseKey(r.kasiguranin, r.english)));

  const snap = await db.collection('vocabulary').get();
  const corrupt = [];
  const multi = [];
  const genuine = [];

  for (const doc of snap.docs) {
    const d = doc.data();
    if (corpusKeys.has(senseKey(d.kasiguranin, d.english))) continue;
    const row = { id: doc.id, kasiguranin: d.kasiguranin || '', tagalog: d.tagalog || '', english: d.english || '' };

    const tokens = norm(row.kasiguranin).split(/\s+/).filter(Boolean);
    const repeated = tokens.length > 1 && new Set(tokens).size < tokens.length;
    const strayParen = /[()]/.test(row.kasiguranin);
    const trailingComma = /,\s*$/.test(row.english) || /,\s*$/.test(row.tagalog);
    const phraseOfKnownWords = tokens.length > 1 && tokens.some((t) => corpusHeads.has(t));

    if (/[,;/]/.test(row.kasiguranin)) multi.push(row);
    else if (repeated || strayParen || trailingComma || phraseOfKnownWords) corrupt.push(row);
    else genuine.push(row);
  }

  const show = (title, rows) => {
    console.log('\n== ' + title + ' (' + rows.length + ') ==');
    rows.forEach((r) => console.log('   ' + r.kasiguranin + '  |  ' + r.tagalog + '  |  ' + r.english));
  };

  console.log('Corpus senses: ' + corpus.length + '    Firestore docs: ' + snap.size);
  show('CORRUPTED TWINS - the corpus has repaired these; the documents are stale', corrupt);
  show('MULTI-FORM - one document holding several Kasiguranin forms', multi);
  show('GENUINE - not in the corpus at all; needs definitions written in the portal', genuine);
  console.log('\ncorrupt=' + corrupt.length + '  multi=' + multi.length + '  genuine=' + genuine.length);

  if (outPath) {
    fs.writeFileSync(outPath, JSON.stringify({ corrupt, multi, genuine }, null, 2), 'utf8');
    console.log('Wrote ' + outPath);
  }
}

main().catch((err) => { console.error(err); process.exit(1); });
