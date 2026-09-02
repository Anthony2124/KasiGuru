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
            theme = cloud.theme.ifBlank { local.theme },
            partOfSpeech = cloud.partOfSpeech.ifBlank { local.partOfSpeech },
            meaningEnglish = cloud.meaningEnglish.ifBlank { local.meaningEnglish },
            meaningTagalog = cloud.meaningTagalog.ifBlank { local.meaningTagalog },
            audioFileName = cloud.audioFileName.ifBlank { local.audioFileName },
            exampleSentence = cloud.exampleSentence.ifBlank { local.exampleSentence },
            exampleTranslation = cloud.exampleTranslation.ifBlank { local.exampleTranslation },
            exampleSentence2 = cloud.exampleSentence2.ifBlank { local.exampleSentence2 },
            exampleTranslation2 = cloud.exampleTranslation2.ifBlank { local.exampleTranslation2 },
            ipaNotation = cloud.ipaNotation.ifBlank { local.ipaNotation }
        )
    }

    /**
     * [cloud] with the learner's own history preserved, or the cloud row when this word is new.
     *
     * Fields the admin word form has no control for -- the root form, the audio file name and the
     * two phonetic flags -- fall back to the local value rather than being overwritten. The portal
     * cannot write those keys, so a document it saves simply omits them, and copying the cloud value
     * unconditionally meant that *editing any word in the portal* erased its seeded root form and
     * its audio reference on the next full reconcile: the Root row vanished from the detail screen
     * and audio playback fell back to a slugified headword. A field nothing can author is a field
     * nothing should be able to clear.
     */
    fun merge(local: VocabularyEntity?, cloud: VocabularyEntity): VocabularyEntity {
        if (local == null) return cloud.copy(id = 0)

        return local.copy(
            kasiguranin = cloud.kasiguranin,
            tagalog = cloud.tagalog,
            english = cloud.english,
            rootForm = cloud.rootForm.ifBlank { local.rootForm },
            neutralForm = cloud.neutralForm,
            imperfectiveForm = cloud.imperfectiveForm,
            perfectiveForm = cloud.perfectiveForm,
            contemplativeForm = cloud.contemplativeForm,
            category = cloud.category,
            // Guarded, not copied, for the same reason as the meanings below: every vocabulary
            // document written before the theme field existed omits it, and so does the word-
            // submission approval path, so an unguarded copy would strip a learning-tree tag off
            // every word the next time anything reconciled. Re-tagging still propagates, because
            // that writes a non-blank value; only clearing a tag has to be done in the portal and
            // does not travel. Give every writer a theme key and this can become a plain copy.
            theme = cloud.theme.ifBlank { local.theme },
            partOfSpeech = cloud.partOfSpeech,
            // Guarded rather than copied, because the seeder ships 394 definitions and Firestore
            // carries none until backfill_meanings.js has run: an unguarded copy would wipe every
            // definition off every device on the first reconcile after this version installs. The
            // cost is that clearing a meaning in the portal does not propagate as a clear.
            meaningEnglish = cloud.meaningEnglish.ifBlank { local.meaningEnglish },
            meaningTagalog = cloud.meaningTagalog.ifBlank { local.meaningTagalog },
            audioFileName = cloud.audioFileName.ifBlank { local.audioFileName },
            exampleSentence = cloud.exampleSentence,
            exampleTranslation = cloud.exampleTranslation,
            exampleSentence2 = cloud.exampleSentence2,
            exampleTranslation2 = cloud.exampleTranslation2,
            // No admin control writes these, so `false` from the cloud means "not carried", never
            // "turned off". Give the portal a checkbox for them and this has to become a copy.
            phoneticGlottal = cloud.phoneticGlottal || local.phoneticGlottal,
            phoneticVowelLength = cloud.phoneticVowelLength || local.phoneticVowelLength,
            ipaNotation = cloud.ipaNotation.ifBlank { local.ipaNotation }
        )
    }
}
