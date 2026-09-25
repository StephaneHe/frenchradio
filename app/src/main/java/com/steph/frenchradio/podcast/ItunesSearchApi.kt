package com.steph.frenchradio.podcast

import com.steph.frenchradio.model.PodcastChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder

/**
 * iTunes Search API implementation.
 */
class ItunesSearchApi : PodcastSearchApi {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String, limit: Int): List<PodcastChannel> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&media=podcast&country=fr&limit=$limit"
        val response = withContext(Dispatchers.IO) {
            URL(url).readText()
        }
        val result = json.decodeFromString<ItunesResponse>(response)
        return result.results.mapNotNull { it.toPodcastChannel() }
    }
}

@Serializable
data class ItunesResponse(
    val results: List<ItunesResult> = emptyList(),
)

@Serializable
data class ItunesResult(
    @SerialName("collectionId") val collectionId: Long? = null,
    @SerialName("collectionName") val collectionName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("artworkUrl100") val artworkUrl100: String? = null,
    @SerialName("artworkUrl600") val artworkUrl600: String? = null,
    @SerialName("feedUrl") val feedUrl: String? = null,
) {
    fun toPodcastChannel(): PodcastChannel? {
        if (collectionId == null || collectionName == null || feedUrl == null) return null
        return PodcastChannel(
            id = collectionId.toString(),
            name = collectionName,
            author = artistName ?: "",
            artworkUrl = artworkUrl600 ?: artworkUrl100 ?: "",
            feedUrl = feedUrl,
            source = "iTunes",
        )
    }
}
