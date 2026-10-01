# XP normalization and tiered achievements

Implemented policy version 2, 2026-10-01. The user requested recalculation where
history supports it and six tiers: Beginner, Learner, Achiever, Expert, Master,
Legend. Customized badge artwork was rejected; the app uses its existing medal
icon with family and tier labels.

## Rewards

| Activity | Activity XP | Evidence and limits |
|---|---:|---|
| Lesson | 20, plus 5 for perfect accuracy | Stable unit/index; best lifetime reward target. |
| Scheduled review | Again 1, Hard 2, Good/Easy 3 | First eligible word/day; review activity capped at 60 per day after combining devices. A later positive review can count toward the badge without extra review XP. |
| Verified word mastery | 5 | Once per word, after SM-2 mastery and successful retrieval on three distinct days. Manual knowledge flags earn no XP. |
| Game | 5 + weighted correct answers + 5 for an unassisted perfect run, capped at 40 | At least one star; recognition weight 2, Recall/Aspect Builder/Sentence Order weight 3, Search/Wheel board words weight 1. Speed does not multiply XP. |
| Lesson/game replay | Larger of improvement credit or replay credit | Once per content identity/day; game replay is floor(run reward / 4), capped at 10; lesson replay is 5. Same-day repeat grants no replay credit. |
| Story | 20 | First completion by story ID. Pages and rereads earn no XP. |
| Approved contribution | 10 | Once per approved submission ID. Earlier or undated approvals discovered during backfill are imported and excluded from daily/weekly activity. |
| Onboarding/app open | 0 | No welcome XP or artificial practice-day streak. Existing streak history is retained. |

Tier bonuses are **20/40/80/120/160/200 badge XP**. Journey Rank grants zero
badge XP to avoid a level/bonus feedback loop. Total XP is activity XP plus
badge XP. Account levels use total XP; daily goals and weekly rankings use
dated activity receipts only. Imported completions never count as today's or
this week's activity.

Daily goal choices are 30/50/80/100 activity XP. Upgrades map the old
50/100/150/200 effort bands to those values. The existing daily streak quota
(review words and finish three games) and exact SM-2 algorithm are preserved.

## Account levels

All 30 levels use one threshold table:

`minimumXp(level) = roundHalfUpToNearest10(100 * (level - 1)^1.5)`

Level 2 starts at 100 XP, level 10 at 2,700, level 20 at 8,280 and level 30 at
15,620. Display and stored level agree. At the cap, lifetime XP continues and
there is no next-level target. Account levels describe participation, not a
claim of language fluency. Game modes retain the existing star gates.

## Badge catalogue

Each card shows the highest earned tier, next target and cumulative progress.
Earned tiers are permanent. The detail lists all six milestones and routes to
the relevant activity. Up to three earned families can be pinned to the profile.

| Family / stable ID | Metric | Beginner | Learner | Achiever | Expert | Master | Legend |
|---|---|---:|---:|---:|---:|---:|---:|
| Word Explorer / word_explorer | Distinct verified words | 1 | 10 | 50 | 150 | 400 | 800 |
| Lesson Pathfinder / lesson_pathfinder | Distinct standard lessons | 1 | 5 | 10 | 30 | 75 | 150 |
| Review Keeper / review_keeper | Successful scheduled word/day reviews | 10 | 20 | 100 | 300 | 1,000 | 3,000 |
| Consistent Learner / consistent_learner | Longest qualifying streak | 1 | 3 | 7 | 30 | 90 | 180 |
| Game Adventurer / game_adventurer | Distinct levels with at least one star | 1 | 5 | 10 | 50 | 150 | 400 |
| Precision Player / precision_player | Distinct unassisted perfect levels | 1 | 3 | 5 | 20 | 75 | 150 |
| Mode Explorer / mode_explorer | Distinct qualifying game modes | 1 | 2 | 3 | 4 | 6 | 8 |
| Story Reader / story_reader | Distinct completed stories | 1 | 2 | 3 | 4 | 7 | 10 |
| Category Scholar / category_scholar | Categories with verified mastery for every current word | 1 | 2 | 3 | 6 | 9 | 12 |
| Community Contributor / community_contributor | Distinct approved submissions | 1 | 3 | 5 | 10 | 25 | 50 |
| Journey Rank / journey_rank | Normalized account level | 2 | 3 | 6 | 10 | 20 | 30 |

There are **11 families and 66 milestones**. Category attainment is retained
when dictionary content grows. Checkpoint lessons do not increase the standard
lesson badge count. Word Search category tracks count as one game mode.
Community contribution is optional and requires moderation/connectivity.

Original earned achievements keep their names and dates under Legacy, including
cloud restoration on a fresh install. Archived display definitions grant no XP
or active tier. Retired
unearned achievements, Six for Six, untracked session challenges and weekly
competitive badges are excluded from the active catalogue.

## Persistence and migration

Room v32 adds reward receipts, a normalization report, persisted celebrations
and versioned XP/profile fields. Completion evidence, tier attainment, bonuses,
level and daily XP settle in one transaction. Celebrations remain pending across
navigation or restart until dismissed. Sign-out clears account data in one
transaction after successfully saving pending rewards.

The baseline imports completed lessons at 20/25 XP, starred game levels at the
provable 5 XP completion base, and completed local stories at 20 XP. Old reviews,
replay dates, hints and perfect-game correctness cannot be reconstructed from
aggregate counters and are not invented. Manual mastery does not become verified
word evidence. Original XP/level are archived in a report; earned legacy badges,
SM-2 schedules, game stars and completed lessons are preserved. Existing story
and learning-section access is retained independently of the recalculated XP.

The report explains the recalculation. Historical counters can remain visible
as old practice statistics; active badge criteria use the new receipt evidence.
The normalization transaction is idempotent.

## Cross-device sync and release

Rewards sync as individual, bounded documents under
`users/{uid}/rewardReceipts/{sha256(receiptId)}`, using canonical IDs and
transactions that merge stronger evidence. Initial reads are paginated;
subsequent reads use server timestamps with an inclusive cursor. Totals are
recomputed from the receipt union rather than adding device aggregates. Review
caps apply after the union.

Version-aware main-progress merge prevents old XP/levels from undoing
normalization. Main progress and leaderboard rules reject a policy downgrade.
Weekly XP comes from this week's non-imported activity receipts; badge bonuses
are excluded. Account deletion removes the reward collection in bounded batches.

Deploy the updated Firestore rules before distributing this APK. This source
change does not deploy rules, publish a release or rewrite live Firebase data.
XP remains client-reported under the existing Spark-plan architecture; this
change provides consistent accounting rather than server-authoritative grading.

## Verification

JVM tests cover reward scales/caps, replay improvement, imported activity,
receipt merging, normalized sync and all tier/level boundaries. Isolated Android
database tests cover historical normalization, repeat eligibility, verified
mastery, atomic completions, persisted celebrations and restoration. The full
Room migration suite validates v1 through v33, including a v31 progress row and
preservation of normalized rewards during the UI cleanup's v32-to-v33 migration.

The combined UI/XP verification checkpoint for 2026-10-01 is recorded in
[UI_CLEANUP.md](UI_CLEANUP.md#resume-checkpoint--2026-10-01).

Commands: `.\gradlew.bat testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest`,
`npm run check:structure`, `npm run check:web`.
