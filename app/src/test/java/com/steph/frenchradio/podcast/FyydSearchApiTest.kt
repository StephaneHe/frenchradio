package com.steph.frenchradio.podcast

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class FyydSearchApiTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize Fyyd response and map to PodcastChannel`() {
        val responseJson = """
            {
                "status": 1,
                "data": [
                    {
                        "id": 42,
                        "title": "Mon Podcast",
                        "xmlURL": "https://feed.com/rss.xml",
                        "author": "Jean Dupont",
                        "imgURL": "https://img.com/art.jpg",
                        "description": "Description"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<FyydResponse>(responseJson)
        val channels = response.data.mapNotNull { it.toPodcastChannel() }

        assertEquals(1, channels.size)
        assertEquals("42", channels[0].id)
        assertEquals("Mon Podcast", channels[0].name)
        assertEquals("Jean Dupont", channels[0].author)
        assertEquals("https://img.com/art.jpg", channels[0].artworkUrl)
        assertEquals("https://feed.com/rss.xml", channels[0].feedUrl)
        assertEquals("Fyyd", channels[0].source)
    }

    @Test
    fun `xmlURL is used as id when fyyd id is absent`() {
        val responseJson = """
            {
                "status": 1,
                "data": [
                    {
                        "title": "Sans ID",
                        "xmlURL": "https://feed.com/rss.xml"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<FyydResponse>(responseJson)
        val channels = response.data.mapNotNull { it.toPodcastChannel() }

        assertEquals(1, channels.size)
        assertEquals("https://feed.com/rss.xml", channels[0].id)
        assertEquals("https://feed.com/rss.xml", channels[0].feedUrl)
    }

    @Test
    fun `items without xmlURL are filtered out`() {
        val responseJson = """
            {
                "status": 1,
                "data": [
                    {
                        "id": 1,
                        "title": "Pas de flux"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<FyydResponse>(responseJson)
        val channels = response.data.mapNotNull { it.toPodcastChannel() }

        assertTrue(channels.isEmpty())
    }

    @Test
    fun `items with blank xmlURL are filtered out`() {
        val responseJson = """
            {
                "status": 1,
                "data": [
                    {
                        "id": 1,
                        "title": "Flux vide",
                        "xmlURL": "   "
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<FyydResponse>(responseJson)
        val channels = response.data.mapNotNull { it.toPodcastChannel() }

        assertTrue(channels.isEmpty())
    }

    @Test
    fun `multiple podcasts are all mapped`() {
        val responseJson = """
            {
                "status": 1,
                "data": [
                    { "id": 1, "title": "A", "xmlURL": "https://a.xml" },
                    { "id": 2, "title": "B", "xmlURL": "https://b.xml" },
                    { "id": 3, "title": "C" }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<FyydResponse>(responseJson)
        val channels = response.data.mapNotNull { it.toPodcastChannel() }

        assertEquals(2, channels.size)
        assertEquals("A", channels[0].name)
        assertEquals("B", channels[1].name)
    }

    @Test
    fun `empty data array returns empty list`() {
        val responseJson = """{ "status": 0, "data": [] }"""

        val response = json.decodeFromString<FyydResponse>(responseJson)
        val channels = response.data.mapNotNull { it.toPodcastChannel() }

        assertTrue(channels.isEmpty())
    }
}
