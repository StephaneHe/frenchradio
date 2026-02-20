package com.steph.frenchradio.podcast

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches raw content from a URL. Abstracted for testability.
 */
interface UrlFetcher {
    suspend fun fetch(url: String): String
}

class DefaultUrlFetcher : UrlFetcher {
    override suspend fun fetch(url: String): String =
        withContext(Dispatchers.IO) {
            java.net.URL(url).readText()
        }
}
