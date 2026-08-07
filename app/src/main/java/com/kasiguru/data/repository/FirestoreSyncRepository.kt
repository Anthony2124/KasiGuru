package com.kasiguru.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.dao.ConjugationDao
import com.kasiguru.data.local.dao.VocabularyDao
import com.kasiguru.data.local.entity.ConjugationEntity
import com.kasiguru.data.local.entity.VocabularyEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val vocabularyDao: VocabularyDao,
    private val conjugationDao: ConjugationDao
) {
    private val vocabCollection = firestore.collection("vocabulary")

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

                val vocabEntity = VocabularyEntity(
                    kasiguranin = word,
                    tagalog = if (tagalog == "nan") "" else tagalog,
                    english = if (english == "nan") "" else english,
                    rootForm = if (rootForm == "nan") word else rootForm,
                    category = if (category == "nan") "General" else category,
                    ipaNotation = if (ipaNotation == "nan") "" else ipaNotation,
                    exampleSentence = if (exampleSentence == "nan") "" else exampleSentence
                )

                // Async Room insert/upsert
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val vocabId = vocabularyDao.insertVocabulary(vocabEntity).toInt()

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
