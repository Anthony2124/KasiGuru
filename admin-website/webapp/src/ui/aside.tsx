/**
 * The side panel a computer has room for beside the learning path: level, streak, today's goal,
 * the review deck and badges, each a way into its screen. Hidden on phones (.wide-only), where
 * Home and Me already carry the same figures.
 */
import { BADGE_ROWS } from '../domain/badges';
import { levelProgress, levelTitle, xpToNextLevel } from '../domain/constants';
import { navigate } from '../lib/router';
import { useCorpus, useLearner } from '../lib/store';
import { dayGoal, wordsToReview } from './derive';
import { Icon, ProgressBar, ProgressRing } from './kit';

export function ProgressAside() {
  const learner = useLearner();
  const corpus = useCorpus();
  const p = learner.progress;
  const due = Math.min(corpus.countScheduledDue(), 20);
  const goal = dayGoal(learner, due);
  const toNext = xpToNextLevel(p.totalXp);
  const tiers = BADGE_ROWS.filter((r) => learner.achievements[r.id]?.isUnlocked).length;

  return (
    <aside class="wide-only sticky-col stack">
      <button class="card stack-sm" onClick={() => navigate('/me')}>
        <div class="row" style={{ justifyContent: 'space-between' }}>
          <p class="t-title" style={{ color: 'var(--lime)' }}>{levelTitle(p.level)}</p>
          <span class="row-xs t-label">
            <Icon name="star" size={16} color="var(--gold)" /> {p.totalXp} XP
          </span>
        </div>
        <ProgressBar value={levelProgress(p.totalXp)} label="Progress to the next level" />
        <p class="t-body-s muted">{toNext == null ? 'Highest level reached' : `${toNext} XP to Level ${p.level + 1}`}</p>
      </button>

      <div class="grid-2">
        <button class="card center" onClick={() => navigate('/streak')} style={{ display: 'grid', justifyItems: 'center', gap: 4 }}>
          <Icon name="flash" size={32} color="var(--coral)" />
          <p class="t-headline-s">{p.currentStreak}</p>
          <p class="t-label-s muted">day streak</p>
        </button>
        <button class="card center" onClick={() => navigate('/me')} style={{ display: 'grid', justifyItems: 'center', gap: 4 }}>
          <ProgressRing value={goal.fraction} size={56} stroke={6} color={goal.met ? 'var(--green)' : 'var(--gold)'} label={`Daily goal: ${goal.earned} of ${goal.goal} XP`}>
            <p class="t-label">{goal.earned}</p>
          </ProgressRing>
          <p class="t-label-s muted">of {goal.goal} XP today</p>
        </button>
      </div>

      <button class="card row" onClick={() => navigate('/review')}>
        <span class="ico" style={{ width: 40, height: 40, borderRadius: '50%', display: 'grid', placeItems: 'center', background: 'rgba(79,179,232,0.14)', flex: 'none' }}>
          <Icon name="repeat" size={22} color="var(--info)" />
        </span>
        <span class="grow">
          <span class="t-title-s" style={{ display: 'block' }}>Review</span>
          <span class="t-body-s muted">{due > 0 ? `${wordsToReview(due)} due today` : 'Nothing due today'}</span>
        </span>
        <Icon name="arrowRight" size={18} color="var(--faint)" />
      </button>

      <button class="card row" onClick={() => navigate('/achievements')}>
        <span class="ico" style={{ width: 40, height: 40, borderRadius: '50%', display: 'grid', placeItems: 'center', background: 'var(--sunken)', flex: 'none' }}>
          <Icon name="medalStar" size={22} color="var(--gold)" />
        </span>
        <span class="grow">
          <span class="t-title-s" style={{ display: 'block' }}>Badges</span>
          <span class="t-body-s muted">{tiers} of 66 tiers earned</span>
        </span>
        <Icon name="arrowRight" size={18} color="var(--faint)" />
      </button>
    </aside>
  );
}
