package com.steph.frenchradio.model

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File
import java.text.Normalizer

/**
 * Loads and saves radio stations from/to an external JSON file.
 * On first launch, copies the bundled default to the app's external files directory.
 * The user can then edit the file directly to add/remove/update stations.
 *
 * File location: /sdcard/Android/data/com.steph.frenchradio/files/stations.json
 */
object StationLoader {

    private const val FILENAME = "stations.json"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * Returns the external stations file, creating it from bundled resource if needed.
     */
    fun getStationsFile(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
            ?: context.filesDir // fallback to internal storage
        val file = File(externalDir, FILENAME)
        if (!file.exists()) {
            // Copy bundled default to external storage
            val raw = context.resources.openRawResource(
                context.resources.getIdentifier("stations", "raw", context.packageName)
            )
            raw.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file
    }

    /**
     * Loads stations from the external file, sorted alphabetically.
     * Falls back to bundled resource if file is unreadable.
     */
    fun loadStations(context: Context): List<RadioStation> {
        return try {
            val file = getStationsFile(context)
            val text = file.readText()
            parseStations(text)
        } catch (e: Exception) {
            // Fallback: load from bundled resource
            loadFromResource(context)
        }
    }

    /**
     * Loads stations from the bundled raw resource (fallback).
     */
    fun loadFromResource(context: Context): List<RadioStation> {
        val raw = context.resources.openRawResource(
            context.resources.getIdentifier("stations", "raw", context.packageName)
        )
        val text = raw.bufferedReader().use { it.readText() }
        return parseStations(text)
    }

    /**
     * Parses a JSON string into a sorted list of stations.
     */
    fun parseStations(jsonString: String): List<RadioStation> {
        val wrapper = json.decodeFromString<StationsWrapper>(jsonString)
        return wrapper.stations.sortedBy { stripAccents(it.name).lowercase() }
    }

    /**
     * Saves the full station list to the external JSON file.
     */
    fun saveStations(context: Context, stations: List<RadioStation>) {
        val file = getStationsFile(context)
        val wrapper = StationsWrapper(stations.sortedBy { stripAccents(it.name).lowercase() })
        file.writeText(json.encodeToString(StationsWrapper.serializer(), wrapper))
    }

    /**
     * Adds a station and persists. Returns the updated sorted list.
     */
    fun addStation(context: Context, station: RadioStation): List<RadioStation> {
        val current = loadStations(context).toMutableList()
        current.add(station)
        val sorted = current.sortedBy { stripAccents(it.name).lowercase() }
        saveStations(context, sorted)
        return sorted
    }

    /**
     * Updates a station by ID and persists. Returns the updated sorted list.
     */
    fun updateStation(context: Context, stationId: String, station: RadioStation): List<RadioStation> {
        val current = loadStations(context).toMutableList()
        val index = current.indexOfFirst { it.id == stationId }
        if (index >= 0) {
            current[index] = station
        }
        val sorted = current.sortedBy { stripAccents(it.name).lowercase() }
        saveStations(context, sorted)
        return sorted
    }

    /**
     * Deletes a station by ID and persists. Returns the updated sorted list.
     */
    fun deleteStation(context: Context, stationId: String): List<RadioStation> {
        val current = loadStations(context).filter { it.id != stationId }
        saveStations(context, current)
        return current
    }

    /**
     * Resets the external file by re-copying the bundled resource.
     * Useful if the user corrupts their file.
     */
    fun resetToDefault(context: Context): File {
        val file = getStationsFile(context)
        if (file.exists()) file.delete()
        return getStationsFile(context)
    }

    private fun stripAccents(s: String): String {
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
