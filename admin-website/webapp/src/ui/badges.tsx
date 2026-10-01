/**
 * Badge medals, the reward celebration and the profile scenery picker: StandardBadgeMedal,
 * RewardCelebrationDialog and ProfileBackgroundPicker on Android.
 */
import { BADGE_TIERS, backgroundRequirement, backgroundUnlocked, familyFor, PROFILE_BACKGROUNDS, tierFor, type BadgeTier } from '../domain/badges';
import { useEffect } from 'preact/hooks';
import type { UserProgress } from '../domain/types';
import { levelUpSound } from '../lib/audio';
import { getState } from '../lib/store';
import { ClayButton, Confetti, Dialog, Icon, Jepjep, sceneUrl, type SceneId } from './kit';

/** The app's medal glyph on a tier-coloured face; a lock on a sunken one when not yet earned. */
export function BadgeMedal({ tier, earned, size = 64 }: { tier: BadgeTier | undefined; earned: boolean; size?: number }) {
  const face = earned ? `var(--tier-${(tier?.index ?? 0) + 1})` : 'var(--sunken)';
  return (
    <span
      role="img"
      aria-label={earned ? `${tier?.label ?? ''} badge` : 'Locked badge'}
      style={{
        width: size,
        height: size,
        flex: 'none',
        borderRadius: '50%',
        display: 'grid',
        placeItems: 'center',
        background: face,
        border: `2px solid ${earned ? 'rgba(255,255,255,.35)' : 'var(--hair)'}`,
      }}
    >
      <Icon name={earned ? 'medalStar' : 'lock'} size={Math.round(size * 0.56)} color={earned ? 'var(--reward-ink)' : 'var(--faint)'} />
    </span>
  );
}

/** One celebration for a level reached or tiers earned, highest tier per family. */
export function RewardDialog({
  event,
  onClose,
}: {
  event: { activityXp: number; bonusXp: number; level: number; levelChanged: boolean; badgeIds: string[] };
  onClose: () => void;
}) {
  const best = new Map<string, { name: string; tier: BadgeTier }>();
  for (const id of event.badgeIds) {
    const family = familyFor(id);
    const tier = tierFor(id);
    if (!family || !tier) continue;
    const current = best.get(family.id);
    if (!current || tier.index > current.tier.index) best.set(family.id, { name: family.name, tier });
  }
  const badges = [...best.values()];
  const title = event.levelChanged ? `Level ${event.level} reached!` : 'Badge upgraded!';
  useEffect(() => {
    if (event.levelChanged && getState().prefs.soundEnabled) levelUpSound();
  }, []);
  return (
    <>
      <Confetti pieces={event.levelChanged ? 70 : 40} />
      <Dialog glow label={title} onClose={onClose}>
        <div class="stack center">
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Jepjep pose="celebrating" height={130} breathe />
          </div>
          <h2 class="t-display" style={{ fontSize: 32 }}>{title}</h2>
          {(event.activityXp > 0 || event.bonusXp > 0) && (
            <p class="t-title" style={{ color: 'var(--gold)' }}>
              {[event.activityXp > 0 && `+${event.activityXp} activity XP`, event.bonusXp > 0 && `+${event.bonusXp} badge XP`].filter(Boolean).join(' · ')}
            </p>
          )}
          {badges.length > 0 && (
            <div class="list" style={{ textAlign: 'left', maxHeight: 280, overflowY: 'auto' }}>
              {badges.map((b) => (
                <div key={b.name} class="list-row">
                  <BadgeMedal tier={b.tier} earned size={48} />
                  <div class="grow">
                    <p class="t-title-s">{b.name}</p>
                    <p class="t-body-s muted">{b.tier.label}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
          {badges.length > 1 && <p class="t-body-s muted">All earned milestones are saved in your collection.</p>}
          <ClayButton label="Continue" tone="reward" onClick={onClose} />
        </div>
      </Dialog>
    </>
  );
}

/** Scenery for the profile banner. Places unlock with level or a long enough streak. */
export function BackgroundPicker({ progress, onSelect, onClose }: { progress: UserProgress; onSelect: (id: string) => void; onClose: () => void }) {
  return (
    <Dialog label="Choose your background" onClose={onClose}>
      <div class="stack">
        <div>
          <h2 class="t-headline-s">Choose your background</h2>
          <p class="t-body muted">Unlock more places as you learn.</p>
        </div>
        <div class="grid-2" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
          {PROFILE_BACKGROUNDS.map((b) => {
            const unlocked = backgroundUnlocked(b, progress.level, progress.longestStreak);
            const selected = (progress.profileBackgroundId || 'forest') === b.id;
            return (
              <button
                key={b.id}
                class="card"
                disabled={!unlocked}
                aria-pressed={selected}
                onClick={() => onSelect(b.id)}
                style={{ padding: 0, overflow: 'hidden', textAlign: 'left', border: `${selected ? 2 : 1}px solid ${selected ? 'var(--lime)' : 'var(--hair)'}` }}
              >
                <div style={{ position: 'relative', height: 96 }}>
                  <img src={sceneUrl(b.id as SceneId)} alt="" loading="lazy" style={{ width: '100%', height: '100%', objectFit: 'cover', opacity: unlocked ? 1 : 0.35 }} />
                  {!unlocked && (
                    <span style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center' }}>
                      <Icon name="lock" size={26} color="var(--cream)" />
                    </span>
                  )}
                </div>
                <div style={{ padding: 10 }}>
                  <p class="t-title-s">{b.title}</p>
                  <p class="t-label-s muted">{selected ? 'Selected' : unlocked ? 'Available' : backgroundRequirement(b)}</p>
                </div>
              </button>
            );
          })}
        </div>
        <ClayButton label="Done" tone="quiet" onClick={onClose} />
      </div>
    </Dialog>
  );
}

export const TIER_LADDER = BADGE_TIERS.map((t) => t.label).join(' → ');
