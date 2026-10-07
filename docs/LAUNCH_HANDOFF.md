# Public launch handoff (2026-10-07)

For Anthony, picking up the last checks before KasiGuru goes public. The web app is live and
matches APK 1.25.0; three things are left, in the order below. None of them is code-heavy, but
two of them touch the live Firebase project, so they were left for a person to run.

## Where things stand

- **Web app:** https://kasiguru-app.vercel.app, deployed from `main` by Vercel
  (`admin-website/webapp`, see [WEB_APP.md](WEB_APP.md)).
- **Last web commit:** `198d79fc` "badge art as on the APK, and My words from 1.22", on top of your
  PR #37 (sounds, music, tour). Badge art is now everywhere a badge appears (onboarding wall,
  Badges page, Me, public profiles) and the Library has My words. [WEB_PARITY.md](WEB_PARITY.md)
  is up to date through 1.25.0.
- **Checks passing at that commit:** `tsc`, the 133 webapp tests, `npm run check:web`,
  `npm run check:web-sync`, production build. Screens checked at 390 px and 1280 px, dark and light.

**Uncommitted in the working tree** (on the PC this was written on):

| File | What it is | State |
|---|---|---|
| `firestore.indexes.json` | Adds the `announcements` composite index | Ready; needs deploying (task 1) |
| `scripts/diagnostics/firestore-usage.js` | Read-only usage report per quota day | Written, **not yet run once** (task 2) |
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

**To do.**

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
5. Commit `firestore.indexes.json`, and replace the "Announcements need no index here" section in
   `WEB_APP.md` (it says the index does not exist).

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
| Admin portal open | ~1,200 reads: `admin/js/app.js:394` keeps an `onSnapshot` on the **whole** `vocabulary` collection |
| Each production web deploy | ~1,200+ reads: `webapp/scripts/export-content.mjs` snapshots `vocabulary` and `stories` |
| Bulk dictionary scripts / audio uploads | Large; see [DICTIONARY_MEANINGS.md](DICTIONARY_MEANINGS.md) and `scripts/README.md` |
| Learner, per active day (estimate) | ~100–300: leaderboard 50 per tab viewed (Android `LeaderboardRepository` listener, web `TOP_N = 50`), progress sync (5 docs + new receipts), announcements, ban check |
| Android dictionary sync | Cheap: incremental listener (`whereGreaterThan(updatedAt)`) + fingerprint aggregation; full re-read at most every 30 days |

So on launch day the budget left for learners is whatever development does not use, roughly a
couple of hundred active learners on a quiet dev day and close to none on a busy one.

[MONITORING.md](MONITORING.md) is the general guide but is out of date here (it still describes a
full `vocabulary` download per launch and a 429-word dictionary).

**To do, in order.**

1. **Run the usage report** and check it works (it was written but not run yet):
   ```powershell
   node scripts/diagnostics/firestore-usage.js            # last 7 quota days, % of each limit
   node scripts/diagnostics/firestore-usage.js --hours    # today, hour by hour, PH time
   ```
   It needs only `gcloud auth login` on the machine; it reads Cloud Monitoring, never documents.
   If it works, add `"firebase:usage": "node scripts/diagnostics/firestore-usage.js"` to the root
   `package.json` and a line under `diagnostics/` in `scripts/README.md`. The raw query it makes,
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
2. **Decide with the owner: stay on Spark or move to Blaze.** This is their call (it needs a
   card). Blaze keeps the same free 50k reads a day and bills only beyond it, at cents per 100k
   reads; with a budget alert ([MONITORING.md §3](MONITORING.md), $5/month, alerts at 50/90/100%)
   a launch spike costs little instead of cutting everyone off. Note that some project notes assume
   Spark on purpose (e.g. FCM from the admin portal), so check those before switching.
3. **Whatever the plan, cut the development reads**, cheapest first:
   - **Launch week: freeze bulk dictionary work and avoid needless web deploys.** Pushes that touch
     only Android already skip the web build (`vercel.json` `ignoreCommand`).
   - **Admin portal:** replace the whole-collection `onSnapshot` on `vocabulary` with a cached copy
     plus an `updatedAt > lastSeen` delta, as the web app and Android already do. That is the
     largest single saving and only affects admins.
   - **Leaderboards:** cache each tab's top 50 for a few minutes on both clients instead of
     re-reading on every view.
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
