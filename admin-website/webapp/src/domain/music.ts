/**
 * Which background loop a screen plays (domain/audio/MusicMood.kt). Lessons, stories and the review
 * deck stay silent because a learner is listening to pronunciation recordings there; first-run screens
 * stay silent so the app does not open with music before the learner has seen the settings.
 */
export type MusicMood = 'menu' | 'game' | 'none';

/** The Practice tab is Android's game hub (`games`), so it plays the game loop too. */
export function musicMoodFor(path: string | null): MusicMood {
  if (path == null) return 'none';
  if (path === '/practice' || path === '/games' || path.startsWith('/games/')) return 'game';
  if (path.startsWith('/lesson/')) return 'none';
  if (path === '/stories' || path.startsWith('/story/')) return 'none';
  if (path === '/review') return 'none';
  if (path === '/onboarding') return 'none';
  return 'menu';
}
