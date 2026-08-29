package com.kasiguru.data.repository

import com.kasiguru.data.remote.model.IssueReportDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards parity between [IssueReportDto] properties and `firestore.rules`' `isValidIssueReport()` allowlist.
 */
class IssueReportRulesParityTest {

    @Test
    fun rulesAllowEveryIssueReportDtoProperty() {
        val rules = findRulesFile().readText()
        val allowed = allowlistInIsValidIssueReport(rules)
        val dtoFields = IssueReportDto::class.java.declaredFields
            .map { it.name }
            .filter { !it.startsWith("$") }
            .toSet()

        val rejected = dtoFields - allowed
        assertTrue(
            "firestore.rules' isValidIssueReport() does not allow $rejected, but IssueReportDto writes them.",
            rejected.isEmpty()
        )
    }

    @Test
    fun rulesAllowlistHasNoStaleProperties() {
        val rules = findRulesFile().readText()
        val allowed = allowlistInIsValidIssueReport(rules)
        val dtoFields = IssueReportDto::class.java.declaredFields
            .map { it.name }
            .filter { !it.startsWith("$") }
            .toSet()

        val stale = allowed - dtoFields
        assertTrue(
            "firestore.rules' isValidIssueReport() allows $stale, which IssueReportDto does not have.",
            stale.isEmpty()
        )
    }

    @Test
    fun issueReportDtoDefaultInstantiation() {
        val report = IssueReportDto(
            category = "Bug / System Issue",
            title = "Test crash",
            description = "App froze on level 2",
            targetScreen = "WordMatch",
            photoBase64 = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
            reporterName = "Juan",
            appVersion = "1.10.0 (11)",
            deviceInfo = "Google Pixel 7 (Android 14)"
        )

        assertEquals("Bug / System Issue", report.category)
        assertEquals("Test crash", report.title)
        assertEquals("pending", report.status)
        assertEquals("WordMatch", report.targetScreen)
        assertTrue(report.photoBase64.startsWith("data:image/jpeg;base64,"))
    }

    private fun allowlistInIsValidIssueReport(rules: String): Set<String> {
        val body = rules.substringAfter("function isValidIssueReport()", "")
        require(body.isNotEmpty()) { "isValidIssueReport() not found in firestore.rules" }
        val literal = body.substringAfter("hasOnly([", "").substringBefore("])", "")
        require(literal.isNotEmpty()) { "hasOnly([...]) not found in isValidIssueReport()" }
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
