package com.kasiguru.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kasiguru.domain.contribute.CommunityEvent
import com.kasiguru.domain.contribute.CommunityUpdates
import com.kasiguru.domain.contribute.DictionaryChange
import com.kasiguru.util.notification.KasiGuruNotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells the learner about the dictionary they help build (see [CommunityUpdates]): their own words
 * approved (with the approval's XP) or rejected, and every word an admin adds, edits or removes.
 *
 * There is no server to push these on the free plan, so the app checks: when it comes to the
 * foreground (at most every [FOREGROUND_INTERVAL_MS]) and every few hours from
 * `CommunityUpdatesWorker`. A check costs a read per own submission plus one per new dictionary
 * change, and one more when there is nothing new.
 *
 * While the app is open, news becomes the in-app pop-up ([pending]); otherwise a phone
 * notification. Either way it is kept in the notification inbox.
 */
@Singleton
class CommunityUpdatesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val firestore: FirebaseFirestore,
    private val submissions: SubmissionRepository,
    private val userProgress: UserProgressRepository,
    private val inbox: NotificationRepository
) {
    private val mutex = Mutex()
    private var lastForegroundCheckAt = 0L

    private val _pending = MutableStateFlow<List<CommunityEvent>>(emptyList())

    /** News found while the app was open, oldest first, for the in-app pop-up. */
    val pending: StateFlow<List<CommunityEvent>> = _pending.asStateFlow()

    /** The pop-up showed [event]; the next one, if any, follows. */
    fun dismiss(event: CommunityEvent) = _pending.update { list -> list.filterNot { it.key == event.key } }

    /** A check on coming to the foreground, skipped when one ran in the last few minutes. */
    suspend fun checkInForeground() {
        // On a first launch the anonymous sign-in may still be on its way; the next resume checks.
        if (FirebaseAuth.getInstance().currentUser == null) return
        val now = System.currentTimeMillis()
        if (now - lastForegroundCheckAt < FOREGROUND_INTERVAL_MS) return
        lastForegroundCheckAt = now
        checkAndDeliver(inForeground = true)
    }

    /**
     * Finds what is new and delivers it: [inForeground] as the in-app pop-up, otherwise as phone
     * notifications. Never throws: a failed check leaves its "last seen" marks where they were, so
     * the next one picks the same news up.
     */
    suspend fun checkAndDeliver(inForeground: Boolean) = mutex.withLock {
        val events = runCatching { findNew() }.getOrElse {
            Log.w(TAG, "Community update check failed", it)
            return@withLock
        }
        if (events.isEmpty()) return@withLock
        events.forEach { event ->
            inbox.addNotification(
                title = CommunityUpdates.title(event),
                message = CommunityUpdates.body(event),
                category = if (event is CommunityEvent.DictionaryUpdated) CATEGORY_DICTIONARY else CATEGORY_CONTRIBUTION,
                deepLinkRoute = "notifications"
            )
        }
        if (inForeground) {
            _pending.update { list -> list + events.filterNot { e -> list.any { it.key == e.key } } }
        } else {
            events.forEach { event ->
                KasiGuruNotificationManager.sendDictionaryNotification(
                    context,
                    CommunityUpdates.title(event),
                    CommunityUpdates.body(event),
                    event.key,
                    summary = event is CommunityEvent.DictionaryUpdated
                )
            }
        }
    }

    private suspend fun findNew(): List<CommunityEvent> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        val prefs = dataStore.data.first()

        // The learner's own words: verdicts that changed since the last check.
        val own = submissions.getOwnWordSubmissions().getOrThrow()
        val statusKey = stringPreferencesKey("$KEY_PREFIX/submissions/$uid")
        val previous = prefs[statusKey]?.let(::decodeStatuses)
        val ownEvents = CommunityUpdates.submissionEvents(previous, own.map { it.state })

        // XP for every approval, each at what it was worth when given. Receipts are keyed by
        // submission, so a word already rewarded on the Learn screen or another device is not again.
        own.filter { it.state.status == "approved" }.takeIf { it.isNotEmpty() }?.let { approved ->
            userProgress.recordApprovals(approved.map { ApprovedContribution("word:${it.state.id}", it.approvedDay, it.state.rewardXp) })
        }

        // Every learner: words added, edited or removed since the last check.
        val seenKey = longPreferencesKey("$KEY_PREFIX/dictionary_seen_at")
        val seenAt = prefs[seenKey]
        val updates = firestore.collection(DICTIONARY_UPDATES)
        val dictionaryEvent = if (seenAt == null) {
            // First check on this device: start from the newest change, so nobody gets the history.
            val newest = updates.orderBy("at", Query.Direction.DESCENDING).limit(1).get().await()
                .documents.firstOrNull()?.getLong("at") ?: 0L
            dataStore.edit { it[seenKey] = newest }
            null
        } else {
            val changes = updates.whereGreaterThan("at", seenAt).orderBy("at").limit(PAGE).get().await()
                .documents.mapNotNull(::changeOf)
            if (changes.isNotEmpty()) dataStore.edit { it[seenKey] = changes.maxOf { c -> c.at } }
            CommunityUpdates.dictionaryEvent(changes, own.map { it.state.id }.toSet())
        }

        // Saved last, so a check that failed above is retried in full.
        dataStore.edit { it[statusKey] = encodeStatuses(own.associate { it.state.id to it.state.status }) }
        return ownEvents + listOfNotNull(dictionaryEvent)
    }

    private fun changeOf(doc: DocumentSnapshot): DictionaryChange? {
        val kind = DictionaryChange.Kind.of(doc.getString("kind")) ?: return null
        val at = doc.getLong("at") ?: return null
        return DictionaryChange(
            kind = kind,
            word = doc.getString("word").orEmpty(),
            english = doc.getString("english").orEmpty(),
            at = at,
            submissionId = doc.getString("submissionId")
        )
    }

    private fun encodeStatuses(statuses: Map<String, String>): String = JSONObject(statuses).toString()

    private fun decodeStatuses(text: String): Map<String, String>? = runCatching {
        val json = JSONObject(text)
        json.keys().asSequence().associateWith { json.getString(it) }
    }.getOrNull()

    companion object {
        private const val TAG = "CommunityUpdates"
        private const val KEY_PREFIX = "community_updates"
        const val DICTIONARY_UPDATES = "dictionary_updates"
        const val CATEGORY_CONTRIBUTION = "Contribution"
        const val CATEGORY_DICTIONARY = "Dictionary"

        /** At most this many dictionary changes a check; the next check carries on after them. */
        private const val PAGE = 200L

        /** Coming back to the app checks again only after this long. */
        private const val FOREGROUND_INTERVAL_MS = 5 * 60 * 1000L
    }
}
