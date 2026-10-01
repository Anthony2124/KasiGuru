/**
 * The navigation shell: KasiGuruNavGraph for the browser. Five tabs behind a floating bar, pushed
 * screens on top, the app-wide celebrations (level up, streak), and the suspension gate.
 */
import { useEffect } from 'preact/hooks';
import { dismissCelebration, useApp } from './lib/store';
import { match, navigate, switchTab, useLocation } from './lib/router';
import { ClayButton, Confetti, Dialog, Icon, Loading, ToastHost } from './ui/kit';
import { RewardDialog } from './ui/badges';
import type { IconName } from './ui/icons.generated';
import { HomeScreen } from './ui/screens/Home';
import { LearnScreen } from './ui/screens/Learn';
import { LessonScreen } from './ui/screens/Lesson';
import { PracticeScreen } from './ui/screens/Practice';
import { LibraryScreen } from './ui/screens/Library';
import { MeScreen } from './ui/screens/Me';
import { OnboardingScreen } from './ui/screens/Onboarding';
import { ReviewScreen } from './ui/screens/Review';
import { WordDetailScreen, CategoryScreen } from './ui/screens/Words';
import { StoryReaderScreen } from './ui/screens/Stories';
import { LevelSelectScreen, WordSearchCategoriesScreen } from './ui/games/Levels';
import { WordMatchGame } from './ui/games/WordMatch';
import { WordSearchGame } from './ui/games/WordSearch';
import { WordWheelGame } from './ui/games/WordWheel';
import { LeaderboardScreen } from './ui/screens/Leaderboard';
import { AchievementsScreen } from './ui/screens/Achievements';
import { SettingsScreen } from './ui/screens/Settings';
import { AccountScreen } from './ui/screens/Account';
import { EditProfileScreen } from './ui/screens/EditProfile';
import { AboutScreen, HelpScreen, InstallScreen } from './ui/screens/About';
import { SubmitWordScreen } from './ui/screens/SubmitWord';
import { SubmitLiteratureScreen } from './ui/screens/SubmitLiterature';
import { ReportIssueScreen } from './ui/screens/ReportIssue';
import { NotificationsScreen } from './ui/screens/Notifications';
import { SuspendedScreen } from './ui/screens/Suspended';
import { StoryListScreen } from './ui/screens/StoryList';
import { StreakScreen } from './ui/screens/Streak';
import { PublicProfileScreen } from './ui/screens/PublicProfile';

const TABS: { path: string; label: string; icon: IconName }[] = [
  { path: '/', label: 'Home', icon: 'home' },
  { path: '/learn', label: 'Learn', icon: 'teacher' },
  { path: '/practice', label: 'Practice', icon: 'game' },
  { path: '/library', label: 'Library', icon: 'book' },
  { path: '/me', label: 'Me', icon: 'profile' },
];

function BottomNav({ path }: { path: string }) {
  return (
    <nav class="bottom-nav" aria-label="Main">
      <div class="bar">
        {TABS.map((t) => (
          <button key={t.path} class="tab" aria-current={path === t.path ? 'page' : undefined} onClick={() => switchTab(t.path)}>
            <Icon name={t.icon} size={22} />
            <span>{t.label}</span>
          </button>
        ))}
      </div>
    </nav>
  );
}

type Route = { pattern: string; render: (p: Record<string, string>, q: URLSearchParams) => preact.JSX.Element };

const ROUTES: Route[] = [
  { pattern: '/', render: () => <HomeScreen /> },
  { pattern: '/learn', render: () => <LearnScreen /> },
  { pattern: '/practice', render: () => <PracticeScreen /> },
  { pattern: '/library', render: (_, q) => <LibraryScreen tab={q.get('tab') === 'stories' ? 'stories' : 'words'} /> },
  { pattern: '/me', render: () => <MeScreen /> },
  { pattern: '/onboarding', render: () => <OnboardingScreen /> },
  { pattern: '/lesson/:unitId/:index', render: (p) => <LessonScreen unitId={p.unitId} lessonIndex={Number(p.index) || 0} /> },
  { pattern: '/review', render: () => <ReviewScreen /> },
  { pattern: '/games/word_search', render: () => <WordSearchCategoriesScreen /> },
  { pattern: '/games/levels/:gameType', render: (p) => <LevelSelectScreen gameType={p.gameType} /> },
  { pattern: '/games/word_match/:level', render: (p) => <WordMatchGame level={Number(p.level) || 1} /> },
  { pattern: '/games/word_wheel/:level', render: (p) => <WordWheelGame level={Number(p.level) || 1} /> },
  { pattern: '/games/word_search/:key/:level', render: (p) => <WordSearchGame levelKey={p.key} level={Number(p.level) || 1} /> },
  { pattern: '/leaderboard', render: () => <LeaderboardScreen /> },
  { pattern: '/player/:uid', render: (p) => <PublicProfileScreen uid={p.uid} /> },
  { pattern: '/streak', render: () => <StreakScreen /> },
  { pattern: '/word/:id', render: (p) => <WordDetailScreen id={p.id} /> },
  { pattern: '/category/:name', render: (p) => <CategoryScreen category={p.name} /> },
  { pattern: '/stories', render: () => <StoryListScreen /> },
  { pattern: '/story/:id', render: (p) => <StoryReaderScreen id={Number(p.id)} /> },
  { pattern: '/achievements', render: () => <AchievementsScreen /> },
  { pattern: '/settings', render: () => <SettingsScreen /> },
  { pattern: '/account', render: () => <AccountScreen /> },
  { pattern: '/edit-profile', render: () => <EditProfileScreen /> },
  { pattern: '/about', render: () => <AboutScreen /> },
  { pattern: '/help', render: () => <HelpScreen /> },
  { pattern: '/install', render: () => <InstallScreen /> },
  { pattern: '/submit-word', render: () => <SubmitWordScreen /> },
  { pattern: '/submit-literature', render: () => <SubmitLiteratureScreen /> },
  { pattern: '/report', render: (_, q) => <ReportIssueScreen category={q.get('category')} word={q.get('word')} screenContext={q.get('screen')} /> },
  { pattern: '/notifications', render: () => <NotificationsScreen /> },
];

