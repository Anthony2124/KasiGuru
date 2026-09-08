package com.kasiguru.util.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
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
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // Built directly rather than injected: every screen constructs this class with
    // `remember { AudioPlayerManager(context) }` rather than through Hilt, so there is no graph to
    // inject the repository from. It needs only a Context and the Firestore singleton, both already
    // available process-wide.
    private val wordAudioRepository by lazy {
        WordAudioRepository(context, FirebaseFirestore.getInstance())
    }
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var resolveJob: Job? = null

    init {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("fil", "PH"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.ENGLISH)
            }
            textToSpeech?.setPitch(0.95f)
            textToSpeech?.setSpeechRate(0.85f)
            isTtsInitialized = true
        }
    }

    /**
     * Plays a word's pronunciation: the recording an admin uploaded, if there is one, otherwise the
     * bundled raw resource, otherwise Text-To-Speech.
     *
     * The recording is fetched from Firestore and cached on first use ([WordAudioRepository]); a
     * cache hit plays immediately, a miss costs a short fetch, and any failure — no clip, offline and
     * uncached — drops straight to the same raw/TTS path [playAudio] has always used. The lookup is
     * off the main thread; the actual playback is posted back to it.
     */
    fun playWord(word: VocabularyEntity) {
        stopAudio()
        resolveJob = scope.launch {
            val file = try {
                wordAudioRepository.audioFor(word.audioFileName, word.audioUpdatedAt)
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Audio lookup failed for ${word.kasiguranin}", e)
                null
            }
            if (file != null) playFile(file) else playRawOrTts(word.kasiguranin, word.audioFileName)
        }
    }

    /**
     * Plays a vocabulary audio file from raw resources if available, or speaks the text using
     * Text-To-Speech as a fallback.
     */
    fun playAudio(textToSpeak: String, audioFileName: String = "") {
        stopAudio()
        playRawOrTts(textToSpeak, audioFileName)
    }

    private fun playRawOrTts(textToSpeak: String, audioFileName: String) {
        // 1. Try to find resource ID in res/raw/
        val resName = audioFileName.ifEmpty { textToSpeak.lowercase().replace(Regex("[^a-z0-9_]"), "") }
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)

        if (resId != 0) {
            try {
                mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                    setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                    }
                    start()
                }
                return
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error playing raw media resource", e)
            }
        }

        // 2. Fallback to Text-To-Speech
        if (isTtsInitialized && textToSpeak.isNotBlank()) {
            val cleanText = textToSpeak.replace("ʔ", "").replace("ː", "")
            textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "vocab_tts_${System.currentTimeMillis()}")
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
            Log.e("AudioPlayerManager", "Error playing downloaded clip ${file.name}", e)
        }
    }

    fun stopAudio() {
        resolveJob?.cancel()
        resolveJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping audio", e)
        }
    }
}
