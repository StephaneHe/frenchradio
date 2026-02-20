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
            if (uiState.searchQuery.isBlank() && uiState.history.isEmpty() && uiState.searchResults.isEmpty()) {
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
