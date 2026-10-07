/**
 * Badges (ui/screens/achievements): eleven families with six permanent tiers each, the earned
 * originals under Legacy, and the one-time note explaining the XP recalculation.
 */
import { STORIES_ENABLED } from '../../domain/constants';
import { useState } from 'preact/hooks';
import { BADGE_FAMILIES, BADGE_ROWS, BADGE_TIERS, badgeAmount, badgeProgress, legacyDefinition, type BadgeFamily, type BadgeRow } from '../../domain/badges';
import { legacyAchievements, pinnedFamilies, type LearnerData } from '../../domain/learner';
import { navigate } from '../../lib/router';
import { act, useLearner } from '../../lib/store';
import { BadgeMedal } from '../badges';
import { ClayButton, Dialog, GroundScaffold, Icon, ProgressBar, ProgressRing } from '../kit';

const FILTERS = ['All', 'Learning', 'Practice', 'Games', 'Community', 'Legacy'] as const;
type Filter = (typeof FILTERS)[number];

interface FamilyProgress {
  family: BadgeFamily;
  rows: (BadgeRow & { isUnlocked: boolean; unlockedDate: string | null })[];
  current: BadgeRow | undefined;
  next: BadgeRow | undefined;
  value: number;
}

export function familyProgress(d: Pick<LearnerData, 'achievements'>, family: BadgeFamily): FamilyProgress {
  const rows = BADGE_ROWS.filter((r) => r.family.id === family.id).map((r) => ({
    ...r,
    isUnlocked: d.achievements[r.id]?.isUnlocked === true,
    unlockedDate: d.achievements[r.id]?.unlockedDate ?? null,
  }));
  return {
    family,
    rows,
    current: [...rows].reverse().find((r) => r.isUnlocked),
    next: rows.find((r) => !r.isUnlocked),
    value: Math.max(0, ...rows.map((r) => d.achievements[r.id]?.currentValue ?? 0)),
  };
}

/** Where a family's action button leads (AchievementsScreen's onNavigateToActivity). */
const ACTIVITY: Record<string, string> = {
  word_explorer: '/review',
  review_keeper: '/review',
  game_adventurer: '/practice',
  precision_player: '/practice',
  mode_explorer: '/practice',
  story_reader: '/library?tab=stories',
  category_scholar: '/library',
  community_contributor: '/submit-word',
  consistent_learner: '/',
};

/** Stories are not narrated yet, so there is nothing to do towards Story Reader today. */
const comingSoon = (f: FamilyProgress) => f.family.id === 'story_reader' && !STORIES_ENABLED;
const earnedCount = (f: FamilyProgress) => f.rows.filter((r) => r.isUnlocked).length;
const reach = (f: FamilyProgress) => (f.next ? Math.min(1, Math.max(0, f.value / f.next.requiredValue)) : 1);

