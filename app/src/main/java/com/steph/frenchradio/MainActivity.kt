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
import com.steph.frenchradio.model.RadioStation
import com.steph.frenchradio.model.StationLoader
import com.steph.frenchradio.navigation.AppNavigation
import com.steph.frenchradio.player.AudioBoostController
import com.steph.frenchradio.player.ExoPlayerAudioEngine
import com.steph.frenchradio.player.PlaybackService
import com.steph.frenchradio.player.SimplePlayerController
import com.steph.frenchradio.podcast.AggregatedSearchApi
import com.steph.frenchradio.podcast.DefaultUrlFetcher
import com.steph.frenchradio.podcast.FyydSearchApi
import com.steph.frenchradio.podcast.ItunesSearchApi
import com.steph.frenchradio.podcast.PodcastIndexSearchApi
import com.steph.frenchradio.podcast.PodcastViewModel
import com.steph.frenchradio.podcast.RssFeedParser
import com.steph.frenchradio.radio.RadioListViewModel
import com.steph.frenchradio.radio.StationWriter
import com.steph.frenchradio.ui.theme.FrenchRadioTheme
import kotlinx.coroutines.MainScope

class MainActivity : ComponentActivity() {

    private var mediaController: MediaController? = null
    private var audioBoostController: AudioBoostController? = null
    private val appScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dependencies
        val prefs = DataStoreAppPreferences(this)
        val stations = StationLoader.loadStations(this)
        val context = this

        // Station writer for in-app editing
        val stationWriter = object : StationWriter {
            override fun addStation(station: RadioStation): List<RadioStation> =
                StationLoader.addStation(context, station)

            override fun updateStation(stationId: String, station: RadioStation): List<RadioStation> =
                StationLoader.updateStation(context, stationId, station)

            override fun deleteStation(stationId: String): List<RadioStation> =
                StationLoader.deleteStation(context, stationId)
        }

        val radioViewModel = RadioListViewModel(stations, prefs, stationWriter)
        val podcastViewModel = PodcastViewModel(
            searchApi = AggregatedSearchApi(listOf(
                ItunesSearchApi(),
                FyydSearchApi(),
                PodcastIndexSearchApi(), // clé vide → silencieux jusqu'à config
            )),
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
            val playerController = SimplePlayerController(audioEngine, prefs, appScope)
            val boostController = AudioBoostController(player, prefs, appScope)
            audioBoostController = boostController

            setContent {
                FrenchRadioTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            radioViewModel = radioViewModel,
                            podcastViewModel = podcastViewModel,
                            playerController = playerController,
                            audioBoostController = boostController,
                        )
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        audioBoostController?.release()
        audioBoostController = null
        mediaController?.release()
        mediaController = null
        super.onDestroy()
    }
}
