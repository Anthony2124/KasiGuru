# Web redesign plan: download site and admin panel into Jepjep's Forest

**Written 2026-09-30.** Brief from Adrian: "create a plan for UI/UX redesign of download website and admin
panel. It should look the same as the app."

The Android app moved to **Jepjep's Forest** in `e9d8432` and shipped in v1.17.0. Both web surfaces still
use the retired **Violet Sheet** look: a lavender ground, violet actions and Nunito headings. The job is
to replace that look on both surfaces. The content, functions and data they already carry stay.

## Decisions already made (do not re-ask)

| Decision | Answer | When |
|---|---|---|
| Replace or evolve? | **Replace.** Both surfaces take the app's world as it is | Brief, 2026-09-30 |
| Admin theme | **Dark, same as the app.** This replaces the light-only rule from 2026-08-21. No light toggle | 2026-09-30 |
| Artwork on the download site | **Reuse Adrian's existing app art**: the Jepjep pose WebPs and the seven scenery illustrations. Nothing new is generated. This overrides the taste skill's §4.8 "generate images first" | 2026-09-30 |
| Process | **Mockups first, Adrian signs off, then build.** Nothing live is deleted before sign-off | 2026-08-21, standing |
| Design skills | impeccable + ui-ux-pro-max + taste (`.agents/skills/design-taste-frontend`) on every design step | Standing |
| Admin layout | Keep the bento Overview and rounded cards across **every** tab. Only real data goes into a slot | 2026-08-21, standing |

## The source of truth: what "the same as the app" means, token by token

Every value below is copied from `app/src/main/java/com/kasiguru/ui/theme/`. None was re-picked for
the web. The contrast figures are the app's own measurements, and they get re-measured on the web in
Phase 4.

### Colour (`Color.kt`)

| CSS token | Hex | Role | Measured |
|---|---|---|---|
| `--ground` | `#0A0E0D` | Page background ("night") | white 19.4 |
| `--surface` | `#141414` | Cards, rows, inputs, bars | white 18.4 |
| `--sunken` | `#1C211D` | Search fields, inactive segments, table zebra | white 16.4 |
| `--track` | `#2C312D` | Progress tracks | non-text |
| `--hair` | `#3A3F3B` | 1px borders on cards and rows | non-text |
| `--canopy-top` / `--canopy-bottom` | `#0F3A10` → `#0A1A0C` | Hero panel gradient | white 12.9 / 18.0 |
| `--glow-core` / `--glow-mid` | `#0A250B` / `#0A190C` | Radial glow behind Jepjep, no rings | |
| `--lime` | `#71BD1D` | **The one thing to do**: primary buttons, active nav, links, selected borders | 8.3 on ground |
| `--lime-lip` | `#3F6D0E` | Clay lip under lime buttons | |
| `--lime-tint` | `#1C2E12` | Selected row fill | lime text 6.2 |
| `--on-lime` | `#0B1A05` | Label on any lime fill. **White on lime is 2.3 and banned** | 7.7 |
| `--brand-lime` | `#6CB619` | "Guru" in the wordmark, highlighted words in headings | 7.7 |
| `--olive` / `--olive-deep` | `#446025` / `#2C4418` | Selected segment or tile; olive hero gradients | white 7.1 / 10.8 |
| `--cream` | `#F2ECCA` | Jepjep's barong: warm highlights, **focus rings** | 16.3 |
| `--ink` / `--muted` / `--faint` | `#FFFFFF` / `#C9D1C5` / `#8C948A` | Text tiers | muted 11.8, faint 5.9 on surface |
| `--gold` / `--coral` | `#FFC83D` / `#FF9F1C` | XP and streak. They are fills that carry `--reward-ink` | |
| `--reward-ink` | `#0B1A05` | Text on gold, coral, lime and red fills | 11.7 on gold |
| `--red` / `--red-deep` / `--red-tint` | `#FF6B5B` / `#C8352B` / `#3A1E1C` | Error text / destructive button face (white 5.3) / error fill | red 6.6 |
| `--amber` / `--amber-tint` | `#F5C86A` / `#3A2E12` | Caution: pending, duplicate warnings | 11.7 |
| `--info` | `#4FB3E8` | Audio, tips, help links | 7.9 |
| `--scrim` | `#000` at .72 | Behind modals | |

