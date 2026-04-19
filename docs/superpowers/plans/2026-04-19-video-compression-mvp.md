# Video Editor — Compression MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Testing policy for this project:** No automated tests. Verify each task with `./gradlew assembleDebug`, `./gradlew lintDebug`, install on a device/emulator, and a manual click-through. Do **not** add JUnit, MockK, Turbine, or compose-ui-test dependencies.

**Goal:** Ship a Material 3 Android video-editor app whose home screen is a feature hub, with the first feature — full-control video compression powered by FFmpegKit — fully working end-to-end (pick → configure → background-encode → save to gallery).

**Architecture:** Single-Activity Compose app, MVVM with `StateFlow`-backed `ViewModel`s, Hilt DI, Navigation Compose, WorkManager + Foreground Service for the encode job, FFmpegKit for video processing, MediaStore for output. Code is organised by feature package (`feature/home`, `feature/compress`, `feature/<future>`) atop a thin `core/` (theme, navigation, storage, ffmpeg). The home screen is data-driven over a `FeatureRegistry`, so adding a future tool = registering a card + a route, no rewrites.

**Tech Stack:** Kotlin 2.1.x · AGP 8.7.x · Compose BOM 2025.01.00 · Material 3 · Hilt 2.52 · Navigation Compose 2.8.x · WorkManager 2.10.x · DataStore Preferences · FFmpegKit `com.arthenica:ffmpeg-kit-full:6.0-2` · Coil for thumbnails · Accompanist Permissions · `androidx.media3:media3-common` (only for utility constants) · minSdk 26, targetSdk 34.

---

## 0. Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│ MainActivity (single)                                             │
│   └── NavHost                                                     │
│        ├── home/                Feature hub (cards from registry) │
│        ├── compress/            Stepper: pick → configure → run   │
│        └── (future routes)                                        │
└──────────────────────────────────────────────────────────────────┘
                       │ uses
┌──────────────────────────────────────────────────────────────────┐
│ feature/compress                                                  │
│   ui/        CompressScreen + step composables                    │
│   vm/        CompressViewModel (StateFlow<CompressUiState>)       │
│   model/     CompressionSettings, SmartPreset, ProbeResult        │
│   work/      CompressWorker (WorkManager) + ForegroundService     │
└──────────────────────────────────────────────────────────────────┘
                       │ depends on
┌──────────────────────────────────────────────────────────────────┐
│ core/                                                             │
│   theme/     M3 colour scheme, typography, motion specs           │
│   navigation/ Routes, FeatureRegistry                             │
│   storage/   MediaStoreSaver, ScopedTempDir                       │
│   ffmpeg/    FFmpegCommandBuilder, FFmpegRunner, ProgressParser   │
│   probe/     VideoProbe (FFprobeKit + MediaMetadataRetriever)     │
│   estimator/ OutputSizeEstimator                                  │
│   designsys/ Reusable Compose components (SectionCard, StepHeader)│
└──────────────────────────────────────────────────────────────────┘
```

**Key cross-cutting decisions:**

- **Single-Activity:** All screens are Composables under one `NavHost`. Avoids fragment lifecycle pain.
- **MVVM + unidirectional flow:** Each screen has a `*ViewModel` exposing `StateFlow<UiState>`; UI emits intents via plain methods (`onCodecSelected(...)`).
- **Hilt** wires `Application`, `WorkerFactory`, repositories, and the `FFmpegRunner` singleton.
- **WorkManager** owns the encode lifecycle so the OS doesn't kill it on app backgrounding. Promoted to a foreground service via `setForegroundAsync` for an ongoing notification with progress + cancel action.
- **FFmpegKit** runs async (`FFmpegKit.executeAsync`); progress callback feeds `WorkInfo.progress` which the UI observes.
- **Hardware accel:** Try `*_mediacodec` encoders first when target params don't require features they lack; fall back to libx264/libx265.
- **Storage:** Output is written to a private cache file first, then copied into `MediaStore.Video` (Movies/VideoEditor) so the file appears in the system Photos app on Android 10+.

---

## 1. UI Wireframes (ASCII)

### Home (`feature/home`)

```
┌─────────────────────────────────────────┐
│  Video Editor                       ⚙   │   ← top bar (logo + settings icon)
├─────────────────────────────────────────┤
│                                         │
│  Tools                                  │   ← section header
│                                         │
│  ┌─────────────┐   ┌─────────────┐      │
│  │  🎬          │   │  ✂           │      │   ← FeatureCard, large square
│  │  Compress    │   │  Trim        │      │     (M3 OutlinedCard, elevated
│  │  Shrink size │   │  Coming soon │      │      on hover/press)
│  └─────────────┘   └─────────────┘      │
│                                         │
│  ┌─────────────┐   ┌─────────────┐      │
│  │  🔄          │   │  🎵          │      │
│  │  Convert     │   │  Audio       │      │
│  │  Coming soon │   │  Coming soon │      │
│  └─────────────┘   └─────────────┘      │
│                                         │
│  ┌─────────────┐                        │
│  │  📐          │                        │
│  │  Resize      │                        │
│  │  Coming soon │                        │
│  └─────────────┘                        │
└─────────────────────────────────────────┘
```

- Grid: 2 columns on phones, 3 on tablets (`LazyVerticalGrid` with `GridCells.Adaptive(minSize = 160.dp)`).
- Each card: leading emoji/icon, title, subtitle. Disabled cards (`enabled = false`) render at 60% alpha and don't navigate.
- Press feedback: M3 ripple + slight scale (0.97 → 1.0, 120ms) via `Modifier.pointerInput` and `animateFloatAsState`.

### Compress — Step 1 (pick)

```
┌─────────────────────────────────────────┐
│  ←   Compress Video           Step 1/3  │
├─────────────────────────────────────────┤
│                                         │
│         [    📁  Pick a video    ]      │   ← Large M3 FilledTonalButton
│                                         │
│         or drag a file here             │
│                                         │
└─────────────────────────────────────────┘
```

After pick:

```
┌─────────────────────────────────────────┐
│  ←   Compress Video           Step 1/3  │
├─────────────────────────────────────────┤
│  ┌───────────────────────────────────┐  │
│  │  ▶ thumbnail                      │  │   ← Coil-loaded frame
│  │      00:01:24                     │  │
│  └───────────────────────────────────┘  │
│  beach.mp4                              │
│  1920×1080  ·  29.97 fps  ·  H.264      │
│  8.4 Mbps  ·  86.3 MB                   │
│                                         │
│             [   Continue   ]            │   ← FilledButton
└─────────────────────────────────────────┘
```

### Compress — Step 2 (configure)

```
┌─────────────────────────────────────────┐
│  ←   Compress Video           Step 2/3  │
├─────────────────────────────────────────┤
│  Smart presets                          │
│  ( Small ) ( Balanced* ) ( HQ ) ( Social)│   ← M3 FilterChip row
│                                         │
│  ▼ Video                                │   ← Collapsible SectionCard
│    Codec     [ H.264  ▼ ]               │
│    Mode      ( CRF )( CBR )( VBR )      │
│    CRF       ━━━━━●━━━━━━━━  23         │   ← Slider 0..51
│    Resolution[ 1080p ▼ ] (max 1080p)    │
│    FPS       [ Keep ▼ ]                 │
│    Preset    [ medium ▼ ]               │
│  ▼ Audio                                │
│    Codec     AAC                        │
│    Bitrate   [ 128 kbps ▼ ]             │
│    Channels  ( Stereo )( Mono )         │
│  ▶ Advanced                             │   ← Collapsed by default
│                                         │
│  ─────────────────────────────────────  │
│  Estimated output: ~28 MB (×0.32)       │   ← OutputPreview
│  Codec: H.264 high@4.1 · 1080p · 30fps  │
│                                         │
│             [    Continue   ]           │
└─────────────────────────────────────────┘
```

### Compress — Step 3 (run + result)

```
┌─────────────────────────────────────────┐
│  ←   Compress Video           Step 3/3  │
├─────────────────────────────────────────┤
│  Compressing…                           │
│  ━━━━━━━━━━●━━━━━━━━━━━━━━  43%         │   ← LinearProgressIndicator
│  ETA 1:12 · frame 1284 · 187 fps        │
│                                         │
│  [  ⏸ Pause  ]   [  ✕ Cancel  ]         │
└─────────────────────────────────────────┘
```

After done:

```
│  ✓ Done                                 │
│  Output: 27.8 MB  (was 86.3 MB, ×0.32)  │
│  Saved to Photos › VideoEditor          │
│                                         │
│  [  Open  ]  [  Share  ]  [  Done  ]    │
```

---

## 2. Data Model

```kotlin
// core/probe/ProbeResult.kt
data class ProbeResult(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val widthPx: Int,
    val heightPx: Int,
    val frameRate: Double,        // null/Double.NaN → unknown; treated as 30
    val videoBitrateBps: Long,    // 0 if unknown
    val videoCodec: String,       // "h264", "hevc", "vp9", …
    val audioCodec: String?,      // null if no audio track
    val audioChannels: Int?,
    val sizeBytes: Long,
    val rotationDegrees: Int,     // 0 / 90 / 180 / 270
)

// feature/compress/model/CompressionSettings.kt
enum class VideoCodec { H264, H265 }
enum class RateControl { CRF, CBR, VBR }
enum class EncodingPreset { ULTRAFAST, SUPERFAST, VERYFAST, FAST, MEDIUM, SLOW, VERYSLOW }
enum class H264Profile { BASELINE, MAIN, HIGH }
enum class FpsChoice { KEEP, FPS_24, FPS_30, FPS_60 }
enum class AudioChannels { MONO, STEREO }

enum class ResolutionPreset(val shortEdgePx: Int, val label: String) {
    P480(480, "480p"), P720(720, "720p"), P1080(1080, "1080p"),
    P1440(1440, "1440p"), P2160(2160, "2160p (4K)"), P4320(4320, "4320p (8K)");
    companion object { fun keep() = null }
}

data class CompressionSettings(
    val codec: VideoCodec = VideoCodec.H264,
    val rateControl: RateControl = RateControl.CRF,
    val crf: Int = 23,                        // 0..51, default per-codec
    val targetBitrateKbps: Int = 2_500,       // used when CBR/VBR
    val fps: FpsChoice = FpsChoice.KEEP,
    val resolution: ResolutionPreset? = null, // null = keep
    val preset: EncodingPreset = EncodingPreset.MEDIUM,
    val profile: H264Profile = H264Profile.HIGH,
    val gopSeconds: Int = 2,                  // keyframe interval in seconds
    val useHardwareAccel: Boolean = true,
    val audio: AudioSettings = AudioSettings(),
)

data class AudioSettings(
    val bitrateKbps: Int = 128,               // 64/96/128/192/256
    val channels: AudioChannels = AudioChannels.STEREO,
)

