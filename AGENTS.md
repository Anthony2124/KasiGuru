# Working in KasiGuru

The application repository is this directory, `KasiGuru-main`. Its parent
`C:\KasiGuru` also holds local backups, private credentials, signing keys,
archives and temporary work. Start code searches here.

## Find the code first

- Read [docs/CODE_MAP.md](docs/CODE_MAP.md) for the feature map, entry points,
  folder conventions and commands.
- Read [PRODUCT.md](PRODUCT.md) for product and language requirements.
- For visual changes, also read [DESIGN.md](DESIGN.md). The app and the two web
  surfaces have their design authority there.
- [HANDOFF.md](HANDOFF.md) records operational context and past design work.
  Check the current code before relying on historical file paths or status.

## Put changes in the right place

- Android screens and their ViewModels live together in `ui/screens/<feature>/`.
  Each game has its own directory under `ui/screens/games/`.
- Reusable UI belongs in `ui/components/`; game rules shared by game screens
  belong in `ui/screens/games/shared/`.
- Learning algorithms and game logic that do not render UI belong in `domain/`.
- Room entities, DAOs and migrations belong in `data/local/`; Firestore models
  and transport code in `data/remote/`; data orchestration in `data/repository/`.
- Match Kotlin package declarations to directories. Update explicit imports
  when moving declarations. Unit tests mirror the package of the code they test.
- Dictionary maintenance tools belong in `scripts/dictionary/`, shared local
  paths in `scripts/lib/project-paths.js`, authored dictionary data in
  `data/dictionary/`, and reference SQL in `data/sql/`.
- The current corpus workflow is described in `docs/DICTIONARY_MEANINGS.md`.
  Use the sourced wordlist importer instead of heuristic category rewrites.
- Design plans belong in `docs/design/`, prompts in `docs/prompts/`, and visual
  references in `design/`. Build resources remain in `app/src/main/res/`.
- Keep the admin portal in `admin-website/admin/` and the download site in
  `admin-website/download/`. They maintain separate stylesheets; run the shared
  brand token check after changing either stylesheet.
- Keep operational entry points in `functions/` and existing deployment and
  backup scripts at their documented paths. Local startup tasks and CI use them.

## Preserve the research and runtime contracts

- Do not invent Kasiguranin words, translations, stories or pronunciation.
  Preserve sourced vocabulary, authored definitions and field notes.
- Artwork is authored by the user. Consult the design documents for asset work.
- Preserve Android application ID, Room entity/schema identities, migration
  history, navigation route strings and Firestore collection/field names when
  organizing code.
- Do not edit generated `build/`, `.gradle/`, `.kotlin/` or `node_modules/`
  content. Keep credentials and signing material out of source.
- A code refactor does not authorize publishing, running data writes against
  Firebase, resetting databases, or deleting user files.

## Verify relevant changes

From the repository root:

```powershell
npm run check:structure
npm run check:web
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Use `./gradlew` on Unix. Run the existing Room instrumented migration suite
when changing the database or migration behavior. Dictionary commands have
preview options listed in `scripts/README.md`; inspect output before applying
corpus changes.
