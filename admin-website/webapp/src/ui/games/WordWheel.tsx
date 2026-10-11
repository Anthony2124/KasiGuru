/**
 * Word Wheel (WordWheelViewModel): spell dictionary words from one wheel of letters to fill a small
 * crossword. Swipe across the letters, or tap them and press Check. Three hints per level, each a
 * clue for one hidden word: its length and meaning. The board is saved as it is played and picked
 * up again when the level is reopened (lib/savedGames.ts).
 */
import { useEffect, useMemo, useRef, useState } from 'preact/hooks';
import { GAMES } from '../../domain/constants';
import { shuffled } from '../../domain/random';
import { resumableWordWheel } from '../../domain/savedGame';
import { cellKey, generateWordWheel, MAX_HINTS, MIN_WORD_LENGTH, slotCells, WORD_WHEEL_MAX_LEVEL, wordWheelTier } from '../../domain/wordWheel';
import { feedbackTone, playSfx } from '../../lib/audio';
import { navigate } from '../../lib/router';
import { clearSavedGame, loadSavedGame, saveGame } from '../../lib/savedGames';
import { act, getCorpus, useApp } from '../../lib/store';
import { ClayButton, Icon, Loading, sceneForIndex } from '../kit';
import { GameFrame, GameOver, GameUnavailable } from './shell';
import { nextClueSlot, wordWheelHint } from '../../domain/gameHints';
import { tagalogGloss } from './gloss';

type Feedback =
  | { kind: 'found' | 'bonus'; word: string; gloss: string }
  | { kind: 'already'; word: string }
  | { kind: 'notWord'; attempt: string }
  | { kind: 'tooShort' };

