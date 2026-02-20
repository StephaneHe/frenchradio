package com.steph.frenchradio.podcast

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RssFeedParserTest {

    private val parser = RssFeedParser()

    @Test
    fun `parse valid RSS extracts episodes`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
            <channel>
                <title>Test Podcast</title>
                <item>
                    <title>Episode 1</title>
                    <description>First episode</description>
                    <enclosure url="https://example.com/ep1.mp3" type="audio/mpeg"/>
                    <pubDate>Mon, 01 Jan 2025 00:00:00 GMT</pubDate>
                    <itunes:duration>01:30:00</itunes:duration>
                </item>
                <item>
                    <title>Episode 2</title>
                    <description><![CDATA[<p>Second</p>]]></description>
                    <enclosure url="https://example.com/ep2.mp3" type="audio/mpeg"/>
                    <pubDate>Tue, 02 Jan 2025 00:00:00 GMT</pubDate>
                    <itunes:duration>45:30</itunes:duration>
                </item>
            </channel>
            </rss>
        """.trimIndent()

        val episodes = parser.parse(xml)

        assertEquals(2, episodes.size)
        assertEquals("Episode 1", episodes[0].title)
        assertEquals("https://example.com/ep1.mp3", episodes[0].audioUrl)
        assertEquals(5400, episodes[0].durationSeconds) // 1:30:00
        assertEquals("Episode 2", episodes[1].title)
        assertEquals(2730, episodes[1].durationSeconds) // 45:30
    }

    @Test
    fun `items without enclosure are skipped`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
            <channel>
                <item>
                    <title>No Audio</title>
                    <description>Text only</description>
                </item>
                <item>
                    <title>Has Audio</title>
                    <enclosure url="https://example.com/ep.mp3" type="audio/mpeg"/>
                </item>
            </channel>
            </rss>
        """.trimIndent()

        val episodes = parser.parse(xml)
        assertEquals(1, episodes.size)
        assertEquals("Has Audio", episodes[0].title)
    }

    @Test
    fun `empty RSS returns empty list`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel></channel></rss>
        """.trimIndent()

        val episodes = parser.parse(xml)
        assertTrue(episodes.isEmpty())
    }

    @Test
    fun `parseDuration handles HH MM SS`() {
        assertEquals(5400, RssFeedParser.parseDuration("01:30:00"))
    }

    @Test
    fun `parseDuration handles MM SS`() {
        assertEquals(2730, RssFeedParser.parseDuration("45:30"))
    }

    @Test
    fun `parseDuration handles raw seconds`() {
        assertEquals(3600, RssFeedParser.parseDuration("3600"))
    }

    @Test
    fun `parseDuration handles empty string`() {
        assertEquals(0, RssFeedParser.parseDuration(""))
    }

    @Test
    fun `stripHtml removes tags`() {
        assertEquals("Hello world", RssFeedParser.stripHtml("<p>Hello <b>world</b></p>"))
    }
}