enum class SmartPreset(val label: String) {
    SMALL("Small size"),
    BALANCED("Balanced"),
    HIGH_QUALITY("High quality"),
    SOCIAL("Social media"),
}
```

**Smart preset → settings mapping** (applied client-side, then user can tweak):

| Preset | Codec | Mode | CRF / Bitrate | Resolution | FPS | Preset | Audio |
|---|---|---|---|---|---|---|---|
| Small | H.265 | CRF | 30 | min(720p, original) | min(30, original) | medium | 96 kbps stereo |
| Balanced | H.264 | CRF | 23 | keep | keep | medium | 128 kbps stereo |
| High quality | H.265 | CRF | 20 | keep | keep | slow | 192 kbps stereo |
| Social | H.264 | CBR @ 6 Mbps | — | min(1080p, original) | min(30, original) | fast | 128 kbps stereo |

**State envelope for the screen:**

```kotlin
sealed interface CompressUiState {
    data object Idle : CompressUiState
    data object PickingVideo : CompressUiState
    data class Configuring(
        val source: ProbeResult,
        val settings: CompressionSettings,
        val activeSmartPreset: SmartPreset?,
        val estimate: OutputEstimate,
        val expandedSections: Set<SectionId>,
    ) : CompressUiState
    data class Running(
        val source: ProbeResult,
        val workId: UUID,
        val progress: EncodeProgress,
        val isPaused: Boolean,
    ) : CompressUiState
    data class Done(
        val source: ProbeResult,
        val output: SavedOutput,
        val ratio: Double,
    ) : CompressUiState
    data class Failed(val reason: String) : CompressUiState
}
```

---

## 3. FFmpeg Command Mapping Strategy

The builder produces a list of arguments (no shell). FFmpegKit accepts either a single string or a `String[]`. We use `String[]` to avoid quoting bugs.

### Argument order convention

```
ffmpeg -y -hide_banner -i <input> [filter_args] [video_args] [audio_args] -movflags +faststart -progress pipe:1 <output>
```

### Per-control mapping

| Control | Args |
|---|---|
| Input | `-i <abs path of cached copy>` (we copy `content://` URIs into cache because FFmpeg needs a real path) |
| Output container | Always `.mp4` (compatible with H.264 + H.265 + AAC) |
| Codec H.264 SW | `-c:v libx264` |
| Codec H.264 HW | `-c:v h264_mediacodec` |
| Codec H.265 SW | `-c:v libx265 -tag:v hvc1` (`hvc1` for QuickTime/iOS playback) |
| Codec H.265 HW | `-c:v hevc_mediacodec -tag:v hvc1` |
| Preset (SW only) | `-preset <ultrafast..veryslow>` (silently dropped on `*_mediacodec`) |
| Profile H.264 | `-profile:v baseline\|main\|high` |
| Profile H.265 | `-profile:v main` (always; `main10` requires 10-bit input) |
| Level | `-level 4.1` (auto-bumped from resolution: ≤1080p30→4.1, ≤2160p30→5.1, ≤2160p60→5.2, ≤4320p→6.2) |
| CRF | `-crf <int>` (libx264 0..51 default 23; libx265 0..51 default 28) |
| CBR | `-b:v <kbps>k -minrate <kbps>k -maxrate <kbps>k -bufsize <2*kbps>k -x264-params nal-hrd=cbr` (libx265 uses `-x265-params strict-cbr=1`) |
| VBR | `-b:v <kbps>k -maxrate <1.5*kbps>k -bufsize <2*kbps>k` |
| HW bitrate | `-b:v <kbps>k` (mediacodec ignores CRF; we silently switch to CBR-like when HW is on) |
| FPS | `-r <int>` (omitted if `KEEP`) |
| Resolution | `-vf scale=-2:<short>:flags=lanczos` if landscape, else `-vf scale=<short>:-2:flags=lanczos`. `-2` keeps even dimensions. Skipped if target ≥ source on the relevant axis. |
| Rotation | If source rotation ≠ 0, prepend `-noautorotate` and chain `transpose=...` (or simply trust mp4 metadata via `-metadata:s:v rotate=<deg>` — we do the latter for speed). |
| GOP | `-g <fps × gopSeconds>` `-keyint_min <fps × gopSeconds>` (`-sc_threshold 0` to disable scene-cut keyframes when CBR) |
| Audio codec | `-c:a aac -b:a <kbps>k -ac <1|2>` |
| Audio (no track) | `-an` |
| Faststart | `-movflags +faststart` (moves moov atom to front for streamable playback) |
| Progress | `-progress pipe:1 -nostats` (parsed by `ProgressParser`) |
| Misc | `-y -hide_banner -loglevel warning` |

### Hardware accel decision tree

```
useHardwareAccel?
├── no → libx264 / libx265
└── yes
    ├── codec=H264 → h264_mediacodec
    └── codec=H265
         ├── target.shortEdge ≤ 2160 → hevc_mediacodec
         └── 8K → libx265 (most devices' HW HEVC encoders cap below 8K)
```

If FFmpeg returns a non-zero exit code with a `*_mediacodec` encoder, the runner re-runs once with the software encoder and sets `result.usedFallback = true` so the UI can surface "Hardware encoder unsupported, used software fallback".

### Example commands

**Balanced preset, 1080p H.264 source, default settings:**

```
ffmpeg -y -hide_banner -loglevel warning -i /cache/in.mp4 \
  -c:v libx264 -preset medium -profile:v high -level 4.1 \
  -crf 23 -g 60 -keyint_min 60 \
  -c:a aac -b:a 128k -ac 2 \
  -movflags +faststart -progress pipe:1 -nostats \
  /cache/out.mp4
```

**Social preset, downscale 4K → 1080p, CBR 6 Mbps:**

```
ffmpeg -y -hide_banner -loglevel warning -i /cache/in.mp4 \
  -vf scale=-2:1080:flags=lanczos \
  -c:v libx264 -preset fast -profile:v high -level 4.1 \
  -b:v 6000k -minrate 6000k -maxrate 6000k -bufsize 12000k \
  -x264-params nal-hrd=cbr \
  -r 30 -g 60 -keyint_min 60 -sc_threshold 0 \
  -c:a aac -b:a 128k -ac 2 \
  -movflags +faststart -progress pipe:1 -nostats \
  /cache/out.mp4
```

### Progress parsing

FFmpeg `-progress pipe:1` writes `key=value` lines, one per ~500ms, ending each block with `progress=continue` / `progress=end`. We parse:

- `out_time_us` → fraction of `durationMs` → percent
- `frame` → display
- `fps` → for ETA: `ETA = (totalFrames - frame) / fps`
- `bitrate` → display

---

## 4. Performance Considerations

- **Hardware encoders**: 5–10× faster than libx264 on most SoCs and dramatically lower battery, but quality at the same bitrate is generally worse and they ignore `-crf`. We default `useHardwareAccel = true` and bias HW encodes toward CBR with a slightly higher bitrate.
- **Filter graphs are expensive**: we only emit `-vf scale=...` when actually needed (smaller-than-source). No filter ⇒ FFmpeg can do bit-exact stream copy when codecs match (out of MVP scope, but the absence of filter args is what would unlock it later).
- **Container copy when nothing changes**: reserved for a future "remux only" path.
- **Working dir**: Encode I/O happens in `cacheDir/compress/<workId>/` (private, evictable). Final file is moved to `MediaStore` only after success — partial files never appear in Photos.
- **Memory**: We never decode video frames into the JVM heap. FFmpeg streams natively.
- **Foreground service**: required to keep encoding alive when the screen is off; uses `FOREGROUND_SERVICE_DATA_SYNC` foreground type (Android 14+).
- **Battery**: We respect `WorkManager` constraints — `setRequiresCharging(false)` (we want to run anyway) but `setRequiresBatteryNotLow(true)` so we pause auto-restarts when the battery is critically low.
- **Cancellation latency**: WorkManager cancellation is cooperative. We register `coroutineContext[Job]?.invokeOnCompletion { FFmpegKit.cancel(sessionId) }` so cancel is near-instant.
- **Probe latency**: `MediaMetadataRetriever` is fast (<200 ms) but bitrate is sometimes missing; we lazily call `FFprobeKit.getMediaInformation` only when we need accurate bitrate (i.e., for the size estimator).
- **Coil thumbnail decoding** uses a `VideoFrameDecoder` so we never block the main thread.

---

## 5. File Structure

```
video-editor/
├── settings.gradle.kts                 (module include)
├── build.gradle.kts                    (root, plugin versions)
├── gradle/
│   └── libs.versions.toml              (Gradle version catalog — single source of truth for deps)
├── gradle.properties                   (jvm args, AndroidX flag)
└── app/
    ├── build.gradle.kts                (Compose, Hilt, FFmpegKit, etc.)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml         (perms, service, picker)
        ├── res/
        │   ├── values/strings.xml
        │   ├── values/themes.xml
        │   ├── values-night/themes.xml
        │   └── xml/file_paths.xml      (FileProvider for share)
        └── kotlin/com/videoeditor/
            ├── App.kt                  (@HiltAndroidApp, WM init)
            ├── MainActivity.kt         (single Activity)
            ├── core/
            │   ├── theme/
            │   │   ├── Color.kt
            │   │   ├── Type.kt
            │   │   ├── Motion.kt
            │   │   └── Theme.kt
            │   ├── designsys/
            │   │   ├── SectionCard.kt
            │   │   ├── StepHeader.kt
            │   │   ├── LabeledRow.kt
            │   │   └── ChipsRow.kt
            │   ├── navigation/
            │   │   ├── Routes.kt
            │   │   ├── FeatureRegistry.kt
            │   │   └── AppNavHost.kt
            │   ├── probe/
            │   │   ├── ProbeResult.kt
            │   │   └── VideoProbe.kt
            │   ├── ffmpeg/
            │   │   ├── FFmpegCommandBuilder.kt
            │   │   ├── FFmpegRunner.kt
            │   │   └── ProgressParser.kt
            │   ├── estimator/
            │   │   └── OutputSizeEstimator.kt
            │   ├── storage/
            │   │   ├── MediaStoreSaver.kt
            │   │   └── ScopedTempDir.kt
            │   └── di/
            │       └── CoreModule.kt
            └── feature/
                ├── home/
                │   ├── HomeScreen.kt
                │   └── HomeViewModel.kt        (very thin — just exposes registry)
                └── compress/
                    ├── CompressScreen.kt        (entry route)
                    ├── CompressViewModel.kt
                    ├── ui/
                    │   ├── PickStep.kt
                    │   ├── ConfigureStep.kt
                    │   ├── RunStep.kt
                    │   ├── DoneStep.kt
                    │   ├── SmartPresetChips.kt
                    │   ├── VideoSection.kt
                    │   ├── AudioSection.kt
                    │   ├── AdvancedSection.kt
                    │   └── OutputPreviewBar.kt
                    ├── model/
                    │   ├── CompressionSettings.kt
                    │   ├── SmartPreset.kt
                    │   ├── CompressUiState.kt
                    │   └── OutputEstimate.kt
                    ├── work/
                    │   ├── CompressWorker.kt
                    │   ├── CompressWorkLauncher.kt
                    │   └── CompressForegroundInfo.kt
                    └── di/
                        └── CompressModule.kt
```

This structure is pragmatic single-module: clean enough that a future split into `:feature-compress` / `:core` Gradle modules is a mechanical move.

---

## Phase A — Project Bootstrap

### Task A1: Initialise Gradle wrapper, project skeleton, and version catalog

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` (via `gradle wrapper --gradle-version 8.10`)

- [ ] **Step 1: Install Android Studio Ladybug+ and JDK 17.** Open the empty `video-editor/` folder in Android Studio so it can suggest the correct Gradle version. Or use the CLI: `gradle wrapper --gradle-version 8.10` from the project root (requires a system Gradle install).

- [ ] **Step 2: Create `settings.gradle.kts`:**

```kotlin
pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "VideoEditor"
include(":app")
```

- [ ] **Step 3: Create `gradle/libs.versions.toml`:**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
hilt = "2.52"
hiltNavCompose = "1.2.0"
composeBom = "2025.01.00"
activityCompose = "1.9.3"
navCompose = "2.8.5"
lifecycle = "2.8.7"
workManager = "2.10.0"
datastore = "1.1.1"
coil = "2.7.0"
accompanist = "0.36.0"
ffmpegKit = "6.0-2"
media3 = "1.5.1"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.15.0" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navCompose" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-nav-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavCompose" }
hilt-work = { module = "androidx.hilt:hilt-work", version = "1.2.0" }
hilt-work-compiler = { module = "androidx.hilt:hilt-compiler", version = "1.2.0" }
work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "workManager" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
coil-video = { module = "io.coil-kt:coil-video", version.ref = "coil" }
accompanist-permissions = { module = "com.google.accompanist:accompanist-permissions", version.ref = "accompanist" }
ffmpeg-kit-full = { module = "com.arthenica:ffmpeg-kit-full", version.ref = "ffmpegKit" }
media3-common = { module = "androidx.media3:media3-common", version.ref = "media3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 4: Create root `build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 5: Create `gradle.properties`:**

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 6: Verify scaffold:** Run `./gradlew --version`. Expected: prints Gradle 8.10, JVM 17.

