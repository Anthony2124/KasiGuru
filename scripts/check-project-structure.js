'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { PROJECT_ROOT, DATABASE_SEEDER, dictionaryData } = require('./lib/project-paths');

const failures = [];
const relative = (file) => path.relative(PROJECT_ROOT, file).split(path.sep).join('/');

function filesUnder(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    if (entry.name.startsWith('.') || entry.name === 'node_modules' || entry.name === 'build') return [];
    const file = path.join(directory, entry.name);
    return entry.isDirectory() ? filesUnder(file) : [file];
  });
}

const required = [
  DATABASE_SEEDER, dictionaryData('meanings.json'), dictionaryData('wordlist_notes.json'),
  ...['AGENTS.md', 'docs/CODE_MAP.md', 'scripts/dictionary', 'scripts/lib', 'data/sql', 'design/assets']
    .map((file) => path.join(PROJECT_ROOT, file)),
];
for (const file of required) {
  if (!fs.existsSync(file)) failures.push('Missing project path: ' + relative(file));
}

const kotlin = filesUnder(path.join(PROJECT_ROOT, 'app', 'src')).filter((file) => file.endsWith('.kt'));
for (const file of kotlin) {
  const text = fs.readFileSync(file, 'utf8');
  const packageName = text.match(/^package\s+([\w.]+)/m)?.[1];
  const folder = relative(path.dirname(file)).split('/java/')[1];
  if (!packageName || folder !== packageName.replaceAll('.', '/')) {
    failures.push('Kotlin package does not match folder: ' + relative(file));
  }
}

const games = path.join(PROJECT_ROOT, 'app/src/main/java/com/kasiguru/ui/screens/games');
for (const entry of fs.readdirSync(games, { withFileTypes: true })) {
  if (entry.isFile() && entry.name.endsWith('.kt')) {
    failures.push('Place game code in a game, hub, levels or shared folder: ' + entry.name);
  }
}

// Inspect local imports without executing maintenance tools or contacting Firebase.
const javascript = ['scripts', 'functions'].flatMap((folder) => filesUnder(path.join(PROJECT_ROOT, folder)))
  .filter((file) => file.endsWith('.js'));
for (const file of javascript) {
  const text = fs.readFileSync(file, 'utf8');
  for (const match of text.matchAll(/require\(\s*['"](\.[^'"]+)['"]\s*\)/g)) {
    const target = path.resolve(path.dirname(file), match[1]);
    if (![target, target + '.js', target + '.json', path.join(target, 'index.js')]
      .some((candidate) => fs.existsSync(candidate) && fs.statSync(candidate).isFile())) {
      failures.push('Broken local require in ' + relative(file) + ': ' + match[1]);
    }
  }
}

if (failures.length) {
  failures.forEach((message) => console.error(message));
  process.exitCode = 1;
} else {
  console.log(`Project structure OK: ${kotlin.length} Kotlin packages and ${javascript.length} maintenance scripts checked.`);
}
