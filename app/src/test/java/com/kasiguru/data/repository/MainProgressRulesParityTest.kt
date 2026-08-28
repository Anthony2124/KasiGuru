package com.kasiguru.data.repository

import com.kasiguru.data.local.entity.UserProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the one parity [ProgressSyncFieldParityTest] cannot see: `toMap()` against the
 * `hasOnly()` allowlist in `firestore.rules`' `isValidMainProgress()`.
 *
 * `hasOnly()` rejects the *whole* document when it carries a single key the list omits, and the
 * client only logs a warning — so a field added to `toMap()` without being added to the rules
 * does not degrade sync, it stops `users/{uid}/progress/main` from ever being written again.
 * Nothing else fails, no screen changes, and the damage only surfaces on the next sign-in, which
 * restores a document frozen at the moment the mismatch shipped. That is exactly how the three
 * daily streak quota fields (`dailyReviewCompletedDate`, `dailyGamesDate`,
 * `dailyGamesPlayedCount`) took the streak and the daily requirements down with them.
 *
 * Parsed textually rather than evaluated: the rules cannot be run from a JVM unit test, but the
 * allowlist is a plain literal, and comparing the two lists catches the whole failure mode.
 */
class MainProgressRulesParityTest {

    @Test
    fun rulesAllowEveryKeyToMapWrites() {
        val rules = findRulesFile().readText()
        val allowed = allowlistInIsValidMainProgress(rules)
        val written = toMap(UserProgressEntity()).keys

        val rejected = written - allowed
        assertTrue(
            "firestore.rules' isValidMainProgress() does not allow $rejected, but " +
                "ProgressSyncManager.toMap() writes them. hasOnly() rejects the entire " +
                "document over one unlisted key, so this silently stops main progress from " +
                "syncing at all — add these to the allowlist (and give them a bound next to " +
                "the other field checks).",
            rejected.isEmpty()
        )
    }

    @Test
    fun rulesAllowlistHasNoKeyToMapNeverWrites() {
        val rules = findRulesFile().readText()
        val allowed = allowlistInIsValidMainProgress(rules)
        val written = toMap(UserProgressEntity()).keys

        val stale = allowed - written
        assertTrue(
            "firestore.rules' isValidMainProgress() allows $stale, which " +
                "ProgressSyncManager.toMap() no longer writes. Harmless today, but the " +
                "allowlist is the readable record of the document's shape — drop them.",
            stale.isEmpty()
        )
    }

    /**
     * The other half of the same drift: `isValidProgressDoc()` names the learning-state
     * documents the rules will accept a write for, and [ProgressDocuments.LEARNING] names the
     * ones the client writes. A document the client writes but the rules omit falls through to
     * the `hasOnly`-style rejection above; one the rules allow but the client no longer writes
     * is dead surface area.
     */
    @Test
    fun rulesAndClientAgreeOnTheLearningDocuments() {
        val rules = findRulesFile().readText()
        val body = rules.substringAfter("function isValidProgressDoc(", "")
        require(body.isNotEmpty()) { "isValidProgressDoc() not found in firestore.rules" }
        val literal = body.substringAfter("docId in [", "").substringBefore("]", "")
        require(literal.isNotEmpty()) { "docId allowlist not found in isValidProgressDoc()" }
        val allowed = Regex("'([A-Za-z][A-Za-z0-9]*)'").findAll(literal)
            .map { it.groupValues[1] }
            .toSet()

        assertEquals(
            "firestore.rules' isValidProgressDoc() and ProgressDocuments.LEARNING disagree " +
                "on which learning-state documents exist.",
            ProgressDocuments.LEARNING.toSet(),
            allowed
        )
    }

    private fun allowlistInIsValidMainProgress(rules: String): Set<String> {
        val body = rules.substringAfter("function isValidMainProgress()", "")
        require(body.isNotEmpty()) { "isValidMainProgress() not found in firestore.rules" }
        val literal = body.substringAfter("hasOnly([", "").substringBefore("])", "")
        require(literal.isNotEmpty()) { "hasOnly([...]) not found in isValidMainProgress()" }
        return Regex("'([A-Za-z][A-Za-z0-9]*)'").findAll(literal)
            .map { it.groupValues[1] }
            .toSet()
    }

    /**
     * Gradle runs unit tests from the module directory, so the repository-root rules file is one
     * level up — but that is a convention, not a guarantee, so walk up until it is found rather
     * than hard-coding `../firestore.rules`.
     */
    private fun findRulesFile(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "firestore.rules")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        throw AssertionError(
            "firestore.rules not found above ${System.getProperty("user.dir")}. If it moved, " +
                "update this test rather than deleting it — it is the only check that the " +
                "rules and ProgressSyncManager.toMap() still agree."
        )
    }
}
