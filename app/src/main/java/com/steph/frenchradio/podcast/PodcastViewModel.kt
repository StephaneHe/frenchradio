package com.steph.frenchradio.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steph.frenchradio.data.AppPreferences
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PodcastUiState(
    val searchQuery: String = "",
    val searchResults: List<PodcastChannel> = emptyList(),
    val history: List<PodcastChannel> = emptyList(),
    val inProgress: List<EpisodeProgress> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val selectedChannel: PodcastChannel? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
    val isLoadingEpisodes: Boolean = false,
    val episodeError: String? = null,
)

class PodcastViewModel(
    private val searchApi: PodcastSearchApi,
    private val feedParser: FeedParser,
    private val urlFetcher: UrlFetcher,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.podcastHistory.collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
        viewModelScope.launch {
            prefs.episodeProgressList.collect { progress ->
                _uiState.update { it.copy(inProgress = progress) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), searchError = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(500) // debounce
            search()
        }
    }

    fun search() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            try {
                val results = searchApi.search(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = e.message ?: "Search failed",
                        searchResults = emptyList(),
                    )
                }
            }
        }
    }

    fun onChannelSelected(channel: PodcastChannel) {
        _uiState.update { it.copy(selectedChannel = channel, isLoadingEpisodes = true, episodeError = null, episodes = emptyList()) }
        viewModelScope.launch {
            try {
                val xml = urlFetcher.fetch(channel.feedUrl)
                val episodes = feedParser.parse(xml)
                _uiState.update { it.copy(episodes = episodes, isLoadingEpisodes = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingEpisodes = false,
                        episodeError = e.message ?: "Failed to load episodes",
                    )
                }
            }
        }
    }

    fun onBackFromEpisodes() {
        _uiState.update { it.copy(selectedChannel = null, episodes = emptyList(), episodeError = null) }
    }

    fun onPlayEpisode(episode: PodcastEpisode) {
        val channel = _uiState.value.selectedChannel ?: return
        viewModelScope.launch {
            prefs.addToHistory(channel.copy(lastPlayedAt = System.currentTimeMillis()))
        }
    }

    fun onRemoveFromHistory(channelId: String) {
        viewModelScope.launch {
            prefs.removeFromHistory(channelId)
        }
    }

    /**
     * Remove an in-progress episode from the saved list.
     */
    fun onRemoveProgress(audioUrl: String) {
        viewModelScope.launch {
            prefs.removeEpisodeProgress(audioUrl)
        }
    }

    /**
     * Build minimal PodcastEpisode + PodcastChannel from saved progress,
     * so the caller (PodcastScreen) can invoke playerController.playPodcast().
     */
    fun buildResumeData(progress: EpisodeProgress): Pair<PodcastEpisode, PodcastChannel> {
        val episode = PodcastEpisode(
            title = progress.episodeTitle,
            description = "",
            audioUrl = progress.episodeAudioUrl,
            publishDate = "",
            durationSeconds = (progress.durationMs / 1000).toInt(),
        )
        val channel = PodcastChannel(
            id = progress.channelId,
            name = progress.channelName,
            author = "",
            artworkUrl = progress.channelArtwork,
            feedUrl = progress.feedUrl,
        )
        return episode to channel
    }
}
