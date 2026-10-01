# KasiGuru UI cleanup

Implemented from Adrian's `KasiGuru UI Cleanup.html` (2026-10-01). This guide and the current Forest theme supersede the historical Violet Sheet descriptions in DESIGN.md. Existing normalized XP, six permanent badge tiers, sourced language content, and review-plus-three-games streak rules remain authoritative. Customized AI badge images are excluded.

## Screen changes

1. Shared `KasiGuruTextField`: persistent labels, example placeholders, 16 dp corners, hairline borders and lime focus. Locked achievements show the existing lock glyph.
2. Library has one inline dictionary search. Category search filters only its own category. The floating search component was removed.
3. Home puts Continue first, followed by today's goal/review tiles, quick practice, and a real dictionary word of the day. Announcements and account backup reminders expand from compact rows below the learning content.
4. Learn uses 72 dp nodes, a winding Canvas trail, section scenery and segmented completion. Labels identify every lesson. Returning from a completed lesson advances the current-node position.
5. Lessons have a scene strip, Jepjep, segmented progress, correct-answer combos, short-answer grids, answer animations and feedback audio. Sound and haptic preferences are independent of pronunciation playback.
6. Flashcards have a paper stack, a left-hinged cover, 12 category vectors, word/answer sides and an opening hint. Rating buttons appear after revealing the answer and show real SM-2 intervals. Swipes use Good/Again; reduced motion removes cover movement.
7. The streak page shows the real weekly quota and permanent Consistent Learner milestones at 1, 3, 7, 30, 90 and 180 days. Home's chip and the first completed lesson of a day open it.
8. Me has scene selection, an overlapping avatar, rank link, compact stats and recent achievements. Forest and Casapsapan are free; Farm unlocks at level 2, River at 3, Ermita Hill at 4, Ontok Lighthouse at level 5 or a 7-day longest streak, and Tibu Tidal Pool at a 30-day longest streak.
9. Practice includes Games/Leaderboard. This-week activity XP, all-time XP and streak rankings have independent caches, a podium, rows and a pinned current-player entry. Expired weekly data does not appear as the current week's ranking.
10. Player profiles expose only the display name, avatar/background and learning statistics. The showcase chooses the highest tier per family, then tier and measured rarity. Profiles include weekly comparison, all 66 achievement tiers and section completion.
11. Appearance supports System/Light/Dark and 90/100/115/130% text size on top of Android's font scale. Light mode uses #F6F4EA ground, white cards, #142012 ink and #3D7010 lime foregrounds; clay reward fills keep their Forest colors.
12. Guided chapters include flashcard gestures, streaks, backgrounds, Appearance and player rankings. The spotlight springs between targets; captions arrive afterward. Chapters can be replayed from Help or Settings.

## Data and release requirements

Room 33 adds `user_progress.profileBackgroundId`, board-specific leaderboard cache fields, and `public_profile_cache`. Migration 32→33 preserves normalized totals, reward receipts and streak history. Background selection travels through the existing private progress sync and merges by the latest profile edit; older clients without the field do not erase it.

`public_profiles/{uid}` is a separate allowlisted DTO. It never copies full name, age, address, email, password or recovery fields. Reads are public; writes belong to a registered owner; guest sessions do not publish. Account deletion removes the public document. Section and badge identifiers are validated by the rules.

The repository includes updated Firestore rules and weekly ranking indexes. These are **not deployed** by this local UI change. Deploy them with the compatible app release before relying on public profiles or cross-device background sync. Ranking totals remain client-published under the existing bounded-write rules.

## Verification

Run repository checks, `testDebugUnitTest assembleDebug lintDebug`, and the complete `connectedDebugAndroidTest` suite. The database test runner disables token-delivery services only during its plain-Application test run, then restores them.

Public-profile security checks run solely against a demo Firestore emulator:

```powershell
# Requires Java 21 or newer. The checked-in config binds Firestore to 127.0.0.1.
firebase emulators:exec --only firestore --project demo-kasiguru-ui "node scripts/tests/public-profile-rules.cjs"
```

The script refuses a non-local emulator address. It verifies valid publishing, public reads, owner-only writes/deletes, guest rejection, private-field rejection, known backgrounds/badges/sections and bounded statistics. It never touches a production project.

### Resume checkpoint — 2026-10-01

Resumed UI cleanup together with XP normalization and tiered badges. Repository structure, web tokens and `git diff --check` pass. Gradle `testDebugUnitTest assembleDebug lintDebug` succeeds; the current unit-test reports contain 312 passing tests across 45 suites. Lint reports 0 errors and 95 warnings.

The saved device report for the current code contains 16 passing tests: six gamification repository tests and ten migration tests, including Room 32→33. Device tests were inspected, not rerun during this resume. All 22 public-profile security checks were rerun successfully against `demo-kasiguru-ui` on the local Firestore emulator.

Visual review was subsequently completed on the offline Android emulator; see
[the emulator review](EMULATOR_REVIEW_2026-10-01.md) for exercised flows, fixes,
reward evidence and remaining coverage limits. Firestore rules/index deployment
and release publishing remain separate release steps.

The subsequent [focused lint cleanup](LINT_CLEANUP_2026-10-01.md) corrected tour
input, image metadata, backup rules and locale formatting. Its final checks have
312 passing unit tests, six passing focused device tests, and 86 lint warnings
with zero errors.
