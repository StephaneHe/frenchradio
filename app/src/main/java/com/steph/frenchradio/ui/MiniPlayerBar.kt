package com.steph.frenchradio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steph.frenchradio.player.AudioBoostController
import com.steph.frenchradio.player.PlayerState

@Composable
fun MiniPlayerBar(
    state: PlayerState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekBack: () -> Unit = {},
    onSeekForward: () -> Unit = {},
    onOpenSeekDrawer: () -> Unit = {},
    onOpenBoost: () -> Unit = {},
    boostPercent: Int = 0,
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Error or buffering indicator
            if (state.isError) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = state.currentTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.currentSubtitle.isNotEmpty()) {
                    Text(
                        text = if (state.isError) state.errorMessage ?: "Error"
                               else state.currentSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Rewind 10s (podcast only)
            if (!state.isRadio) {
                IconButton(onClick = onSeekBack) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10 seconds")
                }
            }

            // Play/Pause
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                )
            }

            // Forward 30s (podcast only)
            if (!state.isRadio) {
                IconButton(onClick = onSeekForward) {
                    Icon(Icons.Default.Forward30, contentDescription = "Forward 30 seconds")
                }
            }

            // Seek drawer (podcast only, when duration is known)
            if (!state.isRadio && state.durationMs > 0) {
                IconButton(onClick = onOpenSeekDrawer) {
                    Icon(Icons.Default.Tune, contentDescription = "Navigation avancée")
                }
            }

            // Boost (radio + podcast)
            BoostButton(boostPercent = boostPercent, onClick = onOpenBoost)

            // Stop
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Close, contentDescription = "Stop")
            }
        }
    }
}

@Composable
private fun BoostButton(boostPercent: Int, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.GraphicEq, contentDescription = "Booster le son")
        }
        if (boostPercent > 0) {
            val gainDb = AudioBoostController.percentToGainDb(boostPercent)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp),
            ) {
                Text(
                    text = "+$gainDb",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
    }
}
