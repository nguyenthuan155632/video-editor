# Aurora UI Refactor — Design Spec

**Date:** 2026-04-19
**Status:** Approved for implementation
**Scope:** Full UI overhaul (theme + design system + Home + Compress + navigation transitions)
**Strategy:** Single PR / single branch
**Supersedes (visually):** `2026-04-19-compression-ui-redesign.md` — palette and components are replaced by Aurora; the underlying compression UX (resolution presets, sticky bottom estimator, etc.) is preserved.

---

## 1. Goal

Refactor the entire visible surface of the Video Editor Android app to a modern, high-energy "Aurora" visual language: animated mesh-gradient backgrounds, glassmorphism cards, neon-violet/magenta/cyan accents, springy motion. The aim is a CapCut/VN-class first impression while keeping all existing business logic untouched.

## 2. Non-goals (YAGNI)

- No light theme (force dark).
- No bottom navigation, no Projects/history feature, no onboarding.
- No new ViewModel logic, no changes to FFmpeg/encoding/estimator code.
- No icon-set replacement — keep current Material icons; restyle via `GradientIconBadge`.
- No animations during heavy encoding (`RunningStep` keeps backgrounds static for perf).
- No Material You dynamic colors (would dilute the Aurora identity).

## 3. Visual language

### 3.1 Palette (Aurora · dark only)

| Token | Hex | Use |
|---|---|---|
| `AuroraBgBase` | `#070712` | App background |
| `AuroraSurface1` | `#0F0E1F` | Glass card base tint |
| `AuroraSurface2` | `#171430` | Elevated glass tint |
| `AuroraViolet` | `#7C3AED` | Gradient stop |
| `AuroraMagenta` | `#EC4899` | Gradient stop |
| `AuroraCyan` | `#22D3EE` | Gradient stop |
| `AuroraTextPrimary` | `#F4F3FF` | Headlines, body |
| `AuroraTextSecondary` | `#A8A4C7` | Subtitles |
| `AuroraTextMuted` | `#5B5778` | Hints, disabled |
| `AuroraBorder` | `#2A2540` | Hairline borders |
| `AuroraSuccess` | `#34D399` | Done state |
| `AuroraWarning` | `#FBBF24` | Cautions |
| `AuroraError` | `#F87171` | Failed state |

### 3.2 Gradients

Defined in `core/theme/AuroraGradients.kt` as `Brush` factories:

- `auroraHorizontal()` — Violet → Magenta → Cyan, left → right
- `auroraDiagonal()` — Violet (TL) → Cyan (BR)
- `auroraRadial(center)` — Magenta core fading to transparent
- `auroraMesh(progressFraction)` — two `radialGradient` blobs whose centers orbit slowly; consumed by `AuroraBackground`

### 3.3 Typography

- Family: system `sans-serif`; the display style switches to `sans-serif-medium` with negative letter spacing (`-0.02.em`) for headlines.
- `displayLarge` 40sp / Medium / -0.02em
- `displayMedium` 32sp / Medium / -0.02em
- `headlineSmall` 22sp / Medium
- `bodyMedium` 14sp / Normal / 1.45 line height
- `labelLarge` 14sp / Medium (button text)

### 3.4 Motion

`core/theme/Motion.kt` adds:

- `auroraSpring`: `spring(dampingRatio = 0.7f, stiffness = StiffnessMediumLow)`
- `auroraEaseOut`: `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)` (250ms default)
- `pressScale`: 0.97 on press

### 3.5 Glassmorphism

`Modifier.glass(alpha = 0.6f, borderAlpha = 0.18f)` extension in `core/theme/Glass.kt`:

- API 31+: `Modifier.graphicsLayer { renderEffect = RenderEffect.createBlurEffect(24f, 24f, EDGE) }`, then translucent surface fill, then 1px gradient hairline border via `drawWithContent`.
- API 26–30 (fallback): translucent surface fill + hairline gradient border, no blur.

The fallback path MUST be visually acceptable on its own, since min SDK is 26.

## 4. Design system components (`core/designsys/`)

Each component lives in its own file — small, focused, easy to reason about.

