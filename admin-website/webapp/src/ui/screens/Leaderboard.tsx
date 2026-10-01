/**
 * Leaderboard (ui/screens/leaderboard): signed-in learners ranked by all-time XP, this week's XP, or
 * streak. Guests are not ranked, on either app.
 */
import { useEffect, useState } from 'preact/hooks';
import { fetchLeaderboard, type LeaderboardEntry, type LeaderboardOrder } from '../../lib/remote';
import { navigate } from '../../lib/router';
import { act, useApp } from '../../lib/store';
import { Avatar, EmptyState, GroundScaffold, Icon, Loading } from '../kit';

export function LeaderRow({ rank, entry, mode }: { rank: number; entry: LeaderboardEntry; mode: LeaderboardOrder }) {
  const byStreak = mode === 'currentStreak';
  const figure = byStreak ? `${entry.currentStreak}d` : mode === 'weeklyXp' ? `${entry.weeklyXp}` : `${entry.totalXp}`;
  const medal = rank === 1 ? 'var(--tier-gold)' : rank === 2 ? 'var(--tier-silver)' : rank === 3 ? 'var(--tier-bronze)' : null;
  return (
    <div
      class="list-row"
      style={entry.isCurrentUser ? { background: 'var(--lime-tint)' } : undefined}
      aria-label={`Rank ${rank}, ${entry.name}${entry.isCurrentUser ? ', you' : ''}, ${byStreak ? `${entry.currentStreak} day streak` : `${figure} XP`}`}
    >
      <span style={{ width: 28, height: 28, borderRadius: '50%', display: 'grid', placeItems: 'center', background: medal ?? 'transparent', color: medal ? 'var(--reward-ink)' : 'var(--muted)', fontFamily: 'var(--display)', fontWeight: 700, fontSize: 14, flex: 'none' }}>
        {rank}
      </span>
      <Avatar id={entry.avatarIconId} size={40} />
      <div class="grow">
        <p class="t-title-s" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{entry.isCurrentUser ? `${entry.name} (you)` : entry.name}</p>
        <p class="t-body-s muted" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{entry.levelTitle}</p>
      </div>
      <span class="row-xs t-label">
        <Icon name={byStreak ? 'flash' : 'star'} size={16} color={byStreak ? 'var(--coral)' : 'var(--gold)'} />
        {figure}
      </span>
    </div>
  );
}

const FILTERS: { key: LeaderboardOrder; label: string }[] = [
  { key: 'totalXp', label: 'All-time' },
  { key: 'weeklyXp', label: 'This week' },
  { key: 'currentStreak', label: 'Streaks' },
];

export function LeaderboardScreen() {
  const [mode, setMode] = useState<LeaderboardOrder>('totalXp');
  const [rows, setRows] = useState<LeaderboardEntry[] | null>(null);
  const [error, setError] = useState(false);
  const account = useApp((s) => s.account);
  const avatar = useApp((s) => s.learner.progress.profileIconId);

  useEffect(() => {
    if (!account.uid) return;
    setRows(null);
    setError(false);
    fetchLeaderboard(mode)
      .then((r) => {
        setRows(r);
        // Top of the Week: unlocks whenever the live weekly ranking has this learner in the top ten.
        if (mode === 'weeklyXp' && r.slice(0, 10).some((x) => x.isCurrentUser)) act((d) => d.checkAchievements('weeklyTopTen', 1));
      })
      .catch(() => setError(true));
  }, [mode, account.uid]);

  const myIndex = rows?.findIndex((r) => r.isCurrentUser) ?? -1;
  const me = myIndex >= 0 ? rows![myIndex] : null;

  return (
    <GroundScaffold title="Leaderboard" largeTitle subtitle="Learners of Kasiguranin, ranked">
      <div class="stack">
        <div class="segmented" role="tablist">
          {FILTERS.map((f) => (
            <button key={f.key} role="tab" aria-selected={mode === f.key} onClick={() => setMode(f.key)}>
              {f.label}
            </button>
          ))}
        </div>

        <div class="card panel row">
          <Avatar id={me?.avatarIconId ?? avatar} size={52} />
          {me ? (
            <div class="grow">
              <p class="t-title-l">You are #{myIndex + 1}</p>
              <p class="t-body muted">{mode === 'currentStreak' ? `${me.currentStreak} day streak` : mode === 'weeklyXp' ? `${me.weeklyXp} XP this week` : `${me.totalXp} XP`}</p>
            </div>
          ) : (
            <div class="grow">
              <p class="t-title">You're not ranked yet</p>
              <p class="t-body-s muted">
                {account.isAnonymous ? 'Only signed-in learners appear here. Sign in from Me, then keep earning XP.' : 'Keep earning XP to climb into the top 50.'}
              </p>
            </div>
          )}
          {account.isAnonymous && (
            <button class="text-btn lime" onClick={() => navigate('/account')}>Sign in</button>
          )}
        </div>

        {error ? (
          <EmptyState pose="worried" title="Rankings unavailable" message="Check your connection and try again." />
        ) : rows == null ? (
          <Loading label="Loading the rankings" />
        ) : rows.length === 0 ? (
          <EmptyState pose="curious" title="No one ranked yet" message="Rankings appear once signed-in learners start earning XP. Yours could be the first name here." />
        ) : (
          <div class="list">
            {rows.map((r, i) => (
              <LeaderRow key={r.uid} rank={i + 1} entry={r} mode={mode} />
            ))}
          </div>
        )}
      </div>
    </GroundScaffold>
  );
}
