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
  rehearsal of misses, remediation lines, mastery checkpoints, the "N in a row" chip, and the
  streak page after the day's first lesson.
- **Review** — the flashcard deck, SM-2 with lapses and the relearning ladder.
- **Games** — Word Match, Word Search (per category, drag or tap) and Word Wheel (swipe or tap).
  The other five show *Coming soon*, exactly as the Android app ships them.
- **Library** — dictionary with search, categories, word of the day, recordings (see below);
  My words (Android 1.22: every word met in a lesson, review or game, with filters; kept on the
  device like `WordEncounterRepository`, and refilled from history after a sign-in); stories with
  page pictures from `story_page_images`.
- **Me** — profile with scenery (seven places, unlocked by level or streak), all eleven badges on
  one grid (pinned ones first, each at its highest tier with a six-dot track) under the badge closest
  to its next tier, your weekly rank, settings, account (Google or email, sign-out, delete account),
  contribute a word, share a story (PDF), report an issue (with a photo).
- **Badges, streak, XP, rankings** — eleven badge families with six permanent tiers each (and the
  original badges under Legacy), the streak page, the XP page (Android 1.24: total and level, today
  against the daily goal, the last seven days, XP by source, game stars, Journey Rank milestones;
  opened from the XP chip on Home or the total on Me), and the weekly / all-time / streak
  leaderboards, where a row opens that learner's public profile.
- **Tap sounds** — the soft click on buttons, tabs and links (Android's `TapSounds.kt`), with its own
  switch in Settings. A press with a finger or mouse clicks; a keyboard or screen-reader activation
  stays silent, and a control whose tap already plays an answer sound carries `data-no-tap-sound`.
- **Sound effects and music (Android 1.23)** — the nine effects of `SoundEffects.kt` (correct, wrong,
  found, complete, level up, streak, badge, card flip, tap) played where Android plays them, and the
  menu and game loops of `MusicPlayer.kt`, chosen by route as `musicMoodFor` does (`src/domain/music.ts`:
  silent in lessons, stories, the review deck and onboarding). Settings has Sound effects, Tap sounds and
  Background music switches with the two 0–100% volume sliders; 50% is the level the sounds were tuned
  at. Music starts on the first tap (browsers allow nothing sooner), stops while the page is hidden,
  and drops to a fifth under a word recording, as audio focus ducks it on Android (`src/lib/music.ts`).
- **Appearance (Android 1.18)** — System (the default), Light and Dark in Settings. See *Light theme*
  below. Text size is the browser's own, as [WEB_PARITY.md](WEB_PARITY.md) records.
- **Guided tour (Android 1.18)** — the core chapter runs once after onboarding; the other eight
  chapters are offered on the help page and under Settings → Tutorial chapters, with the same New,
  Updated, Done and Continue states (`src/domain/tour.ts`, `src/lib/tour.ts`, `src/ui/tour.tsx`).
  Stops find their element by `data-tour="<Anchor>"`; a test checks every anchor a stop names is
  attached somewhere.

## How it stays compatible with Android

The rules that decide what a learner's progress *is* are ports of the Kotlin, with the Android
unit tests ported alongside them (`tests/domain.test.ts`, `tests/xp.test.ts`):

| Web (`src/domain/`) | Android |
|---|---|
| `sm2.ts` | `util/srs/Sm2Algorithm.kt`, `ReviewRatingMapper.kt` |
| `merge.ts` | `LearningStateMerge.kt`, `mergeProgress` / `toMap` / `toEntity` in `ProgressSyncManager.kt` |
| `lesson.ts` | `domain/lesson/*`, `LessonRepository.kt` |
| `xp.ts` | `domain/gamification/XpPolicy.kt` (`XpPolicy`, `RewardLedger`), `XpSummary.kt`, `data/remote/RewardReceiptCodec.kt` |
| `badges.ts` | `BadgeCatalog`, `LegacyBadgeCatalog`, `BadgeShowcase`, `BadgeSummary`, `ProfileBackgroundCatalog` |
| `learner.ts` | `GamificationRepository`, `UserProgressRepository`, `VocabularyRepository.processWordReview` |
| `publicProfile.ts` | `data/remote/model/PublicProfileDto.kt` |
| `wordSearch.ts`, `wordWheel.ts` | `domain/wordsearch`, `domain/wordwheel` |
| `kotlinRandom.ts` | `kotlin.random.Random` (XorWow), checked against kotlin-stdlib output |

Sync (`src/lib/sync.ts`) reads and writes the same five documents under
`users/{uid}/progress/` with the same keys (`theme:pamilya#0`, `word_match_1`, lowercase
headwords…), merges additively, and publishes `leaderboard_public/{uid}` and
`public_profiles/{uid}` for signed-in accounts only. The main document carries exactly the keys
`isValidMainProgress()` allows; a test asserts it.

### XP policy 2 (Android 1.18)

XP is never added to a counter. Every reward is a **receipt** with a stable id (`lesson:<unit>#<i>:<day>`,
`review:word:<headword>:<day>`, `badge:<family>:<tier>`…), stored one document each under
`users/{uid}/rewardReceipts/{sha256(id)}`, and total XP, level, today's XP and the badge tiers are
recomputed from their union (`totals()` in `xp.ts`, `settle()` in `learner.ts`). So two devices
cannot pay the same lesson twice, and the 60-a-day review cap applies after combining them.

- Sync reads receipts first by document id, then by server `updatedAt` from a cursor; uploads read
  each receipt in a transaction and keep the stronger evidence. Main progress is written only after
  the receipts are in, because it is their projection (`totalXp = activityXp + badgeBonusXp`).
- A device that still holds pre-1.18 progress is **normalized** once: completed lessons, starred
  game levels and finished stories become imported receipts, the old XP and level are archived and
  shown once on the badges page, and stories and sections the old XP had opened stay open (access
  receipts). A version-1 cloud document is archived the same way and never restores old XP.
- `firestore.rules` refuses any write that lowers `xpPolicyVersion`, so a web app on the old
  rules could not have saved progress for an account that had used Android 1.18.
- The level table (`LEVEL_THRESHOLDS`) and the receipt document ids are checked in the tests
  against values printed by the JVM, so they match Android to the digit.

**If you change a rule on Android, change it here too** — the merge rules, the progress fields, the
XP values, the lesson slicing and the badge list above all. A field added to
`UserProgressEntity` needs adding to `types.ts`, `merge.ts` (`MAIN_PROGRESS_KEYS`, `toMainDoc`,
`fromMainDoc`, `mergeProgress`) and `firestore.rules`, the same parity the Kotlin tests guard.

### Deliberate differences

- **Word Match options** use the lesson's meaning format ("tagalog · english", English when the
  Tagalog only repeats the headword). On Android a word like *mainit* offers "mainit" as its own
  answer. Worth porting back to `WordMatchViewModel`.
