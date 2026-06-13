# Theme-Palette Subsystem Specification

**Scope:** Port `ui/theme/Palette.java` + color resources from Android XML to Kotlin data class + Jetpack Compose + Material 3.

**Date:** 2026-06-13  
**Revision:** 1.0

---

## 1. Current Behavior

### 1.1 Architecture Overview

The theme-palette system is a **per-tab color scheme** that unifies 9 color tokens across four groups:
- Group 0: Tasks1 (default chrome; null palette → use global resources)
- Group 1: Tasks2 / Cornflower (blue palette)
- Group 2: Tasks3 / Canary (yellow palette)
- Group 3: Notes / Indigo (dark blue palette)

**Single source of truth:** `Palette.java` (lines 1–87) is the only programmatic color abstraction. All callers hold a single `Palette` instance instead of branching on group IDs.

### 1.2 The Nine Tokens

Each palette holds exactly 9 color fields:

| Token | Purpose | Example (Cornflower) |
|-------|---------|----------------------|
| `bg` | Background (used for action bar + status/nav bar) | `#5C7CC0` |
| `surface` | Card/surface backgrounds | `#EEF1F8` |
| `surfaceMuted` | Slightly darker surface (e.g., dialog bg) | `#5274B7` |
| `text` | Primary text on surfaces | `#F2F4FA` |
| `textSoft` | Secondary text (with alpha transparency) | `#BCF2F4FA` |
| `inputText` | Text inside input fields | `#1C2952` |
| `counter` | Counter/badge text (section counts) | `#7889B0` |
| `accent` | Primary accent (buttons, highlights, stroke) | `#E8B85C` |
| `divider` | Line dividers (with alpha transparency) | `#2EF2F4FA` |

### 1.3 Per-Group Hex Values

**Cornflower (Group 1, Tasks2)**
```
bg:              #5C7CC0
surface:         #EEF1F8
surfaceMuted:    #5274B7
text:            #F2F4FA
textSoft:        #BCF2F4FA (includes alpha: BC = ~74% opaque)
inputText:       #1C2952
counter:         #7889B0
accent:          #E8B85C
divider:         #2EF2F4FA (includes alpha: 2E = ~18% opaque)
```

**Canary (Group 2, Tasks3)**
```
bg:              #FFD93D
surface:         #FFFCEA
surfaceMuted:    #F2CC30
text:            #2E2406
textSoft:        #A82E2406 (includes alpha: A8 = ~66% opaque)
inputText:       #2E2406
counter:         #94802E
accent:          #D7305C
divider:         #242E2406 (includes alpha: 24 = ~14% opaque)
```

**Indigo (Group 3, Notes tab)**
```
bg:              #3D52A0
surface:         #ECEEF8
surfaceMuted:    #34468F
text:            #F2F4FA
textSoft:        #BCF2F4FA (includes alpha: BC = ~74% opaque)
inputText:       #161F45
counter:         #6E7CB2
accent:          #F4A742
divider:         #2EF2F4FA (includes alpha: 2E = ~18% opaque)
```

**Dialog Default (Group 0 / null palette fallback)**  
Assembled from individual color resources at runtime (Palette.java:62–70):
```
dialogSurface:   #1F4F38 (from colorDialogSurface)
onSurface:       #F1F8F4 (from colorDialogOnSurface)
onSurfaceVariant: #C7E0D1 (from colorDialogOnSurfaceVariant)
white:           #ffffff (from colorWhite)
accent:          #e47c5d (from colorAccent)
```
Maps to Palette fields:
```
bg = dialogSurface
surface = dialogSurface
surfaceMuted = dialogSurface
text = onSurface
textSoft = onSurfaceVariant
inputText = white
counter = white
accent = colorAccent
divider = onSurfaceVariant
```

### 1.4 Color Resource Layer

**File:** `app/src/main/res/values/colors.xml` (lines 95–134)

All Palette tokens are backed by XML color definitions, grouped by palette:
- Cornflower: 9 colors (`cornflowerBg`, `cornflowerSurface`, ..., `cornflowerDivider`)
- Canary: 9 colors (`canaryBg`, `canarySurface`, ..., `canaryDivider`)
- Indigo: 9 colors (`indigoBg`, `indigoSurface`, ..., `indigoDivider`)
- Dialog: 5 colors (`colorDialogSurface`, `colorDialogOnSurface`, `colorDialogOnSurfaceVariant`, `colorDialogStroke`, `colorDialogScrim`)

**File:** `app/src/main/res/values/styles.xml` (lines 1–187)

Style layer applies palettes via theme overlays:

**AppTheme (base, lines 4–16):**
- Sets `colorPrimary`, `colorPrimaryDark`, `colorAccent`, status/nav bar colors
- Applies `android:statusBarColor` = `@color/colorPrimaryDark` (green on default tab)
- Applies `android:navigationBarColor` = `@color/colorPrimaryDark`
- Sets `alertDialogTheme` (AppCompat legacy) and `materialAlertDialogTheme` (Material 3)

