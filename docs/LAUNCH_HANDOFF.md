# Public launch handoff (2026-10-07)

For Anthony, picking up the last checks before KasiGuru goes public. The web app is live and
matches APK 1.25.0. The checklist below records the launch checks and their remaining phone tests.

## Execution update (2026-10-07, Anthony's PC)

- **Announcements index deployed:** compared the live indexes first, retained both leaderboard
  indexes, deployed only `firestore:indexes` without `--force`, and verified all three `READY`.
  The exact Android `active == true`, `createdAt DESC`, limit-5 query succeeded. There are no active
  announcements, so an on-phone Home → News check remains pending; no test announcement was published.
- **Spark retained:** Anthony explicitly chose to keep Spark, and the billing API still reports
  `billingEnabled: false`.
- **Usage report verified:** both daily and hourly modes work through the Firebase CLI sign-in.
  The still-open **2026-10-06 Pacific quota day had 47,354 reads (95%)** at the check on the Philippine
  morning of 2026-10-07. Earlier quota days reached 53,836 (Oct 1), 62,008 (Oct 4), and 55,475 (Oct 5).
  `npm run firebase:usage` is now the daily entry point; `-- --hours` shows the current day by PH hour.
- **Read reductions recorded in this commit:** the admin dictionary uses a saved per-admin copy and an
  `updatedAt` delta listener, with deletion checks and daily reconciliation. Android boards refresh
  at most once per three minutes per account/week; the existing web cache now handles account/week
  changes and concurrent requests. Publishing these code changes remains pending; Android's
  cache change also needs a future APK release. The live index fix applies to APK 1.25.0 already.
- **Validation passed:** 140 web tests, 14 launch cache/report tests, web type checking and production
  build (without a Firestore content export), structure/brand/parity checks, and Android
  `testDebugUnitTest assembleDebug lintDebug`. Production `/`, `/__/auth/handler`, and `/__/auth/iframe`
  returned HTTP 200, with no app CSP on the two Firebase routes.
- **Real-phone smoke test pending:** ADB reported no connected phone or emulator. HTTP endpoint and
  automated checks do not verify Google sign-in in installed iPhone Safari or web/APK progress sync.
  Use task 3 below and record actual results before treating launch verification as complete.

## Where things stand

- **Web app:** https://kasiguru-app.vercel.app, deployed from `main` by Vercel
  (`admin-website/webapp`, see [WEB_APP.md](WEB_APP.md)).
- **Last web commit:** `198d79fc` "badge art as on the APK, and My words from 1.22", on top of your
  PR #37 (sounds, music, tour). Badge art is now everywhere a badge appears (onboarding wall,
  Badges page, Me, public profiles) and the Library has My words. [WEB_PARITY.md](WEB_PARITY.md)
  is up to date through 1.25.0.
- **Checks passing at that commit:** `tsc`, the 133 webapp tests, `npm run check:web`,
  `npm run check:web-sync`, production build. Screens checked at 390 px and 1280 px, dark and light.

