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
