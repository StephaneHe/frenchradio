package com.steph.frenchradio.player

import com.steph.frenchradio.data.AppPreferences
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import com.steph.frenchradio.model.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PlayerController implementation that delegates to an AudioEngine.
 *
 * When playing a podcast, automatically saves playback position to
 * [AppPreferences] so the user can resume later. Position is saved:
 * - On pause
 * - On stop
 * - Every [SAVE_INTERVAL_MS] while playing
 * - Progress is removed when the episode finishes (onEnded)
 */
class SimplePlayerController(
    private val engine: AudioEngine,
    private val prefs: AppPreferences? = null,
    private val scope: CoroutineScope? = null,
) : PlayerController, AudioEngine.Listener {

    companion object {
        const val SAVE_INTERVAL_MS = 15_000L
    }

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    /** Periodic save job — cancelled when playback stops or switches. */
    private var periodicSaveJob: Job? = null

    /** Current channel info, kept for building EpisodeProgress. */
    private var currentChannel: PodcastChannel? = null

    init {
        engine.setListener(this)
    }

    override fun playRadio(station: RadioStation) {
        cancelPeriodicSave()
        currentChannel = null
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
        cancelPeriodicSave()
        currentChannel = channel
        engine.stop()
        _playerState.value = PlayerState(
            isBuffering = true,
            currentTitle = episode.title,
            currentSubtitle = channel.name,
            currentArtwork = channel.artworkUrl,
            currentStreamUrl = episode.audioUrl,
            isRadio = false,
            durationMs = episode.durationSeconds * 1000L,
            channelId = channel.id,
            feedUrl = channel.feedUrl,
        )
        engine.prepare(episode.audioUrl)
        engine.play()

        // Resume at saved position if available
        scope?.launch {
            val saved = prefs?.getEpisodeProgress(episode.audioUrl)
            if (saved != null && saved.positionMs > 0) {
                engine.seekTo(saved.positionMs)
                _playerState.update { it.copy(positionMs = saved.positionMs) }
            }
        }
    }

    override fun pause() {
        engine.pause()
        _playerState.update { it.copy(isPlaying = false) }
        cancelPeriodicSave()
        saveProgressIfPodcast()
    }

    override fun resume() {
        engine.play()
        _playerState.update { it.copy(isPlaying = true, isError = false, errorMessage = null) }
        startPeriodicSave()
    }

    override fun stop() {
        saveProgressIfPodcast()
        cancelPeriodicSave()
        currentChannel = null
        engine.stop()
        _playerState.value = PlayerState()
    }

    override fun seekTo(positionMs: Long) {
        engine.seekTo(positionMs)
        _playerState.update { it.copy(positionMs = positionMs) }
    }

    override fun refreshPosition() {
        val pos = engine.currentPositionMs
        val dur = engine.durationMs
        _playerState.update {
            it.copy(
                positionMs = pos,
                durationMs = if (dur > 0) dur else it.durationMs,
            )
        }
    }

    // --- AudioEngine.Listener ---

    override fun onBuffering() {
        _playerState.update { it.copy(isBuffering = true) }
    }

    override fun onReady() {
        _playerState.update { it.copy(isPlaying = true, isBuffering = false, isError = false, errorMessage = null) }
        startPeriodicSave()
    }

    override fun onEnded() {
        _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
        cancelPeriodicSave()
        // Episode finished — remove saved progress
        val state = _playerState.value
        if (!state.isRadio && state.currentStreamUrl != null) {
            scope?.launch {
                prefs?.removeEpisodeProgress(state.currentStreamUrl)
            }
        }
    }

    override fun onError(message: String) {
        _playerState.update { it.copy(isPlaying = false, isBuffering = false, isError = true, errorMessage = message) }
        cancelPeriodicSave()
    }

    // --- Private helpers ---

    private fun saveProgressIfPodcast() {
        val state = _playerState.value
        if (state.isRadio || state.currentStreamUrl == null) return
        val channel = currentChannel ?: return
        val positionMs = engine.currentPositionMs
        if (positionMs <= 0) return

        val progress = EpisodeProgress(
            episodeAudioUrl = state.currentStreamUrl,
            episodeTitle = state.currentTitle,
            channelId = channel.id,
            channelName = channel.name,
            channelArtwork = channel.artworkUrl,
            feedUrl = channel.feedUrl,
            positionMs = positionMs,
            durationMs = if (engine.durationMs > 0) engine.durationMs else state.durationMs,
        )
        scope?.launch {
            prefs?.saveEpisodeProgress(progress)
        }
    }

    private fun startPeriodicSave() {
        if (_playerState.value.isRadio) return
        cancelPeriodicSave()
        periodicSaveJob = scope?.launch {
            while (true) {
                delay(SAVE_INTERVAL_MS)
                if (_playerState.value.isPlaying && !_playerState.value.isRadio) {
                    saveProgressIfPodcast()
                }
            }
        }
    }

    private fun cancelPeriodicSave() {
        periodicSaveJob?.cancel()
        periodicSaveJob = null
    }
}
