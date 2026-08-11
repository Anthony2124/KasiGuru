# Phase 1 — Step-by-Step Tutorial

Follow these steps in order. Each step says what to run (PowerShell on Windows),
what you should see, and how to fix it if something goes wrong.

Estimated time: **45–75 minutes**, most of it waiting on deploys and verifications.

---

## Step 0 — Prerequisites

You need:

- A terminal (PowerShell) and `git`
- Node.js 18+ (`node --version`) — you have v24, fine
- Ownership of the Firebase project `kasiguru-86042`
- Access to the Google account used for the KasiGuru Gmail
- Access to the Vercel account that hosts the three portals
- A GitHub account (for CI)

Check what's installed:

```powershell
node --version
npm --version
git --version
firebase --version   # if "not recognized", install in Step 3
```

---

## Step 1 — Commit the Phase 1 changes

Before deploying anything, review and commit the work so Vercel/CI can see it.

```powershell
cd C:\KasiGuru\KasiGuru-main
git status                 # review the changed/new files
git add -A
git commit -m "security: phase 1 hardening (rules, credentials, migrations)"
git push
```

> **Expected:** push succeeds. This triggers the new GitHub Actions workflow
> (lint + unit tests + APK build) and redeploys the Vercel portals if they are
> connected to this repo.
>
> **If CI fails:** open the Actions tab, read the failing job. The most common
> cause is the missing `FIREBASE_SERVICE_ACCOUNT` secret — that's Step 12, and
> the rules-deploy job only runs on `main` pushes, so it failing now is expected
> until you add the secret.

---

## Step 2 — Rotate the leaked Gmail password (do NOT skip)

The old app password is recoverable from git history and from any APK built
before this change. Anyone with it can send email as the KasiGuru Gmail account.

1. Open https://myaccount.google.com/security in a browser.
2. Under **How you sign in to Google** → **App passwords**.
   (If prompted, re-enter your password.)
3. Find the app password used for "KasiGuru" and click **Revoke**.
4. If the exact entry isn't listed, change your account password — that revokes
   all app passwords.
5. Check https://myaccount.google.com/security → **Recent security events** for
   any unauthorized sends from the email address.

> **Why:** the credential was committed as plaintext in `EmailService.kt`
> (now deleted). Rotation is the only way to invalidate it.

---

## Step 3 — Install the Firebase CLI and log in

```powershell
npm install -g firebase-tools
firebase --version        # should print something like 13.x
firebase login
```

A browser window opens — pick the Google account that owns `kasiguru-86042` and
allow the permissions. Back in the terminal you should see
`✔  Success! Logged in as you@example.com`.

---

## Step 4 — Install function dependencies

```powershell
cd C:\KasiGuru\KasiGuru-main\functions
npm install
cd ..
```

> **Expected:** a `node_modules` folder appears inside `functions/`.

---

## Step 5 — (Skipped) No Blaze plan needed

KasiGuru stays on the free (Spark) plan. Rules deploy, the admin claim, CI, and
backups all work without Blaze:

- Step 7 deploys **rules only**.
- Step 8 grants the admin claim with the local `set_admin_claim.js` script.
- Step 11 schedules backups from your machine via Task Scheduler.

The Cloud Functions in `functions/index.js` are kept as future reference;
nothing in Phase 1 depends on them.

---

## Step 6 — (Skipped) No bootstrap secret needed

The secret only protected the Cloud Function version of the admin claim. Since
claims are granted with the local `set_admin_claim.js` script, there is nothing
to create here.

---

## Step 7 — Deploy rules

From `C:\KasiGuru\KasiGuru-main` (the project root — `.firebaserc` already
points at `kasiguru-86042`):

```powershell
firebase deploy --only firestore:rules
```

> **Expected output:** one `firestore` section ending with `✔  Deploy complete!`.
>
> **Common errors:**
> - `HTTP Error: 403, The caller does not have permission` → `firebase login`
>   as the project owner, or `firebase use kasiguru-86042`.
> - Rules syntax error → the rules file is validated before upload; if you see
>   a line number, it's a typo in `firestore.rules` — fix and re-run.

---

## Step 8 — Grant yourself the admin claim

The new dashboard only opens for accounts with the `admin` custom claim. Do this
before you rely on the admin panel, otherwise you'll see "Access Denied" (that's
the guard working correctly — this step fixes it).

