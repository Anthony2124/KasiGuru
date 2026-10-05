/**
 * Your XP (ui/screens/xp/XpScreen), the streak page's twin, in the order a learner asks about it:
 * how much do I have, how far is the next level, what did today earn, how did the week go, where
 * does it come from, and which level is the next milestone.
 */
import { useMemo } from 'preact/hooks';
import { BADGE_FAMILIES, BADGE_TIERS, type BadgeTier } from '../../domain/badges';
import { levelProgress } from '../../domain/constants';
import { today as todayIso } from '../../domain/dates';
import { GAME_LEVEL_COUNT } from '../../domain/gamification';
import { dailyXpEarned, totalStars } from '../../domain/learner';
import { lastSevenDays, LEVEL_THRESHOLDS, levelFor, MAX_LEVEL, XP_SOURCE_LABELS, XP_SOURCES, xpBySource, type XpSource } from '../../domain/xp';
import { switchTab } from '../../lib/router';
import { useLearner } from '../../lib/store';
import { BadgeMedal } from '../badges';
import type { IconName } from '../icons.generated';
import { ClayButton, GroundScaffold, Icon, ProgressBar } from '../kit';

const SOURCE_ICONS: Record<XpSource, IconName> = {
  Lessons: 'teacher',
  Games: 'game',
  Reviews: 'repeat',
  Stories: 'book',
  Community: 'people',
  Badges: 'medal',
};

/** "Mon" for a yyyy-MM-dd day, read as a calendar day rather than a UTC instant. */
function weekday(day: string) {
  const [y, m, d] = day.split('-').map(Number);
  return { label: new Date(y, m - 1, d).toLocaleDateString('en', { weekday: 'short' }), dayOfMonth: d };
}

export function XpScreen() {
  const learner = useLearner();
  const p = learner.progress;
  const today = todayIso();
  const records = useMemo(() => Object.values(learner.receipts), [learner.receipts]);
  const week = useMemo(() => lastSevenDays(records, today), [records, today]);
  const bySource = useMemo(() => xpBySource(records), [records]);
  const todayXp = dailyXpEarned(learner, today);
  const stars = totalStars(learner.gameLevels);
  const level = levelFor(p.totalXp);
  const family = BADGE_FAMILIES.find((f) => f.id === 'journey_rank')!;
  const nextRank = family.thresholds.find((t) => t > level);

  return (
    <GroundScaffold title="Your XP" wide>
      <div class="cols xp-panel" style={{ background: 'linear-gradient(to bottom, rgba(255,200,61,.12), transparent 320px)', borderRadius: 'var(--r-panel)' }}>
        <div class="stack-lg">
          <XpHero totalXp={p.totalXp} level={level} />
          <TodayCard earned={todayXp} goal={p.dailyGoalXp} />
          <section class="card">
            <div class="row">
              <h2 class="t-title grow">This week</h2>
              <span class="t-label muted">{week.reduce((s, d) => s + d.xp, 0)} XP</span>
            </div>
            <WeekBars days={week} today={today} />
          </section>
        </div>

        <div class="stack-lg">
          <SourcesCard bySource={bySource} />
          <GameStarsCard stars={stars} possible={GAME_LEVEL_COUNT * 3} />
          <section class="card">
            <h2 class="t-title">Level milestones</h2>
            <p class="t-body-s muted">{nextRank ? `Next: ${family.name} at Level ${nextRank}` : 'Every level milestone earned!'}</p>
            <ol class="milestones">
              {family.thresholds.map((rank, i) => (
                <MilestoneRow key={rank} rank={rank} tier={BADGE_TIERS[i]} totalXp={p.totalXp} level={level} isNext={rank === nextRank} />
              ))}
            </ol>
          </section>
        </div>
      </div>
    </GroundScaffold>
  );
}

