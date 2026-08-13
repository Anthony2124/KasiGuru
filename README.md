# KasiGuru

**A gamified mobile learning application for the preservation and learning of
the Kasiguranin dialect.**

- 📱 **Android app** — Kotlin / Jetpack Compose / Room (SQLite) / Hilt / Firebase
- 🌐 **Web portals** (Vercel) — admin dashboard, public download page
- ☁️ **Backend** — Firebase Firestore (+ Auth, FCM, Crashlytics) on the free plan

---

## Repo layout

| Path | What it is |
|---|---|
| `app/` | The Android app (Compose UI, Room DB, Firestore sync, FCM, Crashlytics) |
| `admin-website/` | Three Vercel projects: root placeholder, `admin/` (login + dashboard), `download/` (public APK page) |
| `functions/` | Free-plan helper scripts: `set_admin_claim.js`, `backup_firestore.js`, `restore_firestore.js`, `send_push.js` |
| `scripts/` | `deploy_web.ps1` — one-command deploy of all web portals |
| `firestore.rules` | The security rules (source of truth; deploy with `firebase deploy --only firestore:rules`) |
| `docs/` | Runbooks and guides (see below) |

## Documentation

| Doc | Contents |
|---|---|
| [docs/PHASE1_RUNBOOK.md](docs/PHASE1_RUNBOOK.md) | Production setup: rules deploy, admin claim, backups, CI |
| [docs/PHASE1_TUTORIAL.md](docs/PHASE1_TUTORIAL.md) | Step-by-step deployment walkthrough |
| [docs/MONITORING.md](docs/MONITORING.md) | Monitoring, costs, failure runbook, free-plan limits |

## Security & hardening (what changed)

The codebase went through a six-phase security/architecture hardening program.
Everything preserves the original UI/UX; all changes are backend,
infrastructure, or data-layer:

1. **Critical fixes** — removed a committed Gmail app password; strict
   Firestore rules (public reads only where needed, admin-claim writes,
   validated anonymous submission creates, no anonymous deletes); unauthenticated
   admin panels taken down; full Room migration chain v1→v19 (no more
   destructive fallback); backups via a local script (no Blaze required).
2. **Backend improvements** — working in-app update flow
   (`BuildConfig.VERSION_CODE` + banner); batched, delta-safe Firestore sync;
   dead code removed; anonymous Firebase Auth foundation.
3. **Database & performance** — indexes on hot queries; consolidated the two
   conflicting level systems onto one source of truth; single-sourced
   categories; level-consistency unit tests.
4. **Security & reliability** — admin audit log (append-only); submission
   cooldown; CSP + SRI web hardening; APK URL scheme validation; Crashlytics;
   fixed the broken QR code on the download page.
5. **Android integration** — FCM push notifications, device registration,
   deep links into app screens, local notification history, free-plan
   `send_push.js`.
6. **Scalability & production hardening** — cross-device progress sync
   (`users/{uid}/progress` with merge logic + tests), monitoring guide.

## Key operational commands

```powershell
# Deploy the Firestore rules (source of truth)
firebase deploy --only firestore:rules

# Deploy all three web portals to your Vercel account
.\scripts\deploy_web.ps1

# Grant the admin claim to an admin email (free plan)
cd functions
node set_admin_claim.js admin@example.com C:\path\to\service-account.json

# Daily backup (runs at logon via Startup folder, once per day)
node backup_firestore.js C:\path\to\service-account.json

# Off-site mirror: KasiGuruBackups -> OneDrive\KasiGuruBackups
# (wired into the same startup task; keeps a cloud copy of every backup)
.\scripts\mirror_backup.cmd

# Restore from a backup (emergency; test on a scratch project first)
node restore_firestore.js C:\path\to\service-account.json C:\KasiGuru\KasiGuruBackups\<date>

# Send a push notification to all registered devices
node send_push.js C:\path\to\service-account.json "Title" "Body" "story/1" "General"
```

## Security model (summary)

- Firestore: public reads only for `vocabulary`/`stories`/`app_releases`;
  admin-claim-only writes to those; anonymous **create-only** (validated) on
  `word_submissions`; owner-only `users/{uid}/**` and `device_tokens/{uid}`;
  append-only `admin_audit_log`.
- Admin panel: Firebase Auth **plus** the `admin` custom claim, enforced in
  both the dashboard JS and the Firestore rules.
- No secrets in the repo. Service-account keys and tokens live outside the
  repo (gitignored patterns included).

## Free-plan notes

- No Cloud Functions / Blaze: claims, backups, and push sending run from local
  scripts with the service-account key.
- Firestore's GCS export requires billing — the local JSON backup is the
  replacement and also captures collections the export API cannot read.
- Server-authoritative leaderboards, content delta pipeline, and real
  email/password accounts remain future work (see docs/MONITORING.md).

## Known loose ends

- Rotate/revoke the legacy Gmail app password (owner action).
- Grant the CI service account **Firebase Rules Admin** + **Service Usage
  Viewer** roles to turn the rules-deploy job green.
- Run `./gradlew connectedDebugAndroidTest` on an emulator before distributing
  (validates the v1→v19 migration chain).
- Rotate the Vercel CLI token if it was ever shared in chat.
