/**
 * Word Search (WordSearchViewModel): hidden words from one category. Drag across a word, or tap its
 * first letter and then its last. No SM-2 review for words found - spotting letters is not recalling
 * a meaning, and the review schedule is a thesis claim that must stay exact.
 */
import { useEffect, useMemo, useRef, useState } from 'preact/hooks';
import { categoryForWordSearchKey, GAMES } from '../../domain/constants';
import {
  generateWordSearch,
  lineBetween,
  matchLine,
  WORD_SEARCH_MAX_LEVEL,
  wordSearchSeed,
  wordSearchTier,
  type Cell,
} from '../../domain/wordSearch';
import { feedbackTone, playSfx, playWord } from '../../lib/audio';
import { navigate, enc } from '../../lib/router';
import { act, getCorpus, useApp } from '../../lib/store';
import { Icon, Loading, sceneForCategory } from '../kit';
import { GameFrame, GameOver, GameUnavailable } from './shell';
import { tagalogGloss } from './gloss';

const same = (a: Cell | null, b: Cell | null) => !!a && !!b && a[0] === b[0] && a[1] === b[1];

export function WordSearchGame({ levelKey, level: rawLevel }: { levelKey: string; level: number }) {
  const ready = useApp((s) => s.contentReady);
  const level = Math.min(Math.max(rawLevel, 1), WORD_SEARCH_MAX_LEVEL);
  const category = categoryForWordSearchKey(levelKey);
  const tier = wordSearchTier(level);
  const [round, setRound] = useState(0);
  const board = useMemo(() => {
    if (!ready || !category) return undefined;
    return generateWordSearch(getCorpus().inCategory(category), level, wordSearchSeed(levelKey, level));
  }, [ready, category, level, round]);
  const [found, setFound] = useState<string[]>([]);
  const [start, setStart] = useState<Cell | null>(null);
  const [drag, setDrag] = useState<{ from: Cell; to: Cell } | null>(null);
  const [missed, setMissed] = useState(false);
  const [misses, setMisses] = useState(0);
  const [result, setResult] = useState<{ xp: number; stars: number } | null>(null);
  const gridRef = useRef<HTMLDivElement>(null);
  const pointerDown = useRef<Cell | null>(null);

  useEffect(() => {
    setFound([]);
    setStart(null);
    setDrag(null);
    setMissed(false);
    setMisses(0);
    setResult(null);
  }, [board]);

  if (!ready || board === undefined) {
    if (!category) return <GameUnavailable message="That category could not be found." />;
    return <Loading label="Hiding the words" />;
  }
  if (board === null) {
    return <GameUnavailable message="This category needs more short words before this grid can be built. Try another category, or an earlier level." />;
  }
  if (result) {
    const next = level + 1 <= WORD_SEARCH_MAX_LEVEL ? level + 1 : null;
    return (
      <GameOver
        score={board.placements.length}
        total={board.placements.length}
        xp={result.xp}
        stars={result.stars}
        nextLevel={next}
        headline={result.stars === 3 ? 'Every word, no misses!' : 'All words found!'}
        onNext={() => navigate(`/games/word_search/${enc(levelKey)}/${next}`, { replace: true })}
        onReplay={() => setRound((r) => r + 1)}
      />
    );
  }

  const foundSet = new Set(found);
  const ownerOf = new Map<string, string>();
  for (const p of board.placements) if (foundSet.has(p.id)) for (const [r, c] of p.cells) ownerOf.set(`${r},${c}`, p.id);
  const preview = drag ? lineBetween(drag.from, drag.to) : null;
  const previewSet = new Set((preview ?? []).map(([r, c]) => `${r},${c}`));

  const judge = (from: Cell, to: Cell) => {
    const hit = matchLine(board, from, to, foundSet);
    if (!hit) {
      setMissed(true);
      setMisses((m) => m + 1);
      feedbackTone(false);
      return;
    }
    setMissed(false);
    playSfx('found');
    const next = [...found, hit.id];
    setFound(next);
    if (next.length === board.placements.length) finish(misses);
  };

  const finish = (missCount: number) => {
    const count = board.placements.length;
    const stars = missCount === 0 ? 3 : missCount <= 2 ? 2 : 1;
    const xp = act((d) =>
      d.finishGame({ mode: GAMES.WORD_SEARCH, levelKeyType: levelKey, level, correct: count, total: count, statsTotal: count + missCount, stars, perfect: missCount === 0 })
    );
    setResult({ xp, stars });
  };

  const cellAt = (x: number, y: number): Cell | null => {
    const el = gridRef.current;
    if (!el) return null;
    const rect = el.getBoundingClientRect();
    const col = Math.floor(((x - rect.left) / rect.width) * board.size);
    const row = Math.floor(((y - rect.top) / rect.height) * board.size);
    if (row < 0 || col < 0 || row >= board.size || col >= board.size) return null;
    return [row, col];
  };

  const onDown = (e: PointerEvent) => {
    const cell = cellAt(e.clientX, e.clientY);
    if (!cell) return;
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
    pointerDown.current = cell;
    setDrag({ from: cell, to: cell });
  };
  const onMove = (e: PointerEvent) => {
    if (!pointerDown.current) return;
    const cell = cellAt(e.clientX, e.clientY);
    if (!cell) return;
    // Snap to the nearest straight line so a slightly wobbly drag still reads as a word.
    const from = pointerDown.current;
    const dr = cell[0] - from[0];
    const dc = cell[1] - from[1];
    let to: Cell = cell;
    if (dr !== 0 && dc !== 0 && Math.abs(dr) !== Math.abs(dc)) {
      if (Math.abs(dr) > Math.abs(dc) * 2) to = [cell[0], from[1]];
      else if (Math.abs(dc) > Math.abs(dr) * 2) to = [from[0], cell[1]];
      else {
        const d = Math.min(Math.abs(dr), Math.abs(dc));
        to = [from[0] + Math.sign(dr) * d, from[1] + Math.sign(dc) * d];
      }
    }
    setDrag({ from, to });
  };
  const onUp = () => {
    const from = pointerDown.current;
    const d = drag;
    pointerDown.current = null;
    setDrag(null);
    if (!from || !d) return;
    if (!same(d.from, d.to)) {
      setStart(null);
      judge(d.from, d.to);
      return;
    }
    // A tap: first letter, then last.
    const cell = d.from;
    if (!start) {
      setStart(cell);
      setMissed(false);
    } else if (same(start, cell)) setStart(null);
    else if (!lineBetween(start, cell)) setStart(cell);
    else {
      setStart(null);
      judge(start, cell);
    }
  };

  const status = missed ? "That line isn't one of the words. Try again." : start ? "Now choose the word's last letter." : 'Drag across a word, from its first letter to its last.';
  const entries = new Map(getCorpus().inCategory(category!).map((w) => [w.id, w]));

  return (
    <GameFrame
      title={category!}
      scene={sceneForCategory(category!)}
      progress={found.length / board.placements.length}
      label={`Found ${found.length} of ${board.placements.length} · Level ${level} · ${tier.name}`}
      active={found.length > 0 || misses > 0}
    >
      <p class={`t-body pill-over${missed ? ' shake' : ''}`} style={{ alignSelf: 'flex-start', color: missed ? 'var(--red)' : 'var(--ink)' }} role="status" aria-live="polite">
        {status}
      </p>
      <div
        ref={gridRef}
        class="ws-grid"
        style={{ gridTemplateColumns: `repeat(${board.size}, 1fr)` }}
        onPointerDown={onDown}
        onPointerMove={onMove}
        onPointerUp={onUp}
        onPointerCancel={() => {
          pointerDown.current = null;
          setDrag(null);
        }}
      >
        {board.grid.map((row, r) =>
          row.map((letter, c) => {
            const k = `${r},${c}`;
            const isFound = ownerOf.has(k);
            const isStart = same(start, [r, c]);
            const inPreview = previewSet.has(k);
            return (
              <div key={k} class={`ws-cell${isFound ? ' found' : ''}${isStart ? ' start' : ''}${inPreview ? ' preview' : ''}`} aria-label={`Row ${r + 1}, column ${c + 1}, ${letter}`}>
                {letter}
              </div>
            );
          })
        )}
      </div>
      <div class="card">
        <p class="t-title-s" style={{ marginBottom: 8 }}>Find these words</p>
        <div class="ws-words">
          {board.placements.map((p) => {
            const isFound = foundSet.has(p.id);
            const entry = entries.get(p.id);
            const meaning = entry ? tagalogGloss(entry) : '';
            return (
              <div key={p.id} class={`ws-word${isFound ? ' found' : ''}`} aria-label={`${p.word}, ${isFound ? 'found' : 'not found yet'}`}>
                <Icon name={isFound ? 'tickCircle' : 'search'} size={16} color={isFound ? 'var(--lime)' : 'var(--faint)'} />
                <div class="grow">
                  <p class="t-title-s" style={{ textDecoration: isFound ? 'line-through' : 'none' }}>{p.word}</p>
                  {isFound && meaning && <p class="t-body-s muted">{meaning}</p>}
                </div>
                {isFound && entry?.audioFileName && (
                  <button class="icon-btn" style={{ width: 36, height: 36, color: 'var(--info)' }} aria-label={`Play ${p.word}`} onClick={() => void playWord(entry)}>
                    <Icon name="volumeHigh" size={18} />
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </GameFrame>
  );
}
