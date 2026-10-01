# 📻 FrenchRadio

Android app to stream French radio stations and search, browse and listen to podcasts — Kotlin, Jetpack Compose and Media3 ExoPlayer.

![Version](https://img.shields.io/badge/version-1.5.3-blue)
![Platform](https://img.shields.io/badge/platform-Android%2012%2B-green)
![License](https://img.shields.io/badge/license-MIT-yellow)

## Overview

FrenchRadio brings live French radio and podcasts together in one lightweight app: no account, no ads, no tracking. The station list ships with the app but can be edited on the device, and podcast search aggregates several public directories so that French-language shows are easier to find.

**Status:** active personal project — current version **1.5.3** (see [CHANGELOG.md](CHANGELOG.md)).

## Features

**Radio**
- 44 bundled French stations (Radio France, NRJ group, RTL group, independents…)
- Search, genre filtering, accent-aware alphabetical sorting, favorites
- In-app station editor (add / edit / delete); list stored as an editable JSON file on the device

**Podcasts**
- Multi-source search aggregating **iTunes**, **Fyyd** and **PodcastIndex** in parallel, deduplicated by RSS feed URL, with a source badge per result
- Browse by category, recently opened podcasts, episode list parsed from the RSS feed
- Resume where you left off ("In Progress" list, position saved on pause/stop and every 15 s)
- **Historique** tab: every episode played, with "Lu" / "En cours" status, progress and replay
- "Lu" / "En cours" markers on episodes and channels (an episode is "played" at ≥ 98 % or when it reaches the end)
- Share a podcast or an episode via the Android share sheet

**Player**
- Background playback through a Media3 foreground service, mini-player bar, seek drawer for podcasts
- Audio boost (0 to +20 dB via `LoudnessEnhancer`), persisted, with a visible `+X dB` badge
- App version shown in the top bar

## Requirements

| To… | You need |
|-----|----------|
| Run the app | Android 12+ (API 31), Internet access |
| Build it | JDK 17, Android SDK (compile SDK 35 / build tools 35); Gradle 8.11.1 is provided by the wrapper |

## Installation

No prebuilt APK is versioned in this repository (`*.apk` is git-ignored): build it from source, then install it.

```bash
git clone <repository-url>
cd frenchradio
cp local.properties.example local.properties   # then fill in your values
./gradlew assembleDebug                         # Windows: .\gradlew.bat assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

`local.properties` (git-ignored) — template: [`local.properties.example`](local.properties.example)

| Key | Role |
|-----|------|
| `sdk.dir` | Path to the Android SDK |
| `signing.storeFile` | Keystore used to sign the release build |
| `signing.storePassword` | Keystore password |
| `signing.keyAlias` | Key alias |
| `signing.keyPassword` | Key password |

The signing keys can also be provided as environment variables: `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`. Only the release build needs them.

**PodcastIndex:** the API requires a key/secret pair. None is configured in the current code (`PodcastIndexSearchApi()` is created with empty credentials), so this source silently returns no results and search relies on iTunes + Fyyd.

## Usage

The app has three tabs: **Radio**, **Podcasts**, **Historique**. Tap a station or an episode to play it; the mini-player stays at the bottom across tabs.

### Editing the station list outside the app

On first launch the bundled list is copied to the app's external files directory:

```bash
adb pull /sdcard/Android/data/com.steph.frenchradio/files/stations.json
# edit, then:
adb push stations.json /sdcard/Android/data/com.steph.frenchradio/files/stations.json
```

Restart the app to reload. Station format:

```json
{
  "id": "my_station",
  "name": "My Station",
  "stream_url": "https://example.com/stream.mp3",
  "logo": "my_station",
  "genres": ["Pop", "Rock"],
  "color": "#FF0000"
}
```

## Architecture

```
 Compose UI (radio / podcast / history screens, mini-player)
        │
   ViewModels (RadioListViewModel, PodcastViewModel)
        │                        │
 StationLoader (JSON)     PodcastSearchApi ── AggregatedSearchApi
 AppPreferences (DataStore)       ├─ ItunesSearchApi
        │                        ├─ FyydSearchApi
 PlayerController                 └─ PodcastIndexSearchApi (HMAC-SHA1 auth)
        │                  RssFeedParser (episodes)
 ExoPlayerAudioEngine ── PlaybackService (Media3) ── AudioBoostController
```

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.x, Java 17 |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3, Navigation Compose |
| Audio | Media3 ExoPlayer + Session 1.5.1 |
| Persistence | DataStore Preferences 1.1.1 |
| Networking / JSON | `HttpURLConnection` / `URL.readText()`, Ktor 3.0.2, kotlinx.serialization 1.7.3 |
| Images | Coil 2.7.0 |
| Tests | JUnit 4, MockK, Robolectric, kotlinx-coroutines-test |

```
app/src/main/java/com/steph/frenchradio/
├── MainActivity.kt
├── data/         # AppPreferences + DataStore implementation (favorites, progress, history, boost)
├── model/        # RadioStation, podcast models, StationLoader
├── navigation/   # AppNavigation (tabs, top bar with version)
├── player/       # AudioEngine, ExoPlayer wrapper, PlaybackService, PlayerController, AudioBoostController
├── podcast/      # search APIs, RSS parser, PodcastViewModel, podcast/episode/history screens
├── radio/        # RadioListScreen, RadioListViewModel
└── ui/           # mini-player, seek drawer, audio boost sheet, markers, share, theme
app/src/main/res/raw/stations.json   # bundled default station list
app/src/test/                        # unit tests
specs/                               # functional specifications + reference station list
```

## Tests

```bash
./gradlew testDebugUnitTest      # Windows: .\gradlew.bat testDebugUnitTest
```

JVM unit tests (Robolectric where Android APIs are needed) covering station loading/editing, radio list filtering and sorting, iTunes / Fyyd parsing, search aggregation and deduplication, RSS parsing, the player controller (resume, progress saving, history recording), DataStore persistence (favorites, progress, listening history) and the podcast ViewModel. There is no CI pipeline; run the suite before each release.

## Build & release

```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

The release build uses R8 minification and resource shrinking and is signed with the keystore set in `local.properties` / environment variables. The resulting APK is meant for sideloading, not for the Play Store.

## Versioning & changelog

[Semantic Versioning](https://semver.org/). Every shipped build bumps `versionName` and increases `versionCode` by exactly 1 (`app/build.gradle.kts`), and adds an entry to [CHANGELOG.md](CHANGELOG.md) ([Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format). The running version is displayed in the app's top bar.

## Roadmap

- Make PodcastIndex credentials configurable to enable the third search source

Detailed task tracking: [PROGRESS.md](PROGRESS.md).

## Security

- Signing keys, keystores and `local.properties` are git-ignored and must never be committed.
- The app only performs outgoing HTTP(S) requests (radio streams, podcast directories, RSS feeds); it exposes no network service and collects no personal data.
- To report a vulnerability, use GitHub's private vulnerability reporting (Security tab) or contact the maintainer privately rather than opening a public issue.

## Contributing

Issues and pull requests are welcome. Read [CLAUDE.md](CLAUDE.md) for project conventions. Work on a branch, keep the test suite green, and include a version bump plus a CHANGELOG entry with every change that ships.

## License

[MIT](LICENSE) © 2026 Stéphane Hercot.

Radio streams, logos and podcast content belong to their respective broadcasters and publishers; this project only links to publicly available streams and feeds.

## Author

[StephaneHe](https://github.com/StephaneHe)
