package com.kasiguru.domain.contribute

/**
 * What the app tells a learner about the dictionary they help build: their own word approved or
 * rejected, and every word an admin adds, edits or removes. Pure rules; the reads, the XP and the
 * delivery are `CommunityUpdatesRepository`.
 *
 * There is no server to push these (the free Firebase plan runs none), so the app compares what it
 * sees now with what it saw at its last check: its own `word_submissions`, and the
 * `dictionary_updates` log the admin portal writes beside every dictionary change.
 */

/** A word this learner sent, as the verification queue shows it now. */
data class SubmissionState(
    val id: String,
    val word: String,
    /** "pending", "approved" or "rejected". */
    val status: String,
    /** What its approval is worth; see [CommunityUpdates.approvalXp]. */
    val rewardXp: Int = CommunityUpdates.LEGACY_APPROVAL_XP
)

/** One change an admin made to the dictionary, from `dictionary_updates`. */
data class DictionaryChange(
    val kind: Kind,
    val word: String,
    val english: String,
    /** Epoch milliseconds, as the portal stamped it. */
    val at: Long,
    /** Set when the change is an approved submission, so its sender is not told twice. */
    val submissionId: String? = null
) {
    enum class Kind(val wire: String) {
        Added("added"), Edited("edited"), Removed("removed");

        companion object {
            fun of(wire: String?): Kind? = entries.firstOrNull { it.wire == wire }
        }
    }
}

/** A word named in a dictionary notice, with its English gloss when the change carried one. */
data class NoticeWord(val word: String, val english: String = "")

sealed interface CommunityEvent {
    /** Stable per event, for the phone notification's id and so the same event is shown once. */
    val key: String

    data class Approved(val submissionId: String, val word: String, val xp: Int) : CommunityEvent {
        override val key get() = "approved:$submissionId"
    }

    data class Rejected(val submissionId: String, val word: String) : CommunityEvent {
        override val key get() = "rejected:$submissionId"
    }

    data class DictionaryUpdated(
        val added: List<NoticeWord>,
        val edited: List<NoticeWord>,
        val removed: List<NoticeWord>
    ) : CommunityEvent {
        override val key get() = "dictionary:" + listOf(added, edited, removed).joinToString("|") { list -> list.joinToString(",") { it.word } }
        val count: Int get() = added.size + edited.size + removed.size
    }
}

object CommunityUpdates {

    /** What an approved word is worth from 1.28 on. The portal stamps it on the submission as `rewardXp`. */
    const val APPROVAL_XP = 100

    /** What every approval was worth before, and still is for those made then. */
    const val LEGACY_APPROVAL_XP = 10

    /**
     * XP for an approved submission. Only approvals the portal stamped with [APPROVAL_XP] give it, so
     * words approved before the change keep the 10 they gave, and no learner's total jumps.
     */
    fun approvalXp(rewardXp: Any?): Int =
        if ((rewardXp as? Number)?.toInt() == APPROVAL_XP) APPROVAL_XP else LEGACY_APPROVAL_XP

    /**
     * The learner's own words whose verdict changed since [previous] (submission id to the status
     * last seen). A null [previous] is the first check for this account on this device: everything
     * is taken as already seen, so nobody is told about decisions made before the feature existed.
     * A word first seen already decided (sent and reviewed between two checks) is told.
     */
    fun submissionEvents(previous: Map<String, String>?, current: List<SubmissionState>): List<CommunityEvent> {
        if (previous == null) return emptyList()
        return current.mapNotNull { s ->
            if (previous[s.id] == s.status) return@mapNotNull null
            when (s.status) {
                "approved" -> CommunityEvent.Approved(s.id, s.word, s.rewardXp)
                "rejected" -> CommunityEvent.Rejected(s.id, s.word)
                else -> null
            }
        }
    }

