/**
 * The guided tour on screen (SpotlightOverlay, TourChapterList). The overlay dims the app, cuts a
 * hole around the stop's anchor — any element marked `data-tour="<Anchor>"` — and puts the caption
 * beside it. It takes every pointer event, as Android's does: the learner moves with Next and Back,
 * and Skip is one tap away on every stop.
 */
import { createPortal } from 'preact/compat';
import { useEffect, useRef, useState } from 'preact/hooks';
import { chapterById, chapterState, sanitizeResume, TOUR_CHAPTERS, type TourChapterState, type TourStop } from '../domain/tour';
import { navigate, useLocation } from '../lib/router';
import { useApp } from '../lib/store';
import { nextStop, openChapter, previousStop, skipTour } from '../lib/tour';
import { Icon } from './kit';

/** How far the hole reaches past the element, so a clay lip or a shadow is not clipped. */
const PAD = 8;
/** How long a stop waits for its anchor to render after navigating before explaining the screen as a whole. */
const ANCHOR_WAIT_MS = 1500;
/** How long the hole follows a found anchor frame by frame: longer than the app's longest transition. */
const SETTLE_MS = 600;

interface Hole {
  top: number;
  left: number;
  width: number;
  height: number;
  radius: number;
}

function findAnchor(anchor: string): HTMLElement | null {
  // A tab is marked in both the bottom bar and the side rail; only one of them is showing.
  for (const el of document.querySelectorAll<HTMLElement>(`[data-tour="${anchor}"]`)) {
    const r = el.getBoundingClientRect();
    if (r.width > 0 && r.height > 0) return el;
  }
  return null;
}

function measure(el: HTMLElement): Hole {
  const r = el.getBoundingClientRect();
  const corner = parseFloat(getComputedStyle(el).borderTopLeftRadius) || 0;
  const width = r.width + PAD * 2;
  const height = r.height + PAD * 2;
  return { top: r.top - PAD, left: r.left - PAD, width, height, radius: Math.min(Math.max(corner + PAD, 12), width / 2, height / 2) };
}

/** Finds, scrolls to and follows the current stop's anchor. Null once found missing: the stop explains the whole screen. */
function useHole(stop: TourStop, path: string, key: string): { hole: Hole | null; ready: boolean } {
  const [state, setState] = useState<{ hole: Hole | null; ready: boolean }>({ hole: null, ready: false });
  useEffect(() => {
    setState({ hole: null, ready: false });
    if (path !== stop.route) return;
    if (!stop.anchor) {
      window.scrollTo(0, 0);
      setState({ hole: null, ready: true });
      return;
    }
    let el: HTMLElement | null = null;
    let frame = 0;
    const started = performance.now();
    const resized = new ResizeObserver(() => follow());
    const follow = () => el && setState({ hole: measure(el), ready: true });
    // An anchor can still be settling when it appears (the open tab grows into its pill), so the
    // hole tracks it frame by frame for a moment, then on size, scroll and window changes.
    const settle = (until: number) => {
      follow();
      if (performance.now() < until) frame = requestAnimationFrame(() => settle(until));
    };
    const look = () => {
      el = findAnchor(stop.anchor!);
      if (el) {
        el.scrollIntoView({ block: 'center', inline: 'nearest' });
        resized.observe(el);
        frame = requestAnimationFrame(() => settle(performance.now() + SETTLE_MS));
      } else if (performance.now() - started < ANCHOR_WAIT_MS) {
        frame = requestAnimationFrame(look);
      } else {
        setState({ hole: null, ready: true });
      }
    };
    frame = requestAnimationFrame(look);
    window.addEventListener('resize', follow);
    window.addEventListener('scroll', follow, true);
    return () => {
      cancelAnimationFrame(frame);
      resized.disconnect();
      window.removeEventListener('resize', follow);
      window.removeEventListener('scroll', follow, true);
    };
  }, [key, path]);
  return state;
}

/** Below the hole when there is more room there, above it otherwise; centred when there is no hole. */
function captionPlacement(hole: Hole | null): preact.JSX.CSSProperties {
  if (!hole) return { top: '50%', left: '50%', transform: 'translate(-50%, -50%)' };
  const width = Math.min(400, window.innerWidth - 32);
  const left = Math.min(Math.max(hole.left + hole.width / 2 - width / 2, 16), window.innerWidth - width - 16);
  const below = window.innerHeight - (hole.top + hole.height);
  return below >= hole.top
    ? { top: Math.min(hole.top + hole.height + 12, window.innerHeight - 160), left }
    : { bottom: Math.min(window.innerHeight - hole.top + 12, window.innerHeight - 160), left };
}

