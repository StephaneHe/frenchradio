package com.steph.frenchradio.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import com.steph.frenchradio.player.PlayerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel,
    playerController: PlayerController,
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.selectedChannel != null) {
        EpisodeListScreen(
            channel = uiState.selectedChannel!!,
            episodes = uiState.episodes,
            isLoading = uiState.isLoadingEpisodes,
            error = uiState.episodeError,
            onBack = { viewModel.onBackFromEpisodes() },
            onPlayEpisode = { episode ->
                viewModel.onPlayEpisode(episode)
                playerController.playPodcast(episode, uiState.selectedChannel!!)
            },
        )
    } else {
        PodcastSearchScreen(
            uiState = uiState,
            onSearchChanged = { viewModel.onSearchQueryChanged(it) },
            onChannelClick = { viewModel.onChannelSelected(it) },
            onRemoveHistory = { viewModel.onRemoveFromHistory(it) },
            onResumeEpisode = { progress ->
                val (episode, channel) = viewModel.buildResumeData(progress)
                playerController.playPodcast(episode, channel)
            },
            onRemoveProgress = { viewModel.onRemoveProgress(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastSearchScreen(
    uiState: PodcastUiState,
    onSearchChanged: (String) -> Unit,
    onChannelClick: (PodcastChannel) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onResumeEpisode: (EpisodeProgress) -> Unit = {},
    onRemoveProgress: (String) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Search podcasts...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
        )

        if (uiState.isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (uiState.searchError != null) {
            Text(
                text = uiState.searchError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            // Search results
            if (uiState.searchResults.isNotEmpty()) {
                item {
                    Text(
                        "Results",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(uiState.searchResults, key = { "search_${it.id}" }) { channel ->
                    PodcastChannelRow(
                        channel = channel,
                        onClick = { onChannelClick(channel) },
                    )
                }
            }

            // In-progress episodes (only when not searching)
            if (uiState.searchQuery.isBlank() && uiState.inProgress.isNotEmpty()) {
                item {
                    Text(
                        "In Progress",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(uiState.inProgress, key = { "prog_${it.episodeAudioUrl}" }) { progress ->
                    InProgressRow(
                        progress = progress,
                        onResume = { onResumeEpisode(progress) },
                        onRemove = { onRemoveProgress(progress.episodeAudioUrl) },
                    )
                }
            }

            // History
            if (uiState.searchQuery.isBlank() && uiState.history.isNotEmpty()) {
                item {
                    Text(
                        "Recent Podcasts",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(uiState.history, key = { "hist_${it.id}" }) { channel ->
                    PodcastChannelRow(
                        channel = channel,
                        onClick = { onChannelClick(channel) },
                        onRemove = { onRemoveHistory(channel.id) },
                    )
                }
            }

            // Empty state
            if (uiState.searchQuery.isBlank() && uiState.history.isEmpty()
                && uiState.searchResults.isEmpty() && uiState.inProgress.isEmpty()
            ) {
                item {
                    Text(
                        "Search for podcasts to get started",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Row showing a podcast episode in progress with a progress bar and resume button.
 */
@Composable
fun InProgressRow(
    progress: EpisodeProgress,
    onResume: () -> Unit,
    onRemove: () -> Unit,
) {
    val fraction = if (progress.durationMs > 0) {
        (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
    } else 0f

    val remaining = if (progress.durationMs > 0) {
        val remainMs = (progress.durationMs - progress.positionMs).coerceAtLeast(0)
        formatDuration(remainMs)
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onResume() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = progress.channelArtwork,
            contentDescription = progress.channelName,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = progress.episodeTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = progress.channelName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                if (remaining.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = remaining,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Format milliseconds into a human-readable duration like "1h 23m" or "45m".
 */
private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m left"
        minutes > 0 -> "${minutes}m left"
        else -> "<1m left"
    }
}

@Composable
fun PodcastChannelRow(
    channel: PodcastChannel,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = channel.artworkUrl,
            contentDescription = channel.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = channel.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(20.dp))
            }
        }
    }
}
