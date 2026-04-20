# Video Editor (Android)

A Material 3 Android video editing app. MVP feature: full-control video compression powered by FFmpegKit.

## Tech Stack

| | |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose BOM 2025.01.00, Material 3 |
| DI | Hilt 2.52 |
| Architecture | MVVM, single-activity, unidirectional data flow |
| Navigation | Navigation Compose 2.8.x |
| Background work | WorkManager 2.10.0 + Foreground Service |
| Video processing | FFmpegKit (`com.antonkarpenko:ffmpeg-kit-full-gpl:2.1.0`) |
| Image loading | Coil 2.7.0 |
| Permissions | Accompanist Permissions |
| Min SDK | 26 (Android 8.0) / Target SDK 35 |

## Architecture

```
MainActivity
  └── NavHost
       ├── HomeScreen          Feature hub (cards from registry)
       └── CompressScreen      Stepper: pick → configure → run
```

### Package structure

```
app/src/main/kotlin/com/videoeditor/
├── App.kt                        # @HiltAndroidApp + WorkManager init
├── MainActivity.kt               # Single-activity Compose host
├── core/
│   ├── theme/                   # M3 color scheme, typography, motion
│   ├── designsys/               # Reusable composables (SectionCard, StepHeader)
│   ├── navigation/               # Routes, FeatureRegistry, AppNavHost
│   ├── probe/                   # VideoProbe — metadata + color info extraction
│   ├── ffmpeg/                  # FFmpegCommandBuilder, FFmpegRunner, ProgressParser
│   ├── estimator/               # OutputSizeEstimator — pre-encoding size prediction
│   ├── storage/                 # MediaStoreSaver, ScopedTempDir
│   └── di/                     # CoreModule (Hilt)
└── feature/
    ├── home/                    # HomeScreen + HomeViewModel
    └── compress/
        ├── model/              # CompressionSettings, SmartPreset, CompressUiState, OutputEstimate
        ├── ui/                 # PickStep, ConfiguringStep, RunningStep, DoneStep
        ├── work/              # CompressWorker, CompressWorkLauncher, ForegroundInfo
        └── di/                # CompressModule (Hilt)
```

## Compression Features

### Smart Presets
| Preset | Codec | Mode | Resolution | FPS | Preset | Use case |
|--------|-------|------|------------|-----|--------|----------|
| Small | H.265 | CRF 30 | max 720p | max 30 | medium | Maximum savings |
| Balanced | H.264 | CRF 23 | keep | keep | medium | Default |
| High quality | H.265 | CRF 20 | keep | keep | slow | Quality-first |
| Social | H.264 | CBR 6 Mbps | max 1080p | max 30 | fast | Sharing online |

### Controls
- **Codec**: H.264 or H.265 (software or hardware accelerated)
- **Rate control**: CRF, CBR, VBR
- **CRF slider**: 0–51 (default 23)
- **Resolution**: keep original or cap at 480p/720p/1080p/1440p/4K/8K
- **FPS**: keep or cap at 24/30/60
- **Encoding preset**: ultrafast → veryslow (software only)
- **Audio**: bitrate 64–256 kbps, stereo or mono

### Hardware Acceleration
Tries `h264_mediacodec` / `hevc_mediacodec` first, falls back to `libx264` / `libx265` on failure. 5–10× faster on supported SoCs.

- Hardware encoders **do not** support `-preset`, `-profile:v`, `-level`, or `-crf`. These flags are software-only and are omitted automatically when HW is active.
- When HW is active, CRF is translated to an estimated bitrate (`-b:v`) using a lookup table.

### Color Range Handling

Phone cameras record in **full range** (pc, 0–255). Most displays and players expect **limited range** (tv, 16–235). Without correction, re-encoded video appears washed out / lighter than the original.

**Detection**: `VideoProbe` uses FFprobeKit to read `color_range` and `color_space` from the video stream metadata. The content URI is accessed via `/proc/self/fd/<fd>` since FFprobeKit does not accept `content://` URIs directly.

**Correction**: When `color_range == FULL` and the content is **not HDR**, the filter chain includes `zscale=rangein=pc:range=tv` (requires `libzimg`, included in the antonkarpenko build).

**HDR exception**: BT.2020 (`bt2020nc` / `bt2020c`) content is left untouched — applying range conversion to HDR breaks tone mapping.

### 10-bit / HDR Video (SW Encoding)

