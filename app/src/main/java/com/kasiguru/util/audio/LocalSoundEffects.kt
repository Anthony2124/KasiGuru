package com.kasiguru.util.audio

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lets shared components play feedback without each needing a ViewModel. Null in previews and
 * tests, where nothing is provided, so sounds are simply skipped there.
 */
val LocalSoundEffects = staticCompositionLocalOf<SoundEffects?> { null }
