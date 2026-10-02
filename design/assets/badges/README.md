# Tiered badge artwork

`word-explorer-tier-concept-v1.png` is an AI-generated concept board created
with the built-in image-generation tool at the user's request on 2026-09-30.
The exact prompt is in `docs/prompts/word-explorer-tier-concept-v1.txt`.

The five stages are Bronze, Silver, Gold, Platinum and Diamond, using the
Word Explorer thresholds from `docs/design/XP_AND_TIERED_BADGES_PLAN.md`.
The open book and green leaves identify the family; frame materials and
ornaments distinguish each stage.

This board is a design reference. Production assets should be generated as
individual transparent images without labels, counts, forest backgrounds or
large surrounding shadows. Render the family name, tier, requirement and
progress in Compose so they remain accessible and update from real data.
Keep the original generated board intact; do not use it as a sprite sheet.

## Production badges (2026-10-02)

`source/` holds the user's ten six-tier boards, renamed from the downloaded
files. The app art is generated from them by `scripts/generate-badge-art.py`.
Boards are mapped by what they show, not their original file names:

| Family | Board |
|---|---|
| Word Explorer | `word_explorer.png` (speech bubble) |
| Lesson Pathfinder | `six_mountain_achievement_badges.png` (mountain trail) |
| Review Keeper | `lesson_pathfinder.png` (arrows around a checked book) |
| Consistent Learner | `consistent_learner.png` (flame) |
| Game Adventurer, Mode Explorer | `game_mode_explorer.png` (controller, shared) |
| Precision Player | `precision_player.png` (target) |
| Story Reader | `story_reader.png` (open book) |
| Category Scholar | `category_scholar.png` (tabbed book) |
| Community Contributor | `community_contributor.png` (speech bubbles) |
| Journey Rank | `journey_rank.png` (growing tree) |

Game Adventurer has no board of its own yet. When one exists, add it to
`BOARDS` in the script and give it its own list in `BadgeArt.kt`.
