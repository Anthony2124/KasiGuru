/**
 * Badges (ui/screens/achievements): eleven families with six permanent tiers each, the earned
 * originals under Legacy, and the one-time note explaining the XP recalculation.
 */
import { STORIES_ENABLED } from '../../domain/constants';
import { useState } from 'preact/hooks';
import { BADGE_FAMILIES, BADGE_ROWS, badgeAmount, badgeProgress, legacyDefinition, type BadgeFamily, type BadgeRow } from '../../domain/badges';
import { legacyAchievements, pinnedFamilies, type LearnerData } from '../../domain/learner';
import { navigate } from '../../lib/router';
import { act, useLearner } from '../../lib/store';
import { BadgeMedal, TIER_LADDER } from '../badges';
import { ClayButton, Dialog, GroundScaffold, ProgressBar } from '../kit';

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

export function AchievementsScreen() {
  const learner = useLearner();
  const [filter, setFilter] = useState<Filter>('All');
  const [selected, setSelected] = useState<string | null>(null);
  const families = BADGE_FAMILIES.map((f) => familyProgress(learner, f));
  const tiersEarned = families.reduce((s, f) => s + f.rows.filter((r) => r.isUnlocked).length, 0);
  const pins = pinnedFamilies(learner.progress);
  const report = learner.normalization;
  const legacy = legacyAchievements(learner);
  const open = families.find((f) => f.family.id === selected);

  return (
    <GroundScaffold title="Badges" largeTitle subtitle="Your collection" wide>
      <div class="stack-lg">
        <div class="card panel stack-sm">
          <p class="t-headline-s">
            {families.filter((f) => f.current).length} of 11 badges · {tiersEarned} of 66 tiers
          </p>
          <p class="t-body-s muted">{TIER_LADDER}</p>
          <p class="t-label" style={{ color: 'var(--lime)' }}>
            {learner.progress.activityXp} activity XP · {learner.progress.badgeBonusXp} badge XP
          </p>
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

        <div class="row-xs" role="tablist" style={{ overflowX: 'auto', paddingBottom: 4 }}>
          {FILTERS.map((f) => (
            <button key={f} role="tab" aria-selected={filter === f} class="chip" onClick={() => setFilter(f)} style={filter === f ? { background: 'var(--olive)', color: 'var(--ink)' } : undefined}>
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
                    <div key={id} class="list-row" style={{ alignItems: 'flex-start' }}>
                      <div class="grow">
                        <p class="t-title-s">{def.name}</p>
                        <p class="t-body-s muted">{def.description}</p>
                        <p class="t-label-s faint" style={{ marginTop: 4 }}>Earned {s.unlockedDate ?? 'previously'}</p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>
        ) : (
          <div class="list cols-2">
            {families
              .filter((f) => filter === 'All' || f.family.section === filter)
              .map((f) => (
                <button key={f.family.id} class="list-row" onClick={() => setSelected(f.family.id)} style={{ alignItems: 'center' }}>
                  <BadgeMedal tier={f.current?.tier} earned={!!f.current} size={56} />
                  <div class="grow">
                    <p class="t-title-s">{f.family.name}</p>
                    <p class="t-body-s muted">{f.current ? f.current.tier.label : 'Not earned yet'}</p>
                    {f.family.id === 'story_reader' && !STORIES_ENABLED ? (
                      <p class="t-label-s muted" style={{ marginTop: 4 }}>Coming soon</p>
                    ) : f.next ? (
                      <>
                        <p class="t-label-s" style={{ marginTop: 4 }}>
                          {badgeProgress(f.family, Math.min(f.value, f.next.requiredValue), f.next.requiredValue)} · Next: {f.next.tier.label}
                        </p>
                        <div style={{ marginTop: 6 }}>
                          <ProgressBar value={f.value / f.next.requiredValue} color="var(--gold)" height={6} />
                        </div>
                      </>
                    ) : (
                      <p class="t-label-s" style={{ color: 'var(--lime)', marginTop: 4 }}>Legend achieved</p>
                    )}
                    {pins.includes(f.family.id) && (
                      <p class="t-label-s" style={{ color: 'var(--lime)', marginTop: 4 }}>Pinned to profile</p>
                    )}
                  </div>
                </button>
              ))}
          </div>
        )}
      </div>

      {open && (
        <Dialog label={open.family.name} onClose={() => setSelected(null)}>
          <div class="stack">
            <div>
              <h2 class="t-headline-s">{open.family.name}</h2>
              <p class="t-body-s muted">One badge, six tiers. Each tier stays earned.</p>
            </div>
            <div class="list" style={{ maxHeight: '45vh', overflowY: 'auto' }}>
              {open.rows.map((row) => (
                <div key={row.id} class="list-row">
                  <BadgeMedal tier={row.tier} earned={row.isUnlocked} size={44} />
                  <div class="grow">
                    <p class="t-title-s">{row.tier.label}</p>
                    <p class="t-body-s">{badgeAmount(open.family, row.requiredValue)}</p>
                    <p class="t-label-s muted">
                      {row.isUnlocked ? `Earned · ${row.unlockedDate ?? ''}` : `${Math.min(open.value, row.requiredValue)} / ${row.requiredValue}`}
                    </p>
                  </div>
                  {row.xpReward > 0 && <span class="tag gold">+{row.xpReward} badge XP</span>}
                </div>
              ))}
            </div>
            {open.current && (
              <button
                class="text-btn lime"
                disabled={!pins.includes(open.family.id) && pins.length >= 3}
                onClick={() => act((d) => d.pin(open.family.id), { celebrate: false })}
              >
                {pins.includes(open.family.id) ? 'Unpin from profile' : pins.length >= 3 ? 'Three badges already pinned' : 'Pin to profile'}
              </button>
            )}
            <div class="stack-sm">
              <ClayButton
                label={open.family.action}
                onClick={() => {
                  setSelected(null);
                  navigate(ACTIVITY[open.family.id] ?? '/learn');
                }}
              />
              <ClayButton label="Close" tone="quiet" onClick={() => setSelected(null)} />
            </div>
          </div>
        </Dialog>
      )}
    </GroundScaffold>
  );
}
