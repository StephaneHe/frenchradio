package com.steph.frenchradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.steph.frenchradio.data.DataStoreAppPreferences
import com.steph.frenchradio.model.StationLoader
import com.steph.frenchradio.navigation.AppNavigation
import com.steph.frenchradio.player.ExoPlayerAudioEngine
import com.steph.frenchradio.player.SimplePlayerController
import com.steph.frenchradio.podcast.DefaultUrlFetcher
import com.steph.frenchradio.podcast.ItunesSearchApi
import com.steph.frenchradio.podcast.PodcastViewModel
import com.steph.frenchradio.podcast.RssFeedParser
import com.steph.frenchradio.radio.RadioListViewModel
import com.steph.frenchradio.ui.theme.FrenchRadioTheme

class MainActivity : ComponentActivity() {

    private lateinit var audioEngine: ExoPlayerAudioEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dependencies
        val prefs = DataStoreAppPreferences(this)
        val stations = StationLoader.loadStations(this)
        audioEngine = ExoPlayerAudioEngine(this)
        val playerController = SimplePlayerController(audioEngine)

        val radioViewModel = RadioListViewModel(stations, prefs)
        val podcastViewModel = PodcastViewModel(
            searchApi = ItunesSearchApi(),
            feedParser = RssFeedParser(),
            urlFetcher = DefaultUrlFetcher(),
            prefs = prefs,
        )

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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioEngine.isInitialized) {
            audioEngine.release()
        }
    }
}

