package com.steph.frenchradio.player

/**
 * Thin abstraction over ExoPlayer for testability.
 */
interface AudioEngine {
    fun prepare(url: String)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun release()
    val isPlaying: Boolean
    val currentPositionMs: Long
    val durationMs: Long

    /** Register a listener for playback events. */
    fun setListener(listener: Listener)

    interface Listener {
        fun onBuffering()
        fun onReady()
        fun onEnded()
        fun onError(message: String)
    }
}
