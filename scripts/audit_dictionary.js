/**
 * Read-only dictionary audit.
 *
 * Compares the shipping corpus in DatabaseSeeder.kt against the clean extract
 * (Kasiguranin / Tagalog / English / Category) and reports every disagreement,
 * grouped by kind. It changes nothing: the repairs are applied by hand from this
 * report so that each one is a reviewed decision rather than a bulk overwrite.
 *
 *   node scripts/audit_dictionary.js [path-to-xlsx]
 *
 * Exits 1 when a blocking defect (scrambled row, English in the Tagalog field)
 * is still present, so it can gate a release.
 */
const fs = require('fs');
const path = require('path');
const XLSX = require('xlsx');

const SEEDER = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'kasiguru', 'data', 'local', 'DatabaseSeeder.kt');
const DEFAULT_XLSX = 'C:/Users/U S E R - P C/Downloads/Extracted_Vocabulary_Dictionary.xlsx';
const DEFAULT_WORDLIST = 'C:/Users/U S E R - P C/Downloads/WORDLIST-1228 (1).xlsx';

/**
 * Tokens that join a genuine multi-word Kasiguranin headword ("bitoin sa umaga" = morning star,
 * "bakas ng bësset" = footprint). A headword containing one of these is a phrase, not the
 * doubled-token wreckage the scrambled check is looking for.
 */
const LINKERS = new Set(['sa', 'ng', 'na', 'ang', 'nang', 'si', 'ni']);

// Field pairs inside one VocabularyEntity(...) literal. One regex, applied
// globally, rather than one per field name -- building per-field patterns from
// strings needs doubled escapes that do not survive every shell round-trip.
const FIELD = /(\w+)\s*=\s*"([^"]*)"/g;
const ENTITY = /VocabularyEntity\(([\s\S]*?)\n {8}\)/g;

function readSeeder() {
  const src = fs.readFileSync(SEEDER, 'utf8');
  const from = src.indexOf('fun getInitialVocabulary');
  if (from < 0) throw new Error('getInitialVocabulary() not found in DatabaseSeeder.kt');
  const seg = src.slice(from);
  const rows = [];
  for (const m of seg.matchAll(ENTITY)) {
    const fields = {};
    for (const f of m[1].matchAll(FIELD)) fields[f[1]] = f[2];
    // getInitialVocabulary() is the last list in the file, but guard anyway:
    // only vocabulary literals carry a kasiguranin field.
    if ('kasiguranin' in fields) rows.push(fields);
  }
  return rows;
}

function readSource(file) {
  const wb = XLSX.readFile(file);
  const sheet = wb.Sheets[wb.SheetNames[0]];
  return XLSX.utils.sheet_to_json(sheet, { defval: '' }).map((r) => ({
    kasiguranin: String(r.Kasiguranin || '').trim(),
    tagalog: String(r.Tagalog || '').trim(),
    english: String(r.English || '').trim(),
    category: String(r.Category || '').trim(),
  }));
}

/** "bear / suffer" and "bear, suffer" are the same gloss written two ways. */
const senses = (s) => String(s || '').split(/[/,]/).map((x) => x.trim().toLowerCase()).filter(Boolean);
const norm = (s) => String(s || '').trim().toLowerCase();
const sameGloss = (a, b) => {
  const [x, y] = [senses(a), senses(b)];
  return x.length > 0 && y.length > 0 && x.some((s) => y.includes(s));
};

/** Kasiguranin headwords recorded in the field wordlist, if it can be found. */
function readWordlist() {
  const file = process.argv[3] || DEFAULT_WORDLIST;
  if (!fs.existsSync(file)) return [];
  const wb = XLSX.readFile(file);
  const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { defval: '', header: 1 });
  const at = rows.findIndex((r) => String(r[0] || '').trim() === 'NO.');
  if (at < 0) return [];
  const out = [];
  for (const r of rows.slice(at + 1)) {
    const cell = String(r[5] || '').trim();
    if (!cell) continue;
    cell.split(/[,;/]/).forEach((part) => {
      const clean = part.replace(/\s*\([^)]*\)\s*/g, ' ').replace(/\s+/g, ' ').trim();
      if (clean) out.push(clean);
    });
  }
  return out;
}

