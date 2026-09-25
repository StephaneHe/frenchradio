package com.steph.frenchradio.podcast

import com.steph.frenchradio.model.PodcastChannel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class AggregatedSearchApi(
    private val sources: List<PodcastSearchApi>,
) : PodcastSearchApi {

    override suspend fun search(query: String, limit: Int): List<PodcastChannel> =
        coroutineScope {
            sources
                .map { source ->
                    async {
                        try {
                            source.search(query, limit)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }
                .awaitAll()
                .flatten()
                .distinctBy { it.feedUrl }
        }
}
