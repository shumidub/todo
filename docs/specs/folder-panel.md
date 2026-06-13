# Folder Panel Subsystem Spec: XML→Compose Migration

> Single source of truth for implementing the folder-panel Compose rewrite.
> Target: maps sothree SlidingUpPanel, folder CRUD dialogs, drag-reorder, contextual action-mode,
> and bottom add-task panel to custom AnchoredDraggable composable + M3 dialogs.

## 1. Current Behavior (Exhaustive)

### 1.1 Layout hierarchy

**XML root:** `slide_up_panel_layout.xml` (FolderSlidingPanelFragment:149)
- `SlidingUpPanelLayout` (sothree; `com.sothree.slidinguppanel:slidinguppanel`)
  - **Peek height:** 24dp (attribute `umanoPanelHeight`)
  - **Drag view:** `ll_footer` (LinearLayout, 24dp tall, "Tasks" label)
  - **Overlay mode:** `umanoOverlay="false"` (panel pushes content, not overlaid)
  - **Shadow:** 4dp (attribute `umanoShadowHeight`)
  - **Scrollable view:** `rv_lists` (RecyclerView; feeds drag gestures)

**Lower panel layout** (includes):
- `ll_footer` (24dp): footer bar with "Tasks"/"Notes" text (line 172)
- `slide_up_panel_include_task_fragment_viewpager_layout.xml`: ViewPager (SmallTasksViewPager)

**Upper (collapsible) content** (`folder_task_container.xml`):
- RecyclerView `rv_lists` (folder cards)
- LinearLayout `ll_empty_state` (hidden by default, "no folders" message)
- LinearLayout `ll_bottom` (add-task input panel):
  - "add new task ..." hint + 4 toggle buttons (points / max / priority / cycling)
  - EditText `et` (multi-line, max 120dp height)

### 1.2 Panel states & transitions

**Initial state (onViewCreated:174-176):** COLLAPSED (24dp peek visible)
- footer visible (alpha 1.0)
- folder list RV above (constrained)

**Expanded state:**
- Panel slides up to fill screen
- footer fades out (`alpha = 1 - slideOffset`, line 182)
- Footer hidden when `slideOffset > 0.87` (line 184)
- ViewPager visible, folder cards hidden
- Action bar title changes to folder name (line 201-202)

**Snap/fling behavior:**
- SlidingUpPanelLayout handles physics; no explicit snap code
- Intermediate offset (0.3 < offset < 0.7): action-mode auto-dismissed (line 189)

### 1.3 Folder CRUD

**Add Folder (AddFolderDialog:29-108):**
- Dialog shows on menu click (overflow "Add category"; FolderSlidingPanelFragment:474)
- Input: `etName` (EditText), `cbIsDaily` (CheckBox), `tabColorToggleGroup` (MaterialButtonToggleGroup)
- Color picker: 4 buttons (Green/Blue/Yellow/Indigo) mapped to taskGroup 0/1/2/3 (TabColorPickerHelper:19-35)
- On confirm: `FolderTaskRealmController.addFolder(text, isDaily, group)` → triggers adapter refresh (line 72)
- Keyboard shown on open (line 479-481); hidden on cancel/confirm (line 88, 78)
- Back-press blocked (setOnKeyListener KEYCODE_BACK, line 96-101)
- Dialog doesn't close automatically; caller dismisses via IMM

**Edit Folder (EditDelFolderDialog:29-152):**
- Two modes: `EDIT_LIST` or `DELETE_LIST` (static strings, line 32-34)
- EDIT: shows same layout as AddFolderDialog, pre-populates fields (line 85-87)
- DELETE: confirmation dialog ("Are you sure?")
- On EDIT confirm: `FolderTaskRealmController.editFolder(folder, name, isDaily)` + `moveFolderToGroup(folder, targetGroup)` (line 98-100)
- On DELETE: checks `folderObject.getId() != defaultFolderId` before deleting (line 115)
- Both trigger `finishActionModeAndRefreshPanels()` (line 56-61): dismisses contextual action-mode + calls `notifySmallTasksViewPagerListsChanged()` on all living FolderSlidingPanelFragments
- Keyboard manually hidden (line 104, 134)
- Back-press blocked

**Color picker helper (TabColorPickerHelper:14-37):**
- Stateless utility; maps toggle-group checked ID to group (0-3)
- `resolveSelectedGroup(group)`: returns 0/1/2/3 based on checked button ID (R.id.tabColorGreen/Blue/Yellow/Indigo) or 0 (fallback)
- `setCheckedByGroup(group)`: checks the button matching the group; green for unknown

### 1.4 Folder list (RecyclerView adapter)

**FolderOfTaskRecyclerViewAdapter (FolderOfTaskRecyclerViewAdapter:29-166):**
- Holds `RealmList<FolderTaskObject>` (live from Realm)
- Item view: `folder_tasks_item_card_view.xml` (CardView with two TextViews: folder name + task count)
- Bind (onBindViewHolder:70-88):
  - `tv_note_text`: folder name, tagged with folder ID
  - `tvFolderTaskCounts`: displays done/total points for regular tabs; note count for tab 3 (Notes)
  - Formula: `done = sum(task.countAccumulation * task.countValue)`, `all = sum(task.maxAccumulation * task.countValue)` (line 106-107)
  - Color logic: palette.counter if theme active; else colorPrimaryDark if daily; else gray (line 116-122)
- Click listener: folder select → set ViewPager page, expand panel, hide keyboard (FolderSlidingPanelFragment:229-270)
- Long-click listener: enter action-mode (FolderSlidingPanelFragment:272-283)
- Palette applied (line 125-133): surface background + inputText foreground

### 1.5 Drag-reorder (folders)

**ItemTouchHelper setup (FolderSlidingPanelFragment:290-346):**
- Callback: `UP | DOWN` movements enabled; no swipe
- `onMove()` (line 303-318):
  - Track `dragFrom` (initial position) and `dragTo` (current target)
  - Call `notifyItemMoved()` immediately for UI feedback
  - Don't persist yet
