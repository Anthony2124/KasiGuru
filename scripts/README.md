# Local tools

Run these commands from the repository root. Shared project paths are resolved
by `lib/project-paths.js`, so dictionary tools also work when invoked by an
absolute path from a different working directory. Node dependencies are in
the root `package.json`; Firebase admin tools have their own `functions/package.json`.

## Read-only checks

```powershell
npm run check:structure
npm run check:web
npm run test:launch
npm run firebase:usage
npm run firebase:usage -- --hours
npm run dictionary:repair:preview
npm run dictionary:meanings:preview

# Audit against supplied research workbooks
npm run dictionary:audit -- "C:\path\Extracted_Vocabulary_Dictionary.xlsx" "C:\path\WORDLIST-1228.xlsx"
```

The structure check validates Kotlin packages, feature folders, required data
paths and local JavaScript imports without running maintenance scripts.
`dictionary:audit` compares the current corpus with the research sources and
may report existing corpus defects. It defaults to the corresponding workbook
names in the current user's Downloads directory.

## Badge artwork

The user-authored six-tier boards live in `design/assets/badges/source/`, one
per badge family. To cut them into the 60 transparent tier images in
`app/src/main/res/drawable-nodpi/badge_<family>_<tier>.webp`, run:

```powershell
python scripts/generate-badge-art.py --preview
```

This requires numpy, scipy and Pillow. `--preview` also writes a dark and light
contact sheet to `build/badge-preview.png`; check it before committing. The
board-to-family mapping is at the top of the script, and
`ui/components/BadgeArt.kt` names each drawable so release shrinking keeps it.

## Dictionary category icons

The user-authored category icons live in `design/assets/categories/source/`.
To cut them into `app/src/main/res/drawable-nodpi/category_<slug>.webp`, run:

```powershell
python scripts/generate-category-icons.py --preview
```

This requires numpy, scipy and Pillow. The slug-to-image mapping is at the top
of the script; `ui/theme/CategoryMetaData.kt` names each drawable.
`category_general` is the fallback for any category the registry does not know.

## Launcher artwork

The user-authored icon source is `design/assets/app-icon.png`. To export all
Android launcher densities after replacing that square image, run:

```powershell
python scripts/generate-launcher-icons.py
```

This requires Pillow. It exports opaque legacy icons and adaptive foreground
layers with the artwork centered in the 72 dp visible area, plus an 18 dp edge
extension on each side for launcher motion. Android applies the icon shape.

## Dictionary workflow

| Tool under `dictionary/` | Purpose and invocation |
|---|---|
| `audit_dictionary.js` | Read-only audit; accepts source workbook and wordlist paths |
| `repair_dictionary.js` | Idempotent, sourced corrections; inspect with `--dry-run` |
| `import_wordlist.js` | Merge elicitation wordlist; inspect with `--dry-run <workbook.xlsx>` |
| `apply_meanings.js` | Apply `data/dictionary/meanings.json` to the Android seeder; inspect with `--dry-run` |
| `withdraw_senses.js` | Record senses a backup no longer has with `--from-backup <folder>`, then remove them from the seeder; inspect with `--dry-run` |
| `merge_meanings.js` | Merge a supplied authored batch into the definitions JSON; reports collisions and writes the file |
| `parse_excel_database.js` | Inspect workbook columns and categories; optional workbook path |
| `parse_sql.js` | Parse a supplied SQL migration into `data/dictionary/kasiguranin_vocabulary_seed.json`; optional SQL path |

The old heuristic category updaters and Excel seeder importer were removed.
The sourced wordlist importer is the maintained corpus workflow described in
[../docs/DICTIONARY_MEANINGS.md](../docs/DICTIONARY_MEANINGS.md).
Research source workbooks are supplied separately;
SQL references in `data/sql/` are historical inputs, not the active Room database.

## Established operational paths

- `deploy_web.ps1`: deploy the three web projects using the configured Vercel account.
- `mirror_backup.cmd`: existing local startup backup mirror entry point.
- `archive-apks-to-releases.sh`: archive surviving APKs to GitHub Release assets.
- `check-web-tokens.js`: verify shared brand tokens across the two web stylesheets.
- `diagnostics/check_pos.js`: read vocabulary part-of-speech coverage from Firebase.
- `diagnostics/firestore-usage.js`: reads Cloud Monitoring's daily/hourly Firestore operation totals;
  never reads documents. Run `npm run firebase:usage` (or add `-- --hours` / `-- --days 14`).
  Uses `gcloud auth login` or an existing `npx --yes firebase-tools login` session. Firebase CLI
  refreshes an expired login using project metadata. Quota days use Pacific time; 70% flags a warning.
- `../functions/`: administration, backup/restore, release and Firestore maintenance
  scripts. Their existing paths remain the operational entry points.

Deployment, Firebase writes, restore/reset and release commands affect external
state. Folder restructuring does not run those commands.

## App sounds and music

Sound effects and music in `app/src/main/res/raw/` come from Freesound, CC0
only. The search inputs per sound are in `data/audio/sound-slots.json`; the
chosen sounds and their provenance are locked in
`data/audio/freesound-sounds.json`. The API key is read from
`FREESOUND_API_KEY` or `..\private\freesound-api-key.txt` (create one at
https://freesound.org/apiv2/apply) and is never bundled in the app.

```powershell
node scripts/audio/freesound.js shortlist   # writes a page to listen to candidates and copy a selection
node scripts/audio/freesound.js lock selection.json   # records the picks, rejecting anything not CC0
node scripts/audio/freesound.js fetch       # downloads the locked previews into res/raw as <slot>.ogg
```

`fetch` re-checks each license before downloading and replaces any other
format with the same resource name. If a music loop is over about 2.5 MB, set
its `"quality"` to `"lq"` in the manifest and fetch again.

## Word recordings

Pronunciation clips are uploaded in the admin portal to Firestore `word_audio`.
The APK and the web app ship copies so recordings play offline and do not stop
when the Spark plan's daily reads run out. After a recording session:

```powershell
npm run audio:export -- --dry-run   # how many clips are new or changed (reads only)
npm run audio:export                # writes app/src/main/assets/word_audio and its manifest.json
npm run sync:web                    # copies them to admin-website/webapp/public/audio
```

The export reads Firestore only (one read per vocabulary document plus one per
changed clip) and never writes to it. A word re-recorded after a release still
plays: both apps download the newer clip and fall back to the bundled take.
