package com.steph.frenchradio.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.ListenHistoryEntry
import com.steph.frenchradio.model.ListenStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreListenHistoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun createPrefs(): DataStoreAppPreferences {
        val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { tmpFolder.newFile("test_history.preferences_pb") }
        )
        return DataStoreAppPreferences(testDataStore)
    }

    private fun entry(
        url: String,
        positionMs: Long = 0L,
        durationMs: Long = 600_000L,
        completed: Boolean = false,
        at: Long = 1_000L,
    ) = ListenHistoryEntry(
        episodeAudioUrl = url, episodeTitle = "Ep $url", channelId = "c1", channelName = "Pod",
        channelArtwork = "", feedUrl = "http://feed.xml", positionMs = positionMs,
        durationMs = durationMs, completed = completed, lastListenedAt = at,
    )

    @Test
    fun `initially empty`() = runTest {
        assertTrue(createPrefs().listenHistory.first().isEmpty())
    }

    @Test
    fun `recordListen stores and sorts newest first`() = runTest {
        val prefs = createPrefs()
        prefs.recordListen(entry("a", at = 1_000L))
        prefs.recordListen(entry("b", at = 3_000L))
        prefs.recordListen(entry("c", at = 2_000L))
        assertEquals(listOf("b", "c", "a"), prefs.listenHistory.first().map { it.episodeAudioUrl })
    }

    @Test
    fun `recordListen upserts by audio url`() = runTest {
        val prefs = createPrefs()
        prefs.recordListen(entry("a", positionMs = 10_000L, at = 1_000L))
        prefs.recordListen(entry("a", positionMs = 20_000L, at = 2_000L))
        val all = prefs.listenHistory.first()
        assertEquals(1, all.size)
        assertEquals(20_000L, all[0].positionMs)
        assertEquals(2_000L, all[0].lastListenedAt)
    }

    @Test
    fun `start event does not erase known position`() = runTest {
        val prefs = createPrefs()
        prefs.recordListen(entry("a", positionMs = 120_000L, at = 1_000L))
        prefs.recordListen(entry("a", positionMs = 0L, at = 2_000L))
        assertEquals(120_000L, prefs.listenHistory.first()[0].positionMs)
    }

    @Test
    fun `completed is sticky`() = runTest {
        val prefs = createPrefs()
        prefs.recordListen(entry("a", completed = true, at = 1_000L))
        prefs.recordListen(entry("a", positionMs = 5_000L, at = 2_000L))
        val e = prefs.listenHistory.first()[0]
        assertTrue(e.completed)
        assertEquals(ListenStatus.PLAYED, e.status)
    }

    @Test
    fun `removeListenHistory and clearListenHistory`() = runTest {
        val prefs = createPrefs()
        prefs.recordListen(entry("a"))
        prefs.recordListen(entry("b"))
        prefs.removeListenHistory("a")
        assertEquals(listOf("b"), prefs.listenHistory.first().map { it.episodeAudioUrl })
        prefs.clearListenHistory()
        assertTrue(prefs.listenHistory.first().isEmpty())
    }

    @Test
    fun `history survives episode progress removal`() = runTest {
        val prefs = createPrefs()
        prefs.recordListen(entry("a", completed = true))
        prefs.removeEpisodeProgress("a")
        assertEquals(1, prefs.listenHistory.first().size)
    }

    @Test
    fun `history is seeded from existing episode progress on first use`() = runTest {
        val prefs = createPrefs()
        prefs.saveEpisodeProgress(EpisodeProgress(
            episodeAudioUrl = "old", episodeTitle = "Old", channelId = "c", channelName = "Pod",
            channelArtwork = "", feedUrl = "http://feed.xml", positionMs = 30_000L,
            durationMs = 600_000L, updatedAt = 500L,
        ))
        val seeded = prefs.listenHistory.first()
        assertEquals("old", seeded.single().episodeAudioUrl)
        assertEquals(30_000L, seeded.single().positionMs)

        prefs.recordListen(entry("new", at = 1_000L))
        assertEquals(listOf("new", "old"), prefs.listenHistory.first().map { it.episodeAudioUrl })
    }

    @Test
    fun `entry stored at 95 percent without end is in progress at its position`() = runTest {
        // Migration case: marked "Lu" under the old 90 % rule, never completed
        val prefs = createPrefs()
        prefs.recordListen(entry("a", positionMs = 570_000L))
        val e = prefs.listenHistory.first()[0]
        assertEquals(ListenStatus.STARTED, e.status)
        assertEquals(570_000L, e.positionMs)
    }

    // --- Status rule ---

    @Test
    fun `status is STARTED below 98 percent and PLAYED at or above`() {
        assertEquals(ListenStatus.STARTED, entry("a", positionMs = 540_000L).status) // 90 %
        assertEquals(ListenStatus.STARTED, entry("a", positionMs = 587_999L).status)
        assertEquals(ListenStatus.PLAYED, entry("a", positionMs = 588_000L).status) // 98 %
        assertEquals(ListenStatus.PLAYED, entry("a", completed = true).status)
        assertEquals(ListenStatus.STARTED, entry("a", positionMs = 5_000L, durationMs = 0L).status)
    }
}