/** The total, big, inside a star medallion; the level and how far the next one is under it. */
function XpHero({ totalXp, level }: { totalXp: number; level: number }) {
  const floor = LEVEL_THRESHOLDS[level - 1];
  const nextFloor = level < MAX_LEVEL ? LEVEL_THRESHOLDS[level] : null;
  return (
    <section class="stack-sm" style={{ paddingTop: 'var(--s-sm)' }}>
      <div class="center stack-sm">
        <div style={{ display: 'grid', placeItems: 'center' }}>
          <div class="xp-medallion">
            <Icon name="star" size={64} color="var(--gold)" />
          </div>
        </div>
        <p>
          <span class="sr-only">{totalXp} XP in total</span>
          <span class="t-display" style={{ fontSize: 64, lineHeight: 1 }} aria-hidden="true">{totalXp}</span>{' '}
          <span class="t-headline-s" aria-hidden="true">XP</span>
        </p>
      </div>
      <div class="card">
        <div class="row">
          <p class="t-title grow">Level {level}</p>
          <span class="t-label muted">{nextFloor == null ? 'Top level reached' : `${nextFloor - totalXp} XP to Level ${level + 1}`}</span>
        </div>
        <div style={{ marginTop: 'var(--s-xs)' }}>
          <ProgressBar value={levelProgress(totalXp)} label="Progress to the next level" />
        </div>
        {nextFloor != null && (
          <p class="t-body-s muted" style={{ marginTop: 'var(--s-xxs)' }}>
            {totalXp - floor} of {nextFloor - floor} XP this level
          </p>
        )}
      </div>
    </section>
  );
}

/** Today's XP against the daily goal the learner chose. The lime button is the way to earn more. */
function TodayCard({ earned, goal }: { earned: number; goal: number }) {
  const met = goal > 0 && earned >= goal;
  return (
    <section class="card">
      <div class="row">
        <h2 class="t-title grow">Today</h2>
        <span class={`tag${met ? '' : ' amber'}`}>{met ? 'Goal met' : 'In progress'}</span>
      </div>
      <div class="row" style={{ marginTop: 'var(--s-sm)' }}>
        <span class={`xp-ico large${met ? ' met' : ''}`}>
          <Icon name={met ? 'tickCircle' : 'star'} size={20} color={met ? 'var(--on-lime)' : 'var(--gold)'} />
        </span>
        <div class="grow">
          <p class="t-title-s">Daily goal</p>
          <p class="t-body-s muted">{met ? `${earned} XP today, goal of ${goal} reached` : `${earned} of ${goal} XP`}</p>
          {!met && goal > 0 && (
            <div style={{ marginTop: 'var(--s-xxs)' }}>
              <ProgressBar value={earned / goal} height={6} label="Daily goal progress" />
            </div>
          )}
        </div>
      </div>
      <div style={{ marginTop: 'var(--s-md)' }}>
        <ClayButton label={met ? 'Keep learning' : 'Earn more XP'} onClick={() => switchTab('/')} />
      </div>
    </section>
  );
}

/** Seven bars, oldest first, each as tall as its share of the best day; today is labelled in ink. */
function WeekBars({ days, today }: { days: { day: string; xp: number }[]; today: string }) {
  const best = Math.max(1, ...days.map((d) => d.xp));
  return (
    <div class="xp-week" role="list">
      {days.map(({ day, xp }) => {
        const { label, dayOfMonth } = weekday(day);
        const isToday = day === today;
        return (
          <div key={day} class="xp-day" role="listitem" aria-label={`${label} ${dayOfMonth}, ${xp} XP`}>
            {xp > 0 && <span class="t-label-s muted" aria-hidden="true">{xp}</span>}
            {/* A day with nothing still shows a stub, so the week reads as seven slots. */}
            <span
              class={`xp-bar${xp > 0 ? (isToday ? ' today' : ' earned') : ''}`}
              style={{ height: xp > 0 ? Math.max(6, (76 * xp) / best) : 4 }}
              aria-hidden="true"
            />
            <span class={`t-label${isToday ? '' : ' muted'}`} aria-hidden="true">{label}</span>
          </div>
        );
      })}
    </div>
  );
}