- `clearView()` (line 332-339):
  - Only persist if `dragFrom != dragTo` (line 335)
  - Call `reallyMoved(from, to)` → Realm transaction: `folderOfTasksLis.add(to2, folderOfTasksLis.remove(from))` (line 326)
  - Clears drag tracking (line 338)
- Edge case: if `to >= folderOfTasksLis.size()`, clamp to `size - 1` (line 325)
- **TODO comment (line 320):** "need fix if move below 'add list'" — unclear if "add new folder" card was planned

### 1.6 Contextual action-mode (folders)

**FolderActionModeCallback (FolderActionModeCallback:24-83):**
- Triggered by long-click on folder card (FolderSlidingPanelFragment:277)
- Menu items (onCreateActionMode:40-59):
  - "edit ": opens EditDelFolderDialog (EDIT_LIST mode)
  - "delete ": opens EditDelFolderDialog (DELETE_LIST mode)
- Title: folder name (onPrepareActionMode:66)
- Keyboard toggles on edit (SHOW_FORCED; line 46-48)
- No return value in onActionItemClicked; menu click handlers do the work (line 72-74)
- Finish: called explicitly via `finishActionMode()` (FolderSlidingPanelFragment:683-685) or auto-dismissed on panel slide (line 189) or on page-select (line 359)

**EmptyActionModeCallback (EmptyActionModeCallback:7-28):**
- Used to dismiss action-mode: `activity.startSupportActionMode(new EmptyActionModeCallback())` (FolderSlidingPanelFragment:684)
- All methods return false/no-op

### 1.7 Bottom add-task panel (toggles)

**Panel structure (folder_task_container.xml:54-131):**
- Label: "add new task ..."
- 4 clickable TextViews: `task_value`, `task_max_accumulate`, `task_priority`, `task_cycling`
- EditText `et`: text input for task name

**Toggle logic (FolderSlidingPanelFragment:595-630):**

1. **`tvTaskCountValue` (points toggle, line 595-606):**
   - Click handler: `onTaskValueClick(tvTaskCountValue)`
   - Current value: `Integer.valueOf(view.getText())`
   - Cycle: 1 → 2 → ... → 9 → 10 → 1
   - Color: white if value < 2; else active accent (palette.accent or colorAccent)
   - Semantics: initial count value for new task

2. **`tvTaskMaxAccumulate` (max points toggle, line 596 + 606):**
   - Same click logic as `tvTaskCountValue`
   - Same cycling (1 → 10 → 1)
   - Semantics: `maxAccumulation` — how many times a task can be done before reset

3. **`tvTaskPriority` (priority toggle, line 568 + 608-624):**
   - Click handler: `onTaskPriorityClick()`
   - State: `priority` field (0/1/2+; default 0)
   - Cycle: 0 → 1 → 2 → 3 → 0
   - Display: "!" for priority=1; "!!" for priority=2; etc.
   - Color: white if priority=0; else active accent
   - Semantics: priority level passed to task at creation

4. **`tvTaskCycling` (cycling toggle, line 569 + 626-630):**
   - Click handler: `onTaskCyclingClick()`
   - State: `cycling` boolean (default false)
   - Toggle: false ↔ true
   - Display: "C" text
   - Color: white if false; else active accent
   - Semantics: whether task resets daily (on daily-reset pass, cycling tasks marked done today get reset to undone tomorrow)

**Task creation (line 239-256):**
- On folder card click + non-empty EditText:
  - Collect: `text` (name), `count` (tvTaskCountValue), `maxAccumulation` (tvTaskMaxAccumulate), `cycling` (field), `priority` (field)
  - Call: `TasksRealmController.addTask(text, count, maxAccumulation, cycling, priority, idFolderFromTag)`
  - Reset toggles: count=1, max=1, priority=0, cycling=false
  - Clear EditText
  - Refresh adapter + re-bind ViewPager

**Notes tab (taskGroup=3) special case (line 416-428):**
- Toggles hidden (tvTaskCountValue/Max/Priority/Cycling visibility = GONE)
- Fields still hold defaults (count=1, max=1, priority=0, cycling=false)
- Task added with these defaults (no visible UI)

### 1.8 Daily reset (cycling tasks)

**Reset trigger (onResume:452-466):**
- Compute today's date as `int todayDate = DAY_OF_YEAR + YEAR` (line 456-457)
- Compare to `lastDateResetTasksCountAccumulation` (initialized in onCreate)
- If date differs, call `resetTasksCountAccumulation()` (line 460)
- Notify each SmallTasksFragment: `setTasksAndNotifyDataSetChanged()` (line 462)

**Reset logic (line 691-711):**
- Fetch all done + partially-done tasks via `TasksRealmController.getDoneAndPartiallyDoneTasks()`
- For each task: if `task.isCycling()` AND `task.getLastDoneDate() != todayDate`:
  - Call `TasksRealmController.setTaskDoneOrParticularyDone(task, false)`
  - Mark `resetingIsUsed = true`
- Update `lastDateResetTasksCountAccumulation = todayDate`
- Return true if any task was reset
- Called on every onResume; fast if nothing changed (date check is cheap)

### 1.9 State & lifecycle

**Fragment lifecycle:**
- `onCreate`: register self in `App.folderSlidingPanelFragments` (a static list; line 135-137)
- `onDestroy`: remove from list (line 142)
- `onViewCreated`: bind views, set listeners, init ItemTouchHelper, apply palette (line 154-382)
- `onResume`: check daily reset (line 452-466)

**Data binding:**
- `folderObjects` (RealmList): live reference to `FolderTaskRealmController.getFoldersList(taskGroup)` (line 213)
- Adapter holds the same reference; Realm change listeners feed updates

