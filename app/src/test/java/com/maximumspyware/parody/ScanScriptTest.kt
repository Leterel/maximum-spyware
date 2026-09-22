package com.maximumspyware.parody

import org.junit.Assert.*
import org.junit.Test

class ScanScriptTest {
    @Test fun progressIsClampedAndMonotonic() {
        assertEquals(0f, ScanScript.progress(-500), 0f)
        assertEquals(1f, ScanScript.progress(Int.MAX_VALUE), 0f)
        var previous = 0f
        for (ms in 0..ScanScript.DURATION_MS step ScanScript.TICK_MS) {
            val progress = ScanScript.progress(ms)
            assertTrue(progress >= previous && progress in 0f..1f)
            previous = progress
        }
    }
    @Test fun timelineEndsExactlyWithCompletion() {
        assertEquals(0, ScanScript.entries.first().atMs)
        assertEquals(ScanScript.DURATION_MS, ScanScript.entries.last().atMs)
        assertTrue(ScanScript.entries.zipWithNext().all { (a, b) -> a.atMs < b.atMs })
        assertFalse(ScanScript.complete(ScanScript.DURATION_MS - 1))
        assertTrue(ScanScript.complete(ScanScript.DURATION_MS))
        assertEquals(ScanScript.entries, ScanScript.visibleEntries(ScanScript.DURATION_MS))
    }
    @Test fun futureEventsCannotLeakIntoScan() {
        for (ms in 0..ScanScript.DURATION_MS step 53) {
            val visible = ScanScript.visibleEntries(ms)
            assertTrue(visible.all { it.atMs <= ms })
            assertTrue(ScanScript.entries.filter { it.atMs > ms }.none { it in visible })
        }
    }
    @Test fun reportAndMessagesExplicitlyDiscloseFiction() {
        assertTrue(ScanScript.entries.first().message.contains("SIMULATION"))
        assertTrue(ScanScript.entries.last().message.contains("Alles Fake"))
        assertEquals(5, ScanScript.findings.size)
        assertTrue(ScanScript.findings.all { it.label.isNotBlank() && it.description.isNotBlank() })
    }
}
