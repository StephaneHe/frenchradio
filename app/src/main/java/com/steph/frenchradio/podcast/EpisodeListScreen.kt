package com.steph.frenchradio.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.steph.frenchradio.model.ListenHistoryEntry
import com.steph.frenchradio.model.ListenStatus
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import com.steph.frenchradio.ui.EpisodeListenBadge
import com.steph.frenchradio.ui.shareEpisode
import com.steph.frenchradio.ui.sharePodcast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    channel: PodcastChannel,
    episodes: List<PodcastEpisode>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    listenHistory: Map<String, ListenHistoryEntry> = emptyMap(),
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = channel.artworkUrl,
                contentDescription = channel.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { sharePodcast(context, channel) }) {
                Icon(Icons.Default.Share, contentDescription = "Partager le podcast")
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            items(episodes) { episode ->
                EpisodeRow(
                    episode = episode,
                    channel = channel,
                    onPlay = { onPlayEpisode(episode) },
                    listenEntry = listenHistory[episode.audioUrl],
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            }

            if (!isLoading && error == null && episodes.isEmpty()) {
                item {
                    Text(
                        "No episodes found",
                        modifier = Modifier.padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeRow(
    episode: PodcastEpisode,
    channel: PodcastChannel,
    onPlay: () -> Unit,
    listenEntry: ListenHistoryEntry? = null,
) {
    val context = LocalContext.current
    val isPlayed = listenEntry?.status == ListenStatus.PLAYED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyMedium,
                // Played episodes are slightly de-emphasized (still fully readable)
                color = if (isPlayed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            EpisodeListenBadge(listenEntry, modifier = Modifier.padding(vertical = 2.dp))
            Row {
                if (episode.publishDate.isNotBlank()) {
                    Text(
                        text = episode.publishDate.take(16),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (episode.durationSeconds > 0) {
                    Text(
                        text = " · ${formatDuration(episode.durationSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        IconButton(onClick = { shareEpisode(context, episode, channel) }) {
            Icon(Icons.Default.Share, contentDescription = "Partager l'épisode")
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h${m.toString().padStart(2, '0')}"
    else "${m} min"
}
