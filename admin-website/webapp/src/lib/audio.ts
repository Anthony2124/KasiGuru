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
export function feedbackTone(correct: boolean) {
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
