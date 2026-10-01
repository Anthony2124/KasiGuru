/**
 * App-specific composites shared by more than one screen: the story cover, the streak dialog, the
 * audio button, a word row.
 */
import { useState } from 'preact/hooks';
import type { Word } from '../domain/types';
import { hasAudio, playWord } from '../lib/audio';
import { navigate } from '../lib/router';
import { ClayButton, Dialog, Icon, sceneForIndex, sceneUrl } from './kit';

export function StoryCover({
  id, title, titleKasiguranin, totalPages, unlocked, completed, requiredXp, width, onClick,
}: { id: number; title: string; titleKasiguranin: string; totalPages: number; unlocked: boolean; completed: boolean; requiredXp: number; width?: number | string; onClick: () => void }) {
  const lead = titleKasiguranin || title;
  return (
    <button
      class="card flat"
      onClick={onClick}
      style={{ width, flex: 'none', textAlign: 'left' }}
      aria-label={`${lead}${unlocked ? '' : `, locked, needs ${requiredXp} XP`}${completed ? ', read' : ''}`}
    >
      <div class={`scene${unlocked ? '' : ' locked'}`} style={{ aspectRatio: '3 / 2', borderRadius: 0 }}>
        <img src={sceneUrl(sceneForIndex(id))} alt="" loading="lazy" />
        <div class="over" style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: 10 }}>
          <div class="row-xs" style={{ justifyContent: 'flex-end' }}>
            {!unlocked && (
              <span class="tag neutral" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                <Icon name="lock" size={12} /> {requiredXp} XP
              </span>
            )}
            {completed && (
              <span class="tag" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                <Icon name="tickCircle" size={12} /> Read
              </span>
            )}
          </div>
          <span class="t-label-s muted">{totalPages} pages</span>
        </div>
      </div>
      <div style={{ padding: '10px 12px 12px' }}>
        <p class="t-title-s" style={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden', color: unlocked ? 'var(--ink)' : 'var(--muted)' }}>
          {lead}
        </p>
        {titleKasiguranin && title && <p class="t-body-s faint" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{title}</p>}
      </div>
    </button>
  );
}

export function StreakDialog({ current, longest, quota, onClose }: { current: number; longest: number; quota: { reviewCompleted: boolean; gamesPlayed: number; requiredGames: number; isMet: boolean }; onClose: () => void }) {
  const games = Math.min(quota.gamesPlayed, quota.requiredGames);
  return (
    <Dialog label="Your streak" onClose={onClose} glow>
      <div class="stack">
        <div class="row">
          <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--coral)', display: 'grid', placeItems: 'center', flex: 'none' }}>
            <Icon name="flash" size={36} color="var(--reward-ink)" />
          </div>
          <div class="grow">
            <h2 class="t-headline">{current} day streak</h2>
            <p class="t-body muted">Longest streak: {longest} {longest === 1 ? 'day' : 'days'}</p>
          </div>
        </div>
        <p class="t-body-l">
          {quota.isMet ? 'Your streak is active for today! Keep up the momentum!' : "Complete your daily goals below to activate today's streak!"}
        </p>
        <div class="list">
          <div class="list-row">
            <span class="ico">
              <Icon name={quota.reviewCompleted ? 'tickCircle' : 'repeat'} size={20} color={quota.reviewCompleted ? 'var(--lime)' : 'var(--info)'} />
            </span>
            <div class="grow">
              <p class="t-title-s">Complete review words</p>
              <p class="t-body-s muted">{quota.reviewCompleted ? 'Daily review completed' : 'Finish the review deck'}</p>
            </div>
            <span class="tag" style={quota.reviewCompleted ? undefined : { background: 'var(--sunken)', color: 'var(--muted)' }}>
              {quota.reviewCompleted ? 'Done' : 'Pending'}
            </span>
          </div>
          <div class="list-row">
            <span class="ico">
              <Icon name={games >= quota.requiredGames ? 'tickCircle' : 'game'} size={20} color={games >= quota.requiredGames ? 'var(--lime)' : 'var(--gold)'} />
            </span>
            <div class="grow">
              <p class="t-title-s">Play 3 mini-game levels</p>
              <p class="t-body-s muted">{games >= quota.requiredGames ? '3 of 3 levels played' : `${games} of ${quota.requiredGames} played today`}</p>
            </div>
            <span class="tag" style={games >= quota.requiredGames ? undefined : { background: 'var(--sunken)', color: 'var(--muted)' }}>
              {games}/{quota.requiredGames}
            </span>
          </div>
        </div>
        <ClayButton label="Keep it up!" onClick={onClose} />
      </div>
    </Dialog>
  );
}

/** Plays a word's recording; hidden entirely for a word that has none. */
export function AudioButton({ word, size = 44, label }: { word: Word; size?: number; label?: string }) {
  const [state, setState] = useState<'idle' | 'loading' | 'failed'>('idle');
  if (!hasAudio(word)) return null;
  return (
    <button
      class="icon-btn boxed"
      style={{ width: size, height: size, color: state === 'failed' ? 'var(--faint)' : 'var(--info)' }}
      aria-label={label ?? `Play ${word.kasiguranin}`}
      onClick={async (e) => {
        e.stopPropagation();
        setState('loading');
        const ok = await playWord(word);
        setState(ok ? 'idle' : 'failed');
      }}
    >
      {state === 'loading' ? <span class="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} /> : <Icon name={state === 'failed' ? 'volumeSlash' : 'volumeHigh'} size={Math.round(size * 0.5)} />}
    </button>
  );
}

export function WordRow({ word, onClick }: { word: Word; onClick?: () => void }) {
  return (
    <button class="list-row" onClick={onClick ?? (() => navigate(`/word/${encodeURIComponent(word.id)}`))}>
      <div class="grow">
        <p class="t-title" style={{ overflowWrap: 'anywhere' }}>{word.kasiguranin}</p>
        <p class="t-body muted" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {[word.tagalog, word.english].filter(Boolean).join(' · ')}
        </p>
      </div>
      {word.isLearned && <Icon name="tickCircle" size={20} color="var(--lime)" label="Learned" />}
      <Icon name="arrowRight" size={18} color="var(--faint)" />
    </button>
  );
}
