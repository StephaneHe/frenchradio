package com.steph.frenchradio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.PodcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class DataStoreAppPreferences(
    private val dataStore: DataStore<Preferences>,
) : AppPreferences {

    /** Convenience constructor for production use. */
    constructor(context: Context) : this(context.dataStore)

    companion object {
        val FAVORITES_KEY = stringSetPreferencesKey("radio_favorites")
        val PODCAST_HISTORY_KEY = stringPreferencesKey("podcast_history")
        val EPISODE_PROGRESS_KEY = stringPreferencesKey("episode_progress")
        const val MAX_HISTORY = 50
        const val MAX_PROGRESS = 100
    }

    private val json = Json { ignoreUnknownKeys = true }

    // --- Radio Favorites ---

    override val favoriteStationIds: Flow<Set<String>> =
        dataStore.data.map { prefs ->
            prefs[FAVORITES_KEY] ?: emptySet()
        }

    override suspend fun toggleFavorite(stationId: String) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] = if (stationId in current) {
                current - stationId
            } else {
                current + stationId
            }
        }
    }

    // --- Podcast History ---

    override val podcastHistory: Flow<List<PodcastChannel>> =
        dataStore.data.map { prefs ->
            val raw = prefs[PODCAST_HISTORY_KEY] ?: "[]"
            try {
                json.decodeFromString<List<PodcastChannel>>(raw)
                    .sortedByDescending { it.lastPlayedAt }
            } catch (_: Exception) {
                emptyList()
            }
        }

    override suspend fun addToHistory(channel: PodcastChannel) {
        dataStore.edit { prefs ->
            val raw = prefs[PODCAST_HISTORY_KEY] ?: "[]"
            val current = try {
                json.decodeFromString<List<PodcastChannel>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            current.removeAll { it.id == channel.id }
            val updated = channel.copy(
                lastPlayedAt = if (channel.lastPlayedAt > 0) channel.lastPlayedAt
                    else System.currentTimeMillis()
            )
            current.add(0, updated)
            val trimmed = current.take(MAX_HISTORY)
            prefs[PODCAST_HISTORY_KEY] = json.encodeToString(trimmed)
        }
    }

    override suspend fun removeFromHistory(channelId: String) {
        dataStore.edit { prefs ->
            val raw = prefs[PODCAST_HISTORY_KEY] ?: "[]"
            val current = try {
                json.decodeFromString<List<PodcastChannel>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
            val filtered = current.filter { it.id != channelId }
            prefs[PODCAST_HISTORY_KEY] = json.encodeToString(filtered)
        }
    }

    override suspend fun clearHistory() {
        dataStore.edit { prefs ->
            prefs[PODCAST_HISTORY_KEY] = "[]"
        }
    }

    // --- Episode Playback Progress ---

    override val episodeProgressList: Flow<List<EpisodeProgress>> =
        dataStore.data.map { prefs ->
            val raw = prefs[EPISODE_PROGRESS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<EpisodeProgress>>(raw)
                    .sortedByDescending { it.updatedAt }
            } catch (_: Exception) {
                emptyList()
            }
        }

    override suspend fun saveEpisodeProgress(progress: EpisodeProgress) {
        dataStore.edit { prefs ->
            val raw = prefs[EPISODE_PROGRESS_KEY] ?: "[]"
            val current = try {
                json.decodeFromString<List<EpisodeProgress>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            current.removeAll { it.episodeAudioUrl == progress.episodeAudioUrl }
            val updated = progress.copy(
                updatedAt = if (progress.updatedAt > 0) progress.updatedAt
                    else System.currentTimeMillis()
            )
            current.add(0, updated)
            val trimmed = current.take(MAX_PROGRESS)
            prefs[EPISODE_PROGRESS_KEY] = json.encodeToString(trimmed)
        }
    }

    override suspend fun removeEpisodeProgress(audioUrl: String) {
        dataStore.edit { prefs ->
            val raw = prefs[EPISODE_PROGRESS_KEY] ?: "[]"
            val current = try {
                json.decodeFromString<List<EpisodeProgress>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
            val filtered = current.filter { it.episodeAudioUrl != audioUrl }
            prefs[EPISODE_PROGRESS_KEY] = json.encodeToString(filtered)
        }
    }

    override suspend fun getEpisodeProgress(audioUrl: String): EpisodeProgress? {
        val all = episodeProgressList.first()
        return all.find { it.episodeAudioUrl == audioUrl }
    }
}
