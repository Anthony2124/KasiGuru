package com.kasiguru.domain.lesson

import com.kasiguru.data.local.entity.VocabularyEntity
import java.time.LocalDate

/**
 * The word of the day: seeded by the epoch day, so it is the same for everyone on a given day and
 * moves on the next, over a stable id-sorted list.
 *
 * Home, the Library and the morning notification each pick it here. Home and the Library used to
 * filter the list differently, so on some days they featured two different words.
 */
object WordOfDay {

    /** A word needs its Kasiguranin form and a gloss to be shown on its own. */
    fun eligible(words: List<VocabularyEntity>): List<VocabularyEntity> =
        words.filter { it.kasiguranin.isNotBlank() && it.tagalog.isNotBlank() }.sortedBy { it.id }

    fun pick(words: List<VocabularyEntity>, date: LocalDate = LocalDate.now()): VocabularyEntity? {
        val list = eligible(words)
        if (list.isEmpty()) return null
        return list[Math.floorMod(date.toEpochDay(), list.size.toLong()).toInt()]
    }
}
