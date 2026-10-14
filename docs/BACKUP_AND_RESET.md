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
   the attack, with a `manifest.json` whose counts look normal; or a `.zip` downloaded from the
   portal. `backup_daily.log` marks a backup that looks wiped with `WARNING`, and the scheduled
   task's last result is then 2.
3. **Preview** — reads only. In the portal: **Backup & Restore**, drop the service-account key, drop
   the backup folder or zip, **Preview restore**. From a terminal:
   ```
   node functions/restore_firestore.js <key> <backup-folder>
   ```
   After a full wipe nearly every row is *restore*. *Overwrite* rows are documents changed since the
   backup: vandalism, or your own edits. If only some collections were hit, untick the others (or
   pass `--only=users,leaderboard_public`).
4. **Restore:** type `kasiguru-86042` and press **Restore** (or run the same command with
   `--confirm=kasiguru-86042`). If it stops part-way, preview and restore again: documents already
   back count as *same* and are skipped.
   If the preview fails with **Quota exceeded**, the free plan's 50,000 reads for the day are used
   up. They reset at midnight Pacific time (3 PM in the Philippines). Writes have their own limit and
   still work, so the learner data can come back now without reading: the page offers **Set up a
   restore of learner data without reading**, which ticks the learner collections and *Don't read
   Firestore first*. From a terminal:
   ```
   node functions/restore_firestore.js <key> <backup-folder> --only=users,leaderboard_public,public_profiles,device_tokens,security_questions,word_submissions,literature_submissions,issue_reports --write-only --confirm=kasiguru-86042
   ```
5. **Check:** take a fresh backup and compare its counts with the backup you restored from.

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

## Why everything needs the service-account key

`firestore.rules` makes learner data **owner-writable only**. An admin may read `users/{uid}` for the
dashboard, but not write it. `security_questions` and `device_tokens` are invisible to admins
entirely. `admin_audit_log` is append-only (`allow delete: if false`).

That is deliberate. It means a compromised admin session cannot rewrite or destroy anyone's learning
record. The cost is that backup, restore and reset need the service-account key, a stronger
credential that security rules do not apply to. An admin login alone cannot take a full backup or put
learner data back, and the rules are not loosened to let it.

The key is used in one of two places:

- **The portal's Backup & Restore page** (admin-only). The operator drops the key file on the page.
  The browser signs a one-hour Google access token with it and talks to Firestore's REST API
  directly, so **Download full backup**, the drag-and-drop **Restore** and the **Factory reset** need
  no terminal. The key is imported as a non-extractable WebCrypto key, never uploaded or saved, and
  forgotten with the tab or on **Forget key**. A key for any other project is refused. Without the key
  the page does nothing, so a stolen admin login still cannot back up, restore or wipe anything from
  it. The trade-off is that the key's contents pass through a browser tab, and that with it the page
  can delete the whole database: load it only on your own computer, and only on the real portal
  address.
- **The operator scripts in `functions/`**, for the scheduled daily backup, for the learner reset,
  and as a fallback for the rest.

The page and the scripts read and write the same backup format, so either restores the other's
backups. Keep the page and this runbook in step: `admin-website/admin/dashboard.html` `#tab-backup`,
`js/backup-page.mjs`, `js/database-backup.mjs`, `js/firestore-rest.mjs`, `js/zip.mjs`.

| Task | Where | Credential |
|---|---|---|
| **Daily full backup**, wipe check and OneDrive copy | Task Scheduler → `functions/backup_daily.js` | Service-account key |
| **One full backup now** | Portal → **Download full backup**, or `functions/backup_firestore.js` | Service-account key |
| **Restore** | Portal → drop a backup → Preview → Restore, or `functions/restore_firestore.js` | Service-account key |
| **Factory reset** | Portal → drop a backup → Preview → Factory reset, or `functions/reset_firestore.js --mode=factory` | Service-account key |
| **Learner reset** | `functions/reset_firestore.js --mode=learner` | Service-account key |

---

## Backup

### From the portal

Backup & Restore → drop the service-account key → **Download full backup**. The browser reads every
collection the way `backup_firestore.js` does, deep and with missing parents recorded, and saves
`kasiguru-backup-<timestamp>.zip`. Inside is one `<timestamp>` folder holding the same
`<collection>.json` files and `manifest.json` the script writes, so unzipped it is a backup folder
like any other. A full backup reads every document once, a few thousand of the day's 50,000 reads.
The download is logged in the activity log as `database.backup`.

### Full backup from a terminal

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

---

## Restore

In the portal: Backup & Restore → drop the service-account key → drop a backup on the Restore zone (a
zip from the page, a zip of a backup folder, or the folder itself; **choose a backup folder** opens a
folder picker) → tick the collections → **Preview restore** → type the project id → **Restore**.
From a terminal:

```
node functions/restore_firestore.js <service-account.json> <backup-dir> [--only=a,b] [--confirm=<projectId>]
```

