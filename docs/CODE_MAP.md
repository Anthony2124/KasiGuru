# KasiGuru code map

Paths in this guide are relative to the `KasiGuru-main` repository. Start here
when deciding where to investigate or place a change.

## Repository areas

| Path | Responsibility |
|---|---|
| `app/` | Android app, resources, unit tests, instrumented tests and exported Room schemas |
| `admin-website/admin/` | Firebase authenticated admin dashboard |
| `admin-website/download/` | Public APK download site |
| `functions/` | Firebase functions entry point and local administration, backup, release and repair tools |
| `scripts/dictionary/` | Vocabulary import, audit, definitions and repair tools |
| `scripts/diagnostics/` | Read-only investigation tools |
| `scripts/lib/` | Shared paths for local tools |
| `scripts/` | Established deployment, backup mirror, release archival and project checks |
| `data/dictionary/` | Authored definitions and elicitation notes |
| `data/sql/` | Reference schemas and historical import SQL; runtime Room schema exports are in `app/schemas/` |
| `docs/` | Code map, runbooks, plans and research chapters |
| `docs/design/` | Design plans, audits and historical redesign notes |
| `docs/prompts/` | Reusable project prompt documents |
| `design/` | HTML mockups, screenshots and reference assets in `design/assets/` |
| `Voice/` | Source pronunciation recordings organized by category |

`README.md` is the entry point, `PRODUCT.md` defines product truth, `DESIGN.md`
defines visual rules, and `AGENTS.md` describes how to work in this repository.
These stay at the root for easy discovery. Android and Firebase build/deployment
configuration also stays at the root.

The parent `C:\KasiGuru` contains local infrastructure and personal artifacts.
Its `private/`, `keystore/`, `KasiGuruBackups/`, ZIP archives and temporary
directories are outside the application source tree.

## Android: where a change starts

The Kotlin source root is `app/src/main/java/com/kasiguru/`. The paths below
are relative to that directory.

| Task | Start here | Follow through to |
|---|---|---|
| App startup and activity | `KasiGuruApp.kt`, `MainActivity.kt` | `di/`, `ui/navigation/` |
| Add a screen or inspect routes | `ui/navigation/NavGraph.kt`, `ui/navigation/Screen.kt` | `ui/screens/<feature>/` |
| Home, learning path and lessons | `ui/screens/home/`, `learn/`, `lesson/` | `domain/lesson/`, `data/repository/LessonRepository.kt` |
| Vocabulary and dictionary | `ui/screens/vocabulary/` | `VocabularyRepository.kt`, `data/local/dao/VocabularyDao.kt` |
| Flashcards and review | `ui/screens/flashcards/` | `util/srs/`, `util/Recall*` |
| Stories and cultural material | `ui/screens/stories/`, `ui/screens/cultural/` | `StoryRepository.kt`, `data/remote/StoryImageRepository.kt` |
| Sign-in and profile | `ui/screens/auth/`, `ui/screens/profile/` | `AuthRepository.kt`, `ProfileRepository.kt` |
| Progress, leaderboard and achievements | Corresponding `ui/screens/` feature | `UserProgressRepository.kt`, `LeaderboardRepository.kt`, `util/gamification/` |
| Offline data or database upgrade | `data/local/KasiGuruDatabase.kt`, `KasiGuruMigrations.kt` | `dao/`, `entity/`, `ContentTopUp.kt`, `DatabaseSeeder.kt` |
| Firebase content sync | `data/remote/FirestoreSyncManager.kt` | `data/repository/FirestoreSyncRepository.kt` |
| Cross-device progress | `data/repository/ProgressSyncManager.kt` | `LearningStateMerge.kt`, `UserProgressRepository.kt` |
| Push notifications and reminders | `util/notification/`, `util/worker/` | `ui/screens/notifications/`, `NotificationRepository.kt` |
| Shared appearance | `ui/theme/`, `ui/components/` | `ui/components/brand/`, `clay/`, `states/` |
| Guided onboarding tour | `ui/tour/`, `ui/screens/onboarding/` | Tour tests in `app/src/test/` |

