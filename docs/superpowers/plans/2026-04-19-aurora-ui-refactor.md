# Aurora UI Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the entire visible Android app surface to a modern "Aurora" visual language (animated mesh gradients, glassmorphism, neon violet/magenta/cyan accents) without changing any business logic.

**Architecture:** Bottom-up. Build the theme tokens, then design-system primitives, then re-skin each screen using those primitives. Force dark theme. Existing model layer (`CompressionSettings`, `ResolutionPreset`, `SectionId`, etc.) is untouched.

**Tech Stack:** Kotlin 2.0, Jetpack Compose BOM 2025.01.00, Material 3, Hilt 2.52, Coil 2.7. Min SDK 26 (means `RenderEffect.blur` requires API 31+ runtime check).

**Spec:** `docs/superpowers/specs/2026-04-19-aurora-ui-refactor-design.md`

**Verification model:** This project has **no automated tests** (per `CLAUDE.md`). Each task ends with:
1. `./gradlew spotlessApply` (auto-format)
2. `./gradlew assembleDebug` (must succeed; zero new warnings)
3. Manual visual smoke if the task changes a visible screen
4. Commit

A single PR / single feature branch (`feat/aurora-ui-refactor`) holds the entire work.

---

## Task 0: Branch and baseline

**Files:** none

- [ ] **Step 1: Create the feature branch**

```bash
git checkout -b feat/aurora-ui-refactor
```

- [ ] **Step 2: Verify baseline build works**

```bash
./gradlew spotlessApply assembleDebug
```

Expected: `BUILD SUCCESSFUL`. If not, stop and fix the baseline before continuing.

- [ ] **Step 3: Commit baseline marker (empty allowed)**

```bash
git commit --allow-empty -m "chore: baseline before aurora UI refactor"
```

---

## Task 1: Aurora color palette

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/core/theme/Color.kt`

- [ ] **Step 1: Replace `Color.kt` with the Aurora palette**

```kotlin
package com.videoeditor.core.theme

import androidx.compose.ui.graphics.Color

val AuroraBgBase = Color(0xFF070712)
val AuroraSurface1 = Color(0xFF0F0E1F)
val AuroraSurface2 = Color(0xFF171430)

val AuroraViolet = Color(0xFF7C3AED)
val AuroraMagenta = Color(0xFFEC4899)
val AuroraCyan = Color(0xFF22D3EE)

val AuroraTextPrimary = Color(0xFFF4F3FF)
val AuroraTextSecondary = Color(0xFFA8A4C7)
val AuroraTextMuted = Color(0xFF5B5778)

val AuroraBorder = Color(0xFF2A2540)

val AuroraSuccess = Color(0xFF34D399)
val AuroraWarning = Color(0xFFFBBF24)
val AuroraError = Color(0xFFF87171)

@Deprecated("Replaced by AuroraViolet for theme primary", ReplaceWith("AuroraViolet"))
val IndigoSeed = AuroraViolet

@Deprecated("Replaced by AuroraCyan for theme tertiary", ReplaceWith("AuroraCyan"))
val AccentTeal = AuroraCyan

@Deprecated("Light theme is removed", ReplaceWith("AuroraBgBase"))
val SurfaceLight = AuroraBgBase

@Deprecated("Replaced by AuroraBgBase", ReplaceWith("AuroraBgBase"))
val SurfaceDark = AuroraBgBase
```

The deprecated aliases preserve any stray references during the transition; they'll be removed in Task 14.

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/theme/Color.kt
git commit -m "feat(theme): introduce Aurora color palette"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 2: Aurora gradient brushes

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/theme/AuroraGradients.kt`

- [ ] **Step 1: Write `AuroraGradients.kt`**

```kotlin
package com.videoeditor.core.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AuroraGradients {
    val stops: List<Color> = listOf(AuroraViolet, AuroraMagenta, AuroraCyan)

    fun horizontal(): Brush = Brush.horizontalGradient(stops)

    fun diagonal(): Brush = Brush.linearGradient(
        colors = stops,
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    fun radial(center: Offset = Offset.Unspecified, radius: Float = Float.POSITIVE_INFINITY): Brush =
        Brush.radialGradient(
            colors = listOf(AuroraMagenta.copy(alpha = 0.85f), AuroraViolet.copy(alpha = 0.0f)),
            center = center,
            radius = radius,
        )

    /**
     * Two orbiting radial blobs. [progress] is 0f..1f, where the caller drives it
     * with an infinite transition.
     */
    fun mesh(width: Float, height: Float, progress: Float): List<Brush> {
        val tau = (2 * Math.PI).toFloat()
        val r = (minOf(width, height) * 0.7f)
        val cx1 = width * 0.5f + (width * 0.25f) * kotlin.math.cos(progress * tau)
        val cy1 = height * 0.35f + (height * 0.18f) * kotlin.math.sin(progress * tau)
        val cx2 = width * 0.5f + (width * 0.30f) * kotlin.math.cos(progress * tau + Math.PI.toFloat())
        val cy2 = height * 0.65f + (height * 0.20f) * kotlin.math.sin(progress * tau + Math.PI.toFloat())

        val violetBlob = Brush.radialGradient(
            colors = listOf(AuroraViolet.copy(alpha = 0.55f), AuroraViolet.copy(alpha = 0f)),
            center = Offset(cx1, cy1),
            radius = r,
        )
        val cyanBlob = Brush.radialGradient(
            colors = listOf(AuroraCyan.copy(alpha = 0.40f), AuroraCyan.copy(alpha = 0f)),
            center = Offset(cx2, cy2),
            radius = r,
        )
        val magentaWash = Brush.radialGradient(
            colors = listOf(AuroraMagenta.copy(alpha = 0.30f), AuroraMagenta.copy(alpha = 0f)),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = r * 1.1f,
        )
        return listOf(violetBlob, cyanBlob, magentaWash)
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/theme/AuroraGradients.kt
git commit -m "feat(theme): add AuroraGradients brush factory"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 3: Glass modifier

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/theme/Glass.kt`

- [ ] **Step 1: Write `Glass.kt`**

```kotlin
package com.videoeditor.core.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism surface treatment.
 * - API 31+: real RenderEffect blur.
 * - API 26..30: translucent fill + gradient hairline border (no blur).
 *
 * Caller is responsible for clipping their own content if they place children on top.
 */
fun Modifier.glass(
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = 0.55f,
    borderAlpha: Float = 0.22f,
    blurRadius: Dp = 24.dp,
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        listOf(
            AuroraViolet.copy(alpha = borderAlpha),
            AuroraMagenta.copy(alpha = borderAlpha),
            AuroraCyan.copy(alpha = borderAlpha),
        ),
    )
    val blurredLayer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = RenderEffect
                .createBlurEffect(blurRadius.toPx(), blurRadius.toPx(), Shader.TileMode.DECAL)
                .asComposeRenderEffect()
        }
    } else {
        Modifier
    }
    this
        .clip(shape)
        .then(blurredLayer)
        .background(AuroraSurface1.copy(alpha = surfaceAlpha), shape)
        .drawWithContent {
            drawContent()
            drawRoundRect(
                brush = borderBrush,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = 1.dp.toPx()),
                color = Color.Unspecified,
            )
        }
}
```

Note: `drawRoundRect` with `brush` overload doesn't take a `color` arg in stable Compose; keep the signature minimal. If the formatter complains, the simpler call is:

