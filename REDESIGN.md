# KasiGuru — UI/UX audit and replacement-world proposal

**Measured 2026-09-02** against 31 screens and 26 shared components (22,690 lines of Compose UI).
Every count below is reproducible from the repository; re-measure before trusting this page.

This document has two halves. The first is what the interface actually is today, measured rather than
remembered. The second is a proposal for replacing its visual world, which is what Adrian asked for.
The two halves disagree, and that disagreement is the most useful thing here, so it is stated in the
open rather than buried.

---

## 1. The audit

### 1.1 What is already right

Recorded first so nobody spends a week redoing it:

| Check | Result |
|---|---|
| Emoji used as interface | **0 files** — the purge held |
| Hardcoded colour literals in screens | **2**, both deliberate and documented — `GameRulesDialog.kt`'s `FixedViolet`, needed because a registry built at class-load time cannot reference theme-reactive tokens |
| Design-system components vs raw Material | `SoftCard` 57, `ClayButton` 35, `TagChip` 11 vs `Button` 8, `Card` 3, `FilterChip` 3 |
| Shell adoption | 2 Canopy (Learn, Onboarding — exactly what DESIGN.md prescribes), 27 Ground, 4 deliberately full-bleed |
| Dark mode | Fully built, measured, and toggleable by the user from Settings |
| Contrast | Measured with a real WCAG calculator, not estimated; figures published in DESIGN.md |
| Gradient text | **0 occurrences** |
| Glass over the Ground | **1 caller**, and it is a legal one — the game rules dialog's gradient band |

An interface with these properties is not a failing interface. That matters for the decision in §2.

### 1.2 Two dead vocabularies, still in live use

The largest single inconsistency. `ui/theme/Color.kt` carries an alias block left over from two
superseded systems — "Casiguran Coast" and an earlier "Play" palette. The aliases point at current
tokens, so nothing renders wrong. The cost is that **every colour has two names**, and new code can
reach for the old one without noticing.

**104 live references**, concentrated in shared components rather than screens:

| Alias | Uses | Alias | Uses |
|---|---|---|---|
| `CoastInk` | 25 | `XpGold` | 10 |
| `CoastMuted` | 19 | `XpGoldDark` | 9 |
| `Primary` | 16 | `HeroCardStart` | 4 |
| `PlayPurpleStart` | 16 | `PlayGoldStart` | 2 |

Worst files: `WordDetailBottomSheet.kt` (5), `StreakDialog.kt` (5), `LevelUpDialog.kt` (5),
`GameOverView.kt` (4), `CoastalComponents.kt` (4), `WordVerificationDialog.kt` (3),
`StreakCelebrationDialog.kt` (3).

**The dialogs are the residue, not the games.** That is worth saying plainly, because the intuition
is that mini-games are the unmigrated corner — and they are unmigrated, but only two game screens
carry legacy tokens against nine shared components that do.

A further **19 aliases are defined and referenced nowhere at all**: `TextWhite`, `TextDark`,
`StaticInk`, `StaticMuted`, `Secondary`, `SecondaryContainer`, `Accent`, `AccentContainer`, `SandBg`,
`CoastFaint`, `LightThemeBackground`, `LightSurfaceCard`, `LightSurfaceVariant`, `PlayPinkStart`,
`PlayPinkEnd`, `PlayNavDark`, `PlayChipTranslucent`, `PrimaryDark`, `PlayGoldEnd`. Pure dead weight.

### 1.3 Two primary buttons

`CoastPillButton` in `ui/components/CoastalComponents.kt` survives at five call sites —
`GameOverView.kt` (×2), `LevelUpDialog.kt`, `WordVerificationDialog.kt` (×2) — while DESIGN.md names
`ClayButton` the primary action.

This is a duplication defect, not a contrast defect: the component handles the gold variant correctly
(`labelColor = if (variant == PillVariant.Gold) RewardInk else Color.White`). But two components
answering "what does the main action look like" is how a system drifts, and a learner meets both
inside the same session — `ClayButton` on Learn, `CoastPillButton` when they level up.

### 1.4 The mini-games never migrated

`AspectBuilderGameScreen.kt` and `SentenceOrderGameScreen.kt` still use `HeroCardStart`/`HeroCardEnd`
and raw Material `FilterChip`s. Six game screens plus the hub sit outside the design system's
component vocabulary while using its colours through aliases.

### 1.5 Responsive behaviour reaches 2 of 31 screens

