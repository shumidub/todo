# Notes Subsystem — Jetpack Compose Migration Spec

**Date:** 2026-06-13  
**Phase:** 3 (Notes)  
**Scope:** page 0 two-level navigation (folders → notes), CRUD dialogs, drag-reorder, action-mode, special "add list" card.

---

## 1. Current Behavior

### 1.1 Two-Level Navigation

**Folders view (initial state)**  
- `FolderNoteFragment.setFolderNoteViews()` (line 136–167)
- Displays all folders from `App.folderOfNotesContainerList` via `FolderNotesRecyclerViewAdapter`
- Action bar: no back button; title = "Notes"
- State: `folderViewShowing = true`, `actionModeIsEnabled = false`, `isNoteFragment = false`
- Click: folder card → `setNoteViews(idFolderFromAdapter)` (line 150–152)
- Long-click: folder card → `ActionMode` with edit/delete pair (line 153–158)

**Notes view (after folder click)**  
- `FolderNoteFragment.setNoteViews(idFolderFromAdapter)` (line 170–200)
- Displays notes in `App.realm.where(FolderNotesObject).equalTo("id", idFolder).findFirst().getTasks()`
- State: `isNoteFragment = true`, `actionModeIsEnabled = false`, `type = TYPE_NOTE`, `id = idFolderFromAdapter`
- Action bar: back button enabled (line 174); title = folder name (line 177)
- Click: first "add list" card → returns to folders view (line 187) _(hardcoded, special behavior)_
- Long-click: note card → `ActionMode` with edit/delete pair (line 188–192)
- Back button / menu home: `onOptionsItemSelected(android.R.id.home)` → `setFolderNoteViews()` (line 204–209)

### 1.2 Adapter Architecture

**`FolderNotesRecyclerViewAdapter` (line 22–96)**  
- Data: `App.folderOfNotesContainerList` (global static `RealmList<FolderNotesObject>`)
- Layout: `notes_item_card_view.xml` (CardView + TextView)
- Binding: `folderNotesList.get(position).getName()` → `tv_note_text`
- Listeners: `OnClickListener`, `OnLongClickListener` (custom interfaces, set by fragment)
- Drag-reorder: `itemMove(from, to)` writes to Realm (line 86–95); called by `ItemTouchHelper`

**`NotesRecyclerViewAdapter` (line 22–107)**  
- Data: `App.realm.where(FolderNotesObject).equalTo("id", folderNotesId).findFirst().getTasks()`
- Redundant: `noteList` field shadowed by constructor initialization (line 44–45 vs line 104–106)
- Bug: `itemMove()` uses `noteList` (line 93–97), not the constructor-bound list; only initialized if `setId()` called (line 102–106)
- Layout: same `notes_item_card_view.xml` (CardView + TextView)
- Binding: `notesList.get(position).getText()` → `tv_note_text`
- Listeners: `OnClickListener` disabled (commented, line 61); `OnLongClickListener` only
- Drag-reorder: `itemMove(from, to)` writes to Realm; same pattern as folders

### 1.3 CRUD Operations

**Add folder** (file:line refs)  
- Trigger: fragment menu item "add" (line 97–113)
- Dialog: `AddNoteDialog.newInstance(TYPE_FOLDER, id=0)` (line 101)
- Implementation: `AddNoteDialog.positiveButtonInterface.onClick()` → `FolderNotesRealmController.addFolderNote(text)` (AddNoteDialog:56–57)
- Realm write: creates new `FolderNotesObject`, appends to `App.folderOfNotesContainerList` (FolderNotesRealmController:19–27)
- Validation: EditText validation in dialog `onStart()` (AddNoteDialog:144–155)
- Callback: `notifyDataChanged()` → finds `FolderNoteFragment`, calls `notifyDataChanged()` → adapter notify + empty state (AddNoteDialog:128–137, FolderNoteFragment:116–131)

**Add note**  
- Trigger: fragment menu item "add" (line 97–113)
- Dialog: `AddNoteDialog.newInstance(TYPE_NOTE, id=folderId)` (line 101)
- Implementation: `AddNoteDialog.positiveButtonInterface.onClick()` → `FolderNotesRealmController.addNote(id, text)` (AddNoteDialog:58–59)
- Realm write: creates new `NoteObject`, appends to folder's `getTasks()` (FolderNotesRealmController:62–72)

