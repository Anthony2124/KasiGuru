package com.kasiguru.data.remote

import com.kasiguru.data.local.entity.VocabularyEntity

/**
 * Folds an admin-edited word from the cloud into the copy on this device.
 *
 * The split is by ownership: the admin portal owns what the word *is* — its spelling, glosses, verb
 * forms, category, examples, phonetics — and the device owns what the learner has *done* with it,
 * which exists nowhere else and cannot be recovered if a sync overwrites it.
 *
 * Written as "start from the local row, apply the content fields" rather than the other way round on
 * purpose. The previous version rebuilt the row from the cloud and copied five learning fields back
 * by name, which meant every field added to the entity afterwards was silently dropped on the next
 * content sync: `lapses` and `relearningStep` were reset to zero the moment they shipped, and
 * `exampleSentence2` — which the admin form has always written — was wiped rather than delivered.
 * With the direction inverted, a new learning field is preserved by default, and a new content field
 * has to be named here, next to the parser that reads it.
 */
object VocabularyContentMerge {

    /**
     * As [merge], but for a source that carries only some of a word.
     *
     * The realtime listener in FirestoreSyncRepository reads a handful of fields under legacy names
     * and knows nothing about the aspect forms, the second example or the phonetic flags. Merging it
     * like a complete record blanked every field it had never heard of -- on every launch, for every
     * word in the collection. A blank from a partial source means "not carried", not "cleared".
     */
    fun mergeNonBlank(local: VocabularyEntity?, cloud: VocabularyEntity): VocabularyEntity {
        if (local == null) return cloud.copy(id = 0)

        return local.copy(
            kasiguranin = cloud.kasiguranin.ifBlank { local.kasiguranin },
            tagalog = cloud.tagalog.ifBlank { local.tagalog },
            english = cloud.english.ifBlank { local.english },
            rootForm = cloud.rootForm.ifBlank { local.rootForm },
            neutralForm = cloud.neutralForm.ifBlank { local.neutralForm },
            imperfectiveForm = cloud.imperfectiveForm.ifBlank { local.imperfectiveForm },
            perfectiveForm = cloud.perfectiveForm.ifBlank { local.perfectiveForm },
            contemplativeForm = cloud.contemplativeForm.ifBlank { local.contemplativeForm },
            category = cloud.category.ifBlank { local.category },
            audioFileName = cloud.audioFileName.ifBlank { local.audioFileName },
            exampleSentence = cloud.exampleSentence.ifBlank { local.exampleSentence },
            exampleTranslation = cloud.exampleTranslation.ifBlank { local.exampleTranslation },
            exampleSentence2 = cloud.exampleSentence2.ifBlank { local.exampleSentence2 },
            exampleTranslation2 = cloud.exampleTranslation2.ifBlank { local.exampleTranslation2 },
            ipaNotation = cloud.ipaNotation.ifBlank { local.ipaNotation }
        )
    }

    /** [cloud] with the learner's own history preserved, or the cloud row when this word is new. */
    fun merge(local: VocabularyEntity?, cloud: VocabularyEntity): VocabularyEntity {
        if (local == null) return cloud.copy(id = 0)

        return local.copy(
            kasiguranin = cloud.kasiguranin,
            tagalog = cloud.tagalog,
            english = cloud.english,
            rootForm = cloud.rootForm,
            neutralForm = cloud.neutralForm,
            imperfectiveForm = cloud.imperfectiveForm,
            perfectiveForm = cloud.perfectiveForm,
            contemplativeForm = cloud.contemplativeForm,
            category = cloud.category,
            audioFileName = cloud.audioFileName,
            exampleSentence = cloud.exampleSentence,
            exampleTranslation = cloud.exampleTranslation,
            exampleSentence2 = cloud.exampleSentence2,
            exampleTranslation2 = cloud.exampleTranslation2,
            phoneticGlottal = cloud.phoneticGlottal,
            phoneticVowelLength = cloud.phoneticVowelLength,
            ipaNotation = cloud.ipaNotation
        )
    }
}
