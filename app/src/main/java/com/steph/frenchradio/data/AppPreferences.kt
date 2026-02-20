package com.steph.frenchradio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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
}
