package com.shumidub.todoapprealm.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Kotlin port of [com.shumidub.todoapprealm.ui.theme.Palette] (the Java 9-token colour
 * set) for the Compose UI. Values mirror res/values/colors.xml exactly so the migrated
 * screens stay pixel-faithful to the legacy fragments.
 *
 * Task groups map to palettes: 0 = Default green chrome (Tasks1), 1 = Cornflower (Tasks2),
 * 2 = Mimosa/Canary (Tasks3), 3 = Indigo (Notes). See [paletteForGroup].
 *
 * `systemBar` / `darkSystemIcons` drive the per-tab status- and navigation-bar tint,
 * replacing the reactive bar recolouring the legacy BaseActivity did in code.
 */
@Immutable
data class TabPalette(
    val bg: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val text: Color,
    val textSoft: Color,
    val inputText: Color,
    val counter: Color,
    val accent: Color,
    val divider: Color,
    val systemBar: Color,
    val darkSystemIcons: Boolean,
)

/** Default green chrome — group 0 (Tasks1). Derived from colorBackgroundActivity / colorAccent. */
val DefaultPalette = TabPalette(
    bg = Color(0xFF599C74),
    surface = Color(0xFFECF5EF),
    surfaceMuted = Color(0xFF4F8A68),
    text = Color(0xFFF2F8F4),
    textSoft = Color(0xBCF2F8F4),
    inputText = Color(0xFF16331F),
    counter = Color(0xFF7CA890),
    accent = Color(0xFFE47C5D),
    divider = Color(0x2EF2F8F4),
    systemBar = Color(0xFF267C52), // colorPrimaryDark
    darkSystemIcons = false,
)

/** Cornflower — group 1 (Tasks2). */
val CornflowerPalette = TabPalette(
    bg = Color(0xFF5C7CC0),
    surface = Color(0xFFEEF1F8),
    surfaceMuted = Color(0xFF5274B7),
    text = Color(0xFFF2F4FA),
    textSoft = Color(0xBCF2F4FA),
    inputText = Color(0xFF1C2952),
    counter = Color(0xFF7889B0),
    accent = Color(0xFFE8B85C),
    divider = Color(0x2EF2F4FA),
    systemBar = Color(0xFF5C7CC0),
    darkSystemIcons = false,
)

/** Mimosa/Canary — group 2 (Tasks3). Light background → dark system icons. */
val CanaryPalette = TabPalette(
    bg = Color(0xFFF3C551),
    surface = Color(0xFFF6F0E5),
    surfaceMuted = Color(0xFFEAB63E),
    text = Color(0xFF2E2406),
    textSoft = Color(0xA82E2406),
    inputText = Color(0xFF2E2406),
    counter = Color(0xFF94802E),
    accent = Color(0xFFDF5C55),
    divider = Color(0x242E2406),
    systemBar = Color(0xFFF3C551),
    darkSystemIcons = true,
)

/** Indigo — group 3 (Notes tab). */
val IndigoPalette = TabPalette(
    bg = Color(0xFF3D52A0),
    surface = Color(0xFFECEEF8),
    surfaceMuted = Color(0xFF34468F),
    text = Color(0xFFF2F4FA),
    textSoft = Color(0xBCF2F4FA),
    inputText = Color(0xFF161F45),
    counter = Color(0xFF6E7CB2),
    accent = Color(0xFFF4A742),
    divider = Color(0x2EF2F4FA),
    systemBar = Color(0xFF3D52A0),
    darkSystemIcons = false,
)

/**
 * Palette for a task group, mirroring [com.shumidub.todoapprealm.ui.theme.Palette.forGroup].
 * Group 0 (and any unmapped value) gets the default green chrome instead of null.
 */
fun paletteForGroup(group: Int): TabPalette = when (group) {
    1 -> CornflowerPalette
    2 -> CanaryPalette
    3 -> IndigoPalette
    else -> DefaultPalette
}

/** Ambient palette for the currently displayed tab. Provided by [Todo100Theme]. */
val LocalTabPalette = staticCompositionLocalOf { DefaultPalette }
