/**
 * App-specific composites shared by more than one screen: the story cover, the streak quota and
 * week strip, the audio button, a word row.
 */
import { useState } from 'preact/hooks';
import { plural } from '../domain/plural';
import type { Word } from '../domain/types';
import { hasAudio, playWord } from '../lib/audio';
import { navigate } from '../lib/router';
import type { DayMark } from './derive';
import { Icon, sceneForIndex, sceneUrl } from './kit';

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
          <span class="t-label-s muted">{plural(totalPages, 'page')}</span>
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

export interface Quota {
  reviewCompleted: boolean;
  gamesPlayed: number;
  requiredGames: number;
  isMet: boolean;
}

/** The day's streak quota: the review deck plus three mini-game levels. */
export function QuotaList({ quota }: { quota: Quota }) {
  const games = Math.min(quota.gamesPlayed, quota.requiredGames);
  return (
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
  );
}

/** Seven days, today last: a flame on each practised day. */
export function WeekStrip({ week }: { week: DayMark[] }) {
  return (
    <div class="week" role="list">
      {week.map((d, i) => {
        const state = d.isToday ? (d.practised ? 'today-done' : 'today') : d.practised ? 'done' : 'missed';
        return (
          <div key={i} class={`day ${state}`} role="listitem" aria-label={`${d.dayOfMonth}${d.practised ? ', practised' : d.isToday ? ', today, not yet practised' : ', missed'}`}>
            <span class="t-label-s muted">{d.label}</span>
            <span class="dot">{d.practised ? <Icon name="flash" size={16} /> : d.dayOfMonth}</span>
          </div>
        );
      })}
    </div>
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
