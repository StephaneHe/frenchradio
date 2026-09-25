package com.steph.frenchradio.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastChannel(
    val id: String,
    val name: String,
    val author: String,
    val artworkUrl: String,
    val feedUrl: String,
    val lastPlayedAt: Long = 0L,
    val source: String = "",
)

data class PodcastEpisode(
    val title: String,
    val description: String,
    val audioUrl: String,
    val publishDate: String,
    val durationSeconds: Int,
)

/**
 * Tracks playback progress for a podcast episode so it can be resumed later.
 */
@Serializable
data class EpisodeProgress(
    val episodeAudioUrl: String,
    val episodeTitle: String,
    val channelId: String,
    val channelName: String,
    val channelArtwork: String,
    val feedUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = 0L,
)

/**
 * One entry of the listening history (one per episode, keyed by audio URL).
 *
 * Unlike [EpisodeProgress] (which is removed when an episode ends), history
 * entries persist until the user deletes them, so "played" markers survive.
 */
@Serializable
data class ListenHistoryEntry(
    val episodeAudioUrl: String,
    val episodeTitle: String,
    val channelId: String,
    val channelName: String,
    val channelArtwork: String,
    val feedUrl: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** Sticky: once an episode has been played through, it stays "played". */
    val completed: Boolean = false,
    val lastListenedAt: Long = 0L,
) {
    val status: ListenStatus
        get() = when {
            completed -> ListenStatus.PLAYED
            durationMs > 0 && positionMs >= durationMs * PLAYED_THRESHOLD -> ListenStatus.PLAYED
            else -> ListenStatus.STARTED
        }

    /** Progress in [0, 1], or 0 when the duration is unknown. */
    val fraction: Float
        get() = when {
            status == ListenStatus.PLAYED -> 1f
            durationMs > 0 -> (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            else -> 0f
        }

    companion object {
        /**
         * An episode counts as "played" once 98 % of it has been listened to (or its end
         * was reached). Also the point past which resuming restarts from 0.
         * Threshold-based "played" is computed, never persisted: only a real end sets
         * [completed], so changing this value re-classifies existing entries.
         */
        const val PLAYED_THRESHOLD = 0.98
    }
}

enum class ListenStatus { STARTED, PLAYED }