**Edit folder**  
- Trigger: long-press folder → `ActionMode` → "edit" (line 36–40 in FolderNoteActionModeCallback)
- Dialog: `EditNoteDialog.newInstance(TYPE_FOLDER, id)` (EditNoteDialog:17–24)
- Implementation: `EditNoteDialog.positiveButtonInterface.onClick()` → `FolderNotesRealmController.editFolderNote(id, text)` (EditNoteDialog:36–37)
- Realm write: mutates existing `FolderNotesObject.name` (FolderNotesRealmController:30–31)
- Callback: same `notifyDataChanged()` flow

**Edit note**  
- Trigger: long-press note → `ActionMode` → "edit"
- Dialog: `EditNoteDialog.newInstance(TYPE_NOTE, id)`
- Implementation: `EditNoteDialog.positiveButtonInterface.onClick()` → `FolderNotesRealmController.editNote(id, text)` (EditNoteDialog:38–39)
- Realm write: mutates existing `NoteObject.text` (FolderNotesRealmController:75–76)

**Delete folder**  
- Trigger: long-press folder → `ActionMode` → "delete"
- Dialog: `DellNoteDialog.newInstance(TYPE_FOLDER, id)` (DellNoteDialog:44–51)
- Implementation: confirmation → `FolderNotesRealmController.delFolderNote(id)` (DellNoteDialog:76–77)
- Realm write: deletes all notes in folder, then deletes folder (FolderNotesRealmController:34–39)
- Callback: same `notifyDataChanged()` flow

**Delete note**  
- Trigger: long-press note → `ActionMode` → "delete"
- Dialog: `DellNoteDialog.newInstance(TYPE_NOTE, id)`
- Implementation: confirmation → `FolderNotesRealmController.delNote(id)` (DellNoteDialog:78–79)
- Realm write: removes note from folder's list, deletes note object (FolderNotesRealmController:79–85)

### 1.4 Drag-Reorder Mechanic

**`ItemTouchHelper` setup** (FolderNoteFragment:217–281)  
- Both adapters (folders and notes) use the same drag handler
- Direction: up/down only (no swipe)
- Callback: `onMove(fromPos, toPos)` → `rv.getAdapter().notifyItemMoved(fromPos, toPos)`
- Drag tracking: `dragFrom`, `dragTo` integers (line 228–229)
- **Bug/TODO:** "todo need fix if move bellow add list" (line 250) — suggests no special handling for "add list" card yet

**`clearView()`** (line 264–271)  
- Called when drag ends
- If movement detected (`dragFrom != dragTo`), calls `reallyMoved(from, to)`
- `reallyMoved()` dispatches: `FolderNotesRecyclerViewAdapter.itemMove()` or `NotesRecyclerViewAdapter.itemMove()`

**Adapter `itemMove()`** (FolderNotesRecyclerViewAdapter:86–95, NotesRecyclerViewAdapter:90–100)  
- Realm write: `App.realm.executeTransaction((realm) -> { list.add(to, list.remove(from)); })`
- Boundary: target position clamped to `list.size()-1` if beyond bounds

### 1.5 Action Mode

**`FolderNoteActionModeCallback`** (file:1–49)  
- Extends `EditDeleteActionModeCallback` (base class: line 18–87)
- Constructor args: `MainActivity`, `FolderNoteFragment`, `type` (TYPE_FOLDER or TYPE_NOTE), `id`
- State: sets `fragment.actionModeIsEnabled = true` (line 27) / `false` (line 32)
- Keyboard: `showKeyboard()` in `onEditClicked()` (line 39)
- Lifecycle: `onShown()` → dismiss empty `ActionMode`, then start this one; `onDismissed()` → flag reset

**`EditDeleteActionModeCallback` base** (line 18–87)  
- Creates menu items: "edit " + "delete " with icons
- Tints action bar for current tab (line 60)
- Overridden labels: `editLabel()`, `deleteLabel()`
- Lifecycle: `onCreateActionMode()`, `onPrepareActionMode()`, `onDestroyActionMode()`

### 1.6 Dialog UI