```kotlin
drawRoundRect(
    brush = borderBrush,
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
    style = Stroke(width = 1.dp.toPx()),
)
```

Use that simpler form.

- [ ] **Step 2: Fix `drawRoundRect` call to the brush-only overload**

Replace the `drawRoundRect(...)` block in step 1 with:

```kotlin
drawRoundRect(
    brush = borderBrush,
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
    style = Stroke(width = 1.dp.toPx()),
)
```

- [ ] **Step 3: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/theme/Glass.kt
git commit -m "feat(theme): add Modifier.glass with API-aware fallback"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 4: Aurora typography

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/core/theme/Type.kt`

- [ ] **Step 1: Replace `Type.kt`**

```kotlin
package com.videoeditor.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val AuroraTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.02).em,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    ),
)

@Deprecated("Use AuroraTypography", ReplaceWith("AuroraTypography"))
val AppTypography = AuroraTypography
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/theme/Type.kt
git commit -m "feat(theme): add Aurora typography scale"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 5: Aurora motion presets

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/core/theme/Motion.kt`

- [ ] **Step 1: Replace `Motion.kt`**

```kotlin
package com.videoeditor.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object AuroraMotion {
    val auroraSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow,
    )

    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    val auroraEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    const val DURATION_SHORT_MS = 180
    const val DURATION_MEDIUM_MS = 250
    const val DURATION_LONG_MS = 400
}

@Deprecated("Use AuroraMotion", ReplaceWith("AuroraMotion"))
val MotionSpec = object {
    val press = AuroraMotion.pressSpring
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/theme/Motion.kt
git commit -m "feat(theme): add AuroraMotion springs and easing"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 6: Wire the theme to MaterialTheme (force dark)

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/core/theme/Theme.kt`

- [ ] **Step 1: Replace `Theme.kt`**

```kotlin
package com.videoeditor.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AuroraDarkScheme = darkColorScheme(
    primary = AuroraViolet,
    onPrimary = AuroraTextPrimary,
    secondary = AuroraMagenta,
    onSecondary = AuroraTextPrimary,
    tertiary = AuroraCyan,
    onTertiary = AuroraBgBase,
    background = AuroraBgBase,
    onBackground = AuroraTextPrimary,
    surface = AuroraBgBase,
    onSurface = AuroraTextPrimary,
    surfaceVariant = AuroraSurface2,
    onSurfaceVariant = AuroraTextSecondary,
    surfaceContainerLowest = AuroraBgBase,
    surfaceContainerLow = AuroraSurface1,
    surfaceContainer = AuroraSurface1,
    surfaceContainerHigh = AuroraSurface2,
    surfaceContainerHighest = AuroraSurface2,
    outline = AuroraBorder,
    outlineVariant = AuroraBorder,
    error = AuroraError,
    onError = AuroraTextPrimary,
)

@Composable
fun VideoEditorTheme(
    @Suppress("UNUSED_PARAMETER") dark: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamic: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AuroraDarkScheme,
        typography = AuroraTypography,
        content = content,
    )
}
```

The signature keeps the original `dark` and `dynamic` parameters so any existing call site (e.g. `MainActivity`) compiles unchanged. Both are ignored — Aurora is dark-only.

- [ ] **Step 2: Verify the app still launches end-to-end**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Expected: app opens to Home, screens look like before but with the Aurora dark base color (`#070712`). Existing components still render because Material slots are remapped.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add app/src/main/kotlin/com/videoeditor/core/theme/Theme.kt
git commit -m "feat(theme): force dark Aurora MaterialTheme"
```

---

## Task 7: AuroraBackground

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/AuroraBackground.kt`

- [ ] **Step 1: Write `AuroraBackground.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.videoeditor.core.theme.AuroraBgBase
import com.videoeditor.core.theme.AuroraGradients

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    static: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(AuroraBgBase)) {
        if (static) {
            StaticAurora()
        } else {
            AnimatedAurora()
        }
        content()
    }
}

@Composable
private fun StaticAurora() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        AuroraGradients.mesh(size.width, size.height, progress = 0.25f).forEach { brush ->
            drawRect(brush = brush)
        }
    }
}

@Composable
private fun AnimatedAurora() {
    val transition = rememberInfiniteTransition(label = "aurora-bg")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aurora-progress",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        AuroraGradients.mesh(size.width, size.height, progress).forEach { brush ->
            drawRect(brush = brush)
        }
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/AuroraBackground.kt
git commit -m "feat(designsys): add AuroraBackground with static fallback"
```

Expected: `BUILD SUCCESSFUL`. Not yet visible — used in screen tasks.

---

## Task 8: GlassCard (replaces SectionCard later)

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/GlassCard.kt`

- [ ] **Step 1: Write `GlassCard.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.glass

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier.glass(cornerRadius = cornerRadius)
    val withClick = if (onClick != null) base.clickable { onClick() } else base
    Box(modifier = withClick.padding(contentPadding)) {
        content()
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/GlassCard.kt
git commit -m "feat(designsys): add GlassCard"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 9: GradientButton

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/GradientButton.kt`

- [ ] **Step 1: Write `GradientButton.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextMuted
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.glass

enum class GradientButtonVariant { Filled, Tonal, Ghost }

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GradientButtonVariant = GradientButtonVariant.Filled,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    cornerRadius: Dp = 20.dp,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btn-scale")

    val shape = RoundedCornerShape(cornerRadius)
    val gradient: Brush = AuroraGradients.horizontal()
    val baseModifier = modifier
        .height(height)
        .scale(scale)
        .clip(shape)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )

    val backgroundModifier = when (variant) {
        GradientButtonVariant.Filled -> Modifier.background(gradient, shape)
        GradientButtonVariant.Tonal -> Modifier.glass(cornerRadius = cornerRadius, surfaceAlpha = 0.65f)
        GradientButtonVariant.Ghost -> Modifier
            .background(Color.Transparent, shape)
            .glass(cornerRadius = cornerRadius, surfaceAlpha = 0.0f, borderAlpha = 0.6f)
    }

    val textColor = when (variant) {
        GradientButtonVariant.Filled -> AuroraTextPrimary
        GradientButtonVariant.Tonal -> AuroraTextPrimary
        GradientButtonVariant.Ghost -> AuroraTextPrimary
    }

    Box(
        modifier = baseModifier.then(backgroundModifier).padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                leadingIcon?.invoke()
                Text(
                    text = text,
                    color = if (enabled) textColor else AuroraTextMuted,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/GradientButton.kt
git commit -m "feat(designsys): add GradientButton (Filled/Tonal/Ghost)"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 10: GradientIconBadge

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/GradientIconBadge.kt`

- [ ] **Step 1: Write `GradientIconBadge.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextPrimary

@Composable
fun GradientIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = size * 0.5f,
    brush: Brush = AuroraGradients.diagonal(),
    iconTint: Color = AuroraTextPrimary,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(brush, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/GradientIconBadge.kt
git commit -m "feat(designsys): add GradientIconBadge"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 11: SectionHeader

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/SectionHeader.kt`

- [ ] **Step 1: Write `SectionHeader.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AuroraTextPrimary,
            )
            if (summary != null) {
                Text(
                    text = "  $summary",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraTextSecondary,
                )
            }
        }
        trailing?.invoke()
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/SectionHeader.kt
git commit -m "feat(designsys): add SectionHeader"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 12: StepIndicator

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/StepIndicator.kt`

- [ ] **Step 1: Write `StepIndicator.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary

enum class StepState { Upcoming, Active, Done }

data class Step(val id: String, val label: String)

@Composable
fun StepIndicator(
    steps: List<Step>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        steps.forEachIndexed { idx, step ->
            val state = when {
                idx < activeIndex -> StepState.Done
                idx == activeIndex -> StepState.Active
                else -> StepState.Upcoming
            }
            StepDot(index = idx + 1, state = state)
            if (idx != steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 1.dp)
                        .background(if (idx < activeIndex) Color.White.copy(alpha = 0.4f) else AuroraBorder),
                )
            }
        }
    }
}

