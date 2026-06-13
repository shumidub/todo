# Task Subsystem Migration Spec — todo100android

## 1. Current Behavior (Exhaustive)

### 1.1 Data Model

**TaskObject** (`realmmodel/task/TaskObject.java:14-136`)
- `id` (long): Primary key; stable across app lifecycle
- `text` (String): Task description; max 7 lines clamp in list view (task_card_view.xml:41)
- `done` (boolean): Fully completed state; mutually exclusive with partial completion
- `taskFolderId` (long): Primary folder ID; task visible in this folder by default
- `extraFolderIds` (RealmList<Long>): Multi-category membership; nullable for backwards compat (line 29)
- `priority` (int): 0..3 (cycles via +1 mod 4; line 197 TaskActionModeCallback)
- `countValue` (int): Points per completion event; range 1..9
- `maxAccumulation` (int): Daily goal; range 1..9
- `countAccumulation` (int): Current day's progress toward goal
- `dateCountAccumulation` (RealmList<RealmInteger>): Historical completion dates (one entry = one day's completion event)
- `isCycling` (boolean): If true, task resets daily (when `lastDoneDate != today`)
- `lastDoneDate` (int): YYYYDDD format; used to detect daily boundary
- `sectionId` (long): Parent section ID; 0 = "free zone" (outer space, not under any section) — added in SCHEMA_VERSION 4
- `position` (int): Ordering index within folder (backfilled from RealmList index in v4 migration)

**SectionObject** (`realmmodel/task/SectionObject.java:19-53`)
- `id` (long): Primary key
- `name` (String): Section title; required; max 40 chars (dialog_section_edit.xml line 107)
- `parentFolderId` (long): Folder that owns this section (indexed)
- `position` (int): Outer-space ordering (not per-task ordering within section)
- `collapsedByDefault` (boolean): UI preference on first open
- `currentlyCollapsed` (boolean): Live collapse state; toggled by user clicking header

### 1.2 Fragment & View Hierarchy

**SmallTasksFragment** (`ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java`)
- Purpose: Displays task list within a single folder (tasked folder picked by parent pager)
- Init: `newInstance(folderId)` stores folder ID in Bundle (line 58)
- Lifecycle:
  - `onCreate`: Extract `tasksFolderId` from Bundle (line 73)
  - `onCreateView`: Inflate `rv_fragment_template_layout` (contains RecyclerView + empty state) (line 108)
  - `onViewCreated`: 
    - Listen for SectionEditDialog results via FragmentResultListener (line 126, key = `section_changed`)
    - Setup RecyclerView, adapter, drag helper, palette (line 164)
    - Setup item click listeners (line 163)
  - `onResume`: Re-flatten to refresh collapsed sections if changed externally (line 116-118)
- State:
  - `tasks`: List<TaskObject> (not-done tasks only, from controller)
  - `doneTasks`: List<TaskObject> (done tasks)
  - `isAllTaskShowing`: bool; toggles "all tasks" vs "undone only" mode
  - `rvTasks`: RecyclerView (LinearLayoutManager)
  - `tasksRecyclerViewAdapter`: Multi-view adapter (line 232, 257)
  - `itemTouchHelperAttacher`: Drag-n-drop handler (line 169)
- Key methods:
  - `setTasksAndRV()`: Load from controller, create adapter, attach drag helper (line 215)
  - `setTasksAndNotifyDataSetChanged()`: Reload + re-flatten (line 204); calls `rebuildItems()`
  - `notifyDataChanged()`: Call adapter notify + scroll to preserve position (line 249)
  - `showAllTasks()`: Toggle between undone-only and all tasks (line 276); updates adapter, preserves scroll position

### 1.3 Adapter: Multi-View-Type RecyclerView

**TasksRecyclerViewAdapter** (`ui/fragment/task_section/small_tasks_fragment/TasksRecyclerViewAdapter.java:39-513`)

Architecture:
- **Legacy field** `tasks: List<TaskObject>` preserved for drag-helper backwards compat (line 42)
- **Authoritative field** `items: List<AdapterItem>` drives binding, viewtype lookup (line 45)
- **View types** (lines 54-60):
  - `VIEW_TYPE_TASK` (1): Task row
  - `VIEW_TYPE_SECTION_HEADER` (2): Section collapse/expand header
  - `VIEW_TYPE_RAIL_BOTTOM` (4): 1dp divider after section (sprint-002)
  - `VIEW_TYPE_SECTION_EMPTY` (5): Placeholder if section expanded but empty (sprint-002)
  - `FOOTER_VIEW` (123): "Done N tasks" footer (not in plain-list mode for Notes tab)

**Flattening logic** (`flatten()` lines 131-189):
1. Rebuild section-counters map from all tasks in folder (lines 136-146; separate from `tasks` list)
   - Counter source: `TasksRealmController.getTasks(folderId)` (all done+undone)
   - Stores `[doneCount, totalCount]` per section ID for progress display
2. If no folder or no tasks: return plain list of tasks + footer (lines 147-153)
3. Else: Merge free-zone tasks with sections by position (lines 156-189)
   - Collect free tasks (sectionId == 0) from current `tasks` list
   - Merge sections + free tasks using two-pointer algorithm respecting position field (lines 171-184)
   - For each section:
     - Emit `SECTION_HEADER` item
     - If not collapsed: emit `RAIL_TOP` (removed per Phase 7; line 195) **OR** section-empty placeholder (line 199), then tasks, then `RAIL_BOTTOM`
4. Add `DONE_FOOTER` if not in plain-list mode (line 186)

**View binding** (`onBindViewHolder` lines 251-273):
- Task row: text, checkbox (done state + cycling state), priority (!), value (1-9), accumulation (X/Y), category stripes, palette colors
- Checkbox states (lines 374-377):
  - Undone + cycling: accent-color checkbox drawable
  - Undone + not cycling: gray checkbox
  - Done + cycling: accent-color checked
  - Done + not cycling: gray checked
  - Palette tint applied if active (line 379-382)
- Priority/value/accumulation: cycled via click listeners; saved live (lines 186-205)
- Done checkbox: toggle triggers `TasksRealmController.setTaskDoneOrParticullaryDone()` + scale-out anim + adapter refresh (lines 387-400)
- Plain-list mode (Notes tab, taskGroup==3): hide checkbox, params row; text expanded to 25 lines; no footer (lines 353-367)
- Section header: name + chevron (▼/▶) + progress counter (X/Y done) (lines 276-311)
  - Chevron: click toggles collapse state (line 300)
  - Long-press: starts SectionActionModeCallback (line 304)
- Done footer: "Done N tasks" text; click calls `showAllTasks()` toggle (line 268)

**Category display** (`bindCategoryStripes()` line 450-454):
- If `task.getExtraFolderIds().size() > 0`: show 16dp × 2dp colored stripe at bottom-right of card (category_stripes View)

**Palette application** (lines 72-104, 432-448):
- `usePaletteForGroup(group)`: Set palette for task group 1/2/3; calls `notifyDataSetChanged()` (line 76)
- `setPlainList(enabled)`: Toggle plain-list mode (Notes); calls `rebuildItems()` + notify (line 79)
- Colors applied in `applyPaletteIfNeeded()` (lines 432-448): surface, input text, counter, accent (priority/cycling)

**Section counter tracking** (lines 46-52):
- `sectionCounts: Map<Long, int[]>` rebuilt on every `flatten()`
- Source: `TasksRealmController.getTasks(folderId)` (all tasks, not just `tasks` list)
- Used in `bindSectionHeader()` to display X/Y progress regardless of show-all mode (line 284-287)

### 1.4 AdapterItem: Tagged Union

**AdapterItem** (`small_tasks_fragment/AdapterItem.java:13-50`)
- Tagged union: one of task/section non-null based on kind
- `Kind` enum: TASK, SECTION_HEADER, DONE_FOOTER, RAIL_TOP, RAIL_BOTTOM, SECTION_EMPTY
- Rails + empty placeholder carry their owning section (so drag resolver knows container)
- No public constructors; factory methods: `ofTask()`, `ofSection()`, `doneFooter()`, `ofRailTop()`, `ofRailBottom()`, `ofSectionEmpty()`

### 1.5 Drag-n-Drop: ItemTouchHelperAttacher

**ItemTouchHelperAttacher** (`small_tasks_fragment/ItemTouchHelperAttacher.java:37-376`)

Purpose: Enable drag of tasks and section headers within/between sections; auto-expand on hover (~400ms).

Architecture:
- `ItemTouchHelper.SimpleCallback` with UP/DOWN flags (no swipe) (lines 67-68)
- `dragFrom`, `dragTo`: indices during active drag
- `pendingAutoExpand`: delayed runnable for auto-expand (handler-based) (lines 48-49)

**Key mechanics**:

1. **Movement flags** (`getMovementFlags()` lines 81-88):
   - Disabled if `isAllTaskShowing` (can't drag in "all tasks" mode)
   - Disabled for FooterViewHolder, RailViewHolder, SectionEmptyViewHolder (lines 83-86)
   - Else: allow UP/DOWN

2. **onMove** (lines 98-140):
   - Validate from/to position bounds (lines 107-110)
   - Reject if target is DONE_FOOTER (line 114-116)
   - Reject if source is DONE_FOOTER or done task (lines 120-124)
   - Schedule auto-expand if dragging TASK over collapsed SECTION_HEADER (lines 130)
   - Move item in adapter's `items` list (lines 137-138) to keep indices consistent
   - Return true to indicate move accepted

3. **Auto-expand logic** (`maybeScheduleAutoExpand()` lines 142-169):
   - Only when dragging a TASK over a collapsed SECTION_HEADER (not when dragging a section)
   - ~400ms delay before expanding (line 46)
   - Handler-based with cancellation on mouse-out (lines 171-175)
   - On expand: calls `SectionsRealmController.setCurrentlyCollapsed(false)` + `setTasksAndNotifyDataSetChanged()` (lines 162-164)

4. **clearView** (lines 178-193):
   - Cancel pending auto-expand (line 179)
   - If `touchOutsideUnDoneTaskArea`: ignore move (line 180-183)
   - Else: call `commitMove()` (line 188)
   - Re-flatten from Realm (line 192)

5. **commitMove** (lines 195-261):
   - Determine container at new position by walking upward in items list:
     - RAIL_BOTTOM above → free-zone (sectionId=0)
     - RAIL_TOP / SECTION_EMPTY / expanded SECTION_HEADER → that section
     - Collapsed SECTION_HEADER → free-zone
   - Two cases:
     a. **Section header moved**: call `SectionsRealmController.rearrangeOuterSpace()` with outer-space entries only (lines 206-210)
     b. **Task moved**:
        - If in section: call `SectionsRealmController.rearrangeTasksInContainer(sectionId, ordered)` (line 247)
        - Else (outer space): call `SectionsRealmController.rearrangeOuterSpace()` with full outer+task entries; forces moved task into free-zone (lines 252-256)

6. **Helper functions**:
   - `collectOuterEntries()` (lines 281-323): Extracts outer-space entries (sections + free tasks) in adapter order; moved task forced to sectionId=0
   - `collectTasksInSection()` (lines 330-347): Task IDs in a section, in adapter order (SECTION_HEADER → RAIL_BOTTOM)

### 1.6 TaskEditorBottomSheet Dialog

**TaskEditorBottomSheet** (`ui/dialog/task_bottomsheet/TaskEditorBottomSheet.java:43-484`)

Purpose: Half-height (tasks) or full-height (Notes) modal editor for a task.

**Init**: `newInstance(taskId, taskGroup)` stores ID and group in Bundle (lines 81-87)

**Lifecycle**:
- `onCreate`: Extract ID, group from args (lines 95-101)
- `onCreateView`: Inflate `bottomsheet_task_editor` (lines 105-109)
- `onViewCreated`: 
  - Load task from Realm; bail if missing (lines 116-120)
  - Load palette for group (lines 122-123)
  - Find views (lines 125-136)
  - Snapshot original text for autosave diff (line 138)
  - Initialize draft fields from task (lines 139-142)
  - Restore text from savedInstanceState if present (lines 144-147)
  - Bind checkbox, render numeric fields, setup click listeners (lines 150-219)
  - Special handling for taskGroup==3 (Notes):
    - Hide checkbox, priority, value, maxAcc, cycling (lines 160-165)
    - Expand text field to 15..200 lines (lines 168-169)
    - Zero padding on content (lines 172-175)
    - Live text autosave on every keystroke (lines 177-183)
  - Setup category adapter (lines 216-219)

**State & mutation**:
- `draftPriority`, `draftValue`, `draftMaxAcc`, `draftCycling`: Live mutable copies of numeric/flag fields
- `originalText`: Snapshot for autosave diff on dismiss
- Numeric fields (priority, value, maxAcc): Cycle via click listeners; saved live to Realm (lines 186-205)
- Done checkbox: Toggle calls `TasksRealmController.setTaskDoneOrParticullaryDone()` (line 208)
- Text field: On focus, expand sheet to full height (line 212)

**Palette & rendering** (lines 322-357):
- Colors applied per group (lines 324, 329, 336, 340, 357)
- Progress counter color (value/maxAcc): accent if ≥2, else counter color (lines 324, 329)
- Priority color: accent if >0, else soft text (line 336)
- Cycling color: accent if enabled, else soft text (line 340)

**Categories** (lines 361-410):
- Rebuild row list: active folders first (in order), then inactive (lines 361-381)
- CategoriesAdapter: checkbox + name with group tag (lines 439-473)
- On tap: add/remove from active list (lines 393-410)
- Constraint: cannot remove last active category (line 397-399)
- No re-sort on tap; refresh flags in place (lines 409, 413-425)

**Dismiss behavior** (lines 280-295):
- If text changed and not empty: call `TasksRealmController.editTask()` with draft fields (lines 285-287)
- Call `onDismissListener` callback (line 294)

**Sheet sizing** (lines 231-269):
- Task group 1/2: half-expanded (0.55 ratio) (lines 261-262)
- Task group 3 (Notes): full-height, full-width "curtain" (lines 246-259)
- Transparent backdrop (lines 240-242)

### 1.7 SectionEditDialog

**SectionEditDialog** (`ui/dialog/section_dialog/SectionEditDialog.java:35-155`)

Purpose: Create or edit a section within a folder.

**Modes**:
- **Create**: blank fields; button = "Add"; no Delete button
- **Edit**: populated from existing; button = "Save"; neutral = "Delete"

**Init**:
- `forCreate(folderId)`: Bundle with mode=create + folderId (lines 46-52)
- `forEdit(section)`: Bundle with mode=edit + folderId + sectionId (lines 55-62)

**Dialog**:
- View: `dialog_section_edit` (TextInputLayout + name EditText + SwitchCompat for collapse-by-default) (line 75)
- Positive button handler (lines 105-125):
  - Validate name: 1..40 chars (line 107)
  - Create or edit section (lines 113-117)
  - Call `notifyHost()` to trigger RESULT_KEY broadcast (line 123)
- Delete button (edit mode only): Shows confirmation, calls `SectionsRealmController.deleteSection()` (lines 128-140)

**Notification** (lines 147-154):
- Broadcast via `FragmentResultListener` with key = `RESULT_KEY` = "section_changed" (line 44, 152)
- Host (SmallTasksFragment) listens and calls `setTasksAndNotifyDataSetChanged()` (line 129)

### 1.8 Action Mode: Task & Section

**TaskActionModeCallback** (`ui/actionmode/task/TaskActionModeCallback.java:39-254`)

Purpose: Floating action bar for task editing (legacy before BottomSheet era; still in use for long-press).

**Modes**:
- Edit: Text + numeric field adjustments (lines 97-148)
- Categories: Multi-choice dialog (lines 151-156)
- Delete: Confirm delete (lines 159-170)

**Flow**:
- Long-press task → calls `onItemLongClicked` callback (SmallTasksFragment line 175-180)
- Creates TaskActionModeCallback, calls `getCallback()` (line 178)
- Inflates `dialog_edit_task` (action-mode variant) (line 66)

**SectionActionModeCallback** (`ui/actionmode/task/SectionActionModeCallback.java`)

Purpose: Floating action bar for section editing (long-press section header).

**Menu**:
- Edit: Opens SectionEditDialog (fragment-based result)
- Delete: Confirm + delete via `SectionsRealmController.deleteSection()`

### 1.9 Daily Reset Logic

**When**: Called in `FolderSlidingPanelFragment.onResume()` (line 50-59 in fragment)

**Logic** (`resetTasksCountAccumulation()` in fragment):
```
1. Fetch all done + partially-done tasks: TasksRealmController.getDoneAndPartiallyDoneTasks()
2. Compute today's date: YYYYDDD format (Calendar.DAY_OF_YEAR + YEAR)
3. For each task:
   - If isCycling && lastDoneDate != today:
     - Call TasksRealmController.setTaskDoneOrParticullaryDone(task, false)
     - Sets done=false, clears dateCountAccumulation, resets lastDoneDate=0
4. Return true if any task reset
```

**Timing**:
- Called in `onCreate()` (line 27)
- Called in `onResume()` only if lastDateResetTasksCountAccumulation != today (line 52-58)
- `lastDateResetTasksCountAccumulation` (int): Caches last date checked to avoid redundant resets
- If reset occurs: all SmallTasksFragment tabs are refreshed via `setTasksAndNotifyDataSetChanged()` (lines 55-58)

**DayScope counter** (in App class):
- Computed in `refreshDayScope()` (lines 50+ of snippet from App)
- Sums countValue × count of dates==today for each cycling task
- Used in action bar title decoration

### 1.10 Realm Controllers

**TasksRealmController** (`realmcontrollers/taskcontroller/TasksRealmController.java:22-271`)

Key methods:
- `getTasks(folderId)`: All tasks (done+undone), sorted by (done ASC, position ASC) (line 58-62)
- `getNotDoneTasks(folderId)`: Undone only, sorted by position (lines 66-69)
- `getDoneTasks(folderId)`: Done only, sorted by position (lines 73-76)
- `getDoneAndPartiallyDoneTasks()`: All done+undone with countAccumulation>0 (for daily reset) (lines 50-55)
- `addTask()`: Create with position=nextOuterPosition, sectionId=0 (lines 95-112)
- `editTask()`: Update text, numeric fields (lines 115-122)
- `setTaskDoneOrParticullaryDone()`: Toggle done state; if done: add date to accumulation list, check if max reached (lines 125-145)
- `deleteTask()`: Remove from all folders (primary + extras) (lines 148-171)
- `setTaskCategories(task, folderIds)`: Set primary + extras; add/remove from folders as needed (lines 223-262)
- `setTaskPriority()`: Update priority field (lines 264-268)

**SectionsRealmController** (`realmcontrollers/taskcontroller/SectionsRealmController.java`)

Key methods:
- `getSections(folderId)`: All sections for folder
- `getSection(id)`: Single section by ID
- `addSection()`: Create with position=nextOuterPosition
- `editSection()`: Update name, collapse-by-default
- `deleteSection()`: Delete section; tasks in it are moved to free-zone (sectionId=0)
- `setCurrentlyCollapsed()`: Toggle collapse state
- `nextOuterPosition()`: Compute next available position in outer space (for new sections/free tasks)
- `rearrangeOuterSpace()`: Reorder sections + free tasks based on ItemMove list (used after drag)
- `rearrangeTasksInContainer()`: Reorder tasks within a section (used after drag within section)

### 1.11 Palette & Theming

**Palette** (`ui/theme/Palette.java`)

Per-tab color scheme (9 tokens):
- `forGroup(context, group)`: Return palette for task group 1/2/3, or null if group is 0/invalid
- `dialogDefault()`: Fallback palette for dialogs outside a tab
- Tokens: bg, surface, text, textSoft, inputText, accent, counter, etc.

Applied in:
- Adapter binding (TasksRecyclerViewAdapter.applyPaletteIfNeeded)
- TaskEditorBottomSheet rendering
- Fragment background (SmallTasksFragment.applyPaletteToFragmentView)

---

## 2. State & Data Flow

### 2.1 Ownership & Mutation

**Realm as source of truth**:
- All TaskObject and SectionObject mutations go through Realm write blocks (RealmDb.write)
- SmallTasksFragment holds **detached** copies (controller fetches fresh from Realm)
- Never held across async boundaries; re-fetched on every interaction

**Fragment state**:
- `SmallTasksFragment.tasks`, `doneTasks`: Snapshots from controller (line 205-206)
- `SmallTasksFragment.isAllTaskShowing`: bool; toggles "all tasks" vs undone-only (line 277-299)
- `TasksRecyclerViewAdapter.items`: Flattened display list; rebuilt on every data mutation (line 128)

**Adapter state**:
- `items`: Multi-view-type list (TASK, SECTION_HEADER, RAIL_TOP/BOTTOM, DONE_FOOTER)
- `sectionCounts`: Map<sectionId, [done, total]>; rebuilt from Realm on every flatten (line 136)
- `plainList`: bool; disables checkbox + params for Notes tab (line 70)
- `palette`: Optional per-group color scheme (line 67)
- `touchOutsideUnDoneTaskArea`: Flag to reject moves outside undone area (line 62)

**BottomSheet state**:
- `originalText`: Text snapshot on open; used for autosave diff on dismiss
- `draftPriority`, `draftValue`, `draftMaxAcc`, `draftCycling`: Live copies; updated on every numeric click; saved to Realm on dismiss

### 2.2 Event Flow

1. **Task open** (single-tap):
   - SmallTasksFragment.onItemClicked → TaskEditorBottomSheet.newInstance(taskId, taskGroup)
   - Sheet binds task data, shows editor
   - On dismiss: `onDismissListener` calls `SmallTasksFragment.onTaskEditorDismissed()`
   - Fragment calls `setTasksAndNotifyDataSetChanged()` (line 196)

2. **Task edit via BottomSheet**:
   - User taps numeric field (priority, value, maxAcc) → increments draft → saves live to Realm
   - User toggles cycling → toggles draft → saves live
   - User toggles done → calls TasksRealmController → updates dateCountAccumulation logic
   - On dismiss: autosave text if changed (lines 282-289)

3. **Section collapse/expand**:
   - SmallTasksFragment.onItemClicked (section header) → toggle currentlyCollapsed (line 300)
   - Fragment calls setTasksAndNotifyDataSetChanged() → reflatten (line 301)
   - Adapter rebuilds items, recalc section counters, notify (line 128-129)

4. **Drag-n-drop**:
   - ItemTouchHelperAttacher.onMove() → update items list during drag
   - ItemTouchHelperAttacher.clearView() → commitMove() → SectionsRealmController.rearrangeOuterSpace/rearrangeTasksInContainer()
   - Fragment re-flattens via setTasksAndNotifyDataSetChanged() (line 192)

5. **Daily reset**:
   - App resume → FolderSlidingPanelFragment.onResume()
   - Checks if today's date differs from last reset date
   - If reset needed: TasksRealmController.setTaskDoneOrParticullaryDone(task, false) for each cycling task with old lastDoneDate
   - All SmallTasksFragment tabs call setTasksAndNotifyDataSetChanged() (lines 55-58)

### 2.3 Threading

**Realm**:
- All reads/writes on main thread (allowQueriesOnUiThread enabled per plan)
- RealmResults objects are live but realm-attached; adapter works with detached copies (`copyFromRealm()` implicit in controller methods)

**UI**:
- All updates dispatched on main thread
- Fragment lifecycle drives state initialization + teardown
- Callbacks (onDismissListener, FragmentResultListener) run on main thread

---

## 3. Edge Cases & Gotchas

### 3.1 Empty Sections

**Case**: Section exists but has no tasks in it.

**Behavior**:
- `emitSection()` emits SECTION_HEADER + SECTION_EMPTY placeholder + RAIL_BOTTOM (lines 192-204)
- SECTION_EMPTY item is not draggable (line 86; getMovementFlags returns 0)
- Placeholder is static (no bind logic) and invisible; serves as drop target for drag-into-empty-section

**Compose mapping**: Render empty-section spacer (maybe grey text "No tasks") instead of invisible View

### 3.2 Partially Done (Accumulation) Tasks

**Case**: Task with countAccumulation < maxAccumulation; done=false.

**Behavior**:
- Counted in `getDoneAndPartiallyDoneTasks()` (line 50-54)
- Included in daily reset check (line 81 of controller)
- Checkbox state: depends on done flag, not countAccumulation
- If cycling && partial && new day: reset via `setTaskDoneOrParticullaryDone(false)` clears dateCountAccumulation (line 129)

**Risk**: User might not realize partial task state exists; no UI indicator of "partial" vs "done"

### 3.3 Multi-Category Membership (extraFolderIds)

**Case**: Task in folder A + B + C (A primary, B/C extras).

**Behavior**:
- `taskFolderId` = A (primary; determines which folder task appears under in folder list)
- `extraFolderIds` = [B, C] (displayed with category stripes in list view) (line 452-453)
- Drag reorder only affects task in current folder context (does not move task between folders)
- Delete removes task from **all** folders (lines 154-157)
- Categories dialog (BottomSheet or action mode) shows all folders; tap to toggle membership
- On membership change: task added/removed from folder's task list (lines 235-249)

**Risk**: Task visible in multiple places but drag only reorders in current context; confusion possible

### 3.4 Drag Across Section Boundaries

**Case**: User drags task from Section A into Section B (or from Section B into free zone).

**Behavior**:
- During drag: adapter's items list is mutated in-place; section membership not yet changed (line 137-138)
- On drop: `commitMove()` walks upward from drop position to determine new container (lines 218-241)
  - If drop over RAIL_BOTTOM of section A: task exits A (containerSectionId = 0)
  - If drop over RAIL_TOP/SECTION_EMPTY under section B: task enters B (containerSectionId = sectionId)
- Realm updated: task.sectionId rewritten + position fields updated
- Fragment re-flattens and notifies

**Risk**: During drag, item list is out-of-sync with Realm; if drop fails or is interrupted, item list is restored via re-flatten

### 3.5 Auto-Expand on Hover

**Case**: User drags task onto collapsed section header; hovers for ~400ms.

**Behavior**:
- `maybeScheduleAutoExpand()` schedules 400ms delayed expand (line 160-168)
- If mouse moves away: `cancelAutoExpand()` cancels the runnable (line 171-175)
- If expanded: section.currentlyCollapsed set to false; adapter re-flattens; tasks in section now visible as drop targets
- User can drop task into newly-exposed section

**Risk**: Timeout is fixed 400ms; may feel slow or fast depending on user's drag speed. No user-facing feedback of pending expansion.

### 3.6 Done Footer Visibility

**Case**: "Done N tasks" footer shown only when undone tasks exist OR (done tasks exist AND not in showAllTasks mode).

**Behavior**:
- Footer not rendered in plain-list mode (Notes tab) (line 186)
- Footer is last item in items list (line 187)
- Footer is static layout (task_card_view_done_tasks.xml); count text set at bind time (line 266)
- Tap footer calls `smallTasksFragment.showAllTasks()` toggle (line 268)
- In "all tasks" mode: footer still shown; both done + undone visible

**Risk**: Footer count is stale if task state changes during "all tasks" view without refreshing

### 3.7 Section Collapse Persistence

**Case**: User collapses section S, kills app, relaunches.

**Behavior**:
- Section state is read from Realm `currentlyCollapsed` field on every flatten (line 194)
- If `collapsedByDefault` is set: defaults to that state on first open (not persisted per se; hardcoded as default)
- If user toggles: `currentlyCollapsed` is written to Realm (line 300)
- No config-change handling; fragment lifecycle handles relaunch

**Risk**: If fragment is destroyed but not the activity (e.g., rotation), collapse state is re-read from Realm correctly. But if Realm write fails silently, state is lost.

### 3.8 BottomSheet Fragment Dismiss & Result

**Case**: User presses back while BottomSheet is open.

**Behavior**:
- BottomSheetBehavior.onStateChanged → collapse → dismiss
- `onDismiss()` called: autosave text if changed (line 282-289)
- `onDismissListener` runnable called: fragment calls `setTasksAndNotifyDataSetChanged()` (line 196)
- `FragmentResultListener` on section-edit results also triggers re-flatten (line 129)

**Risk**: Back-press during edit may cause multiple re-flattens; text not saved if edit loses focus before dismiss

### 3.9 Race: Section Delete During Drag

**Case**: User dragging task into section S; concurrently S is deleted (unlikely in single-app context, possible in multi-window).

**Behavior**:
- During drag: `items` list references section S via AdapterItem.section
- On drop: `commitMove()` calls `SectionsRealmController.rearrangeTasksInContainer(sectionId, ...)` with stale sectionId
- If section not found: controller method likely no-ops or crashes (not checked in code)
- Re-flatten will omit deleted section

**Risk**: Not protected against concurrent mutation; crash possible

### 3.10 Drag Anchor Size (400ms)

**Case**: Auto-expand timeout hard-coded to 400ms.

**Behavior**:
- No visual feedback of pending expansion; user must wait 400ms to see section expand
- Timeout is not reset if user moves over same section again; only starts on first hover

**Risk**: UX feels sluggish; no way to accelerate expansion (e.g., double-hover)

### 3.11 Palette Null Safety

**Case**: `palette` is null if taskGroup is 0 or not 1/2/3.

**Behavior**:
- TasksRecyclerViewAdapter methods check `hasActivePalette()` (line 85)
- If palette null: default colors used (line 91-92, 95, etc.)
- BottomSheet loads palette via `Palette.forGroup()`; falls back to dialogDefault (line 123)

**Risk**: No crash, but colors may not match tab theme if palette lookup fails

### 3.12 Done Task Text Color

**Case**: Text color for done tasks is hardcoded to gray (Color.GRAY).

**Behavior**:
- `setTasksTextColor(holder, isDone)` sets gray if done, black if undone (line 245-247)
- No palette-aware strikethrough or other visual indicator of done

**Risk**: In dark-mode palettes, gray text on dark background may be invisible

### 3.13 Long-Press vs Single-Tap Dispatch

**Case**: User long-presses task.

**Behavior**:
- ItemTouchHelper long-press drag is enabled (line 91)
- Separate long-click listener on text view calls `onItemLongClicked` → TaskActionModeCallback (line 402-404)
- If user holds down, both drag + action-mode may start; unclear which wins

**Risk**: Unclear interaction precedence; may cause visual glitch

### 3.14 ViewPager Offscreen Fragment Lifecycle

**Case**: SmallTasksFragment is created but not visible (ViewPager pre-creates neighbors).

**Behavior**:
- `onCreate()` runs (reset daily done/partially-done states) (line 27 of FolderSlidingPanelFragment)
- `onCreateView()` and `onViewCreated()` run (set up adapter)
- Fragment may not be added to activity immediately; `isAdded()` can be false

**Risk**: Fragment callbacks run even if user never sees the tab; may cause spurious Realm writes or state leaks

### 3.15 SavedInstanceState Handling

**Case**: BottomSheet is open and activity is destroyed (rotation, memory pressure).

**Behavior**:
- BottomSheet saves draft text to savedInstanceState (line 223-227)
- On restore: draft text is restored from bundle (line 144-147)
- Numeric fields (priority, value, maxAcc) are not saved; default to current Realm values
- Fragment does not save tasks/doneTasks (re-fetched on resume)

**Risk**: If rotation happens during edit, text is preserved but numeric edits may be lost

---

## 4. Compose Mapping

### 4.1 Top-Level Composables

**`TasksScreen(folderId, taskGroup, modifier)`**
- Replaces SmallTasksFragment
- State: `tasksUiState by viewModel.tasksFlow.collectAsStateWithLifecycle()`
- Content:
  ```
  LazyColumn(state = lazyListState) {
    items(uiState.items, key = { it.key }) { item ->
      when (item.kind) {
        TASK → TaskItem(...)
        SECTION_HEADER → SectionHeaderItem(...)
        RAIL_BOTTOM → RailItem()
        SECTION_EMPTY → SectionEmptyItem()
        DONE_FOOTER → DoneFooterItem(...)
      }
    }
  }
  ```
- Lazy column size = items.size (no fake empty state row needed)
- Sticky header for done footer? (Material 3 has no native sticky footer; use Box + Spacer)

**`TaskItem(task, onCheck, onLongPress, onTap, modifier)`**
- Replaces NormalViewHolder
- Content:
  - Row: checkbox (done state, cycling indicator), text (clamped to 7 lines), params, category stripe
  - Palette applied via LocalPalette CompositionLocal
  - Checkbox tint: palette.accent if cycling + active palette, else null
  - Tap: open TaskEditorBottomSheet (see below)
  - Long-press: action mode (see below) or ModalBottomSheet for task edit?

**`SectionHeaderItem(section, onToggleCollapse, onLongPress, progressText, modifier)`**
- Replaces SectionHeaderViewHolder
- Content:
  - Row: chevron (▼/▶), name, progress counter (X/Y)
  - Chevron color: palette.accent
  - Tap: toggle collapse + reflatten
  - Long-press: SectionActionModeBottomSheet (or action bar?)

**`DoneFooterItem(count, onShowAll, modifier)`**
- Replaces FooterViewHolder
- Content: "Done N tasks" clickable text (centered)
- Tap: toggle showAllTasks

**`RailItem()` & `SectionEmptyItem()`**
- Static 1dp white line (RailBottom)
- Empty placeholder space (SectionEmpty)

### 4.2 ViewModel & State

**`TasksViewModel(folderId, palette)`** (Kotlin, Dagger-injected)
```kotlin
val tasksFlow: Flow<TasksUiState> = 
  combine(
    tasksRepository.getNotDoneTasks(folderId),
    tasksRepository.getDoneTasks(folderId),
    sectionsRepository.getSections(folderId),
    showAllTasksFlow
  ) { notDone, done, sections, showAll ->
    flattenAndBuildItems(notDone, done, sections, showAll)
  }

fun toggleShowAll()
fun collapseSection(sectionId)
fun editTask(taskId, text, value, maxAcc, cycling, priority)
fun deleteTask(taskId)
fun setTaskDone(taskId, done)
fun setTaskCategories(taskId, folderIds)

// Drag-n-drop
fun onTaskMoved(fromIndex, toIndex)
```

**`TasksUiState`** (data class)
```kotlin
val items: List<DisplayItem>
val sectionCounts: Map<Long, Pair<Int, Int>>
val showAllTasks: Boolean

sealed class DisplayItem(val key: String) {
  data class Task(...) : DisplayItem(...)
  data class SectionHeader(...) : DisplayItem(...)
  data class Rail(...) : DisplayItem(...)
  data class SectionEmpty(...) : DisplayItem(...)
  data class DoneFooter(...) : DisplayItem(...)
}
```

**`TasksRepository`** (Kotlin, wraps Java controller)
```kotlin
fun getNotDoneTasks(folderId): Flow<List<TaskObject>>
fun getDoneTasks(folderId): Flow<List<TaskObject>>
// Etc.; mirrors TasksRealmController static methods
```

### 4.3 ModalBottomSheet Integration

**`TaskEditorSheet(taskId, taskGroup, onDismiss)`**
- Replaces TaskEditorBottomSheet Fragment
- Composable version using Material 3 ModalBottomSheet
- Half-height for taskGroup 1/2 (peekHeight not directly supported; use heightFraction = 0.55)
- Full-height for taskGroup 3 (Notes) (heightFraction = 1.0)
- Content:
  ```
  ModalBottomSheet(
    onDismissRequest = { /* autosave + onDismiss() */ },
    sheetState = rememberModalBottomSheetState(expanded = true)
  ) {
    Column(...) {
      // Drag handle
      TaskEditorContent(...)
    }
  }
  ```
- State: `taskUiState by taskViewModel.taskFlow.collectAsStateWithLifecycle()`
- Numeric click listeners save live to Realm (via ViewModel)
- Text field autosave (Notes only)
- Category list: RecyclerView-like LazyColumn + checkbox

### 4.4 Drag-Reorder: sh.calvin.reorderable Integration

**Library**: `sh.calvin.reorderable` (Compose drag-reorder)

**Usage**:
```kotlin
ReorderableColumn(
  state = rememberReorderableState(...),
  onMove = { from, to -> viewModel.onTaskMoved(from, to) }
) {
  items(state.items, key = { it.id }) { item ->
    ReorderableItem(state, item.id) { isDragging ->
      TaskItem(..., modifier = if (isDragging) draggingMod else normalMod)
    }
  }
}
```

**Limitations**:
- No auto-expand on hover; Compose drag not as granular as ItemTouchHelper
- May need custom implementation for section auto-expand

**Alternative**: Hand-roll drag using Compose Modifier.pointerInput + AnchoredDraggable (if auto-expand needed)

### 4.5 Section Collapse State Management

**Current**:
- `SectionObject.currentlyCollapsed` stored in Realm
- Read on every flatten; written on toggle

**Compose**:
- ViewModel holds `collapsedSectionsFlow: Flow<Set<Long>>` (IDs of collapsed sections)
- On toggle: ViewModel calls `sectionsRepository.setCollapsed(sectionId, collapsed)` → Realm write
- UI observes flow and rebuilds items list (section header visibility change triggers reflatten)

### 4.6 LocalPalette CompositionLocal

**Usage**:
```kotlin
val LocalPalette = compositionLocalOf<Palette?> { null }

CompositionLocalProvider(LocalPalette provides palette) {
  TasksScreen(...)
}

// In child composables:
val palette = LocalPalette.current
TaskItem(..., checkboxTint = palette?.accent ?: Color.Gray)
```

### 4.7 Action-Mode to ModalBottomSheet

**Current**: Long-press task → TaskActionModeCallback (legacy Android ActionMode)

**Compose**: Option 1: Replace with ModalBottomSheet for task edit/delete/categories
- Long-press → show ModalBottomSheet with three buttons: Edit, Categories, Delete
- Edit taps call ViewModel methods (which delegate to Java controller)

**Option 2**: Keep simplified ActionBar composable overlaid on screen
- Less intrusive; matches Compose paradigm better
- No system action bar integration

**Recommendation**: ModalBottomSheet for consistency with TaskEditorSheet

### 4.8 LazyColumn Scroll Position Preservation

**Current**: `notifyDataChanged()` preserves scroll position via `llm.findFirstVisibleItemPosition()` + `scrollToPosition()` (line 251, 266)

**Compose**:
```kotlin
val lazyListState = rememberLazyListState()
LazyColumn(state = lazyListState) { ... }
// ViewModel preserves state via SavedStateHandle or MutableState
// On data refresh: LazyColumn recomposes; state restored automatically
```

### 4.9 Empty State

**Current**: Empty LinearLayout shown when adapter.itemCount == 0 (line 351-355)

**Compose**: Simple conditional in TasksScreen
```kotlin
if (items.isEmpty()) {
  EmptyStateMessage(...)
} else {
  LazyColumn { ... }
}
```

### 4.10 Notes Tab (Plain List)

**Current**: `plainList` flag disables checkbox, params, footer; expands text (line 78-82)

**Compose**: Pass `isPlainList` to ViewModel; items list omits footer if plainList is true (line 186)

```kotlin
if (isPlainList) {
  LazyColumn {
    items(notDoneAndDone) { task -> TaskItem(task, hideCheckbox = true, ...) }
  }
} else {
  LazyColumn {
    items(items) { item -> ... }
  }
}
```

### 4.11 Palette Reactive Updates

**Current**: `usePaletteForGroup(group)` calls `notifyDataSetChanged()` (line 76)

**Compose**: CompositionLocal + Recomposition handles this automatically
- LocalPalette changes → all TaskItem composables recompose → colors reapplied

### 4.12 Category Membership UI

**BottomSheet layout**:
```
TaskEditorContent {
  Column {
    // Task params (hide in Notes)
    // Text field
    // Categories label
    LazyColumn { // categories
      items(rows) { row ->
        Row(
          modifier = Modifier.clickable { viewModel.toggleCategory(...) }
        ) {
          Checkbox(checked = row.active, ...)
          Text(row.folder.name + tagForGroup(...))
        }
      }
    }
  }
}
```

### 4.13 Daily Reset

**Timing**: Not changed; still runs in ViewModel.init or Activity.onCreate before UI renders

**Signal to UI**: ViewModel emits new TasksUiState after reset; LazyColumn recomposes

---

## 5. Files to Delete Once Migrated

### Fragment/Adapter
- `ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java`
- `ui/fragment/task_section/small_tasks_fragment/TasksRecyclerViewAdapter.java`
- `ui/fragment/task_section/small_tasks_fragment/AdapterItem.java`
- `ui/fragment/task_section/small_tasks_fragment/ItemTouchHelperAttacher.java`
- `ui/fragment/task_section/small_tasks_fragment/SlideInDownItemAnimator.java`

### Dialogs (Fragments)
- `ui/dialog/task_bottomsheet/TaskEditorBottomSheet.java`
- `ui/dialog/section_dialog/SectionEditDialog.java`

### Action Modes
- `ui/actionmode/task/TaskActionModeCallback.java`
- `ui/actionmode/task/SectionActionModeCallback.java`

### Layouts
- `res/layout/task_card_view.xml`
- `res/layout/task_card_view_done_tasks.xml`
- `res/layout/section_header_card_view.xml`
- `res/layout/section_rail_top_view.xml`
- `res/layout/section_rail_bottom_view.xml`
- `res/layout/section_empty_card_view.xml`
- `res/layout/dialog_edit_task.xml`
- `res/layout/dialog_section_edit.xml`
- `res/layout/bottomsheet_task_editor.xml`
- `res/layout/item_bs_category_row.xml`

### Models (KEEP)
- `realmmodel/task/TaskObject.java` (schema must stay)
- `realmmodel/task/SectionObject.java` (schema must stay)

### Controllers (KEEP for now, wrap in Kotlin Repository)
- `realmcontrollers/taskcontroller/TasksRealmController.java`
- `realmcontrollers/taskcontroller/SectionsRealmController.java`

---

## 6. Outstanding Risks & Open Questions

### 6.1 Auto-Expand Timing

**Risk**: 400ms timeout may not be achievable in Compose drag (sh.calvin.reorderable may not support hover detection)

**Mitigation**: May need to hand-roll drag using AnchoredDraggable or accept loss of auto-expand feature

### 6.2 Section Drag Behavior

**Question**: Can we drag a section header to reorder it (not just tasks)?

**Current**: ItemTouchHelperAttacher treats section headers as draggable (line 81-87); drag logic reorders sections via `rearrangeOuterSpace()` (line 206-210)

**Compose**: sh.calvin.reorderable supports any item; section header is just another ReorderableItem

### 6.3 Touch Outside Undone Area

**Current**: `touchOutsideUnDoneTaskArea` flag rejects drops on done footer or done tasks (line 109-116, 122-123)

**Compose**: May need custom drop-target validation in `onMove()` callback

### 6.4 Fragment Result Listeners

**Current**: SectionEditDialog broadcasts via `FragmentResultListener` with key `section_changed` (line 127-129)

**Compose**: Dialogs are not fragments; use ViewModel/State callbacks instead:
```kotlin
viewModel.sectionChanged.collect { /* reflatten */ }
```

### 6.5 Palette Timing

**Question**: When is palette loaded? Must it wait for tab to be active?

**Current**: Palette.forGroup() is deterministic (function of taskGroup); no async dependency

**Compose**: Safe to set palette during ViewModel init or CompositionLocal setup

---

## Summary of Key Changes

| Aspect | Current | Compose |
|--------|---------|---------|
| **Fragment** | SmallTasksFragment | TasksScreen composable + ViewModel |
| **RecyclerView** | TasksRecyclerViewAdapter + multi-view-type | LazyColumn + sealed DisplayItem classes |
| **Adapter Item** | AdapterItem (task/section/footer union) | DisplayItem sealed class (1:1 mapping) |
| **Drag-reorder** | ItemTouchHelper + custom logic | sh.calvin.reorderable library + ViewModel |
| **Auto-expand** | Handler + 400ms timeout | TBD (may hand-roll or remove) |
| **BottomSheet** | BottomSheetDialogFragment | Composable ModalBottomSheet |
| **Section dialog** | DialogFragment + AlertDialog | Composable AlertDialog or ModalBottomSheet |
| **Action mode** | System ActionMode | ModalBottomSheet with buttons |
| **State ownership** | Fragment + Adapter local state | ViewModel (observable) |
| **Data mutations** | Direct Realm write blocks | Repository → Realm (via ViewModel) |
| **Theming** | Manual palette.apply() in adapter | LocalPalette CompositionLocal |
| **Empty state** | Separate LinearLayout view | Conditional composable |

---

## Detailed Gotchas for Implementation

1. **Never render live Realm objects**: Always detach snapshots (controller already does this)
2. **Flatten is expensive**: Runs on every data change (O(tasks + sections)); Compose recomposition may exacerbate perf if not memoized
3. **DayScope counter is global**: Update App.dayScope after daily reset; used in action bar title
4. **Category stripes color**: Uses palette.accent (active if visible)
5. **Done task text is gray, not strikethrough**: May be hard to see in dark palettes
6. **Progress counter is stale in showAllTasks mode**: Recalculated on reflatten, not live during edit
7. **Section auto-expand is a UX delight, not a requirement**: If sh.calvin.reorderable doesn't support hover, OK to drop
8. **Back-press during edit may not autosave**: Ensure text field loses focus before dismiss
9. **Multi-category drag only reorders in current folder**: Confusing; document in UX
10. **Config-change handling**: Fragment state is restored from Realm; SavedInstanceState only preserves draft text in BottomSheet

---

**Last updated**: 2026-06-13
**Status**: Ready for Compose implementation (Phase 2)
