package com.steph.frenchradio.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.text.Normalizer
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class StationLoaderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `loadFromResource returns 44 stations`() {
        val stations = StationLoader.loadFromResource(context)
        assertEquals(44, stations.size)
    }

    @Test
    fun `stations are sorted alphabetically`() {
        val stations = StationLoader.loadFromResource(context)
        val names = stations.map { it.name }
        val sortedNames = names.sortedBy { stripAccents(it).lowercase() }
        assertEquals(sortedNames, names)
    }

    @Test
    fun `all stations have non-empty stream URLs`() {
        val stations = StationLoader.loadFromResource(context)
        stations.forEach { station ->
            assertTrue(
                "${station.name} has empty stream URL",
                station.streamUrl.isNotBlank()
            )
        }
    }

    @Test
    fun `stations have at least 10 distinct genres`() {
        val stations = StationLoader.loadFromResource(context)
        val genres = extractGenres(stations)
        assertTrue("Expected >= 10 genres, got ${genres.size}", genres.size >= 10)
    }

    @Test
    fun `parseStations works with custom JSON`() {
        val customJson = """
            {
              "stations": [
                {
                  "id": "test1",
                  "name": "Zebra Radio",
                  "stream_url": "http://example.com/z.mp3",
                  "logo": "test",
                  "genres": ["Test"],
                  "color": "#FF0000"
                },
                {
                  "id": "test2",
                  "name": "Alpha Radio",
                  "stream_url": "http://example.com/a.mp3",
                  "logo": "test",
                  "genres": ["Test"],
                  "color": "#00FF00"
                }
              ]
            }
        """.trimIndent()
        val stations = StationLoader.parseStations(customJson)
        assertEquals(2, stations.size)
        assertEquals("Alpha Radio", stations[0].name) // sorted
        assertEquals("Zebra Radio", stations[1].name)
    }
}


private fun stripAccents(s: String): String {
    val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
    return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}
