package com.steph.frenchradio.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steph.frenchradio.data.AppPreferences
import com.steph.frenchradio.model.RadioStation
import com.steph.frenchradio.model.extractGenres
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

data class RadioListUiState(
    val stations: List<RadioStation> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedGenres: Set<String> = emptySet(),
    val allGenres: List<String> = emptyList(),
)

class RadioListViewModel(
    stations: List<RadioStation>,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RadioListUiState(
            stations = stations.sortedBy { it.name },
            allGenres = extractGenres(stations),
        )
    )
    val uiState: StateFlow<RadioListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.favoriteStationIds.collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onGenreToggled(genre: String) {
        _uiState.update { state ->
            val current = state.selectedGenres
            val updated = if (genre in current) current - genre else current + genre
            state.copy(selectedGenres = updated)
        }
    }

    fun onToggleFavorite(stationId: String) {
        viewModelScope.launch {
            prefs.toggleFavorite(stationId)
        }
    }

    /**
     * Returns stations filtered by search query and selected genres,
     * with favorites sorted first.
     */
    fun filteredStations(): List<RadioStation> {
        val state = _uiState.value
        val query = stripAccents(state.searchQuery.lowercase())

        return state.stations
            .filter { station ->
                // Name filter
                val nameMatch = query.isEmpty() ||
                    stripAccents(station.name.lowercase()).contains(query)
                // Genre filter (OR logic)
                val genreMatch = state.selectedGenres.isEmpty() ||
                    station.genres.any { it in state.selectedGenres }
                nameMatch && genreMatch
            }
            .sortedWith(
                compareByDescending<RadioStation> { it.id in state.favorites }
                    .thenBy { it.name }
            )
    }

    companion object {
        fun stripAccents(input: String): String {
            val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
        }
    }
}