- [ ] **Step 7: Commit.**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml gradle/wrapper/ gradlew gradlew.bat
git commit -m "chore: gradle wrapper, version catalog, root build script"
```

---

### Task A2: Create the `:app` module

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Write `app/build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.videoeditor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.videoeditor"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { /* defaults */ }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/INDEX.LIST",
            "META-INF/io.netty.versions.properties",
        )
        // FFmpegKit ships .so per ABI — keep them.
        jniLibs.useLegacyPackaging = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.nav.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.accompanist.permissions)

    implementation(libs.ffmpeg.kit.full)
    implementation(libs.media3.common)
}
```

- [ ] **Step 2: Write `app/proguard-rules.pro`:**

```
# FFmpegKit JNI
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class **_HiltModules { *; }
```

- [ ] **Step 3: Write `AndroidManifest.xml`:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Picker handles read perms on Android 13+; legacy READ for older APIs only. -->
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO"
        android:minSdkVersion="33" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <application
        android:name=".App"
        android:allowBackup="false"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.VideoEditor"
        android:largeHeap="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.VideoEditor">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge"
            xmlns:tools="http://schemas.android.com/tools">
            <!-- Disable default WM init; we configure WM via Hilt -->
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
    </application>
</manifest>
```

- [ ] **Step 4: Write `app/src/main/res/values/strings.xml`:**

```xml
<resources>
    <string name="app_name">Video Editor</string>
    <string name="home_title">Video Editor</string>
    <string name="home_section_tools">Tools</string>
    <string name="feature_compress_title">Compress</string>
    <string name="feature_compress_subtitle">Shrink size</string>
    <string name="feature_trim_title">Trim</string>
    <string name="feature_convert_title">Convert</string>
    <string name="feature_audio_title">Audio</string>
    <string name="feature_resize_title">Resize</string>
    <string name="feature_coming_soon">Coming soon</string>
</resources>
```

- [ ] **Step 5: Build sanity check:** `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL (no source code yet but Gradle resolves all dependencies — this catches version clashes early).

- [ ] **Step 6: Commit.**

```bash
git add app/
git commit -m "feat(app): scaffold :app module with Compose + Hilt + WorkManager + FFmpegKit"
```

---

### Task A3: `App.kt`, `MainActivity.kt`, base theme

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/App.kt`
- Create: `app/src/main/kotlin/com/videoeditor/MainActivity.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/theme/Color.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/theme/Type.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/theme/Theme.kt`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values-night/themes.xml`

- [ ] **Step 1: `themes.xml` (light) and `values-night/themes.xml` (dark):**

```xml
<!-- values/themes.xml -->
<resources>
    <style name="Theme.VideoEditor" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
    </style>
</resources>
```

```xml
<!-- values-night/themes.xml -->
<resources>
    <style name="Theme.VideoEditor" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
```

- [ ] **Step 2: `core/theme/Color.kt`:**

```kotlin
package com.videoeditor.core.theme

import androidx.compose.ui.graphics.Color

// Brand: cool indigo with cinematic accent
val IndigoSeed = Color(0xFF5B6CFF)
val AccentTeal = Color(0xFF14E0C2)
val SurfaceLight = Color(0xFFFBFBFE)
val SurfaceDark  = Color(0xFF101015)
```

- [ ] **Step 3: `core/theme/Type.kt`:**

```kotlin
package com.videoeditor.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
)
```

- [ ] **Step 4: `core/theme/Theme.kt`:**

```kotlin
package com.videoeditor.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = IndigoSeed,
    secondary = AccentTeal,
    background = SurfaceLight,
)
private val DarkScheme = darkColorScheme(
    primary = IndigoSeed,
    secondary = AccentTeal,
    background = SurfaceDark,
)

@Composable
fun VideoEditorTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
```

- [ ] **Step 5: `App.kt`:**

```kotlin
package com.videoeditor

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
```

- [ ] **Step 6: `MainActivity.kt`:**

```kotlin
package com.videoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.rememberNavController
import com.videoeditor.core.navigation.AppNavHost
import com.videoeditor.core.theme.VideoEditorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VideoEditorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val nav = rememberNavController()
                    AppNavHost(nav)
                }
            }
        }
    }
}
```

(Compiles after Task B1 supplies `AppNavHost`.)

- [ ] **Step 7: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/App.kt \
        app/src/main/kotlin/com/videoeditor/MainActivity.kt \
        app/src/main/kotlin/com/videoeditor/core/theme/ \
        app/src/main/res/values/themes.xml \
        app/src/main/res/values-night/themes.xml
git commit -m "feat(core): app entry, single Activity, M3 theme"
```

---

## Phase B — Navigation Shell + Home

### Task B1: Routes, FeatureRegistry, AppNavHost

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/navigation/Routes.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/navigation/FeatureRegistry.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/navigation/AppNavHost.kt`

- [ ] **Step 1: `Routes.kt`:**

```kotlin
package com.videoeditor.core.navigation

object Routes {
    const val HOME = "home"
    const val COMPRESS = "compress"
}
```

- [ ] **Step 2: `FeatureRegistry.kt`:**

```kotlin
package com.videoeditor.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

data class FeatureCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String?,             // null ⇒ disabled card
)

object FeatureRegistry {
    val cards: List<FeatureCard> = listOf(
        FeatureCard("compress", "Compress", "Shrink size", Icons.Outlined.Compress, Routes.COMPRESS),
        FeatureCard("trim", "Trim", "Coming soon", Icons.Outlined.ContentCut, route = null),
        FeatureCard("convert", "Convert", "Coming soon", Icons.Outlined.SwapHoriz, route = null),
        FeatureCard("audio", "Audio", "Coming soon", Icons.Outlined.AudioFile, route = null),
        FeatureCard("resize", "Resize", "Coming soon", Icons.Outlined.PhotoSizeSelectLarge, route = null),
    )
}
```

- [ ] **Step 3: `AppNavHost.kt`:**

```kotlin
package com.videoeditor.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.videoeditor.feature.compress.CompressScreen
import com.videoeditor.feature.home.HomeScreen

@Composable
fun AppNavHost(nav: NavHostController) {
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onOpenFeature = { route -> nav.navigate(route) })
        }
        composable(Routes.COMPRESS) {
            CompressScreen(onBack = { nav.popBackStack() })
        }
    }
}
```

- [ ] **Step 4: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/core/navigation/
git commit -m "feat(nav): routes, feature registry, NavHost shell"
```

---

### Task B2: Home screen UI

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/feature/home/HomeScreen.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/SectionCard.kt`

- [ ] **Step 1: `core/designsys/SectionCard.kt` (reusable container):**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
```

- [ ] **Step 2: `HomeScreen.kt`:**

```kotlin
package com.videoeditor.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.videoeditor.core.navigation.FeatureCard
import com.videoeditor.core.navigation.FeatureRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenFeature: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Video Editor") }) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(FeatureRegistry.cards, key = { it.id }) { card ->
                FeatureCardView(card) { route -> route?.let(onOpenFeature) }
            }
        }
    }
}

@Composable
private fun FeatureCardView(card: FeatureCard, onClick: (String?) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press-scale")
    val enabled = card.route != null
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = if (enabled) 2.dp else 0.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled) {
                pressed = true
                onClick(card.route)
                pressed = false
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(card.icon, contentDescription = null, modifier = Modifier)
            Column {
                Text(card.title, style = MaterialTheme.typography.headlineSmall)
                Text(card.subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

- [ ] **Step 3: Stub `CompressScreen` so the project compiles** (Task C1 fleshes it out). Create `app/src/main/kotlin/com/videoeditor/feature/compress/CompressScreen.kt`:

```kotlin
package com.videoeditor.feature.compress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CompressScreen(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Compress (TODO)")
    }
}
```

- [ ] **Step 4: Verify:** `./gradlew :app:installDebug` then launch. Expected: Home screen with 5 cards (Compress active, others disabled). Tapping Compress navigates to placeholder screen.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/feature/home/ \
        app/src/main/kotlin/com/videoeditor/feature/compress/CompressScreen.kt \
        app/src/main/kotlin/com/videoeditor/core/designsys/
git commit -m "feat(home): feature-hub grid with M3 cards + press animation"
```

---

## Phase C — Compress Feature

### Task C1: Domain models

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/model/CompressionSettings.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/model/SmartPreset.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/model/CompressUiState.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/model/OutputEstimate.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/probe/ProbeResult.kt`

- [ ] **Step 1: `core/probe/ProbeResult.kt`** — copy verbatim from §2 Data Model.

- [ ] **Step 2: `model/CompressionSettings.kt`** — copy enums + `CompressionSettings` + `AudioSettings` from §2.

- [ ] **Step 3: `model/SmartPreset.kt`:**

```kotlin
package com.videoeditor.feature.compress.model

import com.videoeditor.core.probe.ProbeResult

enum class SmartPreset(val label: String) {
    SMALL("Small size"),
    BALANCED("Balanced"),
    HIGH_QUALITY("High quality"),
    SOCIAL("Social media");

    fun apply(source: ProbeResult): CompressionSettings = when (this) {
        SMALL -> CompressionSettings(
            codec = VideoCodec.H265,
            rateControl = RateControl.CRF,
            crf = 30,
            resolution = capResolution(source, ResolutionPreset.P720),
            fps = capFps(source, 30),
            preset = EncodingPreset.MEDIUM,
            audio = AudioSettings(bitrateKbps = 96),
        )
        BALANCED -> CompressionSettings()  // defaults already match "Balanced"
        HIGH_QUALITY -> CompressionSettings(
            codec = VideoCodec.H265,
            rateControl = RateControl.CRF,
            crf = 20,
            preset = EncodingPreset.SLOW,
            audio = AudioSettings(bitrateKbps = 192),
        )
        SOCIAL -> CompressionSettings(
            codec = VideoCodec.H264,
            rateControl = RateControl.CBR,
            targetBitrateKbps = 6_000,
            resolution = capResolution(source, ResolutionPreset.P1080),
            fps = capFps(source, 30),
            preset = EncodingPreset.FAST,
            audio = AudioSettings(bitrateKbps = 128),
        )
    }
}

private fun capResolution(source: ProbeResult, target: ResolutionPreset): ResolutionPreset? {
    val srcShort = minOf(source.widthPx, source.heightPx)
    return if (srcShort <= target.shortEdgePx) null else target  // null == "keep original"
}

private fun capFps(source: ProbeResult, target: Int): FpsChoice {
    val src = if (source.frameRate.isNaN() || source.frameRate <= 0) 30.0 else source.frameRate
    return if (src <= target) FpsChoice.KEEP else when (target) {
        24 -> FpsChoice.FPS_24
        30 -> FpsChoice.FPS_30
        60 -> FpsChoice.FPS_60
        else -> FpsChoice.KEEP
    }
}
```

- [ ] **Step 4: `model/OutputEstimate.kt`:**

```kotlin
package com.videoeditor.feature.compress.model

