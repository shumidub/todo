package com.shumidub.todoapprealm.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root Compose theme for the migrated UI. Wraps Material 3 with a colour scheme derived
 * from the active [TabPalette], publishes that palette via [LocalTabPalette], and tints
 * the status / navigation bars to match the tab — replacing the reactive bar recolouring
 * the legacy `BaseActivity` did imperatively.
 */
@Composable
fun Todo100Theme(
    palette: TabPalette = DefaultPalette,
    content: @Composable () -> Unit,
) {
    val colorScheme = lightColorScheme(
        primary = palette.accent,
        onPrimary = Color.White,
        secondary = palette.accent,
        background = palette.bg,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.inputText,
        surfaceVariant = palette.surfaceMuted,
        onSurfaceVariant = palette.textSoft,
        outline = palette.divider,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bar = palette.systemBar.toArgb()
            @Suppress("DEPRECATION")
            window.statusBarColor = bar
            @Suppress("DEPRECATION")
            window.navigationBarColor = bar
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = palette.darkSystemIcons
                isAppearanceLightNavigationBars = palette.darkSystemIcons
            }
        }
    }

    CompositionLocalProvider(LocalTabPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
