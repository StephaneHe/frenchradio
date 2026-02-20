# 05 - Persistence

## Purpose

Persist user data across app restarts:
- Radio favorite station IDs
- Podcast channel history

## Technology

**DataStore Preferences** (Jetpack) — lightweight, async, coroutine-based.
No Room/SQLite needed for this simple data.

## Data Stored

### Radio Favorites
- Key: `"radio_favorites"`
- Type: `Set<String>` (station IDs)
- Example: `{"france_inter", "fip", "nrj"}`

### Podcast History
- Key: `"podcast_history"`
- Type: `String` (JSON-serialized list of PodcastChannel)
- Max entries: 50 (oldest removed when exceeding)
- Example: `[{"id":"123","name":"...","lastPlayedAt":1708300000}]`

## Interface: AppPreferences

```kotlin
interface AppPreferences {
    // Radio favorites
    val favoriteStationIds: Flow<Set<String>>
    suspend fun toggleFavorite(stationId: String)
    suspend fun isFavorite(stationId: String): Boolean

    // Podcast history
    val podcastHistory: Flow<List<PodcastChannel>>
    suspend fun addToHistory(channel: PodcastChannel)
    suspend fun removeFromHistory(channelId: String)
    suspend fun clearHistory()
}
```

## Implementation: DataStoreAppPreferences

Uses `DataStore<Preferences>` with:
- `stringSetPreferencesKey("radio_favorites")` for favorites
- `stringPreferencesKey("podcast_history")` for serialized history

## TDD Tests

### Test 1: Initially no favorites
- Given: fresh DataStore
- Then: favoriteStationIds emits empty set

### Test 2: Toggle favorite adds station
- When: toggleFavorite("fip")
- Then: favoriteStationIds contains "fip"

### Test 3: Toggle favorite twice removes station
- When: toggleFavorite("fip") twice
- Then: favoriteStationIds is empty

### Test 4: Initially no podcast history
- Given: fresh DataStore
- Then: podcastHistory emits empty list

### Test 5: addToHistory adds channel
- When: addToHistory(channel)
- Then: podcastHistory contains channel

### Test 6: addToHistory updates lastPlayedAt for existing channel
- Given: channel already in history
- When: addToHistory(same channel)
- Then: only one entry, with updated timestamp

### Test 7: removeFromHistory removes channel
- When: removeFromHistory(channelId)
- Then: podcastHistory no longer contains it

### Test 8: History limited to 50 entries
- Given: 50 channels in history
- When: addToHistory(51st channel)
- Then: oldest channel is removed

### Test 9: Favorites persist across DataStore recreation
### Test 10: History sorted by lastPlayedAt descending

## Files

```
app/src/main/java/com/steph/frenchradio/
  data/
    AppPreferences.kt          (interface)
    DataStoreAppPreferences.kt (implementation)
app/src/test/java/com/steph/frenchradio/
  data/
    DataStoreAppPreferencesTest.kt
```
