# Accounts, Progress Sync & Releases

How a KasiGuru player keeps their progress across reinstalls and devices, how the
leaderboard is built, and what has to be configured for both to work.

---

## 1. What was wrong before

| Symptom | Root cause |
|---|---|
| Reinstalling the APK reset progress to zero | Identity was **anonymous Firebase Auth only**. An anonymous uid lives in app-private storage, so uninstalling destroyed it and the next launch minted a new one. The old `users/{uid}/progress` document still existed but was unreachable — orphaned, not deleted. |
| Badges / level stars / learned words lost even when XP survived | Only `user_progress` was synced. `achievements`, `game_levels` and the per-word SRS fields never left the device. |
| Leaderboard rankings looked wrong | The leaderboard never read other users at all. It listed **hardcoded seed rows** ("Ligaya Santos", 850 XP …) from `DatabaseSeeder.getInitialLeaderboard()` with the local player spliced in. |
| "Weekly XP" tab behaved like All-Time | `LeaderboardViewModel` had no branch for it and fell through to the All-Time case. |
| Update banner rarely appeared | It was emitted **outside** the Home screen's scrolling `Column`, so it rendered as an overlapping sibling instead of in the layout. |
| Every release was optional | The admin publish form never wrote a `forceUpdate` field, so the app always deserialized it as `false`. |
| "Later" on an update reappeared constantly | Dismissal was in-memory only and reset on every app start. |

## 2. How identity works now

```
anonymous session ──link (same uid)──► permanent account (email/password or Google)
                                              │
   reinstall / new device ──sign in───────────┘
```

The **Firebase Auth uid is the primary key** for everything: `users/{uid}/progress/*`,
`device_tokens/{uid}`, `leaderboard_public/{uid}`, and the `uid` stamped on word
submissions.

`linkWithCredential` **keeps the same uid**, so upgrading a guest to a real account
moves no data — the documents it already owns simply become reachable again after a
future sign-in. This is the whole migration strategy for existing players: nothing is
rewritten, copied or deleted.

`KasiGuruApp` only signs in anonymously when there is **no** session at all
(`KasiGuruApp.kt`). Calling `signInAnonymously()` unconditionally — as it did before —
would replace a signed-in account with a throwaway one.

### Merge rules (why a reinstall cannot wipe you)

`ProgressSyncManager` pulls before it pushes, and every merge is **additive**:

- counters (`totalXp`, streaks, `wordsLearned`, …) → `maxOf(local, remote)`
- achievements → unlocked on either side stays unlocked; earliest unlock date wins
- game levels → best star count; unlocked is never re-locked
- word review state → `isLearned` sticks; the side with more reviews owns the SM-2 schedule
- profile text fields → from whichever side has the newer `updatedAt`

A fresh install therefore contributes nothing but receives everything. Merge logic is
pure and unit-tested (`ProgressSyncTest`, `LearningStateMergeTest`).

Words are keyed by **lowercased Kasiguranin text**, not the Room row id — ids are
locally auto-generated and differ per install (matching how `FirestoreSyncManager`
already maps dictionary content).

### What is *not* synced, deliberately

`game_scores` (the per-session history log) stays device-local. It is unbounded,
append-only, and nothing reads it cross-device; the aggregates that matter
(`gamesPlayed`, `totalCorrectAnswers`) live in `user_progress` and do sync.

## 3. Leaderboard

`leaderboard_public/{uid}` is written **only** by the `syncLeaderboardEntry` Cloud
Function, which triggers on writes to `users/{uid}/progress/main`. Clients can read it
but never write it (`firestore.rules`), so a rank can only follow from the user's own
progress document.

The function copies out just `displayName`, `totalXp`, `level`, `currentStreak`,
`profileIconId`, `titleBadge`, `weeklyXp` — never email, address or age.

**Tamper-resistance, not anti-cheat.** XP is still computed on-device. The function
clamps any single write that jumps more than `MAX_XP_DELTA_PER_WRITE` (2000) XP and
logs it, so a sync glitch or casual edit cannot mint a #1 rank. It cannot verify that a
quiz was genuinely answered correctly — that would require scoring server-side.

`resetWeeklyLeaderboard` zeroes `weeklyXp` every Monday 00:05 Asia/Manila.

## 4. Releases and update notifications

`app_releases` is the single source of truth, read by **both** the in-app check
(`AppUpdateRepository` → `HomeViewModel.checkForUpdate`) and the public download page
(`admin-website/download/js/download.js`) with the same ordering, so the app and the
website always agree on "latest".

| Concept | Where it comes from |
|---|---|
| Latest version | highest `versionCode` in `app_releases` |
| Current installed version | `BuildConfig.VERSION_CODE` / `VERSION_NAME` (shown in Settings → About) |
| Update available | latest `versionCode` > installed |
| Required update | `forceUpdate: true` — banner has no "Later" button |

Optional updates can be dismissed once; the dismissed `versionCode` is stored in
DataStore, so the banner stays hidden until a **newer** release is published. Required
updates always show.

**Publishing a release:**

1. Bump `versionCode` **and** `versionName` in `app/build.gradle.kts`. `versionCode`
   must increase — it is what the update check compares.
2. Build and sign the APK, upload it, and get a direct `https://` link.
3. Admin dashboard → *App Release & APK Store Manager* → fill the form. Tick
   **Required update** only for critical releases.

## 5. Setup required for these features

Steps that must be done in the Firebase console (they cannot be scripted from here):

1. **Authentication → Sign-in method**: enable **Email/Password** and **Google**.
   Without this, account creation fails with `CONFIGURATION_NOT_FOUND`.
2. **Google Sign-In** additionally needs the app's SHA-1/SHA-256 fingerprints
   registered on the Android app in project settings, then a fresh
   `google-services.json` copied to `app/`. Until that file contains an `oauth_client`
   entry the Google button **hides itself automatically** (`rememberGoogleSignIn`
   returns null) — email/password still works.
3. **Blaze (pay-as-you-go) billing** for Cloud Functions. Free-tier quotas cover the
   current scale; set a budget alert (see `MONITORING.md`).

Then deploy:

```powershell
firebase deploy --only firestore:rules
firebase deploy --only functions

# One-off: publish leaderboard rows for users who already had progress before the
# trigger existed. Dry run first (no --apply), then:
cd functions
node backfill_leaderboard.js C:\path\to\service-account.json --apply
```

`backfill_leaderboard.js` reads `users/{uid}/progress/main` and writes only
`leaderboard_public/{uid}`. It never modifies or deletes progress.

## 6. Data safety notes

- No migration deletes or rewrites user progress. The only destructive statement in the
  Room chain is migration **19 → 20**, which clears the `leaderboard` table — that table
  held the fake seeded competitors and is now just an offline cache of the real board.
- Take a backup before deploying rules or functions:
  `node backup_firestore.js <service-account.json>`.
- Players who reinstalled **before** this release cannot be reattached automatically:
  their old uid is gone and no email was ever captured to match on. Their orphaned
  `users/{uid}/progress` documents are still in Firestore and can be recovered manually
  if someone contacts you and their old XP/level can be identified.
