package com.kasiguru.data.remote

import com.kasiguru.data.local.entity.RewardReceiptEntity
import org.junit.Assert.*
import org.junit.Test

class RewardReceiptCodecTest {
    @Test fun stableIdsWithSpacesAndSlashesAreSafeFirestoreDocumentIds() {
        val id = "review:word:example/entry:2026-10-01"
        val hash = RewardReceiptCodec.documentId(id)
        assertEquals(64,hash.length)
        assertFalse(hash.contains('/'))
        assertEquals(hash,RewardReceiptCodec.documentId(id))
    }
    @Test fun receiptsRoundTripWithEveryField() {
        val row = RewardReceiptEntity("game:word_match#1:2026-10-01","game","game:word_match#1","2026-10-01",20)
        assertEquals(row,RewardReceiptCodec.decode(RewardReceiptCodec.encode(row)))
    }
    @Test fun malformedAndUnknownReceiptsCannotMintXp() {
        val row = RewardReceiptEntity("review:word:x:2026-10-01","review","word:x","2026-10-01",3)
        val payload = RewardReceiptCodec.encode(row)
        assertNotNull(RewardReceiptCodec.decode(payload))
        assertNull(RewardReceiptCodec.decode(payload + ("xp" to 200)))
        assertNull(RewardReceiptCodec.decode(payload + ("day" to "2026-02-30")))
        assertNull(RewardReceiptCodec.decode(payload + ("kind" to "welcome")))
        assertNull(RewardReceiptCodec.decode(payload + ("policyVersion" to 1)))
    }
}
