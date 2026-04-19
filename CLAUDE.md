# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development

### Common Commands

**Build debug APK:**
```bash
./gradlew assembleDebug
```

**Install and run on connected device/emulator:**
```bash
./gradlew installDebug
```

**Build and install in one step:**
```bash
./gradlew installDebug && adb shell am start -n com.videoeditor/.MainActivity
```

**Check and format (Kotlin only):**
```bash
./gradlew spotlessCheck  # Check formatting
./gradlew spotlessApply  # Auto-fix formatting
```

**Build release APK (for deployment):**
```bash
./gradlew assembleRelease
```

### Project Configuration

- **Kotlin:** 2.0.0
- **Android AGP:** 8.7.3
- **Compose BOM:** 2025.01.00
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 35
- **Java/JVM Target:** 17

### No Automated Tests

This project has no automated test suite. Verify changes via:
1. Build succeeds: `./gradlew assembleDebug`
2. Manual testing on emulator/device: `./gradlew installDebug`
3. Visual inspection of feature behavior in the app

## Architecture Overview

### High-Level Structure

**Single-Activity App:** `MainActivity` hosts a Compose-based navigation graph.

**Modular Feature Design:**
- **Core modules** (`core/`) provide shared infrastructure: DI, FFmpeg integration, navigation, storage, theming, and probing
- **Feature modules** (`feature/`) are self-contained feature screens (compress is MVP, home is the hub)
- New features (trim, convert, extract audio) can be added as sibling modules under `feature/`

**Key Tech Stack:**
- **UI:** Jetpack Compose + Material 3
- **Navigation:** Navigation Compose (data-driven via `FeatureRegistry`)
- **State:** MVVM with Kotlin StateFlow
- **DI:** Hilt (setup in `core/di/CoreModule`)
- **Video Processing:** FFmpegKit v6.1.1 (hardware encoders via MediaCodec when possible)
- **Background Tasks:** WorkManager + ForegroundService
- **Settings:** DataStore (preference-based, not file-based)
- **Media Output:** Android MediaStore (persistent file access)

### Module Structure

```
com/videoeditor/
├── App.kt                      # Hilt app singleton
├── MainActivity.kt             # Single activity, Compose entry point
├── core/
│   ├── di/                     # Hilt setup (CoreModule, WorkerFactory)
│   ├── ffmpeg/                 # FFmpegKit wrappers
│   │   ├── FFmpegRunner        # Execute commands, parse progress
│   │   ├── FFmpegCommandBuilder # Build safe command strings
│   │   └── ProgressParser      # Extract progress from logs
│   ├── navigation/             # Route definitions + registry
│   │   ├── Routes              # Sealed class for type-safe routes
│   │   ├── FeatureRegistry     # Data-driven feature card list
│   │   └── AppNavHost          # Compose NavHost with feature integration
│   ├── probe/                  # ffprobe wrapper (get video metadata)
│   ├── storage/                # File I/O and MediaStore
│   ├── estimator/              # Estimate output file size
│   ├── designsys/              # Reusable UI components (SectionCard, etc.)
│   └── theme/                  # Material 3 color, type, motion
├── feature/
│   ├── home/
│   │   └── HomeScreen          # Feature hub with registered cards
│   └── compress/               # MVP: video compression
│       ├── ui/                 # Composables (screens, parameter controls)
│       ├── model/              # Data classes (CompressionParams, presets)
│       ├── work/               # WorkManager job + ForegroundService
│       └── di/                 # Feature-scoped dependencies
```

### Adding a New Feature

1. **Create feature module structure** under `feature/newfeature/` with `ui/`, `model/`, `work/` (if async), `di/` subdirs
2. **Define a route** in `core/navigation/Routes.kt` as a sealed class case
3. **Add to FeatureRegistry** in `core/navigation/FeatureRegistry.kt` (home screen will auto-display the card)
4. **Implement composable** in `feature/newfeature/ui/` and wire it in `AppNavHost.kt`
5. **Use Hilt** for feature-scoped DI in `feature/newfeature/di/` (use `@hilt:android:entry_point`)

### Key Design Decisions

**Why this structure?**
- Compress is isolated under `feature/compress/` so future features don't pollute it
- Home screen is a feature hub with a data-driven list of `FeatureCard` entries — adding a feature = adding an entry to the registry
- Hilt bindings are centralized in `core/di/` and feature-specific ones in `feature/*/di/`
- FFmpeg commands are built via `FFmpegCommandBuilder` for safety (prevents shell injection)

**FFmpeg specifics:**
- Prefers hardware encoders (`h264_mediacodec` / `hevc_mediacodec`) when codec/profile/level allow, falls back to libx264/libx265
- FFmpegKit is a Maven dependency from a custom fork (see `settings.gradle.kts` maven URL) because official artifacts were deprecated in early 2025
- Progress is parsed from FFmpeg's frame output; time-based progress is calculated client-side

**Background processing:**
- Compression jobs run in WorkManager workers (enqueued from UI)
- ForegroundService ensures the job isn't killed when app backgrounded
- Results are written to MediaStore so files persist after app closes

## Common Patterns

**Theming:** Use `MaterialTheme` from `core/theme/Theme.kt`; leverage M3 dynamic colors when available (API 31+).

**Navigation:** All routes go through `AppNavHost`. Pass arguments via sealed class constructors in `Routes.kt`, not loose strings.

**DI:** Use Hilt annotations (`@Singleton`, `@Provides`, `@EntryPoint`). Feature-scoped objects go in `feature/*/di/`.

**State Management:** Use `StateFlow` wrapped in `@HiltViewModel` for screen-level state; avoid mutable state in Composables.

**File I/O:** Use `MediaStoreSaver` for persisting videos, `ScopedTempDir` for temp files during processing.

## Gradle and Dependencies

All dependency versions and plugins are centralized in `gradle/libs.versions.toml`. Update versions there, not in `build.gradle.kts`.

Key dependencies:
- `ffmpeg-kit-full` (v6.1.1) – full FFmpeg binary for compression
- `work-runtime-ktx` (v2.10.0) – background tasks
- `coil-compose` + `coil-video` – image/video thumbnails
- `accompanist-permissions` – runtime permissions UI

## Resources and Files

- **Localized strings:** `app/src/main/res/values/strings.xml`
- **App theme colors/styles:** `app/src/main/res/values/themes.xml` and `values-night/themes.xml`
- **App manifest:** `app/src/main/AndroidManifest.xml` (permissions, activities, services)
