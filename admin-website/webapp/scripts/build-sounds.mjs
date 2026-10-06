/**
 * Converts the APK's sounds (app/src/main/res/raw, CC0 from Freesound: data/audio/freesound-sounds.json)
 * into files every browser can play. Android ships Ogg Vorbis, which not every iPhone Safari decodes:
 *
 * - Effects become MP3 in public/sounds, which the service worker precaches. MP3 is the one format
 *   every browser's decodeAudioData accepts. The tap stays 16-bit WAV so the click has no decoder delay.
 * - Music keeps the original Ogg (gapless loops in Chrome and Firefox) plus an AAC copy for Safari, in
 *   public/music. Music is not precached: it is most of the audio weight, and it is optional.
 *
 * Needs ffmpeg, which the app does not depend on: on PATH, in $FFMPEG, or `npm i --no-save ffmpeg-static`.
 */
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const webapp = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const raw = path.resolve(webapp, '..', '..', 'app', 'src', 'main', 'res', 'raw');

async function ffmpegPath() {
  if (process.env.FFMPEG) return process.env.FFMPEG;
  try {
    return (await import('ffmpeg-static')).default;
  } catch {
    return 'ffmpeg';
  }
}

const EFFECTS = ['correct', 'wrong', 'found', 'complete', 'streak', 'badge', 'flip'];

async function main() {
  const ffmpeg = await ffmpegPath();
  const run = (input, output, args) =>
    execFileSync(ffmpeg, ['-hide_banner', '-loglevel', 'error', '-y', '-i', path.join(raw, input), '-map_metadata', '-1', ...args, output]);
  const sounds = path.join(webapp, 'public', 'sounds');
  const music = path.join(webapp, 'public', 'music');
  fs.mkdirSync(sounds, { recursive: true });
  fs.mkdirSync(music, { recursive: true });

  for (const name of EFFECTS) run(`ui_${name}.ogg`, path.join(sounds, `${name}.mp3`), ['-c:a', 'libmp3lame', '-q:a', '4']);
  run('ui_tap.ogg', path.join(sounds, 'tap.wav'), ['-ac', '1', '-c:a', 'pcm_s16le']);
  fs.copyFileSync(path.join(raw, 'ui_level_up.wav'), path.join(sounds, 'level-up.wav'));

  for (const mood of ['menu', 'game']) {
    fs.copyFileSync(path.join(raw, `music_${mood}.ogg`), path.join(music, `${mood}.ogg`));
    run(`music_${mood}.ogg`, path.join(music, `${mood}.m4a`), ['-c:a', 'aac', '-b:a', '112k', '-movflags', '+faststart']);
  }
  console.log(`sounds: ${EFFECTS.length + 2}, music: 2 (ogg + m4a)`);
}

main().catch((e) => {
  if (e.code === 'ENOENT') console.error('ffmpeg not found: put it on PATH, set FFMPEG, or npm i --no-save ffmpeg-static.');
  else console.error(e);
  process.exit(1);
});
