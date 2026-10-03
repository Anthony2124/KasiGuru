/**
 * Home: the one thing to do next, and how today is going (ui/screens/home/HomeScreen.kt).
 */
import { STORIES_ENABLED } from '../../domain/constants';
import { useEffect, useMemo } from 'preact/hooks';
import { gameTitle } from '../../domain/gamification';
import { navigate, enc } from '../../lib/router';
import { setPrefs, useApp, useCorpus, useLearner } from '../../lib/store';
import { useInstall } from '../../lib/install';
import { approvedContributions } from '../../lib/remote';
import { act } from '../../lib/store';
import { Avatar, ClayButton, Icon, Jepjep, ProgressRing, SectionHeading } from '../kit';
import { WeekStrip } from '../parts';
import { openGame } from './Practice';
import { activities, buildWeek, continueCard, dayGoal, isStoryUnlocked, jepjepLine, wordOfTheDay, wordsToReview } from '../derive';

const BACKUP_PROMPT_MIN_XP = 150;

export function HomeScreen() {
  const learner = useLearner();
  const corpus = useCorpus();
  const stories = useApp((s) => s.stories);
  const account = useApp((s) => s.account);
  const prefs = useApp((s) => s.prefs);
  const announcements = useApp((s) => s.announcements);
  const install = useInstall();
  const p = learner.progress;

  useEffect(() => {
    document.title = 'KasiGuru';
    // Community Contributor: each approved submission is a one-time 10 XP receipt. Approvals are only
    // visible from here, as on Android.
    if (!account.uid) return;
    approvedContributions().then((list) => {
      const fresh = list?.filter((c) => !learner.receipts[`approved:${c.id}`]) ?? [];
      if (fresh.length) act((d) => d.approved(fresh));
    });
  }, [account.uid]);

  const wordsDue = Math.min(corpus.countScheduledDue(), 20);
  const goal = dayGoal(learner, wordsDue);
  const line = jepjepLine(p, wordsDue, goal.met);
  const card = useMemo(() => continueCard(corpus, learner), [corpus, learner.lessons]);
  const acts = useMemo(() => activities(corpus, learner, stories), [corpus, learner, stories]);
  const week = buildWeek(p);
  const featured = useMemo(() => wordOfTheDay(corpus), [corpus]);
  const lastGame = learner.gameScores[learner.gameScores.length - 1]?.gameType;
  const quickGame = lastGame && ['word_match', 'word_search', 'word_wheel'].includes(lastGame) ? lastGame : null;
  const firstStory = stories.slice().sort((a, b) => a.requiredXp - b.requiredXp).find((s) => isStoryUnlocked(s, learner));
  const displayName = p.fullName || p.userName;
  const showBackup = !prefs.backupPromptDismissed && account.isAnonymous && p.totalXp >= BACKUP_PROMPT_MIN_XP;
  const showInstall = !install.installed && !prefs.installHintDismissed && (install.ios || install.canPrompt);

  const current = acts.find((a) => !a.isDone);
  const fallback = (() => {
    switch (current?.kind) {
      case 'lesson':
        return { label: 'Continue learning', icon: 'play' as const, go: () => current.ref && navigate(`/lesson/${enc(current.ref.unitId)}/${current.ref.lessonIndex}`) };
      case 'game':
        return { label: 'Play a game', icon: 'game' as const, go: () => navigate('/practice') };
      case 'story':
        return { label: 'Read a story', icon: 'book' as const, go: () => navigate('/library?tab=stories') };
      default:
        return { label: 'Review your words', icon: 'repeat' as const, go: () => navigate('/review') };
    }
  })();

  return (
    <main class="page wide">
      <div class="home-grid">
        <div class="stack">
          {/* Hero: who you are, how today stands, and Jepjep's one line. */}
          <section class="card panel glow" style={{ '--gx': '20%', '--gy': '80%', padding: 'var(--s-md)' } as never}>
            <div class="row">
              <Avatar id={p.profileIconId} size={56} level={p.level} onClick={() => navigate('/me')} label={`Your profile, level ${p.level}`} />
              <h1 class="t-headline-s grow" style={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                Magandang aldew, <span class="hl">{displayName}</span>
              </h1>
              <button class="icon-btn boxed" aria-label="Notifications" onClick={() => navigate('/notifications')} style={{ position: 'relative' }}>
                <Icon name="notification" size={22} />
                {announcements.some((a) => !prefs.readAnnouncements.includes(a.id)) && (
                  <span style={{ position: 'absolute', top: 10, right: 11, width: 9, height: 9, borderRadius: '50%', background: 'var(--coral)', border: '2px solid var(--surface)' }} />
                )}
              </button>
            </div>
            <div class="row-xs" style={{ marginTop: 'var(--s-sm)' }}>
              <button class="chip" onClick={() => navigate('/streak')} aria-label={`Streak, ${p.currentStreak} ${p.currentStreak === 1 ? 'day' : 'days'}. Shows what keeps it going.`}>
                <Icon name="flash" size={16} color="var(--coral)" />
                {p.currentStreak === 1 ? '1 day streak' : `${p.currentStreak} day streak`}
              </button>
              <span class="chip" aria-label={`${p.totalXp} XP in total`}>
                <Icon name="star" size={16} color="var(--gold)" />
                {p.totalXp} XP
              </span>
            </div>
            <div class="row" style={{ marginTop: 'var(--s-md)', alignItems: 'center' }}>
              <Jepjep pose={line.pose} height={104} breathe />
              <p class="t-title grow" style={{ background: 'var(--sunken)', border: '1px solid var(--hair)', borderRadius: 'var(--r-tile)', padding: 'var(--s-sm) var(--s-md)' }}>
                {line.text}
              </p>
            </div>
          </section>

          {/* The one action. */}
          {card ? (
            <section class="card panel" style={{ padding: 'var(--s-lg)' }}>
              <h2 class="t-label-l muted" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{card.sectionTitle}</h2>
              <p class="headword" style={{ marginTop: 'var(--s-sm)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{card.heroWord}</p>
              <p class="t-body-l muted" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{card.heroMeaning}</p>
              <p class="t-body-s faint" style={{ marginTop: 'var(--s-xs)' }}>{card.lessonLabel}</p>
              <div style={{ marginTop: 'var(--s-md)' }}>
                <ClayButton label="Continue" icon="play" onClick={() => navigate(`/lesson/${enc(card.ref.unitId)}/${card.ref.lessonIndex}`)} />
              </div>
            </section>
          ) : (
            <ClayButton label={fallback.label} icon={fallback.icon} onClick={fallback.go} />
          )}

          {announcements.map((a) => (
            <div key={a.id} class="banner info">
              <Icon name="notification" size={22} color="var(--info)" />
              <div class="grow">
                <p class="t-title-s">{a.title}</p>
                <p class="t-body muted">{a.message}</p>
              </div>
            </div>
          ))}

          {showInstall && (
            <div class="banner">
              <Icon name="mobile" size={22} color="var(--lime)" />
              <div class="grow stack-sm">
                <p class="t-title-s">Add KasiGuru to your home screen</p>
                <p class="t-body muted">
                  {install.ios ? 'In Safari, tap Share, then Add to Home Screen. It opens full screen and works offline.' : 'Install it to open KasiGuru in its own window, even offline.'}
                </p>
                <div class="row-xs">
                  <button class="text-btn muted" onClick={() => setPrefs({ installHintDismissed: true })}>Not now</button>
                  {install.canPrompt && !install.ios ? (
                    <button class="text-btn lime" onClick={() => void install.prompt()}>Install</button>
                  ) : (
                    <button class="text-btn lime" onClick={() => navigate('/install')}>Show me how</button>
                  )}
                </div>
              </div>
            </div>
          )}

          {showBackup && (
            <div class="banner warn">
              <Icon name="danger" size={22} color="var(--amber)" />
              <div class="grow stack-sm">
                <p class="t-title-s">Secure your progress</p>
                <p class="t-body-s muted">
                  Your XP, streak and badges are saved only in this browser. Add an account so they come back if you clear your browser or change device.
                </p>
                <div class="row-xs">
                  <button class="text-btn muted" onClick={() => setPrefs({ backupPromptDismissed: true })}>Not now</button>
                  <button class="text-btn" onClick={() => navigate('/account')} style={{ fontWeight: 700 }}>Add account</button>
                </div>
              </div>
            </div>
          )}

          {/* Today: the day goal and the review deck. */}
          <div class="grid-2">
            <button class="card center" onClick={() => navigate('/me')} style={{ display: 'grid', justifyItems: 'center', gap: 4 }}>
              <ProgressRing
                value={goal.fraction}
                color={goal.met ? 'var(--green)' : 'var(--gold)'}
                label={`Daily goal: ${goal.earned} of ${goal.goal} XP earned today, ${goal.remainder}`}
              >
                <div>
                  <p class="t-title" style={{ lineHeight: '18px' }}>{goal.earned}</p>
                  <p class="t-label-s muted">XP</p>
                </div>
              </ProgressRing>
              <p class="t-title-s" style={{ marginTop: 'var(--s-xs)' }}>Daily goal</p>
              {goal.met ? (
                <p class="row-xs t-body-s muted" style={{ justifyContent: 'center' }}>
                  <Icon name="tickCircle" size={14} color="var(--green)" /> Goal met
                </p>
              ) : (
                <p class="t-body-s muted">{goal.earned} of {goal.goal} XP</p>
              )}
            </button>
            <button class="card center" onClick={() => navigate('/review')} style={{ display: 'grid', justifyItems: 'center', gap: 4 }}>
              <div style={{ width: 72, height: 72, borderRadius: '50%', background: 'rgba(79,179,232,0.14)', display: 'grid', placeItems: 'center' }}>
                <Icon name="repeat" size={28} color="var(--info)" />
              </div>
              <p class="t-title-s" style={{ marginTop: 'var(--s-xs)' }}>Review</p>
              <p class="t-body-s muted">{wordsDue > 0 ? `${wordsToReview(wordsDue)} due today` : 'Nothing due today'}</p>
            </button>
          </div>
        </div>

        <div class="stack">
          <section>
            <SectionHeading text="Quick practice" />
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 'var(--s-xs)' }}>
              {[
                { label: 'Flashcards', icon: 'repeat' as const, go: () => navigate('/review') },
                { label: quickGame ? gameTitle(quickGame) : 'Games', icon: 'game' as const, go: () => (quickGame ? openGame(quickGame) : navigate('/practice')) },
                // Stories are not narrated yet (STORIES_ENABLED); until they are, the third tile is the dictionary.
                STORIES_ENABLED
                  ? { label: 'Story', icon: 'book' as const, go: () => (firstStory ? navigate(`/story/${firstStory.id}`) : navigate('/library?tab=stories')) }
                  : { label: 'Words', icon: 'book' as const, go: () => navigate('/library') },
              ].map((q) => (
                <button key={q.label} class="card center" onClick={q.go} style={{ display: 'grid', justifyItems: 'center', gap: 6, padding: 'var(--s-sm) var(--s-xs)' }}>
                  <Icon name={q.icon} size={24} color="var(--lime)" />
                  <span class="t-label">{q.label}</span>
                </button>
              ))}
            </div>
          </section>

          {featured && (
            <section>
              <SectionHeading text="Word of the day" />
              <button class="card row" onClick={() => navigate(`/word/${enc(featured.id)}`)} style={{ textAlign: 'left' }}>
                <div class="grow" style={{ minWidth: 0 }}>
                  <p class="headword" style={{ overflowWrap: 'anywhere' }}>{featured.kasiguranin}</p>
                  <p class="t-body muted">{[featured.english, featured.tagalog].filter(Boolean).join(' · ')}</p>
                </div>
                <Jepjep pose="curious" height={84} />
              </button>
            </section>
          )}

          <section>
            <SectionHeading text="This week" />
            <WeekStrip week={week} />
          </section>
        </div>
      </div>
    </main>
  );
}
