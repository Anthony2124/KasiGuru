#!/usr/bin/env node
'use strict';

// Generates KasiGuru's background music loops with the ElevenLabs Music API.
//
//   node scripts/audio/elevenlabs-music.js generate [slot ...] [--takes N]
//       Composes N takes (default 2) per slot from the prompts in data/audio/elevenlabs-music.json
//       and writes them, with an audition page, to ../audio-candidates/elevenlabs/ (outside the repo).
//   node scripts/audio/elevenlabs-music.js install <slot> <take.mp3>
//       Turns one take into a seamless loop (the last seconds crossfaded into the first), matches the
//       slot's loudness, encodes res/raw/<slot>.ogg and records where it came from.
//
// The API key is read from ELEVENLABS_API_KEY or ../private/elevenlabs-api-key.txt. It must never
// enter the repo or the APK. `install` needs ffmpeg: FFMPEG=<path>, ffmpeg on PATH, or ffmpeg-static.

const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { PROJECT_ROOT, RAW_RESOURCES, audioData, privateFile } = require('../lib/project-paths');

const CONFIG = audioData('elevenlabs-music.json');
const FREESOUND_PICKS = audioData('freesound-sounds.json');
const CANDIDATES = path.join(PROJECT_ROOT, '..', 'audio-candidates', 'elevenlabs');
const MODEL = 'music_v1';
const LOOP_CROSSFADE_S = 3;

function apiKey() {
  if (process.env.ELEVENLABS_API_KEY) return process.env.ELEVENLABS_API_KEY.trim();
  const file = privateFile('elevenlabs-api-key.txt');
  if (!fs.existsSync(file)) throw new Error(`No API key: set ELEVENLABS_API_KEY or create ${file}`);
  return fs.readFileSync(file, 'utf8').trim();
}

function ffmpeg() {
  if (process.env.FFMPEG) return process.env.FFMPEG;
  try {
    execFileSync('ffmpeg', ['-version'], { stdio: 'ignore' });
    return 'ffmpeg';
  } catch {
    try {
      return require('ffmpeg-static');
    } catch {
      throw new Error('ffmpeg not found: set FFMPEG, put ffmpeg on PATH, or install ffmpeg-static');
    }
  }
}

const readConfig = () => JSON.parse(fs.readFileSync(CONFIG, 'utf8'));
const writeJson = (file, value) => fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');

async function compose(spec) {
  const response = await fetch('https://api.elevenlabs.io/v1/music?output_format=mp3_44100_192', {
    method: 'POST',
    headers: { 'xi-api-key': apiKey(), 'Content-Type': 'application/json' },
    body: JSON.stringify({
      prompt: spec.prompt,
      music_length_ms: spec.lengthMs,
      model_id: MODEL,
      force_instrumental: true,
    }),
  });
  if (!response.ok) throw new Error(`ElevenLabs ${response.status}: ${(await response.text()).slice(0, 400)}`);
  return Buffer.from(await response.arrayBuffer());
}

function auditionPage(files) {
  const rows = files.map(({ label, name }) =>
    `<section><h2>${label} <small>${name}</small></h2><audio controls loop preload="none" src="${name}"></audio></section>`);
  return `<!doctype html><meta charset="utf-8"><title>KasiGuru music takes</title>
<style>body{font:16px system-ui;background:#0b0f0c;color:#eef;max-width:720px;margin:2rem auto;padding:0 1rem}
section{margin:1.5rem 0}audio{width:100%}small{color:#9a9;font-weight:400}</style>
<h1>KasiGuru music takes</h1><p>Each plays on loop. Install one with <code>install &lt;slot&gt; &lt;file&gt;</code>.</p>
${rows.join('\n')}`;
}

async function generate(args) {
  const config = readConfig();
  const takesAt = args.indexOf('--takes');
  const takes = takesAt >= 0 ? Number(args[takesAt + 1]) : 2;
  const slots = args.filter((a, i) => !a.startsWith('--') && args[i - 1] !== '--takes');
  const wanted = slots.length ? slots : Object.keys(config.slots);
  fs.mkdirSync(CANDIDATES, { recursive: true });

  for (const slot of wanted) {
    const spec = config.slots[slot];
    if (!spec) throw new Error(`Unknown slot ${slot}`);
    for (let take = 1; take <= takes; take++) {
      const name = `${slot}-take${take}.mp3`;
      process.stdout.write(`${name} ... `);
      const bytes = await compose(spec);
      fs.writeFileSync(path.join(CANDIDATES, name), bytes);
      console.log(`${(bytes.length / 1024).toFixed(0)} KB`);
    }
  }

  const files = fs.readdirSync(CANDIDATES).filter((f) => f.endsWith('.mp3')).sort().map((name) => {
    const slot = name.replace(/-take\d+\.mp3$/, '');
    return { name, label: config.slots[slot]?.label || slot };
  });
  fs.writeFileSync(path.join(CANDIDATES, 'index.html'), auditionPage(files));
  console.log(`\nAudition: ${path.join(CANDIDATES, 'index.html')}`);
}

