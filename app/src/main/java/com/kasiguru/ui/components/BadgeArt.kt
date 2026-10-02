package com.kasiguru.ui.components

import androidx.annotation.DrawableRes
import com.kasiguru.R
import com.kasiguru.domain.gamification.BadgeTier

/**
 * The artwork for one tier of one badge family, or null when the family has none (archived badges).
 *
 * Sliced from the six-tier boards in design/assets/badges/source/ by scripts/generate-badge-art.py.
 * Every drawable is named here, not looked up by string: the release build's resource shrinker keeps
 * only resources the code references, so a name built at runtime would be stripped from the APK.
 */
@DrawableRes
fun badgeArt(familyId: String?, tier: BadgeTier): Int? = badgeArtwork[familyId]?.get(tier.ordinal)

private val modeExplorer = listOf(
    R.drawable.badge_mode_explorer_1, R.drawable.badge_mode_explorer_2, R.drawable.badge_mode_explorer_3,
    R.drawable.badge_mode_explorer_4, R.drawable.badge_mode_explorer_5, R.drawable.badge_mode_explorer_6
)

private val badgeArtwork: Map<String, List<Int>> = mapOf(
    "word_explorer" to listOf(
        R.drawable.badge_word_explorer_1, R.drawable.badge_word_explorer_2, R.drawable.badge_word_explorer_3,
        R.drawable.badge_word_explorer_4, R.drawable.badge_word_explorer_5, R.drawable.badge_word_explorer_6
    ),
    "lesson_pathfinder" to listOf(
        R.drawable.badge_lesson_pathfinder_1, R.drawable.badge_lesson_pathfinder_2, R.drawable.badge_lesson_pathfinder_3,
        R.drawable.badge_lesson_pathfinder_4, R.drawable.badge_lesson_pathfinder_5, R.drawable.badge_lesson_pathfinder_6
    ),
    "review_keeper" to listOf(
        R.drawable.badge_review_keeper_1, R.drawable.badge_review_keeper_2, R.drawable.badge_review_keeper_3,
        R.drawable.badge_review_keeper_4, R.drawable.badge_review_keeper_5, R.drawable.badge_review_keeper_6
    ),
    "consistent_learner" to listOf(
        R.drawable.badge_consistent_learner_1, R.drawable.badge_consistent_learner_2, R.drawable.badge_consistent_learner_3,
        R.drawable.badge_consistent_learner_4, R.drawable.badge_consistent_learner_5, R.drawable.badge_consistent_learner_6
    ),
    // Game Adventurer has no board of its own yet; it shares the controller with Mode Explorer.
    "game_adventurer" to modeExplorer,
    "precision_player" to listOf(
        R.drawable.badge_precision_player_1, R.drawable.badge_precision_player_2, R.drawable.badge_precision_player_3,
        R.drawable.badge_precision_player_4, R.drawable.badge_precision_player_5, R.drawable.badge_precision_player_6
    ),
    "mode_explorer" to modeExplorer,
    "story_reader" to listOf(
        R.drawable.badge_story_reader_1, R.drawable.badge_story_reader_2, R.drawable.badge_story_reader_3,
        R.drawable.badge_story_reader_4, R.drawable.badge_story_reader_5, R.drawable.badge_story_reader_6
    ),
    "category_scholar" to listOf(
        R.drawable.badge_category_scholar_1, R.drawable.badge_category_scholar_2, R.drawable.badge_category_scholar_3,
        R.drawable.badge_category_scholar_4, R.drawable.badge_category_scholar_5, R.drawable.badge_category_scholar_6
    ),
    "community_contributor" to listOf(
        R.drawable.badge_community_contributor_1, R.drawable.badge_community_contributor_2,
        R.drawable.badge_community_contributor_3, R.drawable.badge_community_contributor_4,
        R.drawable.badge_community_contributor_5, R.drawable.badge_community_contributor_6
    ),
    "journey_rank" to listOf(
        R.drawable.badge_journey_rank_1, R.drawable.badge_journey_rank_2, R.drawable.badge_journey_rank_3,
        R.drawable.badge_journey_rank_4, R.drawable.badge_journey_rank_5, R.drawable.badge_journey_rank_6
    )
)