@Composable
private fun StepDot(index: Int, state: StepState) {
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            StepState.Done -> Color.Transparent
            StepState.Active -> Color.Transparent
            StepState.Upcoming -> AuroraBorder
        },
        label = "step-border",
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(
                when (state) {
                    StepState.Active -> Modifier.background(AuroraGradients.horizontal(), CircleShape)
                    StepState.Done -> Modifier.background(AuroraGradients.diagonal(), CircleShape)
                    StepState.Upcoming -> Modifier
                },
            )
            .border(width = 1.dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            StepState.Done -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AuroraTextPrimary,
                modifier = Modifier.size(16.dp),
            )
            StepState.Active -> androidx.compose.material3.Text(
                text = "$index",
                color = AuroraTextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            )
            StepState.Upcoming -> androidx.compose.material3.Text(
                text = "$index",
                color = AuroraTextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            )
        }
    }
}

object CompressSteps {
    val ALL: List<Step> = listOf(
        Step("pick", "Pick"),
        Step("configure", "Configure"),
        Step("run", "Run"),
        Step("done", "Done"),
    )
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/StepIndicator.kt
git commit -m "feat(designsys): add StepIndicator + CompressSteps"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 13: AuroraProgressBar (linear and ring helpers)

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/AuroraProgressBar.kt`

- [ ] **Step 1: Write `AuroraProgressBar.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraGradients

@Composable
fun AuroraLinearProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "linear-progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(AuroraBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(AuroraGradients.horizontal()),
        )
    }
}

