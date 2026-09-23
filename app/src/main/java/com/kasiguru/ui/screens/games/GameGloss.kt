package com.kasiguru.ui.screens.games

import com.kasiguru.data.local.entity.VocabularyEntity

/**
 * The meaning shown beside a Kasiguranin word in the word games: its Tagalog translation, which is
 * the learners' own language. English only fills in where the dictionary has no Tagalog. Blank when
 * the translation is spelled the same as the Kasiguranin (`manok` / `manok`), since repeating the
 * word teaches nothing.
 */
internal fun tagalogGloss(entry: VocabularyEntity): String {
    val gloss = entry.tagalog.ifBlank { entry.english }.trim()
    return if (comparable(gloss) == comparable(entry.kasiguranin)) "" else gloss
}

/** Case, accents, the schwa `ë` and hyphens ignored, so `singët` and `singet` count as one spelling. */
private fun comparable(text: String): String =
    java.text.Normalizer.normalize(text.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace("-", "")
