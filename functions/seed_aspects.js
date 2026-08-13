// seed_aspects.js — bulk-load Kasiguranin verb aspect forms into Firestore.
//
// Usage:
//   node seed_aspects.js <service-account.json> [aspect_data.json]
//
// The data file maps a dictionary root word to its aspect forms:
// {
//   "abben": {
//     "neutral": "abben",
//     "imperfective": "magaabenën",
//     "perfective": "nagabben",
//     "contemplative": "magaabben",
//     "alternates": [
//       { "form": "inumabben", "tense": "perfective", "affixType": "Infix" }
//     ],
//     "source": "Supnet (2016) via kasiguranin_database_schema.sql"
//   }
// }
//
// Behavior:
//   - Matches vocabulary docs by rootWord (fallback: kasiguranin word).
//   - Writes only the fields present in the data (never clobbers others).
//   - Builds the nested `conjugations` array the app syncs into Room.
//   - Never deletes or overwrites existing aspect data with blanks.

const fs = require('fs');
const admin = require('firebase-admin');

const [keyPath, dataPath = 'aspect_data.json'] = process.argv.slice(2);
if (!keyPath) {
  console.error('Usage: node seed_aspects.js <service-account.json> [aspect_data.json]');
  process.exit(1);
}

admin.initializeApp({ credential: admin.credential.cert(keyPath) });
const db = admin.firestore();

const ASPECT_FIELDS = ['neutral', 'imperfective', 'perfective', 'contemplative'];

async function findDocs(rootWord) {
  const byRoot = await db.collection('vocabulary').where('rootWord', '==', rootWord).limit(5).get();
  if (!byRoot.empty) return byRoot.docs;
  const byWord = await db.collection('vocabulary').where('kasiguranin', '==', rootWord).limit(5).get();
  return byWord.docs;
}

function buildConjugations(entry) {
  const seen = new Set();
  const out = [];
  const push = (form, tense, affixType) => {
    const key = `${tense}|${form}`;
    if (!form || seen.has(key)) return;
    seen.add(key);
    out.push({ conjugatedForm: form, tense, affixType: affixType || null });
  };
  for (const aspect of ['imperfective', 'perfective', 'contemplative']) {
    if (entry[aspect]) push(entry[aspect], aspect, null);
  }
  for (const alt of entry.alternates || []) {
    push(alt.form, alt.tense, alt.affixType);
  }
  return out;
}

(async () => {
  const data = JSON.parse(fs.readFileSync(dataPath, 'utf8'));
  const words = Object.keys(data);
  if (words.length === 0) {
    console.error('No entries found in ' + dataPath);
    process.exit(1);
  }

  let updated = 0, matchedDocs = 0, missing = 0;
  for (const rootWord of words) {
    const entry = data[rootWord];
    const docs = await findDocs(rootWord);
    if (docs.length === 0) {
      console.warn(`SKIP "${rootWord}" — no vocabulary doc found`);
      missing++;
      continue;
    }

    const patch = { updatedAt: Date.now() };
    for (const aspect of ASPECT_FIELDS) {
      if (entry[aspect] !== undefined && entry[aspect] !== null && String(entry[aspect]).trim() !== '') {
        patch[`${aspect}Form`] = String(entry[aspect]).trim();
      }
    }
    const conjugations = buildConjugations(entry);
    if (conjugations.length > 0) patch.conjugations = conjugations;
    if (entry.source) patch.aspectSource = entry.source;

    for (const doc of docs) {
      await doc.ref.update(patch);
      matchedDocs++;
      console.log(`UPDATED "${rootWord}" (${doc.id}): ${ASPECT_FIELDS.filter(a => patch[`${a}Form`]).map(a => a + '=' + patch[`${a}Form`]).join(', ') || 'no aspect fields'} | conjugations: ${conjugations.length}`);
    }
    updated++;
  }

  console.log(`\nDone. ${updated} word(s) processed, ${matchedDocs} doc(s) updated, ${missing} not found.`);
  process.exit(missing > 0 ? 2 : 0);
})().catch((e) => {
  console.error('FAILED:', e.message);
  process.exit(1);
});
