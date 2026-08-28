---
total_score: 16
max_score: 40
na_heuristics: 
p0_count: 1
p1_count: 3
timestamp: 2026-08-20T16-44-50Z
slug: admin-website-download-index-html
---
Method: dual-agent (A: design review, isolated · B: detector + browser evidence, isolated)

## Design Health Score

| # | Heuristic | Score | Key issue |
|---|---|---|---|
| 1 | Visibility of System Status | 2 | Version tag has three honest states; the button beside it has none, styled enabled while inert |
| 2 | Match System / Real World | 2 | "Free & Safe APK", "active-recall gamification mechanics"; "Support the Mission" delivers a download |
| 3 | User Control and Freedom | 2 | 217px sticky header cannot be dismissed; page scrolls sideways ~28px |
| 4 | Consistency and Standards | 1 | Four labels for one action; the two clay buttons render in different typefaces and surfaces |
| 5 | Error Prevention | 1 | Nothing blocks a tap on the dead link; no Play Protect warning; no file size before a 7.6 MB download |
| 6 | Recognition Rather Than Recall | 2 | The install instruction sits ~2000px above the button that needs it |
| 7 | Flexibility and Efficiency | 2 | QR is a real device hop, but nav Get App silently mutates into a direct binary download |
| 8 | Aesthetic and Minimalist | 2 | Clay and type pairing are strong, against a 217px mobile header and 140px empty gutters |
| 9 | Error Recovery | 1 | The one error string has no aria-live, no retry, no mirror, and sits 2000px down |
| 10 | Help and Documentation | 1 | The page's actual job. One sentence covers the hardest step. No footer at all |
| **Total** | | **16/40** | **Poor** |

No heuristic scored n/a: a Persuade surface still has status, errors, and, on a side-load page especially, help.

## Design Specificity Verdict

Category-interchangeable shell with authored ornaments. Strip the nouns and this is a stock app-landing
template: capsule nav, two-column hero with tilted phone mockups, three-card feature grid, gradient CTA
banner, three-step how-it-works, centred download card. Swap Kasiguranin for any product and no structural
decision changes.

The tell is what the page refuses to make big. DESIGN.md states that Kasiguranin headwords are the loudest
thing on any screen that shows them, because the language is the product. The loudest Kasiguranin word here
is singet at 1.15rem, inside a fake phone half-hidden behind the sticky header. The H1 is in English. A page
whose whole argument is that this language is worth saving never lets the language be the largest thing on
screen.

Deterministic scan: 6 findings. low-contrast x1 (#D98200 on #F6F7FB at 2.73:1), nested-cards x4,
skipped-heading x1 (h1 followed by h4). Five of the six sit inside the fabricated phone mockups, which the
design review independently recommended deleting. Both assessments converge on the same object.

The scan previously reported ZERO findings. That was an artifact of missing parser modules, now installed.

Visual overlays: not available. Android Chrome exposes no adb-reachable JS execution surface, so no overlay
was injected and none is claimed. All measurements come from raw pixels or computed CSS.

## Priority Issues

**[P0] The primary action is a dead link during load and after failure.**
Both CTAs ship href="#download-section" and only become downloads when Firestore resolves. On the stated
audience condition of patchy connectivity, tapping does nothing. On the error path both buttons stay fully
styled, fully enabled, fully dead. Fix: ship aria-disabled with a de-emphasised state and a Checking latest
version label, enable in applyReleaseInfo, swap to retry on failure, add aria-live, and ship a static href to
the newest APK as a floor. Command: /impeccable harden

**[P1] The page scrolls sideways and its own header eats 27% of the screen.**
Computed: the secondary phone's right edge lands at 432px against a 411.4px viewport; total scrollable width
about 440px. The floating chip starts at -8px. No overflow-x guard exists. Separately the header stacks to
four rows at 217 CSS px, is position sticky, and there is no scroll-margin-top anywhere, so every anchor jump
buries its own destination heading. Fix: overflow-x clip, clamp the mockup transforms, collapse or unstick
the header below 760px, add scroll-margin-top. Command: /impeccable layout

**[P1] Nothing earns the trust the page asks for, and there is no footer.**
Zero aria, role or alt attributes. No author, institution, year, licence, contact, permissions list,
checksum, or file size. No warning that Android will block the install. The only trust signal is the
self-issued phrase Free and Safe APK, which is the register of an APK mirror rather than an academic
artifact. The two most persuasive true facts for this audience, offline-first and 7.6 MB, appear nowhere.
Command: /impeccable clarify

**[P1] DESIGN.md's hard colour rules are broken in the permanently visible chrome.**
Gold nav star at 1.83:1 and coral heart at 2.31:1 on white, where rule 3 says reward hues are fills and never
foregrounds. The mission banner paragraph is white at 0.92 alpha on the canopy, measuring 4.38:1 and failing
rule 1, which says never fade text on the canopy. The detector independently found #D98200 on #F6F7FB at
2.73:1. Command: /impeccable colorize

**[P2] The mockups are fabricated product evidence, and they depict an app breaking its own design law.**
Invented gamification and a lesson-progress card for a product PRODUCT.md says has no lessons yet. Inside one
frame: an uppercase WORD OF THE DAY eyebrow, a decorative progress ring duplicating adjacent text, and white
on coral. Three Refuse-list items. Five of six detector findings live here. Command: /impeccable distill

## Persona Red Flags

Jordan, first-timer: does not know what an APK is; taps Support the Mission expecting to donate; meets
Blocked by Play Protect after being promised less than two minutes. Abandons.

Casey, distracted mobile: header takes 27% of every screen; a thumb drag slides the page sideways; is shown a
QR code on the phone they are already holding; Get App becomes a 7.6 MB download with no size and no confirm.

Sam, accessibility: zero aria, role or alt. The canvas QR announces nothing. The heading outline is polluted
by fictional mockup content. lang="en" only, so the endangered language is not marked up as a language on a
language-preservation page.

Marites, thesis panellist: finds no footer, no author, no institution, and no citation for the claim that
entries were extracted from published linguistic thesis documentation. Recognises mocked-up screenshots of
unbuilt features and starts doubting the true claims. The hero says dialect; the banner says language.

Jomar, 17, Casiguran, 2GB prepaid: never learns the download costs 7.6 MB, never learns the app works
offline, and reads copy written for his teacher.

## Minor Observations

- The fluid hero type never applies on phones: clamp(2.05rem, 7vw, 3.4rem) is overridden by a flat 2.5rem at
  max-width 640px. A regression from the recent typography pass.
- 22 box-shadow declarations, all raw rgba literals. The violet-shadow token is declared and referenced zero
  times, despite violet-tinted shadows being the app's most characteristic property.
- Off-palette survivors: #7B6EF6 x8, #1C2233 x6, teal #12B3A6 x4, #FFC94A x2, #12161F x2.
- 7 media blocks, two out of descending order; the 760px block lands last and wins on a 420px screen.
- About 257 KB transferred and 695 KB raw, of which roughly 600 KB is JavaScript. Firestore alone is 436 KB
  raw, on a page whose audience is explicitly data-sensitive.
- Render-blocking: iconsax.js and jsDelivr's qrcode.js load synchronously in head; fonts arrive via @import.
- favicon.ico 404s on every load.
- Focus rings are authored only on the three clay buttons; the nav links fall back to the UA default.
- "Explore over 530+" hedges twice, and the count is floored to the nearest ten, discarding precision that
  was already in hand.
- Two instances of the same button component render in different typefaces, and hovering either one makes the
  clay lip vanish before the press restores it.
