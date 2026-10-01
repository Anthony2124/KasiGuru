/**
 * Leaderboard (ui/screens/leaderboard): this week's XP, all-time XP or streak, a podium for the top
 * three, and your own rank even outside the top 50. A row opens that learner's public profile.
 * Guests are not ranked, on either app.
 */
import { useEffect, useState } from 'preact/hooks';
import { fetchLeaderboard, type LeaderboardEntry, type LeaderboardOrder } from '../../lib/remote';
import { navigate } from '../../lib/router';
import { useApp } from '../../lib/store';
import { Avatar, EmptyState, GroundScaffold, Icon, Loading } from '../kit';

const figure = (e: LeaderboardEntry, mode: LeaderboardOrder) =>
  mode === 'currentStreak' ? `${e.currentStreak} days` : `${mode === 'weeklyXp' ? e.weeklyXp : e.totalXp} XP`;

const openPlayer = (e: LeaderboardEntry) => navigate(`/player/${encodeURIComponent(e.uid)}`);

export function LeaderRow({ entry, mode }: { entry: LeaderboardEntry; mode: LeaderboardOrder }) {
  const byStreak = mode === 'currentStreak';
  const medal = entry.rank === 1 ? 'var(--tier-gold)' : entry.rank === 2 ? 'var(--tier-silver)' : entry.rank === 3 ? 'var(--tier-bronze)' : null;
  return (
    <button
      class="list-row"
      onClick={() => openPlayer(entry)}
      style={entry.isCurrentUser ? { background: 'var(--lime-tint)' } : undefined}
      aria-label={`Rank ${entry.rank}, ${entry.name}${entry.isCurrentUser ? ', you' : ''}, ${figure(entry, mode)}`}
    >
      <span style={{ minWidth: 28, height: 28, borderRadius: 14, padding: '0 4px', display: 'grid', placeItems: 'center', background: medal ?? 'transparent', color: medal ? 'var(--reward-ink)' : 'var(--muted)', fontFamily: 'var(--display)', fontWeight: 700, fontSize: 14, flex: 'none' }}>
        {entry.rank}
      </span>
      <Avatar id={entry.avatarIconId} size={40} />
      <div class="grow" style={{ minWidth: 0 }}>
        <p class="t-title-s" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{entry.isCurrentUser ? `${entry.name} (you)` : entry.name}</p>
        <p class="t-body-s faint" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>Level {entry.level}</p>
      </div>
      <span class="row-xs t-label">
        <Icon name={byStreak ? 'flash' : 'star'} size={16} color={byStreak ? 'var(--coral)' : 'var(--gold)'} />
        {figure(entry, mode)}
      </span>
    </button>
  );
}

function Podium({ rows, mode }: { rows: LeaderboardEntry[]; mode: LeaderboardOrder }) {
  const top = rows.filter((r) => r.rank >= 1 && r.rank <= 3).slice(0, 3);
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 'var(--s-xs)', alignItems: 'end' }}>
      {[1, 0, 2].map((i) => {
        const p = top[i];
        if (!p) return <span key={i} />;
        return (
          <button
            key={p.uid}
            class="card center"
            onClick={() => openPlayer(p)}
            style={{ padding: `${i === 0 ? 22 : 8}px 6px 10px`, background: i === 0 ? 'rgba(255,200,61,.16)' : undefined, display: 'grid', justifyItems: 'center', gap: 4, minWidth: 0 }}
            aria-label={`Rank ${p.rank}, ${p.name}, ${figure(p, mode)}`}
          >
            <Avatar id={p.avatarIconId} size={52} level={p.level} />
            <p class="t-title-s" style={{ maxWidth: '100%', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.name}</p>
            <p class="t-headline-s" style={{ color: 'var(--lime)' }}>#{p.rank}</p>
            <p class="t-label-s muted">{figure(p, mode)}</p>
          </button>
        );
      })}
    </div>
  );
}

const FILTERS: { key: LeaderboardOrder; label: string }[] = [
  { key: 'weeklyXp', label: 'This week' },
  { key: 'totalXp', label: 'All-time' },
  { key: 'currentStreak', label: 'Streaks' },
];

export function LeaderboardScreen() {
  return (
    <GroundScaffold title="Leaderboard" largeTitle subtitle="Learners of Kasiguranin, ranked">
      <LeaderboardContent />
    </GroundScaffold>
  );
}

/** The three boards, as on the Leaderboard screen and Practice's Leaderboard tab. */
export function LeaderboardContent() {
  const [mode, setMode] = useState<LeaderboardOrder>('weeklyXp');
  const [rows, setRows] = useState<LeaderboardEntry[] | null>(null);
  const [error, setError] = useState(false);
  const account = useApp((s) => s.account);
  const avatar = useApp((s) => s.learner.progress.profileIconId);

  useEffect(() => {
    if (!account.uid) return;
    setRows(null);
    setError(false);
    fetchLeaderboard(mode)
      .then(setRows)
      .catch(() => setError(true));
  }, [mode, account.uid]);

  const me = rows?.find((r) => r.isCurrentUser) ?? null;
  const ranked = rows?.filter((r) => r.rank >= 1 && r.rank <= 50).sort((a, b) => a.rank - b.rank) ?? [];

  return (
      <div class="stack">
        <div class="segmented" role="tablist">
          {FILTERS.map((f) => (
            <button key={f.key} role="tab" aria-selected={mode === f.key} onClick={() => setMode(f.key)}>
              {f.label}
            </button>
          ))}
        </div>

        {me ? (
          <button class="card panel row" style={{ borderColor: 'var(--lime)', background: 'var(--lime-tint)', textAlign: 'left' }} onClick={() => openPlayer(me)}>
            <Avatar id={me.avatarIconId} size={48} />
            <div class="grow">
              <p class="t-title">You · #{me.rank}</p>
              <p class="t-body-s muted">{figure(me, mode)}</p>
            </div>
            <Icon name="arrowRight" size={18} color="var(--faint)" />
          </button>
        ) : (
          <div class="card panel row" style={{ borderColor: 'var(--lime)', background: 'var(--lime-tint)' }}>
            <Avatar id={avatar} size={48} />
            <div class="grow">
              <p class="t-title">Your rank</p>
              <p class="t-body-s muted">{account.isAnonymous ? 'Only signed-in learners appear here.' : 'Practise to join the rankings'}</p>
            </div>
            {account.isAnonymous && (
              <button class="text-btn lime" onClick={() => navigate('/account')}>
                Sign in
              </button>
            )}
          </div>
        )}

        {error ? (
          <EmptyState pose="worried" title="Rankings unavailable" message="Check your connection and try again." />
        ) : rows == null ? (
          <Loading label="Loading rankings" />
        ) : ranked.length === 0 ? (
          <EmptyState pose="curious" title="No one ranked yet" message="Signed-in learners appear here when they practise." />
        ) : (
          <>
            <Podium rows={ranked} mode={mode} />
            {ranked.length > 3 && (
              <div class="list">
                {ranked.slice(3).map((r) => (
                  <LeaderRow key={r.uid} entry={r} mode={mode} />
                ))}
              </div>
            )}
          </>
        )}
      </div>
  );
}
