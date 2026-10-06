/**
 * Drives the guided tour (TourViewModel). A chapter crosses every tab, so it lives in the shell rather
 * than in any one screen; the overlay in ui/tour.tsx draws whatever stop is current.
 *
 * Where the learner is in a chapter is saved on every move, so the help page can offer to continue it.
 * Finishing records the chapter at its current version; leaving early marks it skipped, offered again
 * but never badged. Either way the learner goes back to where the chapter was started.
 */
import { chapterById, CURRENT_TUTORIAL_VERSION, sanitizeResume, stamp, type TourChapterId } from '../domain/tour';
import { navigate } from './router';
import { getState, setPrefs, setState, type Prefs } from './store';

export interface ActiveTour {
  chapter: TourChapterId;
  index: number;
  /** Where the chapter was started, and where finishing or skipping returns. */
  entry: string;
}

const here = () => location.pathname + location.search;

export function startChapter(id: TourChapterId, at = 0, entry: string | null = here()) {
  const chapter = chapterById(id);
  if (!chapter || chapter.stops.length === 0) return;
  const index = Math.min(Math.max(0, at), chapter.stops.length - 1);
  setState({ tour: { chapter: id, index, entry: entry ?? chapter.stops[0].route } });
  setPrefs({ tourResume: { chapter: id, step: index } });
}

/** Starts a chapter from the help page or Settings, continuing it if it was left part-way. */
export function openChapter(id: TourChapterId) {
  const { tourResume } = getState().prefs;
  const resume = sanitizeResume(tourResume?.chapter, tourResume?.step);
  startChapter(id, resume?.chapter === id ? resume.step : 0);
}

function move(index: number) {
  const tour = getState().tour;
  if (!tour) return;
  setState({ tour: { ...tour, index } });
  setPrefs({ tourResume: { chapter: tour.chapter, step: index } });
}

export function nextStop() {
  const tour = getState().tour;
  const chapter = tour && chapterById(tour.chapter);
  if (!tour || !chapter) return;
  if (tour.index >= chapter.stops.length - 1) finishTour();
  else move(tour.index + 1);
}

export function previousStop() {
  const tour = getState().tour;
  if (!tour) return;
  if (tour.index === 0) skipTour();
  else move(tour.index - 1);
}

function end(record: (prefs: Prefs, id: TourChapterId) => Partial<Prefs>) {
  const tour = getState().tour;
  if (!tour) return;
  setState({ tour: null });
  const prefs = getState().prefs;
  setPrefs({ ...record(prefs, tour.chapter), tourResume: null, ...(tour.chapter === 'Core' ? { tutorialPending: false } : {}) });
  navigate(tour.entry, { replace: true });
}

/** Reached the end: recorded at the chapter's current version, so a later revision re-offers it. */
export function finishTour() {
  end((prefs, id) => ({ tourCompleted: [...new Set([...prefs.tourCompleted, stamp(chapterById(id)!)])] }));
}

/** Left early: offered again later, but never badged — they already said no once. */
export function skipTour() {
  end((prefs, id) => ({ tourSkipped: [...new Set([...prefs.tourSkipped, id])] }));
}

/** Replays the core chapter from the top, for the Settings row and the help page. */
export function replayCoreTour() {
  setPrefs({ tourResume: null, tutorialPending: true });
  startChapter('Core', 0, null);
}

/** Runs the core chapter a learner is owed, picking up where it was left if the app closed mid-way. */
export function resumePendingTour() {
  const { prefs, tour } = getState();
  if (!prefs.tutorialPending || tour) return;
  const resume = sanitizeResume(prefs.tourResume?.chapter, prefs.tourResume?.step);
  startChapter('Core', resume?.chapter === 'Core' ? resume.step : 0, null);
}

/**
 * Stamps, once, which chapters this install already had: all of today's for a learner who was set up
 * before chapters arrived, none for a new one. Only chapters added later badge as New for the former.
 */
export function ensureTourBaseline(onboarded: boolean) {
  if (getState().prefs.tourBaseline == null) setPrefs({ tourBaseline: onboarded ? CURRENT_TUTORIAL_VERSION : 0 });
}