@Composable
fun AuroraProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 240.dp,
    strokeWidth: Dp = 14.dp,
) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), label = "ring-progress")
    Canvas(modifier = modifier.size(diameter)) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        drawArc(
            brush = Brush.sweepGradient(listOf(AuroraBorder, AuroraBorder)),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            brush = AuroraGradients.diagonal(),
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/AuroraProgressBar.kt
git commit -m "feat(designsys): add AuroraLinearProgress + AuroraProgressRing"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 14: AuroraChip

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/AuroraChip.kt`

- [ ] **Step 1: Write `AuroraChip.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraSurface1
import com.videoeditor.core.theme.AuroraTextMuted
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary

@Composable
fun AuroraChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(
        if (selected) Color.Transparent else AuroraSurface1,
        label = "chip-bg",
    )
    val textColor = when {
        !enabled -> AuroraTextMuted
        selected -> AuroraTextPrimary
        else -> AuroraTextSecondary
    }
    val gradientBorder: Brush = if (selected) AuroraGradients.horizontal() else Brush.linearGradient(
        listOf(AuroraBorder, AuroraBorder),
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Color.Transparent else bg, shape)
            .then(
                if (selected) Modifier.background(AuroraGradients.horizontal().withAlpha(0.18f), shape) else Modifier,
            )
            .border(BorderStroke(1.dp, gradientBorder), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

private fun Brush.withAlpha(@Suppress("UNUSED_PARAMETER") alpha: Float): Brush = this
```

The `withAlpha` helper is a no-op stub — Compose `Brush` doesn't expose alpha directly here; the visual difference is carried by the gradient border + text color. Keep the helper to make later refactors easy.

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/AuroraChip.kt
git commit -m "feat(designsys): add AuroraChip"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 15: StatPill

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/StatPill.kt`

- [ ] **Step 1: Write `StatPill.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraSurface1
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary

@Composable
fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderBrush = Brush.linearGradient(listOf(AuroraBorder, AuroraBorder))
    Row(
        modifier = modifier
            .clip(shape)
            .background(AuroraSurface1, shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AuroraTextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = AuroraTextPrimary,
        )
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/StatPill.kt
git commit -m "feat(designsys): add StatPill"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 16: AuroraFab

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/core/designsys/AuroraFab.kt`

- [ ] **Step 1: Write `AuroraFab.kt`**

```kotlin
package com.videoeditor.core.designsys

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraMagenta
import com.videoeditor.core.theme.AuroraTextPrimary

@Composable
fun AuroraFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "fab-scale")
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = 18.dp, shape = shape, ambientColor = AuroraMagenta, spotColor = AuroraMagenta)
            .clip(shape)
            .background(AuroraGradients.horizontal(), shape)
            .height(56.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = AuroraTextPrimary)
        Text(
            text = text,
            color = AuroraTextPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/core/designsys/AuroraFab.kt
git commit -m "feat(designsys): add AuroraFab"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 17: AppNavHost — animated transitions

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/core/navigation/AppNavHost.kt`

- [ ] **Step 1: Replace `AppNavHost.kt`**

```kotlin
package com.videoeditor.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.videoeditor.core.theme.AuroraMotion
import com.videoeditor.feature.compress.CompressScreen
import com.videoeditor.feature.home.HomeScreen

@Composable
fun AppNavHost(nav: NavHostController) {
    val durationMs = AuroraMotion.DURATION_MEDIUM_MS
    val easing = AuroraMotion.auroraEaseOut

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(durationMs, easing = easing),
                initialOffsetX = { it / 8 },
            ) + fadeIn(animationSpec = tween(durationMs))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMs / 2))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(durationMs, easing = easing),
                initialOffsetX = { -it / 8 },
            ) + fadeIn(animationSpec = tween(durationMs))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(durationMs, easing = easing),
                targetOffsetX = { it / 8 },
            ) + fadeOut(animationSpec = tween(durationMs))
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(onOpenFeature = { route -> nav.navigate(route) })
        }
        composable(Routes.COMPRESS) {
            CompressScreen(onBack = { nav.popBackStack() })
        }
    }
}
```

- [ ] **Step 2: Build and visually confirm**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Expected: tapping a Home card slides into Compress with a soft fade. Back-press slides back. No regressions.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add app/src/main/kotlin/com/videoeditor/core/navigation/AppNavHost.kt
git commit -m "feat(nav): animated transitions between Home and Compress"
```

---

## Task 18: HomeScreen redesign

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/feature/home/HomeScreen.kt`

- [ ] **Step 1: Replace `HomeScreen.kt`**

```kotlin
package com.videoeditor.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.AuroraBackground
import com.videoeditor.core.designsys.AuroraChip
import com.videoeditor.core.designsys.AuroraFab
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.GradientIconBadge
import com.videoeditor.core.navigation.FeatureCard
import com.videoeditor.core.navigation.FeatureRegistry
import com.videoeditor.core.navigation.Routes
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary

@Composable
fun HomeScreen(onOpenFeature: (String) -> Unit) {
    AuroraBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeHero()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(FeatureRegistry.cards, key = { it.id }) { card ->
                        FeatureCardView(card) { route -> route?.let(onOpenFeature) }
                    }
                }
            }
            AuroraFab(
                text = "Quick compress",
                icon = Icons.Outlined.Bolt,
                onClick = { onOpenFeature(Routes.COMPRESS) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun HomeHero() {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 20.dp)) {
        val gradient: Brush = AuroraGradients.horizontal()
        Text(
            text = "Video Editor",
            style = MaterialTheme.typography.displayLarge.copy(brush = gradient),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Compress · Trim · Convert · More",
            style = MaterialTheme.typography.bodyMedium,
            color = AuroraTextSecondary,
        )
    }
}

@Composable
private fun FeatureCardView(card: FeatureCard, onClick: (String?) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press-scale")
    val enabled = card.route != null

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(18.dp),
        onClick = if (enabled) {
            {
                pressed = true
                onClick(card.route)
                pressed = false
            }
        } else null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                GradientIconBadge(icon = card.icon, size = 44.dp)
                Column {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AuroraTextPrimary,
                    )
                    Text(
                        text = card.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraTextSecondary,
                    )
                }
            }
            if (!enabled) {
                AuroraChip(
                    label = "Soon",
                    selected = true,
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}
```

A note on the gradient text fill: `MaterialTheme.typography.displayLarge.copy(brush = gradient)` requires the `brush` argument on `TextStyle` (Compose 1.5+). If your Compose version errors on this, fall back to `Text(... color = AuroraViolet)` for the headline — visually less spectacular but compiles everywhere. Verify the build works with the brush version first.

- [ ] **Step 2: Build, install, visually verify**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Expected:
- Animated aurora background visible behind a transparent surface.
- Big gradient "Video Editor" title.
- Feature cards rendered as glass tiles with a circular gradient icon badge.
- "Coming soon" cards have a "Soon" pill chip top-right.
- A pill "Quick compress" FAB with gradient fill and glow at bottom-right; tapping it routes to Compress.

If `brush = gradient` on `TextStyle` fails to compile, replace the `style = ...` line with `color = AuroraViolet` and rebuild.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add app/src/main/kotlin/com/videoeditor/feature/home/HomeScreen.kt
git commit -m "feat(home): Aurora hero + glass feature cards + FAB"
```

---

## Task 19: CompressScreen shell — header + StepIndicator + AnimatedContent

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/feature/compress/CompressScreen.kt`

- [ ] **Step 1: Replace `CompressScreen.kt`**

```kotlin
package com.videoeditor.feature.compress

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.videoeditor.core.designsys.AuroraBackground
import com.videoeditor.core.designsys.CompressSteps
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.StepIndicator
import com.videoeditor.core.theme.AuroraMotion
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.glass
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.ui.ConfiguringStep
import com.videoeditor.feature.compress.ui.DoneStep
import com.videoeditor.feature.compress.ui.FailedStep
import com.videoeditor.feature.compress.ui.PickStep
import com.videoeditor.feature.compress.ui.RunningStep

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CompressScreen(
    onBack: () -> Unit,
    viewModel: CompressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.onVideoSelected(it) } }

    val pickAction: () -> Unit = {
        if (readPermission.status.isGranted) {
            videoPicker.launch("video/*")
        } else {
            readPermission.launchPermissionRequest()
        }
    }

    val activeIndex = when (uiState) {
        is CompressUiState.Idle, is CompressUiState.PickingVideo -> 0
        is CompressUiState.Configuring -> 1
        is CompressUiState.Running -> 2
        is CompressUiState.Done -> 3
        is CompressUiState.Failed -> 1
    }
    val runningPhase = uiState is CompressUiState.Running

    AuroraBackground(static = runningPhase) {
        Column(modifier = Modifier.fillMaxSize()) {
            CompressTopBar(
                activeIndex = activeIndex,
                onBack = {
                    viewModel.onBack()
                    onBack()
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = uiState,
                    label = "compress-step",
                    transitionSpec = {
                        (slideInHorizontally(
                            tween(AuroraMotion.DURATION_MEDIUM_MS, easing = AuroraMotion.auroraEaseOut),
                        ) { it / 6 } + fadeIn(tween(AuroraMotion.DURATION_MEDIUM_MS))) togetherWith
                            (slideOutHorizontally(
                                tween(AuroraMotion.DURATION_MEDIUM_MS, easing = AuroraMotion.auroraEaseOut),
                            ) { -it / 6 } + fadeOut(tween(AuroraMotion.DURATION_MEDIUM_MS / 2)))
                    },
                ) { state ->
                    when (state) {
                        is CompressUiState.Idle -> PickStep(onPick = pickAction)
                        is CompressUiState.PickingVideo -> PickStep(onPick = pickAction).also {
                            videoPicker.launch("video/*")
                        }
                        is CompressUiState.Configuring -> ConfiguringStep(
                            state = state,
                            onPickDifferent = pickAction,
                            onSmartPreset = viewModel::onSmartPresetSelected,
                            onSettingsChanged = viewModel::onSettingsChanged,
                            onSectionToggle = viewModel::onSectionToggle,
                            onStartEncode = viewModel::onStartEncode,
                        )
                        is CompressUiState.Running -> RunningStep(
                            progress = state.progress,
                            onCancel = viewModel::onCancelEncode,
                        )
                        is CompressUiState.Done -> DoneStep(
                            output = state.output,
                            ratio = state.ratio,
                        )
                        is CompressUiState.Failed -> FailedStep(
                            reason = state.reason,
                            onDismiss = viewModel::onDismissError,
                            onRetry = viewModel::onDismissError,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompressTopBar(activeIndex: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .glass(cornerRadius = 20.dp, surfaceAlpha = 0.45f),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AuroraTextPrimary,
                )
            }
        }
        StepIndicator(steps = CompressSteps.ALL, activeIndex = activeIndex)
        Box(modifier = Modifier.size(40.dp))
    }
}
```

This file references `FailedStep` (Task 23) and assumes `PickStep`, `ConfiguringStep`, `RunningStep`, `DoneStep` exist with their current signatures. Splitting/restyle of those screens happens in later tasks; the build will succeed because the existing implementations still exist. `FailedStep` will be added in Task 23 — until then this file will not compile, so we'll add a stub now and expand it later.

- [ ] **Step 2: Add a stub `FailedStep` to keep the build green**

Create file `app/src/main/kotlin/com/videoeditor/feature/compress/ui/FailedStep.kt`:

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FailedStep(
    reason: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: $reason")
    }
}
```

The stub will be replaced with the real Aurora UI in Task 23.

- [ ] **Step 3: Build, install, visually verify**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Expected:
- Tapping "Quick compress" enters CompressScreen.
- New slim header with glass back-button (left), step indicator dots (center).
- Step 1 (Pick) is active.
- Tapping back returns to Home with the Aurora animated transition.
- Picking a video transitions to step 2 (Configure) and the indicator updates.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add app/src/main/kotlin/com/videoeditor/feature/compress/CompressScreen.kt \
        app/src/main/kotlin/com/videoeditor/feature/compress/ui/FailedStep.kt
git commit -m "feat(compress): Aurora shell with StepIndicator + AnimatedContent"
```

---

## Task 20: PickStep redesign

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/PickStep.kt`

- [ ] **Step 1: Replace `PickStep.kt`**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.designsys.GradientButtonVariant
import com.videoeditor.core.designsys.GradientIconBadge
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextSecondary

@Composable
fun PickStep(onPick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            GradientIconBadge(
                icon = Icons.Default.VideoFile,
                size = 96.dp,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Pick a video",
                style = MaterialTheme.typography.displayMedium.copy(brush = AuroraGradients.horizontal()),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MP4, MOV, MKV, WebM — anything Android can read.",
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraTextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            GradientButton(
                text = "Choose video",
                onClick = onPick,
                variant = GradientButtonVariant.Filled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
```

If `displayMedium.copy(brush = ...)` fails to compile in your Compose version, swap to `style = MaterialTheme.typography.displayMedium, color = AuroraTextPrimary`.

- [ ] **Step 2: Build, install, visually verify**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Expected: in Compress → Pick step, big gradient circular icon, gradient headline, full-width gradient "Choose video" button.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add app/src/main/kotlin/com/videoeditor/feature/compress/ui/PickStep.kt
git commit -m "feat(compress): Aurora PickStep"
```

---

## Task 21: ConfiguringStep — split shared internal controls

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/internal/ConfiguringControls.kt`

This task extracts the `DropdownRow`, `RateControlToggle`, and `ChannelsToggle` helpers from the legacy `ConfiguringStep.kt` into a dedicated file with Aurora styling. Body source is the legacy code, with colors swapped for Aurora tokens.

- [ ] **Step 1: Write `ConfiguringControls.kt`**

```kotlin
package com.videoeditor.feature.compress.ui.configuring.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraSurface1
import com.videoeditor.core.theme.AuroraSurface2
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.AudioChannels
import com.videoeditor.feature.compress.model.RateControl

@Composable
fun AuroraDropdownRow(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AuroraSurface2)
                .border(1.dp, AuroraBorder, RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = AuroraTextSecondary, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = AuroraTextPrimary, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.width(8.dp))
                Text("▾", color = AuroraTextSecondary)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AuroraSurface1),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == value) AuroraTextPrimary else AuroraTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SegmentedToggleRateControl(selected: RateControl, onSelect: (RateControl) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RateControl.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isSelected) Modifier.background(AuroraGradients.horizontal())
                        else Modifier.background(AuroraSurface1),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AuroraTextPrimary.copy(alpha = 0f) else AuroraBorder,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) AuroraTextPrimary else AuroraTextSecondary,
                )
            }
        }
    }
}

@Composable
fun SegmentedToggleChannels(channels: AudioChannels, onSelect: (AudioChannels) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AudioChannels.entries.forEach { ch ->
            val isSelected = channels == ch
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isSelected) Modifier.background(AuroraGradients.horizontal())
                        else Modifier.background(AuroraSurface1),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AuroraTextPrimary.copy(alpha = 0f) else AuroraBorder,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(ch) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ch.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) AuroraTextPrimary else AuroraTextSecondary,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/internal/ConfiguringControls.kt
git commit -m "feat(compress): extract Aurora-styled shared controls"
```

Expected: `BUILD SUCCESSFUL`. Not yet visible — used in subsequent tasks.

---

## Task 22: ConfiguringStep — extract VideoHeroCard, ResolutionPresetRow, sections, OutputEstimateBar; rewrite orchestrator

**Files:**
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/VideoHeroCard.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/ResolutionPresetRow.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/VideoSection.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/AudioSection.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/AdvancedSection.kt`
- Create: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/OutputEstimateBar.kt`
- Modify: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/ConfiguringStep.kt`

This is the largest task. Each new file owns one piece.

- [ ] **Step 1: Write `VideoHeroCard.kt`**

```kotlin
package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.StatPill
import com.videoeditor.core.probe.ProbeResult
import com.videoeditor.core.theme.AuroraTextPrimary
import java.text.DecimalFormat

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun VideoHeroCard(source: ProbeResult, modifier: Modifier = Modifier) {
    val df = DecimalFormat("#.##")
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = source.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = AuroraTextPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatPill(label = "Size", value = "${df.format(source.sizeBytes / (1024.0 * 1024.0))} MB")
                StatPill(label = "Res", value = "${source.widthPx}\u00d7${source.heightPx}")
                if (source.frameRate.isFinite() && source.frameRate > 0) {
                    StatPill(label = "FPS", value = df.format(source.frameRate))
                }
                StatPill(label = "Codec", value = source.videoCodec.uppercase())
                StatPill(label = "Length", value = formatDuration(source.durationMs))
                StatPill(label = "Bitrate", value = "${df.format(source.videoBitrateBps / 1_000_000.0)} Mbps")
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
```

- [ ] **Step 2: Write `ResolutionPresetRow.kt`**

```kotlin
package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.AuroraChip
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.feature.compress.model.ResolutionPreset

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ResolutionPresetRow(
    selected: ResolutionPreset?,
    sourceHeight: Int,
    onSelect: (ResolutionPreset?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Resolution",
            style = MaterialTheme.typography.titleMedium,
            color = AuroraTextPrimary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResolutionPreset.entries.forEach { preset ->
                val disabled = preset.shortEdgePx > sourceHeight
                AuroraChip(
                    label = preset.label,
                    selected = selected == preset,
                    enabled = !disabled,
                    onClick = { onSelect(if (selected == preset) null else preset) },
                )
            }
            AuroraChip(
                label = "Keep",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
    }
}
```

- [ ] **Step 3: Write `VideoSection.kt`**

```kotlin
package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.SectionHeader
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.core.theme.AuroraViolet
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.EncodingPreset
import com.videoeditor.feature.compress.model.FpsChoice
import com.videoeditor.feature.compress.model.H264Profile
import com.videoeditor.feature.compress.model.RateControl
import com.videoeditor.feature.compress.model.VideoCodec
import com.videoeditor.feature.compress.ui.configuring.internal.AuroraDropdownRow
import com.videoeditor.feature.compress.ui.configuring.internal.SegmentedToggleRateControl

@Composable
fun VideoSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var crf by remember(settings.crf) { mutableFloatStateOf(settings.crf.toFloat()) }
    var bitrate by remember(settings.targetBitrateKbps) { mutableFloatStateOf(settings.targetBitrateKbps.toFloat()) }

    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onToggle) {
        Column {
            SectionHeader(
                title = "Video",
                summary = "${settings.codec.name} \u00b7 ${settings.rateControl.name} ${if (settings.rateControl == RateControl.CRF) settings.crf.toString() else "${settings.targetBitrateKbps}k"}",
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AuroraDropdownRow(
                        label = "Codec",
                        value = settings.codec.name,
                        options = VideoCodec.entries.map { it.name },
                        onSelect = { onChange(settings.copy(codec = VideoCodec.valueOf(it))) },
                    )
                    SegmentedToggleRateControl(
                        selected = settings.rateControl,
                        onSelect = { onChange(settings.copy(rateControl = it)) },
                    )
                    if (settings.rateControl == RateControl.CRF) {
                        CrfSlider(
                            crf = crf,
                            onPreview = { crf = it },
                            onCommit = { onChange(settings.copy(crf = crf.toInt())) },
                        )
                    } else {
                        BitrateSlider(
                            bitrate = bitrate,
                            onPreview = { bitrate = it },
                            onCommit = { onChange(settings.copy(targetBitrateKbps = bitrate.toInt())) },
                        )
                    }
                    AuroraDropdownRow(
                        label = "FPS",
                        value = when (settings.fps) {
                            FpsChoice.KEEP -> "Keep original"
                            else -> settings.fps.name.replace("FPS_", "") + " fps"
                        },
                        options = listOf("Keep original", "24 fps", "30 fps", "60 fps", "90 fps", "120 fps"),
                        onSelect = {
                            val fps = when (it) {
                                "Keep original" -> FpsChoice.KEEP
                                "24 fps" -> FpsChoice.FPS_24
                                "30 fps" -> FpsChoice.FPS_30
                                "60 fps" -> FpsChoice.FPS_60
                                "90 fps" -> FpsChoice.FPS_90
                                "120 fps" -> FpsChoice.FPS_120
                                else -> FpsChoice.KEEP
                            }
                            onChange(settings.copy(fps = fps))
                        },
                    )
                    AuroraDropdownRow(
                        label = "Encoding",
                        value = settings.preset.name.lowercase().replaceFirstChar { it.uppercase() },
                        options = EncodingPreset.entries.map {
                            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                        },
                        onSelect = {
                            val preset = EncodingPreset.valueOf(it.uppercase())
                            onChange(settings.copy(preset = preset))
                        },
                    )
                    AuroraDropdownRow(
                        label = "Profile",
                        value = settings.profile.name,
                        options = H264Profile.entries.map { it.name },
                        onSelect = { onChange(settings.copy(profile = H264Profile.valueOf(it))) },
                    )
                    AuroraDropdownRow(
                        label = "GOP",
                        value = "${settings.gopSeconds}s",
                        options = listOf("1s", "2s", "5s", "10s"),
                        onSelect = { onChange(settings.copy(gopSeconds = it.replace("s", "").toInt())) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CrfSlider(crf: Float, onPreview: (Float) -> Unit, onCommit: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Quality (CRF ${crf.toInt()})", color = AuroraTextPrimary)
            Text(qualityLabel(crf.toInt()), color = AuroraTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = crf,
            onValueChange = onPreview,
            onValueChangeFinished = onCommit,
            valueRange = 0f..51f,
            colors = SliderDefaults.colors(
                thumbColor = AuroraViolet,
                activeTrackColor = AuroraViolet,
                inactiveTrackColor = AuroraBorder,
            ),
        )
    }
}

@Composable
private fun BitrateSlider(bitrate: Float, onPreview: (Float) -> Unit, onCommit: () -> Unit) {
    Column {
        Text("Bitrate ${bitrate.toInt()} kbps", color = AuroraTextPrimary)
        Slider(
            value = bitrate,
            onValueChange = onPreview,
            onValueChangeFinished = onCommit,
            valueRange = 100f..20000f,
            colors = SliderDefaults.colors(
                thumbColor = AuroraViolet,
                activeTrackColor = AuroraViolet,
                inactiveTrackColor = AuroraBorder,
            ),
        )
    }
}

private fun qualityLabel(crf: Int): String = when {
    crf <= 18 -> "Lossless"
    crf <= 23 -> "High quality"
    crf <= 28 -> "Medium"
    crf <= 35 -> "Low size"
    else -> "Smallest"
}
```

- [ ] **Step 4: Write `AudioSection.kt`**

```kotlin
package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.SectionHeader
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.ui.configuring.internal.AuroraDropdownRow
import com.videoeditor.feature.compress.ui.configuring.internal.SegmentedToggleChannels

@Composable
fun AudioSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onToggle) {
        Column {
            SectionHeader(
                title = "Audio",
                summary = "AAC \u00b7 ${settings.audio.bitrateKbps}k \u00b7 ${settings.audio.channels.name.lowercase()}",
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AuroraDropdownRow(
                        label = "Bitrate",
                        value = "${settings.audio.bitrateKbps} kbps",
                        options = listOf("64 kbps", "96 kbps", "128 kbps", "192 kbps", "256 kbps"),
                        onSelect = {
                            val bitrate = it.replace(" kbps", "").toInt()
                            onChange(settings.copy(audio = settings.audio.copy(bitrateKbps = bitrate)))
                        },
                    )
                    SegmentedToggleChannels(
                        channels = settings.audio.channels,
                        onSelect = { onChange(settings.copy(audio = settings.audio.copy(channels = it))) },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: Write `AdvancedSection.kt`**

```kotlin
package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.SectionHeader
import com.videoeditor.core.theme.AuroraBorder
import com.videoeditor.core.theme.AuroraCyan
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.CompressionSettings

@Composable
fun AdvancedSection(
    settings: CompressionSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (CompressionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), onClick = onToggle) {
        Column {
            SectionHeader(
                title = "Advanced",
                summary = if (settings.useHardwareAccel) "Hardware accel on" else "Software only",
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text("Hardware acceleration", color = AuroraTextPrimary)
                        Text(
                            text = if (settings.useHardwareAccel) "Uses HW encoder when available"
                                else "Software encoding only",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuroraTextSecondary,
                        )
                    }
                    Switch(
                        checked = settings.useHardwareAccel,
                        onCheckedChange = { onChange(settings.copy(useHardwareAccel = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AuroraCyan,
                            checkedTrackColor = AuroraCyan.copy(alpha = 0.5f),
                            uncheckedThumbColor = AuroraTextSecondary,
                            uncheckedTrackColor = AuroraBorder,
                        ),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 6: Write `OutputEstimateBar.kt`**

```kotlin
package com.videoeditor.feature.compress.ui.configuring

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.OutputEstimate
import java.text.DecimalFormat

@Composable
fun OutputEstimateBar(
    estimate: OutputEstimate,
    sourceSizeBytes: Long,
    onCompress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val df = DecimalFormat("#.#")
    val targetMb = (estimate.sizeBytes / (1024.0 * 1024.0)).toFloat()
    val animatedMb by animateFloatAsState(targetMb, label = "est-mb")

    GlassCard(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estimated",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraTextSecondary,
                )
                Text(
                    text = "${df.format(animatedMb)} MB",
                    style = MaterialTheme.typography.titleMedium,
                    color = AuroraTextPrimary,
                )
                Text(
                    text = "was ${df.format(sourceSizeBytes / (1024.0 * 1024.0))} MB \u00b7 \u00d7${df.format(estimate.ratio)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraTextSecondary,
                )
            }
            GradientButton(
                text = "Compress",
                onClick = onCompress,
                modifier = Modifier,
            )
        }
    }
}
```

- [ ] **Step 7: Replace the orchestrator `ConfiguringStep.kt`**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videoeditor.feature.compress.model.CompressUiState
import com.videoeditor.feature.compress.model.CompressionSettings
import com.videoeditor.feature.compress.model.SectionId
import com.videoeditor.feature.compress.model.SmartPreset
import com.videoeditor.feature.compress.ui.configuring.AdvancedSection
import com.videoeditor.feature.compress.ui.configuring.AudioSection
import com.videoeditor.feature.compress.ui.configuring.OutputEstimateBar
import com.videoeditor.feature.compress.ui.configuring.ResolutionPresetRow
import com.videoeditor.feature.compress.ui.configuring.VideoHeroCard
import com.videoeditor.feature.compress.ui.configuring.VideoSection

@Composable
fun ConfiguringStep(
    state: CompressUiState.Configuring,
    onPickDifferent: () -> Unit,
    onSmartPreset: (SmartPreset) -> Unit,
    onSettingsChanged: (CompressionSettings) -> Unit,
    onSectionToggle: (SectionId) -> Unit,
    onStartEncode: () -> Unit,
) {
    @Suppress("UNUSED_PARAMETER") val _smart = onSmartPreset
    @Suppress("UNUSED_PARAMETER") val _pickAgain = onPickDifferent

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VideoHeroCard(source = state.source)
            ResolutionPresetRow(
                selected = state.settings.resolution,
                sourceHeight = state.source.heightPx,
                onSelect = { onSettingsChanged(state.settings.copy(resolution = it)) },
            )
            VideoSection(
                settings = state.settings,
                expanded = state.expandedSections.contains(SectionId.VIDEO),
                onToggle = { onSectionToggle(SectionId.VIDEO) },
                onChange = onSettingsChanged,
            )
            AudioSection(
                settings = state.settings,
                expanded = state.expandedSections.contains(SectionId.AUDIO),
                onToggle = { onSectionToggle(SectionId.AUDIO) },
                onChange = onSettingsChanged,
            )
            AdvancedSection(
                settings = state.settings,
                expanded = state.expandedSections.contains(SectionId.ADVANCED),
                onToggle = { onSectionToggle(SectionId.ADVANCED) },
                onChange = onSettingsChanged,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            OutputEstimateBar(
                estimate = state.estimate,
                sourceSizeBytes = state.source.sizeBytes,
                onCompress = onStartEncode,
            )
        }
    }
}
```

The previous `ConfiguringStep.kt` exported many helpers (`VideoPreviewCard`, `ResolutionPresetChips`, `VideoSection`, `AudioSection`, `AdvancedSection`, `RateControlToggle`, `ChannelsToggle`, `DropdownRow`, `OutputPreviewBar`, `formatDuration`, `qualityLabel`). All callers were inside the same file, so removing them is safe. The orchestrator above does not reference any of them.

- [ ] **Step 8: Build, install, visually verify**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Expected:
- Pick a video, transition to Configure.
- Hero glass card with stat pills.
- Resolution preset chips (with "Keep" + 480p–4K) — disabled when source is too small.
- Three glass sections (Video / Audio / Advanced) with summary chips and expand/collapse.
- Sticky bottom estimate bar with "Compress" gradient button.
- Tapping Compress starts encoding.

- [ ] **Step 9: Commit**

```bash
./gradlew spotlessApply
git add app/src/main/kotlin/com/videoeditor/feature/compress/ui/ConfiguringStep.kt \
        app/src/main/kotlin/com/videoeditor/feature/compress/ui/configuring/
git commit -m "feat(compress): split ConfiguringStep into Aurora glass sections"
```

---

## Task 23: RunningStep redesign

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/RunningStep.kt`

- [ ] **Step 1: Replace `RunningStep.kt`**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.AuroraProgressRing
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.designsys.GradientButtonVariant
import com.videoeditor.core.designsys.StatPill
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.EncodeProgress
import java.text.DecimalFormat

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RunningStep(
    progress: EncodeProgress,
    onCancel: () -> Unit,
) {
    val df = DecimalFormat("#.#")
    val percent = (progress.percent * 100).toInt()
    val etaText = progress.etaSeconds?.let { eta ->
        val mins = eta / 60
        val secs = eta % 60
        "ETA %d:%02d".format(mins, secs)
    } ?: "Calculating ETA…"

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Compressing",
            style = MaterialTheme.typography.headlineSmall,
            color = AuroraTextPrimary,
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuroraProgressRing(fraction = progress.percent.toFloat(), diameter = 240.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.displayLarge.copy(brush = AuroraGradients.horizontal()),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = etaText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextSecondary,
                )
            }
        }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatPill(label = "Frame", value = progress.frame.toString())
                StatPill(label = "Speed", value = "${df.format(progress.fps)} fps")
                progress.etaSeconds?.let {
                    StatPill(label = "Remaining", value = "${it}s")
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            text = "Cancel",
            onClick = onCancel,
            variant = GradientButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AuroraTextPrimary,
                )
            },
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
```

- [ ] **Step 2: Build, install, visually verify**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Expected: encoding shows a big circular gradient ring, animated percent in the center, stat pills below, ghost cancel button at bottom. Background is paused (static) to keep CPU free for FFmpeg.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add app/src/main/kotlin/com/videoeditor/feature/compress/ui/RunningStep.kt
git commit -m "feat(compress): Aurora RunningStep with progress ring"
```

---

## Task 24: DoneStep redesign + count-up animation

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/DoneStep.kt`

- [ ] **Step 1: Replace `DoneStep.kt`**

```kotlin
package com.videoeditor.feature.compress.ui

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.designsys.GradientButtonVariant
import com.videoeditor.core.designsys.GradientIconBadge
import com.videoeditor.core.designsys.StatPill
import com.videoeditor.core.theme.AuroraGradients
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.feature.compress.model.SavedOutput
import java.text.DecimalFormat
import androidx.compose.runtime.LaunchedEffect

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DoneStep(
    output: SavedOutput,
    ratio: Double,
) {
    val context = LocalContext.current
    val df = DecimalFormat("#.#")
    val savedPercent = ((1.0 - 1.0 / ratio.coerceAtLeast(1.0001)) * 100).toFloat().coerceIn(0f, 99f)

    var animateTo by remember { mutableStateOf(0f) }
    LaunchedEffect(savedPercent) { animateTo = savedPercent }
    val animated by animateFloatAsState(
        targetValue = animateTo,
        label = "saved-count-up",
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        GradientIconBadge(icon = Icons.Default.Check, size = 84.dp)
        Text(
            text = "Compressed",
            style = MaterialTheme.typography.displayMedium.copy(brush = AuroraGradients.horizontal()),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Saved ${animated.toInt()}%",
            style = MaterialTheme.typography.displayLarge.copy(brush = AuroraGradients.diagonal()),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val newMb = output.sizeBytes / (1024.0 * 1024.0)
            val originalMb = newMb * ratio
            StatPill(label = "Original", value = "${df.format(originalMb)} MB")
            StatPill(label = "New", value = "${df.format(newMb)} MB")
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientButton(
                text = "Open in Photos",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(output.uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                },
                variant = GradientButtonVariant.Filled,
                modifier = Modifier.weight(1f),
            )
            GradientButton(
                text = "Share",
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, output.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                },
                variant = GradientButtonVariant.Ghost,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Saved to Photos › VideoEditor",
            style = MaterialTheme.typography.bodySmall,
            color = AuroraTextSecondary,
        )
    }
}
```

Note on the Saved% formula: legacy `ratio` is original ÷ new (so ratio > 1 means saved space). `saved% = 1 − new/original = 1 − 1/ratio`. The `coerceAtLeast(1.0001)` guards against ratio ≤ 1 (no-op encode).

The DoneStep spec mentioned a "particle burst" celebration — for first cut we keep the gradient count-up only and skip particles to keep this task small. Particles can come as a follow-up if desired.

- [ ] **Step 2: Build, install, visually verify**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Expected: after a successful compression, gradient checkmark badge, gradient "Compressed" headline, big "Saved X%" number that animates from 0 up. Two side-by-side StatPills. Gradient "Open in Photos" + ghost "Share" buttons.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add app/src/main/kotlin/com/videoeditor/feature/compress/ui/DoneStep.kt
git commit -m "feat(compress): Aurora DoneStep with count-up Saved %"
```

---

## Task 25: FailedStep — replace stub with Aurora UI

**Files:**
- Modify: `app/src/main/kotlin/com/videoeditor/feature/compress/ui/FailedStep.kt`

- [ ] **Step 1: Replace `FailedStep.kt`**

```kotlin
package com.videoeditor.feature.compress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videoeditor.core.designsys.GlassCard
import com.videoeditor.core.designsys.GradientButton
import com.videoeditor.core.designsys.GradientButtonVariant
import com.videoeditor.core.designsys.GradientIconBadge
import com.videoeditor.core.theme.AuroraError
import com.videoeditor.core.theme.AuroraTextPrimary
import com.videoeditor.core.theme.AuroraTextSecondary
import com.videoeditor.core.theme.AuroraWarning

@Composable
fun FailedStep(
    reason: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GradientIconBadge(
                    icon = Icons.Default.WarningAmber,
                    size = 72.dp,
                    brush = Brush.linearGradient(listOf(AuroraWarning, AuroraError)),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AuroraTextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GradientButton(
                        text = "Try again",
                        onClick = onRetry,
                        variant = GradientButtonVariant.Filled,
                        modifier = Modifier.weight(1f),
                    )
                    GradientButton(
                        text = "Dismiss",
                        onClick = onDismiss,
                        variant = GradientButtonVariant.Ghost,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add app/src/main/kotlin/com/videoeditor/feature/compress/ui/FailedStep.kt
git commit -m "feat(compress): Aurora FailedStep glass card"
```

Expected: `BUILD SUCCESSFUL`. Visually verifiable by force-killing FFmpeg mid-encode (manual smoke).

---

## Task 26: Remove legacy SectionCard and deprecated aliases

**Files:**
- Delete: `app/src/main/kotlin/com/videoeditor/core/designsys/SectionCard.kt`
- Modify: `app/src/main/kotlin/com/videoeditor/core/theme/Color.kt`
- Modify: `app/src/main/kotlin/com/videoeditor/core/theme/Type.kt`
- Modify: `app/src/main/kotlin/com/videoeditor/core/theme/Motion.kt`

- [ ] **Step 1: Verify no callers reference legacy symbols**

```bash
rg --no-heading -n "SectionCard\b|IndigoSeed|AccentTeal|SurfaceLight|SurfaceDark|AppTypography|MotionSpec" app/src
```

Expected output: only references inside the four files being deleted/modified in this task. If any other file shows up, fix it (replace with the Aurora equivalent) before continuing.

- [ ] **Step 2: Delete `SectionCard.kt`**

```bash
git rm app/src/main/kotlin/com/videoeditor/core/designsys/SectionCard.kt
```

- [ ] **Step 3: Strip deprecated aliases from `Color.kt`**

Replace `Color.kt` with the no-aliases version (drop the four `@Deprecated` lines):

```kotlin
package com.videoeditor.core.theme

import androidx.compose.ui.graphics.Color

val AuroraBgBase = Color(0xFF070712)
val AuroraSurface1 = Color(0xFF0F0E1F)
val AuroraSurface2 = Color(0xFF171430)

val AuroraViolet = Color(0xFF7C3AED)
val AuroraMagenta = Color(0xFFEC4899)
val AuroraCyan = Color(0xFF22D3EE)

val AuroraTextPrimary = Color(0xFFF4F3FF)
val AuroraTextSecondary = Color(0xFFA8A4C7)
val AuroraTextMuted = Color(0xFF5B5778)

val AuroraBorder = Color(0xFF2A2540)

val AuroraSuccess = Color(0xFF34D399)
val AuroraWarning = Color(0xFFFBBF24)
val AuroraError = Color(0xFFF87171)
```

- [ ] **Step 4: Strip deprecated `AppTypography` from `Type.kt`**

Remove the trailing two lines:

```kotlin
@Deprecated("Use AuroraTypography", ReplaceWith("AuroraTypography"))
val AppTypography = AuroraTypography
```

- [ ] **Step 5: Strip deprecated `MotionSpec` from `Motion.kt`**

Remove the trailing block:

```kotlin
@Deprecated("Use AuroraMotion", ReplaceWith("AuroraMotion"))
val MotionSpec = object {
    val press = AuroraMotion.pressSpring
}
```

- [ ] **Step 6: Build and commit**

```bash
./gradlew spotlessApply assembleDebug
git add -A app/src/main/kotlin/com/videoeditor/core/
git commit -m "chore: drop legacy SectionCard and deprecated theme aliases"
```

Expected: `BUILD SUCCESSFUL`.

---

## Task 27: Final visual smoke + lint pass

**Files:** none

- [ ] **Step 1: Spotless and lint**

```bash
./gradlew spotlessApply
./gradlew assembleDebug lintDebug
```

Expected: build + lint succeed. Note any new lint warnings introduced and fix them inline.

- [ ] **Step 2: Install and walk through every screen**

```bash
./gradlew installDebug
adb shell am start -n com.videoeditor/.MainActivity
```

Run through this checklist on the device/emulator:

- [ ] Home: animated aurora background, gradient title, glass cards, "Soon" pills on disabled cards, FAB visible with glow.
- [ ] FAB → Compress: animated horizontal slide transition.
- [ ] Pick step: gradient circular icon, gradient title, full-width "Choose video" button.
- [ ] Pick a video → Configure step transitions in.
- [ ] Configure: glass hero card with stat pills; resolution chips toggle; three sections expand/collapse; sticky bottom estimate bar with gradient Compress button.
- [ ] Tap Compress → Run step transitions in; background animation pauses; gradient progress ring fills; stat pills update with frame/fps; ghost Cancel works.
- [ ] On finish → Done step transitions in; gradient check badge; "Saved X%" count-up animates; both buttons functional.
- [ ] Back from Home returns to Home with reverse slide.
- [ ] Force-kill FFmpeg (`adb shell am force-stop com.videoeditor` mid-encode is a coarse trigger; or simulate by passing an unreadable URI) → Failed step shows gradient warning badge with both buttons.

- [ ] **Step 3: Test on API 26 emulator (glass fallback)**

```bash
$ANDROID_HOME/emulator/emulator -avd <api26-avd> &
./gradlew installDebug
```

Expected: glass cards render without blur but with gradient hairline borders; nothing crashes.

- [ ] **Step 4: Commit any final fixes from the checklist**

If the smoke pass found issues, fix them and commit. If clean, finish with:

```bash
git commit --allow-empty -m "chore: finish aurora UI refactor smoke pass"
```

- [ ] **Step 5: Push the feature branch**

```bash
git push -u origin feat/aurora-ui-refactor
```

The PR can then be opened by the user (or via `gh pr create` if requested).

---

## Coverage map (spec → tasks)

| Spec section | Implemented by |
|---|---|
| 3.1 Palette | Task 1, Task 26 |
| 3.2 Gradients | Task 2 |
| 3.3 Typography | Task 4, Task 26 |
| 3.4 Motion | Task 5, Task 26 |
| 3.5 Glassmorphism + API fallback | Task 3 |
| 4. Design system components | Tasks 7–16 |
| 5. Theme integration | Task 6 |
| 6.1 HomeScreen | Task 18 |
| 6.2 CompressScreen shell | Task 19 |
| 6.3 PickStep | Task 20 |
| 6.4 ConfiguringStep split | Tasks 21–22 |
| 6.5 RunningStep | Task 23 |
| 6.6 DoneStep | Task 24 |
| 6.7 Failed state | Tasks 19 (stub) + 25 |
| 6.8 Navigation transitions | Task 17 |
| 7. File map cleanup | Task 26 |
| 9. Verification | Task 27 |
