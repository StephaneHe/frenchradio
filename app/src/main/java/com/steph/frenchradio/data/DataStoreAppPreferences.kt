package com.steph.frenchradio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.ListenHistoryEntry
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
        val AUDIO_BOOST_KEY = intPreferencesKey("audio_boost_percent")
        const val MAX_HISTORY = 50
        const val MAX_PROGRESS = 100
        val LISTEN_HISTORY_KEY = stringPreferencesKey("listen_history")
        const val MAX_LISTEN_HISTORY = 500
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

    // --- Listening History ---

    override val listenHistory: Flow<List<ListenHistoryEntry>> =
        dataStore.data.map { prefs ->
            readListenHistory(prefs).sortedByDescending { it.lastListenedAt }
        }

    override suspend fun recordListen(entry: ListenHistoryEntry) {
        dataStore.edit { prefs ->
            val current = readListenHistory(prefs).toMutableList()
            val existing = current.find { it.episodeAudioUrl == entry.episodeAudioUrl }
            current.removeAll { it.episodeAudioUrl == entry.episodeAudioUrl }
            val merged = entry.copy(
                // A "start" event (position 0) must not erase a known position
                positionMs = if (entry.positionMs > 0 || existing == null) entry.positionMs
                    else existing.positionMs,
                durationMs = if (entry.durationMs > 0 || existing == null) entry.durationMs
                    else existing.durationMs,
                completed = entry.completed || existing?.completed == true,
                lastListenedAt = if (entry.lastListenedAt > 0) entry.lastListenedAt
                    else System.currentTimeMillis(),
            )
            current.add(0, merged)
            val trimmed = current.sortedByDescending { it.lastListenedAt }.take(MAX_LISTEN_HISTORY)
            prefs[LISTEN_HISTORY_KEY] = json.encodeToString(trimmed)
        }
    }

    override suspend fun removeListenHistory(audioUrl: String) {
        dataStore.edit { prefs ->
            val filtered = readListenHistory(prefs).filter { it.episodeAudioUrl != audioUrl }
            prefs[LISTEN_HISTORY_KEY] = json.encodeToString(filtered)
        }
    }

    override suspend fun clearListenHistory() {
        dataStore.edit { prefs ->
            prefs[LISTEN_HISTORY_KEY] = "[]"
        }
    }

    /**
     * Reads the stored history. On first use (key absent), seeds it from the
     * pre-existing episode progress entries so earlier listens are not lost.
     */
    private fun readListenHistory(prefs: Preferences): List<ListenHistoryEntry> {
        val raw = prefs[LISTEN_HISTORY_KEY]
        if (raw == null) {
            val progress = try {
                json.decodeFromString<List<EpisodeProgress>>(prefs[EPISODE_PROGRESS_KEY] ?: "[]")
            } catch (_: Exception) {
                emptyList()
            }
            return progress.map {
                ListenHistoryEntry(
                    episodeAudioUrl = it.episodeAudioUrl,
                    episodeTitle = it.episodeTitle,
                    channelId = it.channelId,
                    channelName = it.channelName,
                    channelArtwork = it.channelArtwork,
                    feedUrl = it.feedUrl,
                    positionMs = it.positionMs,
                    durationMs = it.durationMs,
                    lastListenedAt = it.updatedAt,
                )
            }
        }
        return try {
            json.decodeFromString<List<ListenHistoryEntry>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- Audio Boost ---

    override val audioBoostPercent: Flow<Int> =
        dataStore.data.map { prefs ->
            (prefs[AUDIO_BOOST_KEY] ?: 0).coerceIn(0, 100)
        }

    override suspend fun setAudioBoostPercent(percent: Int) {
        dataStore.edit { prefs ->
            prefs[AUDIO_BOOST_KEY] = percent.coerceIn(0, 100)
        }
    }
}
