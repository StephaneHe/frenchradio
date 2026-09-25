# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Per fleet rules: every shipped build bumps the version (`versionCode` +1, `versionName` semver) and adds an entry here.

## [Unreleased]

## [1.5.0] - 2026-09-25

### Added
- **Listening history ("Historique" tab).** New third tab in the bottom navigation listing every podcast episode played, newest first: artwork, episode title, podcast name, last listen date/time, and status — "Lu" or "En cours" with mini progress bar and position (`12 min / 45 min`). Tap replays the episode (resumes at the saved position if partial, restarts from 0 if already played). Per-entry delete (✕) and "Effacer l'historique" with confirmation dialog.
- **Played / in-progress markers** wherever podcasts are listed or searched:
  - Episode list: "✓ Lu" pill (title slightly de-emphasized) or "En cours" pill + mini progress bar.
  - Search results, browse results, recent podcasts: channel pill "N lu(s)" and/or "En cours".
  - Markers use Material 3 `secondaryContainer` / `tertiaryContainer` roles (readable in light and dark themes) and update immediately (DataStore flow).
- `ListenHistoryEntry` model + `ListenStatus` (`STARTED` / `PLAYED`). **Rule:** an episode is "played" once it reached its end or ≥ 90 % of its duration; "played" is sticky. Otherwise it is "started".
- `AppPreferences.listenHistory` / `recordListen` / `removeListenHistory` / `clearListenHistory`, persisted in DataStore (`listen_history` key, max 500 entries). On first use, seeded from the existing episode-progress entries.

### Changed
- `SimplePlayerController` records history on episode start, on each progress save (pause/stop/every 15 s) and as completed on end. Resuming an episode whose saved position is past 90 % now restarts it from the beginning.
- `versionCode` 6 → 7, `versionName` 1.4.0 → 1.5.0.

## [1.4.0] - 2026-05-03

### Added
- **Share button for podcasts.** New Share icon in the `EpisodeListScreen` header lets users share the podcast channel (name, author, feed URL) via the standard Android share sheet.
- **Share button per episode.** Share icon inline in each episode row (left of Play) shares the episode title, channel name, publish date, and audio URL via the Android share sheet.
- `ui/ShareUtils.kt`: `sharePodcast(context, channel)` and `shareEpisode(context, episode, channel)` pure helpers building and launching `Intent.ACTION_SEND` with `text/plain` payload.

## [1.3.0] - 2026-04-30

### Added
- **Audio boost controllable from the app screen.** New `AudioBoostController` wraps `android.media.audiofx.LoudnessEnhancer` and attaches it to the ExoPlayer audio session. The boost works for both radio streams and podcasts, persists across restarts via DataStore, and is automatically re-attached when the audio session ID changes.
- New "Boost" icon in `MiniPlayerBar` (before Stop), opens a `ModalBottomSheet` with a 0–100 % slider and a "Réinitialiser" button. Slider maps linearly to 0–2000 mB of gain (0 to +20 dB).
- Persistent badge `+X dB` next to the Boost icon when boost > 0, so the user always sees that the effect is active.
- `AppPreferences.audioBoostPercent` (Flow) + `setAudioBoostPercent()` for persistence.
- `MODIFY_AUDIO_SETTINGS` permission in the manifest (required by `LoudnessEnhancer`).

## [1.2.1] - 2026-04-30

### Added
- Visible version label `v<versionName>` rendered in the TopAppBar of `AppNavigation`, reading from `BuildConfig.VERSION_NAME`. Compliance with fleet rule on visible versioning.
- `CLAUDE.md` at the repo root documenting fleet standing rules (versioning, changelog) and project conventions.
- `CHANGELOG.md` (this file) at the repo root.

### Changed
- `versionName` normalized to semver: `"1.2"` → `"1.2.0"` (intermediate) → `"1.2.1"` (this release).
- `versionCode`: `3` → `4` (strict +1 per fleet rule).
- `app/build.gradle.kts`: enabled `buildFeatures.buildConfig = true` so `BuildConfig.VERSION_NAME` is generated and consumable from Compose code.

[Unreleased]: #
[1.5.0]: #150---2026-09-25
[1.4.0]: #140---2026-05-03
[1.3.0]: #130---2026-04-30
[1.2.1]: #121---2026-04-30
