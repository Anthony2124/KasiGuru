/**
 * Which copy of a word's pronunciation clip plays (WordAudioRepository.planClip on Android).
 *
 * content/audio.json lists the clips this site serves, each at the `audioUpdatedAt` it was exported
 * at. A site copy is only trusted at the exact version the word points at, so an admin re-recording
 * still reaches learners; the earlier take stays as the fallback for when the newer one cannot be
 * fetched (offline, or the day's Firestore reads spent).
 */

/** One clip shipped with the site: the version it was exported at, and its file under /audio/. */
export interface BundledClip {
  v: number;
  file: string;
  bytes: number;
}

export interface ClipPlan {
  /** Play this site copy: it is the take the word points at. */
  site: BundledClip | null;
  /** Fetch from Firestore, and play this earlier site copy if that fails. */
  fallback: BundledClip | null;
}

export function clipPlan(clip: BundledClip | undefined, version: number): ClipPlan {
  if (!clip) return { site: null, fallback: null };
  return clip.v === version ? { site: clip, fallback: null } : { site: null, fallback: clip };
}