data class OutputEstimate(
    val sizeBytes: Long,
    val ratio: Double,        // sizeBytes / sourceSizeBytes
    val effectiveBitrateKbps: Int,
    val notes: List<String>,  // e.g. "Hardware encoder will be used"
)
```

- [ ] **Step 5: `model/CompressUiState.kt`** — copy `sealed interface CompressUiState` and supporting types from §2. Add the placeholders:

```kotlin
package com.videoeditor.feature.compress.model

import android.net.Uri
import com.videoeditor.core.probe.ProbeResult
import java.util.UUID

enum class SectionId { VIDEO, AUDIO, ADVANCED }

data class EncodeProgress(
    val percent: Float = 0f,           // 0..1
    val frame: Long = 0,
    val fps: Double = 0.0,
    val etaSeconds: Long? = null,
    val bitrateKbps: Int? = null,
)

data class SavedOutput(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val usedHardwareFallback: Boolean = false,
)

sealed interface CompressUiState {
    data object Idle : CompressUiState
    data object PickingVideo : CompressUiState
    data class Configuring(
        val source: ProbeResult,
        val settings: CompressionSettings,
        val activeSmartPreset: SmartPreset?,
        val estimate: OutputEstimate,
        val expandedSections: Set<SectionId> = setOf(SectionId.VIDEO, SectionId.AUDIO),
    ) : CompressUiState
    data class Running(
        val source: ProbeResult,
        val workId: UUID,
        val progress: EncodeProgress,
        val isPaused: Boolean = false,
    ) : CompressUiState
    data class Done(
        val source: ProbeResult,
        val output: SavedOutput,
        val ratio: Double,
    ) : CompressUiState
    data class Failed(val source: ProbeResult?, val reason: String) : CompressUiState
}
```

- [ ] **Step 6: Build.** `./gradlew :app:assembleDebug` — pure data classes, must succeed.

- [ ] **Step 7: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/feature/compress/model/ \
        app/src/main/kotlin/com/videoeditor/core/probe/ProbeResult.kt
git commit -m "feat(compress): domain models — settings, presets, ui state"
```

---

### Task C2: VideoProbe (gather metadata from a picked URI)

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/probe/VideoProbe.kt`

- [ ] **Step 1: `VideoProbe.kt`:**

```kotlin
package com.videoeditor.core.probe

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.arthenica.ffmpegkit.FFprobeKit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoProbe @Inject constructor(@ApplicationContext private val ctx: Context) {

    suspend fun probe(uri: Uri): ProbeResult = withContext(Dispatchers.IO) {
        val (displayName, size) = queryNameAndSize(uri)
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(ctx, uri)
            val durationMs = mmr.extract(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = mmr.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = mmr.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = mmr.extract(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val mime = mmr.extract(MediaMetadataRetriever.METADATA_KEY_MIMETYPE).orEmpty()

            // Use FFprobe for accurate bitrate + frame rate + codec ids.
            val probeJson = ffprobeJson(uri)
            val (videoBitrate, frameRate, vCodec) = parseVideoStream(probeJson)
            val (aCodec, aChannels) = parseAudioStream(probeJson)

            ProbeResult(
                uri = uri,
                displayName = displayName,
                durationMs = durationMs,
                widthPx = width,
                heightPx = height,
                frameRate = frameRate,
                videoBitrateBps = videoBitrate,
                videoCodec = vCodec.ifEmpty { mime.removePrefix("video/") },
                audioCodec = aCodec,
                audioChannels = aChannels,
                sizeBytes = size,
                rotationDegrees = rotation,
            )
        } finally {
            mmr.release()
        }
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long> {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val name = c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val size = c.getLong(c.getColumnIndexOrThrow(OpenableColumns.SIZE))
                return name to size
            }
        }
        return (uri.lastPathSegment ?: "video") to 0L
    }

    private fun ffprobeJson(uri: Uri): JSONObject {
        // FFprobe needs a real path; use the SAF "fd:" pseudo-path for content URIs.
        val path = "content:" + uri.toString().removePrefix("content:")
        // FFmpegKit's getMediaInformationFromCommand handles "saf:" automatically when given a proper FD.
        // Simpler: open a parcelFileDescriptor and pass /proc/self/fd/<n>.
        val pfd = ctx.contentResolver.openFileDescriptor(uri, "r")
            ?: return JSONObject()
        return pfd.use {
            val fdPath = "pipe:${it.fd}"
            val session = FFprobeKit.getMediaInformation(fdPath)
            JSONObject(session.allProperties?.toString() ?: "{}")
        }
    }

    private fun parseVideoStream(json: JSONObject): Triple<Long, Double, String> {
        val streams = json.optJSONArray("streams") ?: return Triple(0L, Double.NaN, "")
        for (i in 0 until streams.length()) {
            val s = streams.getJSONObject(i)
            if (s.optString("codec_type") == "video") {
                val bitrate = s.optString("bit_rate").toLongOrNull() ?: 0L
                val fr = s.optString("avg_frame_rate", "0/1").let { ratio ->
                    val parts = ratio.split("/")
                    val num = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    val den = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                    if (den == 0.0) 0.0 else num / den
                }
                val codec = s.optString("codec_name", "")
                return Triple(bitrate, if (fr > 0) fr else Double.NaN, codec)
            }
        }
        return Triple(0L, Double.NaN, "")
    }

    private fun parseAudioStream(json: JSONObject): Pair<String?, Int?> {
        val streams = json.optJSONArray("streams") ?: return null to null
        for (i in 0 until streams.length()) {
            val s = streams.getJSONObject(i)
            if (s.optString("codec_type") == "audio") {
                return s.optString("codec_name").ifEmpty { null } to
                        s.optInt("channels").takeIf { it > 0 }
            }
        }
        return null to null
    }

    private fun MediaMetadataRetriever.extract(key: Int): String? = extractMetadata(key)
}
```

> Note: The `pipe:` trick above is fragile across SAF providers. Acceptable simpler fallback: copy the URI to a cache file first (Task C5 already does this for the encode); call probe on the cache file. If you hit issues here, swap to: `val cached = ScopedTempDir.copyToCache(uri); FFprobeKit.getMediaInformation(cached.absolutePath)`.

- [ ] **Step 2: Manual verify** — defer until Task C4 wires this into the UI.

- [ ] **Step 3: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/core/probe/VideoProbe.kt
git commit -m "feat(probe): VideoProbe via MediaMetadataRetriever + FFprobeKit"
```

---

### Task C3: FFmpegCommandBuilder

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/ffmpeg/FFmpegCommandBuilder.kt`

- [ ] **Step 1: `FFmpegCommandBuilder.kt`:**

```kotlin
package com.videoeditor.core.ffmpeg

import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.feature.compress.model.AudioChannels
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.EncodingPreset
import com.videoeditor.feature.compress.model.FpsChoice
import com.videoeditor.feature.compress.model.H264Profile
import com.videoeditor.feature.compress.model.RateControl
import com.videoeditor.feature.compress.model.ResolutionPreset
import com.videoeditor.feature.compress.model.VideoCodec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FFmpegCommandBuilder @Inject constructor() {

    /**
     * Builds a String[] for FFmpegKit.executeAsync(args, ...).
     * @param input absolute file path (caller resolves URI -> file)
     * @param output absolute output mp4 path
     * @param useHwEncoder if true, builds with mediacodec; caller decides fallback on failure.
     */
    fun build(
        source: ProbeResult,
        settings: CompressionSettings,
        input: String,
        output: String,
        useHwEncoder: Boolean = settings.useHardwareAccel,
    ): Array<String> {
        val args = mutableListOf("-y", "-hide_banner", "-loglevel", "warning", "-i", input)

        val effectiveFps = effectiveFps(source, settings.fps)
        val resolution = effectiveResolution(source, settings.resolution)

        // Filter graph (only when scaling is needed)
        if (resolution != null) {
            val isLandscape = source.widthPx >= source.heightPx
            val scale = if (isLandscape)
                "scale=-2:${resolution.shortEdgePx}:flags=lanczos"
            else
                "scale=${resolution.shortEdgePx}:-2:flags=lanczos"
            args += listOf("-vf", scale)
        }

        // Video codec
        args += videoCodecArgs(settings.codec, useHwEncoder)

        // Preset (software only)
        if (!useHwEncoder) {
            args += listOf("-preset", settings.preset.cli())
        }

        // Profile + level
        when (settings.codec) {
            VideoCodec.H264 -> {
                args += listOf("-profile:v", settings.profile.cli())
                args += listOf("-level", h264Level(resolution?.shortEdgePx ?: source.shortEdge(), effectiveFps))
            }
            VideoCodec.H265 -> {
                args += listOf("-profile:v", "main")
                args += listOf("-level", h265Level(resolution?.shortEdgePx ?: source.shortEdge(), effectiveFps))
                args += listOf("-tag:v", "hvc1")
            }
        }

        // Rate control
        when (settings.rateControl) {
            RateControl.CRF -> {
                if (useHwEncoder) {
                    // mediacodec ignores CRF; map to a conservative bitrate from CRF
                    val kbps = crfToBitrateKbps(settings.crf, resolution?.shortEdgePx ?: source.shortEdge(), effectiveFps)
                    args += listOf("-b:v", "${kbps}k")
                } else {
                    args += listOf("-crf", settings.crf.toString())
                }
            }
            RateControl.CBR -> {
                val kbps = settings.targetBitrateKbps
                args += listOf("-b:v", "${kbps}k", "-minrate", "${kbps}k",
                               "-maxrate", "${kbps}k", "-bufsize", "${kbps * 2}k")
                if (!useHwEncoder) {
                    args += when (settings.codec) {
                        VideoCodec.H264 -> listOf("-x264-params", "nal-hrd=cbr")
                        VideoCodec.H265 -> listOf("-x265-params", "strict-cbr=1")
                    }
                }
            }
            RateControl.VBR -> {
                val kbps = settings.targetBitrateKbps
                val max = (kbps * 1.5).toInt()
                args += listOf("-b:v", "${kbps}k", "-maxrate", "${max}k", "-bufsize", "${kbps * 2}k")
            }
        }

        // FPS
        if (settings.fps != FpsChoice.KEEP) {
            args += listOf("-r", effectiveFps.toString())
        }

        // GOP
        val gopFrames = (effectiveFps * settings.gopSeconds).coerceAtLeast(1)
        args += listOf("-g", gopFrames.toString(), "-keyint_min", gopFrames.toString())
        if (settings.rateControl == RateControl.CBR) args += listOf("-sc_threshold", "0")

        // Audio
        if (source.audioCodec == null) {
            args += "-an"
        } else {
            args += listOf("-c:a", "aac",
                           "-b:a", "${settings.audio.bitrateKbps}k",
                           "-ac", if (settings.audio.channels == AudioChannels.MONO) "1" else "2")
        }

        // Faststart + progress
        args += listOf("-movflags", "+faststart", "-progress", "pipe:1", "-nostats", output)

        return args.toTypedArray()
    }

    // ---- Helpers ----

    private fun videoCodecArgs(codec: VideoCodec, hw: Boolean): List<String> = when {
        codec == VideoCodec.H264 && hw -> listOf("-c:v", "h264_mediacodec")
        codec == VideoCodec.H264 && !hw -> listOf("-c:v", "libx264")
        codec == VideoCodec.H265 && hw -> listOf("-c:v", "hevc_mediacodec")
        codec == VideoCodec.H265 && !hw -> listOf("-c:v", "libx265")
        else -> error("unreachable")
    }

    private fun effectiveFps(source: ProbeResult, fps: FpsChoice): Int {
        val src = if (source.frameRate.isNaN() || source.frameRate <= 0) 30.0 else source.frameRate
        val target = when (fps) {
            FpsChoice.KEEP -> src.toInt().coerceAtLeast(1)
            FpsChoice.FPS_24 -> 24
            FpsChoice.FPS_30 -> 30
            FpsChoice.FPS_60 -> 60
        }
        return target.coerceAtMost(src.toInt().coerceAtLeast(1))   // never exceed source
    }

    private fun effectiveResolution(source: ProbeResult, target: ResolutionPreset?): ResolutionPreset? {
        target ?: return null
        return if (target.shortEdgePx >= source.shortEdge()) null else target  // never upscale
    }

    private fun h264Level(shortEdgePx: Int, fps: Int): String = when {
        shortEdgePx <= 1080 && fps <= 30 -> "4.1"
        shortEdgePx <= 2160 && fps <= 30 -> "5.1"
        shortEdgePx <= 2160 && fps <= 60 -> "5.2"
        else -> "6.2"
    }

    private fun h265Level(shortEdgePx: Int, fps: Int): String = when {
        shortEdgePx <= 1080 && fps <= 30 -> "4"
        shortEdgePx <= 2160 && fps <= 30 -> "5"
        shortEdgePx <= 2160 && fps <= 60 -> "5.1"
        else -> "6.2"
    }

    private fun crfToBitrateKbps(crf: Int, shortEdgePx: Int, fps: Int): Int {
        // Rough mapping: aim for visually-equivalent CBR bitrate at 1080p30 baseline.
        val base = when {
            shortEdgePx <= 480 -> 800
            shortEdgePx <= 720 -> 1_800
            shortEdgePx <= 1080 -> 4_500
            shortEdgePx <= 1440 -> 9_000
            shortEdgePx <= 2160 -> 22_000
            else -> 60_000
        }
        val crfFactor = Math.pow(2.0, (23 - crf) / 6.0)  // each 6-step ≈ 2× bitrate
        val fpsFactor = fps / 30.0
        return (base * crfFactor * fpsFactor).toInt().coerceIn(200, 200_000)
    }

    private fun ProbeResult.shortEdge() = minOf(widthPx, heightPx)

    private fun EncodingPreset.cli() = name.lowercase()
    private fun H264Profile.cli() = name.lowercase()
}
```

- [ ] **Step 2: Build.** `./gradlew :app:assembleDebug`. Expected: success.

- [ ] **Step 3: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/core/ffmpeg/FFmpegCommandBuilder.kt
git commit -m "feat(ffmpeg): command builder for full compression matrix"
```

