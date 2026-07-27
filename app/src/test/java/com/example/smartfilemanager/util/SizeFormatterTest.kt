package com.example.smartfilemanager.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SizeFormatterTest {

    @Test
    fun `zero bytes formats as 0 B`() {
        assertEquals("0 B", SizeFormatter.format(0))
    }

    @Test
    fun `negative bytes formats as 0 B`() {
        assertEquals("0 B", SizeFormatter.format(-100))
    }

    @Test
    fun `bytes under 1024 use B unit`() {
        assertEquals("512.0 B", SizeFormatter.format(512))
    }

    @Test
    fun `exactly 1024 bytes formats as KB`() {
        assertEquals("1.0 KB", SizeFormatter.format(1024))
    }

    @Test
    fun `1536 bytes formats as 1point5 KB`() {
        assertEquals("1.5 KB", SizeFormatter.format(1536))
    }

    @Test
    fun `one megabyte formats correctly`() {
        assertEquals("1.0 MB", SizeFormatter.format(1024L * 1024L))
    }

    @Test
    fun `one gigabyte formats correctly`() {
        assertEquals("1.0 GB", SizeFormatter.format(1024L * 1024L * 1024L))
    }

    @Test
    fun `one terabyte formats correctly`() {
        assertEquals("1.0 TB", SizeFormatter.format(1024L * 1024L * 1024L * 1024L))
    }

    @Test
    fun `values beyond terabyte still use TB unit`() {
        val result = SizeFormatter.format(1024L * 1024L * 1024L * 1024L * 1024L)
        assert(result.endsWith("TB")) { "Expected TB unit but was: $result" }
    }
}
