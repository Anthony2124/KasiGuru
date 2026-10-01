/**
 * Word Wheel (WordWheelViewModel): spell dictionary words from one wheel of letters to fill a small
 * crossword. Swipe across the letters, or tap them and press Check. Three hints per level.
 */
import { useEffect, useMemo, useRef, useState } from 'preact/hooks';
import { GAMES, XP_PER_GAME_CORRECT } from '../../domain/constants';
import { shuffled } from '../../domain/random';
import { cellKey, generateWordWheel, MAX_HINTS, MIN_WORD_LENGTH, slotCells, WORD_WHEEL_MAX_LEVEL, wordWheelTier } from '../../domain/wordWheel';
import { feedbackTone } from '../../lib/audio';
import { navigate } from '../../lib/router';
import { act, getCorpus, useApp } from '../../lib/store';
import { ClayButton, Icon, Loading, sceneForIndex } from '../kit';
import { GameFrame, GameOver, GameUnavailable } from './shell';
import { tagalogGloss } from './gloss';

type Feedback =
  | { kind: 'found' | 'bonus'; word: string; gloss: string }
  | { kind: 'already'; word: string }
  | { kind: 'notWord'; attempt: string }
  | { kind: 'tooShort' }
  | { kind: 'revealed'; left: number };

export function WordWheelGame({ level: rawLevel }: { level: number }) {
  const ready = useApp((s) => s.contentReady);
  const soundOn = useApp((s) => s.prefs.soundEnabled);
  const level = Math.min(Math.max(rawLevel, 1), WORD_WHEEL_MAX_LEVEL);
  const tier = wordWheelTier(level);
  const [round, setRound] = useState(0);
  const puzzle = useMemo(() => (ready ? generateWordWheel(getCorpus().all, level) : undefined), [ready, level, round]);
  const [order, setOrder] = useState<number[]>([]);
  const [selection, setSelection] = useState<number[]>([]);
  const [foundSlots, setFoundSlots] = useState<number[]>([]);
  const [revealed, setRevealed] = useState<string[]>([]);
  const [bonus, setBonus] = useState<string[]>([]);
  const [hintsUsed, setHintsUsed] = useState(0);
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [result, setResult] = useState<{ xp: number; stars: number } | null>(null);
  const wheelRef = useRef<HTMLDivElement>(null);
  const dragging = useRef(false);
  const dragged = useRef(false);
  const selRef = useRef<number[]>([]);
  selRef.current = selection;

  useEffect(() => {
    if (!puzzle) return;
    setOrder(puzzle.wheel.map((_, i) => i));
    setSelection([]);
    setFoundSlots([]);
    setRevealed([]);
    setBonus([]);
    setHintsUsed(0);
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
  const shown = new Set<string>([...foundSlots.flatMap((i) => slotCells(puzzle.slots[i]).map(([r, c]) => cellKey(r, c))), ...revealed]);
  const attempt = selection.map((i) => puzzle.wheel[i]).join('');

  const completeFilled = (fs: number[], rev: string[]) => {
    const s = new Set<string>([...fs.flatMap((i) => slotCells(puzzle.slots[i]).map(([r, c]) => cellKey(r, c))), ...rev]);
    const done = puzzle.slots.map((_, i) => i).filter((i) => !fs.includes(i) && slotCells(puzzle.slots[i]).every(([r, c]) => s.has(cellKey(r, c))));
    return done.length ? [...fs, ...done] : fs;
  };

  const maybeFinish = (fs: number[], hints: number, bonusCount: number) => {
    if (fs.length < puzzle.slots.length) return;
    const board = puzzle.slots.length;
    const stars = hints === 0 ? 3 : hints <= 2 ? 2 : 1;
    const xp = board * (XP_PER_GAME_CORRECT / 2) + bonusCount * (XP_PER_GAME_CORRECT / 4);
    act((d) => d.finishGame({ gameType: GAMES.WORD_WHEEL, level, score: board, total: board, statsTotal: board + hints, xp, stars, perfect: hints === 0 }));
    setTimeout(() => setResult({ xp, stars }), 700);
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
      const fs = completeFilled([...foundSlots, slot], revealed);
      setFoundSlots(fs);
      setFeedback({ kind: 'found', word: w.word, gloss: glossFor(w.id) });
      if (soundOn) feedbackTone(true);
      maybeFinish(fs, hintsUsed, bonus.length);
    } else if (b && bonus.includes(b.key)) setFeedback({ kind: 'already', word: b.word });
    else if (b) {
      setBonus([...bonus, b.key]);
      setFeedback({ kind: 'bonus', word: b.word, gloss: glossFor(b.id) });
      if (soundOn) feedbackTone(true);
    } else {
      setFeedback({ kind: 'notWord', attempt: key });
      if (soundOn) feedbackTone(false);
    }
  };

  const hint = () => {
    if (hintsUsed >= MAX_HINTS) return;
    const cell = puzzle.slots
      .map((s, i) => ({ s, i }))
      .filter(({ i }) => !foundSlots.includes(i))
      .sort((a, b) => a.s.word.letters.length - b.s.word.letters.length)
      .map(({ s }) => slotCells(s).map(([r, c]) => cellKey(r, c)).find((k) => !shown.has(k)))
      .find(Boolean);
    if (!cell) return;
    const rev = [...revealed, cell];
    const used = hintsUsed + 1;
    const fs = completeFilled(foundSlots, rev);
    setRevealed(rev);
    setHintsUsed(used);
    setSelection([]);
    setFoundSlots(fs);
    setFeedback({ kind: 'revealed', left: MAX_HINTS - used });
    maybeFinish(fs, used, bonus.length);
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

  const text = (() => {
    if (!feedback) return { t: 'Swipe across the letters, or tap them and press Check.', bad: false };
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
      case 'revealed':
        return { t: feedback.left === 0 ? 'Hint: one letter uncovered. That was your last hint.' : `Hint: one letter uncovered. ${feedback.left} left.`, bad: false };
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
              <span key={k} class={`ww-cell${vis ? ' shown' : ''}${revealed.includes(k) && !foundSlots.some((i) => slotCells(puzzle.slots[i]).some(([rr, cc]) => cellKey(rr, cc) === k)) ? ' hinted' : ''}`}>
                {vis ? letter : ''}
              </span>
            );
          })
        )}
      </div>

      <p class={`t-body pill-over center${text.bad ? ' shake' : ''}`} style={{ alignSelf: 'center', color: text.bad ? 'var(--red)' : 'var(--ink)', maxWidth: '100%' }} role="status" aria-live="polite" key={text.t}>
        {text.t}
      </p>

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
                stroke="var(--lime)"
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

        <button class="icon-btn boxed" aria-label={MAX_HINTS - hintsUsed > 0 ? `Hint: uncover a letter, ${MAX_HINTS - hintsUsed} left` : 'No hints left'} disabled={hintsUsed >= MAX_HINTS} onClick={hint} style={{ position: 'relative', opacity: hintsUsed >= MAX_HINTS ? 0.5 : 1 }}>
          <Icon name="lampOn" size={22} color="var(--gold)" />
          <span style={{ position: 'absolute', top: -4, right: -4, minWidth: 20, height: 20, borderRadius: 10, background: 'var(--gold)', color: 'var(--reward-ink)', fontSize: 12, fontWeight: 700, display: 'grid', placeItems: 'center' }}>{MAX_HINTS - hintsUsed}</span>
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