function main() {
  const xlsxPath = process.argv[2] || DEFAULT_XLSX;
  if (!fs.existsSync(xlsxPath)) {
    console.error('Source extract not found: ' + xlsxPath);
    console.error('Pass its path as the first argument.');
    process.exit(2);
  }

  const seed = readSeeder();
  const src = readSource(xlsxPath);
  const report = { scrambled: [], trailing: [], englishInTagalog: [], unmatched: [], glossDiff: [], tagalogDiff: [], missingIpa: [], missingRoot: [], noMeaning: [], longMeaning: [] };

  // A Tagalog field holding a word that the English column of some other entry
  // uses, and that no source row gives as a Tagalog gloss, is import wreckage.
  const sourceTagalogs = new Set(src.flatMap((r) => senses(r.tagalog)));
  const sourceEnglishes = new Set(src.flatMap((r) => senses(r.english)));
  // Tagalog glosses the corpus itself uses. A cell is only "English in the Tagalog field"
  // if no entry anywhere treats that word as Tagalog -- "pain" is Tagalog for bait.
  const corpusTagalogs = new Set(seed.flatMap((r) => senses(r.tagalog)));

  // Every headword documented in either source. The extract holds 373 rows and the field
  // wordlist about 950; measuring the corpus against only the smaller one reported hundreds
  // of perfectly well-documented words as unknown.
  const knownHeads = new Set(src.map((r) => norm(r.kasiguranin)));
  for (const w of readWordlist()) knownHeads.add(norm(w));

  const byHead = new Map();
  for (const r of src) {
    const k = norm(r.kasiguranin);
    if (!byHead.has(k)) byHead.set(k, []);
    byHead.get(k).push(r);
  }

  for (const row of seed) {
    const head = norm(row.kasiguranin);
    const candidates = byHead.get(head) || [];
    const match = candidates.find((c) => sameGloss(c.english, row.english)) || null;

    if (/,\s*$/.test(row.english || '') || /,\s*$/.test(row.tagalog || '')) {
      report.trailing.push(row);
    }
    if (
      row.tagalog &&
      !sourceTagalogs.has(norm(row.tagalog)) &&
      !corpusTagalogs.has(norm(row.tagalog)) &&
      sourceEnglishes.has(norm(row.tagalog))
    ) {
      report.englishInTagalog.push(row);
    }
    if (candidates.length === 0) {
      // The column-shift import left headwords with a *repeated* token ("tiis tiis",
      // "coal baga baga", "iba iba"). That repetition is the signature, not merely having
      // more than one token: plenty of real headwords are phrases ("bakas ng bësset").
      // Anything with a linker in it is a phrase and is never flagged.
      const tokens = head.split(/\s+/);
      const repeated = tokens.length > 1 && new Set(tokens).size < tokens.length;
      const hasLinker = tokens.some((t) => LINKERS.has(t));
      const token = repeated && !hasLinker ? tokens.find((t) => byHead.has(t)) : null;
      if (token) report.scrambled.push({ row, suggest: byHead.get(token) });
      else if (!knownHeads.has(head)) report.unmatched.push(row);
    } else if (!match) {
      report.glossDiff.push({ row, candidates });
    } else if (norm(match.tagalog) !== norm(row.tagalog)) {
      report.tagalogDiff.push({ row, match });
    }

    if (!(row.ipaNotation || '').trim()) report.missingIpa.push(row);
    if (!(row.rootForm || '').trim()) report.missingRoot.push(row);
    if (!(row.meaningEnglish || '').trim() || !(row.meaningTagalog || '').trim()) report.noMeaning.push(row);
    if ((row.meaningEnglish || '').length > 100 || (row.meaningTagalog || '').length > 100) report.longMeaning.push(row);
  }

  const show = (r) => '  ' + [r.kasiguranin, r.tagalog, r.english, r.category].join(' | ');
  const section = (title, items, render) => {
    console.log('\n== ' + title + ' (' + items.length + ') ==');
    items.forEach(render);
  };

  console.log('Seeder entries: ' + seed.length + '    source rows: ' + src.length);

  section('SCRAMBLED - doubled headword, gloss split across columns', report.scrambled, (x) => {
    console.log(show(x.row));
    x.suggest.forEach((s) => console.log('     source: ' + [s.kasiguranin, s.tagalog, s.english, s.category].join(' | ')));
  });
  section('ENGLISH TEXT IN THE TAGALOG FIELD', report.englishInTagalog, (r) => console.log(show(r)));
  section('TRAILING COMMA IN A GLOSS', report.trailing, (r) => console.log(show(r)));
  section('HEADWORD ABSENT FROM THE SOURCE - needs a human', report.unmatched, (r) => console.log(show(r)));
  section('GLOSS DISAGREES WITH THE SOURCE', report.glossDiff, (x) => {
    console.log(show(x.row));
    x.candidates.forEach((s) => console.log('     source: ' + [s.kasiguranin, s.tagalog, s.english, s.category].join(' | ')));
  });
  section('TAGALOG DISAGREES WITH THE SOURCE', report.tagalogDiff, (x) => {
    console.log(show(x.row));
    console.log('     source: ' + [x.match.kasiguranin, x.match.tagalog, x.match.english, x.match.category].join(' | '));
  });
  section('MISSING IPA', report.missingIpa, (r) => console.log(show(r)));
  section('MISSING ROOT FORM', report.missingRoot, (r) => console.log(show(r)));
  section('MISSING A MEANING', report.noMeaning, (r) => console.log(show(r)));
  section('MEANING OVER 100 CHARACTERS', report.longMeaning, (r) => console.log(show(r)));

  const blocking = report.scrambled.length + report.englishInTagalog.length + report.trailing.length;
  console.log('\nBlocking defects: ' + blocking);
  process.exit(blocking > 0 ? 1 : 0);
}

main();
