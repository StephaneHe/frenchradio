# 03 - Audio Player

## Purpose

Handle audio streaming playback for both radio and podcasts.
Uses AndroidX Media3 (ExoPlayer) with background playback
and media notification controls.

## Architecture

A single `AudioPlayerService` (MediaSessionService) handles all playback.
The UI communicates via a `PlayerController` interface.

### PlayerController Interface

```kotlin
interface PlayerController {
    val playerState: StateFlow<PlayerState>
    fun playRadio(station: RadioStation)
    fun playPodcast(episode: PodcastEpisode, channel: PodcastChannel)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)  // podcast only
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val currentTitle: String = "",
    val currentSubtitle: String = "",
    val currentArtwork: String? = null,
    val isRadio: Boolean = true,  // vs podcast
    val durationMs: Long = 0,     // podcast only
    val positionMs: Long = 0,     // podcast only
)
```

## Error Handling Strategy

### Stream failures

1. **Buffering timeout** (10s): Show "Connecting..." then "Connection failed"
2. **HTTP error** (4xx/5xx): Show "Stream unavailable"
3. **Network error**: Show "No internet connection"
4. **Auto-retry**: For radio, retry 3 times with 2s backoff
5. **User action**: Show "Retry" button after all retries fail

### Visual indicators on station grid

- Station currently playing: highlighted border + animated equalizer icon
- Station with error: greyed out with small error badge (temporary)

## Media Notification

Using Media3 MediaSession:
- Shows station/episode name and artwork
- Play/Pause button
- Stop button
- For podcasts: seek forward/backward 15s buttons

## Mini Player Bar

Composable bar shown above bottom navigation:
```
+--[Logo]--[Title / Subtitle]--[⏸/▶]--[✕]-+
```
- Tap anywhere (except buttons) → expand to full player (future)
- Pause/Resume toggle
- Stop (✕) button

## Foreground Service

Required for background playback on Android 12+.
Uses `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.

## TDD Tests

### Test 1: playRadio updates state with station info
### Test 2: playPodcast updates state with episode info
### Test 3: pause sets isPlaying to false
### Test 4: resume sets isPlaying to true
### Test 5: stop resets state to default
### Test 6: error state is set on stream failure
### Test 7: isRadio flag is correct for radio vs podcast
### Test 8: seekTo updates position for podcast

## Files

```
app/src/main/java/com/steph/frenchradio/
  player/
    PlayerController.kt      (interface)
    PlayerState.kt
    AudioPlayerService.kt
    ExoPlayerController.kt   (implementation)
  ui/
    MiniPlayerBar.kt
app/src/test/java/com/steph/frenchradio/
  player/
    ExoPlayerControllerTest.kt
```

## Manifest Additions

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.INTERNET" />

<service
    android:name=".player.AudioPlayerService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```
