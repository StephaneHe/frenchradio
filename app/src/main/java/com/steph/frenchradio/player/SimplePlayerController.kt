package com.steph.frenchradio.player

import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import com.steph.frenchradio.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * PlayerController implementation that delegates to an AudioEngine.
 */
class SimplePlayerController(
    private val engine: AudioEngine,
) : PlayerController, AudioEngine.Listener {

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        engine.setListener(this)
    }

    override fun playRadio(station: RadioStation) {
        engine.stop()
        _playerState.value = PlayerState(
            isBuffering = true,
            currentTitle = station.name,
            currentSubtitle = station.genres.joinToString(", "),
            currentArtwork = station.logo,
            currentStreamUrl = station.streamUrl,
            isRadio = true,
        )
        engine.prepare(station.streamUrl)
        engine.play()
    }

    override fun playPodcast(episode: PodcastEpisode, channel: PodcastChannel) {
        engine.stop()
        _playerState.value = PlayerState(
            isBuffering = true,
            currentTitle = episode.title,
            currentSubtitle = channel.name,
            currentArtwork = channel.artworkUrl,
            currentStreamUrl = episode.audioUrl,
            isRadio = false,
            durationMs = episode.durationSeconds * 1000L,
        )
        engine.prepare(episode.audioUrl)
        engine.play()
    }

    override fun pause() {
        engine.pause()
        _playerState.update { it.copy(isPlaying = false) }
    }

    override fun resume() {
        engine.play()
        _playerState.update { it.copy(isPlaying = true, isError = false, errorMessage = null) }
    }

    override fun stop() {
        engine.stop()
        _playerState.value = PlayerState()
    }

    override fun seekTo(positionMs: Long) {
        engine.seekTo(positionMs)
        _playerState.update { it.copy(positionMs = positionMs) }
    }

    // --- AudioEngine.Listener ---

    override fun onBuffering() {
        _playerState.update { it.copy(isBuffering = true) }
    }

    override fun onReady() {
        _playerState.update { it.copy(isPlaying = true, isBuffering = false, isError = false, errorMessage = null) }
    }

    override fun onEnded() {
        _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    override fun onError(message: String) {
        _playerState.update { it.copy(isPlaying = false, isBuffering = false, isError = true, errorMessage = message) }
    }
}
