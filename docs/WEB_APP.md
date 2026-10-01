# KasiGuru on the web (iPhone, iPad and computers)

`admin-website/webapp/` is the learner app as an installable web app (a PWA). It exists for
learners who cannot install the APK — above all iPhone and iPad users, since iOS does not allow
side-loading — and for anyone on a computer.

It is the same app, not a companion: the same lessons, review scheduling, games, stories, badges and
leaderboard, on the same Firebase project. **An account works on both.** A learner who signs in on
the website with the Google account or email they use on Android gets their XP, streak, badges,
lesson progress and word reviews, and whatever they do on the web flows back to the phone.

## Why a web app and not a native iOS app

| | Web app (this) | Native iOS app |
|---|---|---|
| Cost | Free (Vercel + Spark plan, as today) | US$99/year Apple Developer Program |
| Build machine | This Windows PC | A Mac with Xcode |
| Distribution | A link; *Share → Add to Home Screen* | App Store review, or TestFlight |
| Updates | Every deploy, instantly | App Store review per release |
| Code | New TypeScript front end over the same data and rules | A second full client (Swift, or a Kotlin Multiplatform rewrite of the Android app) |

Installed from Safari, the web app opens full screen from its own home-screen icon and works
offline. If a native iOS app is ever wanted, the path would be Kotlin/Compose Multiplatform, which
needs a Mac and the paid Apple account either way.

## What is in it

- **Onboarding** — the same fifteen beats as Android. The "Let Jepjep remind you" step is replaced
  by "Keep KasiGuru on your home screen", with the Safari instructions, because a browser cannot
  deliver the reminders reliably.
- **Home, Learn, Practice, Library, Me** — the five tabs, in the Jepjep's Forest theme, with the
  bundled Fredoka/DM Sans fonts, the Iconsax icons (converted from the Android library's own
  vectors), Jepjep, the avatars and the Casiguran scenes.
- **Lessons** — the learning tree, the lesson player (all seven exercise shapes), expanding
  rehearsal of misses, remediation lines, mastery checkpoints.
- **Review** — the flashcard deck, SM-2 with lapses and the relearning ladder.
- **Games** — Word Match, Word Search (per category, drag or tap) and Word Wheel (swipe or tap).
  The other five show *Coming soon*, exactly as the Android app ships them.
- **Library** — dictionary with search, categories, word of the day, recordings from `word_audio`;
  stories with page pictures from `story_page_images`.
- **Me** — profile, badges, settings, account (Google or email, sign-out, delete account),
  leaderboard, contribute a word, share a story (PDF), report an issue (with a photo).

## How it stays compatible with Android

The rules that decide what a learner's progress *is* are ports of the Kotlin, with the Android
unit tests ported alongside them (`tests/domain.test.ts`):

| Web (`src/domain/`) | Android |
|---|---|
| `sm2.ts` | `util/srs/Sm2Algorithm.kt`, `ReviewRatingMapper.kt` |
| `merge.ts` | `LearningStateMerge.kt`, `mergeProgress` / `toMap` / `toEntity` in `ProgressSyncManager.kt` |
| `lesson.ts` | `domain/lesson/*`, `LessonRepository.kt` |
| `learner.ts` | `UserProgressRepository`, `VocabularyRepository.processWordReview`, `GameLevelRepository` |
| `wordSearch.ts`, `wordWheel.ts` | `domain/wordsearch`, `domain/wordwheel` |
| `kotlinRandom.ts` | `kotlin.random.Random` (XorWow), checked against kotlin-stdlib output |

Sync (`src/lib/sync.ts`) reads and writes the same five documents under
`users/{uid}/progress/` with the same keys (`theme:pamilya#0`, `word_match_1`, lowercase
headwords…), merges additively, and publishes `leaderboard_public/{uid}` for signed-in accounts
only. The main document carries exactly the keys `isValidMainProgress()` allows; a test asserts it.

**If you change a rule on Android, change it here too** — the merge rules, the progress fields, the
XP values, the lesson slicing and the badge list above all. A field added to
`UserProgressEntity` needs adding to `types.ts`, `merge.ts` (`MAIN_PROGRESS_KEYS`, `toMainDoc`,
`fromMainDoc`, `mergeProgress`) and `firestore.rules`, the same parity the Kotlin tests guard.

### Deliberate differences

- **Word Match options** use the lesson's meaning format ("tagalog · english", English when the
  Tagalog only repeats the headword). On Android a word like *mainit* offers "mainit" as its own
  answer. Worth porting back to `WordMatchViewModel`.
- **Badge XP recalculates the level** straight away. Android adds a badge's XP to `totalXp` but
  leaves `level` until the next XP award, so Profile can briefly show the next rank beside the old
  level. The merge takes the higher level, so the two converge.
- **A listening exercise whose clip cannot load** shows the word's meaning instead, so it stays
  answerable offline.
