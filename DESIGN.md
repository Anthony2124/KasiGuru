# KasiGuru — Design authority

> Durable visual decisions for the Android app and its two web surfaces. Supersedes the previous
> "Casiguran Coast" system. Every contrast figure below was measured with the WCAG relative-luminance
> formula, not estimated; the validator lives in the scratchpad and the numbers are reproducible.

## The world: **Violet Sheet**

Synthesised from five reference designs Adrian supplied. They are one family, not five directions: all
use violet as the single brand hue, a very light ground, white rounded cards floating on soft diffuse
shadows, generous radii, and a floating bottom bar. The ui-ux-pro-max design-system generator
independently resolved this product to **Claymorphism** — *"soft 3D, chunky, playful, double shadows,
rounded 16-24px; best for educational apps"* — which corroborated the references rather than competing
with them.

### Two shells: Canopy and Ground

**The canopy belongs to Learn, and to nothing else.** A violet canopy carrying *who you are and where
you stand today*, with a white **sheet** — rounded top corners, overlapping upward into it — carrying
*the work*. Three of the five references make exactly this move, and on the app's front door it earns
its space: Learn is the one screen with a streak, a daily goal and a week to state.

It used to be on all seventeen screens, on the reasoning that one repeated motif gives the app a
recognisable spine. In practice sixteen violet headers made the violet mean nothing, every secondary
page read as a near-copy of home, and a hero block on Settings was decoration rather than information.
A spine stops being a spine when everything is spine. The per-screen sizing rule that went with it
("tall on Learn and Progress, short on Dictionary") has no subjects left: there is exactly one canopy,
at one height.

**Every other screen uses the Ground shell.** Not a shortened canopy — the deliberate removal of both
things that constitute one. `Ground` runs unbroken from behind the status bar to the bottom edge, and
white appears only as `SoftCard`s floating on it, which is the relationship the palette table already
described. A fixed 56 dp bar carries the back affordance and any actions; an optional large title sits
in the content and scrolls away, handing its title to the bar; a 1 dp hairline is the only boundary,
and only once scrolled.

| | Canopy — Learn only | Ground — every other screen |
|---|---|---|
| Top region | Violet gradient, `statusBar + 244 dp` | No block; `Ground` under the status bar; fixed 56 dp bar |
| Body | White sheet, 32 dp corners, overlaps up 26 dp | No sheet, no seam, no radius |
| Type | Pure white on violet | `Ink` title, `Muted` subtitle |
| Back | 36 dp white-at-18% pill | Bare `Ink` chevron, 48 dp target |
| Boundary | The sheet's radius | 1 dp hairline, only while scrolled |
| Status-bar icons | Light | Follows the theme |

The Ground shell introduces **no new contrast pairings**: `Ink`/`Ground` at 14.43 and `Muted`/`Ground`
at 5.67 are already in the measured table below.

**Status-bar icons are the shell's job, not the theme's.** They were forced light app-wide on the
premise that every screen opened with a canopy. That was never true of Onboarding, where white glyphs
sat on `#F1EEFF` at roughly 1.05:1 and were invisible. Each shell now declares what is behind them.

### Pattern layer

Ground screens draw a texture behind their content so a page of white cards on lavender does not read
as flat. It is drawn in a `Canvas`, never shipped as a raster: the release APK has to stay small for a
data-sensitive audience, and a drawn pattern re-derives itself from the theme tokens, so it follows
dark mode for free.

- **Orbs** (default) — two or three soft radial fields in Violet/Coral/Gold at 0.06–0.10 alpha.
  Placement varies by screen title, so twenty-two screens do not carry one identical arrangement.
- **Grid** — a 24 dp dot lattice at `Ink` 0.04, for list-heavy screens where colour fields fight rows.
- **Arcs** — concentric rings stepped by the sheet radius, for screens about something earned.
- **None** — a game in play, where any texture competes with the exercise.

**No `Modifier.blur` on the pattern.** Blur is API 31+ against a minSdk of 26, and a radial gradient is
already a soft-edged disc — blurring one buys no visible softness while putting a full-screen render
pass on the mid-range phones this app targets.