| Component | File | Purpose |
|---|---|---|
| `AuroraBackground` | `AuroraBackground.kt` | Full-screen animated mesh gradient. Param `static: Boolean = false` to disable animation. |
| `GlassCard` | `GlassCard.kt` | Translucent card with gradient hairline border. Replaces `SectionCard`. |
| `GradientButton` | `GradientButton.kt` | Primary CTA. Variants: `Filled` (gradient fill), `Tonal` (glass + gradient text), `Ghost` (gradient border only). 56dp height, 20dp corner. |
| `GradientIconBadge` | `GradientIconBadge.kt` | Circular gradient-filled icon container. Default 48dp. |
| `SectionHeader` | `SectionHeader.kt` | Title + optional trailing chip. Uses display weight. |
| `StepIndicator` | `StepIndicator.kt` | Horizontal pill row: Pick · Configure · Run · Done. Active step has gradient fill, completed has check, upcoming muted. |
| `AuroraProgressBar` | `AuroraProgressBar.kt` | Replaces `LinearProgressIndicator`. Animated gradient sweep + glow at leading edge. |
| `AuroraChip` | `AuroraChip.kt` | Selectable chip; gradient border + soft glow when selected. |
| `StatPill` | `StatPill.kt` | Compact key/value pill with hairline gradient border. |
| `AuroraFab` | `AuroraFab.kt` | Extended FAB with aurora-gradient fill and outer glow. |

### Boundary rules

- Each component takes a `Modifier`, exposes only what callers need to vary, and contains no business logic.
- No component reaches into ViewModels or feature packages.
- Theme tokens are accessed through `MaterialTheme.colorScheme` extensions where possible so existing call sites still render correctly.

## 5. Theme integration (`core/theme/Theme.kt`)

```
@Composable
fun VideoEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuroraDarkScheme,   // forces dark; ignores system + dynamic
        typography  = AuroraTypography,
        content     = content,
    )
}
```

`AuroraDarkScheme` maps Aurora tokens onto Material 3 slots so any stray `MaterialTheme.colorScheme.primary` etc. caller still looks right:

- `primary` → `AuroraViolet`
- `secondary` → `AuroraMagenta`
- `tertiary` → `AuroraCyan`
- `background`/`surface` → `AuroraBgBase`
- `surfaceContainer*` → `AuroraSurface1`/`AuroraSurface2`
- `onSurface` → `AuroraTextPrimary`
- `onSurfaceVariant` → `AuroraTextSecondary`
- `outline` → `AuroraBorder`
- `error` → `AuroraError`

Dynamic-color and light-theme branches are removed.

## 6. Screens

### 6.1 `HomeScreen`

- `AuroraBackground(static = false)` behind a transparent `Scaffold`.
- Hero header replaces `TopAppBar`: large `displayLarge` "Video Editor" with `auroraHorizontal()` brush text fill, subtitle "Compress · Trim · Convert · More".
- 2-column adaptive `LazyVerticalGrid` (`minSize = 160.dp`) of redesigned feature cards. Each card:
  - `GlassCard` aspect-ratio 1:1
  - Top-left `GradientIconBadge`
  - Bottom: title (`headlineSmall`) + subtitle (`bodyMedium`, secondary text)
  - Disabled cards: replace alpha-dim with a "Soon" chip (gradient hairline border) in the top-right corner; card stays clear.
- Bottom-end `AuroraFab` "Quick compress" (extended), tapping it routes to Compress.

### 6.2 `CompressScreen` shell

- `AuroraBackground(static = state is Running)` behind everything.
- Custom slim header replaces `TopAppBar`: glass back-button (left), `StepIndicator` (center). The screen title moves into each step's hero area.
- `AnimatedContent` between `CompressUiState` substates:
  - `transitionSpec`: `(slideInHorizontally + fadeIn) togetherWith (slideOutHorizontally + fadeOut)`, 250ms, `auroraEaseOut`.
  - The `videoPicker` launcher and permission state stay in the parent so they aren't disposed on transition.

### 6.3 `PickStep`

