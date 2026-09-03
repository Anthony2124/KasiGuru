# KasiGuru UI/UX Cleanup Prompt

A standing, reusable prompt for a maintenance pass over the **Android app's** interface. Paste the
block below into a fresh Claude Code session with access to this repo.

This is not the audit prompt and not a redesign brief. `docs/AUDIT_PROMPT.md` is read-only and covers
the whole system; this one is scoped to `app/`, is allowed to change code, and assumes the Violet
Sheet design system already exists and is already shipped. Its job is to bring drifted code back to
the system, not to invent anything.

The **KNOWN DEBT** list inside is a real inventory taken against v1.10.0. Re-verify before acting on
it — items get fixed, and a stale list is worse than none.

---

## The prompt

```
You are running a UI/UX cleanup pass on the KasiGuru Android app. This is
maintenance on a design system that already exists and is already shipped, not
a redesign. Bring drifted code back to the system, remove what is dead, and
leave the app more consistent than you found it.

FIRST, READ THESE. They are ground truth. Do not re-derive them, and do not
argue with the visual direction:
  - HANDOFF.md   Adrian's standing instructions, the design rules that get
                 missed, and the verification techniques that work here.
  - DESIGN.md    The "Violet Sheet" design authority: colours, the two shells,
                 shape, motion, and the refuse list, with measured contrast.
  - PRODUCT.md   What the app is, who it serves, what must not change.

Then load the design skills before touching any UI: impeccable:impeccable and
ui-ux-pro-max. ui-ux-pro-max needs the full Python path given in HANDOFF.md
(the python on PATH is a Microsoft Store stub); its --stack jetpack-compose and
native-app guidance are the relevant parts. HANDOFF.md also names the taste
skill: its own scope line excludes multi-step product UI, so it applies to the
web surfaces rather than to this app. Say so rather than skipping it silently.

SCOPE: app/ only. Not admin-website/, not the download page, not functions/.

WHAT IS ALREADY DONE. Do not redo any of this:
  - The visual migration onto Violet Sheet. It is genuinely thorough.
  - A 38-finding UX-psychology audit, implemented.
  - Real dark mode. Every themed token is a @Composable get() reading
    LocalDarkMode, with deliberately fixed tokens for reward fills and scrims.
  - BackHandler coverage.
  - The first-run guided tour and help page, in ui/tour/ and ui/screens/help/.
  - The six mini-games already share GameShellComponents and GameOverView. Do
    not "unify the game screens" - that is finished.
If you believe one of these is wrong, report it. Do not rebuild it.

THREE PASSES, IN THIS ORDER. Finish and report each before starting the next.

PASS 1 - Consistency and polish. This is where nearly all the value is.
  - Off-token colour: any Color(0x...) outside ui/theme/Color.kt.
  - Off-token type: literal fontSize = N.sp instead of a
    MaterialTheme.typography role. Watch for the doubled-up anti-pattern where
    a call sets both a typography style and a literal fontSize.
  - Off-token spacing and shape: literal .dp padding where Space.* exists, and
    literal RoundedCornerShape(N.dp) where Shapes.*/Radius.* exists. Odd values
    (2, 3, 6, 7, 10 dp) are the tell - Space is 4/8/12/16/24/32/48.
  - Components that never migrated off MaterialTheme.colorScheme onto the
    app's own tokens (Ink, Surface, Muted, BorderHairline).
  - Near-duplicate components: the same row, card, button or dialog defined
    privately in several files. Consolidate only where they are genuinely the
    same thing. Structural differences a shared wrapper would have to fake with
    optional slots are not duplication - a previous session reviewed the four
    bespoke list rows and correctly left them alone.
  - DESIGN.md's refuse list, which is binding: no eyebrow/kicker labels above
    headings, no same-size cards as page structure, no emoji as interface, no
    gradient text, no glass over the Ground.
  - The shell rule - CanopyScaffold on Learn and nowhere else, GroundScaffold
    everywhere else, and neither on the immersive screens (the six mini-games,
    Lesson Player, Lesson Complete, Flashcards, Story Reader). This was
    verified clean at v1.10.0. Re-check it cheaply; do not go looking for work
    here.

PASS 2 - Accessibility.
  - Touch targets: 48dp minimum (Touch.minTarget), 8dp apart. The failure mode
    here is .clickable() applied directly to a small Icon. GroundIconButton in
    ui/components/clay/GroundSurfaces.kt is the model - it sizes to
    Touch.minTarget and requires a contentDescription. Point new icon buttons
    at it rather than inventing another.
  - contentDescription on every meaningful icon-only control. Decorative icons
    sitting beside their own visible text label correctly take null; most of
    the ~110 nulls in this codebase are that case, so spot-check rather than
    sweep.
  - Re-measure every colour pairing you touch with a real WCAG calculator,
    never by eye. DESIGN.md's published figures are the reference, and its
    three hard colour rules each came from a measured failure.
  - Walk the app at font_scale 1.3 and fix what clips or truncates.
  - Reduced motion is a contract: use motionTween/motionDuration from
    ui/theme/Motion.kt. Nothing may depend on an animation having run.

PASS 3 - Dead code and loose ends. Expect this pass to be short.
  As of v1.10.0 there were zero TODO/FIXME/HACK markers, zero commented-out
  code, and every route in ui/navigation/Screen.kt was reachable. Confirm that
  is still true and move on. Do not manufacture findings.
  - Re-check every Screen.kt route for a real navigate() call. A previous
    session found five fully-working screens orphaned when the old Home
    dashboard was deleted, so this is a real failure mode here, not a
    hypothetical - it is just currently clean.
  - Unreferenced composables, ViewModels, repository functions, and entity
    fields that are written and seeded but never read.
  - Before deleting anything, check it is not reached by a deep link
    (ui/navigation/NavGraph.kt, functions/send_push.js), a notification route,
    a Room migration, or a test.

KNOWN DEBT, inventoried at v1.10.0 and ordered by value over cost. Re-verify
each before acting; start here, then look for more.

  1. Emoji as interface has returned, which DESIGN.md bans by name and warned
     would come back through GamificationEngine. It did:
     util/gamification/GamificationEngine.kt LEVEL_ICONS/iconEmoji and
     data/local/entity/AchievementEntity.kt iconEmoji are both seeded and never
     read - dead, delete them. Live and user-visible: three seeded notification
     titles in data/local/DatabaseSeeder.kt, util/notification/
     StreakReminderCopy.kt, and flame emoji in StreakDialog.kt and
     StreakCelebrationDialog.kt, including inside a ClayButton label.
  2. Three byte-identical sub-48dp back buttons - a .clickable() on a 28dp Icon
     - in FlashcardDeckScreen.kt, LessonPlayerScreen.kt and StoryReaderScreen.kt.
     One shared "immersive back" component sized to Touch.minTarget fixes an
     accessibility bug and a triplication in the same move.
  3. AboutScreen.kt's FAQ hardcodes the vocabulary count as a string literal.
     VocabularyDao already has the COUNT(*) query. The corpus grows through the
     moderated contribution queue, so this number is guaranteed to drift - it
     has already been wrong twice.
  4. The dialog and overlay cluster never migrated onto the app's tokens:
     StreakDialog, StreakCelebrationDialog, WordVerificationDialog, GameOverView,
     LevelUpDialog, WordDetailBottomSheet, AppUpdateBanner, SecureProgressBanner
     and the six game screens still use MaterialTheme.colorScheme and literal
     RoundedCornerShape. They all take the same fix, and they are on screen
     constantly. Note BorderHairline exists as a token and has exactly one
     caller - these are the rest of its callers.
  5. Three different audio-play buttons for one action. AudioPlayButton is the
     shared one; CategoryDetailScreen.kt and VocabularyScreen.kt each reinvent
     it. Consolidating also removes a 44dp touch target that a nearby comment
     incorrectly calls properly sized.
  6. FlashcardDeckScreen.kt puts an uppercase category chip directly above the
     Kasiguranin headword - the banned kicker-above-heading pattern with dynamic
     text instead of static - and the chip is white at 0.22 alpha on the canopy
     gradient's shallow end, which DESIGN.md measures as a contrast failure.
  7. 44 literal fontSize = N.sp across 9 files. Worst: SubmitWordScreen.kt,
     ReportIssueScreen.kt, LeaderboardScreen.kt.
  8. 38 literal .padding(N.dp) across 18 files. Worst: VocabularyScreen.kt and
     CategoryDetailScreen.kt.
  9. ui/components/CoastalComponents.kt is named after the superseded system but
     its contents are live. Rename and relocate rather than delete, and fold its
     eight raw illustration hexes into named constants.
 10. Color.kt's own doc comment flags VocabSea, Warning and SkyReview as needing
     a design decision rather than a rename. VocabSea is literally the old
     Casiguran Coast teal. Six call sites total. This one needs Adrian.
 11. FixedViolet/FixedVioletDeep is declared twice, in CategoryMetaData.kt and
     GameRulesDialog.kt, with identical values that can drift apart.
 12. Theme.kt's comment says outline is deliberately dark ink; the value
     assigned is Muted. Correct whichever is wrong so they stop disagreeing.

GUARDRAILS. This pass must terminate:
  - Inventory first. Show the full list before you edit anything.
  - Do not restyle a screen that already follows the system. "It could be
    prettier" is not a finding.
  - Do not change copy that states a fact without checking the fact.
  - Do not introduce a new component, token or pattern. If cleanup seems to
    need one, that is a finding to report, not a licence to add it.
  - Anything requiring a judgement about the product rather than the code goes
    to Adrian. Do not decide it yourself.
  - Stop at the end of Pass 3 and write the summary. Do not look for a fourth
    pass.

VERIFYING YOUR WORK:
  - ./gradlew compileDebugKotlin after every batch, and the unit suite before
    you finish.
  - Run impeccable's detector over every file you touched. Expect zero findings.
  - Walk the changed screens on the kasi_test emulator in both light and dark.
    HANDOFF.md has the techniques. In particular: delete the device-side dump
    before every uiautomator dump, or a failed dump silently hands you the
    previous screen's XML and reads exactly like "the tap did nothing". Issue
    one input tap per command with a settle; batched taps land on the wrong
    frame. After a cold boot, wake and dismiss the keyguard or screenshots come
    back blank.
  - Never begin a first-run test with pm clear. Clearing state before every run
    is precisely what hid an every-new-user onboarding bug across several
    releases. Uninstall, install, complete onboarding once, then cold restart
    without clearing.

OUTPUT:
Report per pass. For each change: file:line, what was wrong, what you did. For
each thing you deliberately did not change: what it was and why leaving it was
the right call - a reviewed-and-left decision is as useful as a fix and stops
the next session redoing the analysis. End with anything that needs Adrian's
decision rather than yours.
```

---

*Maintained alongside `docs/AUDIT_PROMPT.md`. Update the WHAT IS ALREADY DONE and KNOWN DEBT sections
whenever a cleanup pass lands, or the next session will chase work that is already finished.*
