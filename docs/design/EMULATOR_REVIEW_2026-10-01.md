# Android emulator review — 2026-10-01

Reviewed the current UI cleanup and normalized XP/tier badges on the existing
`kasi_test` Android emulator, at 1080 × 2400 pixels and density 420. The emulator
ran with a temporary read-only disk overlay. Airplane mode was enabled before
opening the app, with Wi-Fi disabled, so this was an offline local review.

## Coverage

| Area | Observed result |
|---|---|
| Home and Learn | Continue, daily goal, quick practice and dictionary word render. A completed lesson becomes familiar and the next lesson becomes current. |
| Lesson | Completed Family and people lesson 1: translation, reverse translation, matching, typed recall, incorrect feedback and retry. Long answers use vertical options. Completion shows 90% first-attempt accuracy and +20 activity XP. |
| Flashcards | Opened cover, revealed answers, checked rating intervals, used a Good swipe, and completed six cards. Rating controls appear after reveal. |
| Games | Completed Word Match levels 1 and 2 and Greetings & Essentials Word Search level 1. Results, correction review, return to levels and next-level unlocking work. |
| Live game summaries | Word Search best score changed 0 → 4 without restart. Word Match level-picker total changed 6 → 8 stars on returning from level 2. |
| Streak | Review plus one game still showed 1/3 games. The third game activated today; the streak page then showed “Today counts. Your streak is safe!” |
| Me | Avatar level and next threshold follow total XP. Farm selection persisted through APK upgrades; River was locked at level 2. |
| Badges | Collection, activity/badge totals, earned and locked tiers, detail milestones through Legend, and progress render in Light and Dark at 130% text. |
| Appearance | Checked System/default Light at 100%, Dark at 100% and 130%, and Light at 130%. Preferences persist across APK upgrades. |
| Library | Inline search, dictionary result and authored word detail render at 130%. |
| Rankings | This week, all time and streak tabs are usable offline and show their empty state. No live player data was available. |

## Corrections made during review

- Provide the theme ink as the default content color. Badge headings and
  normalization figures previously inherited black text on dark surfaces.
- Reduce horizontal quick-practice button padding so “Flashcards” fits at 130%.
- Give the avatar level chip an explicit line height; the digit was clipped.
- Use singular “day” for one-day streak labels.
- Observe total stars in the level ViewModel and refresh game high scores when
  the score flow changes. Both summaries previously retained their initial value.
- Update game rules to normalized XP, remove obsolete speed/combo reward claims,
  and describe Word Search's touch gesture as dragging.
- Label the result action “Back to levels” to match its destination.
- Replace the streak celebration's obsolete “+25 XP Daily Streak Bonus” claim
  with “Daily goals complete.” The repository advances the streak without a
  separate XP reward. This final text correction was build-checked; the one-time
  celebration was not triggered again after changing its label.

## Reward evidence

The local account started with 105 XP: 65 activity and 40 badge XP.

| Completed activity | Activity XP added | Badge XP added | Total XP afterward |
|---|---:|---:|---:|
| Lesson, 90% | 20 | 0 | 125 |
| Six reviewed cards, Good | 15 | 0 | 140 |
| Word Match level 1, 5/5, no hints | 20 | 60 | 220 |
| Word Search level 1, 4/4, no wrong lines | 14 | 40 | 274 |
| Word Match level 2, 4/5 | 13 | 0 | 287 |

Five distinct review receipts account for six cards: the two authored `dakëp`
entries share the same word/day reward identity. The final local ledger showed
147 activity XP, 140 badge XP, 82 dated daily activity XP, level 3 and three daily
games. Journey Rank's level-3 tier granted no badge XP. The daily streak
celebration granted no separate XP, despite its previous text.

## Verification and limits

`testDebugUnitTest assembleDebug lintDebug` passed after the functional fixes;
the reports contain 312 passing unit tests across 45 suites. Final instruction
and celebration text changes also passed `assembleDebug lintDebug`. Lint has
0 errors and 95 warnings. Structure, web token and whitespace checks passed.
No `AndroidRuntime` crash was observed during the review. The emulator had an
initial System UI stall before app testing, which recovered.

The previously inspected 16 passing device tests and 22 local Firestore rules
checks are recorded in [UI_CLEANUP.md](UI_CLEANUP.md). Device tests were not
rerun for these display/ViewModel changes; no schema changed during this review.

Online rankings, public player profiles, account/cloud sync, all game modes,
all text sizes, pronunciation sound quality and physical-device behavior remain
outside this review. Nothing was published or deployed.

Two corpus entries need author review: `kaliwa kariwe` displays the Tagalog
gloss “(hand)” for English “left”; `panalo` has “talo, tagumpay” against English
“win a competition.” Sourced vocabulary was preserved.

## Local evidence

Screenshots and UI hierarchy captures are in `C:\KasiGuru\emulator-review`.
Representative captures:

- [Dark badges at 130%](../../../emulator-review/badges-fixed-dark-130.png)
- [Light badges at 130%](../../../emulator-review/badges-light-130-final.png)
- [Lesson completion](../../../emulator-review/lesson-completion-summary.png)
- [Review completion](../../../emulator-review/review-completed.png)
- [Game summaries after Word Search](../../../emulator-review/games-refreshed.png)
- [Word Match stars after level 2](../../../emulator-review/wordmatch-stars-refreshed-final.png)
- [Completed streak quota](../../../emulator-review/streak-quota-complete-ready.png)

These evidence files are local workspace artifacts, outside the application
repository. The emulator overlay was discarded after the review, preserving the
original AVD's app data.