1. Make sure your email exists as a Firebase Auth user:
   Firebase console → **Authentication** → **Users**. If missing, **Add user**
   with your email (the one you log into the admin portal with).
2. Generate a service-account key:
   Firebase console → Project settings → **Service accounts** →
   **Generate new private key**. Save the JSON **outside the repo**
   (e.g. `C:\KasiGuru\firebase-service-account.json`).
3. Run the claim script (works on the free plan — no Blaze):

```powershell
cd C:\KasiGuru\KasiGuru-main\functions
node set_admin_claim.js YOUR_ADMIN_EMAIL C:\KasiGuru\firebase-service-account.json
```

> **Expected:** `Admin claim set for YOUR_ADMIN_EMAIL.`
> **If `auth/user-not-found`:** add the user in Authentication → Users first.

4. Sign out of the admin portal and sign back in — custom claims only appear in
   the token after a fresh sign-in.
5. Open the dashboard. It should load normally.
6. Test the denial path: in a private/incognito window, register a throwaway
   account, open the admin portal → you should see **Access Denied** and be
   signed out after a few seconds.

---

## Step 9 — Verify the security rules from outside

These confirm the rules actually do what the audit promised. All are read-only
or use a throwaway document that we delete immediately.

```powershell
# 1. Anonymous read of vocabulary — should still work (HTTP 200)
Invoke-WebRequest -UseBasicParsing `
  "https://firestore.googleapis.com/v1/projects/kasiguru-86042/databases/(default)/documents/vocabulary?pageSize=1"

