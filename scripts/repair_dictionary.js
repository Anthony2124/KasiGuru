/**
 * One-time corpus repair for DatabaseSeeder.kt, kept in the repo as the record
 * of what was changed and why.
 *
 * Every correction is listed explicitly below and matched against the exact
 * current value, so the script is idempotent and cannot silently rewrite a row
 * that has already been fixed or edited since. Run scripts/audit_dictionary.js
 * first -- this table was derived from its report.
 *
 *   node scripts/repair_dictionary.js [--dry-run]
 *
 * Rows whose correct reading could not be established from the source extract
 * are NOT repaired here; they are listed under NEEDS_A_HUMAN and reported.
 */
const fs = require('fs');
const path = require('path');

const SEEDER = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'kasiguru', 'data', 'local', 'DatabaseSeeder.kt');

// from: [kasiguranin, english] as they stand today -- the pair is unique.
// set:  fields to rewrite. Anything omitted is left alone.
const REPAIRS = [
  // -- Column-shift wreckage: a two-part gloss was split, its tail landed in the
  //    Tagalog column, and the displaced token was appended to the headword.
  //    Each of these is confirmed by Extracted_Vocabulary_Dictionary.xlsx.
  { from: ['bukong bokong', 'ankle'], set: { kasiguranin: 'bukong', rootForm: 'bukong' } },
  { from: ['kulet ng kayo', 'bark'], set: { kasiguranin: 'kulet', rootForm: 'kulet' } },
  { from: ['tiis tiis', 'bear,'], set: { kasiguranin: 'tiis', tagalog: 'tiis', english: 'bear, suffer', rootForm: 'tiis' } },
  { from: ['anak anak', 'child'], set: { kasiguranin: 'anak', tagalog: 'anak', english: 'child', rootForm: 'anak' } },
  { from: ['gata gata', 'coconut'], set: { kasiguranin: 'gata', tagalog: 'gata', english: 'coconut milk', rootForm: 'gata' } },
  { from: ['kulot kulot', 'curly'], set: { kasiguranin: 'kulot', tagalog: 'kulot', english: 'curly hair', rootForm: 'kulot' } },
  { from: ['or 24 hrs) araw (also, sun) aldew', 'day'], set: { kasiguranin: 'aldew', tagalog: 'araw', english: 'day', rootForm: 'aldew' } },
  { from: ['madisalad (hukay)', 'deep'], set: { kasiguranin: 'madisalad', rootForm: 'madisalad' } },
  { from: ['tuyo tuyo', 'dry'], set: { kasiguranin: 'tuyo', tagalog: 'tuyo', english: 'dry', rootForm: 'tuyo' } },
  { from: ['coal baga baga', 'ember,'], set: { kasiguranin: 'baga', tagalog: 'baga', english: 'coal, ember', rootForm: 'baga' } },
  { from: ['kaku na', 'goodbye'], set: { kasiguranin: 'kaku', rootForm: 'kaku' } },
  { from: ['gayuma amaya', 'love'], set: { kasiguranin: 'gayuma', tagalog: 'amaya', english: 'love charm', rootForm: 'gayuma' } },
  { from: ['iba iba', 'other,'], set: { kasiguranin: 'iba', tagalog: 'iba', english: 'other, different', rootForm: 'iba' } },
  { from: ['katig pakaway', 'outrigger'], set: { kasiguranin: 'katig', tagalog: 'pakaway', english: 'float outrigger', rootForm: 'katig' } },
  { from: ['tama tama', 'right'], set: { kasiguranin: 'tama', tagalog: 'tama', english: 'right (correct)', rootForm: 'tama' } },
  { from: ['kanan kanan', 'right'], set: { kasiguranin: 'kanan', tagalog: 'kanan', english: 'right (hand)', rootForm: 'kanan' } },
  { from: ['balat kulet', 'skin'], set: { kasiguranin: 'balat', tagalog: 'kulet', english: 'skin', rootForm: 'balat' } },
  { from: ['wood) patpat patpat', 'stick'], set: { kasiguranin: 'patpat', tagalog: 'patpat', english: 'wood (stick)', rootForm: 'patpat' } },
  { from: ['inom inom', 'to'], set: { kasiguranin: 'inom', tagalog: 'inom', english: 'drink', rootForm: 'inom' } },
  { from: ['matuwid diretso', 'straight'], set: { kasiguranin: 'matuwid', tagalog: 'tuwid, diretso', english: 'straight', rootForm: 'matuwid' } },

  // Same doubling, no row in the extract to confirm against -- but both tokens
  // of the headword are identical, so de-doubling introduces no new word and
  // the parenthetical is plainly the tail of the English gloss.
  { from: ['palad palad', 'palm'], set: { kasiguranin: 'palad', tagalog: 'palad', english: 'palm (hand)', rootForm: 'palad' } },
  { from: ['kamalig kamalig', 'storehouse'], set: { kasiguranin: 'kamalig', tagalog: 'kamalig', english: 'storehouse (food)', rootForm: 'kamalig' } },
  { from: ['laban laban', 'to'], set: { kasiguranin: 'laban', tagalog: 'laban', english: 'fight', rootForm: 'laban' } },

  // -- English text sitting in the Tagalog field, plus trailing-comma artifacts.
  { from: ['dakëp', 'catch,'], set: { tagalog: 'dakip', english: 'catch, apprehend' } },
  { from: ['turog', 'leak,'], set: { english: 'leak' } },
  { from: ['ungot', 'torch,'], set: { english: 'torch' } },

  // -- Truncated or misspelled English glosses, corrected against the extract.
  { from: ['igut', 'tiie'], set: { english: 'tie' } },
  { from: ['buno', 'kll'], set: { english: 'kill' } },
  { from: ['ota', 'vommit'], set: { english: 'vomit' } },
  { from: ['korkoran', 'coconut'], set: { english: 'coconut grater', category: 'Occupations & Tools' } },
  { from: ['madëgnen', 'cold'], set: { english: 'cold (weather)' } },
  { from: ['uban', 'gray'], set: { english: 'gray hair' } },
  { from: ['tonok', 'high'], set: { english: 'high tide', category: 'Nature & Environment' } },
  { from: ['tulang', 'fishbone'], set: { tagalog: 'buto, tinik', english: 'bone, fishbone', rootForm: 'tulang' } },

  // -- Tagalog spellings, corrected against the extract.
  { from: ['apdu', 'bile'], set: { tagalog: 'apdo' } },
  { from: ['alapok', 'fog'], set: { tagalog: 'ulap' } },
  { from: ['buto/bungaw', 'penis'], set: { tagalog: 'ari ng lalaki' } },
  { from: ['ogsad', 'down'], set: { tagalog: 'bumaba' } },
  { from: ['ipeta', 'show'], set: { tagalog: 'ipakita' } },
  { from: ['dikkël', 'big'], set: { tagalog: 'malaki' } },
];