---

### Task C4: OutputSizeEstimator

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/estimator/OutputSizeEstimator.kt`

- [ ] **Step 1: `OutputSizeEstimator.kt`:**

```kotlin
package com.videoeditor.core.estimator

import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.feature.compress.model.AudioChannels
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.FpsChoice
import com.videoeditor.feature.compress.model.OutputEstimate
import com.videoeditor.feature.compress.model.RateControl
import com.videoeditor.feature.compress.model.ResolutionPreset
import com.videoeditor.feature.compress.model.VideoCodec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class OutputSizeEstimator @Inject constructor() {

    fun estimate(source: ProbeResult, s: CompressionSettings): OutputEstimate {
        val effectiveFps = effectiveFps(source, s.fps)
        val effectiveRes = effectiveResolution(source, s.resolution)
        val shortEdge = effectiveRes?.shortEdgePx ?: minOf(source.widthPx, source.heightPx)

        val videoKbps = when (s.rateControl) {
            RateControl.CBR, RateControl.VBR -> s.targetBitrateKbps
            RateControl.CRF -> crfToBitrate(s.crf, s.codec, shortEdge, effectiveFps)
        }
        val audioKbps = if (source.audioCodec != null) s.audio.bitrateKbps else 0
        val totalKbps = videoKbps + audioKbps
        val durSec = source.durationMs / 1000.0
        val sizeBytes = (totalKbps * 1000.0 / 8.0 * durSec).toLong() + 100_000  // +100 KB container overhead

        val ratio = if (source.sizeBytes > 0) sizeBytes.toDouble() / source.sizeBytes else 0.0
        val notes = buildList {
            if (s.useHardwareAccel) add("Hardware encoder will be used (CRF mapped to bitrate).")
            if (effectiveRes == null && s.resolution != null) add("Original resolution kept (no upscaling).")
            if (s.fps != FpsChoice.KEEP && effectiveFps != s.fps.toInt()) add("FPS clamped to source (${effectiveFps}).")
        }
        return OutputEstimate(sizeBytes, ratio, totalKbps, notes)
    }

    private fun crfToBitrate(crf: Int, codec: VideoCodec, shortEdge: Int, fps: Int): Int {
        val base = when {
            shortEdge <= 480 -> 800
            shortEdge <= 720 -> 1_800
            shortEdge <= 1080 -> 4_500
            shortEdge <= 1440 -> 9_000
            shortEdge <= 2160 -> 22_000
            else -> 60_000
        }
        val codecFactor = if (codec == VideoCodec.H265) 0.65 else 1.0  // HEVC ~35% smaller
        val crfFactor = Math.pow(2.0, (23 - crf) / 6.0)
        val fpsFactor = fps / 30.0
        return max(200, (base * codecFactor * crfFactor * fpsFactor).toInt())
    }

    private fun effectiveFps(source: ProbeResult, fps: FpsChoice): Int {
        val src = if (source.frameRate.isNaN() || source.frameRate <= 0) 30.0 else source.frameRate
        val target = fps.toInt() ?: src.toInt()
        return target.coerceAtMost(src.toInt().coerceAtLeast(1))
    }

    private fun effectiveResolution(source: ProbeResult, target: ResolutionPreset?): ResolutionPreset? {
        target ?: return null
        return if (target.shortEdgePx >= minOf(source.widthPx, source.heightPx)) null else target
    }
}

private fun FpsChoice.toInt(): Int? = when (this) {
    FpsChoice.KEEP -> null
    FpsChoice.FPS_24 -> 24
    FpsChoice.FPS_30 -> 30
    FpsChoice.FPS_60 -> 60
}
```

- [ ] **Step 2: Build.** `./gradlew :app:assembleDebug`.

- [ ] **Step 3: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/core/estimator/
git commit -m "feat(estimator): output size estimator with codec/CRF/FPS factors"
```

---