**Panel state machine:**
- Sliding triggers listeners (line 179-207)
- On slide (offset change): fade footer, dismiss action-mode on 0.3-0.7, update title
- On state change: reset action-bar title to default on COLLAPSED; set to folder name on EXPANDED; invalidate options menu

**ViewPager (tasks inner pager):**
- Adapter: `SmallTaskFragmentPagerAdapter` (holds child SmallTasksFragments, one per folder)
- Current page: `smallTasksViewPager.getCurrentItem()` (used to get current folder in expand-panel flow)
- Rebuilt on: folder list changes, Realm reload (line 255-256, 663-664)

### 1.10 Empty state

**Condition (line 726-735):**
- Shown if adapter item count == 0
- Hides RV; shows LinearLayout `ll_empty_state` (which includes `empty_state` layout)

**Timing:**
- Set on bind (line 224)
- Updated on data changes (notifySmallTasksViewPagerListsChanged:671)

### 1.11 Action-bar & menu

**Options menu (onCreateOptionsMenu:469-526):**
- "Add category" (id=5): shows AddFolderDialog; only visible when panel COLLAPSED (line 536)
- "Add section" (id=4): shows SectionEditDialog for current folder; only visible when panel EXPANDED (line 537)
- "Backup / Sync" (id=3): shows SyncDialog (with permission check)

**Title logic (line 713-723):**
- Private field `title` (String)
- Only set if current view pager position (on MainActivity) == taskGroup + 1
- Prevents pre-created offscreen tabs from clobbering active tab's title

**Palette/theme:**
- Per-tab color scheme applied in `applyPalette()` (line 384-429)
- Sets: background, surface colors, text colors
- Notes tab (group 3): toggles hidden; custom input color (canary yellow vs body text)

### 1.12 Keyboard handling

**Visibility listener (line 372-379):**
- KeyboardVisibilityEvent.setEventListener (net.yslibrary library)
- Locks ViewPager paging if keyboard open AND panel COLLAPSED AND EditText focused (line 375-377)
- Prevents accidental page-swipe while typing in add-task field

**Manual show/hide:**
- SHOW on AddFolderDialog open (line 479-481) and on action-mode edit (FolderActionModeCallback:46-48)
- HIDE on dialog confirm/cancel (line 78, 88, 104, 134)

### 1.13 Palette (theme)

**Palette class (referenced, line 385):**
- Static `forGroup(Context, taskGroup)` → returns per-tab palette
- Fields: `bg`, `surfaceMuted`, `text`, `textSoft`, `accent`, `surface`, `inputText`, `counter`
- Applied to: root view bg, footer bg, bottom panel bg, text colors, cursor, tint
- Overridden for Notes tab (group 3): input text uses dedicated color

### 1.14 Multi-tab state

**Static registry (App.folderSlidingPanelFragments):**
- List of all living FolderSlidingPanelFragments (one per tab)
- Used by EditDelFolderDialog to refresh all tabs on folder change (line 58-59)

**Tab identity:**
- Fragment stores `taskGroup` (0/1/2/3 for Tasks1/Tasks2/Tasks3/Notes)
- Default title: "Tasks" (group 0/1/2); "Notes" (group 3) (line 118)
- Each tab has separate folder list, separate ViewPager child fragments
- Palette per-tab

---

## 2. State & Data Flow

### 2.1 Data model

**FolderTaskObject (Realm entity):**
- `id` (long, unique)
- `name` (String)
- `isDaily` (boolean)
- `folderTasks` (RealmList<TaskObject>)

**RealmFoldersContainer:**
- Singleton in Realm
- Holds 4 RealmLists: one per taskGroup (0-3)
- Each list contains FolderTaskObjects

**TaskObject (referenced in count calculations):**
- `countValue` (int)
- `maxAccumulation` (int)
- `countAccumulation` (int)
- `isCycling()` → boolean
- `getLastDoneDate()` → int (DAY_OF_YEAR + YEAR)

### 2.2 Realm threading

**Rule: main-thread-only**
- Realm initialized with `allowQueriesOnUiThread()` in production code (App.initRealm)
- All reads/writes on main thread
- FolderTaskRealmController methods call `RealmDb.write()` for transactions; otherwise direct read

**RealmList live binding:**
- Adapter holds `RealmList<FolderTaskObject>` (line 33)
- Not detached; live listener implicit
- Realm change listeners trigger `notifyDataSetChanged()` internally (implicit in Realm, or caller-driven)

### 2.3 View state holders

**Fragment fields (transient, recreated on config change):**
- `idFolderFromTag` (long): selected folder ID (used for context-mode & panel expansion)
- `priority`, `cycling` (int, boolean): current toggle state for add-task panel
- `lastDateResetTasksCountAccumulation` (int): last reset date

**Action-mode (static):**
- `actionMode` (static ActionMode; line 66)
- Not recreated on config change; reachable across fragments
- Dismiss on onPause/onDestroy implicit (fragment destroyed = action-mode dies)

### 2.4 Data mutation

**Add folder:**
1. User taps "Add category" → AddFolderDialog shown
2. Dialog.confirm → `FolderTaskRealmController.addFolder(name, isDaily, group)`
3. Realm transaction: create FolderTaskObject, add to container list
4. Dialog calls `notifySmallTasksViewPagerListsChanged()` on every FolderSlidingPanelFragment (via fragment manager walk)

**Edit folder:**
1. Long-click folder → action-mode
2. Tap "edit" → EditDelFolderDialog (EDIT_LIST) shown
3. Dialog.confirm → `editFolder()` + `moveFolderToGroup()`
4. Realm transaction: mutate fields, move across lists if needed
5. Dialog calls `finishActionModeAndRefreshPanels()` (dismisses action-mode, refreshes all tabs)

**Delete folder:**
1. Long-click folder → action-mode
2. Tap "delete" → EditDelFolderDialog (DELETE_LIST) shown
3. Dialog.confirm → `FolderTaskRealmController.deleteFolder()`
4. Realm transaction: detach/delete child tasks, remove folder from all lists, delete folder
5. Dialog calls `finishActionModeAndRefreshPanels()`

