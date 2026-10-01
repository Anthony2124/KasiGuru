package com.kasiguru.util.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.kasiguru.R

/** Quiet local feedback, independently switchable from vocabulary audio. */
class UiFeedbackSounds(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ready = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    init { pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) } }
    private val correct = pool.load(context, R.raw.ui_correct, 1)
    private val wrong = pool.load(context, R.raw.ui_wrong, 1)
    private val levelUp = pool.load(context, R.raw.ui_level_up, 1)
    private fun play(id: Int) { if (id in ready) pool.play(id, .35f, .35f, 1, 0, 1f) }
    fun answer(correctAnswer: Boolean) = play(if (correctAnswer) correct else wrong)
    fun levelUp() = play(levelUp)
    fun release() = pool.release()
}
