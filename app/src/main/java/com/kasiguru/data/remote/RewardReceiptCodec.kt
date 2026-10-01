package com.kasiguru.data.remote

import com.kasiguru.data.local.entity.RewardReceiptEntity
import com.kasiguru.domain.gamification.BadgeCatalog
import com.kasiguru.domain.gamification.XpPolicy
import java.security.MessageDigest
import java.time.LocalDate

object RewardReceiptCodec {
    const val COLLECTION = "rewardReceipts"
    fun documentId(id: String): String = MessageDigest.getInstance("SHA-256")
        .digest(id.toByteArray(Charsets.UTF_8)).joinToString("") {
            String.format(java.util.Locale.ROOT,"%02x",it.toInt() and 255)
        }
    fun encode(row: RewardReceiptEntity): Map<String,Any> = mapOf(
        "policyVersion" to XpPolicy.VERSION,"id" to row.id,"kind" to row.kind,"source" to row.source,
        "day" to row.day,"xp" to row.xp,"value" to row.value,"imported" to row.imported)
    fun decode(data: Map<String,Any?>): RewardReceiptEntity? {
        if((data["policyVersion"] as? Number)?.toInt() != XpPolicy.VERSION) return null
        val id = data["id"] as? String ?: return null
        val kind = data["kind"] as? String ?: return null
        val source = data["source"] as? String ?: return null
        val day = data["day"] as? String ?: return null
        val xp = (data["xp"] as? Number)?.toInt() ?: return null
        val value = (data["value"] as? Number)?.toInt() ?: return null
        val imported = data["imported"] as? Boolean ?: return null
        if(id.isBlank() || id.length > 512 || source.isBlank() || source.length > 256 || value !in 0..1) return null
        if(day.isNotEmpty() && runCatching { LocalDate.parse(day).toString() == day }.getOrDefault(false).not()) return null
        val validXp = when(kind) {
            "lesson" -> xp == 20 || xp == 25
            "game" -> xp in 5..40
            "replay" -> xp in 1..10 && day.isNotEmpty()
            "review" -> xp in 1..3 && day.isNotEmpty()
            "story" -> xp == 20
            "mastery" -> xp == 5
            "approved" -> xp == 10
            "badge" -> BadgeCatalog.familyFor(id) != null &&
                xp == if(BadgeCatalog.familyFor(id)?.bonus == true) BadgeCatalog.tierFor(id)?.bonus else 0
            "perfect","retrieval","category","access" -> xp == 0
            else -> false
        }
        val canonicalId = when(kind) {
            "lesson","game" -> if(imported && day.isEmpty()) "$source:import" else "$source:$day"
            "story","badge" -> source
            "replay","review","retrieval" -> "$kind:$source:$day"
            else -> "$kind:$source"
        }
        val validSource = when(kind) {
            "lesson" -> source.startsWith("lesson:") && source.substringAfterLast('#').toIntOrNull()?.let { it >= 0 } == true
            "game","perfect" -> source.startsWith("game:") && source.substringAfterLast('#').toIntOrNull()?.let { it > 0 } == true
            "replay" -> source.startsWith("lesson:") || source.startsWith("game:")
            "review","retrieval","mastery" -> source.startsWith("word:") && source.length > 5
            "story" -> source.startsWith("story:") && source.substringAfter(':').toIntOrNull()?.let { it > 0 } == true
            "approved" -> source.startsWith("word:") || source.startsWith("literature:")
            "badge" -> BadgeCatalog.familyFor(source) != null
            "access" -> source.startsWith("story:") || source.startsWith("section:")
            "category" -> true
            else -> false
        }
        return if(validXp && validSource && id == canonicalId) RewardReceiptEntity(id,kind,source,day,xp,value,imported) else null
    }
}
