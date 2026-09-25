package com.steph.frenchradio.data

import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.ListenHistoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for persisted user preferences.
 */
interface AppPreferences {
    // Radio favorites
    val favoriteStationIds: Flow<Set<String>>
    suspend fun toggleFavorite(stationId: String)

    // Podcast history
    val podcastHistory: Flow<List<com.steph.frenchradio.model.PodcastChannel>>
    suspend fun addToHistory(channel: com.steph.frenchradio.model.PodcastChannel)
    suspend fun removeFromHistory(channelId: String)
    suspend fun clearHistory()

    // Episode playback progress (for resume)
    val episodeProgressList: Flow<List<EpisodeProgress>>
    suspend fun saveEpisodeProgress(progress: EpisodeProgress)
    suspend fun removeEpisodeProgress(audioUrl: String)
    suspend fun getEpisodeProgress(audioUrl: String): EpisodeProgress?

    // Episode listening history (played / started markers), newest first
    val listenHistory: Flow<List<ListenHistoryEntry>>
    suspend fun recordListen(entry: ListenHistoryEntry)
    suspend fun removeListenHistory(audioUrl: String)
    suspend fun clearListenHistory()

    // Audio boost percent (0..100), persisted across restarts
    val audioBoostPercent: Flow<Int>
    suspend fun setAudioBoostPercent(percent: Int)
}