Repository filenames in the table without a full directory are in
`data/repository/`. UI state stays beside its ViewModel unless several features
need the same model.

## Games: screen and ViewModel together

All game UI is under `ui/screens/games/`:

| Folder | Contents |
|---|---|
| `hub/` | `GameHubScreen.kt`, `GamesViewModel.kt` |
| `levels/` | `LevelSelectionScreen.kt`, `LevelSelectionViewModel.kt` |
| `shared/` | `GameRulesDialog.kt`, its rule information and registry |
| `aspectbuilder/` | Aspect Builder screen, ViewModel and state |
| `fillblank/` | Fill in the Blank screen, ViewModel and state |
| `recall/` | Recall screen, ViewModel and state |
| `reversematch/` | Reverse Match screen, ViewModel and state |
| `sentenceorder/` | Sentence Order screen, ViewModel and state |
| `wordmatch/` | Word Match screen, ViewModel and state |
| `wordsearch/` | Category screen + its ViewModel, gameplay screen + its ViewModel |
| `wordwheel/` | Word Wheel screen, ViewModel and state |

Grid and wheel generation remain in `domain/wordsearch/` and
`domain/wordwheel/`. Shared translation display logic is in
`domain/games/GameGloss.kt`, tested by `domain/games/GameGlossTest.kt` under the
unit-test source root. Game completion and level persistence use
`data/repository/GameRepository.kt` and `GameLevelRepository.kt`.

## Web and backend entry points

- Admin: `admin-website/admin/index.html` (login), `dashboard.html`,
  `js/auth.js`, `js/app.js`, `js/word-normalize.js`, and `css/styles.css`.
- Download: `admin-website/download/index.html`, `js/` and `css/styles.css`.
- Rules: `firestore.rules` and `storage.rules`; deployment configuration is in
  `firebase.json`.
- Cloud Functions: `functions/index.js`. Local scripts in the same folder
  cover admin claims, backups, restoration, releases and other operations.
- CI: `.github/workflows/ci.yml`; release distribution:
  `.github/workflows/release.yml` and `functions/publish_release.js`.

The root `admin-website/index.html` is a placeholder deployment. The admin
portal has a single source in `admin-website/admin/`. The two portals have
separate stylesheets with a shared brand token check.

## Search and verification

```powershell
# Find a feature's files
rg --files app/src/main/java/com/kasiguru/ui/screens/games/wordsearch

# Find callers, state and repository use
rg -n 'WordSearchViewModel|WordSearchCategoryViewModel' app/src
rg -n 'getVocabularyByCategory' app/src

# Check folder/package consistency, local script imports and web brand tokens
npm run check:structure
npm run check:web

# Android verification
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Unit tests live in `app/src/test/java/com/kasiguru/` and mirror source packages.
Room migration tests live in `app/src/androidTest/`; run them on an emulator
with `connectedDebugAndroidTest` when database behavior changes.

See [../scripts/README.md](../scripts/README.md) for dictionary commands,
[DICTIONARY_MEANINGS.md](DICTIONARY_MEANINGS.md) for corpus rules, and the
existing deployment and monitoring runbooks for operational changes.

## Relocated paths

| Previous location | Current location |
|---|---|
| Flat `ui/screens/games/*.kt` | Per-game, `hub/`, `levels/` and `shared/` folders listed above |
| `ui/screens/games/GameGloss.kt` | `domain/games/GameGloss.kt` |
| `scripts/<dictionary-tool>.js` | `scripts/dictionary/<dictionary-tool>.js` |
| `scripts/meanings.json`, `scripts/wordlist_notes.json` | `data/dictionary/` |
| Root `check_pos.js` | `scripts/diagnostics/check_pos.js` |
| Root `update_seeder*` and old Excel seeder importer | Removed; current workflow uses `scripts/dictionary/import_wordlist.js` |
| Root `*.sql` | `data/sql/` |
| `aasets/` | `design/assets/` |
| Root `DESIGN-PLAN.md`, `REDESIGN.md` and visual audit/catalog | `docs/design/` |
| Root `kasiguru_asset_generation_prompt.md` | `docs/prompts/` |