export function WordWheelGame({ level: rawLevel }: { level: number }) {
  const ready = useApp((s) => s.contentReady);
  const level = Math.min(Math.max(rawLevel, 1), WORD_WHEEL_MAX_LEVEL);
  const tier = wordWheelTier(level);
  const [round, setRound] = useState(0);
  const puzzle = useMemo(() => (ready ? generateWordWheel(getCorpus().all, level) : undefined), [ready, level, round]);
  const [order, setOrder] = useState<number[]>([]);
  const [selection, setSelection] = useState<number[]>([]);
  const [foundSlots, setFoundSlots] = useState<number[]>([]);
  const [bonus, setBonus] = useState<string[]>([]);
  const [hintsUsed, setHintsUsed] = useState(0);
  // The board slot whose clue is showing, until that word is found.
  const [cluedSlot, setCluedSlot] = useState<number | null>(null);
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [result, setResult] = useState<{ xp: number; stars: number } | null>(null);
  const wheelRef = useRef<HTMLDivElement>(null);
  const dragging = useRef(false);
  const dragged = useRef(false);
  const selRef = useRef<number[]>([]);
  selRef.current = selection;

  useEffect(() => {
    if (!puzzle) return;
    // The board is rebuilt from the dictionary; a saved one carries on only if it still matches.
    const saved = loadSavedGame(GAMES.WORD_WHEEL, level);
    const resumed = resumableWordWheel(
      saved?.kind === 'word_wheel' ? saved : null,
      puzzle.slots.map((s) => s.word.key),
      puzzle.bonusWords.map((w) => w.key),
      MAX_HINTS
    );
    setOrder(puzzle.wheel.map((_, i) => i));
    setSelection([]);
    setFoundSlots(resumed ? [...resumed.foundSlots] : []);
    setBonus(resumed ? [...resumed.bonusWords] : []);
    setHintsUsed(resumed?.hintsUsed ?? 0);
    setCluedSlot(resumed?.cluedSlot ?? null);
    setFeedback(null);
    setResult(null);
  }, [puzzle]);

  if (!ready || puzzle === undefined) return <Loading label="Spinning the wheel" />;
  if (puzzle === null) return <GameUnavailable message="The dictionary can't build this wheel yet. Try again after the next dictionary update." />;
  if (result) {
    const next = level + 1 <= WORD_WHEEL_MAX_LEVEL ? level + 1 : null;
    return (
      <GameOver
        score={puzzle.slots.length}
        total={puzzle.slots.length}
        xp={result.xp}
        stars={result.stars}
        nextLevel={next}
        headline={result.stars === 3 ? 'Solved with no hints!' : 'Board solved!'}
        onNext={() => navigate(`/games/word_wheel/${next}`, { replace: true })}
        onReplay={() => setRound((r) => r + 1)}
      />
    );
  }

  const entries = new Map(getCorpus().all.map((w) => [w.id, w]));
  const glossFor = (id: string) => {
    const e = entries.get(id);
    return e ? tagalogGloss(e) : '';
  };
  const cellsOf = (slots: number[]) => slots.flatMap((i) => slotCells(puzzle.slots[i]).map(([r, c]) => cellKey(r, c)));
  const shown = new Set<string>(cellsOf(foundSlots));
  const attempt = selection.map((i) => puzzle.wheel[i]).join('');

  // A hint's clue: one hidden word's length and English meaning, never the word itself. English
  // only, since the Tagalog is often spelled close to the Kasiguranin it would give away.
  const clueSlots = puzzle.slots.map((s): [number, string | undefined] => [s.word.letters.length, entries.get(s.word.id)?.meaningEnglish]);
  const clue = cluedSlot === null ? null : wordWheelHint([clueSlots[cluedSlot]]);
  const nextClue = nextClueSlot(clueSlots, foundSlots);
  const hintsLeft = Math.max(MAX_HINTS - hintsUsed, 0);
  // The button shows the current clue again, or gives the next one.
  const canHint = cluedSlot !== null || (hintsLeft > 0 && nextClue !== null);

  /** A word whose every letter shows through crossings counts as found. */
  const completeFilled = (fs: number[]) => {
    const s = new Set<string>(cellsOf(fs));
    const done = puzzle.slots.map((_, i) => i).filter((i) => !fs.includes(i) && slotCells(puzzle.slots[i]).every(([r, c]) => s.has(cellKey(r, c))));
    return done.length ? [...fs, ...done] : fs;
  };

  /** Keeps the board so far, so that leaving the game does not lose it. */
  const saveProgress = (fs: number[], bonusKeys: string[], hints: number, clued: number | null) =>
    saveGame(GAMES.WORD_WHEEL, level, {
      kind: 'word_wheel',
      boardWords: puzzle.slots.map((s) => s.word.key),
      foundSlots: fs,
      bonusWords: bonusKeys,
      hintsUsed: hints,
      cluedSlot: clued,
    });

  /** True when the board is complete; the reward follows. */
  const maybeFinish = (fs: number[], hints: number) => {
    if (fs.length < puzzle.slots.length) return false;
    // Before the reward, so the board can never be picked up and rewarded again.
    clearSavedGame(GAMES.WORD_WHEEL, level);
    const board = puzzle.slots.length;
    const stars = hints === 0 ? 3 : hints <= 2 ? 2 : 1;
    // Bonus words earn nothing extra: XP comes from completing the board (XP policy 2).
    const xp = act((d) => (d.met(puzzle.slots.map((s) => s.word.id), GAMES.WORD_WHEEL), d).finishGame({ mode: GAMES.WORD_WHEEL, level, correct: board, total: board, statsTotal: board + hints, stars, perfect: hints === 0 }));
    setTimeout(() => setResult({ xp, stars }), 700);
    return true;
  };

  const submit = (sel = selection) => {
    if (!sel.length) return;
    const letters = sel.map((i) => puzzle.wheel[i]);
    setSelection([]);
    if (letters.length < MIN_WORD_LENGTH) {
      setFeedback({ kind: 'tooShort' });
      return;
    }
    const key = letters.join('');
    const slot = puzzle.slots.findIndex((s) => s.word.key === key);
    const b = puzzle.bonusWords.find((w) => w.key === key);
    if (slot >= 0 && foundSlots.includes(slot)) setFeedback({ kind: 'already', word: puzzle.slots[slot].word.word });
    else if (slot >= 0) {
      const w = puzzle.slots[slot].word;
      const fs = completeFilled([...foundSlots, slot]);
      // A clue whose word is now found is done with, which frees the hint button for the next one.
      const clued = cluedSlot !== null && fs.includes(cluedSlot) ? null : cluedSlot;
      setFoundSlots(fs);
      setCluedSlot(clued);
      setFeedback({ kind: 'found', word: w.word, gloss: glossFor(w.id) });
      playSfx('found');
      if (!maybeFinish(fs, hintsUsed)) saveProgress(fs, bonus, hintsUsed, clued);
    } else if (b && bonus.includes(b.key)) setFeedback({ kind: 'already', word: b.word });
    else if (b) {
      const bonusKeys = [...bonus, b.key];
      setBonus(bonusKeys);
      setFeedback({ kind: 'bonus', word: b.word, gloss: glossFor(b.id) });
      playSfx('found');
      saveProgress(foundSlots, bonusKeys, hintsUsed, cluedSlot);
    } else {
      setFeedback({ kind: 'notWord', attempt: key });
      feedbackTone(false);
    }
  };

  /**
   * A clue for one hidden word, its length and meaning. The clue stays under the board until that
   * word is found; only then does the next hint give another, and asking before that just shows the
   * current clue again. At most MAX_HINTS per level, and each lowers the stars the level can earn.
   */
  const hint = () => {
    if (cluedSlot !== null) {
      setFeedback(null);
      return;
    }
    if (hintsLeft <= 0 || nextClue === null) return;
    const used = hintsUsed + 1;
    setCluedSlot(nextClue);
    setHintsUsed(used);
    setFeedback(null);
    saveProgress(foundSlots, bonus, used, nextClue);
  };

  const tapLetter = (i: number) => {
    setFeedback(null);
    setSelection((sel) => (sel[sel.length - 1] === i ? sel.slice(0, -1) : sel.includes(i) ? sel : [...sel, i]));
  };

  const letterAtPoint = (x: number, y: number): number | null => {
    const el = document.elementFromPoint(x, y) as HTMLElement | null;
    const idx = el?.closest('[data-letter]')?.getAttribute('data-letter');
    return idx == null ? null : Number(idx);
  };
  const onDown = (e: PointerEvent) => {
    const i = letterAtPoint(e.clientX, e.clientY);
    if (i == null) return;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    dragging.current = true;
    dragged.current = false;
    tapLetter(i);
  };
  const onMove = (e: PointerEvent) => {
    if (!dragging.current) return;
    const i = letterAtPoint(e.clientX, e.clientY);
    if (i == null) return;
    setSelection((sel) => {
      if (sel[sel.length - 1] === i) return sel;
      dragged.current = true;
      if (sel.length >= 2 && sel[sel.length - 2] === i) return sel.slice(0, -1);
      return sel.includes(i) ? sel : [...sel, i];
    });
  };
  const onUp = () => {
    if (!dragging.current) return;
    dragging.current = false;
    // A swipe submits on release; a plain tap just adds the letter and waits for Check.
    if (dragged.current) submit(selRef.current);
  };

  // What the last move did, and otherwise the hint's clue, which stays until its word is found.
  const text = (() => {
    if (!feedback) return { t: clue ? `Hint: ${clue}` : 'Swipe across the letters, or tap them and press Check.', bad: false };
    switch (feedback.kind) {
      case 'found':
        return { t: feedback.gloss ? `Found ${feedback.word}: ${feedback.gloss}` : `Found ${feedback.word}!`, bad: false };
      case 'bonus':
        return { t: feedback.gloss ? `Bonus word ${feedback.word}: ${feedback.gloss}` : `Bonus word ${feedback.word}!`, bad: false };
      case 'already':
        return { t: `You already found ${feedback.word}.`, bad: false };
      case 'notWord':
        return { t: `${feedback.attempt.toLowerCase()} isn't in the dictionary.`, bad: true };
      case 'tooShort':
        return { t: 'Words need at least 3 letters.', bad: true };
    }
  })();

  const n = order.length;
  const radius = 38;

  return (
    <GameFrame
      title="Word Wheel"
      scene={sceneForIndex(level)}
      progress={foundSlots.length / puzzle.slots.length}
      label={`Found ${foundSlots.length} of ${puzzle.slots.length} · Level ${level} · ${tier.name}`}
      score={`Bonus ${bonus.length}`}
      active={foundSlots.length > 0 || hintsUsed > 0 || bonus.length > 0}
    >
      <div class="ww-board" style={{ gridTemplateColumns: `repeat(${puzzle.cols}, 1fr)`, maxWidth: Math.min(puzzle.cols * 44, 440) }} aria-label={`Crossword, ${foundSlots.length} of ${puzzle.slots.length} words found`} role="img">
        {Array.from({ length: puzzle.rows }, (_, r) =>
          Array.from({ length: puzzle.cols }, (_, c) => {
            const k = cellKey(r, c);
            const letter = puzzle.letterAt.get(k);
            if (!letter) return <span key={k} />;
            const vis = shown.has(k);
            return (
              <span key={k} class={`ww-cell${vis ? ' shown' : ''}`}>
                {vis ? letter : ''}
              </span>
            );
          })
        )}
      </div>

      {/* Always two lines tall, so a long clue or message never shrinks the board. */}
      <div class="ww-status">
        <p class={`t-body pill-over center${text.bad ? ' shake' : ''}`} style={{ color: text.bad ? 'var(--red)' : 'var(--ink)' }} role="status" aria-live="polite" key={text.t}>
          {text.t}
        </p>
      </div>

      <div class="ww-attempt" aria-label={attempt ? `Spelling ${attempt}` : 'No letters picked'}>{attempt || ' '}</div>

      <div class="ww-controls">
        <button class="icon-btn boxed" aria-label="Shuffle letters" onClick={() => {
          if (order.length < 2) return;
          let o = shuffled(order);
          while (o.every((v, i) => v === order[i])) o = shuffled(order);
          setOrder(o);
          setSelection([]);
        }}>
          <Icon name="refresh" size={22} />
        </button>

        <div
          ref={wheelRef}
          class="ww-wheel"
          onPointerDown={onDown}
          onPointerMove={onMove}
          onPointerUp={onUp}
          onPointerCancel={() => (dragging.current = false)}
        >
          <svg class="ww-lines" viewBox="0 0 100 100" aria-hidden="true">
            {selection.length > 1 && (
              <polyline
                points={selection
                  .map((i) => {
                    const pos = order.indexOf(i);
                    const a = (pos / n) * 2 * Math.PI - Math.PI / 2;
                    return `${50 + radius * Math.cos(a)},${50 + radius * Math.sin(a)}`;
                  })
                  .join(' ')}
                fill="none"
                stroke="var(--lime-fill)"
                stroke-width="3"
                stroke-linecap="round"
                stroke-linejoin="round"
                opacity="0.8"
              />
            )}
          </svg>
          {order.map((letterIndex, pos) => {
            const a = (pos / n) * 2 * Math.PI - Math.PI / 2;
            const picked = selection.includes(letterIndex);
            return (
              <button
                key={letterIndex}
                data-letter={letterIndex}
                class={`ww-letter${picked ? ' picked' : ''}`}
                style={{ left: `${50 + radius * Math.cos(a)}%`, top: `${50 + radius * Math.sin(a)}%` }}
                aria-label={`Letter ${puzzle.wheel[letterIndex]}`}
                onClick={(e) => {
                  // Pointer handlers already added it; a keyboard press lands here alone.
                  if ((e as MouseEvent).detail === 0) tapLetter(letterIndex);
                }}
              >
                {puzzle.wheel[letterIndex]}
              </button>
            );
          })}
        </div>

        <button
          class="icon-btn boxed"
          aria-label={
            cluedSlot !== null
              ? 'Show the clue again'
              : canHint
                ? `Hint: a clue for a hidden word, ${hintsLeft} left`
                : hintsLeft > 0
                  ? 'No clues for the words left'
                  : 'No hints left'
          }
          disabled={!canHint}
          onClick={hint}
          style={{ position: 'relative', opacity: canHint ? 1 : 0.5 }}
        >
          <Icon name="lampOn" size={22} color="var(--gold)" />
          <span aria-hidden="true" style={{ position: 'absolute', top: -4, right: -4, minWidth: 20, height: 20, borderRadius: 10, background: 'var(--gold-fill)', color: 'var(--reward-ink)', fontSize: 12, fontWeight: 700, display: 'grid', placeItems: 'center' }}>{hintsLeft}</span>
        </button>
      </div>

      <div class="row">
        <div class="grow">
          <ClayButton label="Clear letters" tone="quiet" onClick={() => setSelection([])} disabled={!selection.length} small />
        </div>
        <div class="grow">
          <ClayButton label="Check" onClick={() => submit()} disabled={!selection.length} small />
        </div>
      </div>
    </GameFrame>
  );
}