- Centered hero column.
- Large `GradientIconBadge` (96dp) with video icon.
- `displayMedium` headline "Pick a video to compress" with gradient text fill.
- Muted hint "MP4, MOV, MKV, WebM up to your storage limit".
- `GradientButton(variant = Filled)` "Choose video" full-width-with-padding.
- Optional below: horizontal scroll of recent video thumbnails from MediaStore (best-effort; gracefully empty if no permission or no videos). Tapping a thumbnail acts as if picked.

### 6.4 `ConfiguringStep`

This file is currently 664 lines and will be **split** into smaller composables under `feature/compress/ui/configuring/`:

- `ConfiguringStep.kt` — orchestrator only (state, scroll, sticky bottom bar)
- `VideoHeroCard.kt` — thumbnail + StatPill row (replaces current `VideoPreviewCard`)
- `ResolutionPresetRow.kt` — resolution preset chips (480p / 720p / 1080p / 1440p / 4K / Keep) — preserves existing `ResolutionPreset` model
- `VideoSection.kt` — codec + rate control + CRF/bitrate + FPS + encoding preset + profile + GOP (maps to `SectionId.VIDEO`)
- `AudioSection.kt` — audio bitrate + channels (maps to `SectionId.AUDIO`)
- `AdvancedSection.kt` — hardware acceleration toggle (maps to `SectionId.ADVANCED`)
- `OutputEstimateBar.kt` — sticky-area estimated output size + saved ratio
- `internal/ConfiguringControls.kt` — small shared controls (`AuroraDropdownRow`, `SegmentedToggle`) used by sections

Model layer is **not** changed: `ResolutionPreset`, `SectionId`, `CompressionSettings`, etc. remain as-is. The `onSmartPreset` callback continues to be wired but unused (same as today) — out of scope to remove.

Layout:

- Hero `GlassCard`: video thumbnail (Coil), gradient hairline border, overlay `StatPill` row (duration · size · resolution · codec).
- Resolution preset row: `AuroraChip`s for each `ResolutionPreset` entry plus a "Keep" chip. Selected chip glows; chips disabled when source resolution is smaller than preset.
- Three collapsible glass sections — each is a `GlassCard` with a `SectionHeader` showing title + summary chip (e.g. "H.264 · CRF 23"). Tap header to expand inline.
- Sticky bottom bar (above system nav inset): "Estimated · 142 MB" left (animated number tween on changes), `GradientButton` "Compress" right.

### 6.5 `RunningStep`

- Background animation paused (`AuroraBackground(static = true)`).
- Large circular gradient progress ring drawn in `Canvas`:
  - 240dp diameter, 12dp stroke, sweep gradient using `auroraDiagonal()`.
  - Center: percentage in `displayLarge` with gradient text fill.
  - Below center: ETA in `bodyMedium`.
  - Subtle pulsing outer glow (alpha 0.2 ↔ 0.4, 1.4s period) to signal liveness without heavy work.
- `GlassCard` with stats: input → output sizes, fps, elapsed, ETA.
- `GradientButton(variant = Ghost, destructive = true)` "Cancel" at bottom.

### 6.6 `DoneStep`

- One-shot gradient particle burst (~1.5s, ~40 particles, alpha-fade, no loop) for celebration.
- `displayLarge` "Compressed" with gradient text fill.
- Big "Saved 62%" number (animated count-up from 0 to actual saved-percentage on first composition; computed from `ratio` already exposed by the ViewModel).
- Two `StatPill`s side-by-side: "Original · 248 MB" / "New · 94 MB".
- `GradientButton(Filled)` "Open in Photos" + `GradientButton(Ghost)` "Compress another".

### 6.7 `Failed` state

- Centered `GlassCard`:
  - Warning `GradientIconBadge` (uses warning palette stops)
  - "Something went wrong" headline + reason as muted body
  - `GradientButton(Filled)` "Try again" + `GradientButton(Ghost)` "Dismiss"

### 6.8 Navigation transitions (`AppNavHost`)

- Home → Compress: `slideIntoContainer(Start) + fadeIn` 250ms `auroraEaseOut`.
- Pop back: reverse with `auroraSpring`.

## 7. File map