- **Copy that was no longer true** was adjusted: "Over 1,100 words" (the live corpus is 1,172),
  and the Word Match rules no longer promise a combo multiplier the game does not have. The
  streak dialog does not claim a "+25 XP streak bonus", which neither app awards.
- **No guided tour, notifications, or profile switching.** The Help page covers the tabs instead.

## Content and the free-plan read budget

A web visitor has no APK carrying the dictionary, so a naive client would read all ~1,200
`vocabulary` documents on every first visit — about forty visitors a day would exhaust the Spark
plan's 50,000 reads for the whole project, Android included. Instead:

- `npm run build` runs `scripts/export-content.mjs`, which snapshots `vocabulary` and `stories`
  (plus the seven stories only the APK ships, parsed from `DatabaseSeeder.kt`) into
  `public/content/*.json`. The committed snapshot is used if Firestore cannot be reached.
- In the browser, the app asks Firestore only for documents with `updatedAt` after the snapshot,
  at most every six hours — usually a single read that returns nothing.
- Recordings and story pictures are fetched one at a time when needed and cached.

Admin edits and additions reach web learners within six hours. **A word deleted in the admin portal
disappears from the web app at the next deploy**, since deletions are not in the delta; redeploy
(or re-run the Vercel build) after removing words.

## Deploying (Vercel)

**Live at https://kasiguru-app.vercel.app** — Vercel project `kasi-guru/kasiguru-app`, created
2026-10-01.

- It is connected to the GitHub repository with **Root Directory `admin-website/webapp`** and
  production branch `main`, like the admin portal: merging to `main` redeploys it, and that
  production build also refreshes the content snapshot. Branch pushes get preview deployments,
  which use the committed snapshot and spend no Firestore reads.
- `vercel.json`'s `ignoreCommand` skips the build when a push changes nothing under
  `admin-website/webapp`, so Android-only commits do not redeploy it (or re-read the dictionary).
  To pick up deleted words without a web change, redeploy from the Vercel dashboard.
- Because the Root Directory is set, `vercel deploy` run from inside `admin-website/webapp` fails
  with "Root Directory does not exist" (the same as the admin project). Deploy by pushing to
  `main`, or from the dashboard.
- The download page (`admin-website/download`) links to it: on iPhone and iPad its main button is
  **Open KasiGuru** and the APK buttons are hidden; elsewhere "Use it in your browser" sits beside
  the APK download.

### Google sign-in setup (done 2026-10-01)

Google sign-in runs through the app's own domain, so it keeps working in an installed iPhone app,
where Safari's storage partitioning breaks a sign-in handler on another domain:

1. **Firebase console → Authentication → Settings → Authorized domains** lists
   `kasiguru-app.vercel.app`.
2. **Google Cloud console → APIs & Services → Credentials**, web OAuth client: JavaScript origin
   `https://kasiguru-app.vercel.app`, redirect URI `https://kasiguru-app.vercel.app/__/auth/handler`.
3. Vercel environment variable `VITE_AUTH_DOMAIN=kasiguru-app.vercel.app` (Production only).
   `vercel.json` proxies `/__/auth/*` and `/__/firebase/*` to `kasiguru-86042.firebaseapp.com`.

The app's Content-Security-Policy deliberately skips `/__/` (`"source": "/((?!__/).*)"`). Those are
Firebase's pages: the auth iframe has to be framed by the app, which `frame-ancestors 'none'` forbids,
and the handler runs an inline script.

Preview deployments do not get `VITE_AUTH_DOMAIN`, and their domains are not authorized, so test
Google sign-in on production or against the emulators. If the app moves to a custom domain, repeat
all three steps for it.

No Firebase rules or plan changes are needed: the web app uses exactly the access the Android app
already has.

### Announcements need no index here

The web app filters `announcements` by `active` and sorts by `createdAt` in the browser. The
Android app's query (`whereEqualTo("active", true).orderBy("createdAt", DESC)`) needs a composite
index the project does not have, so it fails with FAILED_PRECONDITION and the Android banner never
shows anything. Either create the index (Firestore → Indexes: `announcements`, `active` ↑,
`createdAt` ↓) or drop the `orderBy` in `AnnouncementRepository` and sort on the device.

## Developing and testing

```powershell
cd admin-website/webapp
npm install
npm test            # domain rules, ported from the Android unit tests
npm run dev         # http://localhost:5173 against the LIVE project - careful
```

To test without touching live data, run the Auth and Firestore emulators with the real rules and
point the app at them:

```powershell
# from the repo root, with a JDK on PATH (see the dev tooling notes)
npx firebase emulators:start --only auth,firestore --project kasiguru-86042
# in admin-website/webapp
$env:VITE_FIREBASE_EMULATORS = '1'; npx vite
```

`npm run icons` regenerates the icons, art and fonts from the Android sources (it finds the Iconsax
library in the Gradle cache), and `npm run content` refreshes the content snapshot.
