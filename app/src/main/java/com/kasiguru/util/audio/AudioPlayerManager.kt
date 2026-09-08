package com.kasiguru.util.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.remote.WordAudioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the pronunciation recording an admin uploaded for a word.
 *
 * There is no synthetic-voice fallback. Text-to-speech was removed once the corpus began carrying
 * real native-speaker recordings: a machine reading of a Kasiguranin headword is not a pronunciation
 * model, and offering one blurred the line between recorded data and generated data that the project
 * is careful to keep. A word with no recording simply has no audio — the caller is expected not to
 * offer an audio control for it (see [VocabularyEntity.audioFileName]).
 */
@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null

    // Built directly rather than injected: every screen constructs this class with
    // `remember { AudioPlayerManager(context) }` rather than through Hilt, so there is no graph to
    // inject the repository from. It needs only a Context and the Firestore singleton.
    private val wordAudioRepository by lazy {
        WordAudioRepository(context, FirebaseFirestore.getInstance())
    }
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var resolveJob: Job? = null

    /**
     * Plays [word]'s uploaded recording, fetching and caching it on first use. Does nothing if the
     * word has no recording, or if the device is offline with nothing cached.
     */
    fun playWord(word: VocabularyEntity) {
        stopAudio()
        if (word.audioFileName.isBlank()) return
        resolveJob = scope.launch {
            val file = try {
                wordAudioRepository.audioFor(word.audioFileName, word.audioUpdatedAt)
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Audio lookup failed for ${word.kasiguranin}", e)
                null
            }
            if (file != null) playFile(file)
        }
    }

    /**
     * Plays a clip bundled in `res/raw` (by [audioFileName], or a slug of [textToSpeak]), or nothing.
     * Kept for story-page narration and other non-vocabulary callers. No synthetic-voice fallback.
     */
    fun playAudio(textToSpeak: String, audioFileName: String = "") {
        stopAudio()
        val resName = audioFileName.ifEmpty { textToSpeak.lowercase().replace(Regex("[^a-z0-9_]"), "") }
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) return
        try {
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing raw resource $resName", e)
        }
    }

    private fun playFile(file: File) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.path)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing clip ${file.name}", e)
        }
    }

    fun stopAudio() {
        resolveJob?.cancel()
        resolveJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping audio", e)
        }
    }
}
