# The dictionary corpus and its definitions

This note records where KasiGuru's vocabulary comes from and, in particular, which parts of it are
primary research data and which are editorial writing added for the app. A thesis panel should be
able to see that line without having to guess at it.

## Where the corpus comes from

The shipping corpus in `app/src/main/java/com/kasiguru/data/local/DatabaseSeeder.kt` is drawn from
two documented sources:

| Source | What it is | Contribution |
|---|---|---|
| `Extracted_Vocabulary_Dictionary.xlsx` | A 373-row extract of Kasiguranin / Tagalog / English / Category, taken from Supnet (2016), *A Grammatical Sketch of Kasiguranin* | The original 394 entries |
| `WORDLIST-1228.xlsx` | A SIL-style field elicitation wordlist: `NO.`, `ENGLISH`, `ENGLISH ELICITATION NOTES`, `TAGALOG`, `CEBUANO`, `LOCAL LANGUAGE`, where `LOCAL LANGUAGE` holds the Kasiguranin form. 1,228 elicitation slots, roughly 950 with a form recorded | 808 further senses |

The two sources are largely disjoint — only about 200 headwords appear in both — which is why the
app carried a third of the documented vocabulary for as long as it did. The corpus now holds
**1,202 senses**.

Both source files live outside the repository, in the maintainer's `Downloads` folder. The scripts
that read them take the path as an argument so they can be pointed elsewhere.

## What is primary data and what is not

**Primary research data, reproduced as recorded:**

- Every Kasiguranin headword, and every Tagalog and English gloss.
- Categories for the original 394 entries.
- IPA notation, verb aspect forms and phonetic flags, where present.

**Derived, but mechanically, from the sources:**

- Categories for the imported 808 senses. The elicitation wordlist has no category column, so
  categories were assigned from the wordlist's own semantic ordering — it is a standard instrument
  that runs geography, then kinship, then animals, then the body, and so on — with a small keyword
  override for unambiguous glosses. The bands are written out in `scripts/import_wordlist.js`.
  These are an editorial convenience for navigation, not a claim about Kasiguranin semantics.

**Editorial writing, not research data:**

- **`meaningEnglish` and `meaningTagalog`**, the one-sentence definitions on every entry. These were
  written for this app to explain the sense that the existing English gloss names. They define the
  *gloss*, not the Kasiguranin word: nothing in them asserts anything about Kasiguranin usage,
  register, etymology or connotation that the corpus does not already record. `apak` is glossed
  "adze" in the source; the definition says what an adze is.

  They are what the dictionary detail screen shows under "Meaning" and what the games offer as a
  hint. They should be read by a fluent speaker before publication, and the Tagalog side especially:
  it is Filipino prose written to match the English, not text collected in the field.

## Things deliberately left alone

Three entries carry a headword whose correct reading could not be established, and were not guessed
at. They are listed under `NEEDS_A_HUMAN` in `scripts/repair_dictionary.js` and reported every time
that script runs:

- `kaliwa kariwe` / *left* and `kasinungalingan kabulean` / *lie* — both tokens are real words, and
  deciding which is the Kasiguranin form rather than an appended Tagalog gloss would be inventing
  primary data.
- `hand` / *press* — an English word standing in the Kasiguranin field, with `with` in the Tagalog
  field. No counterpart in either source.

A further 812 entries have no IPA notation, and no entry has an audio recording. Both are corpus
work, not code work.

## The scripts

