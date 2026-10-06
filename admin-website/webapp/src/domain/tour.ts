/**
 * The guided tour, in chapters (ui/tour on Android: TourChapter.kt, TourStops.kt,
 * TourChapterContent.kt, TourProgress.kt).
 *
 * One chapter runs on its own: Core, once, when a learner finishes onboarding. Every other chapter is
 * optional and started from Settings or the help page. Explaining every control on every screen is
 * fifty-odd stops, and nobody finishes a fifty-step tour; chapters cover everything while only ever
 * asking for ninety seconds.
 *
 * The stops follow Android's, adjusted where the web app differs: no notification filters or
 * reminders, no swipe on the review deck, a browser in place of a phone. The copy carries no counts,
 * because the corpus and the badge list grow.
 */
import { STORIES_ENABLED } from './constants';

export type TourChapterId = 'Core' | 'Dictionary' | 'Contribute' | 'Lessons' | 'Progress' | 'ProfileSettings' | 'Inbox' | 'Flashcards' | 'Social';

export const TOUR_CHAPTER_IDS: TourChapterId[] = ['Core', 'Dictionary', 'Contribute', 'Lessons', 'Progress', 'ProfileSettings', 'Inbox', 'Flashcards', 'Social'];

/** An element a stop cuts its spotlight around, marked in the page with `data-tour`. */
export type TourAnchor =
  | 'ContinueAction' | 'DailyGoalRing' | 'NotificationBell' | 'StreakBadge'
  | 'NavHome' | 'NavLearn' | 'NavPractice' | 'NavLibrary' | 'NavMe'
  | 'DictWordOfDay' | 'DictSubmitBanner' | 'SubmitWordField' | 'SubmitButton'
  | 'PracticeStats' | 'PracticeFeatured' | 'PracticeLeaderboard' | 'LeaderboardPlayer'
  | 'ProgressBadgePanel' | 'ProgressFilter'
  | 'ProfileBackground' | 'ProfileExplore' | 'ProfileSettingsIcon'
  | 'SettingsAppearance' | 'SettingsAccount' | 'SettingsPreferences' | 'SettingsReplayTutorial'
  | 'StoryShelf' | 'FlashcardCard' | 'StreakWeek';

/**
 * One stop. The tour navigates to [route] first, so the caption always describes something the learner
 * can see behind the dim. A null [anchor] explains the screen as a whole: it dims without a hole.
 */
export interface TourStop {
  route: string;
  anchor: TourAnchor | null;
  title: string;
  body: string;
  gesture?: 'tap';
}

/** [version] is bumped when a chapter's content materially changes, so the help page can mark it Updated. */
export interface TourChapter {
  id: TourChapterId;
  title: string;
  subtitle: string;
  version: number;
  stops: TourStop[];
}

const stop = (route: string, anchor: TourAnchor | null, title: string, body: string, gesture?: 'tap'): TourStop => ({ route, anchor, title, body, gesture });

export const coreChapter: TourChapter = {
  id: 'Core',
  title: 'Getting around',
  subtitle: 'The five places, and the one button that always knows what is next',
  version: 2,
  stops: [
    stop('/', 'ContinueAction', 'Start here', 'This button always names what comes next — a lesson, a review, a game or a story. When you do not know where to begin, begin here.'),
    stop('/', 'DailyGoalRing', "Today's goal", "How much of today's target you have done. You chose the target during setup."),
    stop('/', 'NavHome', 'Home', 'Where every day starts: the next thing to do, your goal, the words due for review and a shelf of stories.'),
    stop('/learn', 'NavLearn', 'Learn', 'The whole path, section by section, from greetings to everyday talk. Jepjep waits at the lesson that is yours to take next.'),
    stop('/practice', 'NavPractice', 'Practice', 'Games that drill the words you have already met, and the leaderboard to see where you rank. Three games are open now, and more are coming soon.'),
    stop(
      '/library',
      'NavLibrary',
      'Library',
      STORIES_ENABLED
        ? 'The whole dictionary with pronunciation where a recording is available, and the folk tales beside it. You can add a word or share a story from here.'
        : 'The whole dictionary, with pronunciation where a recording is available, and a way to add any word that is missing. Folk stories are coming soon.'
    ),
    stop('/me', 'NavMe', 'Me', 'Who you are and what you have earned — your badges, and the way through to Settings.'),
    stop('/', 'NotificationBell', 'News from the team', 'Announcements from the KasiGuru team arrive here. A dot on the bell means there is one you have not read.'),
    stop('/', 'StreakBadge', 'Come back tomorrow', 'This is your streak. Practise a little every day and it keeps growing. You can take this tour again any time from Settings.'),
  ],
};

