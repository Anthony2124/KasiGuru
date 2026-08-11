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

## Step 5 — Enable the Blaze plan (required for functions + backups)

The scheduled backup and the `bootstrapAdmin` function need the paid plan.

1. Open the Firebase console → your project `kasiguru-86042` →
   **Upgrade** (top-left) → **Blaze (pay-as-you-go)**.
2. Confirm the billing account.

> Costs: Cloud Functions and Firestore exports are billed per use. With one
> nightly export of a small database this is typically well under a dollar a
> month. You can delete the backup bucket later if you want to stop paying.
>
> **If you do not want Blaze:** rules + Vercel + CI still work. Only the
> admin-claim function and scheduled backups need it — tell me and I'll add a
> one-off Admin SDK script you can run locally instead.

---

## Step 6 — Set the bootstrap secret

This secret protects the "grant admin" function so strangers can't call it.

```powershell
cd C:\KasiGuru\KasiGuru-main\functions
firebase functions:secrets:set BOOTSTRAP_ADMIN_SECRET
```

Type a long random value (e.g. 32+ characters) and press Enter. Store it
somewhere safe — you need it again in Step 8.

> **If it errors with "billing" or "secret manager":** you're still on the free
> plan — go back to Step 5.

---

## Step 7 — Deploy rules and functions

From `C:\KasiGuru\KasiGuru-main` (the project root — `.firebaserc` already
points at `kasiguru-86042`):

```powershell
firebase deploy --only firestore:rules,storage:rules,functions
```

> **Expected output:** three sections — `firestore`, `storage`, `functions` —
> each ending with `✔  Deploy complete!`. The functions take the longest
> (build + upload).
>
> **Common errors:**
> - `HTTP Error: 403, The caller does not have permission` → `firebase login`
>   as the project owner, or `firebase use kasiguru-86042`.
> - `billing account required` → do Step 5 first.
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
2. Call the bootstrap function. On Windows use `Invoke-RestMethod`
   (`curl` in PowerShell is an alias for something else):

```powershell
$body = @{
  data = @{
    email           = "YOUR_ADMIN_EMAIL"
    bootstrapSecret = "THE_SECRET_FROM_STEP_6"
  }
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Method Post `
  -Uri "https://us-central1-kasiguru-86042.cloudfunctions.net/bootstrapAdmin" `
  -ContentType "application/json" `
  -Body $body
```

> **Expected:** `ok True` (PowerShell prints the response object).
> **If `permission-denied`:** wrong secret. **If `not-found`:** the email isn't a
> Firebase Auth user yet.

3. Sign out of the admin portal and sign back in — custom claims only appear in
   the token after a fresh sign-in.
4. Open the dashboard. It should load normally.
5. Test the denial path: in a private/incognito window, register a throwaway
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

## Step 11 — Set up nightly backups

### 11a. Create the bucket

Console (no gcloud needed):
1. Google Cloud console → **Cloud Storage** → **Buckets** → **Create**.
2. Name: `kasiguru-86042-backups`. Region: `asia-east1` (or any). Click create.

Or with gcloud:

```powershell
gcloud auth login
gsutil mb -l asia-east1 gs://kasiguru-86042-backups
```

### 11b. Expire backups after 30 days (control cost)

Console: open the bucket → **Lifecycle** → **Add rule** → *Delete object* when
*Age* ≥ 30 days → Create.

### 11c. Grant the function permission to export

The function runs as `kasiguru-86042@appspot.gserviceaccount.com`. Grant it:

- **Datastore Import Export Admin** (project-level):
  Google Cloud console → **IAM & Admin** → **Grant access** → principal
  `kasiguru-86042@appspot.gserviceaccount.com` → role
  *Cloud Datastore Import Export Admin*.
- **Storage Object Admin** on the bucket:
  bucket → **Permissions** → **Grant access** → same principal → role
  *Storage Object Admin*.

Or with gcloud:

```powershell
gcloud projects add-iam-policy-binding kasiguru-86042 `
  --member="serviceAccount:kasiguru-86042@appspot.gserviceaccount.com" `
  --role="roles/datastore.importExportAdmin"
gsutil iam ch serviceAccount:kasiguru-86042@appspot.gserviceaccount.com:objectAdmin `
  gs://kasiguru-86042-backups
```

### 11d. Verify the first export

The function runs nightly at 02:00 Asia/Shanghai. To test immediately, either
wait for the schedule or run a manual export (works on any plan):

```powershell
gcloud firestore export gs://kasiguru-86042-backups/manual-test
```

Then check the bucket — you should see a folder per export containing
`all_namespaces` / `all_groups` and metadata files.

> **Custom bucket name?** Set the `BACKUP_BUCKET` environment variable on the
> `scheduledFirestoreBackup` function in the Firebase console
> (Functions → the function → Edit → Runtime environment variables).

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
- [ ] `bootstrapAdmin` returned `ok True` for your email
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
| "Billing account required" | Upgrade to Blaze (Step 5) |
| `bootstrapAdmin` returns `permission-denied` | Wrong `bootstrapSecret`; re-run Step 6/8 |
| `bootstrapAdmin` returns `not-found` | Add your email in Firebase Auth → Users first |
| Admin panel shows Access Denied for you | Claim not set, or token stale → sign out/in; re-run Step 8 |
| `vercel` not recognized | `npm install -g vercel`, then `vercel login` |
| Backup function fails with permission error | Complete Step 11c roles |
| Rules deploy green but app can't submit words | Only `word_submissions` create is open; check field names in the app's `WordSubmissionDto` match the allowed keys in `firestore.rules` |
