package com.steph.frenchradio.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Real AudioEngine backed by ExoPlayer.
 */
class ExoPlayerAudioEngine(context: Context) : AudioEngine {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private var listener: AudioEngine.Listener? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> listener?.onBuffering()
                    Player.STATE_READY -> listener?.onReady()
                    Player.STATE_ENDED -> listener?.onEnded()
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val msg = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                        "No internet connection"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Stream unavailable"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Connection timed out"
                    else -> error.localizedMessage ?: "Playback error"
                }
                listener?.onError(msg)
            }
        })
    }

    override fun prepare(url: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    override fun release() {
        exoPlayer.release()
    }

    override val isPlaying: Boolean
        get() = exoPlayer.isPlaying

    override val currentPositionMs: Long
        get() = exoPlayer.currentPosition

    override val durationMs: Long
        get() = exoPlayer.duration

    override fun setListener(listener: AudioEngine.Listener) {
        this.listener = listener
    }
}
