package com.steph.frenchradio.podcast

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ItunesSearchApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize iTunes response`() {
        val responseJson = """
            {
                "resultCount": 2,
                "results": [
                    {
                        "collectionId": 12345,
                        "collectionName": "Mon Podcast",
                        "artistName": "Jean Dupont",
                        "artworkUrl100": "https://img.com/100.jpg",
                        "artworkUrl600": "https://img.com/600.jpg",
                        "feedUrl": "https://feed.com/rss.xml"
                    },
                    {
                        "collectionId": 67890,
                        "collectionName": "Autre Podcast",
                        "artistName": "Marie Martin",
                        "artworkUrl100": "https://img.com/100b.jpg",
                        "feedUrl": "https://feed.com/rss2.xml"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<ItunesResponse>(responseJson)
        assertEquals(2, response.results.size)

        val channels = response.results.mapNotNull { it.toPodcastChannel() }
        assertEquals(2, channels.size)
        assertEquals("12345", channels[0].id)
        assertEquals("Mon Podcast", channels[0].name)
        assertEquals("Jean Dupont", channels[0].author)
        assertEquals("https://img.com/600.jpg", channels[0].artworkUrl)
        assertEquals("https://feed.com/rss.xml", channels[0].feedUrl)

        // Second has no artworkUrl600, falls back to artworkUrl100
        assertEquals("https://img.com/100b.jpg", channels[1].artworkUrl)
    }

    @Test
    fun `results without collectionId are filtered out`() {
        val responseJson = """
            {
                "results": [
                    {
                        "collectionName": "No ID",
                        "feedUrl": "https://feed.com/rss.xml"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<ItunesResponse>(responseJson)
        val channels = response.results.mapNotNull { it.toPodcastChannel() }
        assertTrue(channels.isEmpty())
    }

    @Test
    fun `results without feedUrl are filtered out`() {
        val responseJson = """
            {
                "results": [
                    {
                        "collectionId": 111,
                        "collectionName": "No Feed"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<ItunesResponse>(responseJson)
        val channels = response.results.mapNotNull { it.toPodcastChannel() }
        assertTrue(channels.isEmpty())
    }
}