### Task C5: ScopedTempDir + MediaStoreSaver

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/storage/ScopedTempDir.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/storage/MediaStoreSaver.kt`

- [ ] **Step 1: `ScopedTempDir.kt`:**

```kotlin
package com.videoeditor.core.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScopedTempDir @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun workingDir(workId: String): File =
        File(ctx.cacheDir, "compress/$workId").apply { mkdirs() }

    /** Copies a content:// or file:// URI into a private cache file FFmpeg can read. */
    fun copyToCache(uri: Uri, dir: File, fileName: String): File {
        val out = File(dir, fileName)
        ctx.contentResolver.openInputStream(uri)!!.use { input ->
            out.outputStream().use { output -> input.copyTo(output, bufferSize = 1 shl 20) }
        }
        return out
    }

    fun cleanup(workId: String) {
        File(ctx.cacheDir, "compress/$workId").deleteRecursively()
    }
}
```

- [ ] **Step 2: `MediaStoreSaver.kt`:**

```kotlin
package com.videoeditor.core.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreSaver @Inject constructor(@ApplicationContext private val ctx: Context) {

    /** Inserts the file into MediaStore.Video and copies bytes. Returns the inserted Uri. */
    fun saveToMovies(file: File, displayName: String): Uri {
        val resolver = ctx.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VideoEditor")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values)
            ?: error("MediaStore.insert returned null")

        resolver.openOutputStream(uri)!!.use { out ->
            file.inputStream().use { it.copyTo(out, bufferSize = 1 shl 20) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }
}
```

- [ ] **Step 3: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/core/storage/
git commit -m "feat(storage): scoped temp dir + MediaStore saver (Movies/VideoEditor)"
```

---

### Task C6: ProgressParser + FFmpegRunner

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/ffmpeg/ProgressParser.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/ffmpeg/FFmpegRunner.kt`

- [ ] **Step 1: `ProgressParser.kt`:**

```kotlin
package com.videoeditor.core.ffmpeg

import com.videoeditor.feature.compress.model.EncodeProgress

/** Stateful parser fed FFmpeg `-progress pipe:1` log lines. */
class ProgressParser(private val durationMs: Long) {
    private var outTimeUs: Long = 0
    private var frame: Long = 0
    private var fps: Double = 0.0
    private var bitrateKbps: Int? = null

    /** Returns updated EncodeProgress when a `progress=continue|end` block closes; else null. */
    fun feed(line: String): EncodeProgress? {
        val trimmed = line.trim()
        val eq = trimmed.indexOf('=')
        if (eq <= 0) return null
        val key = trimmed.substring(0, eq)
        val value = trimmed.substring(eq + 1)
        when (key) {
            "out_time_us" -> outTimeUs = value.toLongOrNull() ?: outTimeUs
            "frame" -> frame = value.toLongOrNull() ?: frame
            "fps" -> fps = value.toDoubleOrNull() ?: fps
            "bitrate" -> bitrateKbps = value.removeSuffix("kbits/s").trim().toDoubleOrNull()?.toInt()
            "progress" -> {
                val percent = if (durationMs > 0) (outTimeUs / 1000.0 / durationMs).toFloat().coerceIn(0f, 1f) else 0f
                val eta = if (fps > 0) {
                    val totalFrames = (durationMs / 1000.0 * 30.0).toLong()  // best-effort if we don't know src fps
                    ((totalFrames - frame) / fps).toLong().coerceAtLeast(0)
                } else null
                return EncodeProgress(percent, frame, fps, eta, bitrateKbps)
            }
        }
        return null
    }
}
```

- [ ] **Step 2: `FFmpegRunner.kt`:**

```kotlin
package com.videoeditor.core.ffmpeg

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.videoeditor.feature.compress.model.EncodeProgress
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FFmpegRunner @Inject constructor() {

    data class RunResult(val success: Boolean, val failureReason: String?)

    /**
     * Runs FFmpeg async; emits progress to [onProgress] (called on FFmpegKit threads).
     * Cancels via [FFmpegKit.cancel] when the coroutine is cancelled.
     */
    suspend fun run(
        args: Array<String>,
        durationMs: Long,
        onProgress: (EncodeProgress) -> Unit,
    ): RunResult = suspendCancellableCoroutine { cont ->
        val parser = ProgressParser(durationMs)
        val logCallback = LogCallback { log ->
            log.message?.lineSequence()?.forEach { line ->
                parser.feed(line)?.let(onProgress)
            }
        }
        val session: FFmpegSession = FFmpegKit.executeAsync(
            args,
            { session ->
                val rc = session.returnCode
                when {
                    ReturnCode.isSuccess(rc) -> cont.resume(RunResult(true, null))
                    ReturnCode.isCancel(rc) -> cont.resume(RunResult(false, "Cancelled"))
                    else -> cont.resume(RunResult(false, session.failStackTrace ?: session.output ?: "Unknown error"))
                }
            },
            logCallback,
            null,  // statisticsCallback unused (-progress pipe used instead)
        )
        cont.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
    }
}
```

- [ ] **Step 3: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/core/ffmpeg/ProgressParser.kt \
        app/src/main/kotlin/com/videoeditor/core/ffmpeg/FFmpegRunner.kt
git commit -m "feat(ffmpeg): progress parser + async runner with cancellation"
```

---

### Task C7: CompressWorker + ForegroundInfo + Launcher

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/work/CompressForegroundInfo.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/work/CompressWorker.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/work/CompressWorkLauncher.kt`

- [ ] **Step 1: `CompressForegroundInfo.kt`:**

```kotlin
package com.videoeditor.feature.compress.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo

object CompressForegroundInfo {
    private const val CHANNEL_ID = "compress_progress"
    private const val NOTIFICATION_ID = 0xC0 // any positive int

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Compression progress", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun build(ctx: Context, percent: Int, title: String): ForegroundInfo {
        ensureChannel(ctx)
        val notification: Notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$percent%")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setProgress(100, percent, percent <= 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else
            ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 2: `CompressWorker.kt`:**

```kotlin
package com.videoeditor.feature.compress.work

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.videoeditor.core.ffmpeg.FFmpegCommandBuilder
import com.videoeditor.core.ffmpeg.FFmpegRunner
import com.videoeditor.core.probe.VideoProbe
import com.videoeditor.core.storage.MediaStoreSaver
import com.videoeditor.core.storage.ScopedTempDir
import com.videoeditor.feature.compress.model.CompressionSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

@HiltWorker
class CompressWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val probe: VideoProbe,
    private val builder: FFmpegCommandBuilder,
    private val runner: FFmpegRunner,
    private val temp: ScopedTempDir,
    private val saver: MediaStoreSaver,
) : CoroutineWorker(appContext, params) {

    override suspend fun getForegroundInfo() =
        CompressForegroundInfo.build(applicationContext, percent = 0, title = "Compressing video…")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val inputUri = inputData.getString(KEY_URI)?.let(Uri::parse) ?: return@withContext Result.failure()
        val settingsJson = inputData.getString(KEY_SETTINGS) ?: return@withContext Result.failure()
        val settings = Json.decodeFromString<CompressionSettings>(settingsJson)
        val source = probe.probe(inputUri)

        val workDir = temp.workingDir(id.toString())
        val inputCache = temp.copyToCache(inputUri, workDir, "in.${source.displayName.substringAfterLast('.', "mp4")}")
        val outputFile = File(workDir, "out.mp4")

        setForeground(CompressForegroundInfo.build(applicationContext, 0, "Compressing video…"))

        // Try HW first; on failure, retry with software (only meaningful when settings.useHardwareAccel).
        var attempt = if (settings.useHardwareAccel) Attempt.HW else Attempt.SW
        var fallback = false
        var lastError: String? = null
        var success = false

        while (true) {
            val args = builder.build(
                source = source,
                settings = settings,
                input = inputCache.absolutePath,
                output = outputFile.absolutePath,
                useHwEncoder = (attempt == Attempt.HW),
            )
            val result = runner.run(args, source.durationMs) { progress ->
                val pct = (progress.percent * 100).toInt().coerceIn(0, 100)
                setProgressAsync(workDataOf(
                    KEY_PROGRESS to progress.percent,
                    KEY_FRAME to progress.frame,
                    KEY_FPS to progress.fps,
                    KEY_ETA to (progress.etaSeconds ?: -1L),
                    KEY_BITRATE to (progress.bitrateKbps ?: -1),
                ))
                runCatching { setForegroundAsync(CompressForegroundInfo.build(applicationContext, pct, "Compressing video…")) }
            }
            if (result.success) { success = true; break }
            lastError = result.failureReason
            if (attempt == Attempt.HW) {
                attempt = Attempt.SW
                fallback = true
                continue
            }
            break
        }

        if (!success) {
            temp.cleanup(id.toString())
            return@withContext Result.failure(workDataOf(KEY_ERROR to (lastError ?: "Unknown")))
        }

        val outName = "${source.displayName.substringBeforeLast('.', source.displayName)}-compressed.mp4"
        val savedUri = saver.saveToMovies(outputFile, outName)
        val outBytes = outputFile.length()
        temp.cleanup(id.toString())

        Result.success(workDataOf(
            KEY_OUTPUT_URI to savedUri.toString(),
            KEY_OUTPUT_NAME to outName,
            KEY_OUTPUT_SIZE to outBytes,
            KEY_FALLBACK to fallback,
        ))
    }

    private enum class Attempt { HW, SW }

    companion object {
        const val KEY_URI = "uri"
        const val KEY_SETTINGS = "settings_json"
        const val KEY_PROGRESS = "progress"
        const val KEY_FRAME = "frame"
        const val KEY_FPS = "fps"
        const val KEY_ETA = "eta"
        const val KEY_BITRATE = "bitrate"
        const val KEY_ERROR = "error"
        const val KEY_OUTPUT_URI = "out_uri"
        const val KEY_OUTPUT_NAME = "out_name"
        const val KEY_OUTPUT_SIZE = "out_size"
        const val KEY_FALLBACK = "fallback"
    }
}
```

> Note: `CompressionSettings` must be `@Serializable` for the JSON ferry. Add `kotlinx-serialization` plugin + dependency in Task A1's `libs.versions.toml` if you skipped it. Quick add: `id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"` in `app/build.gradle.kts`'s `plugins {}`, plus `implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")`. Annotate `CompressionSettings`, `AudioSettings`, and the enums with `@Serializable`.

- [ ] **Step 3: `CompressWorkLauncher.kt`:**

```kotlin
package com.videoeditor.feature.compress.work

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.videoeditor.feature.compress.model.CompressionSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompressWorkLauncher @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun enqueue(uri: Uri, settings: CompressionSettings): UUID {
        val request = OneTimeWorkRequestBuilder<CompressWorker>()
            .setInputData(workDataOf(
                CompressWorker.KEY_URI to uri.toString(),
                CompressWorker.KEY_SETTINGS to Json.encodeToString(settings),
            ))
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .addTag(TAG)
            .build()
        WorkManager.getInstance(ctx).enqueue(request)
        return request.id
    }

    fun observe(workId: UUID): Flow<WorkInfo?> =
        WorkManager.getInstance(ctx).getWorkInfoByIdFlow(workId)

    fun cancel(workId: UUID) {
        WorkManager.getInstance(ctx).cancelWorkById(workId)
    }

    companion object { const val TAG = "compress" }
}
```

- [ ] **Step 4: Add `kotlinx-serialization` plugin & dep** to `app/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins
    kotlin("plugin.serialization") version "2.1.0"
}
dependencies {
    // ... existing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
```

And annotate the model file (modify `app/src/main/kotlin/com/videoeditor/feature/compress/model/CompressionSettings.kt`): add `import kotlinx.serialization.Serializable` and `@Serializable` on `CompressionSettings`, `AudioSettings`, and each enum (`@Serializable enum class VideoCodec ...`).

- [ ] **Step 5: Build.** `./gradlew :app:assembleDebug`. Expected: success.

- [ ] **Step 6: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/feature/compress/work/ \
        app/src/main/kotlin/com/videoeditor/feature/compress/model/CompressionSettings.kt \
        app/build.gradle.kts
git commit -m "feat(compress): WorkManager worker with HW→SW fallback + foreground notif"
```

---

### Task C8: CompressViewModel

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/CompressViewModel.kt`

- [ ] **Step 1: `CompressViewModel.kt`:**

```kotlin
package com.videoeditor.feature.compress

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.videoeditor.core.estimator.OutputSizeEstimator
import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.core.probe.VideoProbe
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.EncodeProgress
import com.videoeditor.feature.compress.model.SavedOutput
import com.videoeditor.feature.compress.model.SectionId
import com.videoeditor.feature.compress.model.SmartPreset
import com.videoeditor.feature.compress.work.CompressWorkLauncher
import com.videoeditor.feature.compress.work.CompressWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CompressViewModel @Inject constructor(
    private val probe: VideoProbe,
    private val estimator: OutputSizeEstimator,
    private val launcher: CompressWorkLauncher,
) : ViewModel() {

    private val _state = MutableStateFlow<CompressUiState>(CompressUiState.Idle)
    val state: StateFlow<CompressUiState> = _state.asStateFlow()

    fun onPickClicked() { _state.value = CompressUiState.PickingVideo }
    fun onPickCancelled() { _state.value = CompressUiState.Idle }

    fun onVideoPicked(uri: Uri) = viewModelScope.launch {
        _state.value = CompressUiState.PickingVideo
        runCatching { probe.probe(uri) }
            .onSuccess { res ->
                val settings = CompressionSettings()
                _state.value = CompressUiState.Configuring(
                    source = res,
                    settings = settings,
                    activeSmartPreset = SmartPreset.BALANCED,
                    estimate = estimator.estimate(res, settings),
                )
            }
            .onFailure { _state.value = CompressUiState.Failed(source = null, reason = it.message ?: "Probe failed") }
    }

    fun onSmartPresetChosen(preset: SmartPreset) = updateConfiguring { cfg ->
        val newSettings = preset.apply(cfg.source)
        cfg.copy(settings = newSettings, activeSmartPreset = preset, estimate = estimator.estimate(cfg.source, newSettings))
    }

    fun onSettingsChanged(transform: (CompressionSettings) -> CompressionSettings) = updateConfiguring { cfg ->
        val s = transform(cfg.settings)
        cfg.copy(settings = s, activeSmartPreset = null, estimate = estimator.estimate(cfg.source, s))
    }

    fun toggleSection(id: SectionId) = updateConfiguring { cfg ->
        val expanded = if (id in cfg.expandedSections) cfg.expandedSections - id else cfg.expandedSections + id
        cfg.copy(expandedSections = expanded)
    }

    fun onCompressClicked() {
        val cfg = _state.value as? CompressUiState.Configuring ?: return
        val workId = launcher.enqueue(cfg.source.uri, cfg.settings)
        _state.value = CompressUiState.Running(cfg.source, workId, EncodeProgress())
        observeWork(workId, cfg.source)
    }

    fun onCancelClicked() {
        val running = _state.value as? CompressUiState.Running ?: return
        launcher.cancel(running.workId)
    }

    fun onDoneAcknowledged() { _state.value = CompressUiState.Idle }

    private fun observeWork(id: UUID, source: ProbeResult) = viewModelScope.launch {
        launcher.observe(id).collect { info ->
            if (info == null) return@collect
            val cur = _state.value
            when (info.state) {
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                    val p = info.progress
                    val progress = EncodeProgress(
                        percent = p.getFloat(CompressWorker.KEY_PROGRESS, 0f),
                        frame = p.getLong(CompressWorker.KEY_FRAME, 0L),
                        fps = p.getDouble(CompressWorker.KEY_FPS, 0.0),
                        etaSeconds = p.getLong(CompressWorker.KEY_ETA, -1L).takeIf { it >= 0 },
                        bitrateKbps = p.getInt(CompressWorker.KEY_BITRATE, -1).takeIf { it >= 0 },
                    )
                    if (cur is CompressUiState.Running) _state.update { cur.copy(progress = progress) }
                }
                WorkInfo.State.SUCCEEDED -> {
                    val out = SavedOutput(
                        uri = Uri.parse(info.outputData.getString(CompressWorker.KEY_OUTPUT_URI) ?: return@collect),
                        displayName = info.outputData.getString(CompressWorker.KEY_OUTPUT_NAME).orEmpty(),
                        sizeBytes = info.outputData.getLong(CompressWorker.KEY_OUTPUT_SIZE, 0L),
                        usedHardwareFallback = info.outputData.getBoolean(CompressWorker.KEY_FALLBACK, false),
                    )
                    val ratio = if (source.sizeBytes > 0) out.sizeBytes.toDouble() / source.sizeBytes else 0.0
                    _state.value = CompressUiState.Done(source, out, ratio)
                }
                WorkInfo.State.FAILED -> {
                    val err = info.outputData.getString(CompressWorker.KEY_ERROR) ?: "Compression failed"
                    _state.value = CompressUiState.Failed(source, err)
                }
                WorkInfo.State.CANCELLED -> _state.value = CompressUiState.Configuring(
                    source = source,
                    settings = (cur as? CompressUiState.Running)?.let { CompressionSettings() } ?: CompressionSettings(),
                    activeSmartPreset = SmartPreset.BALANCED,
                    estimate = estimator.estimate(source, CompressionSettings()),
                )
                else -> Unit
            }
        }
    }

    private inline fun updateConfiguring(block: (CompressUiState.Configuring) -> CompressUiState.Configuring) {
        _state.update { cur ->
            if (cur is CompressUiState.Configuring) block(cur) else cur
        }
    }
}
```

- [ ] **Step 2: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/feature/compress/CompressViewModel.kt
git commit -m "feat(compress): ViewModel orchestrating pick → configure → run → done"
```

---

### Task C9: Compress UI — pick step + container `CompressScreen`

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/feature/compress/CompressScreen.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/PickStep.kt`

- [ ] **Step 1: `ui/PickStep.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PickStep(
    autoOpen: Boolean,
    onPicked: (android.net.Uri) -> Unit,
    onCancelled: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) onCancelled() else onPicked(uri)
    }

    LaunchedEffect(autoOpen) {
        if (autoOpen) launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalButton(onClick = {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }) { Text("Pick a video") }
        Spacer(Modifier.height(12.dp))
        Text("MP4, MOV, MKV…", style = MaterialTheme.typography.bodyMedium)
    }
}
```

- [ ] **Step 2: Replace stub `CompressScreen.kt`:**

```kotlin
package com.videoeditor.feature.compress

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.ui.ConfigureStep
import com.videoeditor.feature.compress.ui.DoneStep
import com.videoeditor.feature.compress.ui.PickStep
import com.videoeditor.feature.compress.ui.RunStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(
    onBack: () -> Unit,
    vm: CompressViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(when (state) {
                    is CompressUiState.Idle, is CompressUiState.PickingVideo -> "Compress · Step 1/3"
                    is CompressUiState.Configuring -> "Compress · Step 2/3"
                    is CompressUiState.Running, is CompressUiState.Done, is CompressUiState.Failed -> "Compress · Step 3/3"
                }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            CompressUiState.Idle, CompressUiState.PickingVideo ->
                PickStep(
                    autoOpen = s == CompressUiState.PickingVideo,
                    onPicked = vm::onVideoPicked,
                    onCancelled = vm::onPickCancelled,
                    modifier = Modifier.padding(padding),
                )
            is CompressUiState.Configuring -> ConfigureStep(
                state = s,
                onSmartPreset = vm::onSmartPresetChosen,
                onSettings = vm::onSettingsChanged,
                onToggleSection = vm::toggleSection,
                onContinue = vm::onCompressClicked,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            is CompressUiState.Running -> RunStep(state = s, onCancel = vm::onCancelClicked, modifier = Modifier.fillMaxSize().padding(padding))
            is CompressUiState.Done -> DoneStep(state = s, onDone = vm::onDoneAcknowledged, modifier = Modifier.fillMaxSize().padding(padding))
            is CompressUiState.Failed -> Text(s.reason, modifier = Modifier.padding(padding))
        }
    }
}
```

> Note: `PickStep` above doesn't currently take a `modifier` — extend its signature with `modifier: Modifier = Modifier` and pass through to the root `Column`.

- [ ] **Step 3: Build.** `./gradlew :app:assembleDebug`. Expected: compile errors only for `ConfigureStep`, `RunStep`, `DoneStep` — covered by the next tasks. Comment them out temporarily if you want to install and verify the picker works in isolation, then restore.

- [ ] **Step 4: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/feature/compress/CompressScreen.kt \
        app/src/main/kotlin/com/videoeditor/feature/compress/ui/PickStep.kt
git commit -m "feat(compress): pick step + screen scaffold"
```

---

### Task C10: Configure step UI

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/ConfigureStep.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/SmartPresetChips.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/VideoSection.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/AudioSection.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/AdvancedSection.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/OutputPreviewBar.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/LabeledRow.kt`
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/ChipsRow.kt`

- [ ] **Step 1: `core/designsys/LabeledRow.kt`:**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}
```

- [ ] **Step 2: `core/designsys/ChipsRow.kt`:**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> ChipsRow(
    items: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(label(item)) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}
```

- [ ] **Step 3: `ui/SmartPresetChips.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.videoeditor.core.designsys.ChipsRow
import com.videoeditor.feature.compress.model.SmartPreset

@Composable
fun SmartPresetChips(active: SmartPreset?, onSelect: (SmartPreset) -> Unit, modifier: Modifier = Modifier) {
    ChipsRow(
        items = SmartPreset.entries,
        selected = active,
        label = { it.label },
        onSelect = onSelect,
        modifier = modifier,
    )
}
```

- [ ] **Step 4: `ui/VideoSection.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.ChipsRow
import com.videoeditor.core.designsys.LabeledRow
import com.videoeditor.core.designsys.SectionCard
import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.EncodingPreset
import com.videoeditor.feature.compress.model.FpsChoice
import com.videoeditor.feature.compress.model.RateControl
import com.videoeditor.feature.compress.model.ResolutionPreset
import com.videoeditor.feature.compress.model.VideoCodec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSection(
    source: ProbeResult,
    settings: CompressionSettings,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = "Video", modifier = modifier) {
        LabeledRow("Codec") {
            DropdownPicker(
                options = VideoCodec.entries,
                selected = settings.codec,
                label = { it.name.replace("H", "H.") },
                onSelected = { onChange(settings.copy(codec = it)) },
            )
        }
        LabeledRow("Mode") {
            ChipsRow(
                items = RateControl.entries,
                selected = settings.rateControl,
                label = { it.name },
                onSelect = { onChange(settings.copy(rateControl = it)) },
            )
        }
        when (settings.rateControl) {
            RateControl.CRF -> {
                LabeledRow("CRF") { Text(settings.crf.toString()) }
                Slider(
                    value = settings.crf.toFloat(),
                    onValueChange = { onChange(settings.copy(crf = it.toInt())) },
                    valueRange = 0f..51f,
                    steps = 50,
                )
            }
            RateControl.CBR, RateControl.VBR -> {
                LabeledRow("Bitrate") { Text("${settings.targetBitrateKbps} kbps") }
                Slider(
                    value = settings.targetBitrateKbps.toFloat(),
                    onValueChange = { onChange(settings.copy(targetBitrateKbps = it.toInt())) },
                    valueRange = 200f..50_000f,
                    steps = 99,
                )
            }
        }

        val maxRes = ResolutionPreset.entries.lastOrNull { it.shortEdgePx <= minOf(source.widthPx, source.heightPx) }
        LabeledRow("Resolution") {
            DropdownPicker(
                options = listOf<ResolutionPreset?>(null) + ResolutionPreset.entries.filter {
                    maxRes != null && it.shortEdgePx <= maxRes.shortEdgePx
                },
                selected = settings.resolution,
                label = { it?.label ?: "Keep (${minOf(source.widthPx, source.heightPx)}p)" },
                onSelected = { onChange(settings.copy(resolution = it)) },
            )
        }
        LabeledRow("FPS") {
            val srcFpsInt = source.frameRate.let { if (it.isNaN() || it <= 0) 30 else it.toInt() }
            val choices = listOf(FpsChoice.KEEP) +
                listOf(FpsChoice.FPS_24, FpsChoice.FPS_30, FpsChoice.FPS_60).filter { it.toIntOr(0) <= srcFpsInt }
            DropdownPicker(
                options = choices,
                selected = settings.fps,
                label = { fpsLabel(it, srcFpsInt) },
                onSelected = { onChange(settings.copy(fps = it)) },
            )
        }
        LabeledRow("Preset") {
            DropdownPicker(
                options = EncodingPreset.entries,
                selected = settings.preset,
                label = { it.name.lowercase() },
                onSelected = { onChange(settings.copy(preset = it)) },
            )
        }
    }
}

private fun fpsLabel(c: FpsChoice, srcFpsInt: Int) = when (c) {
    FpsChoice.KEEP -> "Keep ($srcFpsInt)"
    FpsChoice.FPS_24 -> "24"
    FpsChoice.FPS_30 -> "30"
    FpsChoice.FPS_60 -> "60"
}

private fun FpsChoice.toIntOr(default: Int) = when (this) {
    FpsChoice.KEEP -> default
    FpsChoice.FPS_24 -> 24
    FpsChoice.FPS_30 -> 30
    FpsChoice.FPS_60 -> 60
}

@Composable
fun <T> DropdownPicker(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row {
        TextButton(onClick = { expanded = true }) {
            Text(label(selected))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(label(opt)) }, onClick = {
                    onSelected(opt)
                    expanded = false
                })
            }
        }
    }
}
```

- [ ] **Step 5: `ui/AudioSection.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.videoeditor.core.designsys.ChipsRow
import com.videoeditor.core.designsys.LabeledRow
import com.videoeditor.core.designsys.SectionCard
import com.videoeditor.feature.compress.model.AudioChannels
import com.videoeditor.feature.compress.model.CompressionSettings

