package com.steph.frenchradio.podcast

import com.steph.frenchradio.model.PodcastEpisode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Parses a podcast RSS feed into a list of PodcastEpisode.
 */
interface FeedParser {
    fun parse(xml: String): List<PodcastEpisode>
}

class RssFeedParser : FeedParser {

    override fun parse(xml: String): List<PodcastEpisode> {
        val episodes = mutableListOf<PodcastEpisode>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var insideItem = false
        var title = ""
        var description = ""
        var audioUrl = ""
        var pubDate = ""
        var duration = ""
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        insideItem = true
                        title = ""
                        description = ""
                        audioUrl = ""
                        pubDate = ""
                        duration = ""
                    }
                    if (insideItem && currentTag == "enclosure") {
                        audioUrl = parser.getAttributeValue(null, "url") ?: ""
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "title" -> title = text
                            "description" -> description = stripHtml(text)
                            "pubDate" -> pubDate = text
                            "duration" -> duration = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && insideItem) {
                        insideItem = false
                        if (audioUrl.isNotBlank()) {
                            episodes.add(
                                PodcastEpisode(
                                    title = title,
                                    description = description,
                                    audioUrl = audioUrl,
                                    publishDate = pubDate,
                                    durationSeconds = parseDuration(duration),
                                )
                            )
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return episodes
    }

    companion object {
        fun stripHtml(input: String): String =
            input.replace(Regex("<[^>]*>"), "").trim()

        /**
         * Parse duration from "HH:MM:SS", "MM:SS", or raw seconds.
         */
        fun parseDuration(input: String): Int {
            if (input.isBlank()) return 0
            // Try raw seconds
            input.toIntOrNull()?.let { return it }
            // Try HH:MM:SS or MM:SS
            val parts = input.split(":")
            return when (parts.size) {
                3 -> {
                    val h = parts[0].toIntOrNull() ?: 0
                    val m = parts[1].toIntOrNull() ?: 0
                    val s = parts[2].toIntOrNull() ?: 0
                    h * 3600 + m * 60 + s
                }
                2 -> {
                    val m = parts[0].toIntOrNull() ?: 0
                    val s = parts[1].toIntOrNull() ?: 0
                    m * 60 + s
                }
                else -> 0
            }
        }
    }
}