**`AddNoteDialog` (extends `DialogFragment`)** (line 30–162)  
- Layout: `note_and_folder_add_edit_dialog.xml` (TextInputLayout + TextInputEditText)
- Material dialog: `MaterialAlertDialogBuilder` (line 104)
- Validation: on positive button click (line 144–155), if empty → error on `TextInputLayout`
- Keyboard: `toggleSoftInputFromWindow(SHOW_FORCED)` on show (line 104–107); `hideSoftInputFromWindow()` on dismiss
- Back key: intercepted, no-op (line 116–122)
- Dialog options: `setCanceledOnTouchOutside(false)`, `setOnKeyListener()` block back
- Callback: `notifyDataChanged()` walks fragment manager, finds `FolderNoteFragment`, calls its `notifyDataChanged()`

**`EditNoteDialog` (extends `AddNoteDialog`)** (line 15–58)  
- Overrides button text: "Edit"
- Overrides `positiveButtonInterface`: calls `editFolderNote()` or `editNote()`
- Overrides `setEtText()`: pre-fills with current value from Realm

**`DellNoteDialog` (extends `DialogFragment`)** (line 28–114)  
- Same layout (inflated but not used, line 71)
- Confirmation message: "Are you sure?" (line 74)
- Button text: "Dell" (likely typo for "Delete"; line 75)
- Back key: intercepted, no-op (same as Add)
- No keyboard handling (no EditText focus)
- Callback: same `notifyDataChanged()` pattern

### 1.7 Empty State

**Trigger:** (FolderNoteFragment:116–131, 161–165, 195–199)  
- `notifyDataChanged()` checks adapter item count
- If `== 0`: `emptyState.setVisibility(View.VISIBLE)`
- Otherwise: `emptyState.setVisibility(View.GONE)`

**Layout:** (note_fragment_layout.xml:17–22)  
- `LinearLayout` with id `empty_state`
- Includes `@layout/empty_state` (not shown; likely a standard empty state UI)

### 1.8 Special "Add List" Card (TODO / Incomplete)

**Evidence:**  
- Comment in `FolderNoteFragment.setTouchHelper()` line 250: "todo need fix if move bellow add list"
- Same comment in folder panel code
- No explicit implementation in notes adapters or layouts
- Possible interpretation: **placeholder for a future feature** to add a new folder/note without dialog, or a special card that behaves differently

**Current state:** Not implemented; mentioned only as a TODO.

---

## 2. State & Data Flow

### 2.1 Data Model

**Realm schema (Java objects, no migrations)**  
- `FolderNotesObject`: `id` (long), `name` (String), `notesObjectRealmList` (RealmList<NoteObject>)
- `NoteObject`: `id` (long), `idFolder` (long), `text` (String)

**Global state holders (anti-pattern)**  
- `App.folderOfNotesContainerList`: static global `RealmList<FolderNotesObject>` containing all folders
- `App.realm`: static global Realm instance, initialized via `App.initRealm()`

**Fragment state**  
- `folderViewShowing: boolean` (never read; candidate for removal)
- `actionModeIsEnabled: boolean` (used to gate action mode behavior)
- `isNoteFragment: boolean` (tracks current view)
- `type: int` (TYPE_FOLDER=7 or TYPE_NOTE=5)
- `id: long` (folderId when viewing notes)
- `title: String` (current title, for back navigation)
- `idFolderNoteObject, idNoteObject: long` (unused fields)

### 2.2 Data Flow (CRUD)

**Creation:**  
1. User taps menu "add"
2. Fragment creates dialog with type + id (parent folder id for notes)
3. Dialog shows EditText, user enters text
4. Positive button → validation → `positiveButtonInterface.onClick()`
5. Implementation calls `FolderNotesRealmController.addFolderNote/addNote()`
6. Controller creates object, generates unique ID via `RealmDb.newUniqueId()`, appends to Realm list + global list
7. Dialog calls `notifyDataChanged()` (walks fragment manager)
8. Fragment adapter notifies, UI updates

**Mutation:**  
1. User long-presses card → action mode starts
2. User taps "edit" → dialog opens with current value pre-filled
3. User edits, taps positive button
4. Dialog calls `editFolderNote/editNote()` → Realm transaction mutates object
5. Same `notifyDataChanged()` callback

**Deletion:**  
1. User long-presses card → action mode
2. User taps "delete" → confirmation dialog
3. User confirms → `delFolderNote/delNote()` executes Realm delete
4. Same `notifyDataChanged()` callback

### 2.3 Threading

**Main thread only**  
- All operations on main thread (no explicit threading)
- Realm configured with `allowQueriesOnUiThread = true` (implicit, based on Fragment/Activity lifecycle)
- `App.realm.executeTransaction()` blocks on main thread
- Keyboard toggling on main thread via `InputMethodManager`

