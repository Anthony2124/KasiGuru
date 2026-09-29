package com.kasiguru.ui.tour

import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.theme.Radius

/**
 * The optional chapters, launched from the help page and never forced on anyone.
 *
 * Two rules govern the copy here, and both come from what a new account actually looks like.
 *
 * A brand-new learner has no badges, no rank, no streak and no finished lessons, so a stop cannot say
 * "here are your badges" over an empty grid - it says what will appear there and what earns it. And
 * no stop states a count: the corpus grows through the moderation queue, and the last time a number
 * was written into copy it was wrong for months before anyone noticed.
 */

/**
 * Everything about the dictionary except the words themselves.
 *
 * Walked on the pushed dictionary screen rather than on the Library tab. It is the same content, but
 * a pushed screen unwinds cleanly back to the help page when the chapter ends, where a tab switch
 * would leave the learner on Library with the help page stacked on top of it.
 */
val dictionaryChapter: TourChapter = TourChapter(
    id = TourChapterId.Dictionary,
    title = "The dictionary",
    subtitle = "Finding a word, hearing it, and adding one that is missing",
    // 2: the dictionary moved into the Library tab, with one search field.
    version = 2,
    stops = listOf(
        TourStop(
            target = TourTarget.Fixed(Screen.VocabularyList.route),
            anchor = TourAnchor.DictWordOfDay,
            title = "A word each day",
            body = "One word is offered every day. Tap it for the whole entry, or the speaker to hear " +
                "how it is said.",
            corner = Radius.panel
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.VocabularyList.route),
            anchor = null,
            title = "Categories, and search",
            body = "Words are grouped by what they are about - the body, the house, the weather. The " +
                "search field at the top looks through every entry and every group at once. You will " +
                "find all of this under Words in the Library tab."
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.VocabularyList.route),
            anchor = TourAnchor.DictSubmitBanner,
            title = "A word we are missing",
            body = "The dictionary is not finished, and it is not meant to be. If you know a word that " +
                "is not here, Add a word is where it starts its way in.",
            corner = Radius.panel
        )
    )
)

/** The contribution flow, including the duplicate check the app has always had and never explained. */
val contributeChapter: TourChapter = TourChapter(
    id = TourChapterId.Contribute,
    title = "Adding a word",
    subtitle = "What we need from you, and what happens after you send it",
    version = 1,
    stops = listOf(
        TourStop(
            target = TourTarget.Fixed(Screen.SubmitWord.route),
            anchor = TourAnchor.SubmitWordField,
            title = "Start with the Kasiguranin",
            body = "Type the word here. As you type, the app checks the dictionary and tells you if it " +
                "is already there - including spellings that differ only by an accent."
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.SubmitWord.route),
            anchor = null,
            title = "Say what it means",
            body = "Tagalog or English, whichever you are sure of - you do not need both. Everything " +
                "below that is welcome and optional."
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.SubmitWord.route),
            anchor = TourAnchor.SubmitButton,
            title = "Then it goes for review",
            body = "A moderator checks every submission before it joins the dictionary, so nothing " +
                "enters the record unchecked. You will see it appear once it is approved."
        )
    )
)

/** Practice: what the games are for, and why most of them start locked. */
val lessonsChapter: TourChapter = TourChapter(
    id = TourChapterId.Lessons,
    title = "Lessons and practice",
    subtitle = "How the app works out what to show you next",
    // 2: the primary action moved from Learn to Home.
    version = 2,
    stops = listOf(
        TourStop(
            target = TourTarget.Fixed(Screen.GameHub.route),
            anchor = TourAnchor.PracticeStats,
            title = "What you have earned",
            body = "Experience, stars and accuracy across every game you have played. Stars are the " +
                "ones that matter here - they are what opens the rest."
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.GameHub.route),
            anchor = TourAnchor.PracticeFeatured,
            title = "Where to pick up",
            body = "The game you have the most room to improve at, chosen for you. Ignore it freely - " +
                "it is a suggestion, not an instruction.",
            corner = Radius.panel
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.GameHub.route),
            anchor = null,
            title = "More games are coming",
            body = "Word Match, Word Search and Word Wheel are open now. The tiles marked Coming soon " +
                "are games still being finished, and they open in a later update."
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Home.route),
            anchor = TourAnchor.ContinueAction,
            title = "Or just start here",
            body = "This button on Home works out what you are due and takes you straight into it, so " +
                "you never have to plan a session yourself. The full path of lessons is on Learn.",
            corner = Radius.panel
        )
    )
)

