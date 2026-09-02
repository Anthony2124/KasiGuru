# KasiGuru — handoff prompt

Paste everything below into a new agent session, working in `C:\KasiGuru\KasiGuru-main`.

---

You are continuing a UI/UX redesign of KasiGuru's two web surfaces. Read this whole brief before acting.

## The project

KasiGuru is an Android app (`com.kasiguru`, Jetpack Compose, minSdk 26) for learning **Kasiguranin**, an
endangered Northern Philippine language spoken in Casiguran, Aurora. It is the software artifact of an
undergraduate thesis. It ships alongside two web surfaces in the same repo:

- `admin-website/download/` — the public APK download site. **Persuade** mode. Vercel project `download`,
  live at `https://download-woad-iota.vercel.app`.
- `admin-website/admin/` — the internal moderation portal. **Operate** mode. Vercel project `admin`,
  live at `https://kasiguru-admin.vercel.app`.

`DESIGN.md` at the repo root is the design authority for the app **and** both web surfaces. `PRODUCT.md`
holds product truth and the mode assignments. Read both first — they are short and they are binding.

## Standing instructions from the user (Adrian)

1. **Run design work through three skills, every time**: `impeccable:impeccable`, `ui-ux-pro-max`, and the
   taste skill (`design-taste-frontend`, installed from `https://github.com/Leonxlnx/taste-skill`). He has
   asked for this repeatedly. `ui-ux-pro-max` needs Python at
   `C:/Users/U S E R - P C/AppData/Local/Programs/Python/Python313/python.exe` — the `WindowsApps` python on
   PATH is a Store stub that only prints an install prompt.
2. **Adrian authors all artwork himself.** Never generate illustrations, mascots or screenshots. Expose art
   positions as optional slots and make every layout look finished with none present.
3. **Never invent Kasiguranin.** It is the endangered language the thesis documents. Generating plausible
   Kasiguranin would fabricate primary research data. The ten story pages currently carry
   `kasiguranin: ""` deliberately, and the reader degrades honestly around that.
4. **Firebase stays on the free Spark plan.** No Cloud Functions deploy, no Admin SDK in a browser, no
   Firebase Storage. Anything needing those runs as a local Node script with a service-account key.
5. **Verify visually, don't assume.** Render pages at real phone width and look. Several defects in this
   project were invisible in the CSS and obvious in a screenshot.

## Where things stand

The last release shipped is **v1.10.0 (versionCode 11)**. HEAD is `cae88a1`.

**The admin portal is not up to date on the web.** Nothing deploys automatically — no Vercel project has
a Git link, so the site only changes when a person runs `vercel --prod`. The last production deploy of the
`admin` project was 2026-08-25, so the bug-report moderation UI, the user email and registration-date
columns, and the account-deduplication fixes are all in the repo and not on the live site. Check before
assuming: `curl -s https://kasiguru-admin.vercel.app/js/app.js | wc -l` against
`wc -l admin-website/admin/js/app.js`.

### Already done and committed

- `admin-website/shared/tokens.css` and `shared/components.css` — the canonical design layer, ported from
  `app/src/main/java/com/kasiguru/ui/theme/`. Copies synced into both surfaces' `css/`.
- `scripts/sync-web-shared.js` — copies shared → both surfaces; `--check` mode exits 1 if a copy is stale.
- **Typography fixed.** Nunito (display) + DM Sans (body) are wired into both surfaces; zero `Outfit`
  references remain.
- **New download pages**: `install.html`, `releases.html`, `faq.html`, plus `js/releases.js`.
- Landing page and admin dashboard restructured; `DESIGN.md` gained an 85-line web section.
- `functions/publish_release.js` partially updated.
- The invisible white-on-white release heading is fixed.

### Outstanding — your work

Re-measured 2026-09-02. Items 1, 2, 3 and 6 from the previous handoff are **done** — the users table is
6 columns and its `colspan="6"` is correct, only 3 `alert`/`confirm` call sites remain (down from 26, and
the survivors are in comments), 22 `data-label` attributes are in place, and no `--play-` / `--vocab-` /
`--coast-` / `--sand-bg` aliases remain. What is genuinely still open:

