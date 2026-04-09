package com.steph.frenchradio.player

import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import com.steph.frenchradio.model.RadioStation
import kotlinx.coroutines.flow.StateFlow

/**
 * Controls audio playback for both radio streams and podcast episodes.
 */
interface PlayerController {
    val playerState: StateFlow<PlayerState>

    fun playRadio(station: RadioStation)
    fun playPodcast(episode: PodcastEpisode, channel: PodcastChannel)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun refreshPosition()
}
