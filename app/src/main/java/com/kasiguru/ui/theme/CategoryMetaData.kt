package com.kasiguru.ui.theme

import androidx.compose.ui.graphics.Color
import com.kasiguru.R

/**
 * [CategoryRegistry] is a plain data object built once at class-load time, outside composition — so
 * its gradients cannot reference the theme-reactive [Lime]/[LimeLip] tokens. That's the correct
 * call anyway: category identity is a fixed brand choice, the same discipline [Gold]/[Coral] already
 * follow (see their own doc comments in `Color.kt`). These are that same light-mode violet, frozen.
 */
private val FixedForest = Color(0xFF5B4CDB)
private val FixedForestDeep = Color(0xFF4034A8)

enum class BentoSpan {
    HERO_2X2,
    MEDIUM_2X1,
    SMALL_1X1
}

data class CategoryMetaData(
    val name: String,
    val iconRes: Int,
    /** The category's illustrated icon, from design/assets/categories/ via scripts/generate-category-icons.py. */
    val customDrawableRes: Int? = null,
    val startColor: Color,
    val endColor: Color,
    val description: String,
    val bentoSpan: BentoSpan
) {
    /**
     * Gold and Coral are bright fills that carry [RewardInk]; white on them fails. Olive is the only
     * one of the three that carries white. See DESIGN.md.
     */
    val onGradientIsInk: Boolean get() = startColor == Gold || startColor == Coral
}

/** Three category gradients, cycled: olive, gold, coral. */
object CategoryRegistry {
    val categories = listOf(
        CategoryMetaData(
            name = "Greetings & Essentials",
            iconRes = Iconsax.BookBold,
            customDrawableRes = R.drawable.category_greetings,
            startColor = FixedForest,
            endColor = FixedForestDeep,
            description = "Hellos, politeness, questions & basic phrases",
            bentoSpan = BentoSpan.HERO_2X2
        ),
        CategoryMetaData(
            name = "Food & Dining",
            iconRes = Iconsax.VolumeHighBold,
            customDrawableRes = R.drawable.category_food,
            startColor = Gold,
            endColor = GoldDeep,
            description = "Rice, fruits, dishes, drinks & cooking",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Animals & Wildlife",
            iconRes = Iconsax.FlashBold,
            customDrawableRes = R.drawable.category_animals,
            startColor = Coral,
            endColor = CoralDeep,
            description = "Carabao, birds, dogs, fish & forest life",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Body Parts & Health",
            iconRes = Iconsax.ProfileBold,
            customDrawableRes = R.drawable.category_health,
            startColor = FixedForest,
            endColor = FixedForestDeep,
            description = "Anatomy, head, limbs, face & senses",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Numbers & Time",
            iconRes = Iconsax.Calendar,
            customDrawableRes = R.drawable.category_numbers,
            startColor = Gold,
            endColor = GoldDeep,
            description = "Counting 1-10, days, times of day & seasons",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Weather & Climate",
            iconRes = Iconsax.Global,
            customDrawableRes = R.drawable.category_weather,
            startColor = Coral,
            endColor = CoralDeep,
            description = "Rain, wind, sun, clouds & temperature",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Emotions & Feelings",
            iconRes = Iconsax.StarBold,
            customDrawableRes = R.drawable.category_emotions,
            startColor = Coral,
            endColor = CoralDeep,
            description = "Happy, angry, sad, afraid & love",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "House & Daily Life",
            iconRes = Iconsax.HomeBold,
            customDrawableRes = R.drawable.category_house,
            startColor = Gold,
            endColor = GoldDeep,
            description = "Home objects, clothing, tools & routines",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Nature & Environment",
            iconRes = Iconsax.Teacher,
            customDrawableRes = R.drawable.category_nature,
            startColor = FixedForest,
            endColor = FixedForestDeep,
            description = "Ocean, rivers, mountains, soil & plants",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Family & People",
            iconRes = Iconsax.People,
            customDrawableRes = R.drawable.category_family,
            startColor = FixedForest,
            endColor = FixedForestDeep,
            description = "Parents, siblings, children & community",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Colors & Shapes",
            iconRes = Iconsax.Element4Bold,
            customDrawableRes = R.drawable.category_colors,
            startColor = Coral,
            endColor = CoralDeep,
            description = "Black, white, red, round, sharp & flat",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Occupations & Tools",
            iconRes = Iconsax.SettingBold,
            customDrawableRes = R.drawable.category_occupations,
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
                customDrawableRes = R.drawable.category_general,
                startColor = FixedForest,
                endColor = FixedForestDeep,
                description = "Kasiguranin vocabulary",
                bentoSpan = BentoSpan.SMALL_1X1
            )
    }
}