**ThemeOverlay.App.MaterialAlertDialog.Cornflower (lines 151–161):**
- Used when dialogs spawn from Tasks2 (Cornflower) tab
- Overrides `colorSurface` → `cornflowerSurfaceMuted`
- Overrides `colorOnSurface` → `cornflowerText`
- Overrides `colorAccent` → `cornflowerAccent`
- Overrides `colorPrimary` → `cornflowerAccent` (drives button text + TextInputLayout stroke)

**ThemeOverlay.App.MaterialAlertDialog.Canary (lines 164–173):**
- Used when dialogs spawn from Tasks3 (Canary) tab
- Same structure; swaps `canary*` colors

**ThemeOverlay.App.MaterialAlertDialog.Indigo (lines 176–185):**
- Used when dialogs spawn from Notes (Indigo) tab
- Same structure; swaps `indigo*` colors

**Theme.MaterialComponents.Light.DarkActionBar.Bridge (parent):**
- Bridge variant allows AppCompat widgets + Material 3 attributes to coexist
- Provides baseline material color tokens for dialog inflation

### 1.5 Programmatic Usage Points

**File:** `ui/theme/Palette.java` (lines 42–87)

**`Palette.forGroup(Context ctx, int group) -> Palette | null`**
- Line 43: Switch on group ID
- Lines 45–56: Load color resources via `ContextCompat.getColor()` for groups 1/2/3
- Line 57: Return null for group 0 (no themed palette; use default chrome)
- Called from:
  - `MainActivity.applyTabChrome()` (line 347): sets action bar + status/nav bar colors
  - `MainActivity.tintActionModeBarForCurrentTab()` (line 300): sets CAB background
  - `TaskEditorBottomSheet.onViewCreated()` (line 122): resolves palette for the bottom sheet
  - `TasksRecyclerViewAdapter.usePaletteForGroup()` (line 73): applies palette to task list
  - `FolderOfTaskRecyclerViewAdapter()` constructor (line 47): folder card list palette
  - `FolderSlidingPanelFragment.applyPalette()` (called during inflation): folder panel colors

**`Palette.dialogDefault(Context ctx) -> Palette`**
- Lines 62–70: Constructs a "default dialog" palette from individual color resources
- Used when `forGroup()` returns null (default Tasks1 tab)
- Called from `TaskEditorBottomSheet.onViewCreated()` (line 123): fallback if no group palette

### 1.6 Chrome & System Bar Tinting

**MainActivity.applyTabChrome(int position)** (lines 342–371)

Called on:
- `onCreate()` (line 216) with `START_PAGE = 1`
- `onPageSelected()` (line 205) during pager scroll

Behavior:
- If palette exists (group 1/2/3):
  - `rootLayout.setBackgroundColor(p.bg)` — entire background
  - `actionBar.setBackgroundDrawable(ColorDrawable(p.bg))` — action bar
  - `window.setStatusBarColor(p.bg)` — status bar
  - `window.setNavigationBarColor(p.bg)` — nav bar
  - **Line 355:** Special case for Canary (group 2): `setAppearanceLightStatusBars(true)` + `setAppearanceLightNavigationBars(true)` (dark icons on yellow)
