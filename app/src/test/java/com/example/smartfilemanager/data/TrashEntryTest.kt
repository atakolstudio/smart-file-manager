package com.example.smartfilemanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrashEntryTest {

    @Test
    fun `encode then decode returns an equivalent entry`() {
        val original = TrashEntry(
            originalPath = "/storage/emulated/0/Download/photo.jpg",
            trashPath = "/data/data/com.example.smartfilemanager/files/trash/12345_photo.jpg",
            deletedAt = 1_700_000_000_000L,
            name = "photo.jpg"
        )

        val decoded = TrashEntry.decode(original.encode())

        assertEquals(original, decoded)
    }

    @Test
    fun `decode returns null for malformed input`() {
        assertNull(TrashEntry.decode("not-a-valid-entry"))
    }

    @Test
    fun `decode returns null when deletedAt is not numeric`() {
        val malformed = listOf("path1", "path2", "not-a-number", "name").joinToString(":::")
        assertNull(TrashEntry.decode(malformed))
    }
}
