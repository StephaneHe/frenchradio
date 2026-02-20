package com.steph.frenchradio.radio

import com.steph.frenchradio.data.AppPreferences
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
        viewModel = RadioListViewModel(testStations, fakePrefs)
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

    // --- Helpers ---

    private fun makeStation(id: String, name: String, genres: List<String>) =
        RadioStation(
            id = id, name = name, streamUrl = "http://test/$id",
            logo = id, genres = genres, color = "#000000",
        )
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
}
