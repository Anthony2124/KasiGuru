/**
 * Imports the elicitation wordlist into the shipping corpus.
 *
 * The app was seeded from a ~373-row extract of the Supnet grammar sketch, while the field wordlist
 * (WORDLIST-1228.xlsx: NO. / ENGLISH / ENGLISH ELICITATION NOTES / TAGALOG / CEBUANO / LOCAL
 * LANGUAGE) documents roughly 950 Kasiguranin forms. The two are almost disjoint -- only about 200
 * forms appear in both -- so most of what was actually collected in the field has never reached a
 * learner. This brings them together.
 *
 *   node scripts/import_wordlist.js [--dry-run] [path-to-xlsx]
 *
 * It rewrites getInitialVocabulary() in DatabaseSeeder.kt, keeping every existing entry exactly as
 * it stands (including hand-authored IPA, aspect forms and definitions) and appending only senses
 * the corpus does not already have. Matching is on headword + English gloss, so a genuine homonym
 * is added rather than swallowed.
 *
 * It also splits the list across several private methods. A single Kotlin method is capped at 64 KB
 * of bytecode; the 394-entry version already compiled to ~18 KB, so a corpus of this size in one
 * method would not build at all.
 */
const fs = require('fs');
const path = require('path');
const XLSX = require('xlsx');

const SEEDER = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'kasiguru', 'data', 'local', 'DatabaseSeeder.kt');
const DEFAULT_XLSX = 'C:/Users/U S E R - P C/Downloads/WORDLIST-1228 (1).xlsx';
const CHUNK_SIZE = 150;

const ENTITY = /VocabularyEntity\(([\s\S]*?)\n {8}\)/g;
const FIELD = /(\w+)\s*=\s*"([^"]*)"/g;
const BOOL = /(\w+)\s*=\s*(true|false)/g;

/**
 * The wordlist is a standard elicitation instrument, so it is ordered by semantic domain rather
 * than alphabetically. Those bands map onto the app's twelve category cards far more accurately
 * than keyword-matching an English gloss does -- the previous recategorisation pass matched
 * substrings and filed "fishbone" under Animals because it contains "fish".
 *
 * Ranges are inclusive and refer to the wordlist's own NO. column.
 */
const BANDS = [
  [1, 55, 'Nature & Environment'],
  [56, 80, 'Weather & Climate'],
  [81, 89, 'Nature & Environment'],
  [90, 140, 'Family & People'],
  [141, 150, 'Greetings & Essentials'],
  [151, 250, 'Animals & Wildlife'],
  [251, 435, 'Body Parts & Health'],
  [436, 510, 'Food & Dining'],
  [511, 570, 'House & Daily Life'],
  [571, 670, 'Nature & Environment'],
  [671, 780, 'House & Daily Life'],
  [781, 810, 'Occupations & Tools'],
  [811, 890, 'Greetings & Essentials'],
  [891, 970, 'Numbers & Time'],
  [971, 1000, 'Food & Dining'],
  [1001, 1030, 'Colors & Shapes'],
  [1031, 1100, 'Emotions & Feelings'],
  [1101, 1300, 'Greetings & Essentials'],
];