/**
 * Progress: badges, and what they mean on an account where almost all of them are still locked.
 *
 * The badge wall is no longer a tab - it opens from Me - so the chapter pushes it like any other
 * screen and unwinds back to wherever it was started from.
 */
val progressChapter: TourChapter = TourChapter(
    id = TourChapterId.Progress,
    title = "Progress and badges",
    subtitle = "What gets counted, and what each badge is waiting for",
    // 2: badges moved under Me, and the filter covers every kind of badge.
    version = 2,
    stops = listOf(
        TourStop(
            target = TourTarget.Fixed(Screen.Achievements.route),
            anchor = TourAnchor.ProgressBadgePanel,
            title = "Badges earned",
            body = "This counts what you have unlocked against everything there is to unlock. It will " +
                "read low for a while, and that is the point - none of it is given.",
            corner = Radius.panel
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Achievements.route),
            anchor = TourAnchor.ProgressFilter,
            title = "Sorted by how you earn them",
            body = "Some badges come from levelling up, some from words, streaks, games, stories or " +
                "helping the dictionary grow. This narrows the wall to one kind at a time.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Achievements.route),
            anchor = null,
            title = "A locked badge still tells you something",
            body = "Every badge you have not earned shows what it is waiting for, and how far along you " +
                "already are. None of them is a mystery box. Me shows the ones you earned most recently."
        )
    )
)

/** Me and Settings: the two screens everything else hangs off. */
val profileSettingsChapter: TourChapter = TourChapter(
    id = TourChapterId.ProfileSettings,
    title = "Me and settings",
    subtitle = "Your record, and the switches that change how the app behaves",
    // 2: Profile and Progress merged into the Me tab.
    version = 2,
    stops = listOf(
        TourStop(
            target = TourTarget.Fixed(Screen.Profile.route),
            anchor = TourAnchor.ProfileExplore,
            title = "Everything else lives here",
            body = "Casiguran's cultural heritage, this guide, and what the project is. Your badges sit " +
                "just above. Me is the way through to all of it.",
            corner = Radius.panel
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Profile.route),
            anchor = TourAnchor.ProfileSettingsIcon,
            title = "Settings",
            body = "Reminders, sound, dark mode and your account all sit behind this one icon.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Settings.route),
            anchor = TourAnchor.SettingsAccount,
            title = "Keep your progress safe",
            body = "You are signed in anonymously, which means your progress lives only on this phone. " +
                "Adding an email or a Google account is what lets you get it back if the phone is lost.",
            corner = Radius.panel
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Settings.route),
            anchor = TourAnchor.SettingsPreferences,
            title = "Dark mode and audio",
            body = "The app follows whichever you choose here, everywhere - including this tour.",
            corner = Radius.panel
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Settings.route),
            anchor = TourAnchor.SettingsReplayTutorial,
            title = "And you can always come back",
            body = "This row starts the short tour again. The longer chapters are on the help page in " +
                "Me, and none of them ever expire."
        )
    )
)

/**
 * Notifications and stories: the two places outside the daily plan worth a visit - one that fills
 * itself in as things happen, one that has been full since install.
 *
 * The stories stops use the pushed stories screen, for the same reason the dictionary chapter uses
 * the pushed dictionary: it unwinds cleanly back to where the chapter began.
 */
val inboxChapter: TourChapter = TourChapter(
    id = TourChapterId.Inbox,
    title = "Notifications and stories",
    subtitle = "Where reminders arrive, and where the folk tales live",
    // 2: stories moved into the Library tab.
    version = 2,
    stops = listOf(
        TourStop(
            target = TourTarget.Fixed(Screen.Notifications.route),
            anchor = TourAnchor.InboxFilters,
            title = "Filter what you see",
            body = "Streaks, the word of the day, achievements, the leaderboard - narrow the list to " +
                "one kind at a time."
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Notifications.route),
            anchor = null,
            title = "This is where they land",
            body = "Nothing here yet means nothing has happened yet, not that anything is broken. The " +
                "first thing you will see is usually a reminder not to lose your streak."
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.StoryList.route),
            anchor = TourAnchor.StoryShelf,
            title = "Folk tales, in three languages",
            body = "Every story is told in Kasiguranin, Tagalog and English side by side. Tap any word " +
                "as you read to look it up without losing your place. They live under Stories in the " +
                "Library tab.",
            corner = Radius.panel
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.StoryList.route),
            anchor = null,
            title = "Know one we do not have?",
            body = "The Share a story or poem button sends one in for review, the same way a missing " +
                "word does. A moderator reads it before it joins the shelf."
        )
    )
)