| # | Task | Evidence it is still open |
|---|---|---|
| 1 | **The shared CSS layer is gone.** The previous handoff asked for a CI staleness check on `scripts/sync-web-shared.js`; that script and `admin-website/shared/` were both deleted by `db24c80` ("Revert the web redesign and its follow-up fixes"). `admin/css/styles.css` and `download/css/styles.css` are now independent copies again with nothing keeping them in step. Decide deliberately: reinstate a shared layer plus its check, or accept the two surfaces as separate and say so here | `scripts/sync-web-shared.js` does not exist |
| 2 | **Old release downloads all 404.** `functions/backfill_app_releases.js` was written to fix this and has never been run: 7 of 9 `app_releases` docs still carry versioned `apkUrl`s, 6 have empty `releaseNotes`, 5 have no `releasedAt`. `kasiguru-v1.1.0.apk` and `kasiguru-v1.6.0.apk` both return 404 live; only `kasiguru-latest.apk` returns 200 | run the script with `--apply` |
| 3 | **Two legacy Vercel projects publish a stale admin portal to the public.** `kasi-guru-iota.vercel.app/admin/js/app.js` and `admin-website-sandy.vercel.app/admin/js/app.js` both return 200; the first is a 1,235-line copy against the current 3,326. `.vercelignore` now stops a *future* root deploy carrying them, but the already-published deployments have to be taken down in the Vercel dashboard | both URLs return 200 |
| 4 | **Report photos have no client-side size budget.** `firestore.rules:315` caps `photoBase64` at 700,000 chars; `ImageCompressor.kt` compresses to 1024px at quality 75 and never checks the result, so an oversized photo fails as a generic permission error | no size check in `ImageCompressor.kt` |

## The release pipeline — the one thing that can break distribution

**The defect:** `.gitignore` excludes `*.apk`, so CI checks out a tree with zero APKs and copies in only the
one it just built. Every production deploy of the download site contains exactly one APK, so **every older
`apkUrl` in `app_releases` 404s.**

**The agreed fix:** stop hosting APKs on the Vercel deployment. Attach them as **GitHub Release assets**
(`softprops/action-gh-release@v2`, needs `permissions: contents: write`), and point `apkUrl` at
`https://github.com/Anthony2124/KasiGuru/releases/download/v<name>/kasiguru-v<name>.apk`. GitHub keeps assets
permanently. Keep the Vercel deploy — the site still changes per release, it just stops carrying binaries.

**Do not** commit APKs to git instead: ~8 MB per release forever, and this audience is on poor connectivity.

**Also unify the two writers.** `app_releases` currently has two: CI writes only
`versionCode/versionName/apkUrl`, while the admin writes those plus `releaseNotes/forceUpdate/releasedAt`.
CI-published releases therefore render "Invalid Date" and "No release notes provided". Target contract:

```
app_releases/v<versionName>     // deterministic id in BOTH writers; the admin used addDoc, allowing duplicates
  versionCode, versionName, apkUrl, releaseNotes, forceUpdate, releasedAt
```

Use `set(..., { merge: true })` and only set `releasedAt`/`releaseNotes` when the doc does not already exist,
so re-running a workflow cannot clobber notes an admin edited afterwards.

**Verify on a throwaway tag before a real release.** The workflow hard-fails if the tag does not match
`versionName` in `app/build.gradle.kts`.

## What NOT to build

The brief Adrian supplied asks for these. The data does not exist and most need a paid plan. Report them as
gaps; do not ship empty screens.

- **User roster, registration dates, per-user progress, achievements, app version, account status.**
  `firestore.rules:165-180` scopes `users/{uid}` to the owner; `isAdmin()` is never applied there, so an
  admin browser gets `permission-denied`. Listing Auth users needs the Admin SDK.
- **Download counts, growth, engagement, any chart.** There is no analytics at all — no Firebase Analytics
  dependency, zero `logEvent` calls, no events collection. The APK link is a plain `<a href>`, so downloads
  are not observable.
- **In-browser APK upload.** Storage is not enabled: `storage.rules` is entirely commented out and absent
  from `firebase.json`.
- **Lesson/quiz editors.** Lessons are not authored content — a unit *is* a vocabulary category and a lesson
  is a 7-word slice computed at runtime (`domain/lesson/LessonPlan.kt:27-45`). Editing vocabulary *is*
  editing lessons.