**Add task:**
1. User enters text in `et`, taps folder card
2. FolderSlidingPanelFragment.onHolderTextViewOnClickListener triggered (line 229)
3. If text not empty: `TasksRealmController.addTask(...)` with collected params
4. Else: expand panel, show tasks
5. Reset toggle state, clear ET, refresh adapter + ViewPager

**Drag-reorder folders:**
1. User drags folder card up/down
2. ItemTouchHelper.onMove fires; `notifyItemMoved()` updates UI (line 316)
3. User releases
4. ItemTouchHelper.clearView fires; `reallyMoved()` persists order in Realm transaction (line 326)

**Daily reset:**
1. Fragment.onResume called
2. Check if today's date > last reset date
3. If yes: fetch cycling tasks done yesterday, mark as undone, notify SmallTasksFragments
4. Update last reset date

### 2.5 Event flow (UI to model)

- **Direct state holders:** Fragment fields + Realm queries (lazy)
- **Change propagation:** Explicit notifyDataSetChanged() calls + Realm live queries
- **Cross-tab sync:** Static App.folderSlidingPanelFragments list walked on major changes
- **No Rx/Flow:** reactive extensions not used; manual listeners

---

## 3. Edge Cases & Gotchas

### 3.1 Empty states

**No folders (all-tab level):**
- RecyclerView shows 0 items
- `ll_empty_state` (LinearLayout with empty_state layout) shown (line 731)
- Panel still slidable; expand attempts to show ViewPager with 0 pages
- Behavior: not explicitly handled; unclear if expand is blocked

**No tasks in a folder:**
- ViewPager page shows SmallTasksFragment with empty list
- Expected: section headers + empty bottom area

**Notes tab (group 3) with no notes:**
- Toggle buttons hidden visually (visibility GONE)
- ET still works; defaults used on task creation

### 3.2 Config rotation

**Fragment recreation:**
- onSaveInstanceState/onRestoreInstanceState not overridden
- Arguments (task group) restored via Bundle
- `idFolderFromTag`, `priority`, `cycling`, `lastDateResetTasksCountAccumulation` lost
- Action-mode: survives as static; may cause double-action-mode if not cleared

**ViewPager position:**
- SmallTaskFragmentPagerAdapter rebuilt on bind
- onPageSelected fires on new page during bind; may re-trigger panel expand

**Panel state:**
- SlidingUpPanelLayout.setPanelState(COLLAPSED) called on onViewCreated (line 174-176)
- Panel always resets to peek on rotation

**Palette:**
- Re-applied in onViewCreated (line 381)

### 3.3 Back-press handling

**Fragment level:**
- BackHandler (not implemented; no Compose-style flow yet)
- Implicit: back dismisses dialogs, back closes action-mode (Android framework)

**Panel state:**
- Back during expanded panel: unclear if panel collapses or fragment closes
- Expected: collapse first; second back exits fragment

**Action-mode state:**
- Back during action-mode: android framework dismisses (onDestroyActionMode fires)

**Dialogs back-press:**
- Manually blocked (setOnKeyListener, line 96-101, 141-147)
- Back does nothing; must tap button to close

### 3.4 Race conditions & threading

**Realm live list + ItemTouchHelper drag:**
- User drags folder
- Simultaneously, folder added/removed from another source (unlikely in single-user app)
- OnMove called with stale positions
- **Mitigation:** not explicitly handled; app assumes UI-thread-only access

**Fragment destruction during dialog:**
- User opens AddFolderDialog
- Fragment destroyed (config change)
- Dialog.confirm tries to walk fragment manager for FolderSlidingPanelFragments
- **Risk:** NPE or stale callback
- **Mitigation:** dialogs check `activity.isFinishing()` before showing Toast (line 120, 125)

**Static actionMode field + multiple fragments:**
- Only one action-mode active at a time (Android framework enforces)
- But if fragment A enters action-mode, then fragment B is shown, fragment A's reference stale
- **Mitigation:** EmptyActionModeCallback used to dismiss cleanly

**Realm change listener + notifyDataSetChanged:**
- Adapter holds live RealmList; Realm fires change events
- App explicitly calls notifyDataSetChanged() in many places
- Possible double-update
- **Mitigation:** seems tolerated; Realm + manual calls coexist

### 3.5 Multi-tab state sync

**Issue:** App.folderSlidingPanelFragments is a static list; all tabs' fragments registered
- EditDelFolderDialog.finishActionModeAndRefreshPanels() walks this list (line 58-59)
- If one tab is foreground, others are pre-created but view may be null
- Calling `notifySmallTasksViewPagerListsChanged()` on background tab is safe (listeners are on live objects) but wasteful

**Potential issue:** if a background tab's adapter is null, calling notify crashes
- **Mitigation:** line 671 checks `if (rvFolders == null || folderOfTaskRVAdapter == null) return`

### 3.6 ItemTouchHelper edge case

**Drag below "add list" card:**
- Line 320 TODO comment: "need fix if move below 'add list'"
- Suggests an "add new folder" card was planned in the RV
- Current code doesn't show such a card; adapter only shows folders + items
- Edge case: if added, drag-to clamp logic (line 325) handles it, but confirm may be wrong

**Clamp behavior:**
- If dragTo >= folderOfTasksLis.size(), reset to size-1 (line 325)
- Dragging to end of list → pinned to last item
- Confirms drag only if dragFrom != dragTo (line 335)

### 3.7 Keyboard visibility vs panel state

**Issue (line 372-379):**
- Paging locked when keyboard visible AND panel COLLAPSED AND ET focused
- But keyboard can be visible while panel EXPANDED (e.g., editing folder name in dialog)
- Logic doesn't prevent page-swipe in that case
- **Risk:** minor; dialogs are modal, so pager behind is not interactive anyway

### 3.8 Title clobbering (ViewPager pre-creation)

