package com.steph.frenchradio.model

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File
import java.text.Normalizer

/**
 * Loads radio stations from an external JSON file.
 * On first launch, copies the bundled default to the app's external files directory.
 * The user can then edit the file directly to add/remove/update stations.
 *
 * File location: /sdcard/Android/data/com.steph.frenchradio/files/stations.json
 */
object StationLoader {

    private const val FILENAME = "stations.json"

    private val json = Json { ignoreUnknownKeys = true }

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
     * Resets the external file by re-copying the bundled resource.
     * Useful if the user corrupts their file.
     */
    private fun stripAccents(s: String): String {
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }


    fun resetToDefault(context: Context): File {
        val file = getStationsFile(context)
        if (file.exists()) file.delete()
        return getStationsFile(context)
    }
}

