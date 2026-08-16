# Monitoring & Cost Guide (Phase 6)

How to watch KasiGuru in production on the free (Spark) plan, and what to do
before costs ever become a concern.

---

## 1. What to check, and where

| Signal | Where | Frequency |
|---|---|---|
| Crashes | Firebase console → **Crashlytics** | Weekly |
| Firestore reads/writes | Firebase console → **Usage** (per day) | Weekly |
| Firestore storage size | Firebase console → **Usage** | Monthly |
| Dictionary/submission health | Admin portal dashboards | Monthly |
| Backup freshness | `C:\KasiGuru\KasiGuruBackups\` — a folder per day | Daily (scripted) |
| Registered devices | `device_tokens` collection (see below) | Monthly |

Check registered devices and latest backup:

```powershell
cd C:\KasiGuru\KasiGuru-main\functions
node -e "const a=require('firebase-admin');a.initializeApp({credential:a.credential.cert('C:\\Users\\U S E R - P C\\Downloads\\kasiguru-86042-firebase-adminsdk-fbsvc-4677ab3407.json')});a.firestore().collection('device_tokens').get().then(s=>{console.log('devices:',s.size);process.exit(0)})"
Get-ChildItem C:\KasiGuru\KasiGuruBackups -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 3
```

## 2. Cost levers (in order of impact)

1. **Full-collection syncs** — every app launch downloads the whole `vocabulary`
   collection (429 docs today, ~1 read per doc per user per launch). This is the
   dominant cost at scale. The fix (delta sync via a versioned content manifest,
   or moving to a server-side content pipeline) is future work; do not build new
   features that pull the whole dictionary per launch.
2. **Realtime listeners** — the admin panel and the app keep live listeners on
   `vocabulary`/`word_submissions`/`app_releases`. They count reads constantly.
   Fine at current scale; revisit before going to a large audience.
3. **Backups** — the local JSON backup reads every collection each run (one
   run/day). Trivial now.

## 3. Budget alerts (when billing is enabled)

The project runs on the free plan (no billing account), so Google cannot email
you about spending. If you ever upgrade to Blaze:

1. Google Cloud console → **Billing → Budgets & alerts**.
2. Create a budget: amount **$5/month**, alerts at **50% / 90% / 100%**.
3. Optional: Pub/Sub notification to email.

Until then, the Usage page is your tripwire — check it monthly.

## 4. Failure runbook (short version)

- **App can't sync**: check the `KasiGuruAuth`/`FirestoreSyncManager` logs
  (`adb logcat`), then the Usage page for a quota spike.
- **Submissions not arriving**: verify the rules (`firebase deploy --only
  firestore:rules`) and that `word_submissions` accepts anonymous creates.
- **Admin dashboard errors**: confirm the account has the `admin` claim
  (`set_admin_claim.js`) and is signed out/in.
- **Push not delivered**: check `device_tokens` has the device, then send via
  the console test message; verify the app isn't force-stopped with
  notifications denied.
- **Data loss scare**: restore from `C:\KasiGuru\KasiGuruBackups\<date>` using
  `restore_firestore.js` (test on a scratch project first).

## 5. Honest limits of the free plan

Resolved (see `docs/ACCOUNTS_AND_SYNC.md`):

- **Real leaderboard, no Blaze needed**: `leaderboard_public/{uid}` is published by
  the client itself on every progress sync, bounded by `firestore.rules`
  (`isValidLeaderboardEntry`: a single write can't raise `totalXp` by more than
  2000 over the previous value). This is tamper-*resistant*, not anti-cheat — XP is
  still computed on-device and the rules can't verify a quiz was really answered
  correctly, only bound how much damage one write can do. A Cloud-Function-mediated
  version (stronger, but requires Blaze) is straightforward to reintroduce later if
  that tradeoff stops being acceptable.
- **Real multi-device identity**: accounts can be upgraded from anonymous to
  email/password or Google without changing the uid, so progress survives a
  reinstall or a device change.

Still outstanding:

- **Content delta pipeline**: needs a versioned manifest + timestamped docs.
- **True anti-cheat**: would require scoring quizzes server-side.