**Issue (line 713-723):**
- ViewPager pre-creates off-screen pages (default 1-page buffer)
- Each tab's fragment calls `setTitle()` in onViewCreated
- Only checked if current tab (position == taskGroup + 1)
- **Fix:** defensive check prevents title overwrite
- But comment notes the problem (line 360-362)

### 3.9 NotifySmallTasksViewPagerListsChanged inefficiency

**Rebuilds adapter + ViewPager from scratch:**
- Line 659: `folderOfTaskRVAdapter = new FolderOfTaskRecyclerViewAdapter(folderObjects, getActivity(), taskGroup)`
- Line 663: `smallTaskFragmentPagerAdapter = new SmallTaskFragmentPagerAdapter(getChildFragmentManager(), taskGroup)`
- Called on: folder add/edit/delete/reload
- **Cost:** high; destroys/recreates all child fragments
- **Expected behavior:** should be fine for small folder counts (< 20)

### 3.10 LastDoneDate calculation

**Formula (line 699-700):**
```java
int todayDate = Integer.valueOf("" + Calendar.getInstance().get(Calendar.DAY_OF_YEAR) +
        Calendar.getInstance().get(Calendar.YEAR));
```
- Concatenates DAY_OF_YEAR (1-366) + YEAR (e.g., 2024)
- Result: "1362024" (day 136 of 2024)
- **Assumption:** year never changes during app lifetime (okay)
- **Edge case:** leap year handling built-in (Calendar.DAY_OF_YEAR respects Feb 29)

### 3.11 Default folder protection

**Line 115 check:**
```java
if (folderObject.getId() != defaultFolderId)
```
- `defaultFolderId` is declared but never initialized (line 42)
- **Bug:** always 0L (default long); assumes default folder ID is 0
- Works if first folder created has ID 0; breaks if incremented IDs used
- **Fix needed:** set defaultFolderId from somewhere (config? Realm query?)

---

## 4. Compose Mapping

### 4.1 Architecture integration

**Per COMPOSE-MIGRATION-PLAN.md:**
- **Data-interop layer:** Kotlin repositories wrapping Java controllers
  - Reads: `Flow<List<FolderTaskObject>>` via Realm change listener + `copyFromRealm()`
  - Writes: delegate to FolderTaskRealmController
- **ViewModels:** Dagger-injected into composables
- **CompositionLocal:** `LocalPalette` (per-tab palette)
- **Custom AnchoredDraggable:** replaces SlidingUpPanelLayout

### 4.2 Composable structure

**New files to create:**

1. **`FolderPanelViewModel.kt` (ViewModel)**
   - State: `selectedFolderId: Long`, `folders: Flow<List<FolderTaskObject>>`, `currentPanelState: AnchorState` (COLLAPSED/EXPANDED)
   - `addTaskParams: TaskCreationState` (count, max, priority, cycling)
   - Events:
     - `onFolderSelected(id)` → set selected folder, expand panel
     - `onTaskCountClick()` → cycle count, update state
     - `onTaskMaxClick()` → cycle max
     - `onTaskPriorityClick()` → cycle priority
     - `onTaskCyclingClick()` → toggle cycling
     - `onAddTaskConfirm(text, folderId)` → call TasksRealmController.addTask(), reset state
     - `onFolderDragEnd(fromIndex, toIndex)` → call FolderTaskRealmController reorder (in transaction)
     - `onPanelStateChanged(state)` → update title, invalidate menu
     - `onDailyResetCheck()` → check date, reset cycling tasks

2. **`FolderPanelScreen.kt` (composable)**
   ```kotlin
   @Composable
   fun FolderPanelScreen(viewModel: FolderPanelViewModel = viewModel()) {
       val folders by viewModel.folders.collectAsState(emptyList())
       val panelState = rememberAnchoredDraggableState(initialState = COLLAPSED)
       val currentAccent = LocalPalette.current.accent
       
       Box {
           // Folder list (behind)
           FolderList(
               folders = folders,
               onFolderClick = { folder -> viewModel.onFolderSelected(folder.id) },
               onFolderLongClick = { folder -> showFolderActionMode(folder) },
               onFolderDragEnd = viewModel::onFolderDragEnd,
               modifier = Modifier.fillMaxSize()
           )
           
           // Sliding panel (draggable)
           AnchoredDraggableBox(
               state = panelState,
               anchors = mapOf(COLLAPSED to 24.dp, EXPANDED to 0.dp),
               onStateChange = viewModel::onPanelStateChanged,
               modifier = Modifier.fillMaxSize()
           ) {
               // Peek (24dp footer)
               PeekBar(
                   title = when(panelState.currentValue) {
                       COLLAPSED -> "Tasks"
                       EXPANDED -> selectedFolderName
                   },
                   alpha = 1f - panelState.offset
               )
               
               // Expanded content (ViewPager → HorizontalPager for SmallTasks)
               SmallTasksViewPager(currentFolder)
           }
           
           // Add-task panel (bottom)
           AddTaskPanel(
               count = viewModel.taskCreationState.count,
               max = viewModel.taskCreationState.max,
               priority = viewModel.taskCreationState.priority,
               cycling = viewModel.taskCreationState.cycling,
               onCountClick = viewModel::onTaskCountClick,
               onMaxClick = viewModel::onTaskMaxClick,
               onPriorityClick = viewModel::onTaskPriorityClick,
               onCyclingClick = viewModel::onTaskCyclingClick,
               onAddTask = viewModel::onAddTaskConfirm,
               accent = currentAccent
           )
       }
   }
   ```

