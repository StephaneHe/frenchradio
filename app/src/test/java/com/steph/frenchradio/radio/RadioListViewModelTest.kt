package com.steph.frenchradio.radio

import com.steph.frenchradio.data.AppPreferences
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RadioListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: RadioListViewModel
    private lateinit var fakePrefs: FakeAppPreferences
    private lateinit var fakeWriter: FakeStationWriter

    private val testStations = listOf(
        makeStation("nrj", "NRJ", listOf("Hits", "Pop")),
        makeStation("fip", "FIP", listOf("Jazz", "Rock")),
        makeStation("rtl", "RTL", listOf("Généraliste", "Info")),
        makeStation("tsf_jazz", "TSF Jazz", listOf("Jazz")),
        makeStation("skyrock", "Skyrock", listOf("Rap", "Hip-Hop")),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakePrefs = FakeAppPreferences()
        fakeWriter = FakeStationWriter(testStations.toMutableList())
        viewModel = RadioListViewModel(testStations, fakePrefs, fakeWriter)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stations are sorted alphabetically`() {
        val names = viewModel.uiState.value.stations.map { it.name }
        assertEquals(listOf("FIP", "NRJ", "RTL", "Skyrock", "TSF Jazz"), names)
    }

    @Test
    fun `all genres extracted and sorted`() {
        val genres = viewModel.uiState.value.allGenres
        assertEquals(
            listOf("Généraliste", "Hip-Hop", "Hits", "Info", "Jazz", "Pop", "Rap", "Rock"),
            genres
        )
    }

    @Test
    fun `search filters by name case-insensitive`() {
        viewModel.onSearchQueryChanged("fip")
        val filtered = viewModel.filteredStations()
        assertEquals(1, filtered.size)
        assertEquals("FIP", filtered[0].name)
    }

    @Test
    fun `search is accent-insensitive`() {
        viewModel.onSearchQueryChanged("rtl")
        val filtered = viewModel.filteredStations()
        assertEquals(1, filtered.size)
        assertEquals("RTL", filtered[0].name)
    }

    @Test
    fun `genre filter shows only matching stations`() {
        viewModel.onGenreToggled("Jazz")
        val filtered = viewModel.filteredStations()
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { "Jazz" in it.genres })
    }

    @Test
    fun `multiple genre filters use OR logic`() {
        viewModel.onGenreToggled("Jazz")
        viewModel.onGenreToggled("Rap")
        val filtered = viewModel.filteredStations()
        assertEquals(3, filtered.size)
    }

    @Test
    fun `toggle genre off removes filter`() {
        viewModel.onGenreToggled("Jazz")
        viewModel.onGenreToggled("Jazz")
        val filtered = viewModel.filteredStations()
        assertEquals(5, filtered.size)
    }

    @Test
    fun `toggle favorite adds to favorites`() = runTest {
        viewModel.onToggleFavorite("fip")
        assertTrue(fakePrefs.favorites.contains("fip"))
    }

    @Test
    fun `toggle favorite twice removes from favorites`() = runTest {
        viewModel.onToggleFavorite("fip")
        viewModel.onToggleFavorite("fip")
        assertFalse(fakePrefs.favorites.contains("fip"))
    }

    @Test
    fun `favorites appear before non-favorites`() = runTest {
        fakePrefs.favorites.add("tsf_jazz")
        fakePrefs._favoriteFlow.value = fakePrefs.favorites.toSet()
        val all = viewModel.filteredStations()
        assertEquals("TSF Jazz", all[0].name)
    }

    @Test
    fun `search plus genre filter combine correctly`() {
        viewModel.onSearchQueryChanged("ts")
        viewModel.onGenreToggled("Jazz")
        val filtered = viewModel.filteredStations()
        assertEquals(1, filtered.size)
        assertEquals("TSF Jazz", filtered[0].name)
    }

    @Test
    fun `empty search shows all stations`() {
        viewModel.onSearchQueryChanged("xyz")
        viewModel.onSearchQueryChanged("")
        val filtered = viewModel.filteredStations()
        assertEquals(5, filtered.size)
    }

    @Test
    fun `no results when nothing matches`() {
        viewModel.onSearchQueryChanged("zzzzzz")
        val filtered = viewModel.filteredStations()
        assertTrue(filtered.isEmpty())
    }

    // --- B4: Station editor tests ---

    @Test
    fun `addStation adds to list and persists`() {
        val newStation = makeStation("nova", "Radio Nova", listOf("Éclectique"))
        viewModel.addStation(newStation)

        val state = viewModel.uiState.value
        assertEquals(6, state.stations.size)
        assertTrue(state.stations.any { it.id == "nova" })
        assertTrue(state.allGenres.contains("Éclectique"))
        assertTrue(fakeWriter.stations.any { it.id == "nova" })
    }

    @Test
    fun `updateStation modifies entry and persists`() {
        val updated = makeStation("fip", "FIP Updated", listOf("Jazz", "Electro"))
        viewModel.updateStation("fip", updated)

        val state = viewModel.uiState.value
        val fip = state.stations.find { it.id == "fip" }
        assertNotNull(fip)
        assertEquals("FIP Updated", fip!!.name)
        assertTrue(fip.genres.contains("Electro"))
        assertTrue(state.allGenres.contains("Electro"))
    }

    @Test
    fun `deleteStation removes entry and persists`() {
        viewModel.deleteStation("skyrock")

        val state = viewModel.uiState.value
        assertEquals(4, state.stations.size)
        assertFalse(state.stations.any { it.id == "skyrock" })
        assertFalse(fakeWriter.stations.any { it.id == "skyrock" })
    }

    // --- Helpers ---

    private fun makeStation(id: String, name: String, genres: List<String>) =
        RadioStation(
            id = id, name = name, streamUrl = "http://test/$id",
            logo = id, genres = genres, color = "#000000",
        )
}

/** Fake StationWriter that operates on an in-memory list. */
class FakeStationWriter(val stations: MutableList<RadioStation>) : StationWriter {
    override fun addStation(station: RadioStation): List<RadioStation> {
        stations.add(station)
        return stations.sortedBy { it.name }
    }

    override fun updateStation(stationId: String, station: RadioStation): List<RadioStation> {
        val index = stations.indexOfFirst { it.id == stationId }
        if (index >= 0) stations[index] = station
        return stations.sortedBy { it.name }
    }

    override fun deleteStation(stationId: String): List<RadioStation> {
        stations.removeAll { it.id == stationId }
        return stations.sortedBy { it.name }
    }
}

/** Fake AppPreferences for testing without DataStore. */
class FakeAppPreferences : AppPreferences {
    val favorites = mutableSetOf<String>()
    val _favoriteFlow = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteStationIds: Flow<Set<String>> = _favoriteFlow

    override suspend fun toggleFavorite(stationId: String) {
        if (stationId in favorites) favorites.remove(stationId)
        else favorites.add(stationId)
        _favoriteFlow.value = favorites.toSet()
    }

    private val _historyFlow = MutableStateFlow<List<PodcastChannel>>(emptyList())
    override val podcastHistory: Flow<List<PodcastChannel>> = _historyFlow
    override suspend fun addToHistory(channel: PodcastChannel) {}
    override suspend fun removeFromHistory(channelId: String) {}
    override suspend fun clearHistory() {}

    // Episode progress (stub — not needed for radio tests)
    private val _progressFlow = MutableStateFlow<List<EpisodeProgress>>(emptyList())
    override val episodeProgressList: Flow<List<EpisodeProgress>> = _progressFlow
    override suspend fun saveEpisodeProgress(progress: EpisodeProgress) {}
    override suspend fun removeEpisodeProgress(audioUrl: String) {}
    override suspend fun getEpisodeProgress(audioUrl: String): EpisodeProgress? = null
}