const dictionaryChapter: TourChapter = {
  id: 'Dictionary',
  title: 'The dictionary',
  subtitle: 'Finding a word, hearing it, and adding one that is missing',
  version: 2,
  stops: [
    stop('/library', 'DictWordOfDay', 'A word each day', 'One word is offered every day. Tap it for the whole entry, or the speaker to hear how it is said.'),
    stop('/library', null, 'Categories, and search', 'Words are grouped by what they are about — the body, the house, the weather. The search field at the top looks through every entry and every group at once. You will find all of this under Words in the Library tab.'),
    stop('/library', 'DictSubmitBanner', 'A word we are missing', 'The dictionary is not finished, and it is not meant to be. If you know a word that is not here, Add a word is where it starts its way in.'),
  ],
};

const contributeChapter: TourChapter = {
  id: 'Contribute',
  title: 'Adding a word',
  subtitle: 'What we need from you, and what happens after you send it',
  version: 1,
  stops: [
    stop('/submit-word', 'SubmitWordField', 'Start with the Kasiguranin', 'Type the word here. As you type, the app checks the dictionary and tells you if it is already there — including spellings that differ only by an accent.'),
    stop('/submit-word', null, 'Say what it means', 'Tagalog or English, whichever you are sure of — you do not need both. Everything below that is welcome and optional.'),
    stop('/submit-word', 'SubmitButton', 'Then it goes for review', 'A moderator checks every submission before it joins the dictionary, so nothing enters the record unchecked. You will see it appear once it is approved.'),
  ],
};

const lessonsChapter: TourChapter = {
  id: 'Lessons',
  title: 'Lessons and practice',
  subtitle: 'How the app works out what to show you next',
  version: 2,
  stops: [
    stop('/practice', 'PracticeStats', 'What you have earned', 'Experience, stars and accuracy across every game you have played. Stars are the ones that matter here — they are what opens the rest.'),
    stop('/practice', 'PracticeFeatured', 'Where to pick up', 'The game you have the most room to improve at, chosen for you. Ignore it freely — it is a suggestion, not an instruction.'),
    stop('/practice', null, 'More games are coming', 'Word Match, Word Search and Word Wheel are open now. The tiles marked Coming soon are games still being finished, and they open in a later update.'),
    stop('/', 'ContinueAction', 'Or just start here', 'This button on Home works out what you are due and takes you straight into it, so you never have to plan a session yourself. The full path of lessons is on Learn.'),
  ],
};

const progressChapter: TourChapter = {
  id: 'Progress',
  title: 'Progress and badges',
  subtitle: 'What gets counted, and what each badge is waiting for',
  version: 2,
  stops: [
    stop('/achievements', 'ProgressBadgePanel', 'Badges earned', 'This counts what you have unlocked against everything there is to unlock. It will read low for a while, and that is the point — none of it is given.'),
    stop('/achievements', 'ProgressFilter', 'Sorted by how you earn them', 'Some badges come from levelling up, some from words, streaks, games, stories or helping the dictionary grow. This narrows the wall to one kind at a time.'),
    stop('/achievements', null, 'A locked badge still tells you something', 'Every badge you have not earned shows what it is waiting for, and how far along you already are. None of them is a mystery box. Me shows every badge at its highest tier.'),
  ],
};

const profileSettingsChapter: TourChapter = {
  id: 'ProfileSettings',
  title: 'Me and settings',
  subtitle: 'Your record, and the switches that change how the app behaves',
  version: 3,
  stops: [
    stop('/me', 'ProfileBackground', 'Choose your place', 'Forest and Casapsapan are yours from the start. Change your background here; more places unlock as your level and streak grow.'),
    stop('/settings', 'SettingsAppearance', 'Make reading comfortable', "Choose System, Light or Dark here. System follows your device. For larger text, use your browser's own text size or zoom: the app follows it."),
    stop('/me', 'ProfileExplore', 'Everything else lives here', 'This guide, installing the app, adding a word or a story, and what the project is. Me is the way through to all of it.'),
    stop('/me', 'ProfileSettingsIcon', 'Settings', 'Sound, appearance and your account all sit behind this one icon.'),
    stop('/settings', 'SettingsAccount', 'Keep your progress safe', 'As a guest, your progress lives only in this browser. Signing in with an email or a Google account is what lets you get it back — here, on another device, or in the Android app.'),
    stop('/settings', 'SettingsPreferences', 'Sound and music', "Turn sound effects, tap sounds and music on or off here, and set how loud they play. Vocabulary audio stays available from each word's speaker button."),
    stop('/settings', 'SettingsReplayTutorial', 'And you can always come back', 'This row starts the short tour again. The longer chapters are just below it and on the help page in Me, and none of them ever expire.'),
  ],
};