3. **`FolderListComposable.kt`**
   ```kotlin
   @Composable
   fun FolderList(
       folders: List<FolderTaskObject>,
       onFolderClick: (FolderTaskObject) -> Unit,
       onFolderLongClick: (FolderTaskObject) -> Unit,
       onFolderDragEnd: (Int, Int) -> Unit
   ) {
       if (folders.isEmpty()) {
           EmptyStateContent()
           return
       }
       
       var draggedIndex by remember { mutableIntStateOf(-1) }
       
       LazyColumn(
           modifier = Modifier
               .fillMaxSize()
               .padding(6.dp),
           state = rememberLazyListState()
       ) {
           itemsIndexed(folders) { index, folder ->
               FolderCard(
                   folder = folder,
                   isDragging = draggedIndex == index,
                   onLongPress = { onFolderLongClick(folder); draggedIndex = index },
                   onClick = { onFolderClick(folder) },
                   onDragEnd = { newIndex -> onFolderDragEnd(index, newIndex); draggedIndex = -1 },
                   modifier = Modifier.draggable(
                       state = rememberDraggableState(
                           onDelta = { /* reorder RV locally */ }
                       ),
                       orientation = Orientation.Vertical
                   )
               )
           }
       }
   }
   ```

4. **`FolderCardComposable.kt`**
   ```kotlin
   @Composable
   fun FolderCard(
       folder: FolderTaskObject,
       isDragging: Boolean,
       onLongPress: () -> Unit,
       onClick: () -> Unit,
       onDragEnd: (Int) -> Unit,
       modifier: Modifier = Modifier
   ) {
       Card(
           modifier = modifier
               .fillMaxWidth()
               .padding(bottom = 4.dp)
               .combinedClickable(
                   onClick = onClick,
                   onLongClick = onLongPress
               )
       ) {
           Row(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(8.dp),
               horizontalArrangement = Arrangement.SpaceBetween,
               verticalAlignment = Alignment.CenterVertically
           ) {
               Text(
                   text = folder.name,
                   style = MaterialTheme.typography.bodyMedium,
                   modifier = Modifier.weight(1f),
                   maxLines = 7,
                   overflow = TextOverflow.Ellipsis
               )
               Text(
                   text = computeTaskCounts(folder),
                   style = MaterialTheme.typography.labelSmall,
                   color = LocalPalette.current.counter
               )
           }
       }
   }
   ```

5. **`AddTaskPanelComposable.kt`**
   ```kotlin
   @Composable
   fun AddTaskPanel(
       count: Int,
       max: Int,
       priority: Int,
       cycling: Boolean,
       onCountClick: () -> Unit,
       onMaxClick: () -> Unit,
       onPriorityClick: () -> Unit,
       onCyclingClick: () -> Unit,
       onAddTask: (String) -> Unit,
       accent: Color,
       modifier: Modifier = Modifier
   ) {
       var taskName by remember { mutableStateOf("") }
       
       Column(
           modifier = modifier
               .fillMaxWidth()
               .background(LocalPalette.current.surfaceMuted)
               .padding(8.dp)
       ) {
           Row(
               modifier = Modifier.fillMaxWidth(),
               horizontalArrangement = Arrangement.SpaceBetween,
               verticalAlignment = Alignment.CenterVertically
           ) {
               Text("add new task ...", color = Color.White, modifier = Modifier.weight(1f))
               
               ToggleButton(
                   text = "$count",
                   onClick = onCountClick,
                   isActive = count > 1,
                   accent = accent
               )
               ToggleButton(
                   text = "$max",
                   onClick = onMaxClick,
                   isActive = max > 1,
                   accent = accent
               )
               ToggleButton(
                   text = "!".repeat(maxOf(1, priority)),
                   onClick = onPriorityClick,
                   isActive = priority > 0,
                   accent = accent
               )
               ToggleButton(
                   text = "C",
                   onClick = onCyclingClick,
                   isActive = cycling,
                   accent = accent
               )
           }
           
           TextField(
               value = taskName,
               onValueChange = { taskName = it },
               modifier = Modifier.fillMaxWidth(),
               keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
               keyboardActions = KeyboardActions(
                   onDone = {
                       if (taskName.isNotEmpty()) {
                           onAddTask(taskName)
                           taskName = ""
                       }
                   }
               )
           )
       }
   }
   ```

6. **`FolderDialogsComposable.kt`**
   ```kotlin
   @Composable
   fun AddFolderDialog(
       onConfirm: (name: String, isDaily: Boolean, group: Int) -> Unit,
       onDismiss: () -> Unit
   ) {
       var name by remember { mutableStateOf("") }
       var isDaily by remember { mutableStateOf(true) }
       var selectedGroup by remember { mutableIntStateOf(0) }
       
       AlertDialog(
           onDismissRequest = onDismiss,
           title = { Text("Add new folder") },
           text = {
               Column {
                   OutlinedTextField(
                       value = name,
                       onValueChange = { name = it },
                       label = { Text("Folder name") }
                   )
                   Checkbox(
                       checked = isDaily,
                       onCheckedChange = { isDaily = it },
                       label = { Text("Daily") }
                   )
                   TabColorPicker(
                       selectedGroup = selectedGroup,
                       onGroupSelected = { selectedGroup = it }
                   )
               }
           },
           confirmButton = {
               Button(onClick = {
                   if (name.isNotEmpty()) {
                       onConfirm(name, isDaily, selectedGroup)
                       onDismiss()
                   }
               }) { Text("Add") }
           },
           dismissButton = {
               Button(onClick = onDismiss) { Text("Cancel") }
           }
       )
   }
   
   @Composable
   fun TabColorPicker(selectedGroup: Int, onGroupSelected: (Int) -> Unit) {
       Row {
           repeat(4) { group ->
               Button(
                   onClick = { onGroupSelected(group) },
                   colors = ButtonDefaults.buttonColors(
                       containerColor = when(group) {
                           0 -> Color.Green
                           1 -> Color.Blue
                           2 -> Color.Yellow
                           3 -> Color.Magenta
                           else -> Color.Gray
                       }
                   ),
                   modifier = Modifier.weight(1f)
               ) {}
           }
       }
   }
   
   @Composable
   fun EditFolderDialog(
       folder: FolderTaskObject,
       onConfirm: (name: String, isDaily: Boolean, group: Int) -> Unit,
       onDismiss: () -> Unit
   ) {
       // Similar to AddFolderDialog, pre-filled with folder data
   }
   
   @Composable
   fun DeleteFolderConfirmDialog(
       folder: FolderTaskObject,
       onConfirm: () -> Unit,
       onDismiss: () -> Unit
   ) {
       AlertDialog(
           onDismissRequest = onDismiss,
           title = { Text("Delete ${folder.name}?") },
           text = { Text("Are you sure?") },
           confirmButton = {
               Button(onClick = {
                   onConfirm()
                   onDismiss()
               }) { Text("DELETE") }
           },
           dismissButton = {
               Button(onClick = onDismiss) { Text("Cancel") }
           }
       )
   }
   ```

