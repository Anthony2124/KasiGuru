package com.kasiguru.ui.theme

import androidx.annotation.DrawableRes
import com.kasiguru.R

/**
 * Adrian's seven illustrated Casiguran places, 9:16, exported at 810 × 1439 WebP.
 *
 * They replace the six Wikimedia photos the app used to show (and with them the CC BY-SA credit
 * lines those photos required). Scenery appears where the learner *arrives* somewhere: a path
 * section, a story, a game board, a profile. Everyday screens stay flat night.
 */
enum class Scenery(@DrawableRes val res: Int, val place: String) {
    Casapsapan(R.drawable.scene_casapsapan, "Casapsapan Beach at sunset"),
    ErmitaHill(R.drawable.scene_ermita_hill, "The steps up Ermita Hill"),
    Farm(R.drawable.scene_farm, "A farm in Casiguran"),
    Forest(R.drawable.scene_forest, "The forest"),
    OntokLighthouse(R.drawable.scene_ontok_lighthouse, "Ontok Lighthouse"),
    River(R.drawable.scene_river, "The river"),
    TibuTidalPool(R.drawable.scene_tibu_tidal_pool, "Tibu Tidal Pool");

    companion object {
        /**
         * One place per learning-path section (ids from `LearningTree`). Thirteen sections share seven
         * places by the nearest fit; to give a section its own, add a scene and point its entry at it.
         */
        private val bySection: Map<String, Scenery> = mapOf(
            "pagbati" to Casapsapan,
            "pamilya" to Farm,
            "tahanan" to Farm,
            "pagkain" to Farm,
            "paglalakbay" to OntokLighthouse,
            "katawan" to River,
            "kalikasan" to Forest,
            "hayop" to TibuTidalPool,
            "bilang" to ErmitaHill,
            "kabuhayan" to Casapsapan,
            "kilos" to River,
            "paglalarawan" to TibuTidalPool,
            "araw_araw" to Forest
        )

        fun forSection(sectionId: String): Scenery = bySection[sectionId] ?: Forest

        /** Dictionary categories (display names from the corpus), matched to the closest place. */
        private val byCategory: Map<String, Scenery> = mapOf(
            "Animals & Wildlife" to TibuTidalPool,
            "Nature & Environment" to Forest,
            "Body Parts & Health" to River,
            "Food & Dining" to Farm,
            "House & Daily Life" to Farm,
            "Greetings & Essentials" to Casapsapan,
            "Family & People" to Farm,
            "Occupations & Tools" to Casapsapan,
            "Numbers & Time" to ErmitaHill,
            "Emotions & Feelings" to River,
            "Colors & Shapes" to TibuTidalPool,
            "Weather & Climate" to OntokLighthouse
        )

        fun forCategory(category: String): Scenery = byCategory[category] ?: Forest

        /** For things without a category (game levels, stories): each one shows the next place in turn. */
        fun forIndex(index: Int): Scenery = entries[Math.floorMod(index - 1, entries.size)]
    }
}
