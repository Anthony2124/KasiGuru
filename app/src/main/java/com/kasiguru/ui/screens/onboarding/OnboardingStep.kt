package com.kasiguru.ui.screens.onboarding

/**
 * The onboarding flow, in order: Adrian's fifteen screens.
 *
 * The first three are full-bleed story moments (the forest welcome, Jepjep asleep, Jepjep awake);
 * the other twelve share one frame with the segmented progress bar, Skip, Back and Next. The order
 * here *is* the order on screen, so moving a step means moving its entry.
 *
 * Deliberately free of Android and Compose types, so the skip and back rules can be unit-tested.
 *
 * @param framed drawn inside the shared frame, and counted by its progress bar.
 * @param skippable the frame offers Skip. Off for the first word and its reward, which Skip must
 *   never jump past, and for the last step, where Next already finishes.
 * @param optional Next works without an answer. Only the first word has to be answered.
 */
enum class OnboardingStep(
    val framed: Boolean,
    val skippable: Boolean,
    val optional: Boolean
) {
    Welcome(framed = false, skippable = false, optional = true),
    Asleep(framed = false, skippable = false, optional = true),
    Awake(framed = false, skippable = false, optional = true),
    Name(framed = true, skippable = true, optional = true),
    Greeting(framed = true, skippable = true, optional = true),
    Words(framed = true, skippable = true, optional = true),
    Minutes(framed = true, skippable = true, optional = true),
    Games(framed = true, skippable = true, optional = true),
    Level(framed = true, skippable = true, optional = true),
    Goal(framed = true, skippable = true, optional = true),
    FirstWord(framed = true, skippable = false, optional = false),
    FirstWordLearned(framed = true, skippable = false, optional = true),
    Reminders(framed = true, skippable = true, optional = true),
    Avatar(framed = true, skippable = true, optional = true),
    Badges(framed = true, skippable = false, optional = true);

    /** The step Next leads to; null on the last one, where Next finishes. */
    val next: OnboardingStep?
        get() = entries.getOrNull(ordinal + 1)

    /**
     * The step Back returns to. The wake-up moments play once on the way in and are not replayed on
     * the way out, so Back from the name step lands on the welcome screen.
     */
    val previous: OnboardingStep?
        get() = entries.take(ordinal).lastOrNull { it.framed || it == Welcome }

    /**
     * Where Skip goes. Everything before the first word jumps straight to it, because reading and
     * getting one real word is the moment the flow exists for; once it is done, Skip finishes with
     * defaults (null).
     */
    val skipTarget: OnboardingStep?
        get() = if (ordinal < FirstWord.ordinal) FirstWord else null

    /** Zero-based position among the framed steps, or -1 for the story moments. */
    val progressIndex: Int
        get() = if (framed) entries.count { it.framed && it.ordinal < ordinal } else -1

    /** Steps that reward something, where the glow may breathe. */
    val celebrates: Boolean
        get() = this == Awake || this == FirstWordLearned

    /**
     * Which top-level scene draws this step. Every framed step shares one, so moving between them
     * animates only the middle of the frame while the progress bar and buttons stay put.
     */
    val stage: Int
        get() = if (framed) FRAMED_STAGE else ordinal

    companion object {
        private const val FRAMED_STAGE = -1

        /** Segments in the progress bar. */
        val framedCount: Int = entries.count { it.framed }

        /** Restores a saved step, falling back to the start if the saved value no longer exists. */
        fun fromOrdinal(ordinal: Int): OnboardingStep = entries.getOrElse(ordinal) { Welcome }
    }
}

/**
 * Daily goals, led by minutes because that is how people plan a day. [xp] is what is stored as
 * `dailyGoalXp`; the minute figures are the same estimates the goal tiles have always shown.
 */
enum class DailyGoal(val xp: Int, val minutes: Int, val label: String) {
    Casual(xp = 50, minutes = 5, label = "Casual"),
    Regular(xp = 100, minutes = 10, label = "Regular"),
    Serious(xp = 150, minutes = 15, label = "Serious"),
    Intense(xp = 200, minutes = 20, label = "Intense");

    companion object {
        /** Used when the learner skips past both the level question and the goal. */
        val Default = Regular
    }
}

/**
 * The answer to "How much Kasiguranin do you know?".
 *
 * There is no database field for it yet, so it lives in UI state only and does one job: it picks
 * which goal tile starts selected. The more someone already knows, the faster the early words go,
 * so the suggestion rises with it. A goal the learner taps always wins over the suggestion.
 */
enum class KnowledgeLevel(val label: String, val suggestedGoal: DailyGoal) {
    New(label = "I'm new to it", suggestedGoal = DailyGoal.Casual),
    AFewWords(label = "I know a few words", suggestedGoal = DailyGoal.Regular),
    Understands(label = "I understand it but rarely speak it", suggestedGoal = DailyGoal.Serious),
    Speaks(label = "I speak it", suggestedGoal = DailyGoal.Intense)
}

/** The goal that gets saved: the learner's own pick, else the level's suggestion, else the default. */
fun resolveDailyGoal(chosen: DailyGoal?, level: KnowledgeLevel?): DailyGoal =
    chosen ?: level?.suggestedGoal ?: DailyGoal.Default

/**
 * The first word. "aldew" pays off the "Magandang aldew" Jepjep greets the learner with on the name
 * step, and "Water" is the meaning of "danom". Both are seeded in DatabaseSeeder (aldew = day and
 * aldew = sun, danom = water); nothing here is new Kasiguranin.
 */
object FirstWord {
    const val WORD = "aldew"

    /** The seeded `ipaNotation`, bracketed the way the lesson player and the dictionary show it. */
    const val IPA = "[ˈɁal.dɛw]"

    const val MEANING = "Day, sun"

    /** Answer options in the order they are shown. */
    val options: List<String> = listOf("Water", MEANING)

    val correctIndex: Int = options.indexOf(MEANING)
}
