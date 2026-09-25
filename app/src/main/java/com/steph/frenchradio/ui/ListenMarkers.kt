package com.steph.frenchradio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steph.frenchradio.model.ListenHistoryEntry
import com.steph.frenchradio.model.ListenStatus

/** Aggregated listening state of a podcast channel, computed from the history. */
data class ChannelListenSummary(val playedCount: Int, val startedCount: Int)

/** Groups the history by feed URL (stable across search sources, unlike channel ids). */
fun List<ListenHistoryEntry>.summaryByFeedUrl(): Map<String, ChannelListenSummary> =
    groupBy { it.feedUrl }.mapValues { (_, entries) ->
        ChannelListenSummary(
            playedCount = entries.count { it.status == ListenStatus.PLAYED },
            startedCount = entries.count { it.status == ListenStatus.STARTED },
        )
    }

/** Small rounded pill with an optional icon — the building block of the markers. */
@Composable
private fun ListenPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(text = text, fontSize = 10.sp)
        }
    }
}

/** "Lu" (check) or "En cours" + mini progress bar for an episode. Renders nothing if never listened. */
@Composable
fun EpisodeListenBadge(entry: ListenHistoryEntry?, modifier: Modifier = Modifier) {
    if (entry == null) return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when (entry.status) {
            ListenStatus.PLAYED -> ListenPill(
                text = "Lu",
                icon = Icons.Default.CheckCircle,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            ListenStatus.STARTED -> {
                ListenPill(
                    text = "En cours",
                    icon = Icons.Default.Headphones,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                if (entry.durationMs > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = { entry.fraction },
                        modifier = Modifier
                            .width(48.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}

/** Channel-level marker: "N lu(s)" and/or "En cours". Renders nothing if never listened. */
@Composable
fun ChannelListenBadge(summary: ChannelListenSummary?, modifier: Modifier = Modifier) {
    if (summary == null || (summary.playedCount == 0 && summary.startedCount == 0)) return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (summary.playedCount > 0) {
            ListenPill(
                text = if (summary.playedCount == 1) "1 lu" else "${summary.playedCount} lus",
                icon = Icons.Default.CheckCircle,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        if (summary.playedCount > 0 && summary.startedCount > 0) {
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (summary.startedCount > 0) {
            ListenPill(
                text = "En cours",
                icon = Icons.Default.Headphones,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