function durationSeconds(bin, file) {
  let out = '';
  try {
    execFileSync(bin, ['-hide_banner', '-i', file], { stdio: ['ignore', 'ignore', 'pipe'] });
  } catch (e) {
    out = String(e.stderr || '');
  }
  const m = out.match(/Duration: (\d+):(\d+):([\d.]+)/);
  if (!m) throw new Error(`Could not read the duration of ${file}`);
  return Number(m[1]) * 3600 + Number(m[2]) * 60 + Number(m[3]);
}

function install(args) {
  const [slot, take] = args;
  const config = readConfig();
  const spec = config.slots[slot];
  if (!spec || !take) throw new Error('Usage: install <slot> <take.mp3>');
  const source = fs.existsSync(take) ? take : path.join(CANDIDATES, take);
  if (!fs.existsSync(source)) throw new Error(`No such take: ${take}`);

  const bin = ffmpeg();
  const d = durationSeconds(bin, source);
  const f = LOOP_CROSSFADE_S;
  // body = [f, d-f]; the tail fades out over the head fading in, and that overlap leads back into
  // the body's first sample, so the loop point is the middle of a crossfade rather than a cut.
  const graph = [
    '[0:a]asplit=3[a][b][c]',
    `[a]atrim=${f}:${d - f},asetpts=PTS-STARTPTS[body]`,
    `[b]atrim=${d - f}:${d},asetpts=PTS-STARTPTS,afade=t=out:st=0:d=${f}[tail]`,
    `[c]atrim=0:${f},asetpts=PTS-STARTPTS,afade=t=in:st=0:d=${f}[head]`,
    '[tail][head]amix=inputs=2:normalize=0[seam]',
    '[body][seam]concat=n=2:v=0:a=1,' +
      `loudnorm=I=${spec.loudnessLufs}:TP=-1.5:LRA=11,aresample=44100[out]`,
  ].join(';');
  const target = path.join(RAW_RESOURCES, `${slot}.ogg`);
  execFileSync(bin, ['-hide_banner', '-loglevel', 'error', '-y', '-i', source, '-filter_complex', graph,
    '-map', '[out]', '-ac', '2', '-c:a', 'libvorbis', '-q:a', '5', target], { stdio: 'inherit' });

  config.installed[slot] = {
    source: 'ElevenLabs Music API',
    model: MODEL,
    take: path.basename(source),
    prompt: spec.prompt,
    loopCrossfadeSeconds: f,
    loudnessLufs: spec.loudnessLufs,
    installedAt: new Date().toISOString(),
  };
  writeJson(CONFIG, config);

  // Freesound's `fetch` rewrites every slot it has a pick for; drop this one so it cannot put the
  // old loop back over the new one.
  if (fs.existsSync(FREESOUND_PICKS)) {
    const picks = JSON.parse(fs.readFileSync(FREESOUND_PICKS, 'utf8'));
    for (const value of Object.values(picks)) {
      if (value && typeof value === 'object' && slot in value) delete value[slot];
    }
    if (slot in picks) delete picks[slot];
    writeJson(FREESOUND_PICKS, picks);
  }
  console.log(`${slot}.ogg  ${(fs.statSync(target).size / 1024).toFixed(0)} KB  (${(d - f).toFixed(1)} s loop)`);
}

const [command, ...rest] = process.argv.slice(2);
const run = { generate, install }[command];
if (!run) {
  console.log('Usage: elevenlabs-music.js generate [slot ...] [--takes N] | install <slot> <take.mp3>');
  process.exit(1);
}
// exitCode rather than exit(): exiting while fetch still holds sockets trips a libuv assertion on Windows.
Promise.resolve(run(rest)).catch((e) => {
  console.error(e.message);
  process.exitCode = 1;
});
