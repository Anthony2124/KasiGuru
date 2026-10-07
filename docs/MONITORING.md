# Monitoring & Cost Guide (Phase 6)

How to watch KasiGuru in production on the free (Spark) plan, and what to do
before costs ever become a concern.

---

## 1. What to check, and where

| Signal | Where | Frequency |
|---|---|---|
| Crashes | Firebase console → **Crashlytics** | Weekly |
| Firestore reads/writes | `npm run firebase:usage` or Firebase console → **Usage** | Daily during launch week |
| Firestore storage size | Firebase console → **Usage** | Monthly |
| Dictionary/submission health | Admin portal dashboards | Monthly |
| Backup freshness | `C:\KasiGuru\KasiGuruBackups\` — a folder per day | Daily (scripted) |
| Registered devices | `device_tokens` collection | Monthly |

Check usage without reading Firestore documents:

```powershell
npm run firebase:usage
npm run firebase:usage -- --hours
```

## 2. Cost levers (in order of impact)

1. **Development reads** — production web builds snapshot the ~1,200-word dictionary
   and stories; bulk maintenance and backups also read collections. During launch week,
   freeze bulk dictionary work and avoid repeated production deployments. CI and preview
   builds use the committed content snapshot.
2. **Admin dictionary** — the portal saves a copy per admin and listens to stamped edits.
   It performs cheap count checks while visible and a daily full reconciliation, instead
   of attaching a whole-dictionary listener on every opening.
3. **Leaderboards** — both clients reuse each top-50 board for three minutes per account/week.
   A refresh also fetches the signed-in learner's row and counted rank when outside the top 50.
4. **Learner dictionary sync** — Android uses `updatedAt` deltas and a daily fingerprint
   aggregation, reading the full collection only when needed or after 30 days. The web
   uses a bundled snapshot plus deltas at most every six hours.

The report uses the existing gcloud or Firebase CLI sign-in, groups by Pacific quota day,
and flags any operation at 70% of its daily free limit. It reads only Cloud Monitoring.
The 2026-10-07 launch check found 47,354 reads (95%) for the still-open 2026-10-06 Pacific day;
three of the preceding days exceeded 50,000 reads. Spark remains the owner's chosen plan.

## 3. Budget alerts (when billing is enabled)

The project runs on the free plan (no billing account), so Google cannot email
you about spending. If you ever upgrade to Blaze:

1. Google Cloud console → **Billing → Budgets & alerts**.
2. Create a budget: amount **$5/month**, alerts at **50% / 90% / 100%**.
   A budget alert is a notification, not a spending cap.
3. Optional: Pub/Sub notification to email.

Until then, the usage report or Usage page is your tripwire — check it daily during launch week.

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

- **Connected content**: additional authored sentences, recordings and narrated stories need speakers.
- **True anti-cheat**: would require scoring quizzes server-side.