const inboxChapter: TourChapter = {
  id: 'Inbox',
  title: 'Notifications and stories',
  subtitle: 'Where news arrives, and where the folk tales live',
  version: 2,
  stops: [
    stop('/notifications', null, 'This is where they land', 'Announcements from the KasiGuru team arrive here. Nothing here yet means nothing has been sent, not that anything is broken.'),
    ...(STORIES_ENABLED
      ? [
          stop('/stories', 'StoryShelf', 'Folk tales, in three languages', 'Every story is told in Kasiguranin, Tagalog and English side by side. Tap any word as you read to look it up without losing your place. They live under Stories in the Library tab.'),
          stop('/stories', null, 'Know one we do not have?', 'The Share a story or poem button sends one in for review, the same way a missing word does. A moderator reads it before it joins the shelf.'),
        ]
      : [
          stop('/stories', null, 'Stories are coming soon', 'Folk tales from Casiguran are on their way, read aloud in Kasiguranin. Until then, every word in the dictionary is under Words in the Library tab.'),
        ]),
  ],
};

const flashcardsChapter: TourChapter = {
  id: 'Flashcards',
  title: 'Flashcards and streaks',
  subtitle: 'Opening, flipping, rating, and keeping your week going',
  version: 1,
  stops: [
    stop('/review', 'FlashcardCard', 'Open a word', 'Tap the card to turn it over and see the meaning. If nothing is due today, you can practise ahead.', 'tap'),
    stop('/review', 'FlashcardCard', 'Rate what you remembered', 'On the answer side, choose Again, Hard, Good or Easy. Your answer decides when the word comes back.'),
    stop('/streak', 'StreakWeek', 'Keep the week growing', "This week shows the days that counted. Finish your review and today's games to keep your streak and earn its next badge."),
  ],
};

const socialChapter: TourChapter = {
  id: 'Social',
  title: 'Rankings and players',
  subtitle: "Compare this week and visit a learner's public profile",
  version: 1,
  stops: [
    stop('/practice', 'PracticeLeaderboard', 'Rankings in Practice', "Switch from Games to Leaderboard to see this week's activity, all-time XP, or streaks. Your row stays within reach."),
    stop('/leaderboard', 'LeaderboardPlayer', 'Meet a learner', 'Tap a player to see their badges, scenery and learning path. Only the display name and game stats are public; personal details stay private.', 'tap'),
  ],
};

/** Every chapter, in the order the help page lists them. */
export const TOUR_CHAPTERS: TourChapter[] = [
  coreChapter,
  dictionaryChapter,
  contributeChapter,
  lessonsChapter,
  progressChapter,
  profileSettingsChapter,
  inboxChapter,
  flashcardsChapter,
  socialChapter,
];

export const chapterById = (id: string): TourChapter | undefined => TOUR_CHAPTERS.find((c) => c.id === id);

/** Highest chapter version shipped; stamped as the baseline on an install that was already set up. */
export const CURRENT_TUTORIAL_VERSION = Math.max(...TOUR_CHAPTERS.map((c) => c.version));

// ── Progress (TourProgress.kt) ────────────────────────────────────────────────

/**
 * A completed chapter is recorded as "Dictionary:2". The version travels inside the stamp, so a release
 * that revises one chapter brings exactly that one back as Updated. Stale stamps are never pruned.
 */
export const stamp = (chapter: Pick<TourChapter, 'id' | 'version'>) => `${chapter.id}:${chapter.version}`;

/** Null rather than throwing: a stamp from a build that has since renamed a chapter is data, not a crash. */
export function parseStamp(raw: string): { id: TourChapterId; version: number } | null {
  const at = raw.indexOf(':');
  if (at <= 0) return null;
  const id = raw.slice(0, at);
  const rest = raw.slice(at + 1);
  if (!/^\d+$/.test(rest) || !(TOUR_CHAPTER_IDS as string[]).includes(id)) return null;
  return { id: id as TourChapterId, version: Number(rest) };
}

export interface TourResumePoint {
  chapter: TourChapterId;
  step: number;
}

/** What the help page says against a chapter. */
export type TourChapterState = 'InProgress' | 'New' | 'Updated' | 'Done' | 'Skipped' | 'Available';

/**
 * The [baseline] stops an existing learner seeing every chapter badged New the first time chapters
 * appear: it is written once, as the current version for someone already set up, or 0 for a new
 * learner, so only chapters added after it badge.
 */
export function chapterState(
  chapter: TourChapter,
  completed: string[],
  skipped: string[],
  baseline: number,
  resume: TourResumePoint | null
): TourChapterState {
  if (resume?.chapter === chapter.id) return 'InProgress';
  const seen = completed.map(parseStamp).filter((s) => s?.id === chapter.id).map((s) => s!.version);
  if (seen.includes(chapter.version)) return 'Done';
  if (skipped.includes(chapter.id)) return 'Skipped';
  if (seen.length > 0) return 'Updated';
  return chapter.version > baseline ? 'New' : 'Available';
}

/** Drops a stored resume point for a chapter that no longer exists or has since been shortened. */
export function sanitizeResume(chapter: string | null | undefined, step: number | null | undefined): TourResumePoint | null {
  if (!chapter || step == null || !Number.isInteger(step) || step < 0) return null;
  const found = chapterById(chapter);
  if (!found || step >= found.stops.length) return null;
  return { chapter: found.id, step };
}
