package com.kasiguru.ui.theme

import androidx.compose.ui.graphics.Color
import com.kasiguru.R

/**
 * [CategoryRegistry] is a plain data object built once at class-load time, outside composition — so
 * its gradients cannot reference the theme-reactive [Violet]/[VioletDeep] tokens. That's the correct
 * call anyway: category identity is a fixed brand choice, the same discipline [Gold]/[Coral] already
 * follow (see their own doc comments in `Color.kt`). These are that same light-mode violet, frozen.
 */
private val FixedViolet = Color(0xFF5B4CDB)
private val FixedVioletDeep = Color(0xFF4034A8)

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
) {
    /**
     * Gold and Coral are fills that carry [Ink], never a foreground — measured 1.83 and 2.31 on
     * white, and the same figure holds in reverse for white text on the fill. Violet clears 6.00
     * either direction, so it is the only one of the three that can carry white. See DESIGN.md.
     */
    val onGradientIsInk: Boolean get() = startColor == Gold || startColor == Coral
}

/** Three category gradients, cycled: Violet, Gold, Coral — the app's only three expressive hues. */
object CategoryRegistry {
    val categories = listOf(
        CategoryMetaData(
            name = "Greetings & Essentials",
            iconRes = Iconsax.BookBold,
            startColor = FixedViolet,
            endColor = FixedVioletDeep,
            description = "Hellos, politeness, questions & basic phrases",
            bentoSpan = BentoSpan.HERO_2X2
        ),
        CategoryMetaData(
            name = "Food & Dining",
            iconRes = Iconsax.VolumeHighBold,
            startColor = Gold,
            endColor = GoldDeep,
            description = "Rice, fruits, dishes, drinks & cooking",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Animals & Wildlife",
            iconRes = Iconsax.FlashBold,
            startColor = Coral,
            endColor = CoralDeep,
            description = "Carabao, birds, dogs, fish & forest life",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Body Parts & Health",
            iconRes = Iconsax.ProfileBold,
            startColor = FixedViolet,
            endColor = FixedVioletDeep,
            description = "Anatomy, head, limbs, face & senses",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Numbers & Time",
            iconRes = Iconsax.Calendar,
            startColor = Gold,
            endColor = GoldDeep,
            description = "Counting 1-10, days, times of day & seasons",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Weather & Climate",
            iconRes = Iconsax.Global,
            startColor = Coral,
            endColor = CoralDeep,
            description = "Rain, wind, sun, clouds & temperature",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Emotions & Feelings",
            iconRes = Iconsax.StarBold,
            startColor = Coral,
            endColor = CoralDeep,
            description = "Happy, angry, sad, afraid & love",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "House & Daily Life",
            iconRes = Iconsax.HomeBold,
            startColor = Gold,
            endColor = GoldDeep,
            description = "Home objects, clothing, tools & routines",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Nature & Environment",
            iconRes = Iconsax.Teacher,
            startColor = FixedViolet,
            endColor = FixedVioletDeep,
            description = "Ocean, rivers, mountains, soil & plants",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Family & People",
            iconRes = Iconsax.People,
            startColor = FixedViolet,
            endColor = FixedVioletDeep,
            description = "Parents, siblings, children & community",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Colors & Shapes",
            iconRes = Iconsax.Element4Bold,
            startColor = Coral,
            endColor = CoralDeep,
            description = "Black, white, red, round, sharp & flat",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Occupations & Tools",
            iconRes = Iconsax.SettingBold,
            startColor = Gold,
            endColor = GoldDeep,
            description = "Adze, grater, arrow, farming & crafts",
            bentoSpan = BentoSpan.SMALL_1X1
        )
    )

    fun getMeta(categoryName: String): CategoryMetaData {
        return categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            ?: CategoryMetaData(
                name = categoryName,
                iconRes = Iconsax.BookBold,
                startColor = FixedViolet,
                endColor = FixedVioletDeep,
                description = "Kasiguranin vocabulary",
                bentoSpan = BentoSpan.SMALL_1X1
            )
    }
}