### 4.3 AnchoredDraggable mapping

**Replaces SlidingUpPanelLayout:**

```kotlin
@Composable
fun FolderPanelScreen() {
    val panelState = rememberAnchoredDraggableState(
        initialValue = PanelAnchor.COLLAPSED,
        positionalThreshold = { distance -> distance * 0.5f },
        velocityThreshold = { 125.dp.toPx() }
    )
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Back content (folders list)
        FolderList(...)
        
        // Draggable panel
        Column(
            modifier = Modifier
                .fillMaxSize()
                .anchoredDraggable(
                    state = panelState,
                    orientation = Orientation.Vertical,
                    enabled = true
                )
        ) {
            // Peek bar (24dp, drag handle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(LocalPalette.current.surfaceMuted)
                    .alpha(1f - panelState.offset) // fade as slides up
            ) {
                Text(
                    "Tasks",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            
            // Expanded content (ViewPager)
            SmallTasksViewPager(
                modifier = Modifier
                    .fillMaxSize()
                    .visibility(
                        if (panelState.offset > 0.87) Visibility.GONE else Visibility.VISIBLE
                    )
            )
        }
    }
}

enum class PanelAnchor {
    COLLAPSED,
    EXPANDED
}
```

**Anchor heights:**
- COLLAPSED: 24.dp (peek bar height)
- EXPANDED: 0.dp (top of screen)

**Offset calculation:** Compose's AnchoredDraggable handles; offset = 0 at EXPANDED, 1 at COLLAPSED.

**Fling/snap:** Built-in; tunable via `positionalThreshold` and `velocityThreshold`.

### 4.4 Drag-reorder integration

**Replace ItemTouchHelper with `sh.calvin.reorderable` library:**

```kotlin
val reorderableLazyListState = rememberReorderableLazyListState(
    onMove = { from, to ->
        val reorderedFolders = folders.toMutableList().apply {
            add(to, removeAt(from))
        }
        // Update ViewModel, which calls FolderTaskRealmController.reorder()
        viewModel.onFoldersDragReordered(reorderedFolders)
    }
)

LazyColumn(
    state = reorderableLazyListState.lazyListState,
    modifier = Modifier
        .reorderable(reorderableLazyListState)
) {
    itemsIndexed(folders) { index, folder ->
        FolderCard(
            folder = folder,
            modifier = Modifier.reorderableItem(
                reorderableLazyListState,
                key = folder.id
            )
        )
    }
}
```

### 4.5 Dialog integration (M3)

**Add Folder:** Material3 AlertDialog with TextField + Checkbox + custom color picker
**Edit Folder:** Same as Add, pre-filled
**Delete Folder:** Simple confirm dialog
**Action-mode:** Composable dialog with edit/delete buttons (replaces Android ActionMode)

### 4.6 ViewModel state

**State holders:**
```kotlin
class FolderPanelViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val _folders = MutableStateFlow<List<FolderTaskObject>>(emptyList())
    val folders: StateFlow<List<FolderTaskObject>> = _folders.asStateFlow()
    
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()
    
    private val _panelState = MutableStateFlow<PanelAnchor>(PanelAnchor.COLLAPSED)
    val panelState: StateFlow<PanelAnchor> = _panelState.asStateFlow()
    
    private val _taskCreationState = MutableStateFlow(
        TaskCreationState(count = 1, max = 1, priority = 0, cycling = false)
    )
    val taskCreationState: StateFlow<TaskCreationState> = _taskCreationState.asStateFlow()
    
    init {
        viewModelScope.launch {
            folderRepository.getFolders(taskGroup).collect { newFolders ->
                _folders.value = newFolders
            }
        }
    }
    
    fun onFolderSelected(id: Long) {
        _selectedFolderId.value = id
        _panelState.value = PanelAnchor.EXPANDED
    }
    
    fun onTaskCountClick() {
        val current = _taskCreationState.value.count
        val next = if (current < 10) current + 1 else 1
        _taskCreationState.value = _taskCreationState.value.copy(count = next)
    }
    
    fun onAddTaskConfirm(text: String, folderId: Long) {
        if (text.isEmpty()) return
        val state = _taskCreationState.value
        viewModelScope.launch {
            taskRepository.addTask(
                text = text,
                count = state.count,
                maxAccumulation = state.max,
                cycling = state.cycling,
                priority = state.priority,
                folderId = folderId
            )
            _taskCreationState.value = TaskCreationState() // reset
        }
    }
    
    fun onFolderDragEnd(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            folderRepository.reorderFolders(fromIndex, toIndex)
        }
    }
}
```

### 4.7 Repository layer (data-interop)

