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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode
import com.steph.frenchradio.player.PlayerController
import com.steph.frenchradio.ui.ChannelListenBadge
import com.steph.frenchradio.ui.ChannelListenSummary
import com.steph.frenchradio.ui.summaryByFeedUrl
import com.steph.frenchradio.ui.shareEpisodeProgress
import com.steph.frenchradio.ui.sharePodcast

private val CATEGORIES = listOf(
    "Actualités", "Culture", "Science", "Histoire", "Humour", "Société", "Politique",
    "Santé", "Technologie", "Sport", "Économie", "Arts", "Éducation", "Religion", "Vrai crime",
)

@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel,
    playerController: PlayerController,
) {
    val uiState by viewModel.uiState.collectAsState()
    val historyByUrl = remember(uiState.listenHistory) {
        uiState.listenHistory.associateBy { it.episodeAudioUrl }
    }

    if (uiState.selectedChannel != null) {
        EpisodeListScreen(
            listenHistory = historyByUrl,
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
            onBrowseCategory = { viewModel.onBrowseCategorySelected(it) },
            onClearBrowse = { viewModel.onClearBrowse() },
        )
    }
}

@Composable
fun PodcastSearchScreen(
    uiState: PodcastUiState,
    onSearchChanged: (String) -> Unit,
    onChannelClick: (PodcastChannel) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onResumeEpisode: (EpisodeProgress) -> Unit = {},
    onRemoveProgress: (String) -> Unit = {},
    onBrowseCategory: (String) -> Unit = {},
    onClearBrowse: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(0) }
    val listenSummaries = remember(uiState.listenHistory) { uiState.listenHistory.summaryByFeedUrl() }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Rechercher") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Parcourir") },
            )
        }

        when (selectedTab) {
            0 -> SearchTab(
                uiState = uiState,
                onSearchChanged = onSearchChanged,
                onChannelClick = onChannelClick,
                onRemoveHistory = onRemoveHistory,
                onResumeEpisode = onResumeEpisode,
                onRemoveProgress = onRemoveProgress,
                onCategoryClick = onBrowseCategory,
                listenSummaries = listenSummaries,
            )
            1 -> BrowseTab(
                listenSummaries = listenSummaries,
                browseCategory = uiState.browseCategory,
                browseResults = uiState.browseResults,
                isBrowsing = uiState.isBrowsing,
                browseError = uiState.browseError,
                onCategoryClick = onBrowseCategory,
                onClearBrowse = onClearBrowse,
                onChannelClick = onChannelClick,
            )
        }
    }
}

@Composable
private fun SearchTab(
    uiState: PodcastUiState,
    onSearchChanged: (String) -> Unit,
    onChannelClick: (PodcastChannel) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onResumeEpisode: (EpisodeProgress) -> Unit,
    onRemoveProgress: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    listenSummaries: Map<String, ChannelListenSummary>,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
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
                        listenSummary = listenSummaries[channel.feedUrl],
                    )
                }
            }

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
                        onShare = { shareEpisodeProgress(context, progress) },
                        onRemove = { onRemoveProgress(progress.episodeAudioUrl) },
                    )
                }
            }

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
                        onShare = { sharePodcast(context, channel) },
                        onRemove = { onRemoveHistory(channel.id) },
                        listenSummary = listenSummaries[channel.feedUrl],
                    )
                }
            }

            // Empty state: category suggestion grid
            if (uiState.searchQuery.isBlank() && uiState.history.isEmpty()
                && uiState.searchResults.isEmpty() && uiState.inProgress.isEmpty()
            ) {
                item {
                    CategoryGrid(
                        title = "Suggestions",
                        onCategoryClick = onCategoryClick,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseTab(
    browseCategory: String?,
    browseResults: List<PodcastChannel>,
    isBrowsing: Boolean,
    browseError: String?,
    onCategoryClick: (String) -> Unit,
    onClearBrowse: () -> Unit,
    onChannelClick: (PodcastChannel) -> Unit,
    listenSummaries: Map<String, ChannelListenSummary>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (browseCategory == null) {
            item {
                CategoryGrid(title = "Catégories", onCategoryClick = onCategoryClick)
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClearBrowse) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                    Text(
                        text = browseCategory,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }

            if (isBrowsing) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            if (browseError != null) {
                item {
                    Text(
                        text = browseError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            if (!isBrowsing && browseError == null && browseResults.isEmpty()) {
                item {
                    Text(
                        text = "Aucun résultat pour cette catégorie",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }

            items(browseResults, key = { "browse_${it.id}" }) { channel ->
                PodcastChannelRow(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    listenSummary = listenSummaries[channel.feedUrl],
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    title: String,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        CATEGORIES.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEach { category ->
                    SuggestionChip(
                        onClick = { onCategoryClick(category) },
                        label = { Text(category, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PodcastChannelRow(
    channel: PodcastChannel,
    onClick: () -> Unit,
    onShare: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    listenSummary: ChannelListenSummary? = null,
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
            val hasListens = listenSummary != null &&
                (listenSummary.playedCount > 0 || listenSummary.startedCount > 0)
            if (channel.source.isNotEmpty() || hasListens) {
                Row(
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (channel.source.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = channel.source,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                        if (hasListens) Spacer(modifier = Modifier.width(4.dp))
                    }
                    ChannelListenBadge(listenSummary)
                }
            }
            Text(
                text = channel.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onShare != null) {
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Partager", modifier = Modifier.size(20.dp))
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun InProgressRow(
    progress: EpisodeProgress,
    onResume: () -> Unit,
    onShare: () -> Unit = {},
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
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Partager", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(20.dp))
        }
    }
}

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
