# 📻 FrenchRadio

An Android app to listen to French radio stations and browse podcasts, built with Kotlin, Jetpack Compose, and Media3 ExoPlayer.

## Features

- **44 French radio stations** with verified streaming URLs (Radio France, NRJ Group, RTL Group, independents...)
- **Podcast search** via iTunes Search API with RSS feed parsing
- **Favorites** system with persistent storage (DataStore)
- **External JSON configuration** — edit the station list without rebuilding the app
- **Genre filtering and sorting** with accent-aware alphabetical ordering
- **Mini-player** with background playback support
- **Adaptive icon** with French tricolor radio antenna design

## Quick Install (APK)

If you just want to use the app on your Android phone:

1. Download `release/FrenchRadio-v1.0-debug.apk`
2. Transfer it to your phone (USB, email, cloud...)
3. Open the APK on your phone and allow installation from unknown sources
4. Or via ADB:
   ```bash
   adb install release/FrenchRadio-v1.0-debug.apk
   ```

**Requirements:** Android 12+ (API 31)

## Build from Source

### Prerequisites

- **Java 17** (JDK)
- **Android SDK** with:
  - Compile SDK 35 (Android 15)
  - Build Tools 35
- **Gradle 8.11.1** (wrapper included)

### Build

```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Run Tests

```bash
# Windows
.\gradlew.bat testDebugUnitTest

# Linux / macOS
./gradlew testDebugUnitTest
```

65 unit tests covering: station loading, podcast search, RSS parsing, audio player, persistence, ViewModel logic.

### Install on Device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.steph.frenchradio/.MainActivity
```

## Editing the Station List

After first launch, the station list is copied to:
```
/sdcard/Android/data/com.steph.frenchradio/files/stations.json
```

You can edit this file to add, remove, or modify stations without rebuilding the app:

```bash
# Pull from phone
adb pull /sdcard/Android/data/com.steph.frenchradio/files/stations.json

# Edit the file, then push back
adb push stations.json /sdcard/Android/data/com.steph.frenchradio/files/stations.json
```

Restart the app to load the changes.

### Station JSON Format

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

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.12.01) |
| Audio | Media3 ExoPlayer 1.5.1 |
| Persistence | DataStore Preferences 1.1.1 |
| Networking | Ktor 3.0.2, kotlinx.serialization 1.7.3 |
| Images | Coil 2.7.0 |
| Testing | JUnit 4, MockK 1.13.13, Robolectric 4.14.1 |
| Min SDK | 31 (Android 12) |
| Target SDK | 35 (Android 15) |

## Project Structure

```
app/src/main/java/com/steph/frenchradio/
├── model/          # Data models (RadioStation, PodcastChannel, PodcastEpisode)
│   └── StationLoader.kt   # JSON loader with external file support
├── player/         # Audio engine abstraction (ExoPlayer wrapper)
├── podcast/        # iTunes search API, RSS feed parser, ViewModel
├── data/           # DataStore preferences (favorites, history)
├── ui/             # Compose screens (radio list, podcast, player)
└── MainActivity.kt

app/src/main/res/raw/
└── stations.json   # Bundled default station list (44 stations)

app/src/test/       # 65 unit tests
```

## Radio Stations (44)

Radio France: France Inter, France Culture, France Info, France Musique, France Bleu Paris, FIP (+ Rock, Jazz, Groove, Monde, Electro), Mouv'

Généralistes: RTL, Europe 1, RMC, BFM Business, Sud Radio

NRJ Group: NRJ, Nostalgie, Chérie FM, Rire & Chansons

Musique: RTL2, Fun Radio, Skyrock, RFM, Radio Nova, Virgin Radio, Voltage, Radio FG, M Radio, Alouette

Jazz/Classique: Radio Classique, OÜI FM, TSF Jazz, Jazz Radio, Crooner Radio

World/Thématiques: RFI Monde, Latina, Générations, Beur FM, Tropiques FM, Radio Orient, Radio Meuh, Africa Radio

## License

Personal project — all rights reserved.
