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
| Video processing | FFmpegKit (`com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.1.1`) |
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
│   ├── probe/                   # VideoProbe — metadata extraction
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

### FFmpeg Command Example (Balanced preset)
```
ffmpeg -y -hide_banner -loglevel warning -i /cache/in.mp4 \
  -c:v libx264 -preset medium -profile:v high -level 4.1 \
  -crf 23 -g 60 -keyint_min 60 \
  -c:a aac -b:a 128k -ac 2 \
  -movflags +faststart -progress pipe:1 -nostats \
  /cache/out.mp4
```

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

# Debug APK + lint
./gradlew assembleDebug lintDebug

# Install on connected device/emulator
./gradlew installDebug

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

### Coming soon
- Trim feature
- Format conversion
- Audio extraction
- Resolution scaling

## License

TBD