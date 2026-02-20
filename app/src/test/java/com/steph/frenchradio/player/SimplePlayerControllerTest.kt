package com.steph.frenchradio.player

import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import com.steph.frenchradio.model.RadioStation
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
    override val currentPositionMs: Long = 0L
    override val durationMs: Long = 0L

    override fun setListener(listener: AudioEngine.Listener) { this.listener = listener }

    fun simulateReady() { listener?.onReady() }
    fun simulateBuffering() { listener?.onBuffering() }
    fun simulateError(msg: String) { listener?.onError(msg) }
    fun simulateEnded() { listener?.onEnded() }
}
