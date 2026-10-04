# Backup and Reset

How KasiGuru's data is backed up, restored, and reset, and why the controls are split the way they
are. Written to be read by someone who has to run it under pressure.

Related: `docs/PHASE1_RUNBOOK.md`, `docs/MONITORING.md`, `docs/AUDIT_PROMPT.md`.

---

## If the database is attacked

Work in this order. Restoring before the attacker is locked out only gives them a second database
to wipe.

1. **Lock the attacker out.**
   - An admin account was used: Firebase console → Authentication → disable that user. Its current
     session can last up to an hour; it cannot sign in again.
   - The service-account key leaked, or you cannot tell how they got in: Google Cloud console →
     IAM & Admin → Service accounts → the `firebase-adminsdk` account → Keys. Add a new key, then
     delete every old one. Use the new key for everything below.
   - Check that the rules in the Firebase console match `firestore.rules` in the repository.
2. **Pick the backup.** The newest folder in the backup folder, or its OneDrive copy, from *before*
   the attack, with a `manifest.json` whose counts look normal. `backup_daily.log` marks a backup
   that looks wiped with `WARNING`, and the scheduled task's last result is then 2.
3. **Dry run** — reads only:
   ```
   node functions/restore_firestore.js <key> <backup-folder>
   ```
   After a full wipe nearly every row is *restore*. *Overwrite* rows are documents changed since the
   backup: vandalism, or your own edits. If only some collections were hit, restore just those with
   `--only=users,leaderboard_public`.
4. **Restore:** the same command with `--confirm=kasiguru-86042`. If it stops part-way, run it
   again: documents already back count as *same* and are skipped.
5. **Check:** take a fresh backup and compare its `manifest.json` with the one you restored from.

Learners need do nothing. Phones keep working offline through the outage, and an empty cloud never
deletes a phone's dictionary (`FirestoreSyncManager.syncVocabulary`). The restore leaves alone any
learner whose phone already synced past the backup, and phones upload whatever is newer the next
time they open the app.

Rehearse this on the emulator before you need it: see *Attack drill* below.

---

## What holds the data

| Store | Contents | Authority |
|---|---|---|
| **Room (on device)** | The learner's own copy of the corpus and all their progress | Source of truth for the app; works with no network |
| **Firestore** | Curated corpus, stories, releases, moderation queues, and each learner's synced progress under `users/{uid}/progress/*` | Source of truth for content; sync target for progress |

The app is offline-first, so a learner's device keeps working if Firestore is unavailable. What a
Firestore loss would destroy is the curated corpus, the moderation queues, and cross-device progress
recovery for anyone who reinstalls.

## Why there is no single "reset everything" button

`firestore.rules` makes learner data **owner-writable only**. An admin may read `users/{uid}` for the
dashboard, but not write it. `security_questions` and `device_tokens` are invisible to admins
entirely. `admin_audit_log` is append-only (`allow delete: if false`).

That is deliberate. It means a compromised admin session cannot rewrite or destroy anyone's learning
record. The cost is that a full reset cannot be a button in a web page — it needs the service-account
key, which is a stronger credential held on one machine and never shipped to a browser.

So the controls are split:

| Task | Where | Credential |
|---|---|---|
| Content + moderation backup | Admin dashboard → Backup & Reset | Admin login |
| Restore content from a file | Admin dashboard → Backup & Reset | Admin login |
| Clear moderation queues | Admin dashboard → Backup & Reset | Admin login |
| **Full backup incl. learner progress** | `functions/backup_firestore.js` | Service-account key |
| **Full restore** | `functions/restore_firestore.js` | Service-account key |
| **Learner or factory reset** | `functions/reset_firestore.js` | Service-account key |

---

## Backup

### Full backup (the one that matters)

```
node functions/backup_firestore.js <service-account.json> [output-dir]
```

Writes `<output-dir>/<timestamp>/<collection>.json` plus a `manifest.json`. Default output is a
`KasiGuruBackups` folder beside the repository folder, deliberately outside the repository
(`C:\KasiGuru\KasiGuruBackups` when the repository is `C:\KasiGuru\KasiGuru-main`).

