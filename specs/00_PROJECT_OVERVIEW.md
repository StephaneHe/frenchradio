# FrenchRadio - Project Overview

## Context

An Android app to listen to French radio stations and podcasts.
Designed for a Doogee V30T running Android 12.

## Core Features

### Tab 1 - Radio
- Grid display of all stations with logo + name
- Filter by genre or search by name
- Sort alphabetically by name
- Favorite stations (persisted), shown first in the grid
- Stream playback with error handling (temporary/permanent failures)
- Background playback with media notification

### Tab 2 - Podcasts
- Search podcasts via iTunes Search API
- Display search results with artwork + name + author
- Play episodes from RSS feed
- Remember played podcast channels (persisted)
- Remove channels from history

## Target Device

- **Phone**: Doogee V30T
- **OS**: Android 12 (API level 31)
- **Min SDK**: 31
- **Target SDK**: 35

## Architecture - Modular Breakdown

| File | Module | Dependencies |
|------|--------|-------------|
| `01_BASE_COMMON.md` | Project skeleton, navigation, data models, theme | None |
| `02_RADIO_LIST.md` | Station grid, filtering, sorting, favorites | Base |
| `03_AUDIO_PLAYER.md` | ExoPlayer streaming, error handling, media notification | Base |
| `04_PODCAST.md` | iTunes search, RSS parsing, episode list, history | Base |
| `05_PERSISTENCE.md` | SharedPreferences/DataStore for favorites + podcast history | Base |

### Dependency Graph

```
01_BASE_COMMON
    ├── 05_PERSISTENCE (independent)
    ├── 02_RADIO_LIST  (needs 05 for favorites)
    ├── 03_AUDIO_PLAYER (independent)
    └── 04_PODCAST     (needs 05 for history)
```

Build order: 01 → 05 → 02 → 03 → 04

## Technology Stack

- **Language**: Kotlin
- **Build system**: Gradle (Kotlin DSL)
- **UI**: Jetpack Compose (Material 3) with bottom tab navigation
- **Audio**: AndroidX Media3 (ExoPlayer)
- **Network**: Ktor Client (lightweight HTTP for podcast search + RSS)
- **Persistence**: DataStore Preferences
- **Images**: Coil (for podcast artwork loading)
- **Testing**: JUnit 4 + Mockk + Robolectric
- **Architecture**: MVVM, single Activity, two tabs

## Station Data

31 French radio stations with metadata in `stations.json`.
Logos are generated vector drawables (brand color + initials),
stored as Android XML drawables at consistent 108x108dp.

## Genre List (for filtering)

Extracted from stations.json:
Alternatif, Blues, Business, Classique, Culture, Dance, Débat,
Divertissement, Éclectique, Electro, Funk, Généraliste, Groove,
Hip-Hop, Hits, Humour, Info, International, Jazz, Love, Pop,
Rap, RnB, Rock, Soul, Sport, Variété, World, 70s, 80s, 90s