    /**
     * [changes] as one notice, or null when nothing is left to tell. Each word is named once, by
     * where it ended up: added and then edited is still new; added and then removed never reached the
     * learner and is left out; anything removed is removed. A change made by approving one of
     * [ownSubmissionIds] is left out too: its sender hears about it as their own word.
     */
    fun dictionaryEvent(changes: List<DictionaryChange>, ownSubmissionIds: Set<String>): CommunityEvent.DictionaryUpdated? {
        val latest = linkedMapOf<String, Pair<DictionaryChange.Kind, NoticeWord>>()
        changes.sortedBy { it.at }.forEach { change ->
            if (change.submissionId != null && change.submissionId in ownSubmissionIds) return@forEach
            val word = change.word.trim()
            if (word.isEmpty()) return@forEach
            val key = word.lowercase()
            val before = latest[key]?.first
            val kind = when {
                change.kind == DictionaryChange.Kind.Removed && before == DictionaryChange.Kind.Added -> null
                change.kind == DictionaryChange.Kind.Edited && before == DictionaryChange.Kind.Added -> DictionaryChange.Kind.Added
                else -> change.kind
            }
            if (kind == null) latest.remove(key)
            else latest[key] = kind to NoticeWord(word, change.english.trim().ifEmpty { latest[key]?.second?.english.orEmpty() })
        }
        fun of(kind: DictionaryChange.Kind) = latest.values.filter { it.first == kind }.map { it.second }
        val event = CommunityEvent.DictionaryUpdated(
            added = of(DictionaryChange.Kind.Added),
            edited = of(DictionaryChange.Kind.Edited),
            removed = of(DictionaryChange.Kind.Removed)
        )
        return event.takeIf { it.count > 0 }
    }

    fun title(event: CommunityEvent): String = when (event) {
        is CommunityEvent.Approved -> "Your word was added!"
        is CommunityEvent.Rejected -> "Your word wasn't added"
        is CommunityEvent.DictionaryUpdated -> single(event)?.let { (kind, w) ->
            when (kind) {
                DictionaryChange.Kind.Added -> "New word: ${w.word}"
                DictionaryChange.Kind.Edited -> "Word updated: ${w.word}"
                DictionaryChange.Kind.Removed -> "Word removed: ${w.word}"
            }
        } ?: "The dictionary was updated"
    }

    fun body(event: CommunityEvent): String = when (event) {
        is CommunityEvent.Approved ->
            "\"${event.word}\" is now in the dictionary, thanks to you. +${event.xp} XP"
        is CommunityEvent.Rejected ->
            "\"${event.word}\" wasn't approved this time. Thank you for helping; you can send another word any time."
        is CommunityEvent.DictionaryUpdated -> single(event)?.let { (kind, w) ->
            val gloss = w.english.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            when (kind) {
                DictionaryChange.Kind.Added -> "\"${w.word}\"$gloss is now in the dictionary."
                DictionaryChange.Kind.Edited -> "\"${w.word}\"$gloss has been corrected or added to."
                DictionaryChange.Kind.Removed -> "\"${w.word}\" is no longer in the dictionary."
            }
        } ?: listOfNotNull(
            line("New", event.added),
            line("Updated", event.edited),
            line("Removed", event.removed)
        ).joinToString("\n")
    }

    private fun single(event: CommunityEvent.DictionaryUpdated): Pair<DictionaryChange.Kind, NoticeWord>? = when {
        event.count != 1 -> null
        event.added.isNotEmpty() -> DictionaryChange.Kind.Added to event.added.first()
        event.edited.isNotEmpty() -> DictionaryChange.Kind.Edited to event.edited.first()
        else -> DictionaryChange.Kind.Removed to event.removed.first()
    }

    /** "New: bale, aso, bëbbi and 4 more" — three names, then a count. */
    private fun line(label: String, words: List<NoticeWord>): String? {
        if (words.isEmpty()) return null
        val shown = words.take(3).joinToString(", ") { it.word }
        val more = words.size - 3
        return if (more > 0) "$label: $shown and $more more" else "$label: $shown"
    }
}