- **Onboarding says only what it grants.** Android 1.18 still shows "+50 XP" and "Day 1 streak"
  after the first word, though onboarding now grants neither XP nor a streak. The web shows
  "First word". Worth porting back to `OnboardingSteps.kt`. The badge wall after it is the
  APK's: six families, one at each tier, with their artwork.
- **A badge tier earned on another device stays earned** here as soon as its receipt arrives;
  Android re-derives it from the same evidence, so the two agree.
- **A listening exercise whose clip cannot load** shows the word's meaning instead, so it stays
  answerable offline.
- **Copy that was no longer true** was adjusted: "Over 1,100 words" (the live corpus is 1,172),
  and the Word Match rules no longer promise a combo multiplier the game does not have. The
  streak dialog does not claim a "+25 XP streak bonus", which neither app awards.
- **No notifications or profile switching.** The inbox shows the team's announcements only.
- **Tour copy follows the web app.** The stops are Android's, worded for what the web has: the
  inbox chapter has no filter stop and talks about announcements, the bell stop says "News from the
  team", the flashcard stops describe tapping the card and the four rating buttons (no swipe), the
  daily-goal stop does not offer to change the goal (the web sets it only during onboarding), and
  "phone" reads "browser". The chapter versions are Android's, so the badges agree.
- **Next up skips Story Reader while stories are switched off** (`STORIES_ENABLED`): no activity can
  move it, so Me never suggests it as the badge to work on. Android's `BadgeSummary.nextUp` can still
  pick it for a learner who read stories before they were switched off. Worth porting back.
- **Offline music** is not guaranteed: the loops are not precached (they are most of the audio
  weight), so music plays offline only if the browser still holds it in its HTTP cache. Effects are
  precached and always play.
- **From 1.25:** the streak quota waives the review when no word is due (`DailyStreakQuota.reviewDue`,
  "Nothing due today" on the Streak page), and a last-active date after today keeps the run rather
  than adding a day (`StreakRules.advancedStreak`). The 1.25 update pop-up, the scheduled reminders
  and the Word of the Day notification are Android-only: the web updates on every deploy and has no
  notifications.
- **Vibrations** follow the Android setting where the browser supports them (Android Chrome);
  iPhone browsers cannot vibrate, so the setting is hidden there.

### Light theme

`src/styles.css` carries both palettes from `ui/theme/Color.kt`: the night one on `:root`, the light
one on `:root[data-theme='light']`, which `src/lib/theme.ts` sets from the Settings choice (System
follows `prefers-color-scheme`, and `public/theme-boot.js` applies it before the first paint). As on
Android, the plain accent tokens (`--lime`, `--gold`, `--coral`, `--red`, `--amber`, `--green`) are the
*text* colours, which darken on the light ground (`LimeText`, `GoldText`…), and the `-fill` tokens
(`--lime-fill`, `--gold-fill`…) are the bright fills that never change. **A background, a button face
or a confetti piece takes a `-fill` token; anything read on a surface takes the plain one.**

