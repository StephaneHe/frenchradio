package com.steph.frenchradio

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.steph.frenchradio.data.DataStoreAppPreferences
import com.steph.frenchradio.model.StationLoader
import com.steph.frenchradio.navigation.AppNavigation
import com.steph.frenchradio.player.ExoPlayerAudioEngine
import com.steph.frenchradio.player.PlaybackService
import com.steph.frenchradio.player.SimplePlayerController
import com.steph.frenchradio.podcast.DefaultUrlFetcher
import com.steph.frenchradio.podcast.ItunesSearchApi
import com.steph.frenchradio.podcast.PodcastViewModel
import com.steph.frenchradio.podcast.RssFeedParser
import com.steph.frenchradio.radio.RadioListViewModel
import com.steph.frenchradio.ui.theme.FrenchRadioTheme

class MainActivity : ComponentActivity() {

    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dependencies
        val prefs = DataStoreAppPreferences(this)
        val stations = StationLoader.loadStations(this)

        val radioViewModel = RadioListViewModel(stations, prefs)
        val podcastViewModel = PodcastViewModel(
            searchApi = ItunesSearchApi(),
            feedParser = RssFeedParser(),
            urlFetcher = DefaultUrlFetcher(),
            prefs = prefs,
        )

        // Connect to PlaybackService via MediaController.
        // The service creates the ExoPlayer with WAKE_MODE_NETWORK,
        // so playback survives screen lock.
        val sessionToken = SessionToken(this,
            ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()

            // The service exposes the ExoPlayer via companion; wrap it
            // in our AudioEngine abstraction.
            val player = PlaybackService.player ?: return@addListener
            val audioEngine = ExoPlayerAudioEngine(player)
            val playerController = SimplePlayerController(audioEngine)

            setContent {
                FrenchRadioTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            radioViewModel = radioViewModel,
                            podcastViewModel = podcastViewModel,
                            playerController = playerController,
                        )
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        mediaController?.release()
        mediaController = null
        super.onDestroy()
    }
}
