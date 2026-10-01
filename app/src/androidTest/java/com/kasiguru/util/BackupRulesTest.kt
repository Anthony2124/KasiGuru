package com.kasiguru.util

import androidx.test.platform.app.InstrumentationRegistry
import com.kasiguru.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class BackupRulesTest {
    @Test
    fun bothBackupFormatsExcludeDatabaseAndPreferences() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val expected = setOf("database:.", "sharedpref:.")
        resources.getXml(R.xml.backup_rules_legacy).use { xml ->
            while (xml.eventType != XmlPullParser.START_TAG) xml.next()
            assertEquals("full-backup-content", xml.name)
            assertEquals(expected, exclusions(xml))
        }
        resources.getXml(R.xml.backup_rules).use { xml ->
            while (xml.eventType != XmlPullParser.START_TAG) xml.next()
            assertEquals("data-extraction-rules", xml.name)
            val sections = mutableMapOf<String, Set<String>>()
            while (xml.next() != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == XmlPullParser.START_TAG && xml.depth == 2) {
                    sections[xml.name] = exclusions(xml)
                }
            }
            assertEquals(mapOf("cloud-backup" to expected, "device-transfer" to expected), sections)
        }
    }

    private fun exclusions(xml: XmlPullParser): Set<String> {
        val depth = xml.depth
        val result = mutableSetOf<String>()
        while (xml.next() != XmlPullParser.END_DOCUMENT) {
            if (xml.eventType == XmlPullParser.END_TAG && xml.depth == depth) break
            if (xml.eventType == XmlPullParser.START_TAG && xml.name == "exclude") {
                result += "${xml.getAttributeValue(null, "domain")}:${xml.getAttributeValue(null, "path")}"
            }
        }
        return result
    }
}
