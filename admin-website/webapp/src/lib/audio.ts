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

function context(): AudioContext | null {
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
    return true;
  } catch (e) {
    console.warn('could not play', key, e);
    return false;
  }
}

/** Short UI sounds for right and wrong answers, synthesised so there is nothing to download. */
// ── Lesson sounds (UiFeedbackSounds) ─────────────────────────────────────────

type UiSound = 'correct' | 'wrong' | 'level-up' | 'tap';
const uiSounds = new Map<UiSound, AudioBuffer>();
let uiLoading: Promise<void> | null = null;
/** Each sound's level, as Sfx sets it at the Android default volume. */
const UI_GAIN: Record<UiSound, number> = { correct: 0.35, wrong: 0.35, 'level-up': 0.35, tap: 0.15 };

/** Adrian's answer, level-up and tap sounds, decoded once. Until they arrive, a synthesised tone stands in for answers. */
function loadUiSounds(c: AudioContext) {
  uiLoading ??= Promise.all(
    (Object.keys(UI_GAIN) as UiSound[]).map(async (name) => {
      try {
        const res = await fetch(`/sounds/${name}.wav`);
        if (res.ok) uiSounds.set(name, await c.decodeAudioData(await res.arrayBuffer()));
      } catch {
        // The synthesised fallback keeps working.
      }
    })
  ).then(() => undefined);
}

function playUiSound(name: UiSound): boolean {
  const c = context();
  if (!c || c.state !== 'running') return false;
  loadUiSounds(c);
  const buffer = uiSounds.get(name);
  if (!buffer) return false;
  const src = c.createBufferSource();
  const gain = c.createGain();
  gain.gain.value = UI_GAIN[name];
  src.buffer = buffer;
  src.connect(gain).connect(c.destination);
  src.start();
  return true;
}

export function levelUpSound() {
  playUiSound('level-up');
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
export function installTapSounds(enabled: () => boolean) {
  let pressedAt = -Infinity;
  document.addEventListener('pointerdown', () => (pressedAt = performance.now()), { capture: true, passive: true });
  document.addEventListener(
    'click',
    (e) => {
      if (!enabled() || performance.now() - pressedAt > LONG_PRESS_MS) return;
      const target = e.target instanceof Element ? e.target.closest(TAPPABLE) : null;
      if (!target || target.hasAttribute('data-no-tap-sound')) return;
      // One click per press: a label forwards its click to its checkbox, which would otherwise click twice.
      pressedAt = -Infinity;
      const c = context();
      if (!c) return;
      // iOS starts the context suspended; a tap is the moment it may be resumed.
      if (c.state === 'suspended') void c.resume().then(() => playUiSound('tap'));
      else playUiSound('tap');
    },
    true
  );
}

export function feedbackTone(correct: boolean) {
  if (playUiSound(correct ? 'correct' : 'wrong')) return;
  const c = context();
  if (!c || c.state !== 'running') return;
  const now = c.currentTime;
  const osc = c.createOscillator();
  const gain = c.createGain();
  osc.type = 'sine';
  const notes = correct ? [660, 880] : [300, 220];
  osc.frequency.setValueAtTime(notes[0], now);
  osc.frequency.setValueAtTime(notes[1], now + 0.09);
  gain.gain.setValueAtTime(0.0001, now);
  gain.gain.exponentialRampToValueAtTime(0.12, now + 0.02);
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
