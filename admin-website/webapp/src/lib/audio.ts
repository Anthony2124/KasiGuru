/**
 * Pronunciation clips: WordAudioRepository + AudioPlayerManager.
 *
 * Clips live in Firestore at `word_audio/{key}` as raw bytes (Storage needs the Blaze plan). A clip
 * is fetched the first time a learner asks for that word and kept in Cache Storage under a name that
 * carries `audioUpdatedAt`, so a re-recording is a new entry rather than a stale hit. A word with no
 * clip plays nothing: there is deliberately no text-to-speech fallback, because a synthetic voice
 * would mispronounce an endangered language to the people learning it.
 *
 * Playback goes through Web Audio. iOS only lets a page start sound from inside a tap, and a clip
 * that has to be downloaded first arrives after the tap has ended; an AudioContext resumed during
 * that first tap stays usable afterwards.
 */
import { doc, getDoc } from 'firebase/firestore/lite';
import type { WordContent } from '../domain/types';
import { db } from './firebase';

const CACHE = 'kasiguru-word-audio-v1';
let ctx: AudioContext | null = null;
let current: AudioBufferSourceNode | null = null;
const decoded = new Map<string, AudioBuffer>();
const inflight = new Map<string, Promise<ArrayBuffer | null>>();

/** The one AudioContext for word clips, effects and music. */
export function context(): AudioContext | null {
  if (ctx) return ctx;
  const Ctor = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!Ctor) return null;
  ctx = new Ctor();
  return ctx;
}

/** Call from any tap handler before an async gap: unlocks audio on iOS. */
export function unlockAudio() {
  const c = context();
  if (c && c.state === 'suspended') void c.resume();
}

const slug = (s: string) =>
  s
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');

/** The same key AudioPlayerManager derives when a word carries no explicit clip name. */
export function audioKey(word: Pick<WordContent, 'audioFileName' | 'kasiguranin' | 'english'>): string {
  if (word.audioFileName) return word.audioFileName;
  const k = slug(word.kasiguranin);
  const e = slug(word.english);
  return k && e ? `${k}__${e}` : k;
}

export const hasAudio = (w: Pick<WordContent, 'audioFileName'>) => !!w.audioFileName;

async function fetchClip(key: string, version: number): Promise<ArrayBuffer | null> {
  const cacheUrl = `/__word-audio/${encodeURIComponent(key)}_${version}`;
  try {
    const cache = await caches.open(CACHE);
    const hit = await cache.match(cacheUrl);
    if (hit) return await hit.arrayBuffer();
    const snap = await getDoc(doc(db, 'word_audio', key));
    if (!snap.exists()) return null;
    const data = snap.data();
    const bytes: Uint8Array | undefined = data.data?.toUint8Array?.();
    if (!bytes || !bytes.length) return null;
    const mime = typeof data.mimeType === 'string' ? data.mimeType : 'audio/mp4';
    const copy = bytes.slice().buffer as ArrayBuffer;
    await cache.put(cacheUrl, new Response(copy.slice(0), { headers: { 'Content-Type': mime } }));
    return copy;
  } catch (e) {
    // Cache Storage can be unavailable (private mode); fall back to a plain read.
    try {
      const snap = await getDoc(doc(db, 'word_audio', key));
      const bytes: Uint8Array | undefined = snap.exists() ? snap.data().data?.toUint8Array?.() : undefined;
      return bytes && bytes.length ? (bytes.slice().buffer as ArrayBuffer) : null;
    } catch {
      console.warn('audio unavailable for', key, e);
      return null;
    }
  }
}

function clip(key: string, version: number) {
  const id = `${key}_${version}`;
  let p = inflight.get(id);
  if (!p) {
    p = fetchClip(key, version);
    inflight.set(id, p);
    p.then((r) => {
      if (!r) inflight.delete(id);
    });
  }
  return p;
}

/** Starts the download without playing, so a listening exercise is ready when it appears. */
export function prefetch(word: WordContent) {
  if (!word.audioFileName || !navigator.onLine) return;
  void clip(audioKey(word), word.audioUpdatedAt);
}

