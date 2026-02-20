package com.steph.frenchradio.model

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RadioStationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize single station from JSON`() {
        val jsonStr = """
            {
                "id": "france_inter",
                "name": "France Inter",
                "stream_url": "https://icecast.radiofrance.fr/franceinter-hifi.aac",
                "logo": "france_inter",
                "genres": ["Généraliste", "Info", "Culture"],
                "color": "#E4002B"
            }
        """.trimIndent()

        val station = json.decodeFromString<RadioStation>(jsonStr)

        assertEquals("france_inter", station.id)
        assertEquals("France Inter", station.name)
        assertEquals("https://icecast.radiofrance.fr/franceinter-hifi.aac", station.streamUrl)
        assertEquals("france_inter", station.logo)
        assertEquals(listOf("Généraliste", "Info", "Culture"), station.genres)
        assertEquals("#E4002B", station.color)
    }

    @Test
    fun `deserialize stations wrapper from JSON`() {
        val jsonStr = """
            {
                "stations": [
                    {
                        "id": "fip",
                        "name": "FIP",
                        "stream_url": "http://direct.fipradio.fr/live/fip-midfi.mp3",
                        "logo": "fip",
                        "genres": ["Éclectique"],
                        "color": "#E95E9A"
                    },
                    {
                        "id": "nrj",
                        "name": "NRJ",
                        "stream_url": "http://cdn.nrjaudio.fm/audio1/fr/30001/mp3_128.mp3",
                        "logo": "nrj",
                        "genres": ["Hits", "Pop"],
                        "color": "#FF0000"
                    }
                ]
            }
        """.trimIndent()

        val wrapper = json.decodeFromString<StationsWrapper>(jsonStr)

        assertEquals(2, wrapper.stations.size)
        assertEquals("FIP", wrapper.stations[0].name)
        assertEquals("NRJ", wrapper.stations[1].name)
    }

    @Test
    fun `extract unique sorted genres from stations`() {
        val stations = listOf(
            makeStation("a", genres = listOf("Rock", "Jazz")),
            makeStation("b", genres = listOf("Jazz", "Pop")),
            makeStation("c", genres = listOf("Rock", "Electro")),
        )

        val genres = extractGenres(stations)

        assertEquals(listOf("Electro", "Jazz", "Pop", "Rock"), genres)
    }

    @Test
    fun `extract genres from empty list returns empty`() {
        val genres = extractGenres(emptyList())
        assertTrue(genres.isEmpty())
    }

    private fun makeStation(
        id: String,
        name: String = id,
        genres: List<String> = emptyList()
    ) = RadioStation(
        id = id,
        name = name,
        streamUrl = "http://test/$id",
        logo = id,
        genres = genres,
        color = "#000000",
    )
}
