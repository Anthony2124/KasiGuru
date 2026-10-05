package com.kasiguru.util.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.data.remote.WordAudioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val speech = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    // Transient focus that may duck: background music lowers while a recording plays.
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(speech)
        .build()

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
        val key = word.audioFileName.ifBlank {
            val slugK = word.kasiguranin.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            val slugE = word.english.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            if (slugK.isNotBlank() && slugE.isNotBlank()) "${slugK}__${slugE}" else slugK
        }
        if (key.isBlank()) return
        resolveJob = scope.launch {
            val file = try {
                wordAudioRepository.audioFor(key, word.audioUpdatedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Audio lookup failed for ${word.kasiguranin}", e)
                tell("Couldn't load the recording. Check your connection and try again.")
                return@launch
            }
            if (file != null) {
                playFile(file)
            } else if (!playAudio(word.kasiguranin, word.audioFileName)) {
                tell("No recording for this word yet.")
            }
        }
    }

    // A silent speaker button reads as a broken phone; say what happened instead.
    private fun tell(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Plays a clip bundled in `res/raw` (by [audioFileName], or a slug of [textToSpeak]), or nothing.
     * Kept for story-page narration and other non-vocabulary callers. No synthetic-voice fallback.
     *
     * @return whether a bundled clip was found and started.
     */
    fun playAudio(textToSpeak: String, audioFileName: String = ""): Boolean {
        stopAudio()
        val resName = audioFileName.ifEmpty { textToSpeak.lowercase().replace(Regex("[^a-z0-9_]"), "") }
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) return false
        return try {
            mediaPlayer = MediaPlayer.create(context, resId, speech, audioManager.generateAudioSessionId())?.apply {
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    audioManager.abandonAudioFocusRequest(focusRequest)
                }
                audioManager.requestAudioFocus(focusRequest)
                start()
            }
            mediaPlayer != null
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing raw resource $resName", e)
            false
        }
    }

    private fun playFile(file: File) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(speech)
                setDataSource(file.path)
                setOnPreparedListener {
                    audioManager.requestAudioFocus(focusRequest)
                    it.start()
                }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    audioManager.abandonAudioFocusRequest(focusRequest)
                }
                setOnErrorListener { mp, what, extra ->
                    Log.w("AudioPlayerManager", "Clip ${file.name} failed to play ($what/$extra)")
                    mp.release()
                    mediaPlayer = null
                    audioManager.abandonAudioFocusRequest(focusRequest)
                    tell("This recording couldn't be played.")
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
            audioManager.abandonAudioFocusRequest(focusRequest)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping audio", e)
        }
    }
}