```kotlin
class FolderRepository @Inject constructor() {
    fun getFolders(taskGroup: Int): Flow<List<FolderTaskObject>> = flow {
        // Use Realm change listener + copyFromRealm
        FolderTaskRealmController.getFoldersList(taskGroup).addChangeListener { _ ->
            val detached = FolderTaskRealmController.getFoldersList(taskGroup)
                .map { it.copyFromRealm() }
            emit(detached)
        }
    }
    
    suspend fun addFolder(name: String, isDaily: Boolean, group: Int) {
        withContext(Dispatchers.Main) {
            FolderTaskRealmController.addFolder(name, isDaily, group)
        }
    }
    
    suspend fun editFolder(folder: FolderTaskObject, name: String, isDaily: Boolean) {
        withContext(Dispatchers.Main) {
            FolderTaskRealmController.editFolder(folder, name, isDaily)
        }
    }
    
    suspend fun deleteFolder(folder: FolderTaskObject) {
        withContext(Dispatchers.Main) {
            FolderTaskRealmController.deleteFolder(folder)
        }
    }
    
    suspend fun reorderFolders(fromIndex: Int, toIndex: Int) {
        withContext(Dispatchers.Main) {
            val folders = FolderTaskRealmController.getFoldersList(currentTaskGroup)
            // Realm transaction: add(toIndex, remove(fromIndex))
            FolderTaskRealmController.reorderFolders(folders, fromIndex, toIndex)
        }
    }
}
```

### 4.8 Back-press state machine

```kotlin
@Composable
fun FolderPanelWithBackHandler(viewModel: FolderPanelViewModel) {
    val panelState by viewModel.panelState.collectAsState()
    val actionModeActive by viewModel.actionModeActive.collectAsState()
    
    BackHandler(enabled = true) {
        when {
            actionModeActive -> {
                // Dismiss action-mode
                viewModel.dismissActionMode()
            }
            panelState == PanelAnchor.EXPANDED -> {
                // Collapse panel
                viewModel.collapsePanelRequest()
            }
            else -> {
                // Exit fragment (or double-tap check)
                // Propagate to activity
            }
        }
    }
    
    // Content...
}
```

### 4.9 Palette integration

```kotlin
val LocalPalette = compositionLocalOf<Palette> { error("No palette") }

@Composable
fun FolderPanelThemed(taskGroup: Int, content: @Composable () -> Unit) {
    val palette = Palette.forGroup(taskGroup)
    CompositionLocalProvider(LocalPalette provides palette) {
        content()
    }
}
```

---

## 5. Files to Delete (Once Migrated)

### 5.1 Fragment & MVP

- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/fragment/FolderSlidingPanelFragment.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/fragment/IViewFolderSlidingPanelFragment.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/presenter/PresenterFolderSlidingPanelFragment.java`

### 5.2 Adapter

- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/folder_panel_sliding_fragment/adapter/FolderOfTaskRecyclerViewAdapter.java`

### 5.3 Dialogs

- `/app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/AddFolderDialog.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/EditDelFolderDialog.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/TabColorPickerHelper.java`

### 5.4 Action-mode

- `/app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/task/FolderActionModeCallback.java`

### 5.5 Layouts

- `/app/src/main/res/layout/slide_up_panel_layout.xml`
- `/app/src/main/res/layout/slide_up_panel_include_task_fragment_viewpager_layout.xml`
- `/app/src/main/res/layout/folder_task_container.xml`
- `/app/src/main/res/layout/dialog_add_folder_layout.xml`
- `/app/src/main/res/layout/folder_tasks_item_card_view.xml`
- `/app/src/main/res/layout/folder_card_view.xml` (if unused)
- `/app/src/main/res/layout/folder_card_view_add_new_folder.xml` (if unused)

### 5.6 Dependencies (gradle)

- `com.sothree.slidinguppanel` (sothree SlidingUpPanel)
- `net.yslibrary.android.keyboardvisibilityevent` (KeyboardVisibilityEvent)

---

## Summary of Key Differences (XML → Compose)

| Aspect | XML/Java | Compose |
|--------|----------|---------|
| **Panel** | SlidingUpPanelLayout (sothree lib) | Custom AnchoredDraggable + Box |
| **Folder list** | RecyclerView + FolderOfTaskRecyclerViewAdapter | LazyColumn + reorderable lib |
| **Drag-reorder** | ItemTouchHelper | `sh.calvin.reorderable` LazyColumnState |
| **Dialogs** | AlertDialog.Builder + fragments | Material3 AlertDialog |
| **Color picker** | MaterialButtonToggleGroup XML | Row of custom color buttons |
| **Action-mode** | Android ActionMode + callback | Composable dialog w/ buttons |
| **Keyboard** | KeyboardVisibilityEvent listener | WindowInsets.ime |
| **State** | Fragment fields + static fields | ViewModel StateFlow |
| **Data flow** | Realm live + manual notify | Flow + Compose recomposition |
| **Palette** | Applied via setTextColor, etc. | CompositionLocal (reactive) |
| **Back-press** | Implicit (framework) | BackHandler composable |

---

## Verification Checklist

Before closing the migration:

- [ ] Panel collapses/expands smoothly with 24dp peek visible at rest
- [ ] Footer text fades as panel slides (offset-driven alpha)
- [ ] Folder cards render with name + done/total count
- [ ] Long-press folder → action-mode dialog with edit/delete
- [ ] Edit/delete dialogs + color picker functional
- [ ] Add folder button shows dialog, creates folder in Realm
- [ ] Drag-reorder folders persists in Realm
- [ ] Add-task panel: all 4 toggles cycle correctly
- [ ] Add-task submit: creates task with collected params in Realm
- [ ] Notes tab (group 3): toggles hidden, task creation still works
- [ ] Panel expand on folder click → shows ViewPager page for folder
- [ ] onResume daily reset: cycling tasks reset if date changed
- [ ] Empty state shows if no folders
- [ ] Keyboard locks ViewPager paging when open in add-task ET
- [ ] Config rotation: panel resets to peek; toggles reset; action-mode cleared
- [ ] Multi-tab: all FolderSlidingPanelFragments update on folder change
- [ ] Back-press: panel expand → collapse; action-mode → dismiss; no-op at rest

---

**Spec author:** Code analysis (exhaustive source read)  
**Date created:** 2026-06-13  
**Target architecture:** COMPOSE-MIGRATION-PLAN.md Phase 2
