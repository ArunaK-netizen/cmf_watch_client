package com.cmfwatch.companion.ui.theme

import androidx.compose.ui.graphics.Color

// CMF Watch Pro — HTML design tokens
val CmfBackground = Color(0xFFF3F3F6)
val CmfSurface = Color(0xFFF3F3F6)
val CmfOnSurface = Color(0xFF1A1C1E)
val CmfOnSurfaceVariant = Color(0xFF5B4039)
val CmfOutline = Color(0xFF907067)
val CmfOutlineVariant = Color(0xFFE4BEB4)

val CmfSurfaceLowest = Color(0xFFFFFFFF)
val CmfSurfaceLow = Color(0xFFF3F3F6)
val CmfSurfaceContainer = Color(0xFFEEEEF0)
val CmfSurfaceHigh = Color(0xFFE8E8EA)
val CmfSurfaceHighest = Color(0xFFE2E2E5)

val CmfPrimary = Color(0xFFB02F00)
val CmfPrimaryContainer = Color(0xFFFF5722)
val CmfPrimaryFixed = Color(0xFFFFDBD1)
val CmfOnPrimary = Color(0xFFFFFFFF)
val CmfOnPrimaryContainer = Color(0xFF541200)

val CmfSecondary = Color(0xFF4C4ACA)
val CmfSecondaryContainer = Color(0xFF6664E4)
val CmfSecondaryFixed = Color(0xFFE2DFFF)
val CmfSecondaryFixedDim = Color(0xFFC2C1FF)
val CmfOnSecondary = Color(0xFFFFFFFF)

val CmfTertiary = Color(0xFF006B27)
val CmfTertiaryContainer = Color(0xFF008733)
val CmfTertiaryFixed = Color(0xFF72FE88)
val CmfOnTertiary = Color(0xFFFFFFFF)
val CmfOnTertiaryFixed = Color(0xFF002107)
val CmfOnErrorContainer = Color(0xFF93000A)
val CmfOnSecondaryFixedVariant = Color(0xFF3631B4)

val CmfError = Color(0xFFBA1A1A)
val CmfErrorContainer = Color(0xFFFFDAD6)

// Legacy aliases — mapped onto the new light palette
val LightBackground = CmfBackground
val BlackBackground = CmfBackground

val SurfaceWhite = CmfSurfaceLowest
val SurfaceCard = CmfSurfaceLowest
val SurfaceCardAlt = CmfSurfaceLow
val DarkCardSurface = CmfSurfaceLowest
val DarkCardVariant = CmfSurfaceLow

val TextPrimary = CmfOnSurface
val TextSecondary = CmfOnSurfaceVariant
val TextMuted = Color(0xFF907067)
val DividerColor = CmfOutlineVariant

val RingGreen = CmfTertiaryContainer
val RingGreenBg = Color(0x1A008733)
val RingOrange = CmfPrimaryContainer
val RingOrangeBg = Color(0x1AFF5722)
val RingBlue = CmfSecondaryContainer
val RingBlueBg = Color(0x1A6664E4)

val HeartRateRed = CmfError
val HeartRateBg = CmfErrorContainer
val SleepPurple = CmfSecondary
val SleepPurpleBg = Color(0x1A4C4ACA)
val SleepDeep = Color(0xFF3631B4)
val SleepCore = CmfSecondaryContainer
val SleepREM = Color(0xFFC2C1FF)

val SpO2Red = CmfSecondaryContainer
val RespBlue = CmfSecondaryContainer
val StressGreen = CmfTertiary

val AccentRed = CmfPrimaryContainer
val AccentActivity = CmfTertiaryContainer
val AccentStressLow = CmfTertiary
val AccentSpO2 = CmfSecondaryContainer
val AccentSleepREM = SleepREM
val AccentSleepLight = SleepCore
val AccentSleepDeep = SleepDeep
val AccentHeartRate = HeartRateRed
val AccentSleepAwake = CmfPrimaryContainer