- If no palette (group 0):
  - `rootLayout.setBackgroundColor(colorBackgroundActivity)` — green (#599c74)
  - `actionBar.setBackgroundDrawable(ColorDrawable(colorPrimary))` — green (#267c52)
  - Status/nav bars set to `colorPrimary` (green)
  - Light icons = false (white icons)

### 1.7 Dialog & Theme Overlay Integration

**MainActivity.dialogBuilder()** (lines 239–256)

Constructs `MaterialAlertDialogBuilder` with per-tab overlay theme:
- Position 2 (Tasks2 / Cornflower): `R.style.ThemeOverlay_App_MaterialAlertDialog_Cornflower`
- Position 3 (Tasks3 / Canary): `R.style.ThemeOverlay_App_MaterialAlertDialog_Canary`
- Position 4 (Notes / Indigo): `R.style.ThemeOverlay_App_MaterialAlertDialog_Indigo`
- Default: base `ThemeOverlay.App.MaterialAlertDialog`

**MainActivity.dialogContext()** (lines 263–281)

Wraps activity context with per-tab overlay for layout inflation:
- Same mapping as `dialogBuilder()`
- Used by dialog layouts to resolve `?attr/colorOnSurface`, `?attr/colorAccent`, etc. during inflation
- Ensures Material 3 color attributes resolve to the correct palette token

**TaskEditorBottomSheet.applyPalette()** (called during `onViewCreated()`, lines 122–123)

- Resolves palette from task group or falls back to `dialogDefault()`
- Applies palette colors to:
  - Bottom sheet root background
  - TextInputLayout stroke (via `tilText.setBoxStrokeColor(accent)`)
  - TextInputEditText text color
  - CheckBox colors
  - All text views (using `palette.text`, `palette.textSoft`, `palette.inputText`, `palette.counter`)

### 1.8 Adapter Usage

**TasksRecyclerViewAdapter** (lines 73–104)

- Method `usePaletteForGroup(int group)`: resolves palette, stores in `palette` field, calls `notifyDataSetChanged()`
- Helper methods:
  - `hasActivePalette()` → `palette != null`
  - `activeAccent()` → palette.accent or global colorAccent
  - `activeSurface()` → palette.surface (0 if null)
  - `activeInputText()` → palette.inputText (0 if null)
  - `activeCounter()` → palette.counter (0 if null)
- Used in `onBindViewHolder()` to set task item background, text colors, checkbox tint

**FolderOfTaskRecyclerViewAdapter**

- Constructor takes `taskGroup` parameter, resolves palette once
- Method `applyPaletteToCard()` applies palette to folder cards:
  - Card background → palette.surface
  - Text colors → palette.text / palette.textSoft
  - Accent elements (buttons, checkmarks) → palette.accent

### 1.9 Special Cases & Composables

**Tab Color Swatch Picker** (styles.xml lines 119–148)

Button styles for the color picker dialog:
- `Widget.App.Button.TabColorSwatch` — base
- `.Green` — `colorBackgroundActivity` background
- `.Blue` — `cornflowerBg` background
- `.Yellow` — `canaryBg` background, `canaryText` foreground (dark on yellow)
- `.Indigo` — `indigoBg` background

Maps directly to the 4 groups.

**Input Field Styling** (styles.xml lines 84–105)

- `Widget.App.TextInputLayout.Dialog` — focused stroke color resolves to `?attr/colorAccent`
- `Widget.App.EditText.Dialog` — text and hint colors via palette tokens
- `Widget.App.CheckBox.Dialog` — button tint via `?attr/colorAccent`

---

## 2. State & Data Flow

### 2.1 Lifecycle & Initialization

1. **App startup** → `MainActivity.onCreate()` (line 82)
   - Calls `onCreateActions()` (not shown, but infers pager setup)
   - Calls `applyTabChrome(START_PAGE = 1)` → loads Task1 palette (null) + sets green chrome
   
2. **Pager page change** → `ViewPager.OnPageChangeListener.onPageSelected()` (line 191)
   - Calls `applyTabChrome(position)` → reloads palette for new tab
   - Queries `Palette.forGroup(this, Tabs.groupForPosition(position))`
   - Updates action bar, status bar, nav bar, root layout background
   - Fragments on that page call `adapter.usePaletteForGroup(group)` (triggered separately, not shown in excerpt)

3. **Dialog spawn** → Fragment/Activity calls `MainActivity.dialogBuilder()` or `dialogContext()`
   - Queries current pager position
   - Wraps builder/context with per-tab overlay theme
   - Dialog layout inflation resolves color attributes against the overlay

4. **Bottom sheet open** → `TaskEditorBottomSheet.onViewCreated()` (line 113)
   - Queries `Palette.forGroup(requireContext(), taskGroup)` (line 122)
   - Falls back to `Palette.dialogDefault()` if null (line 123)
   - Calls `applyPalette()` (not fully shown) to bind palette to UI elements

### 2.2 State Storage & Mutability

- **No mutable state.** Palette is immutable (final fields, private constructor).
- **Read-only lookups.** `forGroup()` and `dialogDefault()` are static factory methods; each call allocates new `Palette` instance (no caching).
- **Per-fragment storage:** Adapters store reference to Palette in instance field (e.g., `TasksRecyclerViewAdapter.palette`, line 67).
- **Per-dialog storage:** `TaskEditorBottomSheet.palette` (line 54) holds the palette for the lifetime of the bottom sheet.

**Threading:**
- All color resource loading happens on the main thread via `ContextCompat.getColor(ctx, @ColorRes int)` (Palette.java:77–85).
- No background threads access Palette.
- No race conditions (read-only after construction).

### 2.3 Reactive Updates

**Page change flow (Canary yellow → Cornflower blue)**

1. User swipes pager to position 3 (Tasks2 / Cornflower)
2. `onPageSelected(3)` fires (MainActivity.java:191)
3. `applyTabChrome(3)` called (line 205)
4. `Palette.forGroup(this, Tabs.groupForPosition(3) = 1)` → Cornflower palette loaded
5. Action bar color → `#5C7CC0` (cornflowerBg)
6. Status bar color → `#5C7CC0`
7. Nav bar color → `#5C7CC0`
8. Light icons disabled (dark icons on blue)
9. Fragment on page 3 (SmallTasksFragment) calls `adapter.usePaletteForGroup(1)`
10. Adapter stores palette, calls `notifyDataSetChanged()`
11. RecyclerView items rebind; each task item background → `palette.surface` (#EEF1F8)

No explicit listener pattern; change detection driven by pager position.

---

## 3. Edge Cases & Gotchas

### 3.1 Null Palette (Group 0 / Default Tab)

**Case:** Tasks1 tab (group 0) has no custom palette; `Palette.forGroup(ctx, 0)` returns null.

**Impact:**
- `applyTabChrome()` enters else branch (line 359) and uses global resources: `colorPrimary` (#267c52), `colorBackgroundActivity` (#599c74)
- Adapters check `hasActivePalette()` and fallback to global `colorAccent` (#e47c5d)
- Text colors must be supplied from elsewhere (not palette tokens)

**Risk:** If group 0 logic is missed during Compose port, tab chrome will show hardcoded colors instead of reacting to palette state.

### 3.2 Canary Light Icons (Line 355)

**Case:** Canary tab (group 2) uses yellow background (#FFD93D) → system icons must be dark.

**Current code:**
```java
boolean light = Tabs.groupForPosition(position) == 2;
insets.setAppearanceLightStatusBars(light);
insets.setAppearanceLightNavigationBars(light);
```

**Gotcha:** `light = true` means "light background, use dark icons." Counter-intuitive naming. The setter is `setAppearanceLightStatusBars(boolean lightAppearance)`.

**Risk:** During Compose port, `statusBarDarkContentEnabled = true` for Canary only, or icon colors will be invisible.

### 3.3 Dialog Theme Wrapping

**Case:** Dialog builder wrapping (MainActivity.dialogBuilder(), line 239–256)

The pager position maps directly to overlay theme. If a dialog is spawned from a fragment but the pager scrolls before the dialog opens, the **pager position may have changed**, and the dialog will use the new tab's theme overlay instead of the one that spawned it.

**Current behavior:** No caching of the group ID at fragment/dialog creation time. Each call to `dialogBuilder()` re-queries the current pager position. This can cause:
- Open Tasks2 dialog, start swiping to Tasks3 → dialog theme starts transitioning mid-animation
- Dialogs spawned from ActionMode (which may cache the group ID) vs. direct fragment calls

**Mitigation in current code:** Fragments and adapters cache the group ID (e.g., `TaskEditorBottomSheet.ARG_TASK_GROUP`, line 48) and pass it to Palette.forGroup(), avoiding reliance on pager position.

**Risk:** Compose port must replicate this caching behavior; using the live pager position in dialogs will cause mid-animation theme thrashing.

### 3.4 Dialog Default Fallback

**Case:** `TaskEditorBottomSheet` uses either `Palette.forGroup()` or falls back to `dialogDefault()`.

The `dialogDefault()` palette is **not** a group palette; it's assembled from individual color resources. Its hex values differ from any of the 4 groups:
- E.g., `dialogDefault.bg = colorDialogSurface (#1F4F38)` vs. `cornflowerBg (#5C7CC0)`

**Risk:** If a bottom sheet is opened from the default tab (Tasks1, group 0), it will use the dialogDefault colors (dark green surface). This is intentional but must be preserved during Compose migration.

### 3.5 Alpha-Component Colors (textSoft, divider)

**Colors in XML:**
- `cornflowerTextSoft`: `#BCF2F4FA` → alpha BC (~74%), RGB F2F4FA (light blue)
- `cornflowerDivider`: `#2EF2F4FA` → alpha 2E (~18%), RGB F2F4FA
- `canaryTextSoft`: `#A82E2406` → alpha A8 (~66%), RGB 2E2406 (dark brown)
- `canaryDivider`: `#242E2406` → alpha 24 (~14%), RGB 2E2406

**Impact:** When ported to Kotlin, these must be `Color(Long)` or `@Composable Color` with explicit alpha channels. Hex parsing must preserve the alpha byte (most significant in ARGB format).

**Risk:** Simple `Color.parseColor("#BCF2F4FA")` in Kotlin would fail at runtime; must use explicit `Color(red, green, blue, alpha)` constructor or Color utility.

### 3.6 Adapter Palette Reset

**Case:** `TasksRecyclerViewAdapter.usePaletteForGroup(int group)` (line 73)

This method is called when the fragment transitions between tabs. It stores the new palette and calls `notifyDataSetChanged()`, rebinding all visible items. No incremental diffing.

**Risk:** If called frequently (e.g., on every scroll), will cause jank. In practice, called only on tab change, so acceptable.

**Compose equivalent:** Must re-render LazyColumn items when palette changes; use `key(group)` or `remember(group)` to ensure recomposition.

### 3.7 ActionMode Bar Tinting Race

**MainActivity.tintActionModeBarForCurrentTab()** (lines 297–308)

Tints the CAB with `palette.bg`. Uses two-step approach:
1. `decor.post(tint)` — immediate post
2. `decor.postDelayed(tint, 100)` — retry after 100ms

**Reason:** ActionMode bar may not exist until a moment after `onActionModeStarted()` is called; post() ensures the bar is created before tinting.

**Risk:** If the Compose port uses a custom ActionMode-like composable, must ensure it's created before tinting, or implement similar delayed logic.

### 3.8 Bridge Theme Compatibility

**AppTheme parent:** `Theme.MaterialComponents.Light.DarkActionBar.Bridge` (styles.xml:4)

The Bridge variant is a compatibility layer:
- Keeps AppCompat widgets functional
- Exposes Material 3 color attributes (colorSurface, colorOnSurface, etc.) for dialog inflation
- Does **not** provide all M3 tokens (e.g., `colorOutline`, `colorScrim` missing)

**Risk:** If Compose port switches to pure Material 3 theme, must verify all color attributes used in style overlays are defined (e.g., TextAppearance.App.Dialog.LabelSmall, line 112, uses `?android:attr/textColorSecondary` instead of `?attr/colorOnSurfaceVariant`).

### 3.9 Daily Reset & State Invalidation

**Not directly tied to Palette, but relevant:**

The Tabs/Groups are tied to folder-based task lists. If a folder is deleted or renamed, the group ID remains stable (1/2/3 are hard-coded per folder). Palette is **not** invalidated on data changes; it's purely a view concern.

**Risk:** If Compose architecture stores palette in a ViewModel with lifecycle hooks, must ensure it's not tied to Realm data that might be invalidated during restore/sync.

### 3.10 Compose Recomposition & LocalPalette

**Planned architecture (COMPOSE-MIGRATION-PLAN.md, line 54):**
> `LocalPalette` (CompositionLocal) — per-tab palette (Cornflower / Canary / Indigo / default), 9 tokens; status/nav bars + chrome recolour reactively.

**Gotcha:** If `LocalPalette` is provided at the root `AppTheme` composable and changes when the pager page changes, all composables using `LocalPalette.current` will recompose. This includes:
- Tab content (TaskList, FolderPanel, Notes)
- Status/nav bar tinting (via `window.setStatusBarColor()` in effect)
- Dialog theme (via ContextThemeWrapper or equivalent)

**Risk:** Deep recomposition of the entire tab content on page change could cause jank. Mitigation: scope palette changes to leaf composables (TaskList adapts palette, dialogs read from LocalPalette), not root. Use `remember` to cache expensive computations.

---

## 4. Compose Mapping

### 4.1 Kotlin Data Class & Palette Structure

**Target file:** `ui/theme/Palette.kt` (new; replaces Palette.java)

```kotlin
/**
 * Unified per-tab colour set. Task groups map to palettes:
 * 1 = Cornflower (Tasks2), 2 = Canary (Tasks3), 3 = Indigo (Notes).
 */
data class Palette(
    val bg: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val text: Color,
    val textSoft: Color,
    val inputText: Color,
    val counter: Color,
    val accent: Color,
    val divider: Color,
) {
    companion object {
        fun forGroup(context: Context, group: Int): Palette? = when (group) {
            1 -> cornflower()
            2 -> canary()
            3 -> indigo()
            else -> null
        }

        fun dialogDefault(context: Context): Palette = /* assemble from resources */

        private fun cornflower(): Palette = Palette(
            bg = Color(0xFF5C7CC0),
            surface = Color(0xFFEEF1F8),
            surfaceMuted = Color(0xFF5274B7),
            text = Color(0xFFF2F4FA),
            textSoft = Color(0xBCF2F4FA), // alpha BC
            inputText = Color(0xFF1C2952),
            counter = Color(0xFF7889B0),
            accent = Color(0xFFE8B85C),
            divider = Color(0x2EF2F4FA), // alpha 2E
        )

        // ... canary(), indigo() defined similarly
    }
}
```

**Key differences from Java:**
- `Color` instead of `Int` (Compose native type)
- `data class` for equality/hashing (useful for LocalPalette composition tracking)
- Static factory methods remain (same API surface for adapters/VMs)
- Alpha embedded in color value using ARGB constructor

### 4.2 CompositionLocal & Theme Provider

**Target file:** `ui/theme/AppTheme.kt` (new)

```kotlin
val LocalPalette = compositionLocalOf<Palette> { 
    error("No palette provided") 
}

@Composable
fun AppTheme(
    group: Int = 0,
    content: @Composable () -> Unit,
) {
    val palette = Palette.forGroup(LocalContext.current, group) 
        ?: Palette.dialogDefault(LocalContext.current)
    
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(), // M3 color mapping
            typography = /* ... */,
            content = content,
        )
    }
}

private fun Palette.toColorScheme(): ColorScheme = ColorScheme(
    primary = accent,
    onPrimary = text,
    surface = surface,
    onSurface = text,
    // ... map remaining 9 palette tokens to M3 slots
)
```

**Usage in MainActivity-equivalent:**
```kotlin
setContent {
    AppTheme(group = currentTabGroup) {
        MainScreen()
    }
}
```

### 4.3 System Bar Tinting

**Target file:** `ui/activity/MainActivity.kt` (converted)

Replace `applyTabChrome()` logic:

```kotlin
@Composable
fun MainScreen() {
    val palette = LocalPalette.current
    val view = LocalView.current
    
    LaunchedEffect(palette) {
        view.window?.let { window ->
            window.statusBarColor = palette.bg.toArgb()
            window.navigationBarColor = palette.bg.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController?.isAppearanceLightStatusBars = 
                (palette == Palette.canary()) // dark icons for yellow
        }
    }
    
    Box(modifier = Modifier.background(palette.bg)) {
        // Content
    }
}
```

**Key differences:**
- No imperative `applyTabChrome()` method; use `LaunchedEffect` to react to palette changes
- Status/nav bar colors set reactively, not on page change callback
- Canary light-icons check becomes: `palette == Palette.canary()` or add boolean field to Palette

### 4.4 Composables Replacing Adapters

#### TaskList (replacing TasksRecyclerViewAdapter)

**Old code pattern:**
```java
adapter.usePaletteForGroup(group);
// onBindViewHolder() applies palette.surface, palette.text, etc.
```

**Compose equivalent:**
```kotlin
@Composable
fun TaskList(
    group: Int,
    items: List<TaskItem>,
) {
    val palette = LocalPalette.current.takeIf { group != 0 }
        ?: Palette.dialogDefault(LocalContext.current)
    
    LazyColumn {
        items(items, key = { it.id }) { task ->
            TaskItemRow(
                task = task,
                backgroundColor = palette?.surface ?: Color.Transparent,
                textColor = palette?.text ?: Color.Black,
                accentColor = palette?.accent ?: Color.Red,
            )
        }
    }
}
```

#### FolderCard (replacing FolderOfTaskRecyclerViewAdapter)

```kotlin
@Composable
fun FolderCard(
    folder: Folder,
    group: Int,
) {
    val palette = LocalPalette.current
    
    Card(
        modifier = Modifier.background(palette.surface),
    ) {
        Text(
            text = folder.name,
            color = palette.text,
        )
    }
}
```

#### Dialog Theming (replacing MaterialAlertDialogBuilder overlay)

**Old code pattern:**
```java
new MaterialAlertDialogBuilder(activity, 
    R.style.ThemeOverlay_App_MaterialAlertDialog_Cornflower)
```

**Compose equivalent:**
```kotlin
@Composable
fun TaskDialog(group: Int) {
    val dialogPalette = Palette.forGroup(LocalContext.current, group)
        ?: Palette.dialogDefault(LocalContext.current)
    
    CompositionLocalProvider(LocalPalette provides dialogPalette) {
        AlertDialog(
            onDismissRequest = { /* ... */ },
            containerColor = dialogPalette.surfaceMuted,
            textContentColor = dialogPalette.text,
            // M3 AlertDialog automatically uses LocalPalette for colors
        )
    }
}
```

Or, for ModalBottomSheet (replacing TaskEditorBottomSheet):

```kotlin
@Composable
fun TaskEditorSheet(
    taskId: Long,
    group: Int,
    onDismiss: () -> Unit,
) {
    val sheetPalette = Palette.forGroup(LocalContext.current, group)
        ?: Palette.dialogDefault(LocalContext.current)
    
    CompositionLocalProvider(LocalPalette provides sheetPalette) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = sheetPalette.surfaceMuted,
            // Sheet content automatically inherits LocalPalette
        ) {
            TaskEditorContent(/* ... */)
        }
    }
}
```

### 4.5 ViewModel Integration

**Existing architecture (COMPOSE-MIGRATION-PLAN.md, line 44):**
> ViewModels via Dagger 2 (multibinding ViewModelProvider.Factory), injected into composables with `viewModel(factory = …)`.

**Palette integration:**

Palettes are **view-only**; they don't belong in ViewModels. But the group ID does:

```kotlin
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {
    val group: Int = 1 // or passed as constructor arg
    val tasks: Flow<List<Task>> = repo.tasksForGroup(group)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@Composable
fun TasksScreen() {
    val vm: TasksViewModel = viewModel(factory = vmFactory)
    val palette = Palette.forGroup(LocalContext.current, vm.group)
    
    LaunchedEffect(palette) {
        // Update status bar, etc.
    }
    
    CompositionLocalProvider(LocalPalette provides palette) {
        TaskList(items = vm.tasks.collectAsState().value)
    }
}
```

### 4.6 HorizontalPager Integration

**Current code (MainActivity):**
```java
viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
    @Override
    public void onPageSelected(int position) {
        applyTabChrome(position);
        // Fragments/adapters receive implicit position via pager
    }
});
```

**Compose equivalent:**
```kotlin
@Composable
fun MainScreen() {
    var currentGroup by remember { mutableStateOf(1) } // START_PAGE = 1
    
    HorizontalPager(
        pageCount = 5,
        initialPage = 1,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val group = Tabs.groupForPosition(page)
        LaunchedEffect(page) {
            currentGroup = group
        }
        
        when (page) {
            0 -> NoteFoldersScreen(group = group)
            1 -> TasksScreen(group = group)
            2 -> TasksScreen(group = group)
            3 -> TasksScreen(group = group)
            4 -> NotesScreen(group = group)
        }
    }
}

@Composable
fun TasksScreen(group: Int) {
    val palette = Palette.forGroup(LocalContext.current, group)
        ?: Palette.dialogDefault(LocalContext.current)
    
    CompositionLocalProvider(LocalPalette provides palette) {
        // Content
    }
}
```

**Key difference:** Palette is computed and provided **per-page**, not globally. This avoids deep recomposition of tabs not in view.

### 4.7 Action Mode Equivalent

**Old code (MainActivity):**
```java
onActionModeStarted() {
    tintActionModeBarForCurrentTab();
}

private void tintActionModeBarForCurrentTab() {
    Palette p = Palette.forGroup(this, Tabs.groupForPosition(viewPager.getCurrentItem()));
    if (p != null) applyActionModeBarColor(p.bg);
}
```

**Compose equivalent:**

Replace system CAB with custom composable (per COMPOSE-MIGRATION-PLAN.md):

```kotlin
@Composable
fun ActionModeBar(
    isVisible: Boolean,
    itemCount: Int,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    
    if (isVisible) {
        TopAppBar(
            title = { Text("$itemCount selected") },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Dismiss")
                }
            },
            modifier = Modifier.background(palette.bg),
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = palette.bg,
                titleContentColor = palette.text,
            ),
        )
    }
}
```

**Palette automatically applied** via LocalPalette, no imperative tinting needed.

### 4.8 Event Flow

**Page change event:**
1. User swipes HorizontalPager
2. `onPageSelected` callback fires in Compose (internal to Pager)
3. `LaunchedEffect(page)` in MainScreen updates `currentGroup`
4. Each page's composable receives new `group` parameter
5. `TasksScreen(group)` recomposes with new group
6. `AppTheme(group)` at the page level updates LocalPalette
7. All descendants using `LocalPalette.current` recompose (TaskList, Cards, etc.)
8. Status/nav bar colors updated via `LaunchedEffect(palette)` in MainScreen

**Impact:** More granular updates than Java version; only affected subtrees recompose.

---

## 5. Files to Delete (Post-Migration)

### 5.1 Java Theme Code
- `app/src/main/java/com/shumidub/todoapprealm/ui/theme/Palette.java`

### 5.2 RecyclerView Adapters
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/TasksRecyclerViewAdapter.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/adapter/FolderOfTaskRecyclerViewAdapter.java`

### 5.3 Legacy Fragments
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/fragment/FolderSlidingPanelFragment.java`
- (and any other FragmentViewPager-based fragments)

### 5.4 Dialog/BottomSheet Java Code
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_bottomsheet/TaskEditorBottomSheet.java`
- (and related dialog fragments)

### 5.5 ActionMode Callbacks
- `app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/task/TaskActionModeCallback.java`
- `app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/EditDeleteActionModeCallback.java`

### 5.6 XML Layouts
- All layouts in `app/src/main/res/layout/` tied to deleted fragments/dialogs
- Specifically: bottom sheet, folder panel, task list layouts

### 5.7 Style Overlays (Partial)
- Keep `styles.xml` but remove:
  - `AppTheme` (replaced by Compose AppTheme)
  - All `ThemeOverlay.App.MaterialAlertDialog.*` overlays (replaced by per-dialog LocalPalette)
  - Fragment-specific style references
- Keep:
  - `Widget.App.Button.TabColorSwatch.*` (if color picker remains XML-based temporarily)
  - Material 3 base definitions (for interop)

### 5.8 Color Resources (Partial)
- Keep all color definitions in `colors.xml` **during interop phase**
- Migrate to Kotlin during Phase 1 (data-interop)
- Delete `colors.xml` after all composables are live

### 5.9 Dependencies
- `com.sothree.slidinguppanel:library` (replaced by custom AnchoredDraggable)
- `com.google.android.material.keyboardvisibilityevent` (replaced by WindowInsets.ime)
- `io.reactivex.rxjava2:rxjava` (replaced by coroutines)

---

## 6. Migration Checklist

- [ ] **Phase 0 (Scaffold):**
  - [ ] Create `Palette.kt` with all 9-token data class + factory methods
  - [ ] Create `AppTheme.kt` with LocalPalette composition local
  - [ ] Create `MainActivity.kt` (Compose) with HorizontalPager + applyTabChrome equivalent
  - [ ] Verify status/nav bar colors on all 4 groups (especially Canary light icons)
  - [ ] Color round-trip: XML → Kotlin Color → Compose UI

- [ ] **Phase 1 (Data-interop):**
  - [ ] Migrate Palette color loading from XML to inline Kotlin (remove ContextCompat.getColor() calls)
  - [ ] Ensure Palette is context-independent (no Context parameter in factories)

- [ ] **Phase 2 (Folder panel + Tasks):**
  - [ ] Replace TasksRecyclerViewAdapter with TaskList composable
  - [ ] Replace FolderOfTaskRecyclerViewAdapter with FolderCard/FolderList composable
  - [ ] Verify palette application in LazyColumn items
  - [ ] Test section expansion/collapse with palette colors

- [ ] **Phase 3 (Notes):**
  - [ ] Replace Notes fragment with Compose
  - [ ] Apply palette to notes cards/text

- [ ] **Phase 4 (Sync):**
  - [ ] Replace SyncDialog / FirebaseAuthDialog with Compose
  - [ ] Verify dialog palette theming via LocalPalette

- [ ] **Phase 5 (Cleanup):**
  - [ ] Delete Java Palette.java, adapters, fragments, layouts
  - [ ] Delete styles.xml overlays (keep base theme for interop)
  - [ ] Delete unused color resources
  - [ ] Verify all existing `.realm` DBs open
  - [ ] Verify old JSON backups restore

---

## 7. Test Cases

### 7.1 Palette Correctness
- [ ] Tab 1 (Tasks1, group 0): green chrome, null palette, fallback colors
- [ ] Tab 2 (Tasks2, group 1): Cornflower blue chrome, #5C7CC0 bg, #EEF1F8 surface, #E8B85C accent
- [ ] Tab 3 (Tasks3, group 2): Canary yellow chrome, #FFD93D bg, dark icons, #D7305C accent
- [ ] Tab 4 (Notes, group 3): Indigo blue chrome, #3D52A0 bg, #F4A742 accent
- [ ] Verify all 9 tokens per group (use color picker on device)

### 7.2 Chrome Recoloring
- [ ] Status bar matches `bg` token per tab
- [ ] Nav bar matches `bg` token per tab
- [ ] Action bar background matches `bg` token per tab
- [ ] Canary tab only: status/nav bar icons are dark (contrast on yellow)
- [ ] Other tabs: status/nav bar icons are light (contrast on blue/green)

### 7.3 Dialog Theming
- [ ] Open dialog from Tasks1 tab (default): dark green surface, light text
- [ ] Open dialog from Tasks2 tab: Cornflower blue surface, light text, #E8B85C accents
- [ ] Open dialog from Tasks3 tab: Canary yellow surface, dark text, #D7305C accents
- [ ] Open dialog from Notes tab: Indigo surface, light text, #F4A742 accents

### 7.4 BottomSheet Theming
- [ ] TaskEditorSheet from each tab: correct palette applied
- [ ] TextInputLayout stroke matches accent
- [ ] CheckBox button tint matches accent
- [ ] Fallback to dialogDefault if no group palette

### 7.5 Adapter → Composable Parity
- [ ] Task list item background = palette.surface
- [ ] Task text color = palette.text
- [ ] Section count text color = palette.counter
- [ ] Checkbox tint = palette.accent
- [ ] Card backgrounds = palette.surface

### 7.6 Edge Cases
- [ ] Swipe rapidly between tabs: palette updates cleanly, no flicker
- [ ] Open dialog, swipe to another tab: dialog remains in original palette (if cached) or updates (if live)
- [ ] Rotate device: palette reapplied, status/nav bar colors reapplied
- [ ] App backgrounding/foregrounding: palette state preserved

### 7.7 Alpha Colors
- [ ] textSoft (e.g., #BCF2F4FA) renders at correct opacity
- [ ] divider (e.g., #2EF2F4FA) renders at correct opacity (very subtle)
- [ ] Verify in screenshot: secondary text is faded, dividers are light/invisible

### 7.8 Restore & Compatibility
- [ ] Existing `.realm` DBs from old app open and render with correct palettes
- [ ] Old JSON backups restore with correct palettes per group
- [ ] Group IDs (1/2/3) in data are preserved and map to correct palettes

---

## 8. Known Risks & Mitigations

| Risk | Severity | Mitigation |
|------|----------|-----------|
| **Canary light icons flipped** | High | Add test case; verify setAppearanceLightStatusBars logic. Use boolean in Palette: `isLightBackground = (this == Palette.canary())` |
| **Dialog palette thrashing** | Medium | Cache group ID in dialog/sheet args (existing pattern). Use LocalPalette in dialog, not live pager position. |
| **Alpha color parsing failure** | Medium | Use explicit Color(red, green, blue, alpha) constructor; test hex parsing. Avoid Color.parseColor() for ARGB values. |
| **Adapter recomposition jank** | Low | Use `key(group)` in LazyColumn; remember expensive color mappings. |
| **Bridge theme missing tokens** | Low | Audit all style attributes used in overlays before Compose migration. Verify Material 3 AppTheme provides all needed tokens. |
| **Palette context dependency** | Medium | Migrate from context-dependent factory methods to context-free constants during Phase 1. Pre-load all colors in Palette companion object. |
| **System bar tinting lost** | High | Implement LaunchedEffect(palette) in every screen that needs bar color changes. Test on API 26+ (status bar) and API 21+ (nav bar). |

---

## 9. Implementation Notes

### 9.1 Hex Color Parsing

**Old code:**
```java
ContextCompat.getColor(ctx, R.color.cornflowerBg) // returns Int
```

**Kotlin Compose equivalent:**
```kotlin
val cornflowerBg = Color(0xFF5C7CC0) // UInt literal, safe at compile time

// Or, to keep XML resources during interop:
val cornflowerBg = colorResource(R.color.cornflowerBg).toArgb().let { /* convert */ }
```

**For alpha colors, explicit:**
```kotlin
val cornflowerTextSoft = Color(
    red = 0xF2,
    green = 0xF4,
    blue = 0xFA,
    alpha = 0xBC,
)
// Or:
val cornflowerTextSoft = Color(0xBCF2F4FA) // ARGB in single UInt
```

### 9.2 LocalPalette Access

```kotlin
@Composable
fun MyComposable() {
    val palette = LocalPalette.current
    Text(
        text = "Hello",
        color = palette.text,
        modifier = Modifier.background(palette.surface),
    )
}
```

### 9.3 Testing Palette in Previews

```kotlin
@Preview
@Composable
fun TaskListPreview_Cornflower() {
    CompositionLocalProvider(LocalPalette provides Palette.cornflower()) {
        TaskList(items = /* ... */)
    }
}

@Preview
@Composable
fun TaskListPreview_Canary() {
    CompositionLocalProvider(LocalPalette provides Palette.canary()) {
        TaskList(items = /* ... */)
    }
}
```

---

## Glossary

- **Group ID:** Integer 0–3 mapping task tabs to palette schemes.
- **Page Position:** Integer 0–4 mapping HorizontalPager pages (Notes folder page 0, Tasks1–Notes pages 1–4).
- **Palette:** Immutable 9-token color set for a group.
- **LocalPalette:** CompositionLocal providing the active palette to composable tree.
- **Theme Overlay:** Android XML style resource layer (e.g., ThemeOverlay.App.MaterialAlertDialog.Cornflower) that remaps color attributes for dialogs.
- **Bridge Theme:** AppCompat+Material 3 hybrid theme enabling safe interop during migration.
- **Status/Nav Bar:** Android system UI bars; tinted to palette.bg in current implementation.

---

**End of Specification**