/** Applied over the band when the gloss itself is unambiguous. First match wins. */
const KEYWORDS = [
  [/\b(rain|wind|storm|thunder|lightning|cloud|fog|dew|flood|typhoon|monsoon|weather|drizzle|hail)\b/, 'Weather & Climate'],
  [/\b(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|twenty|thirty|forty|fifty|hundred|thousand|million|first|second|third|yesterday|tomorrow|today|month|year|week|hour|minute|o'clock)\b/, 'Numbers & Time'],
  [/\b(red|blue|green|yellow|black|white|brown|orange|purple|colou?r|round|square|straight|crooked)\b/, 'Colors & Shapes'],
  [/\b(father|mother|brother|sister|uncle|aunt|cousin|grandfather|grandmother|son|daughter|husband|wife|nephew|niece|in-law|widow|orphan|child|baby)\b/, 'Family & People'],
  [/\b(eat|drink|rice|food|cook|fruit|meat|fish soup|salty|sweet|sour|bitter|hungry|thirsty|viand|broth)\b/, 'Food & Dining'],
  [/\b(happy|sad|angry|afraid|scared|shy|ashamed|love|hate|jealous|proud|lonely|worried|glad)\b/, 'Emotions & Feelings'],
];

const V = (r, i) => String(r[i] === undefined || r[i] === null ? '' : r[i]).trim();
const norm = (s) => String(s || '').trim().toLowerCase();
const senseKey = (k, e) => norm(k) + ' ' + norm(e);

function categoryFor(no, english) {
  const gloss = norm(english);
  for (const [re, cat] of KEYWORDS) if (re.test(gloss)) return cat;
  for (const [lo, hi, cat] of BANDS) if (no >= lo && no <= hi) return cat;
  return 'General';
}

/**
 * A wordlist cell can hold several forms ("diget,sabeng", "balbal/ yabat/ hablug") and sometimes a
 * parenthetical disambiguator ("bansaway(salt water)"). The first form becomes the headword; the
 * rest are returned so nothing is dropped silently.
 */
function splitForms(cell) {
  const parts = cell.split(/[,;/]/).map((p) => p.trim()).filter(Boolean);
  const clean = (p) => p.replace(/\s*\([^)]*\)\s*/g, ' ').replace(/\s+/g, ' ').trim();
  const forms = parts.map(clean).filter(Boolean);
  return { head: forms[0] || '', alternates: forms.slice(1) };
}

function kotlinEscape(s) {
  return String(s).replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$');
}

function readExisting(src) {
  const from = src.indexOf('fun getInitialVocabulary');
  const to = src.indexOf('fun getInitialStories');
  if (from < 0 || to < 0) throw new Error('Could not locate the vocabulary section.');
  const seg = src.slice(from, to);
  const rows = [];
  for (const m of seg.matchAll(ENTITY)) {
    const f = {};
    for (const p of m[1].matchAll(FIELD)) if (f[p[1]] === undefined) f[p[1]] = p[2];
    for (const p of m[1].matchAll(BOOL)) if (f[p[1]] === undefined) f[p[1]] = p[2] === 'true';
    if ('kasiguranin' in f) rows.push(f);
  }
  return { from, to, rows };
}

const ORDER = ['kasiguranin', 'tagalog', 'english', 'rootForm', 'neutralForm', 'imperfectiveForm',
  'perfectiveForm', 'contemplativeForm', 'category', 'partOfSpeech', 'meaningEnglish',
  'meaningTagalog', 'audioFileName', 'exampleSentence', 'exampleTranslation', 'exampleSentence2',
  'exampleTranslation2', 'ipaNotation'];

function renderEntity(row) {
  const lines = [];
  for (const name of ORDER) {
    const v = row[name];
    if (v === undefined || v === '') continue;
    lines.push('            ' + name + ' = "' + kotlinEscape(v) + '"');
  }
  for (const flag of ['phoneticGlottal', 'phoneticVowelLength']) {
    if (row[flag] === true) lines.push('            ' + flag + ' = true');
  }
  return '        VocabularyEntity(\n' + lines.join(',\n') + '\n        )';
}

function renderSection(rows) {
  const chunks = [];
  for (let i = 0; i < rows.length; i += CHUNK_SIZE) chunks.push(rows.slice(i, i + CHUNK_SIZE));

  const head = [
    '    /**',
    '     * The Kasiguranin corpus: ' + rows.length + ' senses.',
    '     *',
    '     * Split across ' + chunks.length + ' private methods rather than written as one list. A JVM method is',
    '     * capped at 64 KB of bytecode and a corpus this size in a single method does not compile;',
    '     * the split is purely a build constraint and the concatenation order is the corpus order.',
    '     *',
    '     * Generated by scripts/import_wordlist.js. Hand edits survive a re-run -- the importer',
    '     * keeps every existing entry as it stands and only appends senses the corpus lacks.',
    '     */',
    '    fun getInitialVocabulary(): List<VocabularyEntity> =',
    chunks.map((_, i) => '        vocabularyChunk' + (i + 1) + '()').join(' +\n'),
    '',
  ].join('\n');

  const bodies = chunks.map((chunk, i) =>
    '    private fun vocabularyChunk' + (i + 1) + '(): List<VocabularyEntity> = listOf(\n' +
    chunk.map(renderEntity).join(',\n') + '\n    )\n');

  return head + '\n' + bodies.join('\n');
}

function main() {
  const dry = process.argv.includes('--dry-run');
  const xlsxPath = process.argv.find((a) => a.endsWith('.xlsx')) || DEFAULT_XLSX;
  if (!fs.existsSync(xlsxPath)) {
    console.error('Wordlist not found: ' + xlsxPath);
    process.exit(2);
  }

  const src = fs.readFileSync(SEEDER, 'utf8');
  const { from, to, rows: existing } = readExisting(src);
  const have = new Set(existing.map((r) => senseKey(r.kasiguranin, r.english)));
  const haveHead = new Set(existing.map((r) => norm(r.kasiguranin)));

  const wb = XLSX.readFile(xlsxPath);
  const sheet = wb.Sheets[wb.SheetNames[0]];
  const raw = XLSX.utils.sheet_to_json(sheet, { defval: '', header: 1 });
  const headerAt = raw.findIndex((r) => V(r, 0) === 'NO.');
  if (headerAt < 0) throw new Error('Could not find the NO. header row.');

  const added = [];
  const skippedNoForm = [];
  const skippedDuplicate = [];
  const multiForm = [];
  const seenThisRun = new Set(have);

  for (const r of raw.slice(headerAt + 1)) {
    const english = V(r, 1);
    if (!english) continue;
    const no = parseInt(V(r, 0), 10) || 0;
    const cell = V(r, 5);
    if (!cell) { skippedNoForm.push(no + ' ' + english); continue; }

    const { head, alternates } = splitForms(cell);
    if (!head) { skippedNoForm.push(no + ' ' + english); continue; }
    if (alternates.length) multiForm.push(no + ' ' + cell + '  ->  ' + head + '  (also: ' + alternates.join(', ') + ')');

    const notes = V(r, 2);
    const key = senseKey(head, english);
    if (seenThisRun.has(key)) { skippedDuplicate.push(head + ' / ' + english); continue; }
    seenThisRun.add(key);

    added.push({
      kasiguranin: head,
      tagalog: V(r, 3),
      english,
      rootForm: head,
      category: categoryFor(no, english),
      _new: !haveHead.has(norm(head)),
      _no: no,
      // The field linguist's own disambiguator ("NOT 'dust', after you have been sweeping"). Not
      // stored on the entry: the definitions are authored in one consistent voice, and a note is
      // a fragment, not a sentence. It is written to the sidecar below so the authoring pass can
      // use it as evidence for what the word actually denotes.
      _notes: notes,
      _alternates: alternates,
    });
  }

  const merged = existing.concat(added.map((a) => {
    const copy = Object.assign({}, a);
    Object.keys(copy).filter((k) => k.startsWith('_')).forEach((k) => delete copy[k]);
    return copy;
  }));

  console.log('Existing senses:        ' + existing.length);
  console.log('Wordlist senses added:  ' + added.length);
  console.log('  of which new headwords: ' + added.filter((a) => a._new).length);
  console.log('  new senses of a word already present: ' + added.filter((a) => !a._new).length);
  console.log('Corpus after import:    ' + merged.length);
  console.log('Slots with no Kasiguranin form recorded (left out): ' + skippedNoForm.length);
  console.log('Wordlist rows already in the corpus:                ' + skippedDuplicate.length);
  console.log('Cells holding more than one form (first kept):      ' + multiForm.length);
  console.log('Entries carrying a field note into the authoring sidecar: ' + added.filter((a) => a._notes).length);

  const byCat = {};
  merged.forEach((r) => { byCat[r.category] = (byCat[r.category] || 0) + 1; });
  console.log('\nCategories after import:');
  Object.entries(byCat).sort((a, b) => b[1] - a[1]).forEach(([c, n]) => console.log('  ' + String(n).padStart(4) + '  ' + c));

  if (multiForm.length) {
    console.log('\nMulti-form cells (nothing dropped silently):');
    multiForm.forEach((m) => console.log('  ' + m));
  }

  if (dry) { console.log('\nDry run, nothing written.'); return; }

  const out = src.slice(0, from) + renderSection(merged) + '\n' + src.slice(to);
  fs.writeFileSync(SEEDER, out, 'utf8');
  console.log('\nWrote ' + SEEDER);

  // Evidence for the definition-authoring pass: the field notes and the alternate forms that a
  // single-headword corpus cannot represent. Not shipped in the app.
  const sidecar = added
    .filter((a) => a._notes || a._alternates.length)
    .map((a) => ({ no: a._no, kasiguranin: a.kasiguranin, english: a.english, tagalog: a.tagalog, notes: a._notes, alternates: a._alternates }));
  const sidecarPath = path.join(__dirname, 'wordlist_notes.json');
  fs.writeFileSync(sidecarPath, JSON.stringify(sidecar, null, 2), 'utf8');
  console.log('Wrote ' + sidecarPath + '  (' + sidecar.length + ' entries with field notes or alternate forms)');
}

main();
