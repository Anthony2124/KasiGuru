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

object CategoryRegistry {
    val categories = listOf(
        CategoryMetaData(
            name = "Greetings & Essentials",
            iconRes = Iconsax.BookBold,
            startColor = HeroCardStart,
            endColor = HeroCardEnd,
            description = "Hellos, politeness, questions & basic phrases",
            bentoSpan = BentoSpan.HERO_2X2
        ),
        CategoryMetaData(
            name = "Food & Dining",
            iconRes = Iconsax.VolumeHighBold,
            startColor = VocabCardStart,
            endColor = VocabCardEnd,
            description = "Rice, fruits, dishes, drinks & cooking",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Animals & Wildlife",
            iconRes = Iconsax.FlashBold,
            startColor = MiniGamesCardStart,
            endColor = MiniGamesCardEnd,
            description = "Carabao, birds, dogs, fish & forest life",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Body Parts & Health",
            iconRes = Iconsax.ProfileBold,
            startColor = QuestsCardStart,
            endColor = QuestsCardEnd,
            description = "Anatomy, head, limbs, face & senses",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Numbers & Time",
            iconRes = Iconsax.Calendar,
            startColor = StoriesCardStart,
            endColor = StoriesCardEnd,
            description = "Counting 1-10, days, times of day & seasons",
            bentoSpan = BentoSpan.MEDIUM_2X1
        ),
        CategoryMetaData(
            name = "Weather & Climate",
            iconRes = Iconsax.Global,
            startColor = HeroCardStart,
            endColor = QuestsCardEnd,
            description = "Rain, wind, sun, clouds & temperature",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Emotions & Feelings",
            iconRes = Iconsax.StarBold,
            startColor = MiniGamesCardStart,
            endColor = VocabCardStart,
            description = "Happy, angry, sad, afraid & love",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "House & Daily Life",
            iconRes = Iconsax.HomeBold,
            startColor = VocabCardStart,
            endColor = HeroCardStart,
            description = "Home objects, clothing, tools & routines",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Nature & Environment",
            iconRes = Iconsax.Teacher,
            startColor = QuestsCardStart,
            endColor = StoriesCardEnd,
            description = "Ocean, rivers, mountains, soil & plants",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Family & People",
            iconRes = Iconsax.People,
            startColor = StoriesCardStart,
            endColor = MiniGamesCardStart,
            description = "Parents, siblings, children & community",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Colors & Shapes",
            iconRes = Iconsax.Element4Bold,
            startColor = HeroCardStart,
            endColor = MiniGamesCardEnd,
            description = "Black, white, red, round, sharp & flat",
            bentoSpan = BentoSpan.SMALL_1X1
        ),
        CategoryMetaData(
            name = "Occupations & Tools",
            iconRes = Iconsax.SettingBold,
            startColor = VocabCardEnd,
            endColor = QuestsCardStart,
            description = "Adze, grater, arrow, farming & crafts",
            bentoSpan = BentoSpan.SMALL_1X1
        )
    )

    fun getMeta(categoryName: String): CategoryMetaData {
        return categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            ?: CategoryMetaData(
                name = categoryName,
                iconRes = Iconsax.BookBold,
                startColor = HeroCardStart,
                endColor = HeroCardEnd,
                description = "Kasiguranin vocabulary",
                bentoSpan = BentoSpan.SMALL_1X1
            )
    }
}
