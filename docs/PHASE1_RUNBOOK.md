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
| `functions/` | `bootstrapAdmin` (grants the `admin` claim) + `scheduledFirestoreBackup` (nightly export to GCS). |
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

## 2. Deploy Firestore rules + Cloud Functions

Prerequisites: Node 20+, Firebase CLI, and ownership of the `kasiguru-86042` project.

```bash
npm install -g firebase-tools
cd functions && npm install && cd ..
firebase login
firebase use kasiguru-86042
```

Set the bootstrap secret (used once in step 3):

```bash
cd functions
firebase functions:secrets:set BOOTSTRAP_ADMIN_SECRET
cd ..
```

Deploy everything:

```bash
firebase deploy --only firestore:rules,storage:rules,functions
```

> Firestore rules and functions require the **Blaze (pay-as-you-go)** plan.
> Rules alone work on the free plan, but the scheduled backup and the
> `bootstrapAdmin` function do not.

> **On the free (Spark) plan?** You can still grant the admin claim without
> deploying functions:
> 1. Firebase console → Project settings → Service accounts →
>    **Generate new private key** (keep the JSON out of the repo).
> 2. `cd functions && node set_admin_claim.js YOUR_ADMIN_EMAIL ../path/to/key.json`
> 3. Sign out/in of the admin portal. Revisit the Blaze upgrade later for the
>    functions + scheduled backups.

---

## 3. Grant your account the admin claim

Do this **before** you rely on the new admin panel, or you'll see "Access Denied"
(which is the guard working as intended — recoverable, just run this step).

1. In Firebase console → Authentication → add your email as a user if it isn't there.
2. Call the bootstrap function with your email and the secret you set above:

```bash
curl -X POST https://us-central1-kasiguru-86042.cloudfunctions.net/bootstrapAdmin \
  -H "Content-Type: application/json" \
  -d '{"data":{"email":"YOUR_ADMIN_EMAIL","bootstrapSecret":"THE_SECRET_YOU_SET"}}'
```

   (Replace `us-central1` if you deploy to another region. Response: `{"ok":true,...}`.)
3. Sign out and sign back into the admin portal so the token refreshes with the claim.

Verify:
- Open the admin portal → dashboard loads normally.
- Open a private window, register a throwaway account, try the dashboard →
  "Access Denied".

> If you ever need another admin: repeat step 3 with that email.

---

## 4. Redeploy the web portals (Vercel)

The repo now serves a neutral placeholder at the site root instead of the
unauthenticated admin panel.

1. Redeploy the **`kasi-guru`** (root) Vercel project so the placeholder goes live.
2. Confirm the root domain no longer exposes any admin UI (dashboard, imports,
   releases, etc.).
3. Recommended: in Vercel, redirect the root domain to the **`admin`** project URL,
   then delete the root project once nothing links to it. Update the TODO comment
   in `admin-website/index.html` when you do.

The **`admin`** project now enforces the admin claim client-side; the **`download`**
project is unchanged.

---

## 5. Enable scheduled backups

The `scheduledFirestoreBackup` function exports all collections nightly to a GCS
bucket. One-time setup:

1. Create the bucket (default name: `kasiguru-86042-backups`):

```bash
gsutil mb -l asia-east1 gs://kasiguru-86042-backups
```

2. Add a lifecycle rule to expire exports after 30 days (control cost):

```bash
gsutil lifecycle set <(echo '{"rule":[{"action":{"type":"Delete"},"condition":{"age":30}}]}') gs://kasiguru-86042-backups
```

3. Grant the App Engine default service account
   (`kasiguru-86042@appspot.gserviceaccount.com`) the **Datastore Import Export
   Admin** role and **Storage Object Admin** on the bucket.
4. If your bucket is not named `*-backups`, set the `BACKUP_BUCKET` environment
   variable on the function in the Firebase console
   (Functions → scheduledFirestoreBackup → Edit → Runtime environment variables).
5. Re-deploy functions, then wait for the first nightly run and verify a folder
   appears in the bucket. Test restore once on a scratch project before you ever
   need it.

Manual alternative (works on any plan):

```bash
gcloud firestore export gs://kasiguru-86042-backups/manual-$(date +%F)
```

---

## 6. CI (GitHub Actions)

The workflow runs lint, unit tests, and an APK build on every push, and deploys
Firestore rules on `main`.

1. Create a Firebase service-account key in the console:
   Project settings → Service accounts → Generate new private key.
2. Add it as a repository secret named **`FIREBASE_SERVICE_ACCOUNT`**
   (Settings → Secrets and variables → Actions).
3. Give that service account the **Firebase Rules Admin** role
   (or Firestore Admin) so the deploy job can update rules.

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
- [ ] `bootstrapAdmin` returned `ok:true` for your admin email
- [ ] Admin portal loads; a second, non-admin account sees "Access Denied"
- [ ] Root domain shows the placeholder (no admin UI)
- [ ] Anonymous word submission from the app still works (create allowed)
- [ ] Anonymous delete of a submission returns 403 (denied)
- [ ] Nightly backup produced an export in GCS
- [ ] CI green on the next push
