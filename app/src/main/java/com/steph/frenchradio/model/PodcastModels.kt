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
