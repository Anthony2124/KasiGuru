/**
 * Ports of the Android tests for the 1.18 and 1.23 preferences the web app now carries: the guided
 * tour (TourProgressTest, TourChaptersTest, TourAnchorAttachmentTest), background music
 * (MusicMoodTest).
 */
import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { musicMoodFor } from '../src/domain/music';
import {
  chapterState,
  coreChapter,
  CURRENT_TUTORIAL_VERSION,
  parseStamp,
  sanitizeResume,
  stamp,
  TOUR_CHAPTERS,
} from '../src/domain/tour';

const TAB_ROOTS = ['/', '/learn', '/practice', '/library', '/me'];

describe('Tour progress (TourProgressTest)', () => {
  it('a stamp round-trips', () => {
    expect(parseStamp(stamp({ id: 'Dictionary', version: 3 }))).toEqual({ id: 'Dictionary', version: 3 });
  });

  it('a malformed stamp is ignored rather than thrown on', () => {
    for (const raw of ['', 'Dictionary', 'Dictionary:', 'Dictionary:notanumber', 'ChapterThatWasRenamed:1']) expect(parseStamp(raw)).toBeNull();
  });

  it('a chapter seen at its current version reads as done', () => {
    expect(chapterState(coreChapter, [stamp(coreChapter)], [], 0, null)).toBe('Done');
  });

  it('bumping a chapter version makes exactly that chapter updated', () => {
    const revised = { ...coreChapter, version: coreChapter.version + 1 };
    const completed = [stamp(coreChapter)];
    expect(chapterState(revised, completed, [], 0, null)).toBe('Updated');
    expect(chapterState(coreChapter, completed, [], 0, null)).toBe('Done');
  });

  it('an existing install sees no new badges', () => {
    for (const c of TOUR_CHAPTERS) expect(chapterState(c, [], [], CURRENT_TUTORIAL_VERSION, null)).toBe('Available');
  });

  it('a fresh install sees every chapter as new', () => {
    for (const c of TOUR_CHAPTERS) expect(chapterState(c, [], [], 0, null)).toBe('New');
  });

  it('a chapter added after the baseline still badges', () => {
    expect(chapterState({ ...coreChapter, version: CURRENT_TUTORIAL_VERSION + 1 }, [], [], CURRENT_TUTORIAL_VERSION, null)).toBe('New');
  });

  it('skipping is offered again but never badged', () => {
    expect(chapterState(coreChapter, [], ['Core'], 0, null)).toBe('Skipped');
  });

  it('an in-flight chapter outranks every other state', () => {
    expect(chapterState(coreChapter, [stamp(coreChapter)], ['Core'], 0, { chapter: 'Core', step: 2 })).toBe('InProgress');
  });

  it('a resume point past the end, for an unknown chapter, or nonsense is discarded', () => {
    expect(sanitizeResume('Core', coreChapter.stops.length)).toBeNull();
    expect(sanitizeResume('Core', coreChapter.stops.length + 5)).toBeNull();
    expect(sanitizeResume('ChapterThatWasRenamed', 0)).toBeNull();
    expect(sanitizeResume(null, 1)).toBeNull();
    expect(sanitizeResume('', 1)).toBeNull();
    expect(sanitizeResume('Core', null)).toBeNull();
    expect(sanitizeResume('Core', -1)).toBeNull();
  });

  it('a valid resume point survives', () => {
    expect(sanitizeResume('Core', 1)).toEqual({ chapter: 'Core', step: 1 });
  });

  it('the shipped tutorial version is the highest chapter version', () => {
    expect(CURRENT_TUTORIAL_VERSION).toBe(Math.max(...TOUR_CHAPTERS.map((c) => c.version)));
  });
});

describe('Tour chapters (TourChaptersTest)', () => {
  const core = coreChapter.stops;

  it('the core chapter visits only tab roots, opening and closing on Home', () => {
    for (const s of core) expect(TAB_ROOTS).toContain(s.route);
    expect(core[0].route).toBe('/');
    expect(core[core.length - 1].route).toBe('/');
  });

  it('the core chapter holds attention: between six and nine stops', () => {
    expect(core.length).toBeLessThanOrEqual(9);
    expect(core.length).toBeGreaterThanOrEqual(6);
  });

  it('every tab is introduced exactly once', () => {
    const introduced = core.filter((s) => s.anchor?.startsWith('Nav')).map((s) => s.route);
    expect(new Set(introduced)).toEqual(new Set(TAB_ROOTS));
    expect(introduced.length).toBe(new Set(introduced).size);
  });

  it('no stop hardcodes a count, and each says more than its own name', () => {
    for (const c of TOUR_CHAPTERS)
      for (const s of c.stops) {
        expect(s.body, s.title).not.toMatch(/\d/);
        expect(s.title.trim()).not.toBe('');
        expect(s.body.length, s.title).toBeGreaterThan(40);
      }
  });

  it('every chapter appears once and none is longer than a learner will finish', () => {
    const ids = TOUR_CHAPTERS.map((c) => c.id);
    expect(ids.length).toBe(new Set(ids).size);
    for (const c of TOUR_CHAPTERS) {
      expect(c.stops.length, c.id).toBeLessThanOrEqual(9);
      expect(c.stops.length, c.id).toBeGreaterThan(0);
    }
  });

  it('every stop names a route the app serves', () => {
    const app = fs.readFileSync(path.resolve(__dirname, '../src/app.tsx'), 'utf8');
    const patterns = [...app.matchAll(/pattern: '([^']+)'/g)].map((m) => m[1]);
    for (const c of TOUR_CHAPTERS) for (const s of c.stops) expect(patterns, `${c.id}: ${s.route}`).toContain(s.route);
  });
});

describe('Tour anchors (TourAnchorAttachmentTest)', () => {
  const sources = (dir: string): string[] =>
    fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? sources(path.join(dir, e.name)) : e.name.endsWith('.tsx') ? [path.join(dir, e.name)] : []));
  const ui = sources(path.resolve(__dirname, '../src')).map((f) => fs.readFileSync(f, 'utf8')).join('\n');
  const attached = new Set([...ui.matchAll(/data-tour="([A-Za-z]+)"/g)].map((m) => m[1]));
  // The tabs are marked from the tab list: data-tour={`Nav${t.label}`}.
  if (ui.includes('data-tour={`Nav${t.label}`}')) for (const t of ['Home', 'Learn', 'Practice', 'Library', 'Me']) attached.add(`Nav${t}`);

  it('every anchor a stop points at is attached somewhere on screen', () => {
    for (const c of TOUR_CHAPTERS) for (const s of c.stops) if (s.anchor) expect(attached, `${c.id}: ${s.title}`).toContain(s.anchor);
  });
});

describe('Background music (MusicMoodTest)', () => {
  it('tab roots and menus play menu music', () => {
    for (const p of ['/', '/learn', '/library', '/me', '/settings', '/leaderboard', '/word/abc', '/achievements', '/streak']) expect(musicMoodFor(p), p).toBe('menu');
  });

  it('every game route plays game music', () => {
    for (const p of ['/practice', '/games/word_search', '/games/levels/word_match', '/games/word_match/3', '/games/word_wheel/1', '/games/word_search/animals/2'])
      expect(musicMoodFor(p), p).toBe('game');
  });

  it('listening screens and first run are silent', () => {
    for (const p of ['/lesson/u1/0', '/stories', '/story/3', '/review', '/onboarding']) expect(musicMoodFor(p), p).toBe('none');
    expect(musicMoodFor(null)).toBe('none');
  });
});
