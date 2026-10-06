# Web app parity with the APK

The web app (`admin-website/webapp`, see [WEB_APP.md](WEB_APP.md)) is the same app as the APK for
learners who cannot install it. **Every APK release updates it too.** Three things keep that true:

1. **Shared files are copied, not redrawn.** `npm run sync:web` copies the shipping corpus
   (`DatabaseSeeder.kt`) and the illustrations in `res/drawable-nodpi` into the web app.
   `npm run check:web-sync` fails CI when any copy differs from the Android source.
2. **The version matches.** `webapp/tests/version.test.ts` fails when `versionName` in
   `app/build.gradle.kts` and the web app's version differ.
3. **Each APK version has a row below.** `check:web-sync` fails when the current `versionName`
   has none. The row says what the web app got and what stays Android-only, and why.

When you release: bump the version in both apps, run `npm run sync:web`, port the release's
behaviour changes to `webapp/src/domain/` (with the Android tests restated in `webapp/tests/`), and
add the row.

## Ledger

Status: **Ported** (in the web app), **Android-only** (on purpose, with the reason), **Not yet**
(owed; the work list).

| Version | APK change | Web |
|---|---|---|
| 1.18.0 | Forest look with Light, Dark and System themes | Ported (2026-10-06): the light palette from `Color.kt`, Settings → Appearance, System by default |
| 1.18.0 | Adjustable text size | Android-only: browsers have their own text size and zoom, which the web app follows |
| 1.18.0 | Profile scenery, streak page, weekly / all-time / streak rankings, 11 badge families | Ported |
| 1.19.0 | Bottom bar: the open tab grows into a labelled pill, solid icons | Ported (2026-10-06): `KasiGuruBottomBar` sizes and Bold icons |
| 1.19.0 | Continue card with section, lesson number and progress | Ported (2026-10-06): `ContinueCard` layout, "Lesson N of M", segmented bar |
| 1.19.0 | Redrawn learning path with a smooth trail | Ported (2026-10-06): `LearningPath.kt` geometry, trail, captions and section banner |
| 1.19.0 | Compact update card | Android-only: the web app updates itself on every deploy |
| 1.20.0 | Streak page: today's checklist, week, next streak badge | Ported |
| 1.20.0 | Recovery questions in Settings | Ported |
| 1.20.0 | Me page: XP ring, badge wall, full dictionary entries | Ported |
| 1.20.0 | Home: one-row header with streak and XP chips; the week moved to the streak page | Ported (2026-10-06): `HomeHero` |
| 1.20.0 | Splash on plain green | Android-only: no splash screen on the web |
| 1.21.0 | Illustrated badges, six tier frames, locked art in grey | Ported (2026-10-06): the 60 images via `sync:web`, `BadgeMedal family=` |
| 1.21.0 | Category icons in a three-across Library grid | Ported (2026-10-06) |
| 1.21.0 | Floating navigation bar | Ported (2026-10-06) |
| 1.21.0 | Update card on Home | Android-only: see 1.19.0 |
| 1.22.0 | My words page with lesson / review / game filters | Not yet |
| 1.22.0 | Home quick practice opens My words; Me counts words met | Not yet |
| 1.22.0 | Folk stories coming soon; Story Reader badge shows Coming soon | Ported |
| 1.23.0 | Sound effects for found words, completions, streaks, badges, card flips | Not yet: the APK's Ogg files need converting for iPhones |
| 1.23.0 | Background music and volume sliders | Not yet |
| 1.24.0 | XP page, badge summary on Me, tap sounds | Ported |
| 1.24.0 | Downloads and installs updates inside the app | Android-only: see 1.19.0 |
| 1.24.0 | Loads the dictionary only when it changed | Ported: the web app pulls only documents changed since its snapshot |
| 1.25.0 | Streak: no review due means three games keep it; a future last-active date keeps the run | Ported (2026-10-06): `Draft.quota`, `updateStreak` |
| 1.25.0 | Update pop-up | Android-only: see 1.19.0 |
| 1.25.0 | Reminders at the chosen time; 8:00 AM Word of the Day notification; inbox times | Android-only: browsers cannot schedule reliable reminders |
| 1.25.0 | Home and the Library share one word of the day | Ported (2026-10-06): the same word list and order as a phone, so both apps pick the same word |
| 1.25.0 | The shipping corpus under the cloud dictionary (since 1.0) | Ported (2026-10-06): `corpus.json` + `contentMerge.ts`, the APK's merge rules |