Every collection is walked **deep**: documents inside subcollections are captured with their full
path, so `users/{uid}/progress/main` restores to exactly where it came from.

Options:

- `KASIGURU_BACKUP_DAILY=1` — skip if a backup for today already exists. Used by the scheduled task.
- `KASIGURU_BACKUP_BUCKET=<bucket>` — also upload the run to Firebase Storage, so a backup survives
  the loss of the machine that made it. Unset means local-only.
- `KASIGURU_DEEP_COLLECTIONS="users"` — which collections own subcollections worth walking. Only
  change this if the schema grows new nested data.

### Daily, automatically

```powershell
.\scripts\register_backup_task.ps1 -KeyFile <service-account.json>
```

Registers the **KasiGuru Daily Backup** task in Task Scheduler. It runs
`functions/backup_daily.js` at noon and 10 minutes after sign-in, without a window, and catches up
after the PC was off. Options: `-At 20:00`, `-Keep 30`, `-MirrorDir <folder>`. Each run:

1. Takes a full backup, unless today's already exists.
2. Checks it for the signs of a wipe: a collection that lost more than half its documents since the
   previous backup, or an older backup still here that is more than twice its size. Either logs
   `WARNING`, names the backup to restore from, and exits with 2 — the second on every run until the
   database is restored.
3. Keeps the newest 14 backups and the oldest of every month, and removes the rest — except a backup
   more than twice the size of the newest, which it holds. Without that, the empty backups taken each
   day after an attack would rotate the last good ones out within two weeks.
4. Copies new backups to `-MirrorDir`, by default `OneDrive\KasiGuruBackups`, and rotates that folder
   the same way. Copied, never mirrored: deleting backups here does not delete the copies.

Everything is logged to `backup_daily.log` in the backup folder. The task's last result is 0 when
all went well, 1 when the backup or the copy failed, 2 for the wipe warning:
`Get-ScheduledTaskInfo -TaskName 'KasiGuru Daily Backup'`. The copy is off-site only while the
OneDrive app is running and signed in.

### Reading a manifest

```json
{
  "projectId": "kasiguru-86042",
  "format": 2,
  "collections":   { "users": 5, "vocabulary": 1246, ... },
  "nestedParents": { "users": 62 }
}
```

`collections` counts real documents. `nestedParents` counts documents that exist only as parents of
a subcollection — they have no fields to save. A `users` count that is *lower* than
`nestedParents.users` is normal and healthy; it means most learners have progress documents but no
fields on the parent.

**A `format` below 2, or no `format` at all, means the backup predates the subcollection fix and
contains no learner progress.** Those backups are still restorable, but only for content.

### Content-only backup from the dashboard

Admin dashboard → **Backup & Reset** → *Export content backup*. Produces
`kasiguru-content-backup-<date>.json` covering vocabulary, stories, story images, announcements,
releases, the three moderation queues, and the admin audit log. It does not cover `word_audio`
(pronunciation clips) or `user_bans`.

Files are `version: 3`: each document is `{ id, data }`, with timestamps, bytes, geopoints and
references tagged (`{"__t": "bytes", "v": "<base64>"}`) the same way the CLI backup tags them.

It does **not** contain learner progress, and says so in the file's `scope` and `note` fields. A
browser cannot enumerate `users/{uid}` documents that have no fields of their own, and that is all of
them — the Web SDK has no `listDocuments()`.

---

## Restore

```
node functions/restore_firestore.js <service-account.json> <backup-dir> [--only=a,b] [--confirm=<projectId>]
```

A **dry run** until `--confirm` matches the key's project id. It reads what is in Firestore now and
prints what the armed run will do with every document, addressed by its full path:

| Action | When | |
|---|---|---|
| restore | missing from Firestore | written back |
| overwrite | present but different | replaced by the backup copy — undoes vandalism, and also your own edits since the backup |
| same | already identical | not written |
| kept | a learner's `progress/main` or leaderboard row already at or past the backup's XP; a device token already present | not written |

*Kept* exists because of the rules' per-write caps (+2,000 XP, +1 streak day, +50 words, +5 levels
over what the cloud holds). After a wipe, phones re-upload their progress into the empty database.
Writing the older backup copy over it would leave each of those phones further ahead than the caps
allow, and every write it made afterwards would be rejected, silently and for good. Other learner
documents are overwritten when they differ: they carry no such caps, and the app merges them back
additively at its next sync.

