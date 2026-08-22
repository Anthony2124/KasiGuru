# KasiGuru Full-System Audit Prompt

A standing, reusable prompt for auditing KasiGuru end-to-end — Android app, admin website, and Firebase backend. Paste the block below into a fresh Claude Code session with access to this repo whenever you want a full audit, or reuse it as the basis for a partial one (e.g. just security, just UI/UX).

---

## The prompt

```
You are auditing the KasiGuru system end-to-end: the Android app, the admin
website, and the Firebase backend. This is a read-only investigation first —
do not modify any files, run destructive commands, or deploy anything. Your
job is to find and report real problems, not to fix them yet.

SYSTEM CONTEXT (do not re-derive, use as ground truth):
- app/            Kotlin + Jetpack Compose Android app. Hilt DI, Room (SQLite,
                   migrations v1-v19), Firebase Auth/Firestore/FCM/Crashlytics.
- admin-website/  Vanilla HTML/CSS/JS, no framework. Three separate deploy
                   targets: root/ (placeholder), admin/ (login + CRUD
                   dashboard), download/ (public APK download page).
- functions/      Firebase Cloud Functions, Node 20. Scripts for admin
                   claims, Firestore backup/restore, push notifications,
                   release publishing, data seeding.
- Database: Firestore only, project kasiguru-86042, on the FREE SPARK PLAN
  (the owner will not upgrade to Blaze — do not propose fixes that require
  Blaze-only features like scheduled functions with outbound networking,
  or paid Cloud Storage/Functions tiers). Firebase Storage is deliberately
  disabled/unused. Room on-device storage syncs to Firestore.
- Security: firestore.rules already implements admin-claim checks
  (isAdmin()), ownership checks (isOwner()), and anti-cheat delta-cap
  validators (isSaneCounter, isValidMainProgress, isValidLearningDoc) that
  block client-side stat/XP forgery. A prior hardening pass already removed
  a leaked credential and locked down previously-open admin endpoints —
  don't rediscover already-fixed issues, but do verify they're still fixed.
- CI/CD: .github/workflows/ci.yml (lint + unit tests + rules deploy on
  main) and release.yml (tag-triggered signed APK build, validates tag
  matches versionName/versionCode, deploys download site, writes an
  app_releases Firestore doc via functions/publish_release.js).
- Test coverage is thin: ~9 test files against ~160 Kotlin source files,
  and no visible test suite for admin-website or functions/.

AUDIT SCOPE — go through each area and actually read the relevant files,
don't guess from names alone:

1. UI/UX (app + admin website)
   - Compose screens under app/src/.../ui/screens/**: inconsistent spacing,
     missing loading/error/empty states, unhandled configuration changes,
     accessibility (contentDescription, touch target size, text scaling),
     dark/light mode gaps, navigation dead-ends.
   - admin-website/admin/**: broken or inconsistent styling across pages,
     forms with no client-side validation feedback, unclear error messages,
     mobile responsiveness of the admin dashboard.
   - download/ page: does it clearly communicate version, changelog, and
     install instructions; any broken links to APK files.

2. Security
   - firestore.rules: re-verify every collection has both a read AND write
     rule, no collection is left with a permissive default, isAdmin()/
     isOwner() are applied everywhere they should be, and the anti-cheat
     validators can't be bypassed by a crafted write (e.g. missing field
     checks, integer overflow, negative deltas).
   - admin-website/admin/js/auth.js and firebase-config.js: is the admin
     Firebase client config safe to expose client-side (it should be —
     confirm no service-account or privileged key is embedded); is auth
     state checked on every admin page load, not just at login; any client-
     side-only "isAdmin" checks that aren't backed by a Firestore rule or
     custom claim.
   - functions/set_admin_claim.js and any other privileged function: who
     can invoke it, is it callable-only or does it have an HTTP endpoint,
     is the caller's own admin status verified before granting claims to
     someone else.
   - Secrets hygiene: grep for hardcoded API keys, tokens, or credentials
     across app/, admin-website/, functions/, and scripts/ (not just
     .env files — check committed JS/Kotlin/JSON too).
   - Android release signing: confirm keystore.properties and the .jks are
     genuinely gitignored and not present in git history.

3. Database (Firestore + Room)
   - Schema/rule drift: does firestore.rules match the actual document
     shapes the app and functions write (check app/src/.../data/remote and
     functions/*.js against the rules' field-level validators).
   - Room migrations (app/schemas, app/src/.../data/local): every version
     bump 1->19 has a corresponding migration object, migrations are tested
     (check app/src/androidTest), no destructive fallback migration is used
     in production build config.
   - Free-tier (Spark) cost/quota risk: any query pattern doing unbounded
     collection scans, per-frame or per-scroll Firestore reads instead of
     listeners/caching, or lack of pagination on admin dashboard lists.
   - Orphaned or inconsistent data paths: fields written by one code path
     but never read, or read but never written/defaulted (leads to null
     crashes).

4. Performance, reliability & release pipeline
   - Crash-prone patterns: force unwraps / non-null assertions on Firebase
     or Room data, missing try/catch around network calls, unhandled
     coroutine exceptions.
   - CI (.github/workflows/ci.yml): does it actually gate merges on test
     failure, is the Firestore rules deploy step scoped safely (won't
     deploy on a failing build).
   - Release (.github/workflows/release.yml): version/tag validation logic
     for edge cases (e.g. tag without matching Gradle bump), keystore
     secret handling, whether a failed release can leave a half-published
     app_releases Firestore doc.
   - Anything relying on Firebase Storage or Blaze-only features that
     would silently fail on the Spark plan.

OUTPUT FORMAT:
Produce a findings report grouped by the four areas above. For each finding
include:
  - Severity: Critical / High / Medium / Low
  - Location: exact file:line
  - What's wrong and the concrete failure scenario (not just "this could be
    an issue")
  - Suggested fix (respecting the Spark-plan constraint where relevant)
Order findings most-severe-first within each area. If an area has no real
findings, say so explicitly rather than inventing minor nitpicks.
```

---

*Maintained alongside `docs/PHASE1_RUNBOOK.md` and `docs/MONITORING.md`. Update the SYSTEM CONTEXT block above if the stack, plan tier, or CI/release workflows change materially.*
