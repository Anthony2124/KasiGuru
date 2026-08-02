package com.kasiguru.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acoustic Guide Manager (Module 3.3 in System Architecture).
 * Wraps ExoPlayer with speed controls (0.5x, 0.75x, 1.0x) for phoneme & pronunciation practice.
 */
@Singleton
class AcousticPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    init {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
            })
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    @OptIn(UnstableApi::class)
    fun playAudio(rawResName: String) {
        val resId = context.resources.getIdentifier(rawResName, "raw", context.packageName)
        if (resId != 0) {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/$resId")
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                playbackParameters = PlaybackParameters(_playbackSpeed.value)
                prepare()
                play()
            }
        }
    }

    fun stop() {
        exoPlayer?.stop()
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
