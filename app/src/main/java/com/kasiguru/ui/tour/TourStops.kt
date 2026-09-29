package com.kasiguru.ui.tour

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.navigation.Screen
import com.kasiguru.ui.theme.Radius

/**
 * One stop on the guided tour.
 *
 * @param target where this stop happens. The tour navigates there before showing the stop, so the
 *   caption is always describing something the learner can actually see behind the dim. Most stops
 *   name a fixed route; a few describe a destination that only exists at runtime.
 * @param anchor the element to cut a hole around, or null for a stop that explains a screen as a
 *   whole. A null anchor dims without cutting and centres the caption.
 * @param pad how far the hole is inflated past the element, so a clay lip or a shadow is not clipped
 *   mid-falloff.
 * @param corner the hole's corner radius. Matches the shape of the thing underneath.
 */
data class TourStop(
    val target: TourTarget,
    val anchor: TourAnchor?,
    val title: String,
    val body: String,
    val pad: Dp = 8.dp,
    val corner: Dp = Radius.tile
)

/**
 * The core chapter: the ninety seconds every new learner is asked for.
 *
 * Nine stops, opening on the action and closing on the reason to come back. It deliberately does
 * not begin with "this is the navigation bar": the first thing a new learner needs is the one button
 * that starts them learning, and stops 3-7 teach the bar far better by visiting each destination
 * than a single caption pointing at the whole pill ever could.
 *
 * It opens and closes on Home, because Home is where the learner lands every day afterwards.
 *
 * The copy carries no counts - not the size of the corpus, not the number of badges. Those numbers
 * are live and grow through the admin portal, and hardcoding one is how "487 words" survived in the
 * FAQ long after it was wrong.
 */
val coreChapter: TourChapter = TourChapter(
    id = TourChapterId.Core,
    title = "Getting around",
    subtitle = "The five places, and the one button that always knows what is next",
    // 2: the tabs became Home, Learn, Practice, Library and Me.
    version = 2,
    stops = listOf(
        TourStop(
            target = TourTarget.Fixed(Screen.Home.route),
            anchor = TourAnchor.ContinueAction,
            title = "Start here",
            body = "This button always names what comes next - a lesson, a review, a game or a story. " +
                "When you do not know where to begin, begin here.",
            corner = Radius.panel
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Home.route),
            anchor = TourAnchor.DailyGoalRing,
            title = "Today's goal",
            body = "How much of today's target you have done. You set the target during setup and can " +
                "change it whenever it stops fitting your week.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Home.route),
            anchor = TourAnchor.NavHome,
            title = "Home",
            body = "Where every day starts: the next thing to do, your goal, the words due for review " +
                "and a shelf of stories.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Learn.route),
            anchor = TourAnchor.NavLearn,
            title = "Learn",
            body = "The whole path, section by section, from greetings to everyday talk. Jepjep waits " +
                "at the lesson that is yours to take next.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.GameHub.route),
            anchor = TourAnchor.NavPractice,
            title = "Practice",
            body = "Games that drill the words you have already met, and the leaderboard to see where " +
                "you rank. Three games are open now, and more are coming soon.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Library.route),
            anchor = TourAnchor.NavLibrary,
            title = "Library",
            body = "The whole dictionary with audio on every entry, and the folk tales beside it. All " +
                "of it works offline, and you can add a word or share a story from here.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Profile.route),
            anchor = TourAnchor.NavMe,
            title = "Me",
            body = "Who you are and what you have earned - your badges, and the way through to Settings.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Home.route),
            anchor = TourAnchor.NotificationBell,
            title = "Reminders and news",
            body = "Streak reminders, the word of the day and anything the team announces arrive here. " +
                "You choose which of those you want in Settings.",
            corner = Radius.pill
        ),
        TourStop(
            target = TourTarget.Fixed(Screen.Home.route),
            anchor = TourAnchor.StreakBadge,
            title = "Come back tomorrow",
            body = "This is your streak. Practise a little every day and it keeps growing. You can " +
                "take this tour again any time from Settings.",
            corner = Radius.pill
        )
    )
)