Documents created *since* the backup are left alone. This is a **roll-forward, not a rollback** — if
you need the database to end at exactly the state of a backup, use a factory reset instead.

Restored `vocabulary` and `stories` get a fresh `updatedAt`, so phones pull them on their next sync.
Running the same restore twice writes nothing the second time. A folder without `manifest.json` is
refused: that backup never finished.

From the dashboard, drop a `.json` content backup onto *Restore from backup*. Same semantics: merge,
not replace. One exception the browser cannot get around: the rules only let a queue item
(`word_submissions`, `literature_submissions`, `issue_reports`) be *created* as the app submits it,
status `pending` with no review fields. A reviewed item that still exists is restored; one that is
gone is skipped, counted in the result, and needs `restore_firestore.js`. Restored `vocabulary` and
`stories` documents get a fresh `updatedAt`, so the app's incremental sync delivers them on the next
pull instead of waiting for its daily full reconcile.

Neither restore path can tell an integer from a whole-number double: JavaScript has one number type,
so a field stored as `3.0` comes back as `3`. Nothing in this schema depends on the difference.

---

## Reset

Both modes are a **dry run** until you pass `--confirm=<projectId>`, and the project id must match
the one inside the service-account key — a key for the wrong project fails loudly rather than wiping
it.

### Clear learner data, keep the corpus

```
node functions/reset_firestore.js <service-account.json> --mode=learner
```

Deletes `users` (including every `progress` subcollection), `leaderboard_public`, `public_profiles`,
`device_tokens`, `security_questions`, `word_submissions`, `literature_submissions`, `issue_reports`.
`public_profiles` was missing from this list until 2026-10-04, so earlier resets left every
learner's public profile behind.

Leaves the dictionary, stories, story images, announcements, and releases untouched.

This is the one to run after a testing period, before a demo, or before handing the project over.

### Factory reset

```
node functions/reset_firestore.js <service-account.json> --mode=factory --from=<backup-dir>
```

Everything `learner` clears, plus the content collections are deleted and rewritten from the backup
you name — so the database ends at exactly that snapshot, with nothing left over.

It **refuses to run** if the backup folder contains no content collections. A factory reset with
nothing to restore from is just a delete, and the script will not do that by accident.

`admin_audit_log` is never cleared by either mode. It is append-only by rule and it is the record of
who ran the reset — erasing it as part of the reset would destroy the evidence of the reset.

---

## Resetting a single device

The app resets its own local data through `UserDataResetManager.resetAllLocalUserData()`, which runs
on sign-out and on account deletion. It flushes any un-synced progress to the cloud first (unless the
account is being deleted), then clears progress, streaks, review schedules, lesson and game history,
and reseeds the initial game levels, achievements, and stories.

A full device wipe is `adb shell pm clear com.kasiguru`, or Android's own *Clear storage*.

---

## Verifying that any of this works

A backup nobody has restored is a hypothesis.

**1. Traversal self-check — no credentials needed, run it any time:**

```
node functions/verify_backup_util.js
```

Twenty-three checks. Thirteen run over a fixture that reproduces the exact shape that broke: users
with no fields owning progress subcollections. They assert that the deep read finds them, that missing
parents are recorded without inventing data, that restore puts nested documents back at their
original paths, that pre-format-2 backups still restore, that the reset counts and deletes the same
documents, children before parents, and that null fields and nanosecond timestamps survive a trip
through JSON. The other ten cover the restore decisions after a wipe, batches staying under the
request size limit, and the rotation and wipe checks of the daily backup.

**2. Attack drill — on the emulator, with no key and no risk to the live project:**

From the repository root, with JDK 21 as `JAVA_HOME`, start the emulator. It loads `firestore.rules`
and listens on `127.0.0.1:8089` (`firebase.json`):

```powershell
npx firebase-tools emulators:start --only firestore --project demo-kasiguru-drill
```

In a second terminal, point the scripts at it. `emulator:demo-…` stands in for the key; the scripts
refuse it unless `FIRESTORE_EMULATOR_HOST` is set and the id starts with `demo-`, so a drill cannot
reach production by mistake.