**No concurrency primitives:** no locks, no synchronized blocks, no coroutines

### 2.4 Lifecycle Integration

**Fragment lifecycle:**  
- `onCreateView()`: inflate layout, find RecyclerView + empty state (line 71–74)
- `onViewCreated()`: set layout manager, call `setFolderNoteViews()`, attach drag helper (line 77–89)
- `onCreateOptionsMenu()`: add "add" menu item (line 94–114)

**Dialog lifecycle:**  
- `newInstance()`: creates Fragment with args bundle
- `onCreateDialog()`: builds Material dialog
- `onStart()`: sets positive button click listener with validation (line 142–156)
- On positive click: `positiveButtonInterface.onClick()` → Realm write → `notifyDataChanged()` → `dialog.dismiss()`

**Drag-reorder lifecycle:**  
- `ItemTouchHelper.onMove()`: called continuously during drag
- `ItemTouchHelper.clearView()`: called on drag end, writes to Realm if position changed

### 2.5 Adapters as State Holders

**Critical bug: adapters hold mutable lists**  
- `FolderNotesRecyclerViewAdapter.folderNotesList` = reference to `App.folderOfNotesContainerList`
- `NotesRecyclerViewAdapter.notesList` = reference to folder's `getTasks()` at construction time
- When a new adapter is created, lists are fetched fresh from Realm
- **Race condition risk:** if dialog mutates Realm while adapter holds stale reference, inconsistency

**Callback pattern, not reactive:**  
- Adapters don't observe Realm changes
- Manual `notifyDataSetChanged()` called by fragment after each dialog close
- No automatic refresh if Realm mutates elsewhere

---

## 3. Edge Cases & Gotchas

### 3.1 Empty States

**Folders empty:**  
- Initial app state; user taps "add" to create first folder
- Empty state shown (line 161–165)

**Notes empty in folder:**  
- After folder creation or all notes deleted
- Empty state shown (line 195–199)

**Drag-reorder on empty list:**  
- `ItemTouchHelper` won't trigger if `getItemCount() == 0`
- No edge case expected

### 3.2 Navigation Back-Press