### Type (`Type.kt`)

- **Display: Fredoka** 600/700. It sets every heading, every button, and every number that celebrates
  (XP, counts, version numbers). Fredoka stops at 700; nothing asks it for a heavier weight.
- **Body: DM Sans** 400/500/700. It sets reading text, table cells, form fields and labels.
- **The scale** (px equals the app's sp): display 40/34/30, headline 26/22/21, title 18/17/15, body 16/14/12,
  label 16 (Fredoka, buttons) / 13 / 11 (DM Sans bold). Web body text never goes below 16px on phones.
- **The voice switches deliberately.** Anything that tells you how you did is Fredoka. Anything that
  gives you something to read is DM Sans.
- **Fonts are self-hosted** as woff2 subsets, the way the app bundles them. Google Fonts would add a
  second origin, and this audience is on poor connections.

### Shape, space, motion (`Dimens.kt`, `Motion.kt`)

- **Radius, four steps only.** Chip 14, tile 20, panel 28, sheet 32, plus pill 999. The old web radii go.
- **Space scale:** 4 / 8 / 12 / 16 / 24 / 32 / 48, with a 20px page gutter on phones.
- **Depth comes from the lip and the hairline, not from shadow.** Shadows barely read on night. Cards
  are `--surface` with a 1px `--hair` border. The old violet shadows go.
- **Motion:** 120 quick, 240 standard, 400 emphasised; exits run at about 65% of their entrance. Ease
  out is `cubic-bezier(.05,.7,.1,1)` and ease in is `(.3,0,.8,.15)`. List stagger is 35ms, capped at 8
  items. Under `prefers-reduced-motion`, nothing depends on an animation having run.
- **Touch targets:** at least 48px. That is the app's figure, and it is stricter than WCAG's 24px.

### Components to port (web equivalents of the Compose set)

| App component (`ui/components/clay/`) | Web class | Used on |
|---|---|---|
| `ClayButton` Primary / Quiet / Reward | `.btn-clay.primary` (lime face on lime-lip, press compresses the lip), `.btn-clay.quiet` (surface face, hair lip, white label), `.btn-clay.danger` (red-deep, white label) | both |
| `ClayFab` / `ClayCircle` | `.clay-circle` | download (audio sample), admin (play word audio) |
| Soft card / ground surfaces | `.card` (surface, hair border, radius 28 panel / 20 tile) | both |
| `SegmentedToggle` | `.segmented` (dark track, olive active pill, white label) | admin filters, download "Words / Stories" |
| `TagChip` | `.chip` (tinted fill, ink label) | word grammar, status |
| `DotTabs` | `.dot-tabs` | admin detail drawers |
| `FloatingSearchBar` | `.search` (sunken field, lime focus border, cream focus ring) | admin dictionary, download word sampler |
| `Progress` | `.meter` (track + lime fill) | admin category coverage |
| `Glow` + canopy | `.canopy`, `.glow` | download hero, admin Overview header |
| `Wordmark` | inline SVG from `res/drawable/kasiguru_wordmark.xml` ("Kasi" white, "Guru" brand-lime) | both headers |
| `StateViews` (empty / error / loading) | `.state` with a Jepjep pose | admin empty tabs, download offline fallback |

**Clay is still reserved** for things you press or earn: primary CTAs, approve, reject, publish. Content
never uses clay. The refuse list carries over unchanged: no eyebrow labels above headings, no three
equal cards as page structure, no emoji as interface, no gradient text, and no glass over plain night.

---

## Phase 0: Foundation (no visible change)

1. **Rewrite `DESIGN.md`** so Jepjep's Forest is the authority for the app **and** both web surfaces.
   Violet Sheet moves to a short "History" note. Also rewrite the "two web surfaces" section.
2. **Update `scripts/check-web-tokens.js`.** Its `CORE` list names Violet Sheet tokens (`--violet`,
   `--canopy-*`). Replace them with the new identity core: ground, surface, hair, lime, lime-lip, on-lime,
   brand-lime, olive, cream, ink, muted, reward-ink, red-deep, plus the radii. Then re-record
   `KNOWN_DIVERGENT`.
3. **Export the fonts.** Fredoka 600/700 and DM Sans 400/500/700 go to `woff2` in each surface's
   `fonts/`, subset to Latin plus the diacritics Kasiguranin needs (glottal stop and vowel-length
   marks). Check the subset against real dictionary entries.
4. **Prepare web copies of Adrian's art.** Copy from `app/src/main/res/drawable*/`:
   - the Jepjep poses: `waving`, `with_backpack`, `pointing_a_lesson`, `celebrating`, `reading`, `sleeping`,
     `confused`
   - the seven scenes: `scene_forest`, `scene_river`, `scene_ontok_lighthouse`, `scene_ermita_hill`,
     `scene_farm`, `scene_casapsapan`, `scene_tibu_tidal_pool`

   Make two widths of each (1× and 2×) for `srcset`. The files are resized only, never edited.
5. **Measure contrast** for every web pairing with a WCAG script (20 lines; rewrite it if missing). This
   covers the ones the app never had: table zebra (`--sunken` rows), a disabled button, a placeholder on
   `--sunken`, and the status chips.

## Phase 1: Mockups (the sign-off gate)

Standalone HTML in `design/mockups/`. Nothing live is touched.

- `forest-download.html`: the whole landing page.
- `forest-admin.html`: the login screen, the Overview bento, the Submissions queue with a detail drawer
  open, the Dictionary table, and the Publish Release modal. These five cover every component.

Each mockup gets a screenshot at **390px** (emulator, `adb reverse`) and at **1440px**. Adrian approves
or redirects. Nothing moves to Phase 2 without that approval.

## Phase 2: Download site (Persuade), `admin-website/download/`

The visitor has one job: install the APK. The section order stays close to today's, because the content is
right and only the look is wrong.

| Section (today's id) | Jepjep's Forest treatment |
|---|---|
| Header / `#site-nav` | Night bar, wordmark left, one lime **Download** pill right. On phones it is the wordmark plus that pill, with no hamburger |
| Hero `#how-it-works` | The app's canopy gradient with the green glow behind **Jepjep waving**. Headline in Fredoka 40/30 with one brand-lime word. The version and size are read live from `app_releases`. One primary clay button, and the QR only at ≥1024px |
| Why it matters `#heritage` | Today it is four equal cards, which the refuse list bans. It becomes one asymmetric block: `scene_ontok_lighthouse` as a tall image panel beside a list of the four facts (verified by speakers, IPA, practice, free) |
| Six ways to practise `#practice` | Game tiles styled like the app's Practice tab: olive-deep gradient tiles with each game's scene behind them. Horizontal scroll-snap on phones, a 3×2 grid on desktop |
| The words `#dictionary` | Real entries from the corpus as app-style word cards: a large headword, TagChips for aspect, and an Info-blue audio button where audio exists. **Only real entries, never invented Kasiguranin** |
| Voices `#voices` | Keep it only if the quotes are real. If they are not, cut the section |
| Download hub `#download-section` | **Jepjep with backpack**, then three side-load steps as a numbered path like the lesson path's nodes, with the QR, the version and the release notes |
| Footer | Night, hairline top, a quiet wordmark |

**Keep** every id and class that `download.js` and the QR canvas depend on. **Keep** the static
`href` to `/releases/latest/download/kasiguru-latest.apk`. **Remove** the `prefers-color-scheme` block,
because the site becomes dark-only like the app, and declare `color-scheme: dark`.

## Phase 3: Admin panel (Operate), `admin-website/admin/`

The moderator has one job: clear the queue and keep the dictionary correct. The brand shows in the details;
scanning speed matters more than expression.

**Shell**
- **Desktop:** a night sidebar with the wordmark and ten tabs grouped in three sections. *Moderate*:
  Submissions, Reports, Users. *Content*: Dictionary, Stories, Stages. *System*: Releases, Backup, Logs.
  Overview stays at the top. The active tab gets the lime-tint fill and a lime icon. Pending counts show
  as amber pills.
- **Phones:** ten tabs cannot fit the app's bottom bar, which holds at most five. Phones get a top bar
  (wordmark, the current tab's name, a menu button) that opens the sidebar as a sheet. The four
  highest-traffic tabs (Overview, Submissions, Dictionary, Releases) also sit in a bottom bar styled
  like `KasiGuruBottomBar`.
- The hash routes (`#submissions`) already work. Keep them.

**Tabs**
| Tab | Change |
|---|---|
| Login (`index.html`) | A canopy panel with the glow and **Jepjep waving**, a card form, a lime Sign in clay button, a quiet Google button. Errors show in the red tint under the field |
| Overview | The bento carries over in the new materials. Pending queue count (large Fredoka number), vocabulary by category (lime meters), submission outcomes, recent submissions, story shelf, live release, top learners from `leaderboard_public`. **No invented figures**: the leaderboard card says it lists only learners with XP |
| Submissions / Reports | A list plus a detail drawer. Status chips use icon **and** colour: amber clock for pending, lime check for approved, red cross for rejected. Approve is primary clay, reject is danger clay, and they are spaced apart |
| Dictionary | A dense table: 48px rows, `--sunken` zebra, hairline dividers, a sticky header, the existing 50-per-page pagination restyled, the search field as `.search`, the category filter as `.segmented` or a select |
| Stories / Stages / Users / Logs / Backup | Cards and rows in the new materials. Destructive actions (reset, restore, delete) are danger clay behind a confirmation dialog |
| Releases | The release header on the canopy gradient. The publish form in a modal with the scrim |

**Fixes that come with the restyle**
- **Replace the two remaining `confirm()` calls** in `js/app.js` with the in-page confirmation dialog.
- **Cream focus ring**, 2px with a 2px offset, on every control.
- **Empty and error states** use `StateViews`' pattern with a Jepjep pose (sleeping for an empty queue,
  confused for an error) plus one action.

`js/app.js` (5,220 lines) is restyled, not rewritten. Markup changes stay at the template level, and the
Firestore logic is left alone.

## Phase 4: Verify and ship

1. **Render at 390px on the emulator and at 1440px on desktop.** Do one batched round, fix everything it
   shows, then do at most one more round.
2. **Admin behind auth:** build a scratchpad preview that lifts the real markup and stylesheet. Never
   stub the auth guard.
3. **Run the checks:** the contrast script, `node scripts/check-web-tokens.js`, the impeccable detector
   on the changed files, and a keyboard-only pass through login → approve a submission → publish a
   release.
4. **Ship:**
   - **Admin:** push to `main`. The Vercel project is Git-linked.
   - **Download site:** `npx vercel deploy --prod` from `admin-website/download/` after
     `npx vercel whoami` returns Anthony's account. APKs are GitHub Release assets now, so the site no
     longer carries binaries.
   - `git fetch` and rebase before pushing, because `main` is shared.

## Files touched

- **Rewritten:** `DESIGN.md` (web section + authority), `admin-website/download/css/styles.css`,
  `admin-website/admin/css/styles.css`.
- **Markup edits:** `admin-website/download/index.html`, `admin-website/admin/index.html`,
  `admin-website/admin/dashboard.html`.
- **Small JS edits:** `admin-website/admin/js/app.js` (the `confirm()` calls, status-chip templates,
  the phone bottom bar), `admin-website/download/js/download.js` (hero version text only if its target
  moves).
- **New:** `fonts/` and `img/` in each surface, `design/mockups/forest-*.html`.
- **Updated:** `scripts/check-web-tokens.js`.

## Out of scope: the data does not exist

Download counts, analytics charts, a full user roster, per-user progress, APK upload from the browser,
and lesson editors. The reasons are in `HANDOFF.md` under "What NOT to build" (Spark plan, no analytics,
no Storage). The redesign adds no slot for any of them.
