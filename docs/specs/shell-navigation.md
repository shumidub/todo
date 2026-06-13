# Shell Navigation Spec — Java to Jetpack Compose Migration

> Exhaustive mapping of the app shell (ViewPager + ActionBar + SlidingUpPanel + back-press chain) to Compose constructs.
> Reference: COMPOSE-MIGRATION-PLAN.md, SOURCE: MainActivity.java, CustomViewPager.java, MainPagerAdapter.java, BaseActivity.java, Tabs.java

## 1. Current Behavior

### 1.1 ViewPager Layout (5 pages)
- **Source:** MainActivity.java:138-142, activity_main.xml
- **Pages:**
  - Position 0: FolderNoteFragment — Notes/folders navigation
  - Position 1: FolderSlidingPanelFragment (taskGroup=0) — Tasks1 (default chrome)
  - Position 2: FolderSlidingPanelFragment (taskGroup=1) — Tasks2 (Cornflower blue, #5C7CC0)
  - Position 3: FolderSlidingPanelFragment (taskGroup=2) — Tasks3 (Canary yellow, #FFD93D)
  - Position 4: FolderSlidingPanelFragment (taskGroup=3) — Notes tab (Indigo, #3D52A0)
- **START_PAGE:** 1 (Tasks1) — Tabs.java:20
- **Offscreen limit:** 1 (only adjacent pages cached) — MainActivity.java:141
- **Touch interception:** CustomViewPager gates swipe via `setPageCanChangedScrolled(boolean)` — CustomViewPager.java:42-44, called from FolderSlidingPanelFragment when panel expanded

### 1.2 ActionBar & TopBar
- **Source:** MainActivity.java:135, 215-216, 342-371
- **Title management:**
  - Default: "Tasks" (MainActivity.java:215)
  - Page 0 (Notes folders): FolderNoteFragment.getValidTitle() — updated on nav into note-detail
  - Pages 1-4 (Tasks): FolderSlidingPanelFragment.getValidTitle() — folder name when panel expanded, default when collapsed
  - Updated on page change (MainActivity.java:184-200) and panel state change (FolderSlidingPanelFragment.java:193-206)
- **Status/nav bar tinting:** Per-tab palette applied in applyTabChrome(position) — MainActivity.java:342-371
  - Reads Palette.forGroup(context, Tabs.groupForPosition(position)) — Palette.java:43-59
  - Sets: window.setStatusBarColor, window.setNavigationBarColor, rootLayout.setBackgroundColor, actionBar.setBackgroundDrawable
  - Light icon appearance (setAppearanceLightStatusBars) only for Canary (group 2) — MainActivity.java:355-358
  - Called on page select (MainActivity.java:205) and at startup (MainActivity.java:216)
- **Display up button:** Conditional on position & UI state
  - Position 1: never shown (MainActivity.java:160)
  - Position 0: shown only if isNoteFragment=true (detail view) — MainActivity.java:173-175
  - Others: never shown (MainActivity.java:179)
  - Updated on page change (MainActivity.java:154-180) and panel state changes (FolderSlidingPanelFragment.java:198-202)

### 1.3 Palette System (Theme Tokens per Tab)
- **Source:** Palette.java (9 tokens per palette)
- **Token set:**
  - `bg` — background (status/nav bar, root layout)
  - `surface` — card/surface backgrounds
  - `surfaceMuted` — muted surface (e.g., disabled states)
  - `text` — primary text
  - `textSoft` — secondary text (alpha-blended)
  - `inputText` — text input (dark on light palettes like Canary)
  - `counter` — day-scope counter badge
  - `accent` — action/accent color
  - `divider` — divider/border (alpha-blended)
- **Palette cases:** Tasks1 returns null (default chrome), Tasks2=Cornflower, Tasks3=Canary, Notes-tab=Indigo — Palette.java:43-59
- **Applied to:**
  - Root layout & ActionBar background (MainActivity.java:349-351)
  - Status/nav bar color (MainActivity.java:351-352)
  - ActionMode bar background (MainActivity.java:310-322)
  - Dialog context wrapping (MainActivity.java:262-281)

### 1.4 SlidingUpPanel (Folder/Task List)
- **Source:** FolderSlidingPanelFragment.java:171-206, activity_main.xml:15-19, com.sothree.slidinguppanel library
- **Layout:** Pages 1-4 inflate R.layout.slide_up_panel_layout with a SlidingUpPanelLayout
- **Panel states:** COLLAPSED (peek 24dp footer) ↔ EXPANDED (full screen task editor)
- **Listeners:**
  - `onPanelSlide(View, float slideOffset)`: footer alpha = 1.0 - slideOffset; footer visibility toggled at 85% offset; action-mode finishes at 30-70% — FolderSlidingPanelFragment.java:179-190
  - `onPanelStateChanged`: title updates (folder name on expand, "Tasks" on collapse), adapter notifyDataSetChanged, action-mode finishes, options menu invalidated — FolderSlidingPanelFragment.java:193-206
- **Touch handling:** ViewPager swipe disabled when panel expanded via setPageCanChangedScrolled(false) — (implied from FolderSlidingPanelFragment usage, explicit in MainActivity.java:464)

### 1.5 Back-Press Chain (Hierarchical Dismiss)
- **Source:** MainActivity.java:417-450, onBackPressed()
- **Chain (executed in order, first match returns):**
  1. Pages 1-4 (Tasks): if panel EXPANDED → collapse panel, return
  2. Pages 1-4: if panel COLLAPSED → proceed to step 3
  3. Page 0 (Notes): if actionModeIsEnabled → finishActionMode(), return
  4. Page 0 (Notes): if isNoteFragment (detail view) → setFolderNoteViews() (back to folders), return
  5. All pages: double-tap-to-exit — onBackPressedWithTimer() — MainActivity.java:452-460
     - First back press: show toast "For exit press again", record time
     - Second back press within 2 seconds: call super.onBackPressed() (exit app)
- **ActionMode state:** FolderSlidingPanelFragment.actionModeIsEnabled (boolean flag); FolderNoteFragment.actionModeIsEnabled

### 1.6 Day-Scope Counter (Menu Item)
- **Source:** MainActivity.java:60, 392-414
- **Menu item:**
  - Group 2, id 2, order 2, title = App.dayScope (integer)
  - SHOW_AS_ACTION_ALWAYS (right side of ActionBar)
  - Click listener: logs Realm data, no UI action
- **Visibility:** Hidden on page 4 (Notes tab, group 3) — MainActivity.java:396, 203
- **Update timing:**
  - onCreateOptionsMenu: posted to rootLayout after App.setDayScopeValue() — MainActivity.java:145-147
  - invalidateOptionsMenu() called after restore (MainActivity.java:380-381) and on panel state change (FolderSlidingPanelFragment.java:205)
  - onCreateOptionsMenu called again after invalidateOptionsMenu(), menu title updated — MainActivity.java:410-413
- **Calculation:** App.setDayScopeValue() — App.java:111-131
  - Sums task counts where lastDoneDate matches today's (day-of-year + year integer)
  - Iterates all done/partially-done tasks, checks dateCountAccumulation for today entries

### 1.7 Window Insets & Edge-to-Edge
- **Source:** MainActivity.java:127-133
- **Root layout inset listener:** Pads bottom with max(navigationBars.bottom, ime.bottom)
- **KeyboardVisibilityEvent:** BaseActivity.java broadcasts "KeyboardWillShow"/"KeyboardWillHide" intents with keyboard height — BaseActivity.java:18-40
  - onGlobalLayout calculates heightDiff = rootView.height - rootLayout.height
  - If heightDiff > contentViewTop, keyboard is shown

### 1.8 Miscellaneous State
- **pagerAdapterPosition:** Cached position from onPageSelected — MainActivity.java:154-156, queried by getPagerAdapterPosition()
- **actionMode:** Single ActionMode instance, created on every page select — MainActivity.java:181 (EmptyActionModeCallback)
- **Permissions:** Runtime permissions for storage (API 29-30) — MainActivity.java:95-118; SAF picker for restore — MainActivity.java:68-78

---

## 2. State & Data Flow

### 2.1 ViewPager & Fragment State
- **Fragment creation:** MainPagerAdapter.getItem(position) — MainPagerAdapter.java:29-33
  - Page 0: new FolderNoteFragment()
  - Pages 1-4: FolderSlidingPanelFragment.newInstance(taskGroup)
- **Fragment lifecycle:** onCreate → onViewCreated; instances added to App.folderSlidingPanelFragments list on onCreate (FolderSlidingPanelFragment.java:135-137)
- **Data source:** Live Realm lists (RealmList, RealmResults) bound directly in adapters
  - No copying; objects are mutated in place (declared in Realm.setDefaultConfiguration with allowWritesOnUiThread=true, allowQueriesOnUiThread=true) — App.java:64-65

### 2.2 Palette Selection
- **Immutable per tab:** Palette.forGroup(context, taskGroup) called once per page select, stored locally in FolderSlidingPanelFragment.palette
- **Root cause of palette:** Tabs.groupForPosition(pagerPosition) → returns taskGroup or -1 — Tabs.java:25-26

### 2.3 Back-Press State Machine
- **Button press timing:** MainActivity.time (long) stores milliseconds of last back press — MainActivity.java:53
- **Reset:** time = 0 on app creation
- **Logic:** if (time != 0 && now - time < 2000) exit; else time = now, show toast
- **No explicit state tracking** — purely imperative checks on the call stack

### 2.4 ActionMode Lifecycle
- **Creation:** startSupportActionMode(new EmptyActionModeCallback()) on every page select — MainActivity.java:181
- **Dismissal:** finishActionMode() called in FolderSlidingPanelFragment when panel slides (30-70%) or state changes — FolderSlidingPanelFragment.java:189, 199
- **Dismissal:** finishActionMode() called in FolderNoteFragment on back press if actionModeIsEnabled — FolderNoteFragment.java:438

### 2.5 Daily Reset Timing
- **Trigger:** MainActivity.onCreate calls App.setDayScopeValue() — MainActivity.java:86-90, 145-147
- **Schedule:** No explicit trigger after cold start; relies on app kill + restart
- **Calculation:** Compares task.lastDoneDate (integer: day-of-year + year) against Calendar.getInstance().get(DAY_OF_YEAR) + YEAR — App.java:115-116
- **Race condition:** If user crosses midnight while app is open, dayScope is stale until next activity restart (no timer/listener)

### 2.6 Thread Model
- **Main thread only:** All Realm reads/writes on UI thread (allowQueriesOnUiThread=true) — App.java:64-65
- **No background threads for data operations** — blocking calls acceptable at current scale (noted as deliberate debt in App.java:55-60)
- **Broadcast intents:** BaseActivity posts LocalBroadcastManager intents for keyboard visibility — BaseActivity.java:24-37

---

## 3. Edge Cases & Gotchas

### 3.1 Panel Expanded → ViewPager Swipe Disabled
- **Issue:** When SlidingUpPanelLayout is expanded, touch events must not trigger page scrolling
- **Solution:** CustomViewPager.onTouchEvent/onInterceptTouchEvent return false if enable=false — CustomViewPager.java:28-40
- **Caller responsibility:** Fragment must call MainActivity.setPageCanChangedScrolled(boolean) — MainActivity.java:464
- **Risk:** If forget to disable, swipe during expanded panel will cause race condition (panel collapse vs page scroll)

### 3.2 Palette Null for Tasks1 (Default Chrome)
- **Issue:** Palette.forGroup(context, 0) returns null — Palette.java:43-59
- **Consequence:** dialogBuilder() and dialogContext() must check for null and apply defaults — MainActivity.java:239-256, 262-281
- **Risk:** Untested code path if Tasks1-specific dialog isn't exercised

### 3.3 Page 0 Title Ambiguity
- **State:** isNoteFragment (boolean in FolderNoteFragment) — true = note detail, false = folder list
- **Title:** onPageSelected queries all fragments to find FolderNoteFragment, then calls getValidTitle() — MainActivity.java:184-190
- **Risk:** Fragment queries are O(n) and match by instanceof; if multiple instances leak, wrong title appears
- **Real-world case:** Restore from JSON might create stale fragments

### 3.4 ActionBar Display-Up Button Race Condition
- **Timing:** onPageSelected sets displayHomeAsUpEnabled; onViewCreated also sets it in FolderNoteFragment.setFolderNoteViews()
- **Order:** ViewPager calls onPageSelected → Fragment.onViewCreated → Fragment.setFolderNoteViews() (setDisplayHomeAsUpEnabled)
- **Risk:** If setFolderNoteViews() runs after onPageSelected, it may override the correct state
- **Actual behavior:** onViewCreated is async; but since ViewPager keeps offscreenPageLimit=1, adjacent fragments are pre-created, so onViewCreated may run before onPageSelected fires

### 3.5 Daily Reset Midnight Boundary
- **Scenario:** User launches at 11:59 PM, completes task, sees dayScope = N
- **Midnight crosses:** App is still running
- **Result:** dayScope remains N; user completes another task, counter shows 2N (doesn't reset)
- **Fix:** Requires timer or Broadcast receiver for calendar midnight — not implemented
- **Current workaround:** Kill app and relaunch after midnight

### 3.6 Keyboard Visibility Broadcasting Without Unregistration
- **Issue:** BaseActivity broadcasts intents on every global layout pass
- **Receiver:** Fragments may register broadcast receivers in onViewCreated but never unregister
- **Risk:** Receiver leak if fragment is destroyed while still subscribed

### 3.7 Realm Live Object Aliasing
- **Issue:** Fragment adapters hold direct references to RealmList/RealmResults
- **Danger:** If Realm transaction replaces the container (e.g., restore), the old list becomes invalid
- **Mitigation:** MainActivity.refreshAfterRestore() iterates fragments and calls reloadFromRealm() — MainActivity.java:379-389
- **Risk:** Not all fragments may implement reloadFromRealm(); adapter.notifyDataSetChanged() won't help if the underlying list is dead

### 3.8 Dialog Theme Wrapping Per-Tab
- **Pattern:** dialogContext() and dialogBuilder() wrap MaterialAlertDialog theme overlay per current tab
- **Risk:** If dialog builder/context is obtained, stored, then tab changes, the dialog will still use the old context
- **Real case:** Add task dialog cached in field and reused → unlikely, but possible

### 3.9 Menu Item Visibility Binding
- **Issue:** dayScopeMenu.setVisible(boolean) called on page select (MainActivity.java:203) and panel state change (FolderSlidingPanelFragment.java:205)
- **Timing:** invalidateOptionsMenu() → onCreateOptionsMenu called again, but if dayScopeMenu is null on first call, subsequent calls won't re-create it
- **Risk:** Menu item disappears if invalidated before first creation

### 3.10 OffscreenPageLimit & Fragment Lifecycle
- **Setting:** setOffscreenPageLimit(1) means ViewPager keeps 1 page + current on memory — MainActivity.java:141
- **Consequence:** Page 0 fragments may be created/destroyed frequently if user swipes between pages 1-4
- **Risk:** If fragment stores temporary state in fields, rapid creation/destruction will reset it

### 3.11 Back-Press Timing Boundary
- **Condition:** second back press within 2000 milliseconds
- **Edge case:** If system clock jumps (NTP sync), time calculation could fail
- **Actual risk:** Low in practice, but System.currentTimeMillis() vs onBackPressedWithTimer() — MainActivity.java:452-453

### 3.12 Empty State Visibility in Notes Fragment
- **Behavior:** emptyState visibility linked to adapter.getItemCount() — FolderNoteFragment.java:126-130
- **Risk:** If adapter is not notified on data change, empty state and list UI become out-of-sync

---

## 4. Compose Mapping

### 4.1 HorizontalPager (ViewPager → Compose)
```kotlin
// Source: app/src/main/java/com/shumidub/todoapprealm/ui/activity/main/MainActivity.java

val pagerState = rememberPagerState(initialPage = Tabs.START_PAGE) { Tabs.PAGE_COUNT }

HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    userScrollEnabled = true  // Initially true; disabled when panel expanded
) { page ->
    when (page) {
        0 -> NotesScreen()  // Replaces FolderNoteFragment
        1, 2, 3, 4 -> TasksScreen(taskGroup = Tabs.groupForPosition(page))  // Replaces FolderSlidingPanelFragment
    }
}
```
- **State:** PagerState holds currentPage; sync with BackHandler & panel expansion state
- **OffscreenPageLimit:** HorizontalPager default is 1 (matches behavior)
- **Touch interception:** Set `userScrollEnabled = !panelExpanded` reactively

### 4.2 TopAppBar with Day-Scope Counter
```kotlin
// Source: MainActivity.java:135-216

TopAppBar(
    title = {
        Text(
            text = currentTitle,
            color = palette?.text ?: defaultTextColor
        )
    },
    actions = {
        if (pagerState.currentPage != Tabs.positionForGroup(3)) {
            TextButton(
                onClick = { /* log debug info */ },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "$dayScope",
                    color = palette?.counter ?: defaultCounterColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    },
    navigationIcon = {
        if (pagerState.currentPage == 0 && isNoteDetailView) {
            IconButton(onClick = { onNavigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = palette?.bg ?: defaultBg,
        navigationIconContentColor = palette?.text ?: defaultText,
        actionIconContentColor = palette?.text ?: defaultText,
        titleContentColor = palette?.text ?: defaultText
    )
)
```
- **State holders:** `currentTitle` (from NotesViewModel or TasksViewModel), `dayScope` (from AppViewModel), `palette` (from LocalPalette)
- **Update trigger:** Recompose on pagerState.currentPage, tasksViewModel.folderName, appViewModel.dayScope changes
- **Visibility:** Counter hidden when pagerState.currentPage == 4

### 4.3 LocalPalette (CompositionLocal)
```kotlin
// Source: ui/theme/Palette.java (port to Kotlin)

val LocalPalette = compositionLocalOf<Palette?> { null }

// In MainScreen or Shell composable:
val palette = Palette.forGroup(context, Tabs.groupForPosition(pagerState.currentPage))
CompositionLocalProvider(LocalPalette provides palette) {
    // All descendant composables can access palette via LocalPalette.current
}

// Usage in any composable:
val palette = LocalPalette.current
```
- **Kotlin data class:**
```kotlin
data class Palette(
    val bg: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val text: Color,
    val textSoft: Color,
    val inputText: Color,
    val counter: Color,
    val accent: Color,
    val divider: Color
)
```

### 4.4 Status/NavBar Tinting (applyTabChrome → Compose)
```kotlin
// Source: MainActivity.java:342-371

LaunchedEffect(pagerState.currentPage) {
    val palette = Palette.forGroup(context, Tabs.groupForPosition(pagerState.currentPage))
    if (palette != null) {
        setWindowColors(palette.bg, isLight = pagerState.currentPage == 2)  // Canary only
    } else {
        setWindowColors(colorResource(R.color.colorPrimary), isLight = false)
    }
}

private fun setWindowColors(bgColor: Color, isLight: Boolean) {
    view.window?.statusBarColor = bgColor.toArgb()
    view.window?.navigationBarColor = bgColor.toArgb()
    WindowCompat.getInsetsController(view.window, view.decorView)?.setAppearanceLightStatusBars(isLight)
    WindowCompat.getInsetsController(view.window, view.decorView)?.setAppearanceLightNavigationBars(isLight)
}
```
- **Trigger:** LaunchedEffect on pagerState.currentPage change
- **Location:** MainActivity (ComponentActivity) or Shell composable with local window reference

### 4.5 Sliding Panel (AnchoredDraggable + Custom Composable)
```kotlin
// Source: FolderSlidingPanelFragment.java:71-206, com.sothree.slidinguppanel library

data class PanelState(
    val isExpanded: Boolean = false,
    val slideOffset: Float = 0f  // 0 = collapsed, 1 = expanded
)

// Custom composable replacing SlidingUpPanelLayout:
@Composable
fun SlidingPanel(
    state: MutableState<PanelState>,
    modifier: Modifier = Modifier,
    peekHeight: Dp = 24.dp,
    content: @Composable (slideOffset: Float) -> Unit,
    header: @Composable (slideOffset: Float) -> Unit
) {
    val density = LocalDensity.current
    val maxHeight = remember { mutableFloatStateOf(0f) }
    
    val anchors = DraggableAnchors {
        false at 0f
        true at maxHeight.value - with(density) { peekHeight.toPx() }
    }
    
    val draggableState = remember {
        AnchoredDraggableState(initialValue = false, anchors, animationSpec = tween())
    }
    
    LaunchedEffect(draggableState.currentValue) {
        state.value = state.value.copy(isExpanded = draggableState.currentValue)
    }
    
    val slideOffset = if (maxHeight.value == 0f) 0f else draggableState.offset / maxHeight.value
    
    LaunchedEffect(slideOffset) {
        state.value = state.value.copy(slideOffset = slideOffset)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { maxHeight.value = it.height.toFloat() }
    ) {
        // Collapsed content (folder list)
        content(slideOffset)
        
        // Draggable panel header + expanded content
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(with(density) { (maxHeight.value - draggableState.offset).toDp() })
                .anchoredDraggable(draggableState, Orientation.Vertical)
        ) {
            header(slideOffset)
        }
    }
}
```
- **Anchors:** 2 positions — collapsed (0f) and expanded (maxHeight - peekHeight)
- **Offset animation:** Smooth fling via tween(); snapping to anchors
- **Footer fade:** opacity = 1 - slideOffset; visibility toggle at 85% offset
- **Touch during drag:** ViewPager userScrollEnabled = !panelExpanded (reactive binding)

### 4.6 BackHandler State Machine
```kotlin
// Source: MainActivity.java:417-450

@Composable
fun ShellBackHandler(
    pagerState: PagerState,
    panelState: State<PanelState>,
    isNoteDetailView: State<Boolean>,
    actionModeActive: State<Boolean>,
    onExit: () -> Unit
) {
    var backPressTime by remember { mutableLongStateOf(0L) }
    
    BackHandler {
        val currentPage = pagerState.currentPage
        
        when {
            // Step 1: Panel expanded on pages 1-4 → collapse
            (currentPage in 1..4) && panelState.value.isExpanded -> {
                panelState.value = panelState.value.copy(isExpanded = false)
            }
            // Step 2: Page 0, action-mode active → dismiss
            (currentPage == 0) && actionModeActive.value -> {
                actionModeActive.value = false
            }
            // Step 3: Page 0, note detail → back to folders
            (currentPage == 0) && isNoteDetailView.value -> {
                isNoteDetailView.value = false
            }
            // Step 4: Double-tap exit
            else -> {
                val now = System.currentTimeMillis()
                if (backPressTime != 0L && now - backPressTime < 2000) {
                    onExit()
                } else {
                    backPressTime = now
                    showToast("For exit press again")
                }
            }
        }
    }
}
```
- **State:** All state holders passed as parameters for reactivity
- **Order:** Checked in cascade; first match returns without executing remaining conditions
- **Double-tap:** Stored in local mutableState, reset on app exit

### 4.7 Window Insets Handling (Root Layout Padding)
```kotlin
// Source: MainActivity.java:127-133

Scaffold(
    modifier = Modifier
        .fillMaxSize()
        .imePadding()  // Compose built-in IME padding
        .navigationBarsPadding()  // Compose built-in nav bar padding
) { paddingValues ->
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) { page ->
        // Page content
    }
}
```
- **Alternative:** Use WindowInsets.asPaddingValues() directly
- **Note:** Compose handles IME visibility detection natively; no need for BaseActivity keyboard broadcasting

### 4.8 ActionMode Bar (Custom Composable)
```kotlin
// Source: MainActivity.java:284-340

@Composable
fun ActionModeBar(
    isActive: Boolean,
    itemCount: Int,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    palette: Palette?
) {
    if (!isActive) return
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(palette?.bg ?: MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$itemCount selected",
            color = palette?.text ?: Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.Check, contentDescription = null, tint = palette?.accent ?: Color.White)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = palette?.accent ?: Color.White)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null, tint = palette?.accent ?: Color.White)
            }
        }
    }
}
```
- **State:** actionModeActive (boolean in ViewModel)
- **Theming:** Uses LocalPalette or passed Palette parameter

### 4.9 Data Flow: Palette → StateHolder → Recompose
```kotlin
class AppViewModel : ViewModel() {
    private val _palette = MutableState<Palette?>(null)
    val palette: State<Palette?> get() = _palette
    
    fun updatePaletteForPage(page: Int) {
        _palette.value = Palette.forGroup(context, Tabs.groupForPosition(page))
    }
}

// In Shell composable:
val appViewModel: AppViewModel = viewModel(factory = ...)
val pagerState = rememberPagerState(initialPage = Tabs.START_PAGE) { Tabs.PAGE_COUNT }

LaunchedEffect(pagerState.currentPage) {
    appViewModel.updatePaletteForPage(pagerState.currentPage)
}

CompositionLocalProvider(LocalPalette provides appViewModel.palette.value) {
    TopAppBar(/* ... */)
    HorizontalPager(/* ... */)
}
```

### 4.10 Day-Scope Update Trigger
```kotlin
class AppViewModel : ViewModel() {
    private val _dayScope = MutableState(0)
    val dayScope: State<Int> get() = _dayScope
    
    fun refreshDayScope() {
        _dayScope.value = App.setDayScopeValue()  // Returns Int, update Kotlin side
    }
}

// In MainActivity.onCreate (via LaunchedEffect):
LaunchedEffect(Unit) {
    appViewModel.refreshDayScope()
}

// Reactively update on page change & panel collapse (optional):
LaunchedEffect(pagerState.currentPage, panelState.value.isExpanded) {
    appViewModel.refreshDayScope()
}
```
- **Midnight reset:** Wrap refreshDayScope() in a coroutine job that recalculates at calendar boundaries (requires new implementation)

---

## 5. Files to Delete Once Migrated

### Activities & Fragments
- `/app/src/main/java/com/shumidub/todoapprealm/ui/activity/main/MainActivity.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/activity/base/BaseActivity.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/fragment/FolderSlidingPanelFragment.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/note_fragment/FolderNoteFragment.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/note_fragment/FolderNotesRecyclerViewAdapter.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/note_fragment/NotesRecyclerViewAdapter.java`

### ViewPager & Adapters
- `/app/src/main/java/com/shumidub/todoapprealm/ui/activity/main/CustomViewPager.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/activity/main/MainPagerAdapter.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/adapter/FolderOfTaskRecyclerViewAdapter.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTaskFragmentPagerAdapter.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java`

### ActionMode Callbacks
- `/app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/EmptyActionModeCallback.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/task/FolderActionModeCallback.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/note/FolderNoteActionModeCallback.java`

### Layouts
- `/app/src/main/res/layout/activity_main.xml`
- `/app/src/main/res/layout/slide_up_panel_layout.xml`
- `/app/src/main/res/layout/note_fragment_layout.xml` (and all fragment layouts)
- `/app/src/main/res/layout/dialog_add_folder_layout.xml`
- `/app/src/main/res/layout/[all RecyclerView item layouts]`

### Libraries (Gradle)
- `com.sothree:slidinguppanel:3.4.0` (replaced by custom AnchoredDraggable)
- `net.yslibrary.android:keyboardvisibilityevent:0.1.4` (replaced by WindowInsets.ime)
- `io.reactivex.rxjava2:rxjava:2.x.x` (if only used for navigation; coroutines replace)

---

## 6. Summary of Key Differences

| Aspect | Java/XML | Compose |
|--------|----------|---------|
| **Pager** | `ViewPager` (legacy) | `HorizontalPager` + `PagerState` |
| **Touch lock** | `CustomViewPager.setPageCanChangedScrolled()` | `HorizontalPager.userScrollEnabled` reactive binding |
| **Panel** | `SlidingUpPanelLayout` (library) | `AnchoredDraggable` custom composable |
| **Back-press** | `onBackPressed()` imperative callback | `BackHandler` recomposable DSL |
| **Palette** | Instance variable + per-tab wrapping | `LocalPalette` CompositionLocal |
| **ActionBar** | Android AppCompat `ActionBar` + XML | M3 `TopAppBar` composable |
| **Keyboard detection** | `BaseActivity.OnGlobalLayoutListener` + broadcasts | `WindowInsets.ime` native in Compose |
| **Window tinting** | `getWindow().setStatusBarColor()` in callback | `LaunchedEffect` on page change |
| **State holder** | Fragments + static App fields | ViewModel + MutableState |
| **Recomposition** | Manual `invalidateOptionsMenu()`, `notifyDataSetChanged()` | Automatic on State change |

---

## 7. Verification Checklist for Compose Impl

- [ ] HorizontalPager starts on page 1 (Tasks1)
- [ ] Palette colors apply to status/nav bars per tab (green=Tasks1, blue=Tasks2, yellow=Tasks3, purple=Notes)
- [ ] Day-scope counter visible on all tabs except page 4 (Notes tab)
- [ ] Day-scope counter updates after restore or manual refresh
- [ ] Panel collapses on back press from expanded state (pages 1-4)
- [ ] Action-mode dismisses on back press (page 0)
- [ ] Note detail back navigates to folder list (page 0)
- [ ] Double-tap to exit shows toast "For exit press again" on first tap
- [ ] ViewPager swipe disabled when panel expanded
- [ ] Page 0 up button shown only in note detail view
- [ ] Panel footer fades as it slides up; hidden at >85% offset
- [ ] TopAppBar title tracks current folder (panel expanded) or "Tasks" (panel collapsed)
- [ ] All color resources imported (cornflower, canary, indigo) and mapped to Palette in Kotlin
- [ ] WindowInsets padding applied correctly (no clipping of nav bar or IME)
- [ ] Midnight boundary tested: dayScope resets on app restart after midnight

---

End of spec. Reference: COMPOSE-MIGRATION-PLAN.md Phase 0 (Scaffold) approval date 2026-06-13.
