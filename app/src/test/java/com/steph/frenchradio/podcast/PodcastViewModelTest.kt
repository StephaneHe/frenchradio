package com.steph.frenchradio.podcast

import com.steph.frenchradio.data.AppPreferences
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeSearchApi: FakePodcastSearchApi
    private lateinit var fakeFeedParser: FakeFeedParser
    private lateinit var fakeUrlFetcher: FakeUrlFetcher
    private lateinit var fakePrefs: FakePodcastPrefs
    private lateinit var viewModel: PodcastViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSearchApi = FakePodcastSearchApi()
        fakeFeedParser = FakeFeedParser()
        fakeUrlFetcher = FakeUrlFetcher()
        fakePrefs = FakePodcastPrefs()
        viewModel = PodcastViewModel(fakeSearchApi, fakeFeedParser, fakeUrlFetcher, fakePrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search returns parsed results`() = runTest {
        fakeSearchApi.results = listOf(makeChannel("1", "Podcast A"))
        viewModel.onSearchQueryChanged("test")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.searchResults.size)
        assertEquals("Podcast A", state.searchResults[0].name)
        assertFalse(state.isSearching)
    }

    @Test
    fun `empty search shows no results`() = runTest {
        fakeSearchApi.results = listOf(makeChannel("1", "Podcast A"))
        viewModel.onSearchQueryChanged("test")
        advanceUntilIdle()
        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun `search error sets error state`() = runTest {
        fakeSearchApi.shouldThrow = true
        viewModel.onSearchQueryChanged("test")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.searchError)
        assertFalse(state.isSearching)
    }

    @Test
    fun `channel selection loads episodes from RSS`() = runTest {
        val channel = makeChannel("1", "Podcast A")
        fakeUrlFetcher.content = "<rss>...</rss>"
        fakeFeedParser.episodes = listOf(
            PodcastEpisode("Ep 1", "desc", "http://audio.mp3", "2025-01-01", 600)
        )

        viewModel.onChannelSelected(channel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(channel, state.selectedChannel)
        assertEquals(1, state.episodes.size)
        assertEquals("Ep 1", state.episodes[0].title)
        assertFalse(state.isLoadingEpisodes)
    }

    @Test
    fun `episode error on invalid feed`() = runTest {
        fakeUrlFetcher.shouldThrow = true
        viewModel.onChannelSelected(makeChannel("1", "Bad"))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.episodeError)
        assertFalse(viewModel.uiState.value.isLoadingEpisodes)
    }

    @Test
    fun `onPlayEpisode adds channel to history`() = runTest {
        val channel = makeChannel("1", "Podcast A")
        viewModel.onChannelSelected(channel)
        advanceUntilIdle()

        val episode = PodcastEpisode("Ep", "", "http://a.mp3", "", 60)
        viewModel.onPlayEpisode(episode)
        advanceUntilIdle()

        assertEquals(1, fakePrefs.historyAdded.size)
        assertEquals("1", fakePrefs.historyAdded[0].id)
    }

    @Test
    fun `onRemoveFromHistory removes channel`() = runTest {
        viewModel.onRemoveFromHistory("42")
        advanceUntilIdle()

        assertEquals("42", fakePrefs.lastRemoved)
    }

    @Test
    fun `onBackFromEpisodes clears selection`() {
        viewModel.onBackFromEpisodes()
        val state = viewModel.uiState.value
        assertNull(state.selectedChannel)
        assertTrue(state.episodes.isEmpty())
    }

    @Test
    fun `history from prefs is reflected in state`() = runTest {
        val channel = makeChannel("1", "From History")
        fakePrefs._historyFlow.value = listOf(channel)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.history.size)
        assertEquals("From History", viewModel.uiState.value.history[0].name)
    }

    // --- Helpers ---

    private fun makeChannel(id: String, name: String) = PodcastChannel(
        id = id, name = name, author = "Author",
        artworkUrl = "https://img.com/art.jpg", feedUrl = "https://feed.com/rss.xml",
    )
}

// --- Fakes ---

class FakePodcastSearchApi : PodcastSearchApi {
    var results: List<PodcastChannel> = emptyList()
    var shouldThrow = false

    override suspend fun search(query: String, limit: Int): List<PodcastChannel> {
        if (shouldThrow) throw RuntimeException("Search failed")
        return results
    }
}

class FakeFeedParser : FeedParser {
    var episodes: List<PodcastEpisode> = emptyList()

    override fun parse(xml: String): List<PodcastEpisode> = episodes
}

class FakeUrlFetcher : UrlFetcher {
    var content: String = ""
    var shouldThrow = false

    override suspend fun fetch(url: String): String {
        if (shouldThrow) throw RuntimeException("Fetch failed")
        return content
    }
}

class FakePodcastPrefs : AppPreferences {
    val _favoriteFlow = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteStationIds: Flow<Set<String>> = _favoriteFlow
    override suspend fun toggleFavorite(stationId: String) {}

    val _historyFlow = MutableStateFlow<List<PodcastChannel>>(emptyList())
    override val podcastHistory: Flow<List<PodcastChannel>> = _historyFlow

    val historyAdded = mutableListOf<PodcastChannel>()
    var lastRemoved: String? = null

    override suspend fun addToHistory(channel: PodcastChannel) {
        historyAdded.add(channel)
    }
    override suspend fun removeFromHistory(channelId: String) {
        lastRemoved = channelId
    }
    override suspend fun clearHistory() {}
}
