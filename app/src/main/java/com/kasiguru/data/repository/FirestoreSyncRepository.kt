package com.kasiguru.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.dao.ConjugationDao
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.ConjugationEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.remote.VocabularyContentMerge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val vocabularyDao: VocabularyDao,
    private val conjugationDao: ConjugationDao
) {
    private val vocabCollection = firestore.collection("vocabulary")
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Listens to real-time changes in Firestore "vocabulary" collection
     * and automatically syncs changes into the local SQLite Room Database.
     */
    fun startRealtimeSync() {
        vocabCollection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val word = (data["kasiguranin"] ?: data["kasiguranin_word"] ?: "").toString().trim()
                if (word.isEmpty()) continue

                val tagalog = (data["tagalog"] ?: data["filipino_translation"] ?: "").toString().trim()
                val english = (data["english"] ?: data["english_translation"] ?: "").toString().trim()
                val rootForm = (data["rootForm"] ?: data["root_word"] ?: word).toString().trim()
                val category = (data["category"] ?: "General").toString().trim()
                val ipaNotation = (data["ipaNotation"] ?: data["ipa"] ?: "").toString().trim()
                val exampleSentence = (data["sampleSentence"] ?: data["sample_sentence"] ?: "").toString().trim()
                val partOfSpeech = (data["partOfSpeech"] ?: data["part_of_speech"] ?: "").toString().trim()
                val meaningEnglish = (data["meaningEnglish"] ?: "").toString().trim()
                val meaningTagalog = (data["meaningTagalog"] ?: "").toString().trim()
                // Which learning-tree section claims the word. Set in the admin portal and absent
                // from every document written before it existed, so blank is the normal case.
                val theme = (data["theme"] ?: "").toString().trim()

                // Async Room upsert, matched on the sense rather than the headword: keyed on the
                // headword alone this merged the cloud's `lima` (five) onto the device's `lima`
                // (hand), losing one sense of every homonym in the corpus.
                scope.launch {
                    try {
                        val cleanEnglish = if (english == "nan") "" else english
                        val existing = vocabularyDao.getVocabularyBySense(word, cleanEnglish)
                        // This listener reads six fields under legacy names and knows nothing about
                        // the aspect forms, the second example, the phonetic flags or the SM-2
                        // ladder. Building a whole entity from it therefore wrote a default over
                        // every one of those, on every launch -- how a word seeded with six lapses
                        // came back with zero. Only what the document actually carries is applied.
                        val fromDoc = VocabularyEntity(
                            kasiguranin = word,
                            tagalog = if (tagalog == "nan") "" else tagalog,
                            english = cleanEnglish,
                            rootForm = if (rootForm == "nan") word else rootForm,
                            category = if (category == "nan") "General" else category,
                            ipaNotation = if (ipaNotation == "nan") "" else ipaNotation,
                            exampleSentence = if (exampleSentence == "nan") "" else exampleSentence,
                            partOfSpeech = if (partOfSpeech == "nan") "" else partOfSpeech,
                            meaningEnglish = if (meaningEnglish == "nan") "" else meaningEnglish,
                            meaningTagalog = if (meaningTagalog == "nan") "" else meaningTagalog,
                            theme = if (theme == "nan") "" else theme
                        )
                        val vocabEntity = VocabularyContentMerge.mergeNonBlank(existing, fromDoc)

                        vocabularyDao.insert(vocabEntity)
                        val fetchedWord = vocabularyDao.getVocabularyBySense(word, cleanEnglish)
                        val vocabId = fetchedWord?.id ?: vocabEntity.id

                        // Parse nested conjugations array if present
                        val conjugationsRaw = data["conjugations"] as? List<Map<String, Any>>
                        if (conjugationsRaw != null) {
                            val conjugationEntities = conjugationsRaw.mapNotNull { cMap ->
                                val form = (cMap["conjugatedForm"] ?: cMap["conjugated_form"] ?: "").toString().trim()
                                val tense = (cMap["tense"] ?: "perfective").toString().trim()
                                val affix = cMap["affixType"]?.toString()?.trim()

                                if (form.isNotEmpty()) {
                                    ConjugationEntity(
                                        vocabularyId = vocabId,
                                        conjugatedForm = form,
                                        tense = tense,
                                        affixType = if (affix == "nan") null else affix
                                    )
                                } else null
                            }

                            if (conjugationEntities.isNotEmpty()) {
                                conjugationDao.deleteConjugationsForWord(vocabId)
                                conjugationDao.insertAll(conjugationEntities)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
