/**
 * Background music for menus and games (util/audio/MusicPlayer.kt). The screen sets the mood, the
 * page being visible stands in for the app being in the foreground, and the learner's settings turn
 * it on or off and set its level.
 *
 * Each loop is an <audio> element played through a GainNode: iOS ignores an element's own volume, so
 * fades and the slider have to happen in Web Audio. The element offers the APK's Ogg first, which
 * Chrome and Firefox loop without a gap, and an AAC copy that Safari takes instead. Nothing downloads
 * until a loop first plays, and a browser only lets music start from a tap, so it begins on the first
 * one. A word recording lowers the music while it plays, as audio focus ducks it on Android.
 */
import type { MusicMood } from '../domain/music';
import { context, DEFAULT_VOLUME_PERCENT, setWordClipListener } from './audio';

/** The level at the default 50%, so the slider doubles it at 100%. */
const VOLUME = 0.35;
const DUCK_RATIO = 0.2;
const FADE_S = 0.6;
const QUICK_S = 0.15;

type Track = Exclude<MusicMood, 'none'>;
interface Loop {
  el: HTMLAudioElement;
  gain: GainNode;
  level: number;
  stopTimer?: ReturnType<typeof setTimeout>;
}

const loops = new Map<Track, Loop>();
let mood: MusicMood = 'none';
let enabled = false;
let volumePercent = DEFAULT_VOLUME_PERCENT;
let previewPercent: number | null = null;
let visible = typeof document === 'undefined' || !document.hidden;
/** A browser refuses to start media before the learner has interacted with the page. */
let interacted = false;
let ducked = false;
let duckTimer: ReturnType<typeof setTimeout> | undefined;

const percent = () => previewPercent ?? volumePercent;
const target = (): MusicMood => (enabled && percent() > 0 && visible && interacted ? mood : 'none');

function loopFor(track: Track): Loop | null {
  const existing = loops.get(track);
  if (existing) return existing;
  const c = context();
  if (!c) return null;
  const el = document.createElement('audio');
  el.loop = true;
  el.preload = 'auto';
  for (const [ext, type] of [['ogg', 'audio/ogg; codecs=vorbis'], ['m4a', 'audio/mp4']]) {
    const source = document.createElement('source');
    source.src = `/music/${track}.${ext}`;
    source.type = type;
    el.appendChild(source);
  }
  const gain = c.createGain();
  gain.gain.value = 0;
  c.createMediaElementSource(el).connect(gain).connect(c.destination);
  const loop: Loop = { el, gain, level: 0 };
  loops.set(track, loop);
  return loop;
}

function rampTo(loop: Loop, level: number, seconds: number) {
  const c = context();
  if (!c) return;
  const g = loop.gain.gain;
  const now = c.currentTime;
  g.cancelScheduledValues(now);
  g.setValueAtTime(g.value, now);
  if (seconds > 0) g.linearRampToValueAtTime(level, now + seconds);
  else g.setValueAtTime(level, now);
  loop.level = level;
}

function sync() {
  const want = target();
  for (const [track, loop] of loops) {
    if (track === want || loop.el.paused) continue;
    clearTimeout(loop.stopTimer);
    // Leaving the app stops the music at once; only a change of screen fades it out.
    if (!visible) {
      loop.el.pause();
      rampTo(loop, 0, 0);
      continue;
    }
    rampTo(loop, 0, FADE_S);
    loop.stopTimer = setTimeout(() => {
      if (target() !== track) loop.el.pause();
    }, FADE_S * 1000 + 50);
  }
  if (want === 'none') return;

  const loop = loopFor(want);
  const c = context();
  if (!loop || !c) return;
  clearTimeout(loop.stopTimer);
  const full = Math.min(1, Math.max(0, (VOLUME * percent()) / DEFAULT_VOLUME_PERCENT));
  const volume = ducked ? full * DUCK_RATIO : full;
  const wasPlaying = !loop.el.paused;
  if (!wasPlaying) {
    if (c.state === 'suspended') void c.resume();
    loop.el.play().catch(() => {
      // Not allowed yet, or the file could not load: the next tap tries again from silence.
      rampTo(loop, 0, 0);
    });
  }
  // Already playing means only the level changed (a duck or the slider): answer quickly.
  if (loop.level !== volume) rampTo(loop, volume, wasPlaying ? QUICK_S : FADE_S);
}

/** Starts the music on the first interaction and stops it whenever the page is hidden. */
export function installMusic() {
  const onInteract = () => {
    interacted = true;
    sync();
  };
  for (const type of ['click', 'keydown', 'touchend']) document.addEventListener(type, onInteract, { capture: true, passive: true });
  document.addEventListener('visibilitychange', () => {
    visible = !document.hidden;
    sync();
  });
  setWordClipListener((seconds) => {
    ducked = true;
    sync();
    clearTimeout(duckTimer);
    duckTimer = setTimeout(() => {
      ducked = false;
      sync();
    }, seconds * 1000 + 150);
  });
}

export function setMusicMood(next: MusicMood) {
  mood = next;
  sync();
}

export function setMusicSettings(on: boolean, volume: number) {
  if (on === enabled && volume === volumePercent) return;
  enabled = on;
  volumePercent = volume;
  sync();
}

/** Follows the Settings slider while it is dragged; the saved value takes over on release (null). */
export function previewMusicVolume(value: number | null) {
  previewPercent = value;
  sync();
}