### Soft by default, clay for rewards, glass on vivid backdrops only

The references split on depth: four are soft and neumorphic, one is chunky and dimensional. Combining
them is a discipline, not an average.

- **`SoftCard`** — the default. White, one diffuse violet-tinted shadow, no lip. Carries content.
- **`ClaySurface`** — a solid darker lip beneath the face, a lit top gradient, and a press that
  physically compresses. Reserved for **things you earn or press**: podium blocks, badges, the FAB, the
  primary button.
- **`GlassPanel` / `GlassChip`** — a translucent panel with a diagonal light-catching edge and a soft
  violet shadow. Reserved for content riding on top of something already vivid: a stat readout on the
  canopy, a status pill on a reward-tier gradient, a summary panel straddling the canopy-to-sheet seam.
  Never a page's ordinary content surface — over the plain Ground or a white card a translucent white
  fill has nothing to differentiate itself from and just reads as a duller `SoftCard`, which is exactly
  the "glass and blur as decoration rather than as a specific effect" failure the craft floor bans.

Nothing that is merely content gets built out of clay, and nothing gets a glass treatment unless it is
genuinely floating over a vivid backdrop. That restraint is what keeps the app from reading as a toy,
which matters because the audience includes teenagers and a thesis panel.

**Glass has no real backdrop blur below Android 12.** `Modifier.blur` renders only on API 31+ — minSdk
here is 26 — and Compose has no portable backdrop-filter that samples arbitrary content behind a
composable; adding one would mean a new dependency this rebuild did not need. Glass's identity therefore
never depends on blur: translucency, the light-catching edge and the violet shadow render identically on
every device, and an optional `glow` slot lets a panel blur a couple of its own decorative colour discs
(not a capture of what's behind it) on the API levels that support it, as a bonus. Because a lightened
backdrop reads white text slightly less than the canopy alone, glass content follows the canopy's own
placement rule: pure white text, on the canopy's deep end or a comparably deep fill, never directly on
`CanopyTop` — see the translucent-chip rule below, which the same measurement governs.

**Mode: Operate.** The learner is in a task. Familiarity and scanability outrank expression; the brand
lives in the canopy, the clay, the glass, and the motion — not in novel affordances for standard jobs.

**Where glass is legal, as of the Ground restructure.** Glass needs a genuinely vivid backdrop, and
sixteen screens just lost theirs. It is legal on the Learn canopy, on a clay or reward fill, and on a
dialog's colour band — and nowhere else. A translucent white fill over `Ground` has nothing to
differentiate against and degrades into a duller `SoftCard`, which is the failure this rule already
named. Enforcing it removed the Progress `GlassPanel` (now gold clay, since it counts badges earned)
and five detail-screen `GlassChip`s (now `TagChip`s, a progress bar, and one rank card). Two callers
remain, both correct: the game rules dialog's gradient band, and Onboarding's hero panel.

## Palette (verified)

| Token | Hex | Role |
|---|---|---|
| `Ground` | `#F1EEFF` | App background. Soft lavender; white cards read as floating. |
| `Surface` | `#FFFFFF` | Cards, rows, sheets. |
| `CanopyTop` | `#6C5CE7` | Canopy gradient start. |
| `CanopyBottom` | `#4A3FC0` | Canopy gradient end. |
| `Violet` | `#5B4CDB` | Interactive violet on light surfaces: links, active nav, focus. |
| `Ink` | `#1F1B3A` | Primary text. |
| `Muted` | `#5E5A80` | Secondary text. |
| `Faint` | `#8A86A6` | Placeholders, disabled. Non-text only below 4.5. |
| `Coral` | `#FF8B5E` | Callout fills. **Ink text only.** |
| `Gold` | `#FFB020` | XP and reward fills. **Ink text only.** |
| `Green` | `#15803D` | Correct / success. Carries white text. |
| `Red` | `#DC2626` | Incorrect / destructive. Carries white text. |

Measured: ink/ground **14.43**, muted/ground **5.67**, ink/card **16.46**, muted/card **6.46**,
white/canopy-top **4.86**, white/canopy-bottom **7.60**, ink/coral **7.14**, ink/gold **9.00**,
white/green **5.02**, white/red **4.83**, violet/card **6.00**. All ≥4.5. Zero failures.

### Three hard colour rules, each from a measured failure

1. **Never fade text on the canopy.** White at 0.70–0.90 alpha measures 3.23–4.27 on `#6C5CE7` — all
   fail AA. Secondary text on the canopy is **pure white**, differentiated by size and weight, never by
   opacity. Faded white is for decoration only (dividers, wave shapes, chip fills).
2. **Translucent chips belong on the deep end.** `white@0.22` over `#6C5CE7` yields `#8C80EC`; a white
   label on it measures **3.27 — fail**. The same chip over `#4A3FC0` measures **4.54 — pass**. Put
   translucent chips low on the canopy, or give them a solid fill with ink text.
3. **Coral and gold are fills, never foregrounds.** On white they measure **2.31** and **1.83** — below
   even the 3:1 non-text floor. A gold icon on a white card is invisible to a low-vision user. Use them
   as backgrounds with ink on top; when a gold *mark* is needed on light, use `Violet` or ink instead.

## Typography

**Nunito** (display) + **DM Sans** (body) — the ui-ux-pro-max "Claymorphism Mobile" pairing, scoped to
children's education and gamification. Rounded terminals are what make Nunito belong to clay; a
geometric-square face would fight the world. Both are on Google Fonts, so the existing downloadable-font
provider serves them with no APK cost.

- **Nunito 800/900** — all headings, every number that celebrates (XP, streak, score, rank).
- **DM Sans 400/500/700** — body, labels, controls, dictionary entries, long-form story text.
- Scale (sp, so system font scaling works): Display 40/44/−1 · Headline 28/34/−0.7 · Title 22/28/−0.4 ·
  Card title 18/24/−0.3 · Body 16/24/0 · Label 13/18/+0.2.
- Kasiguranin headwords are the loudest thing on any screen that shows them. The language is the product.

## Clay technique in Compose

Compose has no inset-shadow API, so claymorphism's "double shadow" is built from three real layers:

1. **The lip** — a solid shape in a darker tone of the face colour, offset **+5 dp on Y**, drawn behind.
   This is what makes the podium blocks, the FAB and the badges read as solid objects rather than circles.
2. **The lit top** — a vertical gradient overlay on the face, `White @0.22 → Transparent` across the top
   45%.
3. **The cast shadow** — `Modifier.shadow()` with violet-tinted ambient and spot colours, never neutral
   grey, and always with both offset and blur.

Pressing a clay object **reduces the lip to 1 dp and drops the face 4 dp** — the object physically
compresses. 120 ms down, 180 ms release. This replaces ripple on clay controls; flat controls keep ripple.

## Shape and spacing

Radii: chip 14 · tile 20 · panel 28 · sheet 32 · pill 999. Spacing steps 4/8/12/16/24/32/48, gutter 20 dp.
Claymorphism's 16–24 dp radius band is the floor for anything clay; flat list rows may go tighter.

## Motion

Operate mode: **150–250 ms** on most transitions, and motion must carry state, never decorate.
One authored moment per screen, not an entrance on every element.

- Clay press: compress/release, as above.
- Answer feedback: the feedback panel rises from the bottom edge in 220 ms with a colour flood.
- XP and counters: `animateIntAsState`, 600 ms, ease-out — the number counts, it does not fade in.
- Path unlock: the newly available node scales 0.8→1 with a single spring overshoot. Once, on unlock.
- Ground shell title handoff: the compact title and its hairline crossfade in together over 150 ms,
  once the large title has scrolled 40 dp out of view.
- Honour the system "Remove animations" setting with a crossfade or instant cut. This is a contract,
  not a courtesy: durations collapse to zero via `LocalReducedMotion`, and nothing may depend on an
  animation having run. Tokens live in `ui/theme/Motion.kt` — quick 120 / standard 240 / emphasized
  400 ms, exits at roughly 65% of their entrance, so everything moves to one rhythm.

## Android structure (non-negotiable)

Material 3 governs structure; the brand expresses through Material's theming.

- **Navigation bar**, 5 labelled destinations, 48 dp targets, and **no docked FAB**. A copied iPhone
  bottom bar is the top Android slop tell; the reference's iOS chrome is reinterpreted, not
  transcribed. The continue action is a primary `ClayButton` at the top of Learn instead, for two
  reasons: an action whose meaning changes depending on which tab you are on is not a floating action,
  and the docked FAB overflowed the bar it was docked into, so roughly its top 12 dp never received
  touches. A full-width button is wholly tappable and can say which of "continue" or "review" it
  currently means.
- Predictive **Back** always works. Edge-to-edge with real window insets — status bar, navigation bar,
  cutout and IME.
- One primary action per screen, stated in place. The app has no FAB. Snackbars for transient feedback; dialogs only for decisions that
  must interrupt.
- `sp` for type, `dp` for space. Never fixed px.

## Refuse

Beyond impeccable's craft floor, these are specific to this product's history:

- **Eyebrow/kicker labels above headings.** The current app has `"COMMUNITY CONTRIBUTIONS"`,
  `"DICTIONARY CORPUS"`, `"DAILY GOAL"`, `"LEADERBOARD"` chips stacked over their own headings. The
  heading carries its own weight. All of them go.
- **Cards as page structure.** Same-size rounded rectangles in a vertical stack is the failure this
  rebuild exists to fix. Cards hold content; they are not the layout.
- **Emoji as interface.** Already purged once; it must not return through `GamificationEngine`.
- **Gradient text.** Emphasis comes from weight and size.
- **Decorative progress rings and sparklines** standing in for content.
- **A canopy on a screen that has nothing to state.** A hero block over a settings list is decoration.
- **Glass over the Ground or over a white card.** Glass needs a vivid backdrop or it is just grey.
- **A bottom-bar action whose meaning depends on which tab you are on.**
- **A control that overflows its parent.** In Compose it silently stops receiving touches there.

## Illustration

**Adrian authors all artwork.** Every art position is an optional `@DrawableRes` defaulting to null;
layouts must look finished with none present. Document filename, aspect ratio and dp size at each slot.

**Story covers.** `story_cover_<id>`, 3:2, rendered at roughly 160x107 dp on Learn's shelf and
gutter-width on the Stories screen. With it absent the violet field plus the page count is a finished
cover, not a placeholder — which is the state every story ships in today.

**Ground pattern overlay.** An optional tileable motif drawn over the pattern layer at 0.05–0.08 alpha,
1080x1080, PNG or vector. Every screen must look finished with it null.

## Web Surface Architecture & Specification

DESIGN.md governs the Android app and its two web surfaces (`admin-website/download` and `admin-website/admin`). Both web surfaces read as direct continuations of the Android app's design system.

### Shared Layer & Canonical Source

The two web surfaces are separate static deployments with no build step. To prevent visual drift without a complex bundler:
- Canonical source files live in `admin-website/shared/`:
  - `tokens.css` — palette, type scale, spacing, radii, elevation, motion tokens.
  - `components.css` — clay, SoftCard, TagChip, status badges, section headings, progress bars, state placeholders, toasts, focus rings, reduced motion.
- `scripts/sync-web-shared.js` copies these into each surface's `css/` folder.
- CI runs `node scripts/sync-web-shared.js --check` to enforce zero drift.

### Clay in CSS

The web ports the Android app's 3-layer clay press language:
1. **The Lip:** Solid bottom edge offset (`5px` rest, `1px` active) in a deeper tone (`--violet-deep`, `--gold-deep`, `--red-deep`, or `--violet-tint`).
2. **The Lit Top:** Vertical highlight gradient (`linear-gradient(to bottom, rgba(255,255,255,0.22), transparent 45%)`).
3. **The Cast Shadow:** Soft violet-tinted shadow (`0 10px 20px -4px var(--violet-shadow)`), never neutral grey.
4. **Timing:** `120ms` down (`--dur-quick`), `180ms` release (`--dur-exit`).

```css
.clay {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: var(--clay-face, var(--violet));
  color: var(--clay-label, #FFFFFF);
  box-shadow: 0 var(--clay-lip) 0 0 var(--clay-lip-color, var(--violet-deep)), 0 10px 20px -4px var(--violet-shadow);
  transition: box-shadow var(--dur-exit) var(--ease-out), transform var(--dur-exit) var(--ease-out);
}
.clay:active {
  transform: translateY(4px);
  box-shadow: 0 var(--clay-lip-pressed) 0 0 var(--clay-lip-color, var(--violet-deep)), 0 3px 8px -2px var(--violet-shadow);
  transition-duration: var(--dur-quick);
}
```

### Web Typography Mapping (16px Root)

| Android Token | Web Rem | Size | Line Height | Tracking | Family |
|---|---|---|---|---|---|
| `DisplayLg` | `2.5rem` | 40sp | 1.1 | `-0.025em` | Nunito 900 |
| `DisplayMd` | `2.125rem` | 34sp | 1.15 | `-0.0225em` | Nunito 900 |
| `DisplaySm` | `1.75rem` | 28sp | 1.2 | `-0.0175em` | Nunito 800 |
| `HeadlineLg` | `1.625rem` | 26sp | 1.25 | `-0.015em` | Nunito 800 |
| `HeadlineMd` | `1.375rem` | 22sp | 1.3 | `-0.010em` | Nunito 800 |
| `HeadlineSm` | `1.25rem` | 20sp | 1.3 | `-0.010em` | Nunito 800 |
| `TitleLg` | `1.125rem` | 18sp | 1.35 | `-0.0075em` | Nunito 800 |
| `TitleMd` | `1.0rem` | 16sp | 1.4 | `-0.005em` | Nunito 700 |
| `TitleSm` | `0.875rem` | 14sp | 1.4 | `-0.0025em` | Nunito 700 |
| `BodyLg` | `1.0rem` | 16sp | 1.55 | `0` | DM Sans 400 |
| `BodyMd` | `0.875rem` | 14sp | 1.5 | `+0.0025em` | DM Sans 400 |
| `BodySm` | `0.75rem` | 12sp | 1.45 | `+0.005em` | DM Sans 400 |
| `LabelLg` | `0.875rem` | 14sp | 1.3 | `+0.0025em` | DM Sans 700 |
| `LabelMd` | `0.8125rem` | 13sp | 1.35 | `+0.005em` | DM Sans 500 |
| `LabelSm` | `0.6875rem` | 11sp | 1.3 | `+0.036em` | DM Sans 700 |
| `KasiguraninHeadword` | `2.25rem` | 36sp | 1.15 | `-0.028em` | Nunito 900 (`.headword`) |

### Responsive Breakpoints

Mobile-first progression:
- `360px–420px`: Compact mobile; full-width stacked CTAs, comfortable ≥48px touch targets.
- `640px`: Small tablet / landscape phone; grids adjust to 1–2 columns.
- `720px / 768px`: Tablet boundary; admin sidebar switches from slide-out mobile drawer to fixed desktop navigation.
- `1024px`: Desktop; multi-column bento grids and asymmetric editorial feature layouts.
- `1200px`: Max page container width (`--page-max`).

### Component Equivalence

| App Component (Compose) | Web Class | Role & Notes |
|---|---|---|
| `ClayButton(Primary)` | `.clay.clay--primary` | Main action, violet face, violet-deep lip, white text |
| `ClayButton(Reward)` | `.clay.clay--reward` | Gold face, gold-deep lip, **ink text only** |
| `ClayButton(Quiet)` | `.clay.clay--quiet` | Surface face, violet-tint lip, violet text |
| `ClayButton(Danger)` | `.clay.clay--danger` | Red face, red-deep lip, white text |
| `SoftCard` | `.soft-card` | White surface with diffuse `--violet-shadow`, 28px radius |
| `TagChip` | `.tag-chip` | Violet-tint background with violet text |
| `StatusBadge` | `.badge.badge--{status}` | Semantic status indicators with text + color |
| `Snackbar` | `.toast` | Transient feedback host (`#toast-host`), replacing alert() |
| `LinearProgressIndicator` | `.progress` + `.progress__fill` | Violet on neutral track |