**Notes view → Folders view:**  
- Back button (action bar home icon): `onOptionsItemSelected(android.R.id.home)` → `setFolderNoteViews()` (line 204–209)
- **Bug:** title not reset explicitly; `title` variable used but not updated in `setFolderNoteViews()` (line 141 sets local "Notes" but doesn't update field)

**Fragment back-press (via activity):**  
- No `onBackPressed()` override; relies on activity default behavior
- If activity calls `popBackStack()`, fragment is destroyed

**Action mode back-press:**  
- Intercepted by `setOnKeyListener()` in dialogs (line 116–122)
- Action mode itself: no custom back handling; dismissed normally

### 3.3 Config Changes (Rotation)

**Fragment persistence:**  
- Fragment state saved via `onSaveInstanceState()`, restored via `onCreate(savedInstanceState)`
- **Missing:** no explicit state save for `folderViewShowing`, `isNoteFragment`, `id`, `type`
- **Risk:** on rotation, `FolderNoteFragment.onViewCreated()` always calls `setFolderNoteViews()` (line 82), resetting to folders view even if user was viewing notes

**Dialog persistence:**  
- DialogFragment auto-saved/restored by framework if shown via `show(..., tag)`
- **Risk:** if dialog open during rotation, it survives but may reference stale adapter

**Adapter persistence:**  
- Adapters not persisted; new adapters created on each `setFolderNoteViews()` / `setNoteViews()`
- **Risk:** drag state (`dragFrom`, `dragTo`) in `ItemTouchHelper` not preserved

### 3.4 Concurrent Mutations

**No protection if:**  
- Background service mutates Realm while UI is open
- Multiple fragments/dialogs open simultaneously (app uses `getSupportFragmentManager().getFragments()` linear search in dialogs)
- User drags while dialog writes

**Result:** race between drag position update and Realm write, or stale adapter list

### 3.5 "Add List" Card (Incomplete Feature)

**Current state:** TODO comment indicates feature not finished (line 250)
- No special card rendered in adapter
- Drag-reorder TODO suggests handling needed for a special card position
- Possible intended behavior: final card in list is a static "add" button card (not a note), must not be reorderable or must always stay at bottom

**Compose migration impact:** must clarify requirement before implementing

### 3.6 ActionMode State Inconsistency

**Bug:** `fragment.actionModeIsEnabled` flag set in callback, but never read in Java code (line 27, 32)  
- Set by `onShown()` / `onDismissed()` in `FolderNoteActionModeCallback`
- No guard using this flag in fragment logic
- Likely artifact of incomplete refactor; remove in Compose

### 3.7 Drag-Reorder Position Clamping

**Boundary handling:**  
- If user drags beyond list end, target position clamped to `size()-1` (FolderNotesRecyclerViewAdapter:90, NotesRecyclerViewAdapter:95)
- **Edge case:** user drags folder to position 100, only 3 folders exist → item placed at position 2
- **With "add list" card:** if card is final item and is not in adapter data, drag math breaks

### 3.8 Stale EditText References

**AddNoteDialog pattern:**  
- `EditText etText` initialized in `onCreateDialog()` (line 99)
- Positive button listener set in `onStart()` (line 144)
- `onStart()` is called after `onCreateDialog()`, guaranteed in Fragment lifecycle
- **Risk:** if dialog is recreated (rotation) while open, reference to old EditText persists in positive button lambda

### 3.9 Keyboard Management

**Show:**  
- Menu item click: `toggleSoftInputFromWindow(..., SHOW_FORCED)` (AddNoteDialog:105–107)
- Edit action mode: `showKeyboard()` in `onEditClicked()` (FolderNoteActionModeCallback:39)

**Hide:**  
- Dialog dismiss: `hideSoftInputFromWindow()` (AddNoteDialog:152, DellNoteDialog implicit)
- Manual calls in click handlers

**Edge case:** if dialog dismissed before handler runs, keyboard hidden prematurely

### 3.10 Realm ID Generation

**Pattern:** `RealmDb.newUniqueId(Class)` generates next ID (details in RealmDb, not shown)  
**Risk:** if app exits during transaction, ID counter may be inconsistent on restart

### 3.11 Fragment Manager Linear Search

**Anti-pattern:** dialogs call:
```java
List<Fragment> fragments = getActivity().getSupportFragmentManager().getFragments();
for (Fragment fragment : fragments) {
  if (fragment instanceof FolderNoteFragment) { ... }
}
```
(AddNoteDialog:129–135, DellNoteDialog:102–109)

**Edge case:** if multiple `FolderNoteFragment` instances exist, only first found is notified. However, app architecture likely single-instance.

### 3.12 Commented-Out Code

**NotesRecyclerViewAdapter:** note click listener disabled (line 61):
```java
// holder.text.setOnClickListener((v)-> onClickListener.onClick(holder,position,id));
```
Only long-click enabled. **Impact:** notes can only be edited via long-press; quick tap does nothing.

---

## 4. Compose Mapping

### 4.1 Navigation & State Management

**Current fragment-based nav:**  
```
FolderNoteFragment 
  ├─ setFolderNoteViews() → show FolderNotesRecyclerViewAdapter
  └─ setNoteViews(id) → show NotesRecyclerViewAdapter + back button
```

**Target Compose structure:**  
```
NotesScreen(viewModel: NotesViewModel)
  ├─ state: NotesUiState = viewModel.uiState.collectAsState()
  ├─ when (state.view) {
  │   VIEW_FOLDERS → FoldersScreen(...)
  │   VIEW_NOTES → NotesListScreen(folderId, ...)
  │ }
  ├─ LazyColumn(state = rememberLazyListState()) with drag-reorder via sh.calvin.reorderable
  ├─ ActionModeBar (replaces system CAB; shown when actionModeId != null)
  ├─ Dialog layer (AddNoteDialog, EditNoteDialog, DeleteNoteDialog composables)
  └─ EmptyState (shown when list.isEmpty())
```

**ViewModel scope:**  
```kotlin
class NotesViewModel @Inject constructor(
  private val notesRepository: NotesRepository,
  // ...
) : ViewModel() {
  val uiState: StateFlow<NotesUiState> = 
    notesRepository.observeFolders()  // Flow<List<FolderNotesObject>>
      .map { folders ->
        NotesUiState(
          view = if (selectedFolderId == null) VIEW_FOLDERS else VIEW_NOTES,
          folders = folders,
          currentNotes = notesRepository.getNotes(selectedFolderId),
          selectedFolderId = selectedFolderId,
          actionModeId = actionModeId
        )
      }.stateIn(...)
  
  fun selectFolder(id: Long) { selectedFolderId = id }
  fun clearActionMode() { actionModeId = null }
  fun showAddFolder() { dialogState = ADD_FOLDER_DIALOG }
  // ...
}

data class NotesUiState(
  val view: NotesView,  // FOLDERS or NOTES
  val folders: List<FolderNotesObject>,
  val currentNotes: List<NoteObject>,
  val selectedFolderId: Long?,
  val actionModeId: Long?,  // ID of item in action mode
  val actionModeType: NotesActionModeType? = null,  // FOLDER or NOTE
  val dialogState: DialogState? = null
)
```

**Repository (data-interop layer):**  
```kotlin
class NotesRepository @Inject constructor(
  private val realmController: FolderNotesRealmController
) {
  fun observeFolders(): Flow<List<FolderNotesObject>> =
    flow {
      val results = App.folderOfNotesContainerList  // or RealmDb query
      emit(results.copyFromRealm())  // detach from Realm
      // TODO: add listener for live updates via RealmResults.addChangeListener
    }
  
  fun getNotes(folderId: Long): List<NoteObject> =
    FolderNotesRealmController.getNotesList(folderId).copyFromRealm()
  
  suspend fun addFolder(name: String) =
    withContext(Dispatchers.Main) {
      FolderNotesRealmController.addFolderNote(name)
    }
  
  suspend fun deleteFolder(id: Long) =
    withContext(Dispatchers.Main) {
      FolderNotesRealmController.delFolderNote(id)
    }
  // ...
}
```

### 4.2 Composable Components

**`NotesScreen`** (main entry point for page 0)
```kotlin
@Composable
fun NotesScreen(
  viewModel: NotesViewModel = viewModel(factory = ...),
  onNavigateFolders: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  
  when (uiState.view) {
    NotesView.FOLDERS -> FoldersListScreen(uiState, viewModel)
    NotesView.NOTES -> NotesListScreen(uiState, viewModel)
  }
  
  // Action mode bar (replaces system CAB)
  if (uiState.actionModeId != null) {
    ActionModeBar(
      title = when (uiState.actionModeType) {
        ActionModeType.FOLDER -> "Folder"
        ActionModeType.NOTE -> "Note"
      },
      onEdit = { viewModel.showEditDialog() },
      onDelete = { viewModel.showDeleteDialog() },
      onDismiss = { viewModel.clearActionMode() }
    )
  }
  
  // Dialog layer
  when (uiState.dialogState) {
    is DialogState.AddFolder -> AddFolderDialog(viewModel)
    is DialogState.EditFolder -> EditFolderDialog(viewModel, uiState.dialogState.id)
    is DialogState.DeleteFolder -> DeleteFolderDialog(viewModel, uiState.dialogState.id)
    is DialogState.AddNote -> AddNoteDialog(viewModel, uiState.selectedFolderId!!)
    is DialogState.EditNote -> EditNoteDialog(viewModel, uiState.dialogState.id)
    is DialogState.DeleteNote -> DeleteNoteDialog(viewModel, uiState.dialogState.id)
    null -> {}
  }
}
```

**`FoldersListScreen`**  
```kotlin
@Composable
fun FoldersListScreen(
  uiState: NotesUiState,
  viewModel: NotesViewModel
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Notes") },
        navigationIcon = null  // no back button at folders level
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.showAddFolderDialog() }
      ) { Icon(Icons.Default.Add, null) }
    }
  ) { innerPadding ->
    if (uiState.folders.isEmpty()) {
      EmptyState()
    } else {
      ReorderableLazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        state = rememberLazyListState(),
        onMove = { from, to ->
          viewModel.reorderFolders(from, to)
        }
      ) {
        items(
          uiState.folders,
          key = { it.id }
        ) { folder ->
          FolderCard(
            folder = folder,
            isInActionMode = uiState.actionModeId == folder.id,
            onClick = { viewModel.selectFolder(folder.id) },
            onLongClick = {
              viewModel.showActionMode(folder.id, ActionModeType.FOLDER)
            },
            modifier = Modifier
              .animateItemPlacement()
              .draggableItem(reorderableState, ...)
          )
        }
      }
    }
  }
}
```

**`NotesListScreen`**  
```kotlin
@Composable
fun NotesListScreen(
  uiState: NotesUiState,
  viewModel: NotesViewModel
) {
  val folderId = uiState.selectedFolderId ?: return
  val folder = uiState.folders.find { it.id == folderId }
  
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(folder?.name ?: "Notes") },
        navigationIcon = {
          IconButton(onClick = { viewModel.clearSelections() }) {
            Icon(Icons.Default.ArrowBack, null)
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { viewModel.showAddNoteDialog() }
      ) { Icon(Icons.Default.Add, null) }
    }
  ) { innerPadding ->
    if (uiState.currentNotes.isEmpty()) {
      EmptyState()
    } else {
      ReorderableLazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        onMove = { from, to ->
          viewModel.reorderNotes(folderId, from, to)
        }
      ) {
        items(
          uiState.currentNotes,
          key = { it.id }
        ) { note ->
          NoteCard(
            note = note,
            isInActionMode = uiState.actionModeId == note.id,
            onLongClick = {
              viewModel.showActionMode(note.id, ActionModeType.NOTE)
            },
            modifier = Modifier
              .animateItemPlacement()
              .draggableItem(reorderableState, ...)
          )
        }
      }
    }
  }
}
```

**Card composables:**  
```kotlin
@Composable
fun FolderCard(
  folder: FolderNotesObject,
  isInActionMode: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 6.dp, vertical = 4.dp)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      ),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isInActionMode) 
        MaterialTheme.colorScheme.primaryContainer 
      else 
        Color.Unspecified
    )
  ) {
    Text(
      folder.name,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      fontSize = 16.sp,
      textAlign = TextAlign.Start
    )
  }
}

@Composable
fun NoteCard(
  note: NoteObject,
  isInActionMode: Boolean,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 6.dp, vertical = 4.dp)
      .combinedClickable(onLongClick = onLongClick),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isInActionMode) 
        MaterialTheme.colorScheme.primaryContainer 
      else 
        Color.Unspecified
    )
  ) {
    Text(
      note.text,
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      fontSize = 16.sp,
      textAlign = TextAlign.Start
    )
  }
}
```

**Dialog composables (Material 3 AlertDialog):**  
```kotlin
@Composable
fun AddFolderDialog(viewModel: NotesViewModel) {
  var text by remember { mutableStateOf("") }
  var showError by remember { mutableStateOf(false) }
  
  AlertDialog(
    onDismissRequest = { viewModel.clearDialog() },
    title = { Text("Add Folder") },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it; showError = false },
        label = { Text("Folder name") },
        isError = showError,
        supportingText = if (showError) { { Text("Required") } } else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (text.isBlank()) {
            showError = true
          } else {
            viewModel.addFolder(text)
            viewModel.clearDialog()
          }
        }
      ) { Text("Add") }
    },
    dismissButton = {
      TextButton(onClick = { viewModel.clearDialog() }) { Text("Cancel") }
    }
  )
}

@Composable
fun EditFolderDialog(
  viewModel: NotesViewModel,
  folderId: Long
) {
  // Similar structure, pre-filled with current folder name
  // Calls viewModel.editFolder(folderId, text)
}

@Composable
fun DeleteFolderDialog(
  viewModel: NotesViewModel,
  folderId: Long
) {
  AlertDialog(
    onDismissRequest = { viewModel.clearDialog() },
    text = { Text("Are you sure?") },
    confirmButton = {
      TextButton(
        onClick = {
          viewModel.deleteFolder(folderId)
          viewModel.clearDialog()
        }
      ) { Text("Delete") }
    },
    dismissButton = {
      TextButton(onClick = { viewModel.clearDialog() }) { Text("Cancel") }
    }
  )
}

// Similar for AddNoteDialog, EditNoteDialog, DeleteNoteDialog
```

**ActionModeBar (replaces system CAB):**  
```kotlin
@Composable
fun ActionModeBar(
  title: String,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    title = { Text(title) },
    navigationIcon = {
      IconButton(onClick = onDismiss) {
        Icon(Icons.Default.Close, null)
      }
    },
    actions = {
      IconButton(onClick = onEdit) {
        Icon(Icons.Default.Edit, "Edit", tint = Color.White)
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Default.Delete, "Delete", tint = Color.White)
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.primary
    ),
    modifier = modifier
  )
}
```

### 4.3 Event Flow

**User action → State update → Recomposition:**  
1. User taps folder card
2. `onClick` lambda calls `viewModel.selectFolder(id)`
3. ViewModel mutates `selectedFolderId` state, emits new `NotesUiState` with `view = NOTES`
4. `uiState.collectAsState()` triggers recomposition
5. `when (uiState.view)` switches to `NotesListScreen`

**Drag-reorder:**  
1. User drags note card (via `ReorderableState`)
2. `reorderableState.onMove(from, to)` calls `viewModel.reorderNotes(folderId, from, to)`
3. ViewModel calls `notesRepository.reorderNotes(folderId, from, to)`
4. Repository calls `FolderNotesRealmController.reorderNote(folderId, from, to)` (Realm transaction)
5. ViewModel fetches updated notes list (via repository `observeNotes()`)
6. UI recomposes with new order

**Delete note (via action mode):**  
1. User long-presses note → action mode shows
2. User taps delete icon in action mode bar
3. `onDelete` lambda calls `viewModel.showDeleteDialog()`
4. ViewModel sets `dialogState = DELETE_NOTE(id)`
5. Recomposition shows `DeleteNoteDialog` composable
6. User confirms → `viewModel.deleteNote(id)`
7. ViewModel calls `repository.deleteNote(id)` → `FolderNotesRealmController.delNote(id)`
8. Repository / ViewModel refreshes note list
9. Dialog dismissed, action mode cleared

### 4.4 Key Differences from Java

| Aspect | Java | Compose |
|--------|------|---------|
| **Navigation** | Fragment + back stack | State machine in ViewModel |
| **Adapter** | Manual RecyclerView adapter | LazyColumn with `items()` lambda |
| **State** | Scattered (fragment, adapter, global) | Centralized ViewModel + StateFlow |
| **Drag** | ItemTouchHelper | `sh.calvin.reorderable` library |
| **Dialogs** | DialogFragment + Material Builder | Composable + AlertDialog |
| **Action mode** | System CAB + ActionMode.Callback | Composable ActionModeBar |
| **Refresh** | Manual `notifyDataChanged()` | Automatic via Flow/StateFlow |
| **Threading** | Main thread (blocking) | Coroutines (non-blocking, default Dispatchers.Main) |
| **Back-press** | Fragment back stack | BackHandler state machine in ViewModel |

---

## 5. Files to Delete After Migration

### Java classes to delete:
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/note_fragment/FolderNoteFragment.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/note_fragment/FolderNotesRecyclerViewAdapter.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/note_fragment/NotesRecyclerViewAdapter.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/dialog/note_dialog/AddNoteDialog.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/dialog/note_dialog/EditNoteDialog.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/dialog/note_dialog/DellNoteDialog.java`
- `/app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/note/FolderNoteActionModeCallback.java`

### XML layouts to delete:
- `/app/src/main/res/layout/note_fragment_layout.xml`
- `/app/src/main/res/layout/notes_item_card_view.xml`
- `/app/src/main/res/layout/note_and_folder_add_edit_dialog.xml`
- `/app/src/main/res/layout/note_fragment_container.xml` (if only used by notes)

### Keep (data layer):
- `/app/src/main/java/com/shumidub/todoapprealm/realmmodel/notes/NoteObject.java`
- `/app/src/main/java/com/shumidub/todoapprealm/realmmodel/notes/FolderNotesObject.java`
- `/app/src/main/java/com/shumidub/todoapprealm/realmcontrollers/notescontroller/FolderNotesRealmController.java`

---

## Appendix: Known Issues & TODO Items

1. **Config change bug:** rotation in notes view resets to folders view (missing state save)
2. **"Add list" card:** incomplete feature, referenced only in TODO comment
3. **Typo:** button text "Dell" instead of "Delete" in `DellNoteDialog`
4. **Unused field:** `folderViewShowing` in fragment
5. **Dead code:** `actionModeIsEnabled` flag set but never read
6. **Race condition:** drag-reorder + dialog mutations not synchronized
7. **Adapter bug:** `NotesRecyclerViewAdapter.noteList` shadowing + stale reference risk
8. **Drag clamping:** boundary handling may produce unintuitive behavior with "add list" card
9. **Keyboard management:** no guarantee soft keyboard hidden before showing next dialog
10. **Fragment manager linear search:** fragile; assumes single `FolderNoteFragment` instance

---

**Spec finalized:** 2026-06-13  
**Migration phase:** 3 (Notes)  
**Target: Jetpack Compose + Material 3**
