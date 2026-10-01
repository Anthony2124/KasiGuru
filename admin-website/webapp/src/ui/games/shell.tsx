/**
 * The frame every mini-game shares: a Casiguran scene behind the round, a header with progress and
 * score, a quit guard, and the results screen (GameShellComponents / GameOverView on Android).
 */
import type { ComponentChildren } from 'preact';
import { useState } from 'preact/hooks';
import { back, navigate } from '../../lib/router';
import { ClayButton, ConfirmDialog, Confetti, CountUp, Icon, Jepjep, ProgressBar, sceneUrl, type SceneId } from '../kit';

export function GameFrame({
  title, scene, progress, label, score, active, children, footer,
}: { title: string; scene: SceneId; progress?: number; label?: string; score?: number | string; active: boolean; children: ComponentChildren; footer?: ComponentChildren }) {
  const [confirm, setConfirm] = useState(false);
  const exit = () => (active ? setConfirm(true) : back('/practice'));
  return (
    <div class="game">
      <img class="game-scene" src={sceneUrl(scene)} alt="" />
      <div class="game-shade" />
      {confirm && (
        <ConfirmDialog
          title="Quit this game?"
          message="Your progress in this round won't be saved."
          confirmLabel="Quit"
          danger
          onCancel={() => setConfirm(false)}
          onConfirm={() => {
            setConfirm(false);
            back('/practice');
          }}
        />
      )}
      <header class="game-head">
        <button class="icon-btn boxed" aria-label="Leave the game" onClick={exit}>
          <Icon name="arrowLeft" size={22} />
        </button>
        <h1 class="t-title-l pill-over">{title}</h1>
        <span class="spacer" />
        {score != null && <span class="chip" style={{ background: 'var(--lime)', color: 'var(--on-lime)', border: 0 }}>{score}</span>}
      </header>
      {progress != null && (
        <div class="game-progress">
          <ProgressBar value={progress} height={10} label={label} />
          {label && <p class="t-label pill-over" style={{ marginTop: 8, display: 'inline-block' }}>{label}</p>}
        </div>
      )}
      <div class="game-body">{children}</div>
      {footer && <div class="game-foot">{footer}</div>}
    </div>
  );
}

export interface ReviewItem {
  prompt: string;
  subPrompt?: string;
  userAnswer: string;
  correctAnswer: string;
  isCorrect: boolean;
}

export function GameOver({
  score, total, xp, stars, nextLevel, onNext, onReplay, review, headline,
}: { score: number; total: number; xp: number; stars: number; nextLevel: number | null; onNext: () => void; onReplay: () => void; review?: ReviewItem[]; headline?: string }) {
  const [showReview, setShowReview] = useState(false);
  return (
    <main class="page no-nav glow" style={{ '--gx': '50%', '--gy': '20%', minHeight: '100dvh', paddingTop: 'calc(var(--safe-top) + var(--s-lg))' } as never}>
      {stars === 3 && <Confetti />}
      <div class="readable stack center">
        <div style={{ display: 'grid', placeItems: 'center' }}>
          <Jepjep pose={stars === 3 ? 'shocked' : stars > 0 ? 'celebrating' : 'encouraging_approve'} height={150} breathe />
        </div>
        <div class="row" style={{ justifyContent: 'center', gap: 6 }} aria-label={`${stars} of 3 stars`}>
          {[0, 1, 2].map((i) => (
            <span key={i} class={i < stars ? 'pop' : ''} style={{ animationDelay: `${i * 120}ms` }}>
              <Icon name="star" size={i === 1 ? 52 : 40} color={i < stars ? 'var(--gold)' : 'var(--track)'} />
            </span>
          ))}
        </div>
        <h1 class="t-display">{headline ?? (stars > 0 ? 'Challenge complete!' : 'Keep practising')}</h1>
        <p class="t-title-l muted">Score: {score} / {total}</p>
        <span class="chip" style={{ alignSelf: 'center', margin: '0 auto', background: 'var(--gold)', color: 'var(--reward-ink)', border: 0 }}>
          <Icon name="star" size={16} /> +<CountUp to={xp} /> XP earned
        </span>
        {review && review.length > 0 && (
          <div style={{ textAlign: 'left' }}>
            <button class="text-btn lime" onClick={() => setShowReview(!showReview)} aria-expanded={showReview}>
              {showReview ? 'Hide results' : 'Review results'}
            </button>
            {showReview && (
              <div class="list">
                {review.map((r, i) => (
                  <div key={i} class="list-row" style={{ alignItems: 'flex-start' }}>
                    <Icon name={r.isCorrect ? 'tickCircle' : 'closeCircle'} size={20} color={r.isCorrect ? 'var(--green)' : 'var(--red)'} />
                    <div class="grow">
                      <p class="t-title-s">{i + 1}. {r.prompt}</p>
                      {r.subPrompt && <p class="t-body-s faint">{r.subPrompt}</p>}
                      <p class="t-body-s muted">Your answer: {r.userAnswer || '(No answer)'}</p>
                      {!r.isCorrect && <p class="t-body-s">Correct answer: {r.correctAnswer}</p>}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        <div class="stack-sm">
          {nextLevel != null && stars > 0 && <ClayButton label="Next level" onClick={onNext} />}
          <ClayButton label={stars > 0 ? 'Play again' : 'Try again'} tone={nextLevel != null && stars > 0 ? 'quiet' : 'primary'} onClick={onReplay} />
          <ClayButton label="Return to games" tone="quiet" onClick={() => navigate('/practice', { replace: true })} />
        </div>
      </div>
    </main>
  );
}

export function GameUnavailable({ message }: { message: string }) {
  return (
    <main class="page no-nav center stack" style={{ paddingTop: 'calc(var(--safe-top) + 80px)' }}>
      <div style={{ display: 'grid', placeItems: 'center' }}>
        <Jepjep pose="confused" height={140} />
      </div>
      <h1 class="t-headline-s">Not enough words yet</h1>
      <p class="t-body-l muted">{message}</p>
      <div class="readable">
        <ClayButton label="Back to games" onClick={() => back('/practice')} />
      </div>
    </main>
  );
}

/** "Show a hint": costs the speed bonus, never the answer. Renders nothing without a definition. */
export function HintButton({ hint, revealed, onReveal }: { hint: string | null; revealed: boolean; onReveal: () => void }) {
  if (!hint) return null;
  return revealed ? (
    <div class="card" style={{ background: 'var(--sunken)' }}>
      <p class="t-label muted">Hint</p>
      <p class="t-body-l" style={{ marginTop: 4, whiteSpace: 'pre-line' }}>{hint}</p>
    </div>
  ) : (
    <button class="text-btn muted pill-over row-xs" onClick={onReveal} style={{ fontFamily: 'var(--body)', fontWeight: 700, fontSize: 14 }}>
      <Icon name="infoCircle" size={18} /> Show a hint
    </button>
  );
}