/**
 * Left alone on purpose. Each is a headword whose two tokens are *different*
 * words, so deciding which one is the Kasiguranin form -- rather than the
 * Tagalog gloss that got appended -- would be inventing primary research data.
 * Resolve against Supnet (2016) and add them to REPAIRS.
 */
const NEEDS_A_HUMAN = [
  ['kaliwa kariwe', 'left', 'Which token is the Kasiguranin form? The Tagalog word for left is kaliwa.'],
  ['kasinungalingan kabulean', 'lie', 'Same question: kasinungalingan is the Tagalog word.'],
  ['hand', 'press', 'An English word used as a Kasiguranin headword; its tagalog field reads "with". No counterpart in the extract.'],
];

const ENTITY = /VocabularyEntity\(([\s\S]*?)\n {8}\)/g;
const FIELD = /(\w+)\s*=\s*"([^"]*)"/g;

function fieldsOf(body) {
  const f = {};
  for (const p of body.matchAll(FIELD)) if (f[p[1]] === undefined) f[p[1]] = p[2];
  return f;
}

function main() {
  const dry = process.argv.includes('--dry-run');
  let src = fs.readFileSync(SEEDER, 'utf8');
  const applied = [];
  const missed = [];

  for (const rep of REPAIRS) {
    const [wantK, wantE] = rep.from;
    const label = wantK + ' / ' + wantE;
    let hit = null;
    let ambiguous = false;

    for (const m of src.matchAll(ENTITY)) {
      const f = fieldsOf(m[1]);
      if (f.kasiguranin === wantK && f.english === wantE) {
        if (hit) { ambiguous = true; break; }
        hit = { block: m[0], body: m[1], fields: f };
      }
    }
    if (ambiguous) { missed.push(label + '  (matches more than one entry)'); continue; }
    if (!hit) { missed.push(label); continue; }

    let body = hit.body;
    const changes = [];
    let ok = true;
    for (const [name, value] of Object.entries(rep.set)) {
      const old = hit.fields[name];
      if (old === undefined) { missed.push(label + '  (no ' + name + ' field to rewrite)'); ok = false; break; }
      // Already correct: a re-run should read as "the corpus is clean", not as forty
      // corrections applied a second time.
      if (old === value) continue;
      const marker = name + ' = "' + old + '"';
      if (!body.includes(marker)) { missed.push(label + '  (could not locate ' + name + ')'); ok = false; break; }
      changes.push(name + ': ' + old + '  ->  ' + value);
      body = body.replace(marker, name + ' = "' + value + '"');
    }
    if (!ok) continue;
    if (changes.length === 0) { missed.push(label + '  (already correct)'); continue; }

    src = src.replace(hit.block, 'VocabularyEntity(' + body + '\n        )');
    applied.push({ label, changes });
  }

  console.log('Applied ' + applied.length + ' of ' + REPAIRS.length + ' repairs' + (dry ? '  (dry run, nothing written)' : ''));
  applied.forEach((a) => {
    console.log('\n  ' + a.label);
    a.changes.forEach((c) => console.log('      ' + c));
  });

  if (missed.length) {
    console.log('\nNot applied - already fixed, or the row no longer reads as expected:');
    missed.forEach((m) => console.log('  ' + m));
  }

  console.log('\nDeliberately left for a human (' + NEEDS_A_HUMAN.length + '):');
  NEEDS_A_HUMAN.forEach((n) => console.log('  ' + n[0] + ' / ' + n[1] + '\n      ' + n[2]));

  if (!dry) {
    fs.writeFileSync(SEEDER, src, 'utf8');
    console.log('\nWrote ' + SEEDER);
  }
}

// Also consumed by functions/fix_firestore_twins.js, so that Firestore receives exactly the
// corrections the corpus received rather than a second, drifting copy of the same table.
module.exports = { REPAIRS, NEEDS_A_HUMAN };

if (require.main === module) main();
