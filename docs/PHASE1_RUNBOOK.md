# Phase 1 — Critical Fixes: Deployment Runbook

Everything in this runbook is already implemented in the repo. The steps below
deploy it and handle the parts that require **your** accounts (Google, Firebase,
Vercel, GitHub). Do them in this order to avoid locking yourself out.

---

## 0. What changed (repo summary)

| Area | Change |
|---|---|
| `app/.../util/EmailService.kt` | **Deleted.** Contained a live Gmail app password compiled into the APK. |
| `firestore.rules`, `firebase.json`, `storage.rules` | New strict rules: public reads only where needed, admin-only writes to dictionary/releases, validated anonymous submission create, no anonymous delete. |
| `functions/` | Local helpers: `set_admin_claim.js` (grants the `admin` claim, free-plan friendly) and `backup_firestore.js` (Firestore export to GCS). |
| `admin-website/admin/js/app.js`, `dashboard.html` | Dashboard now requires the `admin` custom claim, not just a signed-in user. |
| `admin-website/index.html` | Legacy unauthenticated admin panel replaced with a restricted-area placeholder. |
| `app/.../KasiGuruMigrations.kt`, `DatabaseModule.kt`, `StreakReminderWorker.kt` | Full v1→v17 Room migrations; `fallbackToDestructiveMigration()` removed (no more silent data wipes). |
| `AndroidManifest.xml`, `MainActivity.kt` | `POST_NOTIFICATIONS` permission + runtime request so streak reminders work on Android 13+. |
| `.github/workflows/ci.yml` | CI: lint, unit tests, APK build; deploys rules on main. |
| `app/src/androidTest/.../MigrationTest.kt` | Instrumented migration tests (run on an emulator). |

---

## 1. CRITICAL — Rotate the leaked Gmail password (do this first)

The Gmail app password that was committed in `EmailService.kt` is publicly
recoverable from the git history **and** from any APK built before this change.

1. Open https://myaccount.google.com/security → **App passwords**.
2. **Revoke** the password for this app (the one matching the code that was removed).
3. (Recommended) Review Gmail's recent activity for unauthorized sends:
   https://myaccount.google.com/security → *Recent security events*.
