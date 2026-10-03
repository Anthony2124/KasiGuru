package com.kasiguru.util.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.kasiguru.R
import com.kasiguru.data.repository.DEFAULT_VOLUME_PERCENT
import com.kasiguru.data.repository.UserPreferencesRepository
import com.kasiguru.domain.audio.MusicMood
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background music for menus and games. The screen sets the [MusicMood]; the activity reports
 * whether the app is in the foreground; the learner's setting turns it on or off.
 *
 * Every MediaPlayer call is a blocking round trip to the media server, and a fade makes dozens of
 * them, so all player work and state live on one dedicated thread. Public calls only post to it,
 * and the main thread never waits on audio.
 *
 * Music holds ordinary audio focus. A pronunciation recording asks for transient focus that may
 * duck, and the music lowers itself in answer: Android does not auto-duck an app's own players for
 * that app's request. Calls or another app's playback silence it. If the learner is already
 * listening to something else when the app comes forward, it stays quiet.
 */
@Singleton
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    preferences: UserPreferencesRepository
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val thread = HandlerThread("KasiGuruMusic").apply { start() }
    private val handler = Handler(thread.looper)
    private val scope = CoroutineScope(SupervisorJob() + handler.asCoroutineDispatcher())

    // Touched only on [thread].
    private val players = mutableMapOf<MusicMood, MediaPlayer>()
    private val levels = mutableMapOf<MusicMood, Float>()
    private var fadeJob: Job? = null
    /** Where the running [fadeJob] is taking the music, so a repeat request does not restart it. */
    private var heading: Pair<MusicMood, Float>? = null
    private var mood = MusicMood.None
    private var enabled = false
    private var foreground = false
    private var courtesyMuted = false
    private var focusLost = false
    private var transientLoss = false
    private var ducked = false
    private var hasFocus = false
    private var volumePercent = DEFAULT_VOLUME_PERCENT

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener({ change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS -> { hasFocus = false; focusLost = true }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> transientLoss = true
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> ducked = true
                AudioManager.AUDIOFOCUS_GAIN -> { transientLoss = false; ducked = false }
            }
            sync()
        }, handler)
        .build()

    init {
        // DataStore re-emits on every write to any key; only real changes should touch the player.
        preferences.musicEnabled.distinctUntilChanged().onEach { enabled = it; sync() }.launchIn(scope)
        preferences.musicVolumePercent.distinctUntilChanged().onEach { volumePercent = it; sync() }.launchIn(scope)
    }

    /** Follows the Settings slider while it is dragged; the saved value takes over on release. */
    fun previewVolume(percent: Int) = post {
        volumePercent = percent
        sync()
    }

    fun setMood(newMood: MusicMood) = post {
        mood = newMood
        sync()
    }

    fun onForeground() = post {
        // Defer to the learner's own music or podcast. After a quick switch away and back, our own
        // loop may still be fading out, and that must not count as someone else's.
        courtesyMuted = audioManager.isMusicActive && players.values.none { it.isPlaying }
        focusLost = false
        foreground = true
        sync()
    }

    fun onBackground() = post {
        foreground = false
        sync()
    }

    private fun post(block: () -> Unit) {
        handler.post(block)
    }

    private val target: MusicMood
        get() = if (enabled && volumePercent > 0 && foreground && !courtesyMuted && !focusLost && !transientLoss) mood
            else MusicMood.None

    private fun sync() {
        val target = target
        // VOLUME is the level at the default 50%, so the slider doubles it at 100%.
        val full = (VOLUME * volumePercent / DEFAULT_VOLUME_PERCENT).coerceIn(0f, 1f)
        val volume = if (ducked) full * DUCK_RATIO else full
        val audible = players.filterValues { it.isPlaying }.keys
        if (audible == setOfNotNull(target.takeIf { it != MusicMood.None }) && levels[target] == volume) return
        // A fade already on its way to the same place is left to finish; restarting it on every
        // request can keep it from ever reaching the end on a slow device.
        if (fadeJob?.isActive == true && heading == (target to volume)) return
        heading = target to volume
        fadeJob?.cancel()
        fadeJob = scope.launch {
            // Leaving the app stops the music at once; only a change of screen fades it out.
            if (foreground) audible.filter { it != target }.forEach { fade(it, 0f) }
            audible.filter { it != target }.forEach {
                players[it]?.pause()
                setLevel(it, 0f)
            }
            if (target == MusicMood.None) {
                abandonFocus()
                return@launch
            }
            if (!requestFocus()) return@launch
            val player = playerFor(target) ?: return@launch
            if (!player.isPlaying) {
                setLevel(target, levels[target] ?: 0f)
                player.start()
            }
            // Already playing means only the level changed (a duck or the slider): answer quickly.
            fade(target, volume, if (target in audible) QUICK_MS else FADE_MS)
        }
    }

    private suspend fun fade(mood: MusicMood, to: Float, durationMs: Long = FADE_MS) {
        val from = levels[mood] ?: 0f
        for (step in 1..FADE_STEPS) {
            setLevel(mood, from + (to - from) * step / FADE_STEPS)
            delay(durationMs / FADE_STEPS)
        }
        setLevel(mood, to)
    }

    private fun setLevel(mood: MusicMood, level: Float) {
        levels[mood] = level
        players[mood]?.setVolume(level, level)
    }

    private fun playerFor(mood: MusicMood): MediaPlayer? = players[mood] ?: run {
        val res = when (mood) {
            MusicMood.Menu -> R.raw.music_menu
            MusicMood.Game -> R.raw.music_game
            MusicMood.None -> return null
        }
        try {
            MediaPlayer.create(context, res, attributes, audioManager.generateAudioSessionId())
                ?.apply { isLooping = true }
                ?.also { players[mood] = it; levels[mood] = 0f }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Could not load $mood music", e)
            null
        }
    }

    private fun requestFocus(): Boolean {
        if (!hasFocus) {
            hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        return hasFocus
    }

    private fun abandonFocus() {
        if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }

    private companion object {
        const val VOLUME = 0.35f
        const val DUCK_RATIO = 0.2f
        const val FADE_MS = 600L
        const val QUICK_MS = 150L
        const val FADE_STEPS = 12
    }
}