/**
 * Reward and streak moments, one at a time, in the order they happened. A level reached or a badge
 * tier earned is one reward celebration (RewardCelebrationDialog); the streak dialog waits behind it.
 */
function Celebrations() {
  const next = useApp((s) => s.celebrations[0]);
  if (!next) return null;
  if (next.type === 'reward') return <RewardDialog event={next} onClose={dismissCelebration} />;

  // Streak activated: the day's quota (review + three games) was met.
  return (
    <>
      <Confetti pieces={40} />
      <Dialog glow label="Streak activated" onClose={dismissCelebration}>
        <div class="stack center">
          <div style={{ display: 'grid', placeItems: 'center' }}>
            <div class="pop" style={{ width: 96, height: 96, borderRadius: '50%', background: 'var(--coral)', display: 'grid', placeItems: 'center' }}>
              <Icon name="flash" size={52} color="var(--reward-ink)" />
            </div>
          </div>
          <p class="t-label" style={{ color: 'var(--coral)' }}>Streak activated</p>
          <h2 class="t-display">{next.days} day streak!</h2>
          <p class="t-body-l muted">You completed all daily goals. Your learning momentum is on fire today.</p>
          <div class="list" style={{ textAlign: 'left' }}>
            <div class="list-row">
              <Icon name="tickCircle" color="var(--lime)" />
              <span class="t-body">Completed review words</span>
            </div>
            <div class="list-row">
              <Icon name="tickCircle" color="var(--lime)" />
              <span class="t-body">Played 3 mini-game levels</span>
            </div>
          </div>
          <ClayButton label="Keep the flame lit" tone="coral" onClick={dismissCelebration} />
          <button
            class="text-btn lime"
            onClick={() => {
              dismissCelebration();
              navigate('/streak');
            }}
          >
            See your streak
          </button>
        </div>
      </Dialog>
    </>
  );
}

export function App() {
  const { path, query } = useLocation();
  const ready = useApp((s) => s.learnerLoaded && s.contentReady);
  const onboarded = useApp((s) => s.learner.progress.isOnboardingCompleted);
  const ban = useApp((s) => s.ban);
  const online = useApp((s) => s.online);

  // The splash decision: a learner who has not finished onboarding starts there, the way
  // SplashViewModel routes on Android. Account and Install stay reachable so an existing Android
  // learner can sign in straight from the welcome screen.
  useEffect(() => {
    if (!ready) return;
    const allowed = ['/onboarding', '/account', '/install', '/about'];
    if (!onboarded && !allowed.includes(path)) navigate('/onboarding', { replace: true });
    if (onboarded && path === '/onboarding') navigate('/', { replace: true });
  }, [ready, onboarded, path]);

  if (!ready) {
    return (
      <div class="app" style={{ display: 'grid', placeItems: 'center' }}>
        <Loading label="Getting KasiGuru ready" />
      </div>
    );
  }

  if (ban?.isBanned) return <SuspendedScreen />;

  // Rendered straight away rather than after the redirect effect, so no tab screen (and none of its
  // cloud reads) ever mounts for a learner who has not finished onboarding.
  if (!onboarded && !['/onboarding', '/account', '/install', '/about'].includes(path)) {
    return (
      <div class="app">
        <OnboardingScreen />
        <ToastHost />
      </div>
    );
  }

  let screen: preact.JSX.Element | null = null;
  for (const r of ROUTES) {
    const params = match(r.pattern, path);
    if (params) {
      screen = r.render(params, query);
      break;
    }
  }
  if (!screen) screen = <HomeScreen />;
  const isTab = TABS.some((t) => t.path === path) && onboarded;

  return (
    <div class="app">
      {!online && <div class="offline-pill">Offline — progress is saved on this device</div>}
      {screen}
      {isTab && <BottomNav path={path} />}
      <Celebrations />
      <ToastHost />
    </div>
  );
}
