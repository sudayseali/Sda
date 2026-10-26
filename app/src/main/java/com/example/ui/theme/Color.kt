package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Studio Palette
val RecordRed = Color(0xFFFF334B)
val RecordRedDark = Color(0xFFD61831)
val RecordRedGlow = Color(0x40FF334B)
val StudioDarkBg = Color(0xFF0F1015)
val StudioSurface = Color(0xFF181A22)
val StudioSurfaceVariant = Color(0xFF222430)
val StudioCardBorder = Color(0xFF2C2F3F)

val AccentCyan = Color(0xFF00E5FF)
val AccentGreen = Color(0xFF00E676)
val AccentAmber = Color(0xFFFFB300)

val TextPrimary = Color(0xFFF5F6FA)
val TextSecondary = Color(0xFFA0A5B5)
val TextTertiary = Color(0xFF6F7487)

val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = RecordRed,
    onPrimary = Color.White,
    primaryContainer = RecordRedDark,
    onPrimaryContainer = Color.White,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = StudioSurfaceVariant,
    onSecondaryContainer = AccentCyan,
    tertiary = AccentAmber,
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StudioCardBorder,
    error = RecordRed
)

val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = RecordRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = RecordRedDark,
    secondary = Color(0xFF00838F),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF14151B),
    surface = Color.White,
    onSurface = Color(0xFF14151B),
    surfaceVariant = Color(0xFFEBEFF5),
    onSurfaceVariant = Color(0xFF4A4E5C),
    outline = Color(0xFFCFD5E2),
    error = RecordRedDark
)