**Original launch files** (all committed in `f1402d96`, then pulled onto Anthony's PC):

| File | What it is | State |
|---|---|---|
| `firestore.indexes.json` | Adds the `announcements` composite index | Deployed and `READY` (task 1) |
| `scripts/diagnostics/firestore-usage.js` | Read-only usage report per quota day | Daily and hourly modes verified (task 2) |
| `docs/LAUNCH_HANDOFF.md` | This file | |

The other untracked files in the repo (manuscript `.docx`, `docs/paper/`, badge/category source
art under `design/assets/`, `scripts/audio/elevenlabs-music.js`) are the owner's work in progress.
Leave them out of commits unless asked.

---

## Task 1 — Announcements index (Android banner never shows)

**Problem.** `AnnouncementRepository.kt` queries `announcements` with
`whereEqualTo("active", true).orderBy("createdAt", DESC).limit(5)`. That needs a composite index
the project does not have, so the query fails with `FAILED_PRECONDITION`, the error is swallowed
(`emit(emptyList())`), and Android **never shows any announcement**. The web app is unaffected (it
filters and sorts in the browser, `webapp/src/lib/remote.ts`).

**Done.** The index is added to `firestore.indexes.json`:

```json
{ "collectionGroup": "announcements", "queryScope": "COLLECTION",
  "fields": [ { "fieldPath": "active", "order": "ASCENDING" },
              { "fieldPath": "createdAt", "order": "DESCENDING" } ] }
```

The two `leaderboard_public` indexes already in the file match the live project exactly (checked
with `firebase firestore:indexes`), so deploying the file adds one index and removes nothing.

**Deployment completed; phone display check remains.** The commands below are the repeatable runbook.

1. Confirm the live indexes still match the file (nothing extra live that the deploy would offer
   to delete):
   ```powershell
   firebase firestore:indexes --project kasiguru-86042
   ```
2. Deploy **only** the indexes. Do not add `--force`; if the CLI asks to delete an index, answer
   no and stop.
   ```powershell
   firebase deploy --only firestore:indexes --project kasiguru-86042
   ```
3. Wait until the index shows **Enabled** in Firebase console → Firestore → Indexes (a few
   minutes; the collection is tiny).
4. Test: create an active announcement in the admin portal, open the APK's Home → it should appear
   under **News**. No APK release is needed; the query is already in 1.25.0.
5. `firestore.indexes.json` is already committed. `WEB_APP.md` now records the deployed index and
   successful query; include that documentation with the launch-read changes.

---

## Task 2 — Firestore free-plan read budget (the real launch risk)

**Problem.** The project is on **Spark** (billing disabled, confirmed with
`gcloud billing projects describe`). Spark allows **50,000 reads a day** for the whole project
(Android + web + admin portal + scripts), resetting at **midnight Pacific = 3 PM (4 PM in PH
winter) Philippine time**. Past the limit, every read fails until the reset: Android keeps working
offline, but sync, leaderboards, announcements and the web app's content refresh stop.

**We are already at the limit before launch** (Cloud Monitoring, `document/read_ops_count`,
rolling 24 h windows):

| 24 h ending (UTC) | Reads |
|---|---|
| 2026-10-07 02:20 | 44,480 |
| 2026-10-06 02:20 | **65,079** |
| 2026-10-05 02:20 | **50,091** |

Hour by hour, the reads come in bursts during working hours (18:00–00:00 PH time, up to 26,826 in
one hour) and are near zero overnight, so today's load is **development, not learners**:

| Source | Cost |
|---|---|
| Admin portal open | Was ~1,200 reads (a whole-`vocabulary` listener). Now about 50 with a saved dictionary; the full ~1,150 only on a first visit, a weekly backstop, or a fingerprint mismatch, per admin and browser |
| Each production web deploy | ~1,200+ reads: `webapp/scripts/export-content.mjs` snapshots `vocabulary` and `stories` |
| Bulk dictionary scripts / audio uploads | Large; see [DICTIONARY_MEANINGS.md](DICTIONARY_MEANINGS.md) and `scripts/README.md` |
| Learner, per active day (estimate) | ~100–300: leaderboard 50 per tab viewed (Android `LeaderboardRepository` listener, web `TOP_N = 50`), progress sync (5 docs + new receipts), announcements, ban check |
| Android dictionary sync | Cheap: incremental listener (`whereGreaterThan(updatedAt)`) + fingerprint aggregation; full re-read at most every 30 days |

So on launch day the budget left for learners is whatever development does not use, roughly a
couple of hundred active learners on a quiet dev day and close to none on a busy one.

[MONITORING.md](MONITORING.md) now records the current delta sync, caching and daily launch monitoring.

**To do, in order.**

1. **Run the usage report** and check it works (it was written but not run yet):
   ```powershell
   node scripts/diagnostics/firestore-usage.js            # last 7 quota days, % of each limit
   node scripts/diagnostics/firestore-usage.js --hours    # today, hour by hour, PH time
   ```
   **Verified in both modes.** It uses `gcloud auth login` or an existing Firebase CLI sign-in;
   it reads Cloud Monitoring, never documents. `firebase:usage` is added to the root `package.json`
   and documented under `diagnostics/` in `scripts/README.md`. The raw query it makes,
   if you need to check by hand:
   ```bash
   curl -s -G "https://monitoring.googleapis.com/v3/projects/kasiguru-86042/timeSeries" \
     -H "Authorization: Bearer $(gcloud auth print-access-token)" \
     --data-urlencode 'filter=metric.type="firestore.googleapis.com/document/read_ops_count"' \
     --data-urlencode "interval.startTime=2026-10-04T00:00:00Z" \
     --data-urlencode "interval.endTime=2026-10-07T00:00:00Z" \
     --data-urlencode "aggregation.alignmentPeriod=3600s" \
     --data-urlencode "aggregation.perSeriesAligner=ALIGN_SUM" \
     --data-urlencode "aggregation.crossSeriesReducer=REDUCE_SUM"
   ```
2. **Owner decision: stay on Spark (confirmed 2026-10-07).** A later move to Blaze is their call (it needs a
   card). Blaze keeps the same free 50k reads a day and bills only beyond it, at cents per 100k
   reads; with a budget alert ([MONITORING.md §3](MONITORING.md), $5/month, alerts at 50/90/100%)
   a launch spike costs little instead of cutting everyone off. Note that some project notes assume
   Spark on purpose (e.g. FCM from the admin portal), so check those before switching.
3. **Whatever the plan, cut the development reads**, cheapest first:
   - **Launch week: freeze bulk dictionary work and avoid needless web deploys.** Pushes that touch
     only Android already skip the web build (`vercel.json` `ignoreCommand`).
   - **Admin portal:** `admin/js/vocabulary-cache.mjs`, connected in `app.js` and live since
     2026-10-07. Cached rows survive reloads; the delta preserves unchanged words. A fingerprint
     (count and sums) on opening and every five visible minutes catches withdrawals and tagger
     runs; the full read that was daily is now weekly, since it was most of the remaining cost.
     The users list and activity log wait for their tabs. Local deletions update all dictionary
     views and the cache immediately.
   - **Leaderboards:** Android uses a three-minute refresh gate over Room. The web's existing
     three-minute cache now separates accounts/weeks and coalesces concurrent requests. Tests cover
     expiry, account/week switches, tied ranks, and failure/reconnect paths.
4. **Watch it daily for the first week** with the report (or Firebase console → Firestore →
   Usage). A day over 70% of a limit is flagged "near limit".

---

## Task 3 — Signed-in smoke test on real phones (not done yet)

Nothing in the 2026-10-07 work was tested signed in. About 5 minutes, on production:

1. **iPhone, Safari** → https://kasiguru-app.vercel.app → sign in with Google → finish one lesson.
2. Open the **APK** with the same account → XP, streak and the lesson should be there.
3. Do one thing on the APK (a review) → reload the web app → it should appear there too.
4. On the iPhone: **Share → Add to Home Screen**, open from the icon, sign out and back in with
   Google. This is the path that breaks if the auth domain setup changes (see WEB_APP.md,
   "Google sign-in setup").
5. Library → **My words**: after the lesson, its words are listed; Home's **My words** tile
   opens it; Me shows **Words met**.

If an installed web app looks old, close and reopen it once; the service worker updates on the next
open.

---

## Loose ends

- A git stash `mywords-wip` holds an earlier copy of the My words work, already merged into
  `198d79fc`. Safe to drop: `git stash list`, then `git stash drop` on that entry.
- `C:\KasiGuru\tmp-shots\` holds screenshots from the visual check; safe to delete.
- Still deliberately different between web and Android: [WEB_APP.md](WEB_APP.md), "Deliberate
  differences". After launch, the open product work is the five *Coming soon* games and narrated
  stories, in both apps.
