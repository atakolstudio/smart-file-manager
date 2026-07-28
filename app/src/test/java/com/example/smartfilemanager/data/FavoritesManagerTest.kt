package com.example.smartfilemanager.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritesManagerTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val favoritesManager = FavoritesManager(context)

    @Test
    fun `a path is not a favorite by default`() = runBlocking {
        assertFalse(favoritesManager.isFavorite("/storage/emulated/0/DCIM/photo.jpg"))
    }

    @Test
    fun `toggleFavorite adds a path that is not yet favorited`() = runBlocking {
        val path = "/storage/emulated/0/DCIM/photo.jpg"
        favoritesManager.toggleFavorite(path)

        assertTrue(favoritesManager.isFavorite(path))
        assertTrue(favoritesManager.favoritePaths.first().contains(path))
    }

    @Test
    fun `toggleFavorite removes a path that is already favorited`() = runBlocking {
        val path = "/storage/emulated/0/DCIM/photo.jpg"
        favoritesManager.toggleFavorite(path)
        favoritesManager.toggleFavorite(path)

        assertFalse(favoritesManager.isFavorite(path))
    }

    @Test
    fun `removeFavorite removes only the specified path`() = runBlocking {
        val pathA = "/a.txt"
        val pathB = "/b.txt"
        favoritesManager.toggleFavorite(pathA)
        favoritesManager.toggleFavorite(pathB)

        favoritesManager.removeFavorite(pathA)

        assertFalse(favoritesManager.isFavorite(pathA))
        assertTrue(favoritesManager.isFavorite(pathB))
    }
}
