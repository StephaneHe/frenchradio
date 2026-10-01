# CLAUDE.md — FrenchRadio (Android)

Project-specific guidance for Claude Code agents working in this repo. Read this before making changes.

FrenchRadio is an Android app (Kotlin + Jetpack Compose + Media3 ExoPlayer) for streaming French radio stations and browsing/listening to podcasts. Multi-source podcast search aggregates iTunes, Fyyd.de, and PodcastIndex.

---

## Règles standing du fleet

These two rules apply to **every** release of this project. They are non-negotiable — failing either blocks the release.

### Règle 1 — Versioning visible et incrémenté

- **Canonical source** for this Android project: `versionCode` (Int) + `versionName` (semver) in `app/build.gradle.kts`.
- **Visible on the running release:** the version is rendered in the TopAppBar (`AppNavigation.kt`) via `BuildConfig.VERSION_NAME`. `buildFeatures.buildConfig = true` must stay enabled.
- **Bump at every shipped build, no exception:**
  - **patch** (`1.0.0` → `1.0.1`) for fix or small change (incl. doc, build tweak, this fleet-rule scaffolding)
  - **minor** (`1.0.0` → `1.1.0`) for a new feature
  - **major** (`1.0.0` → `2.0.0`) for a breaking change
  - **`versionCode`** must increase by **+1 strict** at every shipped build, independent of the semver bump.

### Règle 2 — Changelog

- The repo keeps a `CHANGELOG.md` at the root in [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.
- Sections allowed per release: **Added, Changed, Fixed, Removed, Deprecated, Security**.
- Entry header: `## [X.Y.Z] - YYYY-MM-DD`.
- **Coupled with Règle 1:** no release ships without **both** a version bump **and** a corresponding changelog entry. Adding a feature without updating both is incomplete work.

---

## Project conventions

- **Min SDK 31, Target SDK 35**, Java 17, Kotlin 2.x.
- **Podcast search architecture:** `PodcastSearchApi` interface, three concrete impls (`ItunesSearchApi`, `FyydSearchApi`, `PodcastIndexSearchApi`), and `AggregatedSearchApi` that fans out in parallel and dedupes by `feedUrl`. Each impl tags results with `source = "iTunes" | "Fyyd" | "PodcastIndex"` so the UI can render a source badge.
- **PodcastIndex auth** uses HMAC-SHA1 (`X-Auth-Key` / `X-Auth-Date` / `Authorization`); empty API key short-circuits to `emptyList()` without making a network call.
- **No new Gradle dependencies** unless strictly necessary — prefer the stdlib + already-included libs (Compose, kotlinx.serialization, Media3, Ktor, Coil, DataStore).
- **HTTP style:** `withContext(Dispatchers.IO) { URL(...).readText() }` for simple GETs; `HttpURLConnection` only when custom headers are required (PodcastIndex).
- **RSS feed URL is mandatory** for any `PodcastChannel` — items without it must be filtered out (the app loads episodes from the feed).
- **Tests:** `app/src/test/`, JUnit 4 + MockK + Robolectric. Existing fakes (`FakePodcastSearchApi`, `FakeFeedParser`, `FakeUrlFetcher`, `FakePodcastPrefs`) live at the bottom of `PodcastViewModelTest.kt` and are reusable across test files in the same package.

## Build & ship

- Signing config is read from `local.properties` (`signing.*` keys, see `local.properties.example`) or `SIGNING_*` env vars — never hardcode credentials or paths — the release APK is signed but with debug credentials, suitable for sideload but not Play Store.
- Release build: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`. R8 minification + resource shrinking are enabled.
- Before shipping a release: bump `versionCode`/`versionName`, add a `CHANGELOG.md` entry, and verify the new version shows in the TopAppBar of the built APK.
