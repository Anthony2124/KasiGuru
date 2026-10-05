/**
 * Me (ui/screens/profile/ProfileScreen): your scenery and avatar, level, streak and weekly rank,
 * every badge with the pinned ones first, and the way into settings and your account.
 */
import { useEffect, useMemo, useState } from 'preact/hooks';
import { BADGE_ROWS, BADGE_TIERS, badgeProgress, badgeSummaries, nextUpBadge, type BadgeFamilySummary } from '../../domain/badges';
import { levelProgress, levelTitle, xpToNextLevel, STORIES_ENABLED } from '../../domain/constants';
import { pinnedFamilies } from '../../domain/learner';
import { weeklyRank } from '../../lib/remote';
import { navigate } from '../../lib/router';
import { act, useApp, useCorpus, useLearner } from '../../lib/store';
import { BackgroundPicker, BadgeMedal } from '../badges';
import { Avatar, GroundScaffold, Icon, ProgressBar, SectionHeading, sceneUrl, toast, type SceneId } from '../kit';
import type { IconName } from '../icons.generated';

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

/** One badge family on the grid: its medal at the highest tier, the name, and six dots for its tiers. */
function BadgeCell({ summary }: { summary: BadgeFamilySummary }) {
  const tierText = summary.highest?.label ?? 'Locked';
  return (
    <button
      class="badge-cell"
      onClick={() => navigate('/achievements')}
      aria-label={`${summary.family.name}, ${tierText}, ${summary.earnedTiers} of ${BADGE_TIERS.length} tiers`}
    >
      <BadgeMedal tier={summary.highest} earned={!!summary.highest} size={52} />
      <span class={`t-label-s name${summary.highest ? '' : ' muted'}`}>{summary.family.name}</span>
      <span class="tier-dots" aria-hidden="true">
        {BADGE_TIERS.map((t) => (
          <i key={t.index} class={t.index < summary.earnedTiers ? 'on' : undefined} />
        ))}
      </span>
      <span class="t-label-s" style={{ color: summary.highest ? 'var(--lime)' : 'var(--faint)' }}>{tierText}</span>
    </button>
  );
}

export function MeScreen() {
  const learner = useLearner();
  const corpus = useCorpus();
  const account = useApp((s) => s.account);
  const [picking, setPicking] = useState(false);
  const [overview, setOverview] = useState(false);
  const [rank, setRank] = useState<number | null>(null);
  const p = learner.progress;
  const displayName = p.fullName || p.userName;
  const toNext = xpToNextLevel(p.totalXp);
  const mastered = corpus.learnedCount();
  const practised = Object.values(learner.wordStates).filter((s) => s.timesReviewed > 0).length;
  const lessons = Object.entries(learner.lessons).filter(([k, s]) => s.isComplete && !k.startsWith('mastery:')).length;
  const isGuest = account.isAnonymous;

  const unlocked = BADGE_ROWS.filter((r) => learner.achievements[r.id]?.isUnlocked).length;
  const families = useMemo(() => badgeSummaries(learner.achievements, pinnedFamilies(p)), [learner.achievements, p.pinnedBadgeIds]);
  // Story Reader cannot move while stories are switched off, so it is never the badge to work on next.
  const nextUp = nextUpBadge(STORIES_ENABLED ? families : families.filter((s) => s.family.id !== 'story_reader'));

  useEffect(() => {
    if (!account.uid || isGuest) return;
    weeklyRank(account.uid)
      .then(setRank)
      .catch(() => setRank(null));
  }, [account.uid, isGuest]);

  const selectBackground = (id: string) => {
    try {
      act((d) => d.selectBackground(id), { celebrate: false });
      setPicking(false);
    } catch (e) {
      toast(e instanceof Error ? e.message : "Couldn't change your background.");
    }
  };

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
      {picking && <BackgroundPicker progress={p} onSelect={selectBackground} onClose={() => setPicking(false)} />}
      <div class="cols me">
        <div class="stack-lg sticky-col">
        <section class="card panel center" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ position: 'relative', height: 176 }}>
            <img src={sceneUrl((p.profileBackgroundId || 'forest') as SceneId)} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,.25)' }} />
            <button class="text-btn" style={{ position: 'absolute', top: 8, right: 8, color: '#fff' }} onClick={() => setPicking(true)}>
              Change background
            </button>
          </div>
          <div class="stack-sm" style={{ padding: '0 var(--s-lg) var(--s-lg)', marginTop: -44 }}>
            <div style={{ display: 'grid', placeItems: 'center' }}>
              <Avatar id={p.profileIconId} size={96} level={p.level} onClick={() => navigate('/edit-profile')} label="Your avatar. Edit profile" />
            </div>
            <h1 class="t-headline">{displayName}</h1>
            <p class="t-title" style={{ color: 'var(--lime)' }}>{levelTitle(p.level)}</p>
            <div style={{ display: 'grid', placeItems: 'center' }}>
              <span class="tag">{p.titleBadge}</span>
            </div>
            <div class="readable" style={{ maxWidth: 320 }}>
              <ProgressBar value={levelProgress(p.totalXp)} label="Progress to the next level" />
            </div>
            <p class="t-body-s faint">{toNext == null ? 'Highest level reached' : `${toNext} XP to Level ${p.level + 1}`}</p>
            <div class="stats3" style={{ marginTop: 'var(--s-sm)' }}>
              <button onClick={() => navigate('/streak')} aria-label={`${p.currentStreak} day streak. Open your streak`} style={{ background: 'none', border: 0, color: 'inherit' }}>
                <p class="t-headline-s">{p.currentStreak}</p>
                <p class="t-label muted">day streak</p>
              </button>
              <button onClick={() => navigate('/xp')} aria-label={`${p.totalXp} XP in total. Open your XP`} style={{ background: 'none', border: 0, color: 'inherit' }}>
                <p class="t-headline-s">{p.totalXp}</p>
                <p class="t-label muted">total XP</p>
              </button>
              <div><p class="t-headline-s">{practised}</p><p class="t-label muted">words practised</p></div>
            </div>
            <button class="text-btn lime" onClick={() => navigate('/leaderboard')}>
              {rank ? `Your rank: #${rank} this week` : 'View leaderboard'}
            </button>
          </div>
        </section>

        {isGuest && (
          <section class="card row" style={{ padding: 'var(--s-sm) var(--s-md)' }}>
            <Icon name="lock" size={22} color="var(--lime)" />
            <p class="t-body-s grow">Save your progress with an account</p>
            <button class="text-btn lime" onClick={() => navigate('/account')}>Sign in</button>
          </section>
        )}
        </div>

        <div class="stack-lg">

        <section class="stack-sm">
          <SectionHeading
            text="Badges"
            action={
              <button class="text-btn lime" style={{ fontSize: 13, fontFamily: 'var(--body)', fontWeight: 700 }} onClick={() => navigate('/achievements')} aria-label="See all badges">
                See all
              </button>
            }
          />
          <p class="t-body-s faint">{unlocked === 0 ? 'No badges yet. Finish a lesson to earn your first.' : `${unlocked} of ${BADGE_ROWS.length} tiers earned`}</p>
          {nextUp?.next && (
            <button class="card" onClick={() => navigate('/achievements')}>
              <p class="t-label-s" style={{ color: 'var(--lime)' }}>NEXT UP</p>
              <p class="t-title-s" style={{ marginTop: 'var(--s-xxs)' }}>
                {nextUp.family.name} · {nextUp.next.row.tier.label}
              </p>
              <div style={{ marginTop: 'var(--s-xs)' }}>
                <ProgressBar value={nextUp.progress} height={6} label={`Progress to ${nextUp.family.name} ${nextUp.next.row.tier.label}`} />
              </div>
              <p class="t-label-s faint" style={{ marginTop: 'var(--s-xxs)' }}>
                {badgeProgress(nextUp.family, Math.min(nextUp.next.currentValue, nextUp.next.row.requiredValue), nextUp.next.row.requiredValue)}
              </p>
            </button>
          )}
          <div class="badge-grid" style={{ paddingTop: 'var(--s-xs)' }}>
            {families.map((s) => (
              <BadgeCell key={s.family.id} summary={s} />
            ))}
          </div>
        </section>

        <section class="stack-sm">
          <button class="text-btn lime" style={{ alignSelf: 'flex-start' }} aria-expanded={overview} onClick={() => setOverview(!overview)}>
            {overview ? 'Hide learning overview' : 'Learning overview'}
          </button>
          {overview && (
            <div class="list">
              <Stat icon="book" label="Words mastered" value={`${mastered}`} />
              <Stat icon="medal" label="Longest streak" value={`${p.longestStreak} ${p.longestStreak === 1 ? 'day' : 'days'}`} />
              <Stat icon="teacher" label="Lessons completed" value={`${lessons}`} />
              <Stat icon="game" label="Games played" value={`${p.gamesPlayed}`} />
              {STORIES_ENABLED && <Stat icon="document" label="Stories read" value={`${p.storiesCompleted}`} />}
              <Stat
                icon="tickCircle"
                label="Accuracy"
                value={p.totalQuestionsAnswered === 0 ? 'Not measured yet' : `${Math.round((p.totalCorrectAnswers / p.totalQuestionsAnswered) * 100)}%`}
              />
            </div>
          )}
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
            <LinkRow icon="cup" tint="var(--gold)" title="Leaderboard" subtitle="This week, all-time and streaks" onClick={() => navigate('/leaderboard')} />
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
      </div>
    </GroundScaffold>
  );
}
