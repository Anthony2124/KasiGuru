package com.kasiguru.ui.theme

import androidx.compose.ui.graphics.Color
import com.kasiguru.R

enum class BentoSpan {
    HERO_2X2,
    MEDIUM_2X1,
    SMALL_1X1
}

data class CategoryMetaData(
    val name: String,
    val iconRes: Int,
    val customDrawableRes: Int? = null,
    val startColor: Color,
    val endColor: Color,
    val description: String,
    val bentoSpan: BentoSpan
)

/**
 * CategoryRegistry mapped STRICTLY to the 3 Signature Play Gradients:
 * 1. Purple (#7B6EF6 -> #A78BFA)
 * 2. Gold (#FFC94A -> #FF9F1C)
 * 3. Pink (#FF9FC0 -> #FF6FA0)
 */
object CategoryRegistry {
    val categories = listOf(
        CategoryMetaData(
            name = "Greetings & Essentials",
            iconRes = Iconsax.BookBold,
            startColor = PlayPurpleStart,
            endColor = PlayPurpleEnd,
            description = "Hellos, politeness, questions & basic phrases",
            bentoSpan = BentoSpan.HERO_2X2
        ),
        CategoryMetaData(
            name = "Food & Dining",
            iconRes = Iconsax.VolumeHighBold,
            startColor = PlayGoldStart,
            endColor = PlayGoldEnd,
            description = "Rice, fruits, dishes, drinks & cooking",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Animals & Wildlife",
            iconRes = Iconsax.FlashBold,
            startColor = PlayPinkStart,
            endColor = PlayPinkEnd,
            description = "Carabao, birds, dogs, fish & forest life",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Body Parts & Health",
            iconRes = Iconsax.ProfileBold,
            startColor = PlayPurpleStart,
            endColor = PlayPurpleEnd,
            description = "Anatomy, head, limbs, face & senses",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Numbers & Time",
            iconRes = Iconsax.Calendar,
            startColor = PlayGoldStart,
            endColor = PlayGoldEnd,
            description = "Counting 1-10, days, times of day & seasons",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Weather & Climate",
            iconRes = Iconsax.Global,
            startColor = PlayPinkStart,
            endColor = PlayPinkEnd,
            description = "Rain, wind, sun, clouds & temperature",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Emotions & Feelings",
            iconRes = Iconsax.StarBold,
            startColor = PlayPinkStart,
            endColor = PlayPinkEnd,
            description = "Happy, angry, sad, afraid & love",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "House & Daily Life",
            iconRes = Iconsax.HomeBold,
            startColor = PlayGoldStart,
            endColor = PlayGoldEnd,
            description = "Home objects, clothing, tools & routines",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Nature & Environment",
            iconRes = Iconsax.Teacher,
            startColor = PlayPurpleStart,
            endColor = PlayPurpleEnd,
            description = "Ocean, rivers, mountains, soil & plants",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Family & People",
            iconRes = Iconsax.People,
            startColor = PlayPurpleStart,
            endColor = PlayPurpleEnd,
            description = "Parents, siblings, children & community",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Colors & Shapes",
            iconRes = Iconsax.Element4Bold,
            startColor = PlayPinkStart,
            endColor = PlayPinkEnd,
            description = "Black, white, red, round, sharp & flat",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Occupations & Tools",
            iconRes = Iconsax.SettingBold,
            startColor = PlayGoldStart,
            endColor = PlayGoldEnd,
            description = "Adze, grater, arrow, farming & crafts",
            bentoSpan = BentoSpan.SMALL_1X1
        )
    )

    fun getMeta(categoryName: String): CategoryMetaData {
        return categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            ?: CategoryMetaData(
                name = categoryName,
                iconRes = Iconsax.BookBold,
                startColor = PlayPurpleStart,
                endColor = PlayPurpleEnd,
                description = "Kasiguranin vocabulary",
                bentoSpan = BentoSpan.SMALL_1X1
            )
    }
}
