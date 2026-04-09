package com.steph.frenchradio.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

private data class SeekSpeed(val deltaMs: Long, val label: String, val intensity: Int)

private fun calculateSeekSpeed(fingerRatio: Float): SeekSpeed {
    val a = abs(fingerRatio)
    val dir = if (fingerRatio < 0) -1 else 1
    return when {
        a < 0.10f -> SeekSpeed(0L, "", 0)
        a < 0.30f -> SeekSpeed(10_000L * dir, "10s", 1)
        a < 0.55f -> SeekSpeed(30_000L * dir, "30s", 2)
        a < 0.80f -> SeekSpeed(60_000L * dir, "1min", 3)
        else      -> SeekSpeed(300_000L * dir, "5min", 4)
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "0:00"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}

@Composable
fun SeekDrawerContent(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var seekPosition by remember { mutableStateOf(positionMs) }
    var fingerRatio by remember { mutableStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    val seekSpeed = remember(fingerRatio) { calculateSeekSpeed(fingerRatio) }

    // Sync position from player when not seeking
    LaunchedEffect(positionMs) {
        if (!isSeeking) seekPosition = positionMs
    }

    // Periodic seek while touching
    LaunchedEffect(isSeeking, seekSpeed.deltaMs) {
        if (isSeeking && seekSpeed.deltaMs != 0L) {
            val maxPos = durationMs.coerceAtLeast(1L)
            // Immediate first seek
            seekPosition = (seekPosition + seekSpeed.deltaMs).coerceIn(0L, maxPos)
            onSeek(seekPosition)
            // Repeat while held
            while (true) {
                delay(400L)
                seekPosition = (seekPosition + seekSpeed.deltaMs).coerceIn(0L, maxPos)
                onSeek(seekPosition)
            }
        }
    }

    // Pulse animation for active zone glow
    val infiniteTransition = rememberInfiniteTransition(label = "seek")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val rewindColor = Color(0xFF42A5F5)
    val forwardColor = Color(0xFFFFA726)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Time display
        Text(
            text = "${formatTime(seekPosition)} / ${formatTime(durationMs)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Speed indicator
        val speedText = when {
            !isSeeking || seekSpeed.deltaMs == 0L -> "Touchez la piste pour naviguer"
            seekSpeed.deltaMs < 0 -> "\u25C4".repeat(seekSpeed.intensity) + " " + seekSpeed.label
            else -> seekSpeed.label + " " + "\u25BA".repeat(seekSpeed.intensity)
        }
        val speedColor = when {
            !isSeeking || seekSpeed.deltaMs == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
            seekSpeed.deltaMs < 0 -> rewindColor
            else -> forwardColor
        }
        Text(
            text = speedText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = speedColor,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Touch track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        isSeeking = true
                        fingerRatio =
                            ((down.position.x / size.width.toFloat()) * 2f - 1f).coerceIn(-1f, 1f)

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                fingerRatio =
                                    ((change.position.x / size.width.toFloat()) * 2f - 1f).coerceIn(
                                        -1f,
                                        1f,
                                    )
                                change.consume()
                            }
                        } finally {
                            isSeeking = false
                            fingerRatio = 0f
                        }
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Gradient background
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1565C0),
                            Color(0xFF64B5F6),
                            surfaceVariant,
                            Color(0xFFFFB74D),
                            Color(0xFFE65100),
                        ),
                    ),
                    alpha = if (isSeeking) 0.5f else 0.25f,
                )

                // Center line
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(w / 2, h * 0.1f),
                    end = Offset(w / 2, h * 0.9f),
                    strokeWidth = 2.dp.toPx(),
                )

                // Finger indicator + glow
                if (isSeeking) {
                    val fingerX = ((fingerRatio + 1f) / 2f) * w

                    // Radial glow
                    if (seekSpeed.deltaMs != 0L) {
                        val glowColor =
                            if (seekSpeed.deltaMs < 0) rewindColor else forwardColor
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = pulseAlpha),
                                    Color.Transparent,
                                ),
                                center = Offset(fingerX, h / 2),
                                radius = 50.dp.toPx(),
                            ),
                        )
                    }

                    // White indicator line
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(fingerX - 2.dp.toPx(), h * 0.05f),
                        size = Size(4.dp.toPx(), h * 0.9f),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }

        // Scale labels
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val labels = listOf(
                "5min" to rewindColor,
                "1min" to rewindColor,
                "30s" to rewindColor,
                "10s" to rewindColor,
                "\u25CF" to MaterialTheme.colorScheme.onSurfaceVariant,
                "10s" to forwardColor,
                "30s" to forwardColor,
                "1min" to forwardColor,
                "5min" to forwardColor,
            )
            labels.forEach { (text, color) ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f),
                )
            }
        }
    }
}