let onWordClip: ((seconds: number) => void) | null = null;

/** The music lowers itself under a recording, as it answers a ducking audio-focus request on Android. */
export function setWordClipListener(fn: (seconds: number) => void) {
  onWordClip = fn;
}

export function stopAudio() {
  try {
    current?.stop();
  } catch {
    /* already stopped */
  }
  current = null;
}

/** Plays a word's clip. Resolves false when the word has no recording or it could not be loaded. */
export async function playWord(word: WordContent): Promise<boolean> {
  unlockAudio();
  stopAudio();
  const key = audioKey(word);
  if (!key || !word.audioFileName) return false;
  const id = `${key}_${word.audioUpdatedAt}`;
  const c = context();
  if (!c) return false;
  try {
    let buffer = decoded.get(id);
    if (!buffer) {
      const raw = await clip(key, word.audioUpdatedAt);
      if (!raw) return false;
      buffer = await c.decodeAudioData(raw.slice(0));
      decoded.set(id, buffer);
    }
    if (c.state === 'suspended') await c.resume();
    const src = c.createBufferSource();
    src.buffer = buffer;
    src.connect(c.destination);
    src.start();
    current = src;
    onWordClip?.(buffer.duration);
    return true;
  } catch (e) {
    console.warn('could not play', key, e);
    return false;
  }
}

// ── Sound effects (SoundEffects.kt) ──────────────────────────────────────────

/**
 * The app's short sounds and each one's level at the default 50% volume (`Sfx` in SoundEffects.kt).
 * The files are converted from the APK's by `npm run sounds`; see data/audio/ for their sources.
 */
const SFX = {
  correct: { file: 'correct.mp3', gain: 0.35 },
  wrong: { file: 'wrong.mp3', gain: 0.35 },
  found: { file: 'found.mp3', gain: 0.4 },
  complete: { file: 'complete.mp3', gain: 0.35 },
  'level-up': { file: 'level-up.wav', gain: 0.35 },
  streak: { file: 'streak.mp3', gain: 0.35 },
  badge: { file: 'badge.mp3', gain: 0.35 },
  flip: { file: 'flip.mp3', gain: 0.3 },
  tap: { file: 'tap.wav', gain: 0.15 },
} as const;
export type Sfx = keyof typeof SFX;

/** Where the sound and music volume sliders start: the level the sounds were tuned at. */
export const DEFAULT_VOLUME_PERCENT = 50;

export interface SoundSettings {
  effects: boolean;
  taps: boolean;
  effectsVolume: number;
}

let soundSettings: () => SoundSettings = () => ({ effects: true, taps: true, effectsVolume: DEFAULT_VOLUME_PERCENT });

/** The learner's settings decide what is heard; callers just play. Wired up once in main.tsx. */
export function configureSounds(read: () => SoundSettings) {
  soundSettings = read;
}

/** A sound's level at [percent]: its own level is the 50% one, so the slider doubles it at 100%. */
export const sfxVolume = (sfx: Sfx, percent: number) => Math.min(1, Math.max(0, (SFX[sfx].gain * percent) / DEFAULT_VOLUME_PERCENT));

const sfxBuffers = new Map<Sfx, AudioBuffer>();
let sfxLoading: Promise<void> | null = null;

/** Every effect, decoded once. Until they arrive, a synthesised tone stands in for answers. */
function loadSfx(c: AudioContext) {
  sfxLoading ??= Promise.all(
    (Object.keys(SFX) as Sfx[]).map(async (name) => {
      try {
        const res = await fetch(`/sounds/${SFX[name].file}`);
        if (res.ok) sfxBuffers.set(name, await c.decodeAudioData(await res.arrayBuffer()));
      } catch {
        // The synthesised fallback keeps working.
      }
    })
  ).then(() => undefined);
}

function playAt(name: Sfx, percent: number): boolean {
  const c = context();
  if (!c || c.state !== 'running') return false;
  loadSfx(c);
  const buffer = sfxBuffers.get(name);
  const volume = sfxVolume(name, percent);
  if (!buffer) return false;
  if (volume <= 0) return true;
  const src = c.createBufferSource();
  const gain = c.createGain();
  gain.gain.value = volume;
  src.buffer = buffer;
  src.connect(gain).connect(c.destination);
  src.start();
  return true;
}

