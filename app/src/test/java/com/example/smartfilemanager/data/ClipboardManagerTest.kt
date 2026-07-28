package com.example.smartfilemanager.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipboardManagerTest {

    @Test
    fun `initial state is empty`() {
        val manager = ClipboardManager()
        assertNull(manager.state.value)
    }

    @Test
    fun `copy sets state with COPY operation and given paths`() {
        val manager = ClipboardManager()
        manager.copy(listOf("/a.txt", "/b.txt"))

        val state = manager.state.value
        assertEquals(ClipboardOperation.COPY, state?.operation)
        assertEquals(listOf("/a.txt", "/b.txt"), state?.paths)
    }

    @Test
    fun `cut sets state with CUT operation`() {
        val manager = ClipboardManager()
        manager.cut(listOf("/a.txt"))

        assertEquals(ClipboardOperation.CUT, manager.state.value?.operation)
    }

    @Test
    fun `clear resets state to null`() {
        val manager = ClipboardManager()
        manager.copy(listOf("/a.txt"))
        manager.clear()

        assertNull(manager.state.value)
    }

    @Test
    fun `a second copy call replaces the previous clipboard content`() {
        val manager = ClipboardManager()
        manager.copy(listOf("/first.txt"))
        manager.copy(listOf("/second.txt"))

        assertEquals(listOf("/second.txt"), manager.state.value?.paths)
    }
}
