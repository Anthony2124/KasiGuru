# KasiGuru UI/UX Makeover Plan

Applied through two design frameworks:
- **impeccable** — mode framework (App = *Operate*, Website = *Persuade*) + craft floor.
- **taste-skill** — anti-slop "Design Read", the three dials, and hard rules on typography / icons / emoji / anti-defaults.

---

## Design Read

- **App** — An Operate-mode gamified learning app for Filipino youth & heritage learners; playful/warm language; leaning toward a culturally-rooted illustrated world (coastal Casiguran + local wildlife), NOT generic edtech.
- **Website** — A Persuade-mode download/landing page for the same audience + academic stakeholders; same illustrated world in a light, confident marketing layout.

---

## The anti-slop finding

Current design flags on the taste-skill Anti-Default checklist:

| Slop signal | Where it is now |
|---|---|
| Default AI-purple gradient everywhere | "Play Purple" `#7B6EF6→#A78BFA` carries the whole brand |
| Centered hero over dark mesh | Website hero = dark scaffold + 3 radial mesh glows |
| Three equal feature cards | Website "Ecosystem Features" = 3 identical cards |
| Inter as workhorse font | Website body is Inter |
| Emoji as UI | App uses 👑🥈🥉👤🔥 as real interface elements |

The six inspiration images win on **custom illustration + character**, not gradients. So the makeover replaces a generic edtech skin with an **ownable visual world tied to Kasiguranin culture** — which also serves the app's language-preservation mission.

---

## POV: "Casiguran Coast" — a visual world, not a palette

A warm, illustrated world from the real place the language comes from: the Aurora coastline, Sierra Madre foothills, and local wildlife (Philippine eagle, tarsier, carabao, sea turtle). Delivers:
- A **mascot** (a Kasiguranin guide) for onboarding, empty states, level-ups, streak nudges.
- **Section identity through illustration + a widened palette** so the brand stops resting on one purple gradient.
- A narrative that ties every screen back to *why the app exists*.

---

## Foundations

**Color** — Delete legacy teal (`Color.kt`/`Theme.kt` Material `primary` is still `#0D7377`; website `--grad-teal`). Keep purple as ONE identity color; promote Gold (warmth/XP) and add coastal accents (sea-teal, sand, sky). Semantic tokens: `xp/gold`, `streak/ember`, `games/coral`, `vocab/sea`, `stories/dusk`.

**Typography** — One family across app + web. Retire Inter. Recommendation: **Outfit** display + clean humanist body; bigger, tighter display headings.

**Icons & emoji** — Replace emoji-as-UI with a real icon set (standardize on Iconsax, one family, consistent stroke) + crafted 3D badges for medals/crowns/streaks. Emoji only as sparing, intentional delight (mascot reactions), never as data.

**Illustration** — Biggest lever. Store as vector drawables so APK stays ~8 MB.

**Motion (dials)** — App `VARIANCE 6 / MOTION 5 / DENSITY 4` (Operate: calm, legible, delight in details). Website `VARIANCE 8 / MOTION 7 / DENSITY 3` (Persuade: bolder, scroll-driven).

---

## Signature component library
Pill button + arrow · progress-ring hero card · 2×2 stat tiles w/ 3D icons · puzzle-piece subject cards · restyled 3-D podium · dot/segmented steppers · floating bottom nav · timeline list · mascot slot.

---

## App screens (Operate — prioritized)
1. **Onboarding** — full-bleed Casiguran illustration per step + mascot; dot stepper; pill Next.
2. **Home** — progress-ring greeting card, stat tiles, section cards with illustration.
3. **Leaderboard** — 3-D podium badges (retire emoji medals).
4. **Games hub / Vocabulary / Flashcards** — glossy game cards, puzzle-piece category tiles, ring + timeline review.
5. Profile / Settings / Account / Stories — tokens + components, lighter touch.

## Website (Persuade)
Reskin dark mesh hero → light, illustrated, characterful; replace 3-equal-cards with asymmetric feature layout; retire Inter/teal; show the NEW app UI in phone mockups; keep live `app_releases`/version wiring intact.

---

## Phased rollout
| Phase | Scope | Effort |
|---|---|---|
| 0 | Tokens: kill teal, unify font, semantic color + radius/spacing (app+web) | 1–2 d |
| 1 | Signature components + mascot slot | 3–4 d |
| 2 | Onboarding, Home, Leaderboard | 3–5 d |
| 3 | Games, Vocabulary, Flashcards | 3–4 d |
| 4 | Website reskin + new mockups | 2 d |
| 5 | Motion, empty states, dark mode, QA | 2–3 d |

---

## Open decisions
1. **Visual world** — commit to culturally-rooted "Casiguran Coast" (recommended), or stay abstract/gradient and polish?
2. **Illustration source** — commissioned, AI-generated, or free cohesive pack?
3. **Purple** — demote to one-of-several (recommended), or keep dominant?
4. **Scope vs deadline** — full 5-phase, or Phase 0–2 only (tokens + top-3 screens)?