Software encoders (`libx264` HIGH / `libx265` MAIN) only support 8-bit `yuv420p`. 4K HDR content is typically 10-bit (`yuv420p10le`, fmt 62).

**Fix**: `format=yuv420p` is placed **inside the `-vf` filter chain** — not as a standalone `-pix_fmt` flag. This forces FFmpeg to perform the 10-bit → 8-bit conversion inside the filter graph before the encoder sees any frames.

An identity `scale=iw:ih` filter is prepended when no other filter (scale/zscale) exists in the chain, so the filter graph input node can correctly resolve the input colorspace (`bt2020nc`) before the format conversion runs.

**H.265 profile**: `-profile:v` is intentionally **not passed** for H.265. libx265 auto-selects `main` for 8-bit input and `main10` for 10-bit input, avoiding the `Invalid or incompatible profile set: main` error when encoding HDR content.

### Spatial / Secondary Audio Tracks

Some Apple devices (iPhone Spatial Audio) write an `apac` track that FFmpeg cannot decode. Without mitigation, FFmpeg aborts with "Could not find codec parameters".

**Fix**:
- `-ignore_unknown`: skip streams with unrecognised codecs instead of aborting.
- `-map 0:v:0 -map 0:a:0?`: explicitly select only the first video and first audio track, skipping spatial/secondary audio streams.

### FFmpegKit Build Choice

The official FFmpegKit Maven artifacts were deprecated in early 2025. This project uses `com.antonkarpenko:ffmpeg-kit-full-gpl:2.1.0` from Maven Central, which:
- Includes `libx264`, `libx265`, and `libzimg` (needed for `zscale`)
- Is 16 KB page-aligned (required for Android 15)
- Does not link `libavdevice` USB HID symbols that cause `UnsatisfiedLinkError` on older devices

### FFmpeg Command Example (H.264 SW, Balanced, full-range phone video)

```
ffmpeg -y -hide_banner -loglevel warning \
  -ignore_unknown -i /cache/in.mp4 \
  -map 0:v:0 -map 0:a:0? \
  -vf "zscale=rangein=pc:range=tv,scale=iw:ih,format=yuv420p" \
  -c:v libx264 -preset medium \
  -profile:v high -level 4.1 \
  -crf 23 -g 60 -keyint_min 60 \
  -c:a aac -b:a 128k -ac 2 \
  -movflags +faststart \
  /cache/out.mp4
```

### H.264 Profiles and Levels

| Profile | Features | Compatibility |
|---------|----------|---------------|
| BASELINE | No B-frames, no CABAC | Maximum (streaming, old devices) |
| MAIN | B-frames, CABAC | Good (most modern devices) |
| HIGH | All Main features + extended tools | Best quality/size ratio |

Level encodes resolution + frame rate limits (e.g. 4.1 = up to 1080p@30, 5.2 = up to 4K@60).

### GOP (Group of Pictures)

A GOP is the span of frames between two I-frames (keyframes). Smaller GOP = better seek accuracy + larger file. Larger GOP = better compression.

- **I-frame**: fully self-contained (large). Decoder can start here.
- **P-frame**: predicted from previous frame (smaller).
- **B-frame**: predicted from both previous and next frames (smallest, not in BASELINE).

## Build

### Prerequisites
- **JDK 17** (e.g. `brew install openjdk@17`)
- **Android SDK** (API 35 + build-tools 35.0.0)
- **Gradle 8.10** (wrapper included, run via `./gradlew`)

### Setup
```bash
# Accept SDK licenses
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# Install required SDK components
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### Build commands
```bash
# Debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Check and auto-fix formatting
./gradlew spotlessCheck
./gradlew spotlessApply

# Clean
./gradlew clean
```

## Project Status

| Phase | Status |
|-------|--------|
| A: Project Bootstrap | ✅ |
| B: Navigation Shell + Home | ✅ |
| C: FFmpeg Core | ✅ |
| D: Compression Feature | ✅ |
| E: Final Verification | ✅ |

### What's working end-to-end
- Home screen with 5 feature cards (Compress active, others disabled/coming soon)
- Pick video from gallery
- Configure compression settings via smart presets or manual controls
- Real-time estimated output size
- Background encoding with WorkManager + foreground notification
- Save compressed video to Photos › VideoEditor
- Color range preservation (full → limited via zscale)
- 10-bit HDR encoding via SW fallback
- Spatial audio / apac track skipping

### Coming soon
- Trim feature
- Format conversion
- Audio extraction
- Resolution scaling

## License

TBD
