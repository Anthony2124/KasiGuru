# UI Localization Plan

How KasiGuru's interface chrome gets a Filipino (`fil`) translation, why it has to be
a PR series rather than one change, and where the line sits between UI text and the
language content this project exists to preserve.

---

## 1. Why this matters

KasiGuru is the software artifact of a thesis on preserving **Kasiguranin**, for an
audience in Casiguran, Aurora. The learning content is already trilingual
(Kasiguranin / Tagalog / English), but every button, label, screen title, dialog and
error message is **English only**. A thesis panel reading the app beside its own
stated purpose will notice that the interface a Casiguran learner navigates is not in
a language of Casiguran.

This is a real gap, not a cosmetic one. It is also genuinely deferred work — not
something to half-ship — for the reasons in section 3.

## 2. Current state (measured 2026-09-09)

| Fact | Value |
|---|---|
| `app/src/main/res/values/strings.xml` | **1 entry** (`app_name`) |
| `values-*/` locale directories | none |
| `res/xml/locales_config.xml` / `android:localeConfig` | not present |
| `resourceConfigurations` in `app/build.gradle.kts` | not set |
| Per-app language API (`AppCompatDelegate.setApplicationLocales`) | not used |
| Distinct hardcoded UI literals | **~515** (≈439 plain, ≈76 carrying format arguments), across ~610 call sites in ~50 files — measurement from PR #7's audit |

Nothing about localization is wired. This is a from-zero effort.

## 3. Why it cannot be one PR

- **Partial extraction is worse than none.** Moving half the strings to resources and
  translating those leaves a UI that is half Filipino and half English on the same
  screen. Either endpoint (all-English, all-translated) is better than the middle.
- **The Filipino copy needs a native speaker.** An English-author's guess at
  Filipino UI copy, shipped in a language-preservation thesis, undercuts the point.
  The translation step is owned by a speaker — ideally one from the Casiguran
  participant pool the thesis already draws on — not by whoever does the extraction.
- **`main` is shared.** Another contributor pushes regularly. A single 50-file,
  500-change PR would block or conflict with everything else in flight for as long as
  it is open. Small, screen-scoped PRs land in a day each and rebase cleanly.

## 4. Scope boundary — read this before touching anything

**In scope:** interface *chrome* — button captions, screen titles, section headings,
form labels, placeholder text, empty-state copy, confirmation dialogs, snackbars,
validation and error messages, notification text, content descriptions for
accessibility.

**Out of scope, and must stay out:**

- **Kasiguranin, Tagalog and English learning content.** Vocabulary entries, example
  sentences, story pages, lesson prompts. This lives in Room and Firestore, not in
  resources, and is authored / synced, never translated in a `values-*` file.
- **Kasiguranin as a UI locale.** Kasiguranin is the endangered language under study.
  A `values-<kasiguranin>/strings.xml` would require *inventing* Kasiguranin UI copy,
  which fabricates primary research data. If a Kasiguranin interface is ever wanted it
  is a fieldwork deliverable with a speaker, not an extraction task.
- **RTL layout, locale-aware number/date formatting.** `fil` and `en` are both LTR;
  the app shows almost no formatted dates or decimals. Skip unless a concrete need
  appears.

## 5. The PR series

### PR 1 — infrastructure only, zero visible change

- `res/xml/locales_config.xml` listing `en` and `fil`; reference it from
  `AndroidManifest.xml` via `android:localeConfig`.
- `resourceConfigurations += ["en", "fil"]` in `app/build.gradle.kts` so unused
  locale resources from libraries are stripped.
- AndroidX per-app language: `androidx.appcompat` per-app locales (the
  `AppLocalesMetadataHolderService` manifest entry) or `core:core-ktx`
  `AppCompatDelegate.setApplicationLocales(...)`, persisted so the choice survives
  restart.
- A **language setting** in `SettingsScreen`: *System default* / *English* /
  *Filipino*. Wired now, with only `en` resources present, so choosing Filipino is a
  no-op until PR 5 lands. This keeps the UI-facing change in its own small review.

Ships with `strings.xml` still one entry. Nothing on screen changes.

### PRs 2..n — extraction, one screen cluster per PR

Suggested clusters, each its own PR:

1. Onboarding wizard
2. Learn + Lesson Player
3. The six mini-games + shared game shell
4. Dictionary (vocabulary list, category, detail) + Stories (list, reader)
5. Profile, Edit Profile, Settings, Account, About, Cultural, Help
6. Notifications, Submit Word, Submit Literature, Report Issue
7. Shared dialogs, snackbars, and every remaining validation / error string

Per PR: move that cluster's literals into `values/strings.xml`, replace with
`stringResource(id)` (Compose) or `context.getString(id)` (elsewhere), **no
`values-fil/` yet**. Each PR leaves the app 100% English but fully resource-backed
for that cluster, and is independently reviewable.

### PR n+1 — non-Compose strings

ViewModels and workers that build user-facing text (analytics-adjacent status text,
`LearningAnalytics` is fine as it carries no copy, but error/result messages are not).
Pass string resource ids outward, or inject a small `StringProvider`, rather than
resolving `Context` inside a ViewModel.

### PR n+2 — lint gate

Once extraction is complete, promote `HardcodedText` to an error in `lintDebug` (CI
already runs `./gradlew lintDebug` in the `Android lint + unit tests + build` job), so
a new hardcoded literal fails the build instead of silently regressing coverage.

### PR n+3 — the translation

Hand `values/strings.xml` to a Filipino/Kasiguranin speaker. Land
`values-fil/strings.xml`. Verify on-device at real phone width — Filipino strings run
noticeably longer than English and will surface truncation and wrapping that looked
fine in English. Turn the Settings language picker's Filipino option live.

## 6. Conventions for the extracted resources

- **Key names:** `screen_element_meaning` — `onboarding_cta_start`,
  `lesson_feedback_correct`, `settings_language_title`, `error_network_generic`.
  Group by screen prefix so a translator can work a screen at a time.
- **Format arguments:** numbered, never bare — `"%1$s of %2$d"`, so a translator can
  reorder them. `xliff:g` tags around placeholders that must not be translated.
- **Plurals:** `<plurals>` with `one` / `other` (Filipino, like English, needs both),
  not string concatenation.
- **Content descriptions:** extracted too — accessibility copy is UI copy.

## 7. What to hand the translator

Just `app/src/main/res/values/strings.xml` after extraction, plus a short glossary of
product terms that should stay fixed (XP, streak, and any Kasiguranin term used as a
label). The translator never sees Kotlin.

## 8. Effort and sequencing

Extraction is mechanical but high-volume — budget roughly one focused pass per cluster
PR. The translation itself is external and gated on speaker availability; start that
conversation early so it is not the long pole. Do **not** open more than one
extraction PR at a time against `main`.
