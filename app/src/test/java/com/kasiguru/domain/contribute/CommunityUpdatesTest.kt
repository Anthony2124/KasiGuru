package com.kasiguru.domain.contribute

import com.kasiguru.domain.contribute.DictionaryChange.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityUpdatesTest {

    private fun sub(id: String, status: String, word: String = "bale", xp: Int = 100) = SubmissionState(id, word, status, xp)
    private fun change(kind: Kind, word: String, at: Long, english: String = "", submissionId: String? = null) =
        DictionaryChange(kind, word, english, at, submissionId)

    @Test
    fun `only approvals the portal stamped give 100 XP`() {
        assertEquals(100, CommunityUpdates.approvalXp(100L))
        assertEquals(100, CommunityUpdates.approvalXp(100))
        assertEquals(10, CommunityUpdates.approvalXp(null))
        assertEquals(10, CommunityUpdates.approvalXp(50L))
    }

    @Test
    fun `the first check tells nothing, so decisions made before the feature stay quiet`() {
        assertTrue(CommunityUpdates.submissionEvents(null, listOf(sub("a", "approved"), sub("b", "rejected"))).isEmpty())
    }

    @Test
    fun `a verdict is told once, when it changes`() {
        val previous = mapOf("a" to "pending", "b" to "pending", "c" to "approved")
        val events = CommunityUpdates.submissionEvents(
            previous,
            listOf(sub("a", "approved", "bale"), sub("b", "rejected", "aso"), sub("c", "approved"), sub("d", "pending"))
        )
        assertEquals(
            listOf(CommunityEvent.Approved("a", "bale", 100), CommunityEvent.Rejected("b", "aso")),
            events
        )
    }

    @Test
    fun `a word sent and decided between two checks is still told`() {
        val events = CommunityUpdates.submissionEvents(emptyMap(), listOf(sub("new", "approved", xp = 10)))
        assertEquals(listOf(CommunityEvent.Approved("new", "bale", 10)), events)
    }

    @Test
    fun `each dictionary word is named once, by where it ended up`() {
        val event = CommunityUpdates.dictionaryEvent(
            listOf(
                change(Kind.Edited, "Aso", 5),
                change(Kind.Added, "bale", 1, english = "house"),
                change(Kind.Edited, "bale", 2),       // added, then edited: still new
                change(Kind.Added, "dalan", 3),
                change(Kind.Removed, "dalan", 4),     // added, then removed: never reached anyone
                change(Kind.Edited, "bëbbi", 6),
                change(Kind.Removed, "bëbbi", 7)      // edited, then removed: removed
            ),
            ownSubmissionIds = emptySet()
        )!!
        assertEquals(listOf(NoticeWord("bale", "house")), event.added)
        assertEquals(listOf(NoticeWord("Aso")), event.edited)
        assertEquals(listOf(NoticeWord("bëbbi")), event.removed)
    }

    @Test
    fun `a learner's own approved word is told as theirs, not again as dictionary news`() {
        val changes = listOf(change(Kind.Added, "bale", 1, submissionId = "mine"))
        assertNull(CommunityUpdates.dictionaryEvent(changes, ownSubmissionIds = setOf("mine")))
        assertEquals(1, CommunityUpdates.dictionaryEvent(changes, ownSubmissionIds = setOf("other"))!!.added.size)
    }

    @Test
    fun `nothing to tell is no notice`() {
        assertNull(CommunityUpdates.dictionaryEvent(emptyList(), emptySet()))
        assertNull(CommunityUpdates.dictionaryEvent(listOf(change(Kind.Added, "  ", 1)), emptySet()))
    }

    @Test
    fun `one change names the word, several are summed up`() {
        val one = CommunityUpdates.dictionaryEvent(listOf(change(Kind.Added, "bale", 1, english = "house")), emptySet())!!
        assertEquals("New word: bale", CommunityUpdates.title(one))
        assertEquals("\"bale\" (house) is now in the dictionary.", CommunityUpdates.body(one))

        val many = CommunityUpdates.dictionaryEvent(
            listOf("a", "b", "c", "d", "e").mapIndexed { i, w -> change(Kind.Added, w, i.toLong()) } +
                change(Kind.Removed, "z", 9),
            emptySet()
        )!!
        assertEquals("The dictionary was updated", CommunityUpdates.title(many))
        assertEquals("New: a, b, c and 2 more\nRemoved: z", CommunityUpdates.body(many))
    }

    @Test
    fun `the learner's own verdicts say what happened to their word`() {
        assertEquals("Your word was added!", CommunityUpdates.title(CommunityEvent.Approved("a", "bale", 100)))
        assertTrue(CommunityUpdates.body(CommunityEvent.Approved("a", "bale", 100)).endsWith("+100 XP"))
        assertEquals("Your word wasn't added", CommunityUpdates.title(CommunityEvent.Rejected("b", "aso")))
        assertTrue(CommunityUpdates.body(CommunityEvent.Rejected("b", "aso")).startsWith("\"aso\" wasn't approved"))
    }
}
