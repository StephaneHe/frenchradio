package com.steph.frenchradio.podcast

import com.steph.frenchradio.model.PodcastChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder

class FyydSearchApi : PodcastSearchApi {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String, limit: Int): List<PodcastChannel> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.fyyd.de/0.2/search/podcast?term=$encoded&count=$limit"
        val response = withContext(Dispatchers.IO) {
            URL(url).readText()
        }
        val result = json.decodeFromString<FyydResponse>(response)
        return result.data.mapNotNull { it.toPodcastChannel() }
    }
}

@Serializable
data class FyydResponse(
    val status: Int = 0,
    val data: List<FyydPodcast> = emptyList(),
)

@Serializable
data class FyydPodcast(
    val id: Long? = null,
    val title: String? = null,
    @SerialName("xmlURL") val xmlURL: String? = null,
    val author: String? = null,
    @SerialName("imgURL") val imgURL: String? = null,
    val description: String? = null,
) {
    fun toPodcastChannel(): PodcastChannel? {
        if (xmlURL.isNullOrBlank()) return null
        return PodcastChannel(
            id = id?.toString() ?: xmlURL,
            name = title ?: "",
            author = author ?: "",
            artworkUrl = imgURL ?: "",
            feedUrl = xmlURL,
            source = "Fyyd",
        )
    }
}