/** Every source that has earned something, largest first, with its share of the total. */
function SourcesCard({ bySource }: { bySource: Partial<Record<XpSource, number>> }) {
  const entries = XP_SOURCES.filter((s) => bySource[s]).sort((a, b) => bySource[b]! - bySource[a]!);
  const total = Math.max(1, entries.reduce((s, k) => s + bySource[k]!, 0));
  return (
    <section class="card">
      <h2 class="t-title">Where your XP comes from</h2>
      {entries.length === 0 ? (
        <p class="t-body muted" style={{ marginTop: 'var(--s-sm)' }}>Finish a lesson or play a game to earn your first XP.</p>
      ) : (
        <div class="stack" style={{ marginTop: 'var(--s-sm)' }}>
          {entries.map((source) => (
            <div key={source} class="row">
              <span class="xp-ico">
                <Icon name={SOURCE_ICONS[source]} size={18} color="var(--lime)" />
              </span>
              <div class="grow">
                <div class="row">
                  <p class="t-title-s grow">{XP_SOURCE_LABELS[source]}</p>
                  <span class="t-label">{bySource[source]} XP</span>
                </div>
                <div style={{ marginTop: 'var(--s-xxs)' }}>
                  <ProgressBar value={bySource[source]! / total} height={6} label={`${XP_SOURCE_LABELS[source]}, share of your XP`} />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

/** The stars won on game levels: a separate tally from XP, so it gets its own card and a way to earn more. */
function GameStarsCard({ stars, possible }: { stars: number; possible: number }) {
  return (
    <button class="card" onClick={() => switchTab('/practice')}>
      <div class="row">
        <span class="xp-ico large gold">
          <Icon name="star" size={22} color="var(--gold)" />
        </span>
        <div class="grow">
          <p class="t-title-s">{stars} game {stars === 1 ? 'star' : 'stars'}</p>
          <p class="t-body-s muted">{possible > 0 ? `Out of ${possible} across every game level` : 'Clear a game level to earn stars'}</p>
        </div>
        <span class="t-label" style={{ color: 'var(--lime)' }}>Play</span>
      </div>
      {possible > 0 && (
        <div style={{ marginTop: 'var(--s-xs)' }}>
          <ProgressBar value={stars / possible} height={6} label="Game stars earned" />
        </div>
      )}
    </button>
  );
}

/** One rung of the Journey Rank ladder. The next rung shows the XP still to earn. */
function MilestoneRow({ rank, tier, totalXp, level, isNext }: { rank: number; tier: BadgeTier; totalXp: number; level: number; isNext: boolean }) {
  const earned = level >= rank;
  const needed = LEVEL_THRESHOLDS[rank - 1] ?? 0;
  return (
    <li class={`milestone${earned ? ' earned' : ''}`}>
      <span class="rail">
        <BadgeMedal tier={tier} earned={earned} size={40} />
      </span>
      <div class="body">
        <div class="row">
          <p class={`t-title-s grow${earned || isNext ? '' : ' muted'}`}>Level {rank}</p>
          <span class="t-label" style={{ color: earned ? 'var(--gold)' : 'var(--faint)' }}>{tier.label}</span>
        </div>
        {earned ? (
          <p class="t-body-s muted">Earned</p>
        ) : isNext ? (
          <>
            <p class="t-body-s muted">{Math.max(0, needed - totalXp)} XP to go</p>
            <div style={{ marginTop: 'var(--s-xxs)' }}>
              <ProgressBar value={needed > 0 ? totalXp / needed : 0} height={6} label={`Progress to Level ${rank}`} />
            </div>
          </>
        ) : (
          <p class="t-body-s faint">{needed} XP</p>
        )}
      </div>
    </li>
  );
}
