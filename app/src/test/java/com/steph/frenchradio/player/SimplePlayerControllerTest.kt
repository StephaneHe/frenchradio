package com.steph.frenchradio.player

import com.steph.frenchradio.data.AppPreferences
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.ListenHistoryEntry
import com.steph.frenchradio.model.ListenStatus
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import com.steph.frenchradio.model.RadioStation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SimplePlayerControllerTest {

    private lateinit var engine: FakeAudioEngine
    private lateinit var controller: SimplePlayerController

    @Before
    fun setup() {
        engine = FakeAudioEngine()
        controller = SimplePlayerController(engine)
    }

    // --- Radio ---

    @Test
    fun `playRadio updates state with station info and starts buffering`() {
        val station = makeStation("fip", "FIP", listOf("Jazz", "Rock"))
        controller.playRadio(station)

        val state = controller.playerState.value
        assertEquals("FIP", state.currentTitle)
        assertEquals("Jazz, Rock", state.currentSubtitle)
        assertEquals("fip", state.currentArtwork)
        assertTrue(state.isRadio)
        assertTrue(state.isBuffering)
        assertEquals("http://test/fip", engine.lastPreparedUrl)
    }

    @Test
    fun `playRadio then onReady sets isPlaying true`() {
        controller.playRadio(makeStation("fip", "FIP"))
        engine.simulateReady()

        val state = controller.playerState.value
        assertTrue(state.isPlaying)
        assertFalse(state.isBuffering)
    }

    // --- Podcast ---

    @Test
    fun `playPodcast updates state with episode info`() {
        val episode = makePodcastEpisode("Episode 1", 1800)
        val channel = makePodcastChannel("pod1", "My Podcast")
        controller.playPodcast(episode, channel)

        val state = controller.playerState.value
        assertEquals("Episode 1", state.currentTitle)
        assertEquals("My Podcast", state.currentSubtitle)
        assertFalse(state.isRadio)
        assertEquals(1800000L, state.durationMs)
        assertTrue(state.isBuffering)
    }

    @Test
    fun `isRadio is true for radio and false for podcast`() {
        controller.playRadio(makeStation("nrj", "NRJ"))
        assertTrue(controller.playerState.value.isRadio)

        controller.playPodcast(
            makePodcastEpisode("Ep", 60),
            makePodcastChannel("p", "Pod")
        )
        assertFalse(controller.playerState.value.isRadio)
    }

    // --- Controls ---

    @Test
    fun `pause sets isPlaying to false`() {
        controller.playRadio(makeStation("fip", "FIP"))
        engine.simulateReady()
        assertTrue(controller.playerState.value.isPlaying)

        controller.pause()
        assertFalse(controller.playerState.value.isPlaying)
        assertTrue(engine.isPaused)
    }

    @Test
    fun `resume sets isPlaying to true`() {
        controller.playRadio(makeStation("fip", "FIP"))
        engine.simulateReady()
        controller.pause()

        controller.resume()
        assertTrue(controller.playerState.value.isPlaying)
    }

    @Test
    fun `stop resets state to default`() {
        controller.playRadio(makeStation("fip", "FIP"))
        engine.simulateReady()

        controller.stop()
        val state = controller.playerState.value
        assertFalse(state.isPlaying)
        assertFalse(state.isBuffering)
        assertEquals("", state.currentTitle)
        assertNull(state.currentStreamUrl)
        assertTrue(engine.isStopped)
    }

    @Test
    fun `seekTo updates position`() {
        controller.playPodcast(
            makePodcastEpisode("Ep", 600),
            makePodcastChannel("p", "Pod")
        )
        engine.simulateReady()

        controller.seekTo(30000L)
        assertEquals(30000L, controller.playerState.value.positionMs)
        assertEquals(30000L, engine.lastSeekPosition)
    }

    // --- Error ---

    @Test
    fun `error state is set on stream failure`() {
        controller.playRadio(makeStation("fip", "FIP"))
        engine.simulateError("Connection failed")

        val state = controller.playerState.value
        assertTrue(state.isError)
        assertEquals("Connection failed", state.errorMessage)
        assertFalse(state.isPlaying)
        assertFalse(state.isBuffering)
    }

    @Test
    fun `resume after error clears error state`() {
        controller.playRadio(makeStation("fip", "FIP"))
        engine.simulateError("Connection failed")

        controller.resume()
        val state = controller.playerState.value
        assertFalse(state.isError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onEnded sets isPlaying false`() {
        controller.playPodcast(
            makePodcastEpisode("Ep", 60),
            makePodcastChannel("p", "Pod")
        )
        engine.simulateReady()
        engine.simulateEnded()

        assertFalse(controller.playerState.value.isPlaying)
    }

    @Test
    fun `playing new station stops previous`() {
        controller.playRadio(makeStation("fip", "FIP"))
        engine.simulateReady()

        controller.playRadio(makeStation("nrj", "NRJ"))
        assertEquals("NRJ", controller.playerState.value.currentTitle)
        assertTrue(engine.stopCount >= 1)
    }


    // --- Podcast Progress Save (A5) ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `pause saves position for podcast`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        val testScope = this
        val ctrl = SimplePlayerController(engine, fakePrefs, testScope)

        val ep = makePodcastEpisode("Ep 1", 600)
        val ch = makePodcastChannel("pod1", "My Podcast")
        ctrl.playPodcast(ep, ch)
        engine.currentPositionMs = 45000L
        engine.durationMs = 600000L
        engine.simulateReady()

        ctrl.pause()
        advanceUntilIdle()

        assertEquals(1, fakePrefs.savedProgress.size)
        assertEquals("http://test/ep.mp3", fakePrefs.savedProgress[0].episodeAudioUrl)
        assertEquals(45000L, fakePrefs.savedProgress[0].positionMs)
        assertEquals("pod1", fakePrefs.savedProgress[0].channelId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `pause does NOT save position for radio`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        val testScope = this
        val ctrl = SimplePlayerController(engine, fakePrefs, testScope)

        ctrl.playRadio(makeStation("fip", "FIP"))
        engine.simulateReady()
        ctrl.pause()
        advanceUntilIdle()

        assertTrue(fakePrefs.savedProgress.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `playPodcast resumes at saved position`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        fakePrefs.savedProgress.add(EpisodeProgress(
            episodeAudioUrl = "http://test/ep.mp3",
            episodeTitle = "Ep 1",
            channelId = "pod1",
            channelName = "My Podcast",
            channelArtwork = "http://test/art.jpg",
            feedUrl = "http://test/feed.xml",
            positionMs = 120000L,
            durationMs = 600000L,
        ))
        val testScope = this
        val ctrl = SimplePlayerController(engine, fakePrefs, testScope)

        ctrl.playPodcast(makePodcastEpisode("Ep 1", 600), makePodcastChannel("pod1", "My Podcast"))
        advanceUntilIdle()

        assertEquals(120000L, engine.lastSeekPosition)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onEnded removes progress entry`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        val testScope = this
        val ctrl = SimplePlayerController(engine, fakePrefs, testScope)

        ctrl.playPodcast(makePodcastEpisode("Ep 1", 600), makePodcastChannel("pod1", "My Podcast"))
        engine.simulateReady()
        engine.simulateEnded()
        advanceUntilIdle()

        assertEquals("http://test/ep.mp3", fakePrefs.lastRemovedProgressUrl)
    }

    // --- Listening history ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `playPodcast records a started listen in history`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        val ctrl = SimplePlayerController(engine, fakePrefs, this)

        ctrl.playPodcast(makePodcastEpisode("Ep 1", 600), makePodcastChannel("pod1", "My Podcast"))
        advanceUntilIdle()

        val entry = fakePrefs.recordedListens.single()
        assertEquals("http://test/ep.mp3", entry.episodeAudioUrl)
        assertEquals("My Podcast", entry.channelName)
        assertEquals("http://test/feed.xml", entry.feedUrl)
        assertFalse(entry.completed)
        assertTrue(entry.lastListenedAt > 0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `pause records position in history`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        val ctrl = SimplePlayerController(engine, fakePrefs, this)

        ctrl.playPodcast(makePodcastEpisode("Ep 1", 600), makePodcastChannel("pod1", "My Podcast"))
        engine.currentPositionMs = 45000L
        engine.durationMs = 600000L
        engine.simulateReady()
        ctrl.pause()
        advanceUntilIdle()

        val last = fakePrefs.recordedListens.last()
        assertEquals(45000L, last.positionMs)
        assertEquals(600000L, last.durationMs)
        assertEquals(ListenStatus.STARTED, last.status)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onEnded records completed listen`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        val ctrl = SimplePlayerController(engine, fakePrefs, this)

        ctrl.playPodcast(makePodcastEpisode("Ep 1", 600), makePodcastChannel("pod1", "My Podcast"))
        engine.simulateReady()
        engine.simulateEnded()
        advanceUntilIdle()

        val last = fakePrefs.recordedListens.last()
        assertTrue(last.completed)
        assertEquals(ListenStatus.PLAYED, last.status)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `radio does not record history`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        val ctrl = SimplePlayerController(engine, fakePrefs, this)

        ctrl.playRadio(makeStation("fip", "FIP"))
        engine.simulateReady()
        ctrl.pause()
        ctrl.stop()
        advanceUntilIdle()

        assertTrue(fakePrefs.recordedListens.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `playPodcast restarts from zero when saved position is past played threshold`() = runTest {
        val fakePrefs = FakePlayerPrefs()
        fakePrefs.savedProgress.add(EpisodeProgress(
            episodeAudioUrl = "http://test/ep.mp3",
            episodeTitle = "Ep 1",
            channelId = "pod1",
            channelName = "My Podcast",
            channelArtwork = "http://test/art.jpg",
            feedUrl = "http://test/feed.xml",
            positionMs = 580000L,
            durationMs = 600000L,
        ))
        val ctrl = SimplePlayerController(engine, fakePrefs, this)

        ctrl.playPodcast(makePodcastEpisode("Ep 1", 600), makePodcastChannel("pod1", "My Podcast"))
        advanceUntilIdle()

        assertEquals(0L, engine.lastSeekPosition)
    }

    // --- Helpers ---

    private fun makeStation(id: String, name: String, genres: List<String> = emptyList()) =
        RadioStation(id = id, name = name, streamUrl = "http://test/$id",
            logo = id, genres = genres, color = "#000000")

    private fun makePodcastEpisode(title: String, durationSec: Int) =
        PodcastEpisode(title = title, description = "", audioUrl = "http://test/ep.mp3",
            publishDate = "2025-01-01", durationSeconds = durationSec)

    private fun makePodcastChannel(id: String, name: String) =
        PodcastChannel(id = id, name = name, author = "Author",
            artworkUrl = "http://test/art.jpg", feedUrl = "http://test/feed.xml")
}

/**
 * Fake AudioEngine for unit testing.
 */
class FakeAudioEngine : AudioEngine {
    var lastPreparedUrl: String? = null
    var isPaused = false
    var isStopped = false
    var stopCount = 0
    var lastSeekPosition = 0L
    private var _isPlaying = false
    private var listener: AudioEngine.Listener? = null

    override fun prepare(url: String) { lastPreparedUrl = url; isStopped = false }
    override fun play() { _isPlaying = true; isPaused = false }
    override fun pause() { _isPlaying = false; isPaused = true }
    override fun stop() { _isPlaying = false; isStopped = true; stopCount++ }
    override fun seekTo(positionMs: Long) { lastSeekPosition = positionMs }
    override fun release() { stop() }
    override val isPlaying: Boolean get() = _isPlaying
    override var currentPositionMs: Long = 0L
    override var durationMs: Long = 0L

    override fun setListener(listener: AudioEngine.Listener) { this.listener = listener }

    fun simulateReady() { listener?.onReady() }
    fun simulateBuffering() { listener?.onBuffering() }
    fun simulateError(msg: String) { listener?.onError(msg) }
    fun simulateEnded() { listener?.onEnded() }
}

/**
 * Fake AppPreferences for player progress tests.
 */
class FakePlayerPrefs : AppPreferences {
    val _favoriteFlow = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteStationIds: Flow<Set<String>> = _favoriteFlow
    override suspend fun toggleFavorite(stationId: String) {}

    val _historyFlow = MutableStateFlow<List<PodcastChannel>>(emptyList())
    override val podcastHistory: Flow<List<PodcastChannel>> = _historyFlow
    override suspend fun addToHistory(channel: PodcastChannel) {}
    override suspend fun removeFromHistory(channelId: String) {}
    override suspend fun clearHistory() {}

    val savedProgress = mutableListOf<EpisodeProgress>()
    var lastRemovedProgressUrl: String? = null
    val _progressFlow = MutableStateFlow<List<EpisodeProgress>>(emptyList())
    override val episodeProgressList: Flow<List<EpisodeProgress>> = _progressFlow

    override suspend fun saveEpisodeProgress(progress: EpisodeProgress) {
        savedProgress.removeAll { it.episodeAudioUrl == progress.episodeAudioUrl }
        savedProgress.add(progress)
    }
    override suspend fun removeEpisodeProgress(audioUrl: String) {
        lastRemovedProgressUrl = audioUrl
        savedProgress.removeAll { it.episodeAudioUrl == audioUrl }
    }
    override suspend fun getEpisodeProgress(audioUrl: String): EpisodeProgress? {
        return savedProgress.find { it.episodeAudioUrl == audioUrl }
    }

    val recordedListens = mutableListOf<ListenHistoryEntry>()
    private val _listenFlow = MutableStateFlow<List<ListenHistoryEntry>>(emptyList())
    override val listenHistory: Flow<List<ListenHistoryEntry>> = _listenFlow
    override suspend fun recordListen(entry: ListenHistoryEntry) { recordedListens.add(entry) }
    override suspend fun removeListenHistory(audioUrl: String) {}
    override suspend fun clearListenHistory() {}

    private val _boostFlow = MutableStateFlow(0)
    override val audioBoostPercent: Flow<Int> = _boostFlow
    override suspend fun setAudioBoostPercent(percent: Int) { _boostFlow.value = percent }
}
