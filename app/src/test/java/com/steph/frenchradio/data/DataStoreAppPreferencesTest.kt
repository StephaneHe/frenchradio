package com.steph.frenchradio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.steph.frenchradio.model.EpisodeProgress
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

    // --- Episode Progress ---

    @Test
    fun `initially no episode progress`() = runTest {
        val prefs = createPrefs()
        val progress = prefs.episodeProgressList.first()
        assertTrue(progress.isEmpty())
    }

    @Test
    fun `saveEpisodeProgress stores entry`() = runTest {
        val prefs = createPrefs()
        prefs.saveEpisodeProgress(makeProgress("http://ep1.mp3", "Episode 1", positionMs = 30000L))
        val progress = prefs.episodeProgressList.first()
        assertEquals(1, progress.size)
        assertEquals("Episode 1", progress[0].episodeTitle)
        assertEquals(30000L, progress[0].positionMs)
    }

    @Test
    fun `saveEpisodeProgress updates existing entry by audioUrl`() = runTest {
        val prefs = createPrefs()
        prefs.saveEpisodeProgress(makeProgress("http://ep1.mp3", "Episode 1", positionMs = 30000L, updatedAt = 1000L))
        prefs.saveEpisodeProgress(makeProgress("http://ep1.mp3", "Episode 1", positionMs = 60000L, updatedAt = 2000L))
        val progress = prefs.episodeProgressList.first()
        assertEquals(1, progress.size)
        assertEquals(60000L, progress[0].positionMs)
    }

    @Test
    fun `removeEpisodeProgress removes entry`() = runTest {
        val prefs = createPrefs()
        prefs.saveEpisodeProgress(makeProgress("http://ep1.mp3", "Episode 1", positionMs = 30000L, updatedAt = 1000L))
        prefs.saveEpisodeProgress(makeProgress("http://ep2.mp3", "Episode 2", positionMs = 45000L, updatedAt = 2000L))
        prefs.removeEpisodeProgress("http://ep1.mp3")
        val progress = prefs.episodeProgressList.first()
        assertEquals(1, progress.size)
        assertEquals("http://ep2.mp3", progress[0].episodeAudioUrl)
    }

    @Test
    fun `getEpisodeProgress returns correct entry`() = runTest {
        val prefs = createPrefs()
        prefs.saveEpisodeProgress(makeProgress("http://ep1.mp3", "Episode 1", positionMs = 30000L, updatedAt = 1000L))
        prefs.saveEpisodeProgress(makeProgress("http://ep2.mp3", "Episode 2", positionMs = 45000L, updatedAt = 2000L))
        val found = prefs.getEpisodeProgress("http://ep2.mp3")
        assertNotNull(found)
        assertEquals(45000L, found!!.positionMs)
        val notFound = prefs.getEpisodeProgress("http://missing.mp3")
        assertNull(notFound)
    }

    @Test
    fun `episode progress limited to 100 entries`() = runTest {
        val prefs = createPrefs()
        for (i in 1..105) {
            prefs.saveEpisodeProgress(makeProgress("http://ep$i.mp3", "Episode $i", positionMs = i * 1000L, updatedAt = i.toLong()))
        }
        val progress = prefs.episodeProgressList.first()
        assertEquals(100, progress.size)
        assertEquals("http://ep105.mp3", progress[0].episodeAudioUrl)
    }

    @Test
    fun `episode progress sorted by updatedAt descending`() = runTest {
        val prefs = createPrefs()
        prefs.saveEpisodeProgress(makeProgress("http://ep1.mp3", "Old", positionMs = 1000L, updatedAt = 1000L))
        prefs.saveEpisodeProgress(makeProgress("http://ep2.mp3", "New", positionMs = 2000L, updatedAt = 3000L))
        prefs.saveEpisodeProgress(makeProgress("http://ep3.mp3", "Mid", positionMs = 3000L, updatedAt = 2000L))
        val progress = prefs.episodeProgressList.first()
        assertEquals(listOf("http://ep2.mp3", "http://ep3.mp3", "http://ep1.mp3"), progress.map { it.episodeAudioUrl })
    }

    // --- Helpers ---

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

    private fun makeProgress(
        audioUrl: String,
        title: String,
        positionMs: Long = 0L,
        updatedAt: Long = 0L,
    ) = EpisodeProgress(
        episodeAudioUrl = audioUrl,
        episodeTitle = title,
        channelId = "ch1",
        channelName = "Channel",
        channelArtwork = "https://example.com/art.jpg",
        feedUrl = "https://example.com/feed.xml",
        positionMs = positionMs,
        durationMs = 3600000L,
        updatedAt = updatedAt,
    )
}
