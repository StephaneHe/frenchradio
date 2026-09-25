package com.steph.frenchradio.podcast

import com.steph.frenchradio.model.PodcastChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AggregatedSearchApiTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `merges results from all sources`() = runTest {
        val source1 = FakePodcastSearchApi().apply {
            results = listOf(channel("1", "A", "https://feed1.xml"))
        }
        val source2 = FakePodcastSearchApi().apply {
            results = listOf(channel("2", "B", "https://feed2.xml"))
        }
        val api = AggregatedSearchApi(listOf(source1, source2))

        val results = api.search("test")

        assertEquals(2, results.size)
        val names = results.map { it.name }
        assertTrue(names.contains("A"))
        assertTrue(names.contains("B"))
    }

    @Test
    fun `deduplicates by feedUrl keeping first occurrence`() = runTest {
        val ch = channel("1", "Podcast A", "https://same.xml")
        val source1 = FakePodcastSearchApi().apply { results = listOf(ch) }
        val source2 = FakePodcastSearchApi().apply {
            results = listOf(ch.copy(id = "2", name = "Podcast A (duplicate)"))
        }
        val api = AggregatedSearchApi(listOf(source1, source2))

        val results = api.search("test")

        assertEquals(1, results.size)
        assertEquals("Podcast A", results[0].name)
    }

    @Test
    fun `failure of one source does not prevent others`() = runTest {
        val badSource = FakePodcastSearchApi().apply { shouldThrow = true }
        val goodSource = FakePodcastSearchApi().apply {
            results = listOf(channel("1", "Survivant", "https://feed.xml"))
        }
        val api = AggregatedSearchApi(listOf(badSource, goodSource))

        val results = api.search("test")

        assertEquals(1, results.size)
        assertEquals("Survivant", results[0].name)
    }

    @Test
    fun `all sources failing returns empty list`() = runTest {
        val bad1 = FakePodcastSearchApi().apply { shouldThrow = true }
        val bad2 = FakePodcastSearchApi().apply { shouldThrow = true }
        val api = AggregatedSearchApi(listOf(bad1, bad2))

        val results = api.search("test")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `empty sources list returns empty`() = runTest {
        val api = AggregatedSearchApi(emptyList())

        val results = api.search("test")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `three sources with overlap are deduplicated correctly`() = runTest {
        val source1 = FakePodcastSearchApi().apply {
            results = listOf(
                channel("i1", "iTunes Only", "https://itunes.xml"),
                channel("shared", "Shared", "https://shared.xml"),
            )
        }
        val source2 = FakePodcastSearchApi().apply {
            results = listOf(
                channel("shared2", "Shared", "https://shared.xml"),
                channel("f1", "Fyyd Only", "https://fyyd.xml"),
            )
        }
        val source3 = FakePodcastSearchApi().apply {
            results = listOf(channel("p1", "PodIndex Only", "https://podindex.xml"))
        }
        val api = AggregatedSearchApi(listOf(source1, source2, source3))

        val results = api.search("test")

        assertEquals(4, results.size)
        val feedUrls = results.map { it.feedUrl }
        assertTrue(feedUrls.contains("https://shared.xml"))
        assertEquals(1, feedUrls.count { it == "https://shared.xml" })
    }

    private fun channel(id: String, name: String, feedUrl: String) = PodcastChannel(
        id = id,
        name = name,
        author = "Author",
        artworkUrl = "https://img.com/art.jpg",
        feedUrl = feedUrl,
    )
}
