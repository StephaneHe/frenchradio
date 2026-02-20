package com.steph.frenchradio.player

data class PlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val currentTitle: String = "",
    val currentSubtitle: String = "",
    val currentArtwork: String? = null,
    val currentStreamUrl: String? = null,
    val isRadio: Boolean = true,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
)