Both make the same decisions; the page's copy of them (`database-backup.mjs`) is checked against the
script's by `verify_backup_util.js`. The script is a **dry run** until `--confirm` matches the key's
project id; the page previews first and writes only once the project id is typed. It reads what is in Firestore now and
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

On the page, minutes may pass between the preview and the click on **Restore**, and a phone can sync
in between. So just before writing, the page reads again every planned progress, leaderboard and
device-token write and keeps any that a phone has since moved past. That costs one read for each.

Documents created *since* the backup are left alone. This is a **roll-forward, not a rollback** — if
you need the database to end at exactly the state of a backup, use a factory reset instead.

Restored `vocabulary` and `stories` get a fresh `updatedAt`, so phones pull them on their next sync.
Running the same restore twice writes nothing the second time. A folder without `manifest.json` is
refused: that backup never finished.

`--write-only` skips reading Firestore and writes every document of the `--only` collections, which
it requires. Use it when reads are refused (`Quota exceeded`) right after a wipe. With nothing to
compare against, it keeps nothing, so a phone that had already synced past the backup is
overwritten. That is harmless minutes after the backup and risky days later. Run the normal
comparison once reads are back, and it should plan zero writes.

The restore cannot tell an integer from a whole-number double: JavaScript has one number type, so a
field stored as `3.0` comes back as `3`. Nothing in this schema depends on the difference.

The restore reads backups only: a folder with `manifest.json`, or on the page a zip of one. A
`kasiguru-content-backup-<date>.json` file exported by the old dashboard page has no manifest and is
refused; every daily backup folder holds the same content and more. Dropping a folder that holds
several backups, such as the whole `KasiGuruBackups` folder, is refused before anything is read.

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
you name: `vocabulary`, `word_audio`, `vocabulary_trash`, `dictionary_updates`, `stories`,
`story_page_images`, `announcements`, `app_releases` and `sentence_submissions`. The content ends at
exactly that snapshot, with nothing left over. Learner data is deleted, not restored.

It **refuses to run** if the backup folder contains no content collections. A factory reset with
nothing to restore from is just a delete, and the script will not do that by accident. A content
collection the backup has no file for is still emptied; the dry run marks it *not in this backup*.

Neither mode touches `admin_staff`, `user_bans` or `user_flags`, so verifiers keep their access and
blocked learners stay blocked. `admin_audit_log` is never cleared by either mode. It is append-only by rule and it is the record of
who ran the reset — erasing it as part of the reset would destroy the evidence of the reset.

#### From the portal

Backup & Restore → load the key → **Download full backup** → drop the backup to reset to on
**Factory reset** → **Preview factory reset** → tick that you have a backup from just before → type
the project id → **Factory reset**. Then put the learners back: **Restore learner data from this
backup** sets up Restore with *Learner data only* ticked, or drop the backup from just before the
reset there yourself if you reset to an older one. Preview, type the project id, **Restore**.

The page does what `--mode=factory` does, with the same lists of collections (`database-backup.mjs`,
checked against `firestore_backup_util.js` by `verify_backup_util.js`). The preview reads every
document once and lists, per collection, what is deleted and what is written back. The reset reads
each collection again, so whatever was written after the preview goes too, then deletes and writes.
Restored content keeps the backup's own `updatedAt`, as the script writes it. The reset is logged as
`database.factory_reset`.

Keep the tab open until it finishes: closing it stops the reset part-way, and the page asks first. If
it stops part-way, for example with **Quota exceeded**, it says how many documents were deleted and
offers **Set up a restore of the content without reading** for any content collection it had started
deleting. Writes have their own limit, so the content can come back the same day. Run the factory
reset again once reads are back.

On the live database the sequence costs roughly: the backup, a read of every document; the preview
and the reset, a read of every document each; the learner restore afterwards, little, since the
collections it compares against are empty. With about 10,300 documents that is some 30,000 of the
day's 50,000 reads, plus about 8,700 deletes and 9,000 writes of the 20,000 each. One factory reset
and restore a day fits; two do not.

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

Twenty-six checks. Thirteen run over a fixture that reproduces the exact shape that broke: users
with no fields owning progress subcollections. They assert that the deep read finds them, that missing
parents are recorded without inventing data, that restore puts nested documents back at their
original paths, that pre-format-2 backups still restore, that the reset counts and deletes the same
documents, children before parents, and that null fields and nanosecond timestamps survive a trip
through JSON. Ten cover the restore decisions after a wipe, batches staying under the request size
limit, and the rotation and wipe checks of the daily backup. The last three check that the portal's
copy of the restore decisions, its factory reset's lists of collections and its REST reading of
values agree with this script.

The portal's own modules have tests that CI runs, with no network or credentials:
`node --test scripts/tests/database-backup.test.cjs`. They cover the zip, the value format, the deep
read, the restore decisions, the re-check before writing, the factory reset's preview, order and
partial stop, batching and retries, and the key's token signing.

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

