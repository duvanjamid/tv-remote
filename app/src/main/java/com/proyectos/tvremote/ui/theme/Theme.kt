package com.proyectos.tvremote.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SeedBlueDim = Color(0xFF9FBFFF)
private val Accent = Color(0xFF00D1B2)

// Fallback palette for pre-Android-12 devices without Material You dynamic color.
private val FallbackDark = darkColorScheme(
    primary = SeedBlueDim,
    secondary = Accent,
)
private val FallbackLight = lightColorScheme(
    primary = Color(0xFF3D7BFF),
    secondary = Accent,
)

/**
 * Uses the phone's own Material You palette (same colors/theme as the rest of
 * Android - derived from the wallpaper on Pixel/Android 12+) so the app looks
 * native instead of a fixed custom color scheme. Falls back to a plain
 * light/dark scheme on older Android versions that don't support it.
 */
@Composable
fun TvRemoteTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) FallbackDark else FallbackLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
