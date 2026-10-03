# Learner web app responsive layout

The learner app in `admin-website/webapp/` keeps the Forest palette, existing
artwork and sourced content. Navigation adapts to the window; content grids
adapt to the space remaining inside the page.

## Layout rules

- Below 768px, the five main tabs use the floating bottom bar. Tab pages reserve
  its full height plus the device's bottom safe area.
- From 768px, navigation uses an 88px rail with visible icons and labels.
- From 1024px, the rail becomes a 240px sidebar with the wordmark and profile.
  Both versions scroll when the window is short.
- Standard pages stay within 760px; content-heavy pages can grow to 1280px.
  Gutters grow from 16px to 40px and respect device cutouts.
- Home, profile and leaderboard columns split at 760px of available page
  content, after the sidebar and gutters. Learn's progress panel appears at
  960px of page content. Base layouts stay in one column when container queries
  are unavailable.
- Library, game and level grids choose columns from their available width.
  Titles and matching answers wrap. Flashcard height follows both faces' content.
- The public player profile shows one row per badge (11 rows) with its highest
  tier and a six-step tier track, rather than 66 separate medals. Its stats,
  showcase and weekly comparison use larger tiles, and leaderboard rows and the
  podium use larger avatars and text. Names wrap between words instead of being
  cut off.
- On page content narrower than 22.5em, profile stat tiles put the icon above
  the number. Below 21.25em, a leaderboard row's score joins its level line so
  the name gets the width. These thresholds use em, so a larger text size
  switches layouts sooner.
- Lessons, games, review, onboarding and story reading keep their focus shell.
  On narrow phones, Word Wheel's shuffle and hint controls sit below its wheel.
- Dialogs stay inside the safe viewport and scroll with long content. Sticky
  profile and progress panels scroll internally; short windows use normal flow.
- Text styles use relative font sizes. A keyboard skip link focuses the main
  page, and the existing reduced-motion behavior remains in place.

## Verification

Checked the real app components in a local Chrome preview with committed
dictionary and story snapshots. Authentication, cloud synchronization and remote
content refresh were not started; external requests were blocked by the browser.

The initial matrix covered phone, tablet, desktop and landscape windows from
320px to 2560px wide, including lessons and all three playable games. After the
learning-path caption fix, 344 checks passed with no horizontal overflow or
runtime exceptions. A further 161 checks verified the final changes at 320px,
390px, 768px, 1440px and 667px landscape, with 150% text size, long names,
simulated cutouts, search, flashcard flipping, a landscape game dialog, a real
navigation tap and keyboard skip-link focus.

TypeScript, the Vite production build, `npm run check:structure` and
`npm run check:web` passed. The production bundle was built directly with Vite
to preserve the committed content snapshots.
