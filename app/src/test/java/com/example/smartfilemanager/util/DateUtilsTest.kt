package com.example.smartfilemanager.util

import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    private val expectedPattern = Regex("""\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}""")

    @Test
    fun `formatTimestamp produces dd-MM-yyyy HH-mm shape`() {
        val result = DateUtils.formatTimestamp(System.currentTimeMillis())
        assertTrue("Unexpected format: $result", expectedPattern.matches(result))
    }

    @Test
    fun `formatTimestamp handles epoch zero without crashing`() {
        val result = DateUtils.formatTimestamp(0L)
        assertTrue("Unexpected format: $result", expectedPattern.matches(result))
    }

    @Test
    fun `formatTimestamp is deterministic for the same input`() {
        val timestamp = 1_700_000_000_000L
        assertTrue(DateUtils.formatTimestamp(timestamp) == DateUtils.formatTimestamp(timestamp))
    }
}
