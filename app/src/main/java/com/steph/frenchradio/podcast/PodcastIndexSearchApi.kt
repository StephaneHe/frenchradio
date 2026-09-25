package com.steph.frenchradio.podcast

import com.steph.frenchradio.model.PodcastChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PodcastIndexSearchApi(
    private val apiKey: String = "",
    private val apiSecret: String = "",
) : PodcastSearchApi {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String, limit: Int): List<PodcastChannel> {
        if (apiKey.isBlank()) return emptyList()

        val unixTime = (System.currentTimeMillis() / 1000).toString()
        val authHash = buildAuthHash(unixTime)
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.podcastindex.org/api/1.0/search/byterm?q=$encoded&max=$limit"

        val response = withContext(Dispatchers.IO) {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("X-Auth-Key", apiKey)
            conn.setRequestProperty("X-Auth-Date", unixTime)
            conn.setRequestProperty("Authorization", authHash)
            conn.setRequestProperty("User-Agent", "frenchradio/1.0")
            conn.inputStream.bufferedReader().readText()
        }

        val result = json.decodeFromString<PodcastIndexResponse>(response)
        return result.feeds
            .filter { it.isFrench() }
            .mapNotNull { it.toPodcastChannel() }
    }

    private fun buildAuthHash(unixTime: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(apiSecret.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val hashBytes = mac.doFinal((apiKey + apiSecret + unixTime).toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

@Serializable
data class PodcastIndexResponse(
    val feeds: List<PodcastIndexFeed> = emptyList(),
)

@Serializable
data class PodcastIndexFeed(
    val id: Long? = null,
    val title: String? = null,
    val url: String? = null,
    val author: String? = null,
    val image: String? = null,
    val description: String? = null,
    val language: String? = null,
) {
    fun toPodcastChannel(): PodcastChannel? {
        if (url.isNullOrBlank()) return null
        return PodcastChannel(
            id = id?.toString() ?: url,
            name = title ?: "",
            author = author ?: "",
            artworkUrl = image ?: "",
            feedUrl = url,
            source = "PodcastIndex",
        )
    }

    fun isFrench(): Boolean {
        if (language.isNullOrBlank()) return true
        return language.lowercase().startsWith("fr")
    }
}