Only `LearnScreen` and `GameHubScreen` consult `rememberWidthClass`. Every other screen renders one
phone-width layout — including on a tablet, in landscape, which is a plausible way for a thesis panel
to see this app for the first time.

### 1.6 Accessibility needs a pass, not a rewrite

**103 `contentDescription = null`** against 44 real descriptions. Many nulls are correct: a decorative
icon sitting beside its own visible label should be hidden from the accessibility tree. The finding is
"these 103 need reading", not "these 103 are wrong". Set against that, the app already does the harder
parts well — measured contrast, state never carried by colour alone in the newer screens, and a
reduced-motion contract.

### 1.7 Six banned kicker labels survive, and DESIGN.md says they do not

DESIGN.md's refuse list opens with eyebrow/kicker labels above headings, names the exact offenders it
purged, and states "All of them go". Six are still rendering:

| Label | File |
|---|---|
| `"LEVEL UP"` | `components/LevelUpDialog.kt:71` |
| `"NEW UNLOCKS"` | `components/LevelUpDialog.kt:120` |
| `"VERIFICATION QUIZ"` | `components/WordVerificationDialog.kt:109` |
| `"TRANSLATION"` | `screens/flashcards/FlashcardDeckScreen.kt:280` |
| `"CLOSE SPELLINGS"` / `"ALSO CLOSE IN SPELLING"` | `screens/contribute/SubmitWordScreen.kt:532` |

All carry the pattern the rule describes: `labelSmall`, `ExtraBold`, letter-spaced, above the thing
they label. The `"TRANSLATION"` pill is also a translucent white chip sitting on a card, which the
same list forbids separately.

One further hit, `"DICTIONARY CORPUS"` in `GameHubScreen.kt:69`, is a false positive — it appears in a
doc comment describing the pattern that screen replaced.

**The finding underneath the finding: the design authority is out of date with the code.** DESIGN.md
asserts these are gone. Anyone reading it to learn the system would believe the rule is enforced. A
rule nobody checks decays into a preference, which is how §1.2's alias block survived too.

Note where these live — `LevelUpDialog`, `WordVerificationDialog`, `FlashcardDeckScreen`,
`SubmitWordScreen`. The same components carrying the dead vocabulary carry the banned patterns. **The
unmigrated layer of this app is its dialogs and its flashcards, not its games.**

---

## 2. Replace the world, or clean the one there?

Adrian has asked for a replacement. The audit does not support that as the cheapest route to a better
interface, and the honest thing is to say so before spending it.

**The case for cleanup.** The measured picture is a coherent world carrying legacy residue. 104 token
references, 19 dead aliases, one duplicate button, six unmigrated screens: that is perhaps a week, and
it produces most of the visible consistency a redesign would. The existing system's contrast figures
were measured against real WCAG failures, and its three colour rules each exist because something
failed a calculator. A replacement starts that work over.

**The case for replacement.** A thesis artifact gets one first impression, and the current world was
synthesised from five reference designs Adrian supplied — an assembly of app-design conventions rather
than anything drawn from Casiguran. "Violet Sheet" would be at home in any language app; nothing in it
says *this town*. For a project whose entire argument is that this language and place are worth
preserving, that is a real weakness, and it is not one cleanup can fix.

**Recommendation: do §5's cleanup regardless, and treat the replacement as a separate decision.** A
replacement inherits every problem in §1 if the residue is not cleared first — the new world would be
built by the same components that still reach for `CoastInk`.

The rest of this document assumes the replacement goes ahead.

---

## 3. Candidate worlds

Directions are derived from the audience's real cultural world — Casiguran, Aurora — rather than from
app-design fashion. That constraint is the point: a world drawn from the town can say something a
palette chosen from references cannot.

**Excluded as the category rut:** the Duolingo-alike (cartoon mascot, green streak furniture, rounded
gamified cards) and its predictable opposite (austere white academic minimalism). Both are what this
category always ships; neither is a candidate.

Ordered by resonance. Ordering is not a decision — see §3.1.

