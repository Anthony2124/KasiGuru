'use strict';

// Sources the app's sound effects and music from Freesound (CC0 only).
//
//   node scripts/audio/freesound.js shortlist [--out <file.html>]
//   node scripts/audio/freesound.js lock <selection.json>
//   node scripts/audio/freesound.js fetch
//
// `shortlist` searches each slot in data/audio/sound-slots.json and writes a page to
// listen to the candidates and copy a selection. `lock` records the chosen sounds in
// data/audio/freesound-sounds.json. `fetch` downloads those previews into res/raw.
// The API key is read from FREESOUND_API_KEY or ../private/freesound-api-key.txt. It is
// used only here and never reaches the app.

const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { RAW_RESOURCES, audioData, privateFile } = require('../lib/project-paths');

const API = 'https://freesound.org/apiv2';
const CC0 = 'Creative Commons 0';
const FIELDS = 'id,name,username,url,license,duration,avg_rating,num_ratings,num_downloads,previews';
const PER_QUERY = 3;
const SLOTS_FILE = audioData('sound-slots.json');
const MANIFEST_FILE = audioData('freesound-sounds.json');

function apiKey() {
  const fromEnv = process.env.FREESOUND_API_KEY?.trim();
  if (fromEnv) return fromEnv;
  const file = privateFile('freesound-api-key.txt');
  if (fs.existsSync(file)) return fs.readFileSync(file, 'utf8').trim();
  throw new Error('No Freesound API key. Create one at https://freesound.org/apiv2/apply and save it to ' + file);
}

async function api(pathname, params = {}) {
  const url = new URL(API + pathname);
  for (const [key, value] of Object.entries(params)) url.searchParams.set(key, value);
  const response = await fetch(url, { headers: { Authorization: 'Token ' + apiKey() } });
  if (response.status === 401) throw new Error('Freesound rejected the API key (401).');
  if (!response.ok) throw new Error(`Freesound ${response.status} for ${url.pathname}: ${await response.text()}`);
  return response.json();
}

const readJson = (file) => JSON.parse(fs.readFileSync(file, 'utf8'));
const writeJson = (file, value) => fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');

// Relevance first, then favour sounds other people have rated and downloaded.
const standing = (sound) => (sound.avg_rating || 0) * Math.log10((sound.num_downloads || 0) + 10);

async function search(query, [min, max]) {
  const page = await api('/search/', {
    query,
    filter: `license:"${CC0}" duration:[${min} TO ${max}]`,
    fields: FIELDS,
    page_size: '15',
  });
  return page.results.sort((a, b) => standing(b) - standing(a)).slice(0, PER_QUERY);
}

const escapeHtml = (text) => String(text).replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

function existingFile(slot) {
  const match = fs.readdirSync(RAW_RESOURCES).find((file) => path.parse(file).name === slot);
  return match ? path.join(RAW_RESOURCES, match) : null;
}

function shortlistPage(slots, results) {
  const sections = Object.entries(slots).map(([slot, spec]) => {
    const current = spec.existing && existingFile(slot);
    const options = results[slot].map((sound, index) => `
      <label class="option">
        <input type="radio" name="${slot}" value="${sound.id}" ${index === 0 && !current ? 'checked' : ''}>
        <span class="meta"><b>${escapeHtml(sound.name)}</b>
          <small>${sound.duration.toFixed(1)} s · ★ ${(sound.avg_rating || 0).toFixed(1)} (${sound.num_ratings}) ·
          ${sound.num_downloads} downloads · by ${escapeHtml(sound.username)} ·
          <a href="${escapeHtml(sound.url)}" target="_blank" rel="noopener">page</a></small></span>
        <audio controls preload="none" ${spec.music ? 'loop' : ''} src="${escapeHtml(sound.previews['preview-hq-mp3'])}"></audio>
      </label>`).join('');
    const keep = current ? `
      <label class="option">
        <input type="radio" name="${slot}" value="keep" checked>
        <span class="meta"><b>Keep the current sound</b><small>${escapeHtml(path.basename(current))}</small></span>
        <audio controls preload="none" src="${escapeHtml('file:///' + current.split(path.sep).join('/'))}"></audio>
      </label>` : '';
    return `<section><h2>${escapeHtml(spec.label)} <code>${slot}</code></h2>${keep}${options || '<p>No CC0 matches.</p>'}</section>`;
  }).join('');

  return `<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>KasiGuru sound picks</title>
<style>
  :root { color-scheme: dark; --bg:#13170f; --card:#1d2318; --ink:#eef2e6; --muted:#a6ae98; --accent:#b7e26b; }
  body { margin:0; background:var(--bg); color:var(--ink); font:15px/1.45 system-ui, sans-serif; }
  main { max-width:760px; margin:0 auto; padding:24px 16px 96px; }
  h1 { font-size:22px; margin:0 0 4px; } p.lead { color:var(--muted); margin:0 0 24px; }
  section { background:var(--card); border-radius:14px; padding:14px 16px; margin-bottom:14px; }
  h2 { font-size:16px; margin:0 0 10px; } code { color:var(--muted); font-size:12px; font-weight:400; }
  .option { display:grid; grid-template-columns:auto 1fr; gap:4px 10px; align-items:center; padding:8px 0; border-top:1px solid #2c3426; cursor:pointer; }
  .option:first-of-type { border-top:0; }
  .meta small { display:block; color:var(--muted); } a { color:var(--accent); }
  audio { grid-column:2; width:100%; height:34px; }
  input { accent-color:var(--accent); width:18px; height:18px; }
  footer { position:fixed; inset:auto 0 0 0; background:#0d100a; padding:12px 16px; display:flex; gap:12px; justify-content:center; align-items:center; }
  button { background:var(--accent); color:#13170f; border:0; border-radius:999px; padding:10px 22px; font-weight:700; font-size:15px; cursor:pointer; }
  #status { color:var(--muted); }
</style></head><body><main>
<h1>Pick KasiGuru's sounds</h1>
<p class="lead">Every candidate is CC0 on Freesound. Play them, choose one per row, then copy your selection and paste it back to Claude.</p>
${sections}
</main><footer><button id="copy">Copy selection</button><span id="status"></span></footer>
<script>
  document.getElementById('copy').onclick = async () => {
    const picks = {};
    for (const input of document.querySelectorAll('input[type=radio]:checked')) {
      picks[input.name] = input.value === 'keep' ? 'keep' : Number(input.value);
    }
    const text = JSON.stringify(picks);
    try { await navigator.clipboard.writeText(text); document.getElementById('status').textContent = 'Copied'; }
    catch { prompt('Copy this selection:', text); }
  };
</script></body></html>
`;
}