| Script | What it does |
|---|---|
| `scripts/audit_dictionary.js` | Read-only. Compares the corpus against both sources and reports scrambled rows, English text in the Tagalog field, trailing-comma glosses, missing IPA, missing or over-long definitions. Exits non-zero on a blocking defect, so it can gate a release. |
| `scripts/repair_dictionary.js` | The record of the 40 one-time corrections applied to the corpus, each matched against its exact prior value so the script is idempotent. |
| `scripts/import_wordlist.js` | Merges the elicitation wordlist into the corpus. Keeps every existing entry as it stands and appends only senses the corpus lacks. Also splits the list across nine private methods — a single Kotlin method is capped at 64 KB of bytecode and a corpus this size does not compile in one. |
| `scripts/meanings.json` | The definitions, keyed by `kasiguranin|english`, so they can be reviewed as one document and re-applied after any regeneration of the corpus. |
| `scripts/apply_meanings.js` | Writes `meanings.json` into `DatabaseSeeder.kt`. Reports keys that match no entry rather than ignoring them. |
| `scripts/merge_meanings.js` | Merges an authored batch into `meanings.json`, reporting collisions. |
| `scripts/wordlist_notes.json` | The field linguist's elicitation notes and the alternate forms a single-headword corpus cannot hold. Reference material; not shipped in the app. |
| `functions/backfill_meanings.js` | Pushes definitions and parts of speech into Firestore. Local Node script with a service-account key — this project stays on the Spark plan. |
| `functions/audit_firestore_corpus.js` | Read-only. Sorts every Firestore document the corpus does not account for into corrupted twins, multi-form cells, and genuine additions. |

## The state of Firestore

The corpus ships in the APK; Firestore is what the admin portal edits and what reaches devices
between releases. After the August 2026 reconciliation the two agree.

Firestore holds **1,247 vocabulary documents**. All **1,202 corpus senses** are present, each with
an English and a Tagalog definition, and there are **no duplicate sense keys** — all four senses of
`baga` exist as four documents with four distinct definitions.

Getting there took three passes, all recoverable from the backup taken first
(`KasiGuruBackups/2026-08-25T17-02-34-277Z`, restorable with `restore_firestore.js`):

1. `backfill_meanings.js` wrote definitions and parts of speech onto the 1,015 documents that
   already matched a corpus sense.
2. `fix_firestore_twins.js` corrected 52 documents **in place**. Firestore had received the same bad
   import the corpus did, so it still carried `kulot kulot / curly`, `coal baga baga / ember,` and
   `ungot / torch,`, plus documents whose headword packed several forms (`bobo/ mangmang/ tanga`).
   Nothing was deleted; the documents were rewritten to the sense they were always meant to hold.
3. `backfill_meanings.js --create-missing` then wrote the remaining 52 and created the 135 corpus
   senses that had no document at all.

### The 45 documents that are still not corpus senses

**31 are genuine** — community submissions and admin additions the corpus does not have
(`Oltaw / Appear`, `Maurikas / Mischievous`, `Kabinga / Spouse`, `Ngetnget / yellow teeth`). These
are real content. They need definitions written for them in the portal, which is the one place a
definition can now be authored without a code change.

**14 are redundant or broken, and were deliberately left for a person to judge:**

- `diget,sabeng / sea`, `udeng/padyaw / shrimp`, `singët/ëggëm / ant`, `bugtong/namugtong / buy`,
  `sanget/ngorngor / cry`, `betbet/betbetën / carry`, `or 24 hrs) araw (also, sun) aldew / day`,
  `inom inom / to` — the corrected sense now exists as its own document, so each of these is a
  second copy of a word a learner already has. Safe to delete once confirmed.
- `tama tama`, `kanan kanan`, `kaliwa kariwe` — empty shells with no Tagalog or English gloss at
  all. They render as blank dictionary entries. `kaliwa kariwe` is also one of the three
  `NEEDS_A_HUMAN` rows.
- `kapatyakang lalëkke / brother` and `kapatyakang bëbbi / sister` — **not** simple duplicates. The
  corpus spells the stem `kapatkaka`; these spell it `kapatyaka`. Which is correct is a question
  about the language, not about the data, so both spellings were left standing rather than one
  being silently chosen.

## Alternate forms

Thirty-seven wordlist cells record more than one Kasiguranin form for a single sense
(`diget,sabeng`; `balbal/ yabat/ hablug`). The corpus stores one headword per entry, so the first
form was taken and the rest preserved in `scripts/wordlist_notes.json`. Nothing was discarded, but
the app does not yet display alternates — that would need a field on `VocabularyEntity` and a place
to show it.
