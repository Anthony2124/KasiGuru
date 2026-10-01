/**
 * Badges (ui/screens/achievements): every badge, grouped by family, earned or with its progress.
 */
import { useState } from 'preact/hooks';
import { ACHIEVEMENTS, METRIC, type AchievementDef } from '../../domain/gamification';
import { useLearner } from '../../lib/store';
import { GroundScaffold, Icon, ProgressBar } from '../kit';
import type { IconName } from '../icons.generated';
import { badgeValue } from './Me';

const FAMILIES = ['Levels', 'Words', 'Streaks', 'Games', 'Stories', 'Community'] as const;
const FAMILY_ICON: Record<string, IconName> = { Levels: 'medalStar', Words: 'book', Streaks: 'flash', Games: 'game', Stories: 'document', Community: 'people' };
const TIER: Record<string, [string, string]> = {
  gold: ['var(--tier-gold)', 'var(--tier-gold-deep)'],
  silver: ['var(--tier-silver)', 'var(--tier-silver-deep)'],
  bronze: ['var(--tier-bronze)', 'var(--tier-bronze-deep)'],
};

function familyOf(a: AchievementDef): string {
  switch (a.metricType) {
    case METRIC.LEVEL:
      return 'Levels';
    case METRIC.WORDS_LEARNED:
    case METRIC.CATEGORY_MASTERED:
      return 'Words';
    case METRIC.STREAK:
      return 'Streaks';
    case METRIC.GAMES_PLAYED:
    case METRIC.PERFECT_GAME:
    case METRIC.GAME_MODES_PLAYED:
    case 'perfectSixInOneSitting':
      return 'Games';
    case METRIC.STORIES_COMPLETED:
      return 'Stories';
    default:
      return 'Community';
  }
}

export function AchievementsScreen() {
  const learner = useLearner();
  const [filter, setFilter] = useState<'all' | 'earned' | 'locked'>('all');
  const unlockedCount = ACHIEVEMENTS.filter((a) => learner.achievements[a.id]?.isUnlocked).length;

  return (
    <GroundScaffold title="Badges" largeTitle subtitle="Every badge here is earned, not given">
      <div class="stack-lg">
        <div class="card panel row" style={{ background: 'linear-gradient(to bottom, rgba(255,255,255,.22), rgba(255,255,255,0) 45%), var(--gold)', color: 'var(--reward-ink)', border: 0, boxShadow: '0 5px 0 var(--gold-deep)' }}>
          <Icon name="cup" size={40} />
          <div class="grow">
            <p class="t-headline">{unlockedCount} / {ACHIEVEMENTS.length}</p>
            <p class="t-label">Badges unlocked</p>
          </div>
        </div>
        <div class="segmented" role="tablist">
          {(['all', 'earned', 'locked'] as const).map((f) => (
            <button key={f} role="tab" aria-selected={filter === f} onClick={() => setFilter(f)}>
              {f === 'all' ? 'All' : f === 'earned' ? 'Earned' : 'Locked'}
            </button>
          ))}
        </div>
        {FAMILIES.map((family) => {
          const badges = ACHIEVEMENTS.filter((a) => familyOf(a) === family).filter((a) => {
            const on = !!learner.achievements[a.id]?.isUnlocked;
            return filter === 'all' || (filter === 'earned' ? on : !on);
          });
          if (!badges.length) return null;
          return (
            <section key={family} class="stack-sm">
              <h2 class="row-xs t-title-l">
                <Icon name={FAMILY_ICON[family]} size={20} color="var(--lime)" /> {family}
              </h2>
              <div class="list">
                {badges.map((a) => {
                  const st = learner.achievements[a.id];
                  const on = !!st?.isUnlocked;
                  const value = badgeValue(a.metricType, st?.currentValue ?? 0, learner);
                  const [face, lip] = a.tier ? TIER[a.tier] : ['var(--lime)', 'var(--lime-lip)'];
                  return (
                    <div key={a.id} class="list-row" style={{ alignItems: 'flex-start' }} aria-label={`${a.name}. ${a.description}. ${on ? `Earned, ${a.xpReward} XP.` : `Locked, ${Math.min(value, a.requiredValue)} of ${a.requiredValue}.`}`}>
                      <div style={{ width: 48, height: 48, borderRadius: '50%', flex: 'none', display: 'grid', placeItems: 'center', background: on ? face : 'var(--sunken)', boxShadow: on ? `0 4px 0 ${lip}` : 'none', border: on ? 0 : '1px solid var(--hair)' }}>
                        <Icon name={on ? FAMILY_ICON[family] : 'lock'} size={22} color={on ? 'var(--reward-ink)' : 'var(--faint)'} />
                      </div>
                      <div class="grow">
                        <div class="row-xs" style={{ justifyContent: 'space-between' }}>
                          <p class="t-title-s" style={{ color: on ? 'var(--ink)' : 'var(--muted)' }}>{a.name}</p>
                          <span class="tag gold">+{a.xpReward} XP</span>
                        </div>
                        <p class="t-body-s muted">{a.description}</p>
                        {on ? (
                          <p class="t-label-s" style={{ color: 'var(--lime)', marginTop: 4 }}>Earned{st?.unlockedDate ? ` · ${st.unlockedDate}` : ''}</p>
                        ) : (
                          <div class="row" style={{ marginTop: 6 }}>
                            <div class="grow">
                              <ProgressBar value={value / a.requiredValue} color="var(--gold)" height={6} />
                            </div>
                            <span class="t-label-s muted">{Math.min(value, a.requiredValue)} of {a.requiredValue}</span>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          );
        })}
      </div>
    </GroundScaffold>
  );
}
