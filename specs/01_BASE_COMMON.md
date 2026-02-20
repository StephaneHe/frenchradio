# 01 - Base Common

## Purpose

Project skeleton: Gradle config, navigation (two tabs), data models,
theme, and shared interfaces.

## Gradle Dependencies

- Jetpack Compose (Material 3 + BOM)
- Navigation Compose (for tabs)
- AndroidX Media3 ExoPlayer
- Ktor Client (Android engine + content negotiation + serialization)
- Kotlinx Serialization JSON
- DataStore Preferences
- Coil Compose (image loading)
- JUnit 4, Mockk, Robolectric (test)

## Data Models

```kotlin
@Serializable
data class RadioStation(
    val id: String,
    val name: String,
    @SerialName("stream_url") val streamUrl: String,
    val logo: String,
    val genres: List<String>,
    val color: String,
)

data class PodcastChannel(
    val id: String,           // iTunes ID
    val name: String,
    val author: String,
    val artworkUrl: String,
    val feedUrl: String,
    val lastPlayedAt: Long,   // timestamp
)

data class PodcastEpisode(
    val title: String,
    val description: String,
    val audioUrl: String,
    val publishDate: String,
    val durationSeconds: Int,
)
```

## Navigation

Single Activity with BottomNavigation:
- Tab "Radio" (icon: radio) → RadioScreen
- Tab "Podcasts" (icon: headphones) → PodcastScreen

Mini-player bar above the bottom nav when audio is playing.

## Theme

Dark theme by default (easy on the eyes, common for media apps).
Accent color: vibrant blue (#4FC3F7).

## Files to Create

```
app/src/main/
  java/com/steph/frenchradio/
    MainActivity.kt
    navigation/
      AppNavigation.kt
    model/
      RadioStation.kt
      PodcastChannel.kt
      PodcastEpisode.kt
    ui/theme/
      Theme.kt
  res/
    values/strings.xml
    raw/stations.json
```

## TDD Tests

### Test 1: RadioStation deserializes from JSON
- Given: a JSON string with station data
- Then: RadioStation object has correct fields

### Test 2: All stations load from bundled JSON
- Given: stations.json asset
- Then: 31 stations are loaded

### Test 3: Genre list extracted from stations
- Given: list of stations
- Then: unique sorted genre list is correct
