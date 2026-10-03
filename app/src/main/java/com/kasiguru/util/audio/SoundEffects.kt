package com.kasiguru.util.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.HandlerThread
import com.kasiguru.R
import com.kasiguru.data.repository.DEFAULT_VOLUME_PERCENT
import com.kasiguru.data.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** The app's short feedback sounds. The files come from Freesound; see `data/audio/`. */
enum class Sfx(val res: Int, val volume: Float, val isTap: Boolean = false) {
    Correct(R.raw.ui_correct, .35f),
    Wrong(R.raw.ui_wrong, .35f),
    Found(R.raw.ui_found, .4f),
    Complete(R.raw.ui_complete, .35f),
    LevelUp(R.raw.ui_level_up, .35f),
    Streak(R.raw.ui_streak, .35f),
    Badge(R.raw.ui_badge, .35f),
    Flip(R.raw.ui_flip, .3f),
    Tap(R.raw.ui_tap, .15f, isTap = true)
}

/**
 * One pool for the whole process, loaded once at startup, so the first sound on a screen is not
 * lost to a pool that is still loading. Callers just [play]; the learner's settings decide whether
 * anything is heard, and are independent of word pronunciation audio. Starting a stream can wait on
 * the audio server, so playback is handed to a background thread and never holds up a tap.
 */
@Singleton
class SoundEffects @Inject constructor(
    @ApplicationContext context: Context,
    preferences: UserPreferencesRepository
) {
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private val handler = Handler(HandlerThread("KasiGuruSfx").apply { start() }.looper)
    private val ids: Map<Sfx, Int>

    @Volatile private var effectsOn = true
    @Volatile private var tapsOn = true
    @Volatile private var volumePercent = DEFAULT_VOLUME_PERCENT

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        ids = Sfx.entries.associateWith { pool.load(context, it.res, 1) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        preferences.soundEnabled.onEach { effectsOn = it }.launchIn(scope)
        preferences.tapSoundsEnabled.onEach { tapsOn = it }.launchIn(scope)
        preferences.sfxVolumePercent.onEach { volumePercent = it }.launchIn(scope)
    }

    fun play(sfx: Sfx) {
        if (!(if (sfx.isTap) tapsOn else effectsOn)) return
        playAt(sfx, volumePercent)
    }

    /** Plays a sample at [percent] for the Settings slider, before the new level is saved. */
    fun preview(percent: Int) = playAt(Sfx.Correct, percent)

    // Each sound's own volume is its level at the default 50%, so the slider doubles it at 100%.
    private fun playAt(sfx: Sfx, percent: Int) {
        val volume = (sfx.volume * percent / DEFAULT_VOLUME_PERCENT).coerceIn(0f, 1f)
        val id = ids.getValue(sfx)
        if (volume > 0f && id in ready) handler.post { pool.play(id, volume, volume, 1, 0, 1f) }
    }
}
