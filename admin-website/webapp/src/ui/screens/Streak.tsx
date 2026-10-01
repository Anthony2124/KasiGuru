/**
 * Your streak (ui/screens/streak/StreakScreen): the count, this week, today's quota, and the six
 * Consistent Learner tiers the longest streak has reached.
 */
import { BADGE_FAMILIES, BADGE_TIERS } from '../../domain/badges';
import { Draft } from '../../domain/learner';
import { switchTab } from '../../lib/router';
import { useLearner } from '../../lib/store';
import { BadgeMedal } from '../badges';
import { buildWeek } from '../derive';
import { ClayButton, GroundScaffold, Icon, Jepjep } from '../kit';
import { QuotaList, WeekStrip } from '../parts';

export function StreakScreen() {
  const learner = useLearner();
  const p = learner.progress;
  const quota = new Draft(learner).quota;
  const family = BADGE_FAMILIES.find((f) => f.id === 'consistent_learner')!;
  const next = family.thresholds.find((t) => t > p.longestStreak);
  const days = (n: number) => `${n} ${n === 1 ? 'day' : 'days'}`;

  return (
    <GroundScaffold title="Your streak">
      <div class="stack-lg" style={{ background: 'linear-gradient(to bottom, rgba(255,159,28,.15), transparent 320px)', borderRadius: 'var(--r-panel)' }}>
        <section class="center stack-sm" style={{ paddingTop: 'var(--s-md)' }}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Icon name="flash" size={88} color="var(--coral)" />
          </div>
          <p class="t-display" style={{ fontSize: 64, lineHeight: 1 }}>{p.currentStreak}</p>
          <p class="t-headline-s">day streak</p>
          <p class="t-body muted">Longest streak: {days(p.longestStreak)}</p>
        </section>

        <WeekStrip week={buildWeek(p)} />

        {p.currentStreak === 0 && p.longestStreak > 0 && (
          <div class="row" style={{ justifyContent: 'center' }}>
            <Jepjep pose="sad" height={96} />
            <p class="t-body">A fresh start counts. Let's practise today.</p>
          </div>
        )}

        <section class="stack-sm">
          <p class="t-title">{quota.isMet ? 'Today counts. Your streak is safe!' : 'Today'}</p>
          <QuotaList quota={quota} />
        </section>

        <section class="stack-sm">
          <p class="t-title">{next ? `Next Consistent Learner badge: ${days(next)}` : 'Every streak tier earned!'}</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 'var(--s-md)' }}>
            {family.thresholds.map((t, i) => {
              const earned = p.longestStreak >= t;
              return (
                <div key={t} class="center stack-sm" style={{ justifyItems: 'center' }}>
                  <div style={{ display: 'grid', placeItems: 'center', borderRadius: '50%', outline: t === next ? '2px solid var(--coral)' : undefined, outlineOffset: 3 }}>
                    <BadgeMedal tier={BADGE_TIERS[i]} earned={earned} size={56} />
                  </div>
                  <p class="t-label">{days(t)}</p>
                  <p class="t-label-s muted">{BADGE_TIERS[i].label}</p>
                </div>
              );
            })}
          </div>
        </section>

        <ClayButton label="Continue practising" onClick={() => switchTab('/')} />
      </div>
    </GroundScaffold>
  );
}
