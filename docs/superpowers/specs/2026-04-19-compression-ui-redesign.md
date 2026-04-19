# Video Compression UI — Professional Redesign Spec

**Date:** 2026-04-19
**Status:** Approved for implementation

---

## Goal

Redesign the Compress screen with a professional dark-theme UI similar to HandBrake/Shutter Encoder, using resolution presets (480p, 720p, 1080p...) instead of the original smart-preset chips (Small/Balanced/HQ/Social).

---

## Design Language

**Aesthetic:** Dark, modern professional compressor tool (HandBrake-style). Deep dark backgrounds, subtle card elevations, indigo primary accent, teal secondary accent.

**Colors:**
- Background: `#0D0D0F` (near-black)
- Surface/cards: `#1A1A1F` (elevated dark)
- Primary accent: `IndigoSeed` (`#5B6CFF`)
- Secondary accent: `AccentTeal` (`#14E0C2`)
- Text: white/gray hierarchy
- Slider track: gradient from primary → secondary

**Typography:** M3 default typography, high contrast on dark bg.

**Spacing:** 16dp standard padding, 8dp between elements, 20dp card corner radius.

---

## Screen Layouts

### Step 1 — Pick (`PickStep`)

- Full-bleed dark surface
- Centered large drop-zone with dashed border (subtle)
- Video file icon (80dp)
- "Select video" FilledTonalButton
- "or drag a file here" subtitle text

### Step 2 — Configure (`ConfiguringStep`)

```
┌──────────────────────────────────────────────────────────┐
│  ←  Compress Video                              2 / 3   │
├──────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────┐ │
│  │  ▶  beach.mp4                                       │ │
│  │     1920×1080 · 29.97 fps · H.264 · 86.3 MB · 2:14 │ │
│  └────────────────────────────────────────────────────┘ │
│                                                           │
│  Resolution presets                                        │
│  [480p] [720p] [1080p] [1440p] [4K] [8K]  [(✓) keep orig]│
│                                                           │
│  ▼ Video settings                                         │
│    Codec         [ H.264 ▼ ]                              │
│    Rate control  ( CRF ) ( CBR ) ( VBR )                  │
│    CRF           ━━━━━━━━━━●━━━━━━━━  23                │
│    Bitrate       ━━━━●━━━━━━━━━━━━━━━  2500 kbps  ← CBR/VBR only
│    FPS           [ Keep ▼ ]  (Keep / 24 / 30 / 60)       │
│    Encoding      [ medium ▼ ]                            │
│                   (Ultrafast / Superfast / Veryfast /    │
│                    Fast / Medium / Slow / Veryslow)       │
│    Profile       [ High ▼ ]   (Baseline / Main / High)   │
│    GOP           [ 2 seconds ▼ ]  (1 / 2 / 5 / 10)        │
│                                                           │
│  ▼ Audio settings                                         │
│    Codec         AAC (fixed)                             │
│    Bitrate       [ 128 kbps ▼ ]  (64 / 96 / 128 / 192 / 256)
│    Channels      ( Stereo ) ( Mono )                      │
│                                                           │
│  ▼ Advanced                                               │
│    HW acceleration   [ (●) on  ( ) off ]                  │
│                                                           │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Est. output: ~27.8 MB   (was 86.3 MB, saved ×0.32)│  │
│  │  H.264 · High · 1080p · 30fps · CRF 23             │  │
│  └────────────────────────────────────────────────────┘  │
│                                                           │
│              [         Compress now           ]           │
└──────────────────────────────────────────────────────────┘
```

**Video settings options:**
- Codec: H.264 / H.265 (dropdown)
- Rate control: CRF / CBR / VBR (toggle buttons)
- CRF slider: 0–51, shown only when CRF selected
- Bitrate slider: shown only when CBR or VBR selected
- FPS: Keep / 24 / 30 / 60 (dropdown)
- Encoding preset: Ultrafast → Veryslow (dropdown)
- Profile: Baseline / Main / High (dropdown, H.264 only)
- GOP: 1 / 2 / 5 / 10 seconds (dropdown)

**Audio settings options:**
- Codec: AAC (fixed, not configurable)
- Bitrate: 64 / 96 / 128 / 192 / 256 kbps (dropdown)
- Channels: Stereo / Mono (toggle)

**Advanced:**
- HW acceleration: on/off toggle (tries h264_mediacodec/hevc_mediacodec, falls back to software)

### Step 3 — Running (`RunningStep`)

- Dark themed
- LinearProgressIndicator with indigo track
- Percentage + ETA + frame count + FPS
- Cancel button only

### Step 4 — Done (`DoneStep`)

- Check icon in teal
- Output size with savings badge
- Open / Share / Done actions

---

## Component Inventory

| Component | Description |
|---|---|
| `PickStep` | Dark pick screen with dashed border zone |
| `ConfiguringStep` | Scrollable configure screen with all controls |
| `VideoPreviewCard` | Thumbnail + file metadata (name, res, fps, codec, size, duration) |
| `ResolutionPresetChips` | Row of selectable resolution chips (480p–8K + keep) |
| `RateControlToggle` | CRF/CBR/VBR toggle buttons |
| `QualitySlider` | Gradient track slider for CRF (0–51) |
| `BitrateSlider` | Slider for CBR/VBR mode (shown conditionally) |
| `DropdownOption` | Reusable dropdown for codec, fps, preset, profile, gop, bitrate |
| `AudioBitrateDropdown` | Audio bitrate selector |
| `ChannelsToggle` | Stereo/Mono toggle |
| `HWAccelToggle` | On/off toggle for hardware acceleration |
| `OutputPreviewCard` | Estimated output size + codec summary bar |
| `RunningStep` | Progress screen with stats |
| `DoneStep` | Success screen with open/share/done |

---

## State Model (unchanged from original spec)

```kotlin
sealed interface CompressUiState {
    data object Idle : CompressUiState
    data object PickingVideo : CompressUiState
    data class Configuring(
        val source: ProbeResult,
        val settings: CompressionSettings,
        val activeSmartPreset: SmartPreset?,  // keep for backwards compat
        val estimate: OutputEstimate,
        val expandedSections: Set<SectionId>,
    ) : CompressUiState
    data class Running(...)
    data class Done(...)
    data class Failed(...)
}
```

---

## Notes

- **No color controls** — too advanced for v1 MVP
- **No HDR controls** — niche, requires source detection
- **Resolution presets** replace smart preset chips as the primary quick-select UX
- **CRF slider** stays as-is (technical but accurate)
- **HW acceleration** defaults to ON, falls back to software automatically
- All existing data models (`CompressionSettings`, `ProbeResult`, etc.) remain unchanged