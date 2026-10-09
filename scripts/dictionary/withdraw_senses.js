/**
 * Takes senses the dictionary has withdrawn out of the shipping corpus, and keeps them out.
 *
 * A word deleted in the admin portal leaves Firestore, but the copy DatabaseSeeder ships stays on every
 * phone: FirestoreSyncManager.pruneWithdrawnWords keeps anything the build seeds, and the web app lays
 * the cloud over the same corpus. So a deleted or re-glossed word lingered in the Library as a second,
 * silent entry (the recordings belong to the cloud's senses) until the corpus dropped it too.
 *
 *   node scripts/dictionary/withdraw_senses.js --from-backup <backup-folder>
 *       Records every corpus sense that backup's vocabulary.json lacks in
 *       data/dictionary/withdrawn_senses.json. Reads local files only; never contacts Firestore.
 *   node scripts/dictionary/withdraw_senses.js [--dry-run]
 *       Removes every recorded sense from getInitialVocabulary(), leaving all other entries as written.
 *
 * import_wordlist.js skips recorded senses, so a re-import cannot bring them back. After removing,
 * run `npm run sync:web`, and bump VOCABULARY_PARSER_VERSION in FirestoreSyncManager.kt so every phone
 * reads the dictionary once and prunes its copies.
 */
const fs = require('fs');
const path = require('path');
const { DATABASE_SEEDER, dictionaryData } = require('../lib/project-paths');

const LIST = dictionaryData('withdrawn_senses.json');
const norm = (s) => String(s || '').trim().toLowerCase();
const senseKey = (k, e) => norm(k) + ' ' + norm(e);

const CHUNK = /( {4}private fun vocabularyChunk\d+\(\): List<VocabularyEntity> = listOf\(\n)([\s\S]*?)(\n {4}\)\n)/g;
const ENTITY = / {8}VocabularyEntity\(\n[\s\S]*?\n {8}\)/g;

/** A Kotlin string field's value, unescaped, or '' when the entity does not set it. */
function field(entity, name) {
  const m = new RegExp('\\b' + name + '\\s*=\\s*"((?:[^"\\\\]|\\\\.)*)"').exec(entity);
  return m ? m[1].replace(/\\(.)/g, '$1') : '';
}

/** The seeder with LF line endings, and the ending to write it back with (a Windows checkout has CRLF). */
function readSeeder() {
  const raw = fs.readFileSync(DATABASE_SEEDER, 'utf8');
  return { src: raw.replace(/\r\n/g, '\n'), eol: raw.includes('\r\n') ? '\r\n' : '\n' };
}

function readList() {
  return fs.existsSync(LIST) ? JSON.parse(fs.readFileSync(LIST, 'utf8')) : {
    about: 'Corpus senses the dictionary no longer has (deleted or re-glossed in the admin portal). '
      + 'Kept out of DatabaseSeeder by scripts/dictionary/withdraw_senses.js; import_wordlist.js skips them.',
    senses: [],
  };
}

/** Every corpus sense, in order. */
function corpusSenses(src) {
  const out = [];
  for (const chunk of src.matchAll(CHUNK)) {
    for (const e of chunk[2].match(ENTITY) || []) out.push({ kasiguranin: field(e, 'kasiguranin'), english: field(e, 'english') });
  }
  return out;
}

function fromBackup(folder) {
  const file = path.join(folder, 'vocabulary.json');
  const docs = JSON.parse(fs.readFileSync(file, 'utf8')).documents.map((d) => d.data);
  const cloud = new Set(docs.map((d) => senseKey(d.kasiguranin, d.english)));
  const glossesOf = new Map();
  for (const d of docs) {
    const h = norm(d.kasiguranin);
    glossesOf.set(h, (glossesOf.get(h) || []).concat(d.english || ''));
  }

  const list = readList();
  const listed = new Set(list.senses.map((s) => senseKey(s.kasiguranin, s.english)));
  const found = corpusSenses(readSeeder().src)
    .filter((s) => !cloud.has(senseKey(s.kasiguranin, s.english)) && !listed.has(senseKey(s.kasiguranin, s.english)));

  for (const s of found) {
    // What the dictionary holds for the headword now: empty when the word itself was deleted, the
    // current gloss when it was re-glossed or a homonym was removed.
    list.senses.push({ ...s, dictionaryNow: glossesOf.get(norm(s.kasiguranin)) || [], missingFrom: path.basename(path.resolve(folder)) });
  }
  fs.writeFileSync(LIST, JSON.stringify(list, null, 2) + '\n', 'utf8');
  console.log(`${found.length} corpus sense(s) missing from ${file} recorded in ${LIST}`);
  found.forEach((s) => console.log(`  ${s.kasiguranin} | ${s.english}`));
}

function remove(dry) {
  const withdrawn = new Set(readList().senses.map((s) => senseKey(s.kasiguranin, s.english)));
  const { src, eol } = readSeeder();
  const removed = [];
  let kept = 0;

  let out = src.replace(CHUNK, (_, open, body, close) => {
    const entities = body.match(ENTITY) || [];
    const keep = entities.filter((e) => {
      const gone = withdrawn.has(senseKey(field(e, 'kasiguranin'), field(e, 'english')));
      if (gone) removed.push(field(e, 'kasiguranin') + ' | ' + field(e, 'english'));
      return !gone;
    });
    kept += keep.length;
    return open + keep.join(',\n') + close;
  });
  out = out.replace(/(The Kasiguranin corpus: )\d+( senses\.)/, `$1${kept}$2`);

  console.log(`Removed ${removed.length} sense(s); ${kept} remain.`);
  removed.forEach((r) => console.log('  ' + r));
  if (removed.length < withdrawn.size) console.log(`${withdrawn.size - removed.length} recorded sense(s) were not in the corpus.`);
  if (dry) { console.log('Dry run, nothing written.'); return; }
  if (out !== src) fs.writeFileSync(DATABASE_SEEDER, out.replace(/\n/g, eol), 'utf8');
}

const args = process.argv.slice(2);
const backupAt = args.indexOf('--from-backup');
if (backupAt >= 0) {
  if (!args[backupAt + 1]) { console.error('Usage: withdraw_senses.js --from-backup <backup-folder>'); process.exit(2); }
  fromBackup(args[backupAt + 1]);
} else {
  remove(args.includes('--dry-run'));
}
