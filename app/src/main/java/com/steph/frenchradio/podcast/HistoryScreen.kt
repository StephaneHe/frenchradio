package com.steph.frenchradio.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.steph.frenchradio.model.ListenHistoryEntry
import com.steph.frenchradio.model.ListenStatus
import com.steph.frenchradio.player.PlayerController
import com.steph.frenchradio.ui.EpisodeListenBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Listening history: every podcast episode played, newest first.
 * Tap replays (resuming partially-listened episodes at their saved position).
 */
@Composable
fun HistoryScreen(
    viewModel: PodcastViewModel,
    playerController: PlayerController,
) {
    val uiState by viewModel.uiState.collectAsState()
    HistoryContent(
        entries = uiState.listenHistory,
        onPlay = { entry ->
            val (episode, channel) = viewModel.buildReplayData(entry)
            playerController.playPodcast(episode, channel)
        },
        onRemove = { viewModel.onRemoveListenHistory(it) },
        onClearAll = { viewModel.onClearListenHistory() },
    )
}

@Composable
fun HistoryContent(
    entries: List<ListenHistoryEntry>,
    onPlay: (ListenHistoryEntry) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Effacer l'historique ?") },
            text = { Text("Tous les épisodes écoutés seront retirés de l'historique et les marqueurs « Lu » disparaîtront.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    onClearAll()
                }) { Text("Effacer") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Annuler") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Historique d'écoute",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
                TextButton(onClick = { showClearConfirm = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Effacer l'historique")
                }
            }
        }

        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aucun épisode écouté pour l'instant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                items(entries, key = { "lh_${it.episodeAudioUrl}" }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onPlay = { onPlay(entry) },
                        onRemove = { onRemove(entry.episodeAudioUrl) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: ListenHistoryEntry,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = entry.channelArtwork,
            contentDescription = entry.channelName,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.episodeTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EpisodeListenBadge(entry)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = historyDetail(entry),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Retirer de l'historique", modifier = Modifier.size(20.dp))
        }
    }
}

/** "25/09/2026 14:32" + position ("12 min / 45 min") for partial listens. */
private fun historyDetail(entry: ListenHistoryEntry): String {
    val date = if (entry.lastListenedAt > 0) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(entry.lastListenedAt))
    } else ""
    val position = when {
        entry.status == ListenStatus.PLAYED -> ""
        entry.durationMs > 0 -> "${formatMinutes(entry.positionMs)} / ${formatMinutes(entry.durationMs)}"
        entry.positionMs > 0 -> formatMinutes(entry.positionMs)
        else -> ""
    }
    return listOf(date, position).filter { it.isNotEmpty() }.joinToString(" · ")
}

private fun formatMinutes(ms: Long): String {
    val totalMinutes = ms / 60_000
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h${m.toString().padStart(2, '0')}" else "$m min"
}
