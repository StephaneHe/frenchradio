package com.steph.frenchradio.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.steph.frenchradio.BuildConfig
import com.steph.frenchradio.player.AudioBoostController
import com.steph.frenchradio.player.PlayerController
import com.steph.frenchradio.player.PlayerState
import com.steph.frenchradio.radio.RadioListScreen
import com.steph.frenchradio.radio.RadioListViewModel
import com.steph.frenchradio.podcast.HistoryScreen
import com.steph.frenchradio.podcast.PodcastScreen
import com.steph.frenchradio.podcast.PodcastViewModel
import com.steph.frenchradio.ui.AudioBoostSheet
import com.steph.frenchradio.ui.MiniPlayerBar
import com.steph.frenchradio.ui.SeekDrawerContent
import kotlinx.coroutines.delay

enum class AppTab(val label: String) {
    Radio("Radio"),
    Podcasts("Podcasts"),
    History("Historique"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    radioViewModel: RadioListViewModel,
    podcastViewModel: PodcastViewModel,
    playerController: PlayerController,
    audioBoostController: AudioBoostController,
) {
    var selectedTab by remember { mutableStateOf(AppTab.Radio) }
    val playerState by playerController.playerState.collectAsState()
    var showSeekDrawer by remember { mutableStateOf(false) }
    var showBoostSheet by remember { mutableStateOf(false) }
    val boostPercent by audioBoostController.boostPercent.collectAsState()

    // Keep position up-to-date while the seek drawer is open
    LaunchedEffect(showSeekDrawer) {
        if (showSeekDrawer) {
            while (true) {
                playerController.refreshPosition()
                delay(500)
            }
        }
    }

    // Seek drawer bottom sheet
    if (showSeekDrawer && !playerState.isRadio) {
        ModalBottomSheet(
            onDismissRequest = { showSeekDrawer = false },
        ) {
            SeekDrawerContent(
                positionMs = playerState.positionMs,
                durationMs = playerState.durationMs,
                onSeek = { playerController.seekTo(it) },
            )
        }
    }

    // Audio boost bottom sheet
    if (showBoostSheet) {
        AudioBoostSheet(
            boostPercent = boostPercent,
            onBoostChange = { audioBoostController.setBoostPercent(it) },
            onDismiss = { showBoostSheet = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FrenchRadio") },
                actions = {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
        bottomBar = {
            Column {
                if (playerState.currentTitle.isNotEmpty()) {
                    MiniPlayerBar(
                        state = playerState,
                        onPlayPause = {
                            if (playerState.isPlaying) playerController.pause()
                            else playerController.resume()
                        },
                        onStop = { playerController.stop() },
                        onSeekBack = {
                            val newPos = (playerState.positionMs - 10_000).coerceAtLeast(0)
                            playerController.seekTo(newPos)
                        },
                        onSeekForward = {
                            val newPos = (playerState.positionMs + 30_000).let { pos ->
                                if (playerState.durationMs > 0) pos.coerceAtMost(playerState.durationMs)
                                else pos
                            }
                            playerController.seekTo(newPos)
                        },
                        onOpenSeekDrawer = { showSeekDrawer = true },
                        onOpenBoost = { showBoostSheet = true },
                        boostPercent = boostPercent,
                    )
                }
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.Radio,
                        onClick = { selectedTab = AppTab.Radio },
                        icon = { Icon(Icons.Default.Radio, contentDescription = "Radio") },
                        label = { Text("Radio") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.Podcasts,
                        onClick = { selectedTab = AppTab.Podcasts },
                        icon = { Icon(Icons.Default.Headphones, contentDescription = "Podcasts") },
                        label = { Text("Podcasts") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.History,
                        onClick = { selectedTab = AppTab.History },
                        icon = { Icon(Icons.Default.History, contentDescription = "Historique") },
                        label = { Text("Historique") },
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                AppTab.Radio -> RadioListScreen(
                    viewModel = radioViewModel,
                    onStationClick = { station -> playerController.playRadio(station) },
                )
                AppTab.Podcasts -> PodcastScreen(
                    viewModel = podcastViewModel,
                    playerController = playerController,
                )
                AppTab.History -> HistoryScreen(
                    viewModel = podcastViewModel,
                    playerController = playerController,
                )
            }
        }
    }
}