```powershell
$env:FIRESTORE_EMULATOR_HOST = "127.0.0.1:8089"
$b = "..\KasiGuruBackups\<newest backup folder>"
node functions/restore_firestore.js emulator:demo-kasiguru-drill $b --confirm=demo-kasiguru-drill
node functions/reset_firestore.js emulator:demo-kasiguru-drill --mode=learner --confirm=demo-kasiguru-drill
node functions/restore_firestore.js emulator:demo-kasiguru-drill $b
node functions/restore_firestore.js emulator:demo-kasiguru-drill $b --confirm=demo-kasiguru-drill
node functions/backup_firestore.js emulator:demo-kasiguru-drill $env:TEMP\kasiguru-drill
```

That loads the backup, wipes the learner data, previews the restore, restores it and backs the result
up. The new `manifest.json` should match the backup's, apart from `admin_audit_log`, which gains one
entry for each reset and each restore. Restoring the same backup again should plan zero writes.

Rehearsed on 2026-10-04 with the 2026-10-03 production backup. The wipe was a full delete, and two
phones wrote through the real rules: one 2,500 XP and two streak days past the backup, one
reinstalled from zero. Results:

- The restore planned 818 restores, 1 overwrite (the reinstalled phone) and 1 kept (the phone that
  was ahead), and finished in about 8 seconds on the emulator.
- Both phones' next syncs were accepted.
- With the backup copy written over the phone that was ahead, as the old restore did, its next sync
  was denied.

**3. Reset rehearsal:** run `reset_firestore.js` without `--confirm`. It prints exactly what it would
delete and, in factory mode, what it would restore. The armed run reports the same numbers.

---

## History worth knowing

**Until 2026-10-04 a restore after a wipe would have locked active learners out of syncing.** It
wrote every backup document over whatever was in Firestore. Phones that had already re-uploaded
their progress into the empty database got the older copy back, and the rules' per-write caps then
rejected each of their syncs for good. Found while preparing for an attack; reproduced and verified
fixed on the emulator. The same review found that a restore of `word_audio` would likely have failed
at its first batch: 400 clips of ~29 KB each exceed Firestore's 10 MiB request limit. Batches now
also close by size. Backups also had to be started by hand on the current machine until the
scheduled task in `scripts/register_backup_task.ps1` was added, and `scripts/mirror_backup.cmd`
used `robocopy /MIR`, which would have deleted the off-site copies along with the local ones.

**Every backup taken before 2026-09-03 contains no learner progress.** `db.listCollections()` returns
root collections only, and the paginated read that followed it saw only documents that exist — so
`users` reported `0` while holding every learner's synced progress underneath it. The manifests
looked healthy the entire time. The fix was to walk parents with `listDocuments()`, which returns
missing documents too, and to recurse their subcollections.

If you need data from a pre-fix backup, the content is intact; the progress is not there to recover.

**The dashboard export named the wrong collection.** It read `system_announcements`; the collection is
`announcements` (`AnnouncementRepository.kt`, `firestore.rules`). Announcements were never included in
a dashboard backup, and a restore would have been rejected by the rules. Fixed at the same time.

**Until 2026-09-29 the dashboard could not restore its own backup.** The export ran values through
`JSON.stringify`, which writes a story picture's `Bytes` as its private `{_byteString}` internals.
Restoring wrote that back as a map, the `story_page_images` rule (`data is bytes`) denied it, and the
restore stopped there: vocabulary and stories were written, and announcements, releases, the queues
and the audit log never were. The same export spread each document's id into its fields, so every
story's numeric `id` field was then deleted as if it were the document id, and the app skips a story
without one. Found by running the real dashboard code against the emulator. Version 2 files still
restore: the restore recognises both broken shapes, and Timestamps saved as `{seconds, nanoseconds}`,
and turns them back into the values they were taken from.

**CLI backups before 2026-09-29 drop null fields and keep timestamps only to the millisecond.** A
field set to `null` restores as absent. Nothing in this schema queries on the difference, so those
backups are sound. Newer backups keep both, and remain readable by the older restore script.
