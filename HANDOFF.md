# KasiGuru — handoff prompt

Paste everything below into a new agent session, working in `C:\KasiGuru\KasiGuru-main`.

---

You are continuing a UI/UX redesign of KasiGuru's two web surfaces. Read this whole brief before acting.

## The project

KasiGuru is an Android app (`com.kasiguru`, Jetpack Compose, minSdk 26) for learning **Kasiguranin**, an
endangered Northern Philippine language spoken in Casiguran, Aurora. It is the software artifact of an
undergraduate thesis. It ships alongside two web surfaces in the same repo:

- `admin-website/download/` — the public APK download site. **Persuade** mode. Vercel project
  `kasi-guru/kasiguru-download` (Anthony Cordial's account, moved 2026-09-04), live at
  `https://kasiguru-download.vercel.app`.
- `admin-website/admin/` — the internal moderation portal. **Operate** mode. Vercel project
  `kasi-guru/admin-wheat-nu-52` (Anthony's account), live at `https://admin-wheat-nu-52-phi.vercel.app`.

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

The last release shipped is **v1.15.0 (versionCode 16)**, published 2026-09-08 — the first release to
run end to end since v1.10.0, and the first through the GitHub Release asset pipeline. v1.11.0 through
v1.13.0 were published by hand and v1.14.0 failed outright; its tag still points at `aa43af3` and was
deliberately skipped rather than moved.

**The two Vercel projects deploy differently, and it matters.** The `admin` project
(`kasi-guru/admin-wheat-nu-52`) *is* Git-linked: it builds on every push to `main`, with its Root
Directory set to `admin-website/admin`, which is why a CLI deploy from inside that folder fails with
"Root Directory does not exist". Push to deploy it; do not run `vercel` there. The `download` project
(`kasi-guru/kasiguru-download`) has no Git link and is deployed by hand from its own folder, or by the
release workflow.

**The download site is behind, and the release workflow cannot fix it.** `secrets.VERCEL_TOKEN` was
issued 2026-08-19, before hosting moved to Anthony's account, so every deploy attempt fails with
"Could not retrieve Project Settings". Since 2026-09-08 that failure is `continue-on-error` and only
warns, so releases still complete — but the live download page keeps serving markup whose static
button points at the old Vercel-hosted APK rather than the GitHub Release asset. Anthony has to
reissue the token; Adrian has push access to the repo but not admin, so he cannot set the secret.

### Already done and committed

Verified 2026-09-08 — the previous version of this list described work that `db24c80` ("Revert the web
redesign and its follow-up fixes") had since removed, so check before trusting an entry here.

- **Typography.** Nunito (display) + DM Sans (body) are wired into both surfaces; zero `Outfit`
  references remain. *(Confirmed present.)*
- **The web token contract.** `scripts/check-web-tokens.js` guards the 14-token brand core shared by the
  two surfaces; CI runs it, and DESIGN.md's "The two web surfaces" section records the reasoning.
- **The release pipeline** now hosts APKs as GitHub Release assets, with the migration complete — see
  the next section.
- **Report photo compression** honours the Firestore size cap (`ImageCompressor.kt`).

Gone with the revert, despite what earlier handoffs claimed — do not go looking for them:
`admin-website/shared/`, `scripts/sync-web-shared.js`, the download site's `install.html` /
`releases.html` / `faq.html` and `js/releases.js`, and DESIGN.md's original 85-line web section. The
download site is a single `index.html` today.

### Outstanding — your work

Re-measured 2026-09-08. Items 1 through 4 from the previous handoff are all **closed**:

- **The shared CSS layer** is settled, not restored. The two surfaces share only 17 token names out of
  62 and 46, and 14 of those already agreed exactly — so they are now formally separate stylesheets
  with a guarded brand core. `scripts/check-web-tokens.js` asserts the 14 identity tokens agree and
  records the three that differ on purpose; CI runs it. The reasoning is in DESIGN.md under "The two
  web surfaces". Do not reintroduce `admin-website/shared/` or a sync script.
- **Old release downloads** are fixed. Every archived APK is now a GitHub Release asset, and the
  backfill repointed ten `app_releases` documents at their own version's permanent URL. Re-running it
  reports 12 already correct.
- **The two legacy Vercel projects** are gone: `kasi-guru-iota.vercel.app/admin/js/app.js` and
  `admin-website-sandy.vercel.app/admin/js/app.js` both return 404.
- **Report photo size budget** shipped in `ImageCompressor.kt`: a quality ladder then a scale ladder,
  checked against the rule's 700,000-character cap, failing at attach time with a message the user can
  act on.

What is genuinely still open:

| # | Task | Evidence it is still open |
|---|---|---|
| 1 | **`VERCEL_TOKEN` is stale.** Issued 2026-08-19, before hosting moved to Anthony's account, so the release workflow's deploy step fails every run. It only warns now, so releases complete and the download site is left serving older markup. Only Anthony can fix it — Adrian has push but not admin on the repo | run 34237024472, step 17: "Could not retrieve Project Settings" |
| 2 | **Four releases have no binary anywhere.** v1.7.0–v1.10.0 were never archived and their APKs are lost. v1.7.0 and v1.8.0 already 404; v1.9.0 and v1.10.0 point at a `kasiguru-latest.apk` alias on the *old* Vercel project in Adrian's account, which serves some other build and dies when that project is deleted. Yank them in the admin release manager rather than leave mislabelled links | `node functions/backfill_app_releases.js <key>` lists all four |
| 3 | **v1.15.0 has no release notes.** CI has no source for them, so it writes an empty string; the admin panel's publish form is where they get typed. Safe to add now — `publish_release.js` only seeds `releaseNotes` when the field is absent, so a workflow re-run will not blank them | `app_releases/v1.15.0.releaseNotes` is `""` |

## The release pipeline — the one thing that can break distribution

**The defect (fixed 2026-09-08, not yet migrated):** `.gitignore` excludes `*.apk`, so CI checks out a tree
with zero APKs and copies in only the one it just built. Every production deploy of the download site
contained exactly one APK, so **every older `apkUrl` in `app_releases` 404ed.**

**The fix, now implemented in `release.yml`:** APKs are no longer hosted on the Vercel deployment. They are
attached as **GitHub Release assets** (`softprops/action-gh-release@v2`, `permissions: contents: write`), two
per release — `kasiguru-v<name>.apk`, the permanent versioned asset `app_releases.apkUrl` points at, and
`kasiguru-latest.apk`, a fixed asset name so `/releases/latest/download/kasiguru-latest.apk` always resolves.
That second URL is the download page's static href. The Vercel deploy still runs so the site's copy stays
current, but it is now `continue-on-error` — the site no longer carries the binary, so a failed deploy must
not withhold a release. (That is precisely what stranded v1.14.0: a `VERCEL_TOKEN` issued by Adrian's
account, which cannot reach the project after hosting moved to Anthony's.)

**Do not** commit APKs to git instead: ~8 MB per release forever, and this audience is on poor connectivity.

**The migration is complete** (2026-09-08). `scripts/archive-apks-to-releases.sh --apply` uploaded the
nine surviving archived APKs to their GitHub Releases, and
`functions/backfill_app_releases.js --apply` repointed ten `app_releases` documents at their own
version's permanent asset; a re-run now reports 12 already correct. The only gap is v1.7.0–v1.10.0,
whose binaries were lost before the archive ran.

Both scripts are idempotent and worth re-running rather than reasoning about: the backfill without
`--apply` is a read-only report of exactly which documents disagree with the assets that exist.

**The two writers are unified.** Both `functions/publish_release.js` and the admin dashboard's publish form
write the deterministic id `app_releases/v<versionName>` with `set(..., { merge: true })`:

```
app_releases/v<versionName>
  versionCode, versionName, apkUrl, releaseNotes, forceUpdate, releasedAt
```

CI always writes the build facts (`versionCode`, `versionName`, `apkUrl`) and seeds `releaseNotes` /
`forceUpdate` / `releasedAt` only when the doc does not already carry them. `merge: true` alone was not
enough for that — merge only protects fields absent from the payload, and the old unconditional
`releaseNotes: ''` meant a re-run blanked notes an admin had typed.

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
