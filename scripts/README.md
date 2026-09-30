# Local tools

Run these commands from the repository root. Shared project paths are resolved
by `lib/project-paths.js`, so dictionary tools also work when invoked by an
absolute path from a different working directory. Node dependencies are in
the root `package.json`; Firebase admin tools have their own `functions/package.json`.

## Read-only checks

```powershell
npm run check:structure
npm run check:web
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

## Dictionary workflow

| Tool under `dictionary/` | Purpose and invocation |
|---|---|
| `audit_dictionary.js` | Read-only audit; accepts source workbook and wordlist paths |
| `repair_dictionary.js` | Idempotent, sourced corrections; inspect with `--dry-run` |
| `import_wordlist.js` | Merge elicitation wordlist; inspect with `--dry-run <workbook.xlsx>` |
| `apply_meanings.js` | Apply `data/dictionary/meanings.json` to the Android seeder; inspect with `--dry-run` |
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
- `../functions/`: administration, backup/restore, release and Firestore maintenance
  scripts. Their existing paths remain the operational entry points.

Deployment, Firebase writes, restore/reset and release commands affect external
state. Folder restructuring does not run those commands.
