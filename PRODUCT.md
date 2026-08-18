# KasiGuru — Product truth

> Captured 2026-08-17 during the UI/UX rebuild. Facts come from the codebase, the release pipeline and
> Adrian's direction. Items marked *(assumed)* were inferred from the brief and code, not confirmed.

## What it is

An Android app (`com.kasiguru`, Jetpack Compose, minSdk 26) for learning and preserving **Kasiguranin**,
an endangered Northern Philippine language spoken in Casiguran, Aurora. It is the software artifact of an
undergraduate thesis on gamified mobile learning for language preservation.

## Who it serves

- **Primary — Filipino youth and heritage learners** in and around Casiguran who understand Tagalog and
  are learning or reclaiming Kasiguranin. Phone-first, mid-range Android, often on patchy connectivity.
- **Secondary — academic and community stakeholders** (thesis panel, local cultural workers) who judge
  whether the app is a credible preservation instrument.
- **Tertiary — contributors**, community members who submit words into a moderated review queue.

## What success looks like

A learner opens the app daily, completes a lesson, and can feel that they moved. Today they cannot: the
app has a dictionary and six mini-games but no lesson, so there is nothing to progress through.

## Product truth that must not change

- **Content**: 417 vocabulary entries across 12 categories, each with Kasiguranin / Tagalog / English,
  IPA, four verb-aspect inflections, example sentences and audio. Plus folk stories and cultural context.
- **The learning model is real**: SuperMemo-2 spaced repetition (`Sm2Algorithm`) drives review scheduling.
  This is a thesis claim; it stays exact.
- **Offline-first**: Room is the source of truth; Firestore syncs progress and receives admin edits.
- **Gamification already in the data**: XP, 10 levels, streaks, achievements, stars, public leaderboard.
- **Accounts**: anonymous by default, upgradeable via Google Sign-In or email; guest progress is at risk
  and the app warns about it.
- **Distribution**: side-loaded APK from a Vercel download page, versioned through a Firestore
  `app_releases` collection. There is no Play Store listing.

## Constraints

- Release APK is ~8 MB and should stay small — this audience is data- and storage-sensitive.
- Compose BOM 2024.06.00, Material 3 1.2.x. No shared-element transitions available.
- Fonts arrive via Google Fonts downloadable provider, so they must exist on Google Fonts.
- **Adrian authors all illustration and mascot artwork himself.** Layouts must look finished with zero
  art present and expose optional drawable slots.

## Surfaces

| Surface | Mode | Notes |
|---|---|---|
| Android app | **Operate** | The learner is in a task. This rebuild's main subject. |
| Download site (`admin-website/download`) | Persuade | Convinces a learner to side-load the APK. |
| Admin portal (`admin-website/admin`) | Operate | Internal: moderate submissions, manage corpus, publish releases. |

## What would make a polished result feel wrong

- Childishness. The audience includes teenagers and academics; "playful" must not become "for toddlers".
  *(assumed from the brief's "refined and modern rather than childish")*
- Anything that reads as a generic SaaS dashboard, which is what the previous pass produced.
- Emoji used as interface elements. Explicitly rejected in the previous design pass.
