# 02 - Radio List

## Purpose

Display radio stations in a grid, with filtering, sorting, and favorites.

## UI Layout

```
+----------------------------------+
| [🔍 Search by name...         ]  |
| [Genre chips: All|Jazz|Rock|..]  |
+----------------------------------+
| ★ FAVORITES                      |
| [Logo] Name  [Logo] Name        |
| [Logo] Name  [Logo] Name        |
+----------------------------------+
| ALL STATIONS                     |
| [Logo] Name  [Logo] Name        |
| [Logo] Name  [Logo] Name        |
| ...                              |
+----------------------------------+
```

### Grid Items

Each item is a Card containing:
- Station logo (72x72dp, rounded corners)
- Station name below
- Small star icon (filled if favorite)
- Long-press or star tap to toggle favorite
- Single tap → start playback

### Filtering

- **Search**: TextField at top, filters by name (case-insensitive, accent-insensitive)
- **Genre chips**: Horizontal scrollable row of FilterChip composables
  - "All" chip selected by default
  - Tapping a genre chip filters the list
  - Multiple genres can be selected (OR logic)

### Sorting

- Stations sorted alphabetically by name
- Favorites always appear first (in their own section)

### Favorites

- Persisted via DataStore (module 05)
- Stored as Set<String> of station IDs
- Favorites section only visible if at least one favorite exists

## ViewModel: RadioListViewModel

```kotlin
data class RadioListUiState(
    val stations: List<RadioStation> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedGenres: Set<String> = emptySet(),
    val allGenres: List<String> = emptyList(),
    val isLoading: Boolean = true,
)
```

Methods:
- `loadStations()` — parse JSON, extract genres
- `onSearchQueryChanged(query: String)`
- `onGenreToggled(genre: String)`
- `onToggleFavorite(stationId: String)`
- `filteredStations()` — returns sorted/filtered list
- `favoriteStations()` — returns favorite subset

## TDD Tests

### Test 1: Stations loaded and sorted alphabetically
### Test 2: Search filters by name (case-insensitive)
### Test 3: Genre filter shows only matching stations
### Test 4: Multiple genre filters use OR logic
### Test 5: Toggle favorite adds station to favorites
### Test 6: Toggle favorite removes station from favorites
### Test 7: Favorites appear before non-favorites
### Test 8: Search + genre filter combine correctly
### Test 9: Empty search shows all stations
### Test 10: No results state when nothing matches

## Files

```
app/src/main/java/com/steph/frenchradio/
  radio/
    RadioListViewModel.kt
    RadioListScreen.kt
    StationLoader.kt
app/src/test/java/com/steph/frenchradio/
  radio/
    RadioListViewModelTest.kt
    StationLoaderTest.kt
```