The portal page was rehearsed the same way on 2026-10-13, against seeded data with nanosecond
timestamps, GeoPoints, references, 300 KB audio bytes, nulls, an empty document, 650 words (more than
one page) and document ids containing spaces, `#?&` and `%`:

- A backup from the page and one from `backup_firestore.js` held the same documents in the same order.
- Windows' *Extract All* opened the page's zip, and the page read a zip made by `Compress-Archive`.
- After a learner reset, with a word vandalised and one phone re-uploaded past the backup, the page's
  restore kept that phone, put the rest back, and `restore_firestore.js` then planned zero writes.
  The reverse also held: the script restored from the page's zip, and the page then planned zero writes.
- The page itself was driven in headless Chrome: key refused for another project, key loaded and
  signed with WebCrypto, backup downloaded, zip dropped, preview, typed confirmation, restore.

The page's factory reset was rehearsed on 2026-10-14 with the 2026-10-14 production backup, after
changing every content collection on the emulator (a clip added and one edited, a recycle-bin entry
emptied, a feed entry and a sentence added, a sentence reviewed):

- Driven in headless Chrome: backup downloaded, the backup zip dropped on Factory reset, preview,
  the reset refused until the box was ticked and the right project id typed, reset, then **Restore
  learner data from this backup**, preview, restore. No console errors.
- The preview counted 8,716 documents to delete and 2,337 to write back, and read 9,091.
- The reset took about 9 seconds on the emulator, and the learner restore wrote 6,377 documents.
- A backup taken afterwards matched the production backup in every collection, document for
  document, apart from the activity log.

**3. Reset rehearsal:** run `reset_firestore.js` without `--confirm`. It prints exactly what it would
delete and, in factory mode, what it would restore. The armed run reports the same numbers.

---

## History worth knowing

**The factory reset moved onto the portal page on 2026-10-14**, by request, so it can be shown
without a terminal. Like backup and restore there, it works only with the service-account key, not
the admin login, so a stolen login still cannot wipe anything. What it adds is that whoever holds
the key and a portal login can now wipe the database from a browser instead of a terminal; the key
could always do that through the scripts. The learner reset stayed in the terminal. The learner
collections the page ticks for *Learner data only* became the reset's full list at the same time,
adding `security_questions` and `literature_submissions`, so a restore after a reset puts back
everything the reset deleted for good.

**Until 2026-10-14 a factory reset left four collections as they were**: `word_audio`,
`vocabulary_trash`, `dictionary_updates` and `sentence_submissions`. The dictionary went back to the
backup while clips, recycle-bin entries and change-feed entries made after it stayed, so the database
did not end at the backup's state. They are now deleted and restored with the rest of the content.

**Backup and restore moved onto the portal page on 2026-10-13**, by request: a download button and a
drag-and-drop restore instead of terminal commands. Unlike the controls removed two days before,
the page uses the service-account key, not the admin login. So it covers the whole database,
learner progress, security questions and device tokens included, makes the same restore decisions
as the script, and an admin login alone still cannot use it. Reset stayed in the terminal.

**The dashboard's Backup & Reset controls were replaced on 2026-10-11.** The page then showed the
steps and commands from this runbook and ran nothing. Before, it offered a content-only JSON
export, a restore from that file, and a button that cleared the moderation queues. It could not back
up or restore learner progress, so it was never the backup that mattered. Its restore wrote every
document in the file and gave every word a fresh `updatedAt`, so after a restore each phone would
download the whole dictionary again on its next sync, against the 50,000-read daily limit. In an
emergency it was a second, weaker path beside the scripts, and its queue-clearing button let any
admin login delete the moderation queues at once. Everything it did is covered by the scripts above,
except clearing the queues alone: delete reviewed items one at a time from the Verification queue, or
use the learner reset, which clears the queues together with learner data.

**The full reset test on 2026-10-05 used up the day's free reads before the restore.** The factory
reset itself went as planned. Then every read was refused with `Quota exceeded`: the free plan
allows 50,000 reads a day, and normal app traffic, the daily backup and the test's own backup and
checks had used them. Each phone's daily full reconcile reads the whole dictionary, about 1,150
reads, so ordinary use alone comes close to the limit. Writes have a separate limit, so the learner
data was restored with the new `--write-only` option: 1,051 documents, about fifteen minutes after
the reset. Plan a reset early in the Pacific day, or check Firebase console → Firestore → Usage first.

**The first live restore, on 2026-10-04, stopped part-way with "Transaction too big".** Firestore
counts a batch's index entries against its limit, and every key in a progress document's `entries`
map is indexed. Production refused 400 learner documents in one batch; the emulator does not enforce
the limit, so the drill had passed. A refused batch is now split in half and retried, and the same
restore, run again, finished the remaining 459 documents. That was a deliberate learner reset and
restore of production. The final manifest matched the pre-reset backup in every collection, except
for the two audit entries recording the reset and the restore.

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
