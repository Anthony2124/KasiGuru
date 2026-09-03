# Backup and Reset

How KasiGuru's data is backed up, restored, and reset, and why the controls are split the way they
are. Written to be read by someone who has to run it under pressure.

Related: `docs/PHASE1_RUNBOOK.md`, `docs/MONITORING.md`, `docs/AUDIT_PROMPT.md`.

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

Writes `<output-dir>/<timestamp>/<collection>.json` plus a `manifest.json`. Default output is
`C:\KasiGuru\KasiGuruBackups`, deliberately outside the repository.

Every collection is walked **deep**: documents inside subcollections are captured with their full
path, so `users/{uid}/progress/main` restores to exactly where it came from.

Options:

- `KASIGURU_BACKUP_DAILY=1` — skip if a backup for today already exists. Used by the scheduled task.
- `KASIGURU_BACKUP_BUCKET=<bucket>` — also upload the run to Firebase Storage, so a backup survives
  the loss of the machine that made it. Unset means local-only.
- `KASIGURU_DEEP_COLLECTIONS="users"` — which collections own subcollections worth walking. Only
  change this if the schema grows new nested data.

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
releases, and the three moderation queues.

It does **not** contain learner progress, and says so in the file's `scope` and `note` fields. A
browser cannot enumerate `users/{uid}` documents that have no fields of their own, and that is all of
them — the Web SDK has no `listDocuments()`.

---

## Restore

```
node functions/restore_firestore.js <service-account.json> <backup-dir>
```

Writes every document back at its original path. Documents with the same path are overwritten;
documents created *since* the backup are left alone. This is a **roll-forward, not a rollback** — if
you need the database to end at exactly the state of a backup, use a factory reset instead.

From the dashboard, drop a `.json` content backup onto *Restore from backup*. Same semantics: merge,
not replace.

---

## Reset

Both modes are a **dry run** until you pass `--confirm=<projectId>`, and the project id must match
the one inside the service-account key — a key for the wrong project fails loudly rather than wiping
it.

### Clear learner data, keep the corpus

```
node functions/reset_firestore.js <service-account.json> --mode=learner
```

Deletes `users` (including every `progress` subcollection), `leaderboard_public`, `device_tokens`,
`security_questions`, `word_submissions`, `literature_submissions`, `issue_reports`.

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

Ten checks over a fixture that reproduces the exact shape that broke: users with no fields owning
progress subcollections. It asserts that the deep read finds them, that missing parents are recorded
without inventing data, that restore puts nested documents back at their original paths, that
pre-format-2 backups still restore, and that the reset counts and deletes the same documents,
children before parents.

**2. Restore drill — do this at least once before the defence:**

1. `node functions/backup_firestore.js <key>` and note the manifest counts.
2. Create a second Firebase project, or use the emulator suite.
3. `node functions/restore_firestore.js <scratch-key> <backup-dir>`.
4. Back up the scratch project and diff the two manifests. They should match.

**3. Reset rehearsal:** run `reset_firestore.js` without `--confirm`. It prints exactly what it would
delete and, in factory mode, what it would restore. The armed run reports the same numbers.

---

## History worth knowing

**Every backup taken before 2026-09-03 contains no learner progress.** `db.listCollections()` returns
root collections only, and the paginated read that followed it saw only documents that exist — so
`users` reported `0` while holding every learner's synced progress underneath it. The manifests
looked healthy the entire time. The fix was to walk parents with `listDocuments()`, which returns
missing documents too, and to recurse their subcollections.

If you need data from a pre-fix backup, the content is intact; the progress is not there to recover.

**The dashboard export named the wrong collection.** It read `system_announcements`; the collection is
`announcements` (`AnnouncementRepository.kt`, `firestore.rules`). Announcements were never included in
a dashboard backup, and a restore would have been rejected by the rules. Fixed at the same time.
