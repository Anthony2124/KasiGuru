# KasiGuru

**A gamified mobile learning application for the preservation and learning of
the Kasiguranin dialect.**

- 📱 **Android app** — Kotlin / Jetpack Compose / Room (SQLite) / Hilt / Firebase
- 🌐 **Web** (Vercel) — the learner app for iPhone/iPad/computers, admin dashboard, public download page
- ☁️ **Backend** — Firebase Firestore (+ Auth, FCM, Crashlytics) on the free plan

---

## Repo layout

Start with [docs/CODE_MAP.md](docs/CODE_MAP.md) to find a feature's screens,
ViewModels, repositories, tests and maintenance tools. [AGENTS.md](AGENTS.md)
records folder conventions for future changes.

| Path | What it is |
|---|---|
| `app/` | The Android app (Compose UI, Room DB, Firestore sync, FCM, Crashlytics) |
| `admin-website/` | Vercel projects: root placeholder, `admin/` (login + dashboard), `download/` (public APK page), `webapp/` (the learner app as an installable web app, for iPhone and anyone without the APK) |
| `functions/` | Free-plan helper scripts: `set_admin_claim.js`, `backup_firestore.js`, `restore_firestore.js`, `send_push.js` |
| `scripts/` | Project checks and established deploy, release and backup entry points |
| `scripts/dictionary/` | Dictionary import, audit, repair and definition tools |
| `scripts/lib/` | Shared project paths for local tools |
| `scripts/diagnostics/` | Read-only Firebase vocabulary diagnostics |
| `data/dictionary/` | Authored meanings and elicitation notes |
| `data/sql/` | Reference database schemas and historical SQL imports |
| `design/` | Mockups, screenshots and source design assets |
| `Voice/` | Source pronunciation recordings by category |
| `firestore.rules` | The security rules (source of truth; deploy with `firebase deploy --only firestore:rules`) |
| `docs/` | Runbooks and guides (see below) |

## Documentation

| Doc | Contents |
|---|---|
| [docs/CODE_MAP.md](docs/CODE_MAP.md) | Feature entry points, folder conventions, search commands and relocated paths |
| [scripts/README.md](scripts/README.md) | Local tooling and dictionary workflow |
| [docs/DICTIONARY_MEANINGS.md](docs/DICTIONARY_MEANINGS.md) | Corpus sources, definitions and repair rules |
| [docs/design/](docs/design/) | Design plans and historical audits |
| [docs/prompts/](docs/prompts/) | Project prompt documents |
| [docs/PHASE1_RUNBOOK.md](docs/PHASE1_RUNBOOK.md) | Production setup: rules deploy, admin claim, backups, CI |
| [docs/PHASE1_TUTORIAL.md](docs/PHASE1_TUTORIAL.md) | Step-by-step deployment walkthrough |
| [docs/MONITORING.md](docs/MONITORING.md) | Monitoring, costs, failure runbook, free-plan limits |
| [docs/WEB_APP.md](docs/WEB_APP.md) | The web app: why a PWA for iOS, parity with Android, deploying, testing |

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

Local verification after code changes:

```powershell
npm run check:structure
npm run check:web
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

```powershell
# Deploy the Firestore rules (source of truth)
firebase deploy --only firestore:rules

# Deploy all three web portals to your Vercel account
.\scripts\deploy_web.ps1

# Grant the admin claim to an admin email (free plan)
cd functions
node set_admin_claim.js admin@example.com C:\path\to\service-account.json

# One backup, now
node backup_firestore.js C:\path\to\service-account.json

# Daily backup with rotation, wipe warning and a copy to OneDrive\KasiGuruBackups
# (from the repository root; registers a Task Scheduler task)
.\scripts\register_backup_task.ps1 -KeyFile C:\path\to\service-account.json

# Restore from a backup (emergency; dry run until --confirm, see docs/BACKUP_AND_RESET.md)
node restore_firestore.js C:\path\to\service-account.json <backup-folder>
node restore_firestore.js C:\path\to\service-account.json <backup-folder> --confirm=kasiguru-86042

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