4. If you want email verification back later, implement it server-side
   (Cloud Functions + Resend/SendGrid, or Firebase Auth's built-in email verification).
   Never put SMTP credentials in the app again.

> If you cannot find/revoke the exact app password, change the account password
> entirely — that revokes all app passwords.

---

## 2. Deploy Firestore rules

Prerequisites: Node 18+, Firebase CLI, and access to the `kasiguru-86042` project.

```bash
npm install -g firebase-tools
firebase login
firebase deploy --only firestore:rules
```

> KasiGuru stays on the **free (Spark) plan**, so Cloud Functions and Secret
> Manager are not used and no billing is needed. The `functions/` folder exists
> only for the local helper scripts (`set_admin_claim.js`,
> `backup_firestore.js`) and as future reference.

---

## 3. Grant your account the admin claim

Do this **before** you rely on the new admin panel, or you'll see "Access Denied"
(which is the guard working as intended — recoverable, just run this step).

1. Make sure your email exists as a Firebase Auth user:
   Firebase console → **Authentication** → **Users** (add if missing).
2. Generate a service-account key:
   Firebase console → Project settings → **Service accounts** →
   **Generate new private key**. Save the JSON **outside the repo**
   (e.g. `C:\KasiGuru\firebase-service-account.json`).
3. Grant the claim (no Blaze needed):

```bash
cd functions && npm install   # one time
node set_admin_claim.js YOUR_ADMIN_EMAIL C:\KasiGuru\firebase-service-account.json
```

4. Sign out and sign back into the admin portal so the token refreshes with the claim.

Verify:
- Open the admin portal → dashboard loads normally.
- Open a private window, register a throwaway account, try the dashboard →
  "Access Denied".

> If you ever need another admin: repeat step 3 with that email.

---

## 4. Redeploy the web portals (Vercel)

The repo now serves a neutral placeholder at the site root instead of the
unauthenticated admin panel.

Deploy all three portals with one command (no Vercel team/payment needed —
deploys run from your own account via the CLI):

```powershell
.\scripts\deploy_web.ps1
```

The script deploys `admin-website/`, `admin-website/admin/`, and
`admin-website/download/` to the `kasi-guru`, `admin`, and `download` projects,
aliasing to the canonical URLs. Authentication is via `vercel login`,
`$env:VERCEL_TOKEN`, or a private token file at `C:\KasiGuru\.vercel_token`.

After deploying, confirm the root domains show the "Restricted Area" placeholder
and expose no admin UI.

The **`admin`** project now enforces the admin claim client-side; the **`download`**
project is unchanged.

---

## 5. Backups (Spark-friendly — no Blaze required)

Firestore's GCS export API requires billing, so `backup_firestore.js` instead
reads every collection with the Admin SDK and writes timestamped JSON locally.
It also captures `word_submissions`, which the export API cannot read without
admin access. No bucket, no IAM, no billing.

1. Test one backup:

```bash
cd functions
node backup_firestore.js C:\KasiGuru\firebase-service-account.json
```

   Expected: `Backup complete -> C:\KasiGuru\KasiGuruBackups\<timestamp>` with a
   JSON file per collection + `manifest.json`.
2. Schedule it nightly with **Task Scheduler**:
   - Task Scheduler → Create Task → Triggers → Daily (e.g. 02:00).
   - Action: Program `node`, Arguments
     `backup_firestore.js C:\KasiGuru\firebase-service-account.json`,
     Start in `C:\KasiGuru\KasiGuru-main\functions`.
   - Run whether user is logged on or not (store the account password).
3. Keep the backup folder **private** (it contains user submissions) and sync it
   to Google Drive / OneDrive / another machine for off-site redundancy.
4. Restore (emergency): `node restore_firestore.js <key.json> <backup-dir>` —
   test once on a scratch project before relying on it.

---

## 6. CI (GitHub Actions)

The workflow runs lint, unit tests, and an APK build on every push, and deploys
Firestore rules on `main`.

1. Create a Firebase service-account key in the console:
   Project settings → Service accounts → Generate new private key.
2. Add it as a repository secret named **`FIREBASE_SERVICE_ACCOUNT`**
   (Settings → Secrets and variables → Actions).
3. Give that service account two roles so the deploy job can update rules:
   **Firebase Rules Admin** and **Service Usage Viewer**
   (IAM & Admin → edit the service account → add both roles).

---

## 7. Before shipping the next APK

1. Run the migration tests on an emulator:
   `./gradlew connectedDebugAndroidTest` — confirms v1→v17 upgrades preserve data.
2. On first launch after upgrade, confirm XP/streak/SRS progress is intact
   (the destructive fallback is gone; a future schema bump without a migration
   now **fails loudly** instead of wiping data — add migrations with every schema change).
3. The app's one-shot sync can no longer push seed data to `vocabulary` (rules
   are admin-only for writes). That's intended: the master dictionary is managed
   from the admin portal. The app still works fully offline with its bundled seed.
4. Notifications now require permission on Android 13+; the app requests it at
   startup.

---

## 8. Verification checklist

- [ ] Gmail app password revoked / account password changed
- [ ] `firebase deploy` succeeds (rules + functions)
- [ ] `set_admin_claim.js` printed "Admin claim set" for your admin email
- [ ] Admin portal loads; a second, non-admin account sees "Access Denied"
- [ ] Root domain shows the placeholder (no admin UI)
- [ ] Anonymous word submission from the app still works (create allowed)
- [ ] Anonymous delete of a submission returns 403 (denied)
- [ ] Nightly backup produced an export in GCS
- [ ] CI green on the next push