**The one real user view that IS possible** — and is already partly built — reads `leaderboard_public`
(`displayName, totalXp, level, currentStreak, weeklyXp, titleBadge, updatedAt`). It only contains learners
with XP above zero, so **the UI must say so**, or someone will read it as a full roster.

## Design rules you will get wrong if you skip DESIGN.md

- **Clay is reserved** for things you earn or press — the primary CTA, publish/approve/reject. Never ordinary
  content. Content uses `.soft-card`. Building content out of clay tips the app into a toy, and the audience
  includes a thesis panel.
- **Reward hues are fills, never foregrounds.** Gold measures 1.83:1 on white and coral 2.31 — below even the
  3:1 non-text floor. Text on gold/coral is `--reward-ink`, never white.
- **Glass needs a genuinely vivid backdrop.** Over the lavender `--ground` it degrades to a muddy card. Legal
  only on the violet band and the release header.
- **Refuse list, all of which the web violated:** eyebrow/kicker labels above headings, three equal cards as
  page structure, emoji as interface, gradient text, glass over the Ground.
- **Contrast is measured, not estimated.** DESIGN.md's figures came from a real WCAG calculator. A working
  one is at `scratchpad/contrast.py` in the previous session's temp dir; rewrite it if absent — it is 20
  lines. Published pairings: ink/ground 14.43, muted/ground 5.67, ink/card 16.46, violet/card 6.00,
  reward-ink/gold 9.00, reward-ink/coral 7.14.
- **Reduced motion is a contract**, not a courtesy: nothing may depend on an animation having run.

## Verification techniques that work here

- **Render on the emulator.** AVD `kasi_test`. Serve a surface locally, `adb reverse tcp:PORT tcp:PORT`, open
  `http://localhost:PORT` in Chrome, `adb shell screencap`. This is how the previously-unreachable "Get App"
  button was found — it was clipped off-screen and invisible in the CSS.
- **The admin dashboard is auth-gated.** Verify it with a scratchpad preview that lifts the real markup and
  stylesheet into a standalone page. Do not stub the auth guard.
- **`uiautomator dump` for ground-truth tap coordinates** — estimating from a screenshot has missed by 200+px
  on this project. Delete the device-side dump first; `/sdcard` persists across emulator restarts and a
  failed dump silently pulls a stale file.
- Run impeccable's detector over changed files at the end.

## Shipping

**Hosting belongs to Anthony Cordial**, matching the GitHub repository (`github.com/Anthony2124/KasiGuru`).
Anthony runs the deploys; do not deploy from Adrian's account, and do not treat a logged-in Vercel CLI
session as permission to. `scripts/deploy_web.ps1` now reads the owning account slug from `$env:VERCEL_SCOPE`
rather than hard-coding one, and refuses to deploy a folder whose `.vercel/project.json` names a different
project than the one that entry is for.

`git push origin main` runs CI, which deploys `firestore.rules`. Pushing a `vX.Y.Z` tag runs the release
workflow: builds and signs the APK, deploys the download site to Vercel, writes the `app_releases` doc.
**The admin panel is a separate Vercel project that the workflow does not touch** — deploy it with
`npx vercel deploy --prod --yes` from `admin-website/admin/`, and only from that folder: the repo root's
`admin-website/` is a public placeholder, not a second copy of the portal.

If the Vercel projects move between accounts, `release.yml` breaks silently: lines 164-165 hard-code
`VERCEL_ORG_ID` and `VERCEL_PROJECT_ID`, and `secrets.VERCEL_TOKEN` belongs to whoever issued it. Update
all three, or the next tagged release fails at the deploy step and the workflow then refuses to announce it.

**Main is shared.** Another contributor pushes to it regularly; three commits landed mid-session once. Always
`git fetch` and rebase rather than force-pushing, and verify both sides survived a clean rebase — a clean
apply is not the same as a correct result when you have edited the same files.

## Known issue worth flagging to Adrian

The corpus has **duplicate content arriving from Firestore sync**. `FirestoreSyncManager.syncStories()` pulls
the remote `stories` collection and overwrites seeded stories by numeric id. The same pattern likely explains
the vocabulary count discrepancy (the app reports ~496 where PRODUCT.md says 417). The admin panel now has a
Stories tab that can delete the offending documents.