# 2. Anonymous delete of a submission — must be DENIED (HTTP 403)
try {
  Invoke-WebRequest -UseBasicParsing -Method Delete `
    "https://firestore.googleapis.com/v1/projects/kasiguru-86042/databases/(default)/documents/word_submissions/does_not_exist"
} catch {
  $_.Exception.Response.StatusCode.value__   # expect 403
}

# 3. Anonymous create of a submission — must still be allowed (HTTP 200)
$sub = '{"fields":{"kasiguranin":{"stringValue":"testprobe"},"tagalog":{"stringValue":"x"},"english":{"stringValue":"x"},"status":{"stringValue":"pending"},"submittedAt":{"integerValue":"1755000000000"}}}'
Invoke-WebRequest -UseBasicParsing -Method Post `
  -Uri "https://firestore.googleapis.com/v1/projects/kasiguru-86042/databases/(default)/documents/word_submissions?documentId=probe_remove_me" `
  -Body $sub -ContentType "application/json"

# cleanup
Invoke-WebRequest -UseBasicParsing -Method Delete `
  "https://firestore.googleapis.com/v1/projects/kasiguru-86042/databases/(default)/documents/word_submissions/probe_remove_me"
```

Expected matrix:

| Check | Expected |
|---|---|
| Anonymous read `vocabulary` | 200 |
| Anonymous delete on `word_submissions` | **403** (was 200 before) |
| Anonymous create on `word_submissions` | 200 (app feature still works) |

---

## Step 10 — Redeploy the web portals (Vercel)

The repo now serves a harmless placeholder at the site root instead of the
unauthenticated admin panel.

Option A — auto-deploy (if the repos are connected to Vercel): the `git push`
from Step 1 already redeployed them. Verify:

- Root domain → shows "Restricted Area" (no dashboard, no imports, no releases).
- Admin portal → login page, then dashboard (with your admin account).
- Download page → unchanged.

Option B — manual deploy:

```powershell
cd C:\KasiGuru\KasiGuru-main\admin-website
vercel --prod
```

Then, recommended cleanup:
1. In Vercel, open the **root** project (`kasi-guru`).
2. Add a redirect from the root domain to the **admin** project URL.
3. Delete the root project once nothing links to it (the placeholder becomes
   unnecessary). Update the TODO comment in `admin-website/index.html` when done.

---

## Step 11 — Set up backups (free-plan friendly)

> Note: Firestore's built-in GCS export requires **billing** — even on the free
> plan. Instead, `backup_firestore.js` reads the database with the Admin SDK
> (which can also read `word_submissions`, unlike the export API) and writes
> timestamped JSON files locally. No billing, no bucket, no IAM setup needed.

### 11a. Run one backup

```powershell
cd C:\KasiGuru\KasiGuru-main\functions
node backup_firestore.js C:\KasiGuru\firebase-service-account.json
```

Expected:

```text
Backup complete -> C:\KasiGuru\KasiGuruBackups\<timestamp>
Collections: {"stories":3,"vocabulary":429,"word_submissions":5}
```

Each run creates `C:\KasiGuru\KasiGuruBackups\<timestamp>\` with one JSON per
collection plus `manifest.json`. **Keep this folder private** — it contains
user-submitted data — and consider syncing it to Google Drive / OneDrive /
another machine for off-site redundancy.

### 11b. Schedule it nightly (Windows Task Scheduler)

1. Open **Task Scheduler** → **Create Task**.
2. **Triggers** → New → Daily, start time 02:00.
3. **Actions** → New → Program/script: `node`
   - Arguments: `backup_firestore.js C:\KasiGuru\firebase-service-account.json`
   - Start in: `C:\KasiGuru\KasiGuru-main\functions`
4. **Conditions** → uncheck "Start the task only if the computer is on AC power".
5. **Settings** → check "Run task as soon as possible after a scheduled start is missed".

> **Custom output folder?** Pass it as a second argument:
> `node backup_firestore.js C:\KasiGuru\firebase-service-account.json D:\backups`

### 11c. Restore (emergency only — test once on a scratch project first)

```powershell
node restore_firestore.js C:\KasiGuru\firebase-service-account.json C:\KasiGuru\KasiGuruBackups\<timestamp>
```

Documents with the same ID are overwritten.

---

## Step 12 — Set up CI (GitHub Actions)

The workflow runs lint + unit tests + APK build on every push, and deploys
Firestore rules on `main`.

1. Firebase console → **Project settings** → **Service accounts** →
   **Generate new private key**. A JSON file downloads — this is a
   *service-account key*, keep it private and never commit it.
2. GitHub → your repo → **Settings** → **Secrets and variables** → **Actions** →
   **New repository secret**.
3. Name: `FIREBASE_SERVICE_ACCOUNT`. Paste the entire JSON contents.
4. Give that service account permission to deploy rules: Google Cloud console →
   **IAM & Admin** → find the service account → edit → add role
   *Firebase Rules Admin* (or *Firestore Admin*).
5. Push a commit to `main` and watch the **Actions** tab — the
   `firebase-rules` job should deploy rules automatically.

---

## Step 13 — Test the app's migration (recommended before shipping)

The destructive fallback is gone, so any future schema change **must** include a
migration or the app will crash on upgrade instead of wiping data. Verify the
v1→v17 chain on an emulator:

```powershell
cd C:\KasiGuru\KasiGuru-main
./gradlew connectedDebugAndroidTest
```

> Needs an emulator/device. If you don't have one, the CI workflow doesn't run
> instrumented tests yet — tell me and I'll add an emulator job.

On a real device that had the app installed: install the new APK over the old
one and confirm your XP/streak/progress survived.

---

## Step 14 — Final checklist

- [ ] Gmail app password revoked
- [ ] `firebase deploy` succeeded (rules + storage + functions)
- [ ] `set_admin_claim.js` printed "Admin claim set" for your email
- [ ] Admin portal opens; throwaway account gets "Access Denied"
- [ ] Root domain shows "Restricted Area"
- [ ] Anonymous submission create = 200; anonymous delete = 403
- [ ] Backup bucket exists; a manual export produced files
- [ ] GitHub secret `FIREBASE_SERVICE_ACCOUNT` set; rules job green
- [ ] Migration tests pass on an emulator (Step 13)

---

## Troubleshooting cheat-sheet

| Symptom | Fix |
|---|---|
| `firebase` not recognized | `npm install -g firebase-tools`, reopen terminal |
| Deploy 403 | `firebase login` as owner; `firebase use kasiguru-86042` |
| `set_admin_claim.js` says user not found | Add your email in Firebase Auth → Users first |
| Admin panel shows Access Denied for you | Claim not set, or token stale → sign out/in; re-run Step 8 |
| `vercel` not recognized | `npm install -g vercel`, then `vercel login` |
| Backup function fails with permission error | Complete Step 11c roles |
| Rules deploy green but app can't submit words | Only `word_submissions` create is open; check field names in the app's `WordSubmissionDto` match the allowed keys in `firestore.rules` |