export function TourOverlay() {
  const tour = useApp((s) => s.tour);
  const { path } = useLocation();
  const chapter = tour ? chapterById(tour.chapter) : undefined;
  const stop = chapter?.stops[tour!.index];
  const key = tour ? `${tour.chapter}:${tour.index}` : '';
  const { hole, ready } = useHole(stop ?? { route: '', anchor: null, title: '', body: '' }, path, key);
  const next = useRef<HTMLButtonElement>(null);

  // The tour shows each stop on its own screen, without piling those screens onto the history.
  useEffect(() => {
    if (stop && path !== stop.route) navigate(stop.route, { replace: true });
  }, [key, path]);

  useEffect(() => {
    if (!tour) return;
    // Back, from the keyboard or the browser, steps back a stop; at the first stop it leaves.
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        previousStop();
      }
    };
    const onPop = () => previousStop();
    document.addEventListener('keydown', onKey);
    window.addEventListener('popstate', onPop);
    return () => {
      document.removeEventListener('keydown', onKey);
      window.removeEventListener('popstate', onPop);
    };
  }, [!!tour]);

  useEffect(() => {
    if (ready) next.current?.focus();
  }, [ready, key]);

  if (!tour || !chapter || !stop) return null;
  const isLast = tour.index >= chapter.stops.length - 1;

  return createPortal(
    <div class="tour" role="dialog" aria-modal="true" aria-labelledby="tour-title" aria-describedby="tour-body">
      {hole ? (
        <div class="tour-hole" style={{ top: hole.top, left: hole.left, width: hole.width, height: hole.height, borderRadius: hole.radius }} />
      ) : (
        <div class="tour-dim" />
      )}
      {ready && (
        <div class="tour-card stack-sm" style={captionPlacement(hole)} key={key}>
          <div class="row">
            <p class="t-label-s muted grow">
              {chapter.title} · {tour.index + 1} of {chapter.stops.length}
            </p>
            <button class="text-btn muted" style={{ minHeight: 36, fontSize: 15 }} onClick={skipTour}>
              Skip
            </button>
          </div>
          <h2 class="t-title-l" id="tour-title">{stop.title}</h2>
          <p class="t-body-l muted" id="tour-body">{stop.body}</p>
          {stop.gesture === 'tap' && (
            <p class="row-xs t-label" style={{ color: 'var(--lime)' }}>
              <Icon name="fingerTap" size={18} /> Tap it after the tour
            </p>
          )}
          <div class="row" style={{ paddingTop: 'var(--s-xs)' }}>
            {tour.index > 0 && (
              <button class="text-btn" onClick={previousStop}>
                Back
              </button>
            )}
            <span class="spacer" />
            <button class="clay small" ref={next} onClick={nextStop}>
              {isLast ? 'Start learning' : 'Next'}
            </button>
          </div>
        </div>
      )}
    </div>,
    document.body
  );
}

const STATE_BADGE: Partial<Record<TourChapterState, string>> = { New: 'New', Updated: 'Updated' };

/** The optional chapters, offered rather than imposed. Core has its own Replay row. */
export function TourChapterList() {
  const prefs = useApp((s) => s.prefs);
  const resume = sanitizeResume(prefs.tourResume?.chapter, prefs.tourResume?.step);
  return (
    <div class="list">
      {TOUR_CHAPTERS.filter((c) => c.id !== 'Core').map((c) => {
        const state = chapterState(c, prefs.tourCompleted, prefs.tourSkipped, prefs.tourBaseline ?? 0, resume);
        const subtitle =
          state === 'InProgress' ? `Continue — step ${(resume?.step ?? 0) + 1} of ${c.stops.length}` : state === 'Done' ? 'Done · take it again' : c.subtitle;
        return (
          <button key={c.id} class="list-row" onClick={() => openChapter(c.id)}>
            <span class="ico" style={{ background: 'var(--lime-tint)' }}>
              <Icon name="teacher" size={20} color={state === 'Done' ? 'var(--green)' : 'var(--lime)'} />
            </span>
            <div class="grow">
              <p class="row-xs t-title-s">
                {c.title}
                {STATE_BADGE[state] && <span class="tag">{STATE_BADGE[state]}</span>}
              </p>
              <p class="t-body-s muted">{subtitle}</p>
            </div>
            <Icon name="arrowRight" size={18} color="var(--faint)" />
          </button>
        );
      })}
    </div>
  );
}