| # | World | Why it resonates | Honest risk |
|---|---|---|---|
| 1 | **Field notebook** — ruled elicitation pages, interlinear glossing, IPA, the linguist's own instrument | The app *is* a documentation project; this is the artifact the discipline and the panel read daily, and it makes the corpus the hero rather than the chrome | Austere; must not read as a spreadsheet to a fifteen-year-old |
| 2 | **Bangka livery** — painted outrigger hulls, hand-lettered boat names, saturated enamel against sea | Every family in Casiguran knows these by heart; a boat carries the journey metaphor the learning path already uses, without a mascot | Nautical decoration is an easy cliché; needs the real lettering, not a beach theme |
| 3 | **Tide table and fishing almanac** — printed tide columns, moon phases, dense numerals | A spaced-repetition schedule *is* a tide table; the rhythm of a fishing town's day is already the app's rhythm | Numeric density fights a beginner audience |
| 4 | **Provincial signage** — jeepney and tricycle lettering, route boards, hand-painted enamel and chrome | The most-read public typography in the province, and unmistakably Philippine rather than generically tropical | Loud; tips into kitsch without discipline |
| 5 | **Handloom banding** — warp and weft structure, geometric registers, woven progress rows | Pattern as structure rather than ornament; a completed row is a natural progress metaphor | Indigenous motifs demand consultation, not appropriation — needs community input before it ships |
| 6 | **Storm coast** — PAGASA signal charts, barometric maps, the Pacific-facing weather everyone tracks | Weather is the shared daily language of this coast; a warning-chart system is a real, rigorous visual grammar | Alarm palettes read as error states in a learning app |
| 7 | **School exercise book** — pad paper, mimeograph, red correction ink | The object the audience literally holds each week | Nostalgia-bait, and closest to the childishness PRODUCT.md explicitly refuses |

Material families represented: paper and print (1, 3, 7), painted wood and enamel (2, 4), textile (5),
meteorological data graphics (6). The list deliberately spans four, so it is not seven renditions of
one idea.

### 3.1 This document proposes; it does not choose

No direction is locked here, and that is deliberate. The `impeccable` skill's replacement-world
process runs a seeded direction round — candidates dealt against catalog challengers, weighed on
audience identification and product clarity, and presented for a real choice. That round is
specifically what stops a redesign converging on the look every model ships by default.

Naming a winner in a file would pre-empt a decision that is Adrian's and skip the step that protects
the outcome. The round runs when the build starts.

---

## 4. What a replacement costs

| Scope | Size |
|---|---|
| Screens | 31 |
| Compose UI | 22,690 lines |
| Shared components | 26 files, 5,214 lines |
| Token layer | `ui/theme/` — colour, type, spacing, motion |
| Documentation | `DESIGN.md` rewritten at the end, from the built result rather than ahead of it |

**What must survive untouched**, because it is product truth rather than styling: the SM-2 learning
model, the learning tree and lesson logic, the corpus and everything in `PRODUCT.md`, the measured
contrast discipline (the numbers change, the rigour does not), Adrian's ownership of all illustration,
and the rule that no Kasiguranin is ever invented.

Sequencing that keeps the app shippable throughout: token layer first, then the shared components
(which is where §1.2's residue lives), then screens in order of traffic — Learn, lesson player,
dictionary, games, the long tail.

---

## 5. The cleanup, sequenced

This list stands whichever path is chosen, and should come first either way.

**Done (2026-09-02, commit `605a45f`):**

1. ~~Migrate the legacy token references.~~ **104 to 0.** Every alias that merely renamed a current
   token was substituted with that token, which is provably identical since each was defined as
   `= <token>`. Verified on the emulator: the answer feedback panel is pixel-identical.
2. ~~Delete the dead aliases and the alias block.~~ **63 aliases to 3.** `Color.kt` drops from 275
   lines to 226. The three survivors — `Warning`, `SkyReview`, `VocabSea` — had no canonical
   equivalent, so they are documented as off-palette colours needing a design decision rather than
   silently deleted or silently kept.
3. ~~Retire `CoastPillButton`.~~ **5 call sites to 0**, component deleted.
4. ~~Delete the kicker labels.~~ **5 to 0.** Four removed; `SubmitWordScreen`'s was kept as
   information and set in sentence case, because with no heading above it that line is the only
   thing distinguishing two lists of matches. DESIGN.md still needs correcting: it claims these were
   already gone.

**Still open:**

5. **Two raw `FilterChip`s** in `SentenceOrderGameScreen`, the last raw Material components in the
   games. Small.
6. **Responsive coverage: 2 of 31 screens.** Unchanged. The largest remaining item, and the one a
   thesis panel on a tablet would notice first.
7. **102 null content descriptions** to read. Many are legitimately decorative; the work is the
   reading, not the typing.

## Provenance

Counts produced by direct search over `app/src/main/java/com/kasiguru/ui` on 2026-09-02. The audit
method and this document's structure follow `DESIGN.md` as the design authority and `PRODUCT.md` as
product truth; where this document and DESIGN.md disagree about what the app should look like,
DESIGN.md wins until a replacement world is built and DESIGN.md is rewritten from it.