async function shortlist(args) {
  const { slots } = readJson(SLOTS_FILE);
  const results = {};
  for (const [slot, spec] of Object.entries(slots)) {
    const seen = new Set();
    results[slot] = [];
    for (const query of spec.queries) {
      for (const sound of await search(query, spec.duration)) {
        if (!seen.has(sound.id)) { seen.add(sound.id); results[slot].push(sound); }
      }
    }
    console.log(`${slot}: ${results[slot].length} candidates`);
  }
  const outIndex = args.indexOf('--out');
  const out = outIndex >= 0 ? path.resolve(args[outIndex + 1]) : path.join(os.tmpdir(), 'kasiguru-sound-shortlist.html');
  fs.writeFileSync(out, shortlistPage(slots, results));
  console.log('Wrote ' + out);
}

async function lock(args) {
  if (!args[0]) throw new Error('Usage: freesound.js lock <selection.json>');
  const selection = readJson(path.resolve(args[0]));
  const { slots } = readJson(SLOTS_FILE);
  const previous = fs.existsSync(MANIFEST_FILE) ? readJson(MANIFEST_FILE).sounds : {};
  const sounds = {};
  for (const [slot, choice] of Object.entries(selection)) {
    if (!slots[slot]) throw new Error('Unknown slot: ' + slot);
    if (choice === 'keep') continue;
    const sound = await api(`/sounds/${Number(choice)}/`, { fields: FIELDS });
    if (sound.license !== CC0 && !sound.license.includes('/publicdomain/zero/')) {
      throw new Error(`${slot}: sound ${sound.id} is not CC0 (${sound.license})`);
    }
    sounds[slot] = {
      id: sound.id, name: sound.name, username: sound.username, url: sound.url,
      license: CC0, duration: sound.duration, quality: previous[slot]?.quality || 'hq',
    };
  }
  writeJson(MANIFEST_FILE, {
    _comment: 'Sounds bundled in app/src/main/res/raw, sourced from Freesound under CC0. Regenerate with scripts/audio/freesound.js.',
    sounds,
  });
  console.log(`Locked ${Object.keys(sounds).length} sounds in ${MANIFEST_FILE}`);
}

async function download() {
  const { sounds } = readJson(MANIFEST_FILE);
  for (const [slot, entry] of Object.entries(sounds)) {
    const sound = await api(`/sounds/${entry.id}/`, { fields: 'id,license,previews' });
    if (sound.license !== CC0 && !sound.license.includes('/publicdomain/zero/')) {
      throw new Error(`${slot}: sound ${entry.id} is no longer CC0 (${sound.license}); not downloading`);
    }
    // Preview files are public CDN links, so the API key is not sent with them.
    const response = await fetch(sound.previews[`preview-${entry.quality || 'hq'}-ogg`]);
    if (!response.ok) throw new Error(`${slot}: preview download failed (${response.status})`);
    const bytes = Buffer.from(await response.arrayBuffer());
    // One resource name per slot: drop any other format of the same name first.
    for (const file of fs.readdirSync(RAW_RESOURCES)) {
      if (path.parse(file).name === slot) fs.rmSync(path.join(RAW_RESOURCES, file));
    }
    fs.writeFileSync(path.join(RAW_RESOURCES, slot + '.ogg'), bytes);
    const size = (bytes.length / 1024).toFixed(0) + ' KB';
    const warn = slot.startsWith('music_') && bytes.length > 2.5 * 1024 * 1024 ? '  (large: consider "quality": "lq")' : '';
    console.log(`${slot}.ogg  ${size}${warn}`);
  }
}

const [command, ...args] = process.argv.slice(2);
const commands = { shortlist, lock, fetch: download };
if (!commands[command]) {
  console.error('Usage: node scripts/audio/freesound.js <shortlist|lock|fetch>');
  process.exitCode = 1;
} else {
  commands[command](args).catch((error) => { console.error(error.message); process.exitCode = 1; });
}
