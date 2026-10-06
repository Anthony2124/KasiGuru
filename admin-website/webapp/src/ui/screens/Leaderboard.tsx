/**
 * Leaderboard (ui/screens/leaderboard): this week's XP, all-time XP or streak, a podium for the top
 * three, and your own rank even outside the top 50. A row opens that learner's public profile.
 * Guests are not ranked, on either app.
 */
import { useEffect, useState } from 'preact/hooks';
import { plural } from '../../domain/plural';
import { fetchLeaderboard, type LeaderboardEntry, type LeaderboardOrder } from '../../lib/remote';
import { navigate } from '../../lib/router';
import { useApp } from '../../lib/store';
import { Avatar, EmptyState, GroundScaffold, Icon, Loading } from '../kit';

const figure = (e: LeaderboardEntry, mode: LeaderboardOrder) =>
  mode === 'currentStreak' ? plural(e.currentStreak, 'day') : `${mode === 'weeklyXp' ? e.weeklyXp : e.totalXp} XP`;

const openPlayer = (e: LeaderboardEntry) => navigate(`/player/${encodeURIComponent(e.uid)}`);

export function LeaderRow({ entry, mode }: { entry: LeaderboardEntry; mode: LeaderboardOrder }) {
  const byStreak = mode === 'currentStreak';
  const medal = entry.rank === 1 ? 'var(--tier-gold)' : entry.rank === 2 ? 'var(--tier-silver)' : entry.rank === 3 ? 'var(--tier-bronze)' : null;
  return (
    <button
      class="list-row leader-row"
      data-tour="LeaderboardPlayer"
      onClick={() => openPlayer(entry)}
      style={entry.isCurrentUser ? { background: 'var(--lime-tint)' } : undefined}
      aria-label={`Rank ${entry.rank}, ${entry.name}${entry.isCurrentUser ? ', you' : ''}, ${figure(entry, mode)}`}
    >
      <span class="rank-pill" style={medal ? { background: medal, color: 'var(--reward-ink)' } : undefined}>
        {entry.rank}
      </span>
      <Avatar id={entry.avatarIconId} size={48} />
      <div class="grow">
        <p class="t-title">{entry.isCurrentUser ? `${entry.name} (you)` : entry.name}</p>
        <p class="t-body muted">
          Level {entry.level}
          {/* On a narrow page the score joins this line, so the name keeps the width. */}
          <span class="leader-inline"> · {figure(entry, mode)}</span>
        </p>
      </div>
      <span class="row-xs t-title-s leader-figure">
        <Icon name={byStreak ? 'flash' : 'star'} size={18} color={byStreak ? 'var(--coral)' : 'var(--gold)'} />
        {figure(entry, mode)}
      </span>
    </button>
  );
}

function Podium({ rows, mode }: { rows: LeaderboardEntry[]; mode: LeaderboardOrder }) {
  const top = rows.filter((r) => r.rank >= 1 && r.rank <= 3).slice(0, 3);
  return (
    <div class="podium">
      {[1, 0, 2].map((i) => {
        const p = top[i];
        if (!p) return <span key={i} />;
        return (
          <button
            key={p.uid}
            class={`card center${i === 0 ? ' first' : ''}`}
            data-tour="LeaderboardPlayer"
            onClick={() => openPlayer(p)}
            aria-label={`Rank ${p.rank}, ${p.name}, ${figure(p, mode)}`}
          >
            <Avatar id={p.avatarIconId} size={i === 0 ? 76 : 64} level={p.level} />
            <p class="t-headline-s" style={{ color: 'var(--lime)' }}>#{p.rank}</p>
            <p class="t-title-s podium-name">{p.name}</p>
            <p class="t-label muted">{figure(p, mode)}</p>
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
    <GroundScaffold title="Leaderboard" largeTitle subtitle="Learners of Kasiguranin, ranked" wide>
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

        {/* On a computer: your rank and the podium on the left, everyone else on the right. */}
        <div class="cols board">
          <div class="stack sticky-col">
            {me ? (
              <button class="card panel row" style={{ borderColor: 'var(--lime)', background: 'var(--lime-tint)', textAlign: 'left' }} onClick={() => openPlayer(me)}>
                <Avatar id={me.avatarIconId} size={56} />
                <div class="grow">
                  <p class="t-title">You · #{me.rank}</p>
                  <p class="t-body muted">{figure(me, mode)}</p>
                </div>
                <Icon name="arrowRight" size={18} color="var(--faint)" />
              </button>
            ) : (
              <div class="card panel row" style={{ borderColor: 'var(--lime)', background: 'var(--lime-tint)' }}>
                <Avatar id={avatar} size={56} />
                <div class="grow">
                  <p class="t-title">Your rank</p>
                  <p class="t-body muted">{account.isAnonymous ? 'Only signed-in learners appear here.' : 'Practise to join the rankings'}</p>
                </div>
                {account.isAnonymous && (
                  <button class="text-btn lime" onClick={() => navigate('/account')}>
                    Sign in
                  </button>
                )}
              </div>
            )}
            {!error && rows != null && ranked.length > 0 && <Podium rows={ranked} mode={mode} />}
          </div>

          <div>
            {error ? (
              <EmptyState pose="worried" title="Rankings unavailable" message="Check your connection and try again." />
            ) : rows == null ? (
              <Loading label="Loading rankings" />
            ) : ranked.length === 0 ? (
              <EmptyState pose="curious" title="No one ranked yet" message="Signed-in learners appear here when they practise." />
            ) : ranked.length > 3 ? (
              <div class="list">
                {ranked.slice(3).map((r) => (
                  <LeaderRow key={r.uid} entry={r} mode={mode} />
                ))}
              </div>
            ) : null}
          </div>
        </div>
      </div>
  );
}
