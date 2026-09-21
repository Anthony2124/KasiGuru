package com.kasiguru.data.repository

import com.kasiguru.data.remote.model.LiteratureSubmissionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards parity between [LiteratureSubmissionDto] properties and `firestore.rules`' `isValidLiteratureSubmission()` allowlist.
 */
class LiteratureSubmissionRulesParityTest {

    @Test
    fun rulesAllowEveryLiteratureSubmissionDtoProperty() {
        val rules = findRulesFile().readText()
        val allowed = allowlistInIsValidLiteratureSubmission(rules)
        val dtoFields = LiteratureSubmissionDto::class.java.declaredFields
            .map { it.name }
            .filter { !it.startsWith("$") }
            .toSet()

        val rejected = dtoFields - allowed
        assertTrue(
            "firestore.rules' isValidLiteratureSubmission() does not allow $rejected, but LiteratureSubmissionDto writes them.",
            rejected.isEmpty()
        )
    }

    @Test
    fun rulesAllowlistHasNoStaleProperties() {
        val rules = findRulesFile().readText()
        val allowed = allowlistInIsValidLiteratureSubmission(rules)
        val dtoFields = LiteratureSubmissionDto::class.java.declaredFields
            .map { it.name }
            .filter { !it.startsWith("$") }
            .toSet()

        val stale = allowed - dtoFields
        assertTrue(
            "firestore.rules' isValidLiteratureSubmission() allows $stale, which LiteratureSubmissionDto does not have.",
            stale.isEmpty()
        )
    }

    @Test
    fun literatureSubmissionDtoDefaultInstantiation() {
        val submission = LiteratureSubmissionDto(
            title = "Alamat ng Agila",
            titleKasiguranin = "Alamat nu Agila",
            pagesJson = "[]",
            contributorName = "Maria Santos",
            pdfBase64 = "data:application/pdf;base64,JVBERi0xLjQK",
            pdfFileName = "alamat_agila.pdf"
        )

        assertEquals("Alamat ng Agila", submission.title)
        assertEquals("Alamat nu Agila", submission.titleKasiguranin)
        assertEquals("pending", submission.status)
        assertEquals("alamat_agila.pdf", submission.pdfFileName)
        assertTrue(submission.pdfBase64.startsWith("data:application/pdf;base64,"))
    }

    private fun allowlistInIsValidLiteratureSubmission(rules: String): Set<String> {
        val body = rules.substringAfter("function isValidLiteratureSubmission()", "")
        require(body.isNotEmpty()) { "isValidLiteratureSubmission() not found in firestore.rules" }
        val literal = body.substringAfter("hasOnly([", "").substringBefore("])", "")
        require(literal.isNotEmpty()) { "hasOnly([...]) not found in isValidLiteratureSubmission()" }
        return Regex("'([A-Za-z][A-Za-z0-9]*)'").findAll(literal)
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun findRulesFile(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "firestore.rules")
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        throw AssertionError("firestore.rules not found above ${System.getProperty("user.dir")}")
    }
}
