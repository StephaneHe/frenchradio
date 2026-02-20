package com.steph.frenchradio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.steph.frenchradio.model.PodcastChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreAppPreferencesTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun createPrefs(): DataStoreAppPreferences {
        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { tmpFolder.newFile("test_prefs.preferences_pb") }
        )
        return DataStoreAppPreferences(testDataStore)
    }

    // --- Radio Favorites ---

    @Test
    fun `initially no favorites`() = runTest {
        val prefs = createPrefs()
        val favorites = prefs.favoriteStationIds.first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `toggle favorite adds station`() = runTest {
        val prefs = createPrefs()
        prefs.toggleFavorite("fip")
        val favorites = prefs.favoriteStationIds.first()
        assertTrue(favorites.contains("fip"))
    }

    @Test
    fun `toggle favorite twice removes station`() = runTest {
        val prefs = createPrefs()
        prefs.toggleFavorite("fip")
        prefs.toggleFavorite("fip")
        val favorites = prefs.favoriteStationIds.first()
        assertFalse(favorites.contains("fip"))
    }

    @Test
    fun `multiple favorites persisted`() = runTest {
        val prefs = createPrefs()
        prefs.toggleFavorite("fip")
        prefs.toggleFavorite("nrj")
        prefs.toggleFavorite("rtl")
        val favorites = prefs.favoriteStationIds.first()
        assertEquals(setOf("fip", "nrj", "rtl"), favorites)
    }

    // --- Podcast History ---

    @Test
    fun `initially no podcast history`() = runTest {
        val prefs = createPrefs()
        val history = prefs.podcastHistory.first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `addToHistory adds channel`() = runTest {
        val prefs = createPrefs()
        val channel = makeChannel("1", "Podcast A")
        prefs.addToHistory(channel)
        val history = prefs.podcastHistory.first()
        assertEquals(1, history.size)
        assertEquals("Podcast A", history[0].name)
    }

    @Test
    fun `addToHistory updates existing channel`() = runTest {
        val prefs = createPrefs()
        val channel = makeChannel("1", "Podcast A", lastPlayedAt = 1000L)
        prefs.addToHistory(channel)
        val updated = makeChannel("1", "Podcast A", lastPlayedAt = 2000L)
        prefs.addToHistory(updated)
        val history = prefs.podcastHistory.first()
        assertEquals(1, history.size)
        assertEquals(2000L, history[0].lastPlayedAt)
    }

    @Test
    fun `removeFromHistory removes channel`() = runTest {
        val prefs = createPrefs()
        prefs.addToHistory(makeChannel("1", "Podcast A", lastPlayedAt = 1000L))
        prefs.addToHistory(makeChannel("2", "Podcast B", lastPlayedAt = 2000L))
        prefs.removeFromHistory("1")
        val history = prefs.podcastHistory.first()
        assertEquals(1, history.size)
        assertEquals("2", history[0].id)
    }

    @Test
    fun `history sorted by lastPlayedAt descending`() = runTest {
        val prefs = createPrefs()
        prefs.addToHistory(makeChannel("1", "Old", lastPlayedAt = 1000L))
        prefs.addToHistory(makeChannel("2", "New", lastPlayedAt = 3000L))
        prefs.addToHistory(makeChannel("3", "Mid", lastPlayedAt = 2000L))
        val history = prefs.podcastHistory.first()
        assertEquals(listOf("2", "3", "1"), history.map { it.id })
    }

    @Test
    fun `history limited to 50 entries`() = runTest {
        val prefs = createPrefs()
        for (i in 1..55) {
            prefs.addToHistory(makeChannel("$i", "Podcast $i", lastPlayedAt = i.toLong()))
        }
        val history = prefs.podcastHistory.first()
        assertEquals(50, history.size)
        assertEquals("55", history[0].id)
    }

    @Test
    fun `clearHistory removes all`() = runTest {
        val prefs = createPrefs()
        prefs.addToHistory(makeChannel("1", "A", lastPlayedAt = 1000L))
        prefs.addToHistory(makeChannel("2", "B", lastPlayedAt = 2000L))
        prefs.clearHistory()
        val history = prefs.podcastHistory.first()
        assertTrue(history.isEmpty())
    }

    private fun makeChannel(
        id: String,
        name: String,
        lastPlayedAt: Long = 0L,
    ) = PodcastChannel(
        id = id,
        name = name,
        author = "Author",
        artworkUrl = "https://example.com/art.jpg",
        feedUrl = "https://example.com/feed.xml",
        lastPlayedAt = lastPlayedAt,
    )
}
