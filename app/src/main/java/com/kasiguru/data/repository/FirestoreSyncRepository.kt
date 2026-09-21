package com.kasiguru.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentChange
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

    companion object {
        private const val TAG = "FirestoreSyncRepository"
    }

    /**
     * Listens to real-time changes in Firestore "vocabulary" collection
     * and automatically syncs changes into the local SQLite Room Database.
     *
     * Uses [DocumentChange] instead of the full snapshot so that:
     *  - Only actually changed documents are upserted (no re-inserting the whole collection).
     *  - Deleted documents (`REMOVED`) are removed from the local database immediately.
     */
    fun startRealtimeSync() {
        vocabCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Realtime sync error", error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener

            val changes = snapshot.documentChanges
            if (changes.isEmpty()) return@addSnapshotListener

            // Batch all changes into one coroutine to avoid interleaving with parallel sync.
            scope.launch {
                try {
                    for (change in changes) {
                        val data = change.document.data
                        val word = (data["kasiguranin"] ?: data["kasiguranin_word"] ?: "").toString().trim()
                        if (word.isEmpty()) continue
                        val english = (data["english"] ?: data["english_translation"] ?: "").toString().trim()
                        val cleanEnglish = if (english == "nan") "" else english

                        when (change.type) {
                            DocumentChange.Type.REMOVED -> {
                                vocabularyDao.deleteBySense(word, cleanEnglish)
                                Log.d(TAG, "Removed: $word ($cleanEnglish)")
                            }

                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val tagalog = (data["tagalog"] ?: data["filipino_translation"] ?: "").toString().trim()
                                val rootForm = (data["rootForm"] ?: data["root_word"] ?: word).toString().trim()
                                val category = (data["category"] ?: "General").toString().trim()
                                val ipaNotation = (data["ipaNotation"] ?: data["ipa"] ?: "").toString().trim()
                                val exampleSentence = (data["sampleSentence"] ?: data["sample_sentence"] ?: "").toString().trim()
                                val partOfSpeech = (data["partOfSpeech"] ?: data["part_of_speech"] ?: "").toString().trim()
                                val meaningEnglish = (data["meaningEnglish"] ?: "").toString().trim()
                                val meaningTagalog = (data["meaningTagalog"] ?: "").toString().trim()
                                val theme = (data["theme"] ?: "").toString().trim()

                                val existing = vocabularyDao.getVocabularyBySense(word, cleanEnglish)
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
                            }
                        }
                    }

                    // Clean up any legacy duplicates left by earlier versions of this listener.
                    vocabularyDao.deleteDuplicateWords()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing realtime changes", e)
                }
            }
        }
    }
}

