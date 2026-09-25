package com.steph.frenchradio.ui

import android.content.Context
import android.content.Intent
import com.steph.frenchradio.model.EpisodeProgress
import com.steph.frenchradio.model.PodcastChannel
import com.steph.frenchradio.model.PodcastEpisode

fun sharePodcast(context: Context, channel: PodcastChannel) {
    val text = buildString {
        append(channel.name)
        if (channel.author.isNotBlank()) append("\nPar ${channel.author}")
        append("\n\n${channel.feedUrl}")
        append("\n\nPartagé depuis FrenchRadio")
    }
    launchShareIntent(context, text)
}

fun shareEpisode(context: Context, episode: PodcastEpisode, channel: PodcastChannel) {
    val text = buildString {
        append(episode.title)
        append("\n${channel.name}")
        if (episode.publishDate.isNotBlank()) append(" · ${episode.publishDate.take(10)}")
        append("\n\n${episode.audioUrl}")
        append("\n\nFlux du podcast : ${channel.feedUrl}")
        append("\n\nPartagé depuis FrenchRadio")
    }
    launchShareIntent(context, text)
}

fun shareEpisodeProgress(context: Context, progress: EpisodeProgress) {
    val text = buildString {
        append(progress.episodeTitle)
        append("\n${progress.channelName}")
        append("\n\n${progress.episodeAudioUrl}")
        if (progress.feedUrl.isNotBlank()) append("\n\nFlux du podcast : ${progress.feedUrl}")
        append("\n\nPartagé depuis FrenchRadio")
    }
    launchShareIntent(context, text)
}

private fun launchShareIntent(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
