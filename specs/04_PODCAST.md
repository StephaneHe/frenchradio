# 04 - Podcast

## Purpose

Search podcasts via iTunes API, browse episodes via RSS,
play episodes, and remember played channels.

## User Flow

1. User types in search bar → results from iTunes API
2. User taps a podcast → episode list loaded from RSS feed
3. User taps an episode → playback starts
4. Played podcast channel is saved to history (module 05)
5. History is shown below search results (or when search is empty)

## UI Layout

### Search State
```
+----------------------------------+
| [🔍 Search podcasts...        ]  |
+----------------------------------+
| RESULTS                          |
| [Art] Podcast Name               |
|       Author                     |
| [Art] Podcast Name               |
|       Author                     |
+----------------------------------+
```

### Empty State (no search)
```
+----------------------------------+
| [🔍 Search podcasts...        ]  |
+----------------------------------+
| RECENT PODCASTS                  |
| [Art] Name        [✕ Remove]     |
|       Author                     |
| [Art] Name        [✕ Remove]     |
|       Author                     |
+----------------------------------+
```

### Episode List (after tapping a podcast)
```
+----------------------------------+
| ← Podcast Name                   |
|    Author                        |
+----------------------------------+
| Episode Title 1                  |
| 2025-02-15 · 45 min      [▶]   |
|                                  |
| Episode Title 2                  |
| 2025-02-10 · 32 min      [▶]   |
+----------------------------------+
```

## iTunes Search API

### Endpoint
```
GET https://itunes.apple.com/search
  ?term={query}
  &media=podcast
  &country=fr
  &limit=20
```

### Response (relevant fields)
```json
{
  "results": [
    {
      "collectionId": 1234567,
      "collectionName": "Podcast Name",
      "artistName": "Author",
      "artworkUrl100": "https://..../100x100.jpg",
      "artworkUrl600": "https://..../600x600.jpg",
      "feedUrl": "https://example.com/feed.xml"
    }
  ]
}
```

## RSS Feed Parsing

Standard podcast RSS. Extract from each `<item>`:
- `<title>` → episode title
- `<description>` → episode description (strip HTML)
- `<enclosure url="..." />` → audio URL
- `<pubDate>` → publish date
- `<itunes:duration>` → duration

Use a simple XML parser (XmlPullParser built into Android).
No external dependency needed.

## ViewModel: PodcastViewModel

```kotlin
data class PodcastUiState(
    val searchQuery: String = "",
    val searchResults: List<PodcastChannel> = emptyList(),
    val history: List<PodcastChannel> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val selectedChannel: PodcastChannel? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
    val isLoadingEpisodes: Boolean = false,
    val episodeError: String? = null,
)
```

Methods:
- `onSearchQueryChanged(query: String)` — debounce 500ms
- `search()` — call iTunes API
- `onChannelSelected(channel: PodcastChannel)` — load RSS
- `onBackFromEpisodes()` — clear selection
- `onRemoveFromHistory(channelId: String)`
- `onPlayEpisode(episode: PodcastEpisode)` — save to history + play

## TDD Tests

### Test 1: Search returns parsed results from iTunes API
### Test 2: Empty search query shows history only
### Test 3: Channel selection triggers RSS loading
### Test 4: RSS parsing extracts episodes correctly
### Test 5: Playing episode adds channel to history
### Test 6: Remove from history works
### Test 7: History sorted by lastPlayedAt descending
### Test 8: Search error state is set on network failure
### Test 9: Episode error state on invalid RSS
### Test 10: Search debounce prevents rapid API calls

## Files

```
app/src/main/java/com/steph/frenchradio/
  podcast/
    PodcastViewModel.kt
    PodcastScreen.kt
    EpisodeListScreen.kt
    ItunesSearchApi.kt
    RssFeedParser.kt
app/src/test/java/com/steph/frenchradio/
  podcast/
    PodcastViewModelTest.kt
    ItunesSearchApiTest.kt
    RssFeedParserTest.kt
```
