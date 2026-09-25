# FrenchRadio v1.1 - Progress Tracker

## Feature A: Podcast Resume & In-Progress List

### A1. Data Model - EpisodeProgress
- [ ] Create `EpisodeProgress` data class (episodeAudioUrl, episodeTitle, channelId, channelName, channelArtwork, positionMs, durationMs, updatedAt)
- [ ] Make it `@Serializable`

### A2. Persistence - AppPreferences
- [ ] Add `episodeProgressList: Flow<List<EpisodeProgress>>` to `AppPreferences`
- [ ] Add `saveEpisodeProgress(progress: EpisodeProgress)` 
- [ ] Add `removeEpisodeProgress(audioUrl: String)`
- [ ] Add `getEpisodeProgress(audioUrl: String): EpisodeProgress?`
- [ ] Implement in `DataStoreAppPreferences`
- [ ] Max 100 entries, oldest auto-removed

### A3. Tests - Persistence
- [ ] Test: initially no episode progress
- [ ] Test: saveEpisodeProgress stores entry
- [ ] Test: saveEpisodeProgress updates existing (same audioUrl)
- [ ] Test: removeEpisodeProgress removes entry
- [ ] Test: getEpisodeProgress returns correct entry
- [ ] Test: progress limited to 100 entries
- [ ] Test: progress sorted by updatedAt descending

### A4. PlayerController - Auto-save position
- [ ] Add `prefs` dependency to `SimplePlayerController`
- [ ] Save position on pause()
- [ ] Save position on stop()
- [ ] Save position periodically (every 15s while playing podcast)
- [ ] Remove progress when podcast finishes (onEnded)
- [ ] On playPodcast: seek to saved position if exists

### A5. Tests - PlayerController position save
- [ ] Test: pause saves position for podcast
- [ ] Test: pause does NOT save position for radio
- [ ] Test: playPodcast resumes at saved position
- [ ] Test: onEnded removes progress entry

### A6. PodcastViewModel - In-Progress list
- [ ] Expose `inProgressEpisodes: Flow<List<EpisodeProgress>>` from prefs
- [ ] Add `onResumeEpisode(progress: EpisodeProgress)` 
- [ ] Wire up in PodcastUiState

### A7. UI - In-Progress Section on Podcast tab
- [ ] Show "In Progress" section above "Recent Podcasts" when not searching
- [ ] Each entry shows: channel artwork, episode title, channel name, progress bar, resume button
- [ ] Swipe-to-dismiss or X button to remove
- [ ] Tap/Resume button resumes playback at saved position

### A8. Integration & compile test
- [ ] Build compiles
- [ ] All tests pass
- [ ] Commit

## Feature B: In-App Station Editor

### B1. StationLoader - Write support
- [ ] Add `saveStations(context, stations)` to StationLoader
- [ ] Add `addStation(context, station)` 
- [ ] Add `updateStation(context, stationId, station)`
- [ ] Add `deleteStation(context, stationId)`

### B2. Tests - StationLoader write
- [ ] Test: saveStations writes valid JSON
- [ ] Test: addStation appends and persists
- [ ] Test: updateStation modifies correct entry
- [ ] Test: deleteStation removes correct entry

### B3. RadioListViewModel - Edit operations
- [ ] Add `addStation(station)`, `updateStation(id, station)`, `deleteStation(id)`
- [ ] Reload station list after mutation
- [ ] Hold reference to Context (or StationLoader) for file writes

### B4. Tests - RadioListViewModel edit
- [ ] Test: addStation adds to list
- [ ] Test: updateStation modifies entry
- [ ] Test: deleteStation removes entry

### B5. UI - Station Editor
- [ ] Add edit/delete button on long-press or swipe on StationCard
- [ ] "Add station" FAB or button on RadioListScreen
- [ ] StationEditDialog composable (name, stream_url, genres, color fields)
- [ ] Validation (name required, URL required, valid URL format)
- [ ] Delete confirmation dialog

### B6. Integration & compile test
- [ ] Build compiles
- [ ] All tests pass
- [ ] Commit

## Feature C: Listening History & Played Markers (v1.5.0, 2026-09-25)

- [x] C1. `ListenHistoryEntry` + `ListenStatus` (played = end reached or ≥ 90 %, sticky)
- [x] C2. `AppPreferences` history API + DataStore impl (upsert by audio URL, max 500, seeded from progress)
- [x] C3. `SimplePlayerController` records start / progress / completion
- [x] C4. `PodcastViewModel` exposes `listenHistory`, remove / clear / replay
- [x] C5. "Historique" tab (`HistoryScreen`): date, status, position, replay, delete, clear all
- [x] C6. Markers "Lu" / "En cours" on episode rows, channel pills on search / browse / recent
- [x] C7. Tests (DataStoreListenHistoryTest, player + ViewModel) — full suite green
- [x] C8. Version 1.5.0 / vc7, CHANGELOG, APK built, local commit
- [x] C9. (v1.5.1) "Played" / restart threshold 90 % → 98 % (`PLAYED_THRESHOLD`), tests updated

## Final
- [ ] Full test suite green
- [ ] Build APK
- [ ] Push to GitHub
- [ ] Install on device
