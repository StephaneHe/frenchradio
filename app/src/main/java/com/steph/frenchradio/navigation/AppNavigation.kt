package com.steph.frenchradio.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.steph.frenchradio.player.PlayerController
import com.steph.frenchradio.player.PlayerState
import com.steph.frenchradio.radio.RadioListScreen
import com.steph.frenchradio.radio.RadioListViewModel
import com.steph.frenchradio.podcast.PodcastScreen
import com.steph.frenchradio.podcast.PodcastViewModel
import com.steph.frenchradio.ui.MiniPlayerBar
import com.steph.frenchradio.ui.SeekDrawerContent
import kotlinx.coroutines.delay

enum class AppTab(val label: String) {
    Radio("Radio"),
    Podcasts("Podcasts"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    radioViewModel: RadioListViewModel,
    podcastViewModel: PodcastViewModel,
    playerController: PlayerController,
) {
    var selectedTab by remember { mutableStateOf(AppTab.Radio) }
    val playerState by playerController.playerState.collectAsState()
    var showSeekDrawer by remember { mutableStateOf(false) }

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

    Scaffold(
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
            }
        }
    }
}
