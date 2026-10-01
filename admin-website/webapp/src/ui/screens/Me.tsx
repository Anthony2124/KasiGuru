/**
 * Me (ui/screens/profile/ProfileScreen): who you are, how far you have come, your badges, and the
 * way into settings and your account.
 */
import { ACHIEVEMENTS, METRIC, RANKS, rankFor } from '../../domain/gamification';
import { navigate } from '../../lib/router';
import { useApp, useCorpus, useLearner } from '../../lib/store';
import { Avatar, ClayButton, GroundScaffold, Icon, ProgressBar, SectionHeading } from '../kit';
import type { IconName } from '../icons.generated';
import type { LearnerData } from '../../domain/learner';

/** A badge's live progress: the stored value, or the counter it measures if that is higher. */
export function badgeValue(metric: string, stored: number, d: LearnerData): number {
  const p = d.progress;
  const live: Record<string, number> = {
    [METRIC.WORDS_LEARNED]: p.wordsLearned,
    [METRIC.STORIES_COMPLETED]: p.storiesCompleted,
    [METRIC.GAMES_PLAYED]: p.gamesPlayed,
    [METRIC.STREAK]: p.currentStreak,
    [METRIC.LEVEL]: p.level,
    [METRIC.SUBMISSIONS_MADE]: p.submissionsMade,
    [METRIC.GAME_MODES_PLAYED]: new Set(d.gameScores.map((s) => s.gameType)).size,
  };
  return Math.max(stored, live[metric] ?? 0);
}

function LinkRow({ icon, tint, title, subtitle, onClick }: { icon: IconName; tint: string; title: string; subtitle: string; onClick: () => void }) {
  return (
    <button class="list-row" onClick={onClick}>
      <span class="ico" style={{ background: 'var(--sunken)' }}>
        <Icon name={icon} size={20} color={tint} />
      </span>
      <div class="grow">
        <p class="t-title-s">{title}</p>
        <p class="t-body-s muted">{subtitle}</p>
      </div>
      <Icon name="arrowRight" size={18} color="var(--faint)" />
    </button>
  );
}

function Stat({ icon, label, value }: { icon: IconName; label: string; value: string }) {
  return (
    <div class="list-row" style={{ minHeight: 52 }}>
      <Icon name={icon} size={20} color="var(--lime)" />
      <span class="t-body muted grow">{label}</span>
      <span class="t-title">{value}</span>
    </div>
  );
}

