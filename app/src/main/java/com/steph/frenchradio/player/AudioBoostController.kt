package com.steph.frenchradio.player

import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.steph.frenchradio.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AudioBoostController(
    private val player: ExoPlayer,
    private val prefs: AppPreferences,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "AudioBoostController"
        const val MAX_GAIN_MB = 2000

        fun percentToGainMb(percent: Int): Int =
            percent.coerceIn(0, 100) * (MAX_GAIN_MB / 100)

        fun percentToGainDb(percent: Int): Int =
            percentToGainMb(percent) / 100
    }

    private val _boostPercent = MutableStateFlow(0)
    val boostPercent: StateFlow<Int> = _boostPercent.asStateFlow()

    private var enhancer: LoudnessEnhancer? = null

    private val sessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachEnhancer(audioSessionId)
        }
    }

    init {
        player.addListener(sessionListener)
        if (player.audioSessionId != 0) {
            attachEnhancer(player.audioSessionId)
        }
        scope.launch {
            val saved = prefs.audioBoostPercent.first()
            _boostPercent.value = saved
            applyGain()
        }
    }

    fun setBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _boostPercent.value = clamped
        applyGain()
        scope.launch { prefs.setAudioBoostPercent(clamped) }
    }

    private fun attachEnhancer(sessionId: Int) {
        try {
            enhancer?.release()
        } catch (_: Exception) {}
        enhancer = if (sessionId != 0) {
            try {
                LoudnessEnhancer(sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot attach LoudnessEnhancer to session $sessionId: ${e.message}")
                null
            }
        } else null
        applyGain()
    }

    private fun applyGain() {
        val gain = percentToGainMb(_boostPercent.value)
        try {
            enhancer?.setTargetGain(gain)
        } catch (e: Exception) {
            Log.w(TAG, "setTargetGain($gain) failed: ${e.message}")
        }
    }

    fun release() {
        try { player.removeListener(sessionListener) } catch (_: Exception) {}
        try { enhancer?.release() } catch (_: Exception) {}
        enhancer = null
    }
}
