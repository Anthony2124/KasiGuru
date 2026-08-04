package com.kasiguru.util.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
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
     * Plays a vocabulary audio file from raw resources if available,
     * or speaks the text using Text-To-Speech as a fallback.
     */
    fun playAudio(textToSpeak: String, audioFileName: String = "") {
        stopAudio()

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

    fun stopAudio() {
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