- `--selected` is the chosen option, tab or chip, read with `--ink`: olive at night, a pale green by
  day. Olive itself stays the fill for found Word Search cells and walked lessons, under cream.
- Words and controls over a scene's dark veil (`.on-scenery`, scene cards, the lesson's scenery
  strip) keep the night ink in both themes.
- The wordmark's "Kasi" follows `--ink`, as `kasiguru_wordmark_light` does on Android.
- An installed iPhone app uses the `black-translucent` status bar, whose glyphs are always white; on
  the light ground a canopy-green strip sits behind them (zero height everywhere else).

## Content and the free-plan read budget

A web visitor has no APK carrying the dictionary, so a naive client would read all ~1,200
`vocabulary` documents on every first visit — about forty visitors a day would exhaust the Spark
plan's 50,000 reads for the whole project, Android included. Instead:

- `npm run build` runs `scripts/export-content.mjs`, which snapshots `vocabulary` and `stories`
  (plus the seven stories only the APK ships, parsed from `DatabaseSeeder.kt`) into
  `public/content/*.json`. The committed snapshot is used if Firestore cannot be reached.
- In the browser, the app asks Firestore only for documents with `updatedAt` after the snapshot,
  at most every six hours — usually a single read that returns nothing.
- Word recordings are served by this site, not Firestore: `npm run audio:export` (repository root)
  copies every `word_audio` clip into the APK's `assets/word_audio`, and `npm run sync:web` copies
  them to `public/audio` with the list in `public/content/audio.json`. Playing one costs no
  Firestore read, so recordings keep working when the day's reads run out (as they did on
  2026-10-04 to 10-07). A played clip is kept on the device; **Settings → Recordings offline**
  saves all of them (~23 MB) for offline use. Only a word re-recorded after the last export is
  fetched from `word_audio`, with the earlier take as its fallback. Re-run both commands after
  recording sessions in the admin portal, then commit and deploy.
- Story pictures are fetched one at a time when needed and cached.
- Leaderboard tabs reuse their top 50 for three minutes, coalesce concurrent requests, and refresh
  when the account or ISO week changes. Android uses the same refresh window over its Room cache.

The admin portal keeps its dictionary in IndexedDB per admin, then listens only for
`updatedAt > checkpoint` changes with a two-minute overlap. A count check on opening and every five
minutes while visible catches withdrawals; a full read once a day catches unstamped edits and
equal-count replacements. Deleting a word in that portal updates its saved copy immediately.
Use `npm run firebase:usage` from the repository root to track the shared Spark budget.

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
and the handler runs an inline script. The service worker skips `/__/` too: it used to cache the
handler page as the offline shell, which showed Firebase's "missing initial state" error.

On iPhones and in installed (home-screen) apps, Google sign-in is a full-page redirect
(`linkWithRedirect` / `signInWithRedirect`), not a pop-up: there the pop-up's return lands in another
tab or storage context and Firebase stops with "missing initial state". `src/lib/auth.ts` marks the
trip in sessionStorage and finishes it in `initAuth`; the Account screen shows the outcome (linked,
signed in, account already exists, or an error). Desktop and Android browser tabs keep the pop-up.

Preview deployments do not get `VITE_AUTH_DOMAIN`, and their domains are not authorized, so test
Google sign-in on production or against the emulators. If the app moves to a custom domain, repeat
all three steps for it.

No Firebase rules or plan changes are needed: the web app uses exactly the access the Android app
already has.

### Announcements index

The web app filters `announcements` by `active` and sorts by `createdAt` in the browser. Android
uses `whereEqualTo("active", true).orderBy("createdAt", DESC).limit(5)`, backed by the composite
index in `firestore.indexes.json` (`active` ↑, `createdAt` ↓). It was deployed to `kasiguru-86042`
on 2026-10-07, verified `READY`, and the exact Android query succeeded. No APK update is needed
for this index fix. There were no active announcements at verification; a Home → News check with
an active announcement is still a phone test.

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

`npm run icons` regenerates the icons, art and fonts from the Android sources (it finds the
Iconsax library in the Gradle cache). The home-screen icons are cut from Adrian's Jepjep launcher art
(`mipmap-xxxhdpi/ic_launcher_foreground.png`) and need sharp, which the app does not depend on:
run `npm i --no-save sharp` first. `npm run content` refreshes the content snapshot.

`npm run sounds` converts the APK's sounds (`res/raw`, CC0 from Freesound, listed in
`data/audio/freesound-sounds.json`), because not every iPhone Safari decodes Ogg Vorbis: effects
become MP3 in `public/sounds` (precached; the tap stays 16-bit WAV so it clicks without decoder delay,
and the level-up WAV is copied as is), and each music loop keeps its Ogg, which Chrome and Firefox
loop without a gap, plus an AAC `.m4a` that Safari picks, in `public/music` (not precached). It needs
ffmpeg, which the app does not depend on: on `PATH`, in `$FFMPEG`, or `npm i --no-save ffmpeg-static`.
Re-run it whenever an Android sound changes.