### New (19)

```
core/theme/AuroraGradients.kt
core/theme/Glass.kt
core/designsys/AuroraBackground.kt
core/designsys/GlassCard.kt
core/designsys/GradientButton.kt
core/designsys/GradientIconBadge.kt
core/designsys/SectionHeader.kt
core/designsys/StepIndicator.kt
core/designsys/AuroraProgressBar.kt
core/designsys/AuroraChip.kt
core/designsys/StatPill.kt
core/designsys/AuroraFab.kt
feature/compress/ui/configuring/VideoHeroCard.kt
feature/compress/ui/configuring/ResolutionPresetRow.kt
feature/compress/ui/configuring/VideoSection.kt
feature/compress/ui/configuring/AudioSection.kt
feature/compress/ui/configuring/AdvancedSection.kt
feature/compress/ui/configuring/OutputEstimateBar.kt
feature/compress/ui/configuring/internal/ConfiguringControls.kt
```

### Modified (11)

```
core/theme/Color.kt           (Aurora palette)
core/theme/Type.kt            (display weights, letter spacing)
core/theme/Motion.kt          (auroraSpring, auroraEaseOut)
core/theme/Theme.kt           (force dark, AuroraDarkScheme, drop dynamic)
core/navigation/AppNavHost.kt (animated transitions)
feature/home/HomeScreen.kt    (hero + redesigned cards + AuroraFab)
feature/compress/CompressScreen.kt (custom header + StepIndicator + AnimatedContent)
feature/compress/ui/PickStep.kt
feature/compress/ui/ConfiguringStep.kt   (becomes orchestrator only)
feature/compress/ui/RunningStep.kt
feature/compress/ui/DoneStep.kt
```

### Deleted (1)

```
core/designsys/SectionCard.kt   (replaced by GlassCard; all callers migrated)
```

## 8. Risks & mitigations

| Risk | Mitigation |
|---|---|
| `RenderEffect.blur` is API 31+, min SDK is 26 | `Modifier.glass()` falls back to flat translucent + hairline border on API < 31. Verified visually acceptable. |
| Animated mesh gradient could hurt low-end devices | Two `radialGradient` brushes per frame; `static` flag for `RunningStep`. Profile on a low-end emulator before merging. |
| `AnimatedContent` transitions could dispose the `videoPicker` launcher | Hoist launcher + permission state to the `CompressScreen` root, outside `AnimatedContent`. |
| Forcing dark theme could surprise users on light-mode devices | Acceptable — confirmed during brainstorming; aligns with CapCut/VN convention. |
| Splitting `ConfiguringStep` could regress behavior | Each new file is purely visual; orchestrator file owns all state and callbacks. No logic moves. |
| Particle burst on `DoneStep` could leak after navigation away | One-shot `LaunchedEffect(Unit)` with bounded duration; no infinite transition. |

## 9. Verification

Per `CLAUDE.md` this project has no automated test suite. Verification is:

1. `./gradlew spotlessApply && ./gradlew assembleDebug` succeeds with zero warnings introduced by this change.
2. `./gradlew installDebug` and manually walk through:
   - Home: hero text gradient, animated background, all feature cards render, FAB visible, FAB → Compress.
   - Compress: header step indicator advances; transitions slide between Pick → Configuring → Running → Done.
   - Pick: gradient button works; recent thumbnails render or fall back gracefully.
   - Configuring: resolution preset chips toggle; the three sections (Video / Audio / Advanced) expand/collapse; estimator updates; sticky bar visible above nav bar.
   - Running: ring fills smoothly; cancel works; background is static.
   - Done: particle burst plays once; Saved % counts up; both buttons work.
   - Failed: trigger by force-killing FFmpeg; glass error card renders; both buttons work.
3. Test on API 26 emulator: glass fallback path renders without artifacts.
4. Test on API 34 device: blur path renders smoothly at 60fps.

## 10. Out-of-scope follow-ups (recorded, not built now)

- Light-theme variant of Aurora.
- Projects/history feature.
- Bottom navigation shell.
- Onboarding flow.
- Custom font (e.g. Inter) bundled as resource.
