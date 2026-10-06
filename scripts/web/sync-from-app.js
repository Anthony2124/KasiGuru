#!/usr/bin/env node
'use strict';

/**
 * Copies what the web app shares with the Android app out of the Android sources, so an APK
 * release cannot leave the web app behind on content or art.
 *
 *   node scripts/web/sync-from-app.js           write the web copies
 *   node scripts/web/sync-from-app.js --check   fail (exit 1) if any web copy differs; used by CI
 *
 * What it carries:
 *   - the shipping corpus, DatabaseSeeder.getInitialVocabulary(), as public/content/corpus.json.
 *     The APK seeds these 1,202 sourced senses and lays the cloud dictionary over them; the web
 *     app does the same with this file (see webapp/src/domain/contentMerge.ts).
 *   - the illustrations in res/drawable-nodpi: category icons, badge art, Jepjep, avatars, scenes.
 *
 * Sounds are not copied: the APK's are Ogg, which older iPhones cannot play. See docs/WEB_APP.md.
 */

const fs = require('node:fs');
const path = require('node:path');
const { PROJECT_ROOT, DATABASE_SEEDER } = require('../lib/project-paths');

const WEB = path.join(PROJECT_ROOT, 'admin-website', 'webapp', 'public');
const DRAWABLES = path.join(PROJECT_ROOT, 'app', 'src', 'main', 'res', 'drawable-nodpi');

/** Android drawable prefix -> web folder. The prefix is dropped from the file name. */
const ART = [
  ['category_', 'img/categories'],
  ['badge_', 'img/badges'],
  ['jepjep_', 'img/jepjep'],
  ['avatar_', 'img/avatars'],
  ['scene_', 'img/scenes'],
];

/** The VocabularyEntity fields the seeder writes, in the order the web file lists them. */
const FIELDS = [
  'kasiguranin', 'tagalog', 'english', 'rootForm', 'category', 'meaningEnglish', 'meaningTagalog',
  'ipaNotation', 'phoneticGlottal', 'phoneticVowelLength',
];

/** Reads one Kotlin string literal starting at src[i] === '"'; returns [value, indexAfter]. */
function readString(src, i) {
  let out = '';
  for (i += 1; i < src.length; i++) {
    const c = src[i];
    if (c === '"') return [out, i + 1];
    if (c === '\\') {
      const n = src[++i];
      if (n === 'u') { out += String.fromCharCode(parseInt(src.slice(i + 1, i + 5), 16)); i += 4; }
      else out += ({ n: '\n', t: '\t', r: '\r', b: '\b' })[n] ?? n;
    } else {
      out += c;
    }
  }
  throw new Error('unterminated string literal in DatabaseSeeder.kt');
}

/** DatabaseSeeder.getInitialVocabulary(), in seeding order (which is Room's id order on a phone). */
function parseCorpus() {
  const kt = fs.readFileSync(DATABASE_SEEDER, 'utf8');
  const start = kt.indexOf('private fun vocabularyChunk1()');
  const end = kt.indexOf('fun getInitialAchievements');
  if (start < 0 || end < 0) throw new Error('DatabaseSeeder.kt: vocabulary chunks not found');
  const src = kt.slice(start, end);
  const words = [];
  const open = /VocabularyEntity\(/g;
  let m;
  while ((m = open.exec(src))) {
    const word = {};
    let i = m.index + m[0].length;
    let depth = 1;
    while (depth > 0 && i < src.length) {
      const field = /^\s*([A-Za-z0-9]+)\s*=\s*/.exec(src.slice(i, i + 80));
      if (field) {
        i += field[0].length;
        if (src[i] === '"') {
          const [v, next] = readString(src, i);
          word[field[1]] = v;
          i = next;
        } else {
          const lit = /^(true|false|-?\d+L?)/.exec(src.slice(i, i + 24));
          if (!lit) throw new Error(`DatabaseSeeder.kt: unreadable value for ${field[1]} near entry ${words.length + 1}`);
          word[field[1]] = lit[1] === 'true' ? true : lit[1] === 'false' ? false : Number(lit[1].replace('L', ''));
          i += lit[1].length;
        }
        continue;
      }
      if (src[i] === '(') depth++;
      else if (src[i] === ')') depth--;
      i++;
    }
    open.lastIndex = i;
    const out = {};
    for (const f of FIELDS) {
      if (f.startsWith('phonetic')) out[f] = word[f] === true;
      else out[f] = typeof word[f] === 'string' ? word[f] : '';
    }
    if (out.kasiguranin) words.push(out);
  }
  return words;
}

function corpusFile() {
  const words = parseCorpus();
  return JSON.stringify({ meta: { source: 'app DatabaseSeeder.getInitialVocabulary()', count: words.length }, words }) + '\n';
}

/** Every [web path, bytes] pair this script owns. */
function plan() {
  const files = [[path.join(WEB, 'content', 'corpus.json'), Buffer.from(corpusFile(), 'utf8')]];
  for (const name of fs.readdirSync(DRAWABLES).sort()) {
    const rule = ART.find(([prefix]) => name.startsWith(prefix));
    if (!rule) continue;
    files.push([path.join(WEB, rule[1], name.slice(rule[0].length)), fs.readFileSync(path.join(DRAWABLES, name))]);
  }
  return files;
}

function main() {
  const check = process.argv.includes('--check');
  const drift = [];
  let written = 0;
  for (const [file, bytes] of plan()) {
    const same = fs.existsSync(file) && fs.readFileSync(file).equals(bytes);
    if (same) continue;
    const rel = path.relative(PROJECT_ROOT, file).split(path.sep).join('/');
    if (check) { drift.push(rel); continue; }
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, bytes);
    written++;
    console.log(`updated ${rel}`);
  }
  if (check) {
    // Every APK version has to say what reached the web app and what stays Android-only.
    const gradle = fs.readFileSync(path.join(PROJECT_ROOT, 'app', 'build.gradle.kts'), 'utf8');
    const version = /versionName\s*=\s*"([^"]+)"/.exec(gradle)?.[1];
    const ledgerPath = path.join(PROJECT_ROOT, 'docs', 'WEB_PARITY.md');
    const ledger = fs.existsSync(ledgerPath) ? fs.readFileSync(ledgerPath, 'utf8') : '';
    if (!version || !ledger.includes(`| ${version} |`)) {
      drift.push(`docs/WEB_PARITY.md has no row for ${version ?? 'the app version'}: say what the web app got from it`);
    }
    if (drift.length) {
      console.error(`The web app is behind the Android app on ${drift.length} file(s):`);
      for (const f of drift) console.error(`  ${f}`);
      console.error('Run `npm run sync:web` and commit the result.');
      process.exit(1);
    }
    console.log('Web app content and art match the Android app.');
  } else {
    console.log(written ? `${written} file(s) updated.` : 'Already in step with the Android app.');
  }
}

if (require.main === module) main();
module.exports = { parseCorpus };
