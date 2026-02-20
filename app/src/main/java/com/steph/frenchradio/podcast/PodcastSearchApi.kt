package com.steph.frenchradio.podcast

import com.steph.frenchradio.model.PodcastChannel

/**
 * Searches podcasts via iTunes Search API.
 */
interface PodcastSearchApi {
    suspend fun search(query: String, limit: Int = 20): List<PodcastChannel>
}