export function AchievementsScreen() {
  const learner = useLearner();
  const [filter, setFilter] = useState<Filter>('All');
  const [selected, setSelected] = useState<string | null>(null);
  const families = BADGE_FAMILIES.map((f) => familyProgress(learner, f));
  const tiersEarned = families.reduce((s, f) => s + earnedCount(f), 0);
  const tiersTotal = BADGE_ROWS.length;
  const pins = pinnedFamilies(learner.progress);
  const report = learner.normalization;
  const legacy = legacyAchievements(learner);
  const open = families.find((f) => f.family.id === selected);
  // Earned badges first, highest tier first; then the locked ones nearest to their first tier.
  const shown = families
    .filter((f) => filter === 'All' || f.family.section === filter)
    .sort((x, y) => earnedCount(y) - earnedCount(x) || reach(y) - reach(x));

  return (
    <GroundScaffold title="Badges" largeTitle subtitle="Your collection" wide>
      <div class="stack-lg">
        {/* CollectionSummary: a ring of tiers earned, then the ladder in its colours with a count on each rung. */}
        <div class="card panel stack" data-tour="ProgressBadgePanel">
          <div class="row" style={{ gap: 'var(--s-md)' }}>
            <ProgressRing value={tiersEarned / tiersTotal} size={76} stroke={7} color="var(--lime-fill)" label={`${tiersEarned} of ${tiersTotal} tiers earned`}>
              <span class="t-headline-s">{tiersEarned}</span>
            </ProgressRing>
            <div class="grow">
              <p class="t-title-l">
                {families.filter((f) => f.current).length} of {families.length} badges
              </p>
              <p class="t-body muted">
                {tiersEarned} of {tiersTotal} tiers earned
              </p>
              <p class="t-label" style={{ color: 'var(--lime)', marginTop: 4 }}>
                +{learner.progress.badgeBonusXp} badge XP · {learner.progress.activityXp} activity XP
              </p>
            </div>
          </div>
          <div class="tier-ladder" aria-hidden="true">
            {BADGE_TIERS.map((t) => (
              <div key={t.index}>
                <i style={{ background: `var(--tier-${t.index + 1})` }} />
                <span class="t-label-s muted">{t.label}</span>
                <span class="t-label">{families.filter((f) => f.current?.tier.index === t.index).length}</span>
              </div>
            ))}
          </div>
        </div>

        {report && !report.acknowledged && (
          <section class="card stack-sm" style={{ borderColor: 'rgba(245,200,106,.4)' }}>
            <p class="t-title">Your progress has been updated</p>
            <p class="t-body">
              Previous: {report.originalXp} XP · Level {report.originalLevel}
              <br />
              New baseline: {report.normalizedXp} XP
            </p>
            <p class="t-body-s muted">Completed learning, earned badges and unlocked content are kept. {report.unsupportedHistory}</p>
            <button class="text-btn lime" style={{ alignSelf: 'flex-start' }} onClick={() => act((d) => d.acknowledgeNormalization(), { celebrate: false })}>
              Got it
            </button>
          </section>
        )}

        <div class="row-xs" role="tablist" data-tour="ProgressFilter" style={{ overflowX: 'auto', paddingBottom: 4 }}>
          {FILTERS.map((f) => (
            <button key={f} role="tab" aria-selected={filter === f} class={`chip${filter === f ? ' on' : ''}`} onClick={() => setFilter(f)}>
              {f}
            </button>
          ))}
        </div>

        {filter === 'Legacy' ? (
          <section class="stack-sm">
            <h2 class="t-title-l">Previously earned badges</h2>
            {legacy.length === 0 ? (
              <p class="t-body muted">Your older earned badges will appear here.</p>
            ) : (
              <div class="list">
                {legacy.map(([id, s]) => {
                  const def = legacyDefinition(id);
                  return (
                    <div key={id} class="list-row">
                      <BadgeMedal tier={undefined} earned size={48} />
                      <div class="grow">
                        <p class="t-title-s">{def.name}</p>
                        <p class="t-body-s muted">{def.description}</p>
                        <p class="t-label-s faint" style={{ marginTop: 4 }}>
                          Earned {s.unlockedDate ?? 'previously'}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        ) : (
          <div class="badge-tiles">
            {shown.map((f) => {
              const earned = !!f.current;
              const count = earnedCount(f);
              return (
                // BadgeTile: the medal is the tile's subject, drawn large enough to read its artwork.
                <button
                  key={f.family.id}
                  class="card badge-tile"
                  onClick={() => setSelected(f.family.id)}
                  aria-label={`${f.family.name}, ${earned ? f.current!.tier.label : 'not earned yet'}, ${count} of 6 tiers${pins.includes(f.family.id) ? ', pinned to profile' : ''}`}
                  style={earned ? { borderColor: `color-mix(in srgb, var(--tier-${f.current!.tier.index + 1}) 55%, transparent)` } : undefined}
                >
                  {pins.includes(f.family.id) && (
                    <span class="badge-pin" aria-hidden="true">
                      <Icon name="star" size={18} color="var(--gold)" />
                    </span>
                  )}
                  <BadgeMedal tier={f.current?.tier} earned={earned} size={96} family={f.family.id} />
                  <p class="t-title-s name">{f.family.name}</p>
                  <p class="t-label" style={{ color: earned ? 'var(--lime)' : 'var(--faint)' }}>
                    {earned ? f.current!.tier.label : 'Not earned yet'}
                  </p>
                  <span class="tier-pips" aria-hidden="true">
                    {BADGE_TIERS.map((t) => (
                      <i key={t.index} style={t.index < count ? { background: `var(--tier-${t.index + 1})` } : undefined} />
                    ))}
                  </span>
                  <div class="foot">
                    {comingSoon(f) ? (
                      <p class="t-label muted">Coming soon</p>
                    ) : f.next ? (
                      <>
                        <ProgressBar value={f.value / f.next.requiredValue} height={6} />
                        <p class="t-label-s muted" style={{ marginTop: 4 }}>
                          {badgeProgress(f.family, Math.min(f.value, f.next.requiredValue), f.next.requiredValue)}
                        </p>
                      </>
                    ) : (
                      <p class="t-label" style={{ color: 'var(--lime)' }}>
                        Legend achieved
                      </p>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {open && (
        <Dialog label={open.family.name} onClose={() => setSelected(null)}>
          <div class="stack">
            {/* BadgeDetail: the medal, then the whole ladder with each rung's requirement and status. */}
            <div class="center" style={{ display: 'grid', justifyItems: 'center', gap: 4 }}>
              <BadgeMedal tier={open.current?.tier} earned={!!open.current} size={120} family={open.family.id} />
              <h2 class="t-headline-s" style={{ marginTop: 'var(--s-xs)' }}>
                {open.family.name}
              </h2>
              <p class="t-body muted">
                {open.current ? open.current.tier.label : 'Not earned yet'} · {badgeAmount(open.family, open.value)}
              </p>
              <p class="t-body-s faint">One badge, six tiers. Each tier stays earned.</p>
            </div>
            <div class="stack-sm" style={{ maxHeight: '42vh', overflowY: 'auto' }}>
              {open.rows.map((row) => (
                <div key={row.id} class={`tier-row${row.id === open.next?.id ? ' next' : ''}`}>
                  <BadgeMedal tier={row.tier} earned={row.isUnlocked} size={48} family={open.family.id} />
                  <div class="grow">
                    <div class="row">
                      <p class="t-title-s grow">{row.tier.label}</p>
                      {row.xpReward > 0 && (
                        <span class="t-label" style={{ color: 'var(--gold)' }}>
                          +{row.xpReward} XP
                        </span>
                      )}
                    </div>
                    <p class="t-body-s muted">{badgeAmount(open.family, row.requiredValue)}</p>
                    {row.isUnlocked ? (
                      <p class="t-label-s" style={{ color: 'var(--lime)' }}>
                        Earned{row.unlockedDate ? ` · ${row.unlockedDate}` : ''}
                      </p>
                    ) : (
                      <>
                        <div style={{ marginTop: 4 }}>
                          <ProgressBar value={open.value / row.requiredValue} height={5} />
                        </div>
                        <p class="t-label-s faint">
                          {Math.min(open.value, row.requiredValue)} / {row.requiredValue}
                        </p>
                      </>
                    )}
                  </div>
                </div>
              ))}
            </div>
            <div class="stack-sm">
              {comingSoon(open) ? (
                <p class="t-body muted center">Stories are coming soon. This badge opens up when they arrive.</p>
              ) : (
                <ClayButton
                  label={open.family.action}
                  onClick={() => {
                    setSelected(null);
                    navigate(ACTIVITY[open.family.id] ?? '/learn');
                  }}
                />
              )}
              {open.current && (
                <ClayButton
                  tone="quiet"
                  label={pins.includes(open.family.id) ? 'Unpin from profile' : pins.length >= 3 ? 'Three badges already pinned' : 'Pin to profile'}
                  disabled={!pins.includes(open.family.id) && pins.length >= 3}
                  onClick={() => act((d) => d.pin(open.family.id), { celebrate: false })}
                />
              )}
              <button class="text-btn muted" style={{ alignSelf: 'center' }} onClick={() => setSelected(null)}>
                Close
              </button>
            </div>
          </div>
        </Dialog>
      )}
    </GroundScaffold>
  );
}
