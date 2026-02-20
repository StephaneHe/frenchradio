package com.steph.frenchradio.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RadioStation(
    val id: String,
    val name: String,
    @SerialName("stream_url") val streamUrl: String,
    val logo: String,
    val genres: List<String>,
    val color: String,
)

@Serializable
data class StationsWrapper(
    val stations: List<RadioStation>,
)

/**
 * Extract a sorted list of unique genres from a list of stations.
 */
fun extractGenres(stations: List<RadioStation>): List<String> =
    stations.flatMap { it.genres }.distinct().sorted()
