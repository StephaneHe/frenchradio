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
