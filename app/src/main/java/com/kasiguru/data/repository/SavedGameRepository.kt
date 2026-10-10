package com.kasiguru.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kasiguru.domain.games.SavedGame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The game each level was left in, one per level, kept on this device only: see [SavedGame].
 *
 * Levels are keyed by the same game type as their `game_levels` row, so Word Search saves one game
 * per category level and Recall saves under its stored key, `audio_quiz`.
 */
@Singleton
class SavedGameRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /**
     * Every read and write runs here, one at a time and in the order asked for, on a scope that
     * outlives the game screen. The last answer before leaving must still be written after the
     * screen's ViewModel is gone, and a save must never land after the clear that follows it: that
     * would bring a finished round back to be rewarded a second time.
     */
    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            for (operation in queue) runCatching { operation() }
        }
    }

    /** The game [level] of [gameType] was left in, or null to deal a fresh one. */
    suspend fun load(gameType: String, level: Int): SavedGame? = awaitQueued {
        dataStore.data.first()[keyFor(gameType, level)]?.let(SavedGameCodec::decode)
    }

    /** Keeps [game] as the one to resume. Returns at once; the write follows in order. */
    fun save(gameType: String, level: Int, game: SavedGame) {
        val text = SavedGameCodec.encode(game)
        queue.trySend { dataStore.edit { it[keyFor(gameType, level)] = text } }
    }

    /** Forgets the saved game, as its round finishes. Returns at once; the write follows in order. */
    fun clear(gameType: String, level: Int) {
        queue.trySend { dataStore.edit { it.remove(keyFor(gameType, level)) } }
    }

    /** The levels of [gameType] that have a game to resume, for the level picker. */
    fun savedLevels(gameType: String): Flow<Set<Int>> {
        val prefix = "$KEY_PREFIX$gameType/"
        return dataStore.data
            .map { prefs ->
                prefs.asMap().keys.mapNotNullTo(mutableSetOf()) { key ->
                    key.name.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull()
                }
            }
            .distinctUntilChanged()
    }

    /** Forgets every saved game, for sign-out: the next learner on this device starts their own. */
    suspend fun clearAll() = awaitQueued {
        dataStore.edit { prefs ->
            prefs.asMap().keys.filter { it.name.startsWith(KEY_PREFIX) }.forEach { prefs.remove(it) }
        }
        Unit
    }

    private suspend fun <T> awaitQueued(operation: suspend () -> T): T {
        val result = CompletableDeferred<T>()
        queue.send { result.completeWith(runCatching { operation() }) }
        return result.await()
    }

    private fun keyFor(gameType: String, level: Int) = stringPreferencesKey("$KEY_PREFIX$gameType/$level")

    private companion object {
        const val KEY_PREFIX = "saved_game/"
    }
}