/** Plays an effect if the learner has that kind of sound on. Tap clicks have their own switch. */
export function playSfx(name: Sfx): boolean {
  const s = soundSettings();
  if (!(name === 'tap' ? s.taps : s.effects)) return true;
  return playAt(name, s.effectsVolume);
}

/** Plays a sample at [percent] for the Settings slider, before the new level is saved. */
export function previewSfx(percent: number) {
  unlockAudio();
  playAt('correct', percent);
}

// ── Tap sounds (TapSounds.kt) ────────────────────────────────────────────────

/** What counts as tapping something. Text fields are left out: a tap there only moves the cursor. */
const TAPPABLE = 'button, a[href], summary, select, input[type="checkbox"], input[type="radio"], [role="button"], [role="tab"], [role="switch"], [role="radio"], [role="checkbox"], [role="menuitem"]';
/** A press held longer than this is not a tap (Compose's long-press timeout). */
const LONG_PRESS_MS = 500;

/**
 * Plays the soft click for every tappable thing in the app, without each button having to ask for
 * it. Only a press with a finger or mouse clicks: activating a control from the keyboard or a screen
 * reader stays silent, as TalkBack's activations do on Android. A control whose tap already makes
 * its own sound (an answer) carries `data-no-tap-sound`. The opt-out belongs to the control it marks,
 * not to separate buttons nested inside it.
 */
export function installTapSounds() {
  let pressedAt = -Infinity;
  document.addEventListener('pointerdown', () => (pressedAt = performance.now()), { capture: true, passive: true });
  document.addEventListener(
    'click',
    (e) => {
      if (!soundSettings().taps || performance.now() - pressedAt > LONG_PRESS_MS) return;
      const target = e.target instanceof Element ? e.target.closest(TAPPABLE) : null;
      if (!target || target.hasAttribute('data-no-tap-sound')) return;
      // One click per press: a label forwards its click to its checkbox, which would otherwise click twice.
      pressedAt = -Infinity;
      const c = context();
      if (!c) return;
      // iOS starts the context suspended; a tap is the moment it may be resumed.
      if (c.state === 'suspended') void c.resume().then(() => playSfx('tap'));
      else playSfx('tap');
    },
    true
  );
}

/** The right- or wrong-answer sound, with a synthesised tone until the files have loaded. */
export function feedbackTone(correct: boolean) {
  const s = soundSettings();
  if (!s.effects) return;
  if (playSfx(correct ? 'correct' : 'wrong')) return;
  const c = context();
  const peak = (0.12 * s.effectsVolume) / DEFAULT_VOLUME_PERCENT;
  if (!c || c.state !== 'running' || peak <= 0.0001) return;
  const now = c.currentTime;
  const osc = c.createOscillator();
  const gain = c.createGain();
  osc.type = 'sine';
  const notes = correct ? [660, 880] : [300, 220];
  osc.frequency.setValueAtTime(notes[0], now);
  osc.frequency.setValueAtTime(notes[1], now + 0.09);
  gain.gain.setValueAtTime(0.0001, now);
  gain.gain.exponentialRampToValueAtTime(peak, now + 0.02);
  gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.28);
  osc.connect(gain).connect(c.destination);
  osc.start(now);
  osc.stop(now + 0.3);
}

/**
 * Whether this device can vibrate: Android phones can; iPhones cannot; a computer's browser may
 * offer the call but has nothing to buzz, hence the touch check.
 */
export const canVibrate = () =>
  typeof navigator !== 'undefined' &&
  typeof navigator.vibrate === 'function' &&
  typeof matchMedia === 'function' &&
  matchMedia('(pointer: coarse)').matches;

/** A short buzz for an answer, as the lesson player's haptics on Android. */
export function answerHaptic(correct: boolean) {
  if (canVibrate()) navigator.vibrate(correct ? 18 : [28, 40, 28]);
}