@Composable
fun AudioSection(
    settings: CompressionSettings,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = "Audio", modifier = modifier) {
        LabeledRow("Codec") { Text("AAC") }
        LabeledRow("Bitrate") {
            DropdownPicker(
                options = listOf(64, 96, 128, 192, 256),
                selected = settings.audio.bitrateKbps,
                label = { "${it} kbps" },
                onSelected = { onChange(settings.copy(audio = settings.audio.copy(bitrateKbps = it))) },
            )
        }
        LabeledRow("Channels") {
            ChipsRow(
                items = AudioChannels.entries,
                selected = settings.audio.channels,
                label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                onSelect = { onChange(settings.copy(audio = settings.audio.copy(channels = it))) },
            )
        }
    }
}
```

- [ ] **Step 6: `ui/AdvancedSection.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.videoeditor.core.designsys.LabeledRow
import com.videoeditor.core.designsys.SectionCard
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.H264Profile

@Composable
fun AdvancedSection(
    settings: CompressionSettings,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(title = "Advanced", modifier = modifier) {
        LabeledRow("Hardware encoder") {
            Switch(checked = settings.useHardwareAccel, onCheckedChange = { onChange(settings.copy(useHardwareAccel = it)) })
        }
        LabeledRow("Keyframe every") { Text("${settings.gopSeconds}s") }
        Slider(
            value = settings.gopSeconds.toFloat(),
            onValueChange = { onChange(settings.copy(gopSeconds = it.toInt().coerceAtLeast(1))) },
            valueRange = 1f..10f, steps = 8,
        )
        LabeledRow("H.264 profile") {
            DropdownPicker(
                options = H264Profile.entries,
                selected = settings.profile,
                label = { it.name.lowercase() },
                onSelected = { onChange(settings.copy(profile = it)) },
            )
        }
    }
}
```

- [ ] **Step 7: `ui/OutputPreviewBar.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.videoeditor.feature.compress.model.OutputEstimate

@Composable
fun OutputPreviewBar(estimate: OutputEstimate, sourceSizeBytes: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Estimated output: ${formatBytes(estimate.sizeBytes)} (×${"%.2f".format(estimate.ratio)})",
                 style = MaterialTheme.typography.titleMedium)
            Text("Source: ${formatBytes(sourceSizeBytes)} · ~${estimate.effectiveBitrateKbps} kbps total",
                 style = MaterialTheme.typography.bodyMedium)
            estimate.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

