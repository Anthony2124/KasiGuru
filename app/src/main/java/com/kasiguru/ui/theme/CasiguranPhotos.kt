package com.kasiguru.ui.theme

import androidx.annotation.DrawableRes
import com.kasiguru.R

/**
 * A photograph of Casiguran, Aurora, and the credit its licence requires wherever it is shown.
 *
 * Every photo here is from Wikimedia Commons and was checked to be of Casiguran, *Aurora* - Commons
 * also holds many of Casiguran, Sorsogon, a different town, which must not slip in. CC BY-SA photos
 * need the author and licence displayed next to them; [credit] is that line.
 */
data class CasiguranPhoto(
    @DrawableRes val imageRes: Int,
    /** What the photo shows, for TalkBack and for the credit line. */
    val place: String,
    val credit: String
)

object CasiguranPhotos {
    private val reef = CasiguranPhoto(
        R.drawable.img_casiguran_reef,
        "Coral reef in a Casiguran marine protected area",
        "AndreaDaluhay · CC BY-SA 4.0 · Wikimedia Commons"
    )
    private val dalugan = CasiguranPhoto(
        R.drawable.img_casiguran_dalugan,
        "Dalugan Beach, Casiguran",
        "Kyendc · CC BY-SA 4.0 · Wikimedia Commons"
    )
    private val casapsapan = CasiguranPhoto(
        R.drawable.img_casiguran_casapsapan,
        "Casapsapan Beach, Casiguran",
        "Kyendc · CC BY-SA 4.0 · Wikimedia Commons"
    )
    private val bancas = CasiguranPhoto(
        R.drawable.img_casiguran_bancas,
        "Bancas at Cuaresma Beach, Casiguran",
        "Kyendc · CC BY-SA 4.0 · Wikimedia Commons"
    )
    private val sunset = CasiguranPhoto(
        R.drawable.img_casiguran_sunset,
        "Sunset at Cuaresma Beach, Casiguran",
        "Kyendc · CC BY-SA 4.0 · Wikimedia Commons"
    )
    private val peninsula = CasiguranPhoto(
        R.drawable.img_casiguran_peninsula,
        "San Ildefonso Peninsula and Casiguran Sound from space",
        "NASA · Public domain · Wikimedia Commons"
    )

    /**
     * Only six free photos of Casiguran, Aurora exist on Commons, so categories share them by the
     * nearest fit. To give a category its own picture, add a drawable and point its entry at it.
     */
    private val byCategory: Map<String, CasiguranPhoto> = mapOf(
        "Animals & Wildlife" to reef,
        "Nature & Environment" to dalugan,
        "Body Parts & Health" to dalugan,
        "Food & Dining" to casapsapan,
        "House & Daily Life" to casapsapan,
        "Greetings & Essentials" to bancas,
        "Family & People" to bancas,
        "Occupations & Tools" to bancas,
        "Numbers & Time" to sunset,
        "Emotions & Feelings" to sunset,
        "Colors & Shapes" to sunset,
        "Weather & Climate" to peninsula
    )

    fun forCategory(category: String): CasiguranPhoto = byCategory[category] ?: casapsapan

    private val all = listOf(casapsapan, bancas, reef, sunset, dalugan, peninsula)

    /** For games without categories: each level shows the next place in turn. */
    fun forLevel(level: Int): CasiguranPhoto = all[Math.floorMod(level - 1, all.size)]
}
