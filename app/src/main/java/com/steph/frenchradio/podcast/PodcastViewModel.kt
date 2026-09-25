package com.steph.frenchradio.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steph.frenchradio.data.AppPreferences
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.ListenHistoryEntry
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val BROWSE_CATEGORIES = listOf(
    "Actualités", "Culture", "Science", "Comédie", "Histoire",
    "Société", "Technologie", "Sport", "Santé", "Éducation",
    "Arts", "Musique", "Politique", "Économie", "Spiritualité",
)

data class PodcastUiState(
    val searchQuery: String = "",
    val searchResults: List<PodcastChannel> = emptyList(),
    val history: List<PodcastChannel> = emptyList(),
    val inProgress: List<EpisodeProgress> = emptyList(),
    val listenHistory: List<ListenHistoryEntry> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val selectedChannel: PodcastChannel? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
    val isLoadingEpisodes: Boolean = false,
    val episodeError: String? = null,
    val selectedTab: Int = 0,
    val browseCategory: String? = null,
    val browseResults: List<PodcastChannel> = emptyList(),
    val isBrowsing: Boolean = false,
    val browseError: String? = null,
)

class PodcastViewModel(
    private val searchApi: PodcastSearchApi,
    private val feedParser: FeedParser,
    private val urlFetcher: UrlFetcher,
    private val prefs: AppPreferences,
    private val browseApi: PodcastSearchApi = FyydSearchApi(),
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
        viewModelScope.launch {
            prefs.listenHistory.collect { entries ->
                _uiState.update { it.copy(listenHistory = entries) }
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
            delay(500)
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

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // Loads browse results into the search list and sets the search query to the category name.
    // Used by BrowseTab when it redirects to the Search tab.
    fun onBrowseCategory(category: String) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(searchQuery = category, isSearching = true, searchError = null, searchResults = emptyList())
        }
        viewModelScope.launch {
            try {
                val results = browseApi.search(category, 30)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, searchError = e.message ?: "Browse failed")
                }
            }
        }
    }

    fun onBrowseCategorySelected(category: String) {
        _uiState.update {
            it.copy(
                browseCategory = category,
                browseResults = emptyList(),
                isBrowsing = true,
                browseError = null,
            )
        }
        viewModelScope.launch {
            try {
                val results = browseApi.search(category, 30)
                _uiState.update { it.copy(browseResults = results, isBrowsing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isBrowsing = false, browseError = e.message ?: "Browse failed")
                }
            }
        }
    }

    fun onClearBrowse() {
        _uiState.update {
            it.copy(browseCategory = null, browseResults = emptyList(), browseError = null)
        }
    }

    fun onChannelSelected(channel: PodcastChannel) {
        _uiState.update {
            it.copy(selectedChannel = channel, isLoadingEpisodes = true, episodeError = null, episodes = emptyList())
        }
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

    fun onRemoveProgress(audioUrl: String) {
        viewModelScope.launch {
            prefs.removeEpisodeProgress(audioUrl)
        }
    }

    fun onRemoveListenHistory(audioUrl: String) {
        viewModelScope.launch {
            prefs.removeListenHistory(audioUrl)
        }
    }

    fun onClearListenHistory() {
        viewModelScope.launch {
            prefs.clearListenHistory()
        }
    }

    fun buildReplayData(entry: ListenHistoryEntry): Pair<PodcastEpisode, PodcastChannel> {
        val episode = PodcastEpisode(
            title = entry.episodeTitle,
            description = "",
            audioUrl = entry.episodeAudioUrl,
            publishDate = "",
            durationSeconds = (entry.durationMs / 1000).toInt(),
        )
        val channel = PodcastChannel(
            id = entry.channelId,
            name = entry.channelName,
            author = "",
            artworkUrl = entry.channelArtwork,
            feedUrl = entry.feedUrl,
        )
        return episode to channel
    }

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