private fun formatBytes(b: Long): String = when {
    b >= 1_000_000_000 -> "%.2f GB".format(b / 1_000_000_000.0)
    b >= 1_000_000 -> "%.1f MB".format(b / 1_000_000.0)
    b >= 1_000 -> "%.0f KB".format(b / 1_000.0)
    else -> "$b B"
}
```

- [ ] **Step 8: `ui/ConfigureStep.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.SectionId
import com.videoeditor.feature.compress.model.SmartPreset

@Composable
fun ConfigureStep(
    state: CompressUiState.Configuring,
    onSmartPreset: (SmartPreset) -> Unit,
    onSettings: ((CompressionSettings) -> CompressionSettings) -> Unit,
    onToggleSection: (SectionId) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${state.source.displayName}",
             style = MaterialTheme.typography.titleMedium)
        Text("${state.source.widthPx}×${state.source.heightPx} · " +
             "${"%.0f".format(if (state.source.frameRate.isNaN()) 0.0 else state.source.frameRate)} fps · " +
             "${state.source.videoCodec} · ${state.source.videoBitrateBps / 1000} kbps",
             style = MaterialTheme.typography.bodyMedium)

        SmartPresetChips(active = state.activeSmartPreset, onSelect = onSmartPreset)

        VideoSection(state.source, state.settings, onChange = { newS -> onSettings { newS } })
        AudioSection(state.settings, onChange = { newS -> onSettings { newS } })
        AdvancedSection(state.settings, onChange = { newS -> onSettings { newS } })

        OutputPreviewBar(state.estimate, state.source.sizeBytes)

        Button(onClick = onContinue, modifier = Modifier.padding(top = 8.dp)) { Text("Compress") }
        Spacer(Modifier.height(16.dp))
    }
}
```

- [ ] **Step 9: Build.** `./gradlew :app:assembleDebug`. Expected: success (RunStep / DoneStep are still missing — temporarily comment out their references in `CompressScreen.kt` if you want to install now).

- [ ] **Step 10: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/feature/compress/ui/ \
        app/src/main/kotlin/com/videoeditor/core/designsys/
git commit -m "feat(compress): configure step UI — sections, sliders, smart presets"
```

---

### Task C11: Run + Done steps

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/RunStep.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/DoneStep.kt`

- [ ] **Step 1: `ui/RunStep.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.feature.compress.model.CompressUiState

@Composable
fun RunStep(state: CompressUiState.Running, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Compressing ${state.source.displayName}…", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { state.progress.percent }, modifier = Modifier.fillMaxWidth())
        Text("${(state.progress.percent * 100).toInt()}%  " +
             "${state.progress.etaSeconds?.let { "ETA ${formatEta(it)}" } ?: ""}  " +
             "frame ${state.progress.frame}  " +
             "${"%.0f".format(state.progress.fps)} fps",
             style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

private fun formatEta(s: Long): String {
    val m = s / 60
    val sec = s % 60
    return "%d:%02d".format(m, sec)
}
```

> Note: pause is not implemented in MVP — FFmpeg/MediaCodec doesn't support true pause-and-resume of an in-progress encode without using segment-based encoding. We render only Cancel.

- [ ] **Step 2: `ui/DoneStep.kt`:**

```kotlin
package com.videoeditor.feature.compress.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.videoeditor.feature.compress.model.CompressUiState

@Composable
fun DoneStep(state: CompressUiState.Done, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("✓ Done", style = MaterialTheme.typography.headlineSmall)
        Text("${state.output.displayName}", style = MaterialTheme.typography.titleMedium)
        Text("Size: ${formatBytes(state.output.sizeBytes)} (was ${formatBytes(state.source.sizeBytes)} · ×${"%.2f".format(state.ratio)})")
        if (state.output.usedHardwareFallback) Text("Note: hardware encoder unsupported, used software fallback.",
            style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val viewIntent = Intent(Intent.ACTION_VIEW, state.output.uri).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(state.output.uri, "video/mp4")
                }
                ctx.startActivity(viewIntent)
            }) { Text("Open") }
            OutlinedButton(onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, state.output.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(share, "Share video"))
            }) { Text("Share") }
            Button(onClick = onDone) { Text("Done") }
        }
    }
}

private fun formatBytes(b: Long): String = when {
    b >= 1_000_000_000 -> "%.2f GB".format(b / 1_000_000_000.0)
    b >= 1_000_000 -> "%.1f MB".format(b / 1_000_000.0)
    b >= 1_000 -> "%.0f KB".format(b / 1_000.0)
    else -> "$b B"
}
```

- [ ] **Step 3: Build + install.** `./gradlew :app:installDebug`. Expected: full app installs.

- [ ] **Step 4: Manual verification matrix:**
  1. Launch app → Home grid renders, only Compress is active.
  2. Tap Compress → picker opens → choose a 10s, ~10MB phone-camera mp4.
  3. Configure step shows source metadata (resolution, fps, codec, bitrate, size).
  4. Tap "Small size" smart preset → settings update, estimate drops, ratio < 1.0.
  5. Tap Compress → progress bar moves, ETA counts down, foreground notification appears with percent.
  6. Background the app, lock the screen → notification still ticks; come back → progress reflects current state.
  7. Tap Cancel → returns to configure step within ~1 sec.
  8. Run again to completion → Done screen appears with new size + ratio.
  9. Tap Open → native video player launches the compressed file from `Movies/VideoEditor`.
  10. Open Photos app → file is visible under VideoEditor album.
  11. Force-quit app, reopen Photos, verify compressed file is still there and plays.
  12. Try a 4K source → builder should pick H.265 if you choose it; downscale to 1080p works; output ratio < 0.5.
  13. Switch to Hardware off → encoding slower, file size more predictable for CRF.

- [ ] **Step 5: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/feature/compress/ui/RunStep.kt \
        app/src/main/kotlin/com/videoeditor/feature/compress/ui/DoneStep.kt
git commit -m "feat(compress): run + done steps with cancel/open/share"
```

---

## Phase D — Polish

### Task D1: Splash + edge-to-edge insets

**Files:**
- Create: `app/src/main/res/values/styles_splash.xml`
- Modify: `app/src/main/AndroidManifest.xml` (point launcher activity at splash theme)

- [ ] **Step 1: Add SplashScreen API**

In `app/build.gradle.kts` dependencies:

```kotlin
implementation("androidx.core:core-splashscreen:1.0.1")
```

Create `app/src/main/res/values/styles_splash.xml`:

```xml
<resources>
    <style name="Theme.VideoEditor.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@android:color/black</item>
        <item name="postSplashScreenTheme">@style/Theme.VideoEditor</item>
    </style>
</resources>
```

In `AndroidManifest.xml`, change the launcher activity's `android:theme` to `@style/Theme.VideoEditor.Splash`.

In `MainActivity.onCreate`:

```kotlin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
// ...
override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    enableEdgeToEdge()
    super.onCreate(...)
    // ...
}
```

- [ ] **Step 2: Verify** dark mode by toggling system theme — Home + Compress screens should both look right.

- [ ] **Step 3: Commit.**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml \
        app/src/main/res/values/styles_splash.xml \
        app/src/main/kotlin/com/videoeditor/MainActivity.kt
git commit -m "feat(ui): splash screen + edge-to-edge"
```

---

### Task D2: Dropdown polish + animation specs

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/theme/Motion.kt`
- Modify: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/ConfigureStep.kt` (add animated visibility for sections, optional)

- [ ] **Step 1: `core/theme/Motion.kt`:**

```kotlin
package com.videoeditor.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    fun fast() = tween<Float>(durationMillis = 200, easing = Emphasized)
    fun medium() = tween<Float>(durationMillis = 320, easing = Emphasized)
}
```

- [ ] **Step 2: Optional** — wrap each `SectionCard` in `AnimatedVisibility` toggled by `expandedSections` (state already plumbed via `vm.toggleSection`). For MVP this is purely cosmetic; skip if pressed for time.

- [ ] **Step 3: Commit.**

```bash
git add app/src/main/kotlin/com/videoeditor/core/theme/Motion.kt
git commit -m "feat(theme): shared motion specs"
```

---

## Future-Scalability Hooks (no code, design notes)

The following are deliberately *not* implemented but are baked into the structure so adding them is incremental:

- **Trim / Convert / Resize / Extract Audio:** create `feature/trim/`, `feature/convert/`, etc. Add `Routes.TRIM`, register a composable in `AppNavHost`, and flip `route` from `null` to the new route in `FeatureRegistry`. Each can reuse `FFmpegRunner`, `ScopedTempDir`, and `MediaStoreSaver`. New feature = `~250 LOC`.
- **Filters & effects:** `FFmpegCommandBuilder` already has a filter-graph slot (the `-vf` insertion point). Add a `FilterChain` data class and emit `-filter_complex` instead.
- **AI enhancements:** these will likely require a separate model module; the foreground worker pattern (`CompressWorker` → `EnhanceWorker`) generalises cleanly.
- **Screen recording:** a `MediaProjection` capture can pipe into a new `ScreenRecordingWorker` that reuses `ScopedTempDir` + `MediaStoreSaver`.

---

## Performance & QA Checklist

After C11 you should be able to run all of these from a debug build:

- [ ] **Cold start** under 1 sec on a Pixel 5 / equivalent.
- [ ] **Probe** of a 200 MB / 4K video completes within 1 sec.
- [ ] **HW encode** of 1080p 60 sec source in CRF mode ≤ 8 sec on Pixel 5.
- [ ] **SW encode** same source ≤ 90 sec on Pixel 5.
- [ ] Encoded file plays in Photos, has correct duration ± 0.1 sec, target resolution, and metadata duration.
- [ ] No `ANR` warnings in `adb logcat -s ANR`.
- [ ] Memory peak (Android Studio Profiler) under 150 MB for a 4K transcode.
- [ ] Cancel during HW encode → process stops within 1 sec, partial output not visible in Photos, cache dir empty.
- [ ] Background app during 4K encode → notification persists, encode completes, file appears in Photos.

---

## Self-Review Notes

- **Spec coverage:** every input/control in the brief maps to a model field and a UI element (Codec → `VideoCodec` + `VideoSection`; Bitrate → `targetBitrateKbps` + slider; FPS → `FpsChoice` + dropdown clamped to source; Resolution → `ResolutionPreset` + dropdown clamped to source; Audio codec/bitrate/channels → `AudioSettings` + `AudioSection`; Encoding preset → `EncodingPreset` + dropdown; CRF/VBR/CBR → `RateControl` + chips + slider; GOP/Profile/Level → `AdvancedSection`; Smart presets → `SmartPreset` chips; Output preview → `OutputPreviewBar`; Background + progress + ETA + cancel → `CompressWorker` + `RunStep`; Save to gallery → `MediaStoreSaver`).
- **Tests omitted intentionally** per project policy.
- **Pause/resume** is documented as out-of-scope (would require segment-based encoding; not realistic in MVP scope).
- **Before/After comparison** is delivered as the size + ratio readout in `OutputPreviewBar` and `DoneStep`. A side-by-side video preview would be a follow-up feature; the data model is ready.
- **HW fallback** is implemented in the worker, surfaced in `Done` UI as a note.
- **Output location picker:** delivered as Photos-gallery save (Movies/VideoEditor folder) as the brief's "Download to Photo Gallery" path. A SAF location picker could be added by switching `MediaStoreSaver` to `ActivityResultContracts.CreateDocument`.