export function MeScreen() {
  const learner = useLearner();
  const corpus = useCorpus();
  const account = useApp((s) => s.account);
  const p = learner.progress;
  const displayName = p.fullName || p.userName;
  const rank = rankFor(p.totalXp);
  const next = RANKS.find((r) => r.level > rank.level);
  const mastered = corpus.learnedCount();
  const practised = Object.values(learner.wordStates).filter((s) => s.timesReviewed > 0).length;
  const lessons = Object.entries(learner.lessons).filter(([k, s]) => s.isComplete && !k.startsWith('mastery:')).length;
  const unlocked = ACHIEVEMENTS.filter((a) => learner.achievements[a.id]?.isUnlocked);
  const recent = unlocked
    .slice()
    .sort((a, b) => (learner.achievements[b.id]?.unlockedDate ?? '').localeCompare(learner.achievements[a.id]?.unlockedDate ?? ''))
    .slice(0, 4);
  const closest = ACHIEVEMENTS.filter((a) => !learner.achievements[a.id]?.isUnlocked && a.metricType in { wordsLearned: 1, storiesCompleted: 1, gamesPlayed: 1, streak: 1, level: 1, submissionsMade: 1 })
    .map((a) => ({ a, v: badgeValue(a.metricType, learner.achievements[a.id]?.currentValue ?? 0, learner) }))
    .sort((x, y) => y.v / y.a.requiredValue - x.v / x.a.requiredValue)[0];
  const isGuest = account.isAnonymous;

  return (
    <GroundScaffold
      title={displayName}
      onBack={false}
      nav
      actions={
        <>
          <button class="icon-btn" aria-label="Edit profile" onClick={() => navigate('/edit-profile')}>
            <Icon name="edit" size={22} />
          </button>
          <button class="icon-btn" aria-label="Settings" onClick={() => navigate('/settings')}>
            <Icon name="setting" size={22} />
          </button>
        </>
      }
    >
      <div class="stack-lg">
        <section class="card panel glow center stack-sm" style={{ '--gx': '50%', '--gy': '25%', padding: 'var(--s-lg)' } as never}>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <Avatar id={p.profileIconId} size={104} level={p.level} onClick={() => navigate('/edit-profile')} label="Your avatar. Edit profile" />
          </div>
          <h1 class="t-headline">{displayName}</h1>
          <p class="t-title muted">{rank.title}</p>
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <span class="tag">{p.titleBadge}</span>
          </div>
          <div class="readable" style={{ maxWidth: 320 }}>
            <ProgressBar value={next ? (p.totalXp - rank.minXp) / (next.minXp - rank.minXp) : 1} label="Progress to the next rank" />
          </div>
          <p class="t-body-s faint">{next ? `${Math.max(0, next.minXp - p.totalXp)} XP to ${next.title}` : 'Highest rank reached'}</p>
          <div class="stats3" style={{ marginTop: 'var(--s-sm)' }}>
            <div><p class="t-headline-s">{p.currentStreak}</p><p class="t-label-s muted">day streak</p></div>
            <div><p class="t-headline-s">{p.totalXp}</p><p class="t-label-s muted">total XP</p></div>
            <div><p class="t-headline-s">{practised}</p><p class="t-label-s muted">words practised</p></div>
          </div>
        </section>

        {isGuest && (
          <section class="card stack-sm" style={{ borderColor: 'rgba(245,200,106,.4)' }}>
            <p class="t-title">Sign in so you don't lose your progress</p>
            <p class="t-body muted">Right now your XP, streak and badges live only in this browser.</p>
            <ClayButton label="Sign in" onClick={() => navigate('/account')} />
          </section>
        )}

        <section class="stack-sm">
          <SectionHeading
            text="Badges"
            action={
              <button class="text-btn lime" style={{ fontSize: 13, fontFamily: 'var(--body)', fontWeight: 700 }} onClick={() => navigate('/achievements')} aria-label="See all badges">
                See all
              </button>
            }
          />
          <button class="card" onClick={() => navigate('/achievements')}>
            <p class="t-body muted">{unlocked.length} of {ACHIEVEMENTS.length} earned</p>
            {recent.length > 0 ? (
              <div class="row wrap" style={{ marginTop: 'var(--s-sm)' }}>
                {recent.map((a) => (
                  <span key={a.id} class="chip" style={{ background: 'var(--sunken)' }}>
                    <Icon name="medalStar" size={16} color="var(--gold)" /> {a.name}
                  </span>
                ))}
              </div>
            ) : (
              <p class="t-body" style={{ marginTop: 8 }}>No badges yet. Finish a lesson to earn your first.</p>
            )}
            {closest && (
              <div style={{ marginTop: 'var(--s-sm)' }}>
                <p class="t-label muted">Closest: {closest.a.name}</p>
                <div class="row" style={{ marginTop: 6 }}>
                  <div class="grow">
                    <ProgressBar value={closest.v / closest.a.requiredValue} color="var(--gold)" />
                  </div>
                  <span class="t-label-s muted">{Math.min(closest.v, closest.a.requiredValue)} of {closest.a.requiredValue}</span>
                </div>
              </div>
            )}
          </button>
        </section>

        <section class="stack-sm">
          <SectionHeading text="Learning overview" />
          <div class="list">
            <Stat icon="book" label="Words mastered" value={`${mastered}`} />
            <Stat icon="medal" label="Longest streak" value={`${p.longestStreak} ${p.longestStreak === 1 ? 'day' : 'days'}`} />
            <Stat icon="teacher" label="Lessons completed" value={`${lessons}`} />
            <Stat icon="game" label="Games played" value={`${p.gamesPlayed}`} />
            <Stat icon="document" label="Stories read" value={`${p.storiesCompleted}`} />
            <Stat
              icon="tickCircle"
              label="Accuracy"
              value={p.totalQuestionsAnswered === 0 ? 'Not measured yet' : `${Math.round((p.totalCorrectAnswers / p.totalQuestionsAnswered) * 100)}%`}
            />
          </div>
        </section>

        <section class="stack-sm">
          <SectionHeading text="Settings and account" />
          <div class="list">
            <LinkRow icon="setting" tint="var(--lime)" title="Settings" subtitle="Sound, sync and more" onClick={() => navigate('/settings')} />
            <LinkRow
              icon="profile"
              tint={isGuest ? 'var(--amber)' : 'var(--lime)'}
              title="Account"
              subtitle={isGuest ? 'Guest - progress is only in this browser' : p.email || account.email || 'Signed in'}
              onClick={() => navigate('/account')}
            />
            <LinkRow icon="edit" tint="var(--lime)" title="Edit profile" subtitle="Name, avatar and details" onClick={() => navigate('/edit-profile')} />
            <LinkRow icon="cup" tint="var(--gold)" title="Leaderboard" subtitle="Learners of Kasiguranin, ranked" onClick={() => navigate('/leaderboard')} />
          </div>
        </section>

        <section class="stack-sm">
          <SectionHeading text="Explore" />
          <div class="list">
            <LinkRow icon="teacher" tint="var(--info)" title="How to use KasiGuru" subtitle="A short guide to every tab" onClick={() => navigate('/help')} />
            <LinkRow icon="mobile" tint="var(--lime)" title="Install the app" subtitle="Add KasiGuru to your home screen" onClick={() => navigate('/install')} />
            <LinkRow icon="addCircle" tint="var(--lime)" title="Contribute a word" subtitle="Send a missing Kasiguranin word for review" onClick={() => navigate('/submit-word')} />
            <LinkRow icon="edit" tint="var(--coral)" title="Share a story or poem" subtitle="Submit literature for the Library" onClick={() => navigate('/submit-literature')} />
            <LinkRow icon="infoCircle" tint="var(--gold)" title="About KasiGuru" subtitle="The project, the language, the mission" onClick={() => navigate('/about')} />
          </div>
        </section>
      </div>
    </GroundScaffold>
  );
}
