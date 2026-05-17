# task-002 · Секции внутри категории

**Sprint**: sprint-001-k8m3n2p-task-ux
**Status**: Phase 2 complete → Phase 3 (architecture)
**Owner**: bg-agent (TBD)

## User-facing описание
Внутри категории (folder) можно создавать секции для группировки задач.
- У секции есть **имя** и настройка **"свёрнута по дефолту"** (collapsed-by-default)
- При открытии категории секция отображается как заголовок-разделитель; задачи внутри могут быть скрыты, если свёрнута
- Настройки секции редактируются по **long click** по заголовку секции
- Секции и задачи без секции могут быть в **любом ручном порядке** внутри папки (нет разделения "сначала без секции, потом секции" — всё в одном drag-n-drop списке)

## Requirements (финал, Phase 2 confirmed)

### Модель данных
- **R1.** Новая Realm-модель `SectionObject`:
  - `long id` (timestamp-based, как `TaskObject` / `FolderTaskObject`)
  - `String name` (1–40 символов, не пустое; уникальность не требуется)
  - `boolean collapsedByDefault`
  - `boolean currentlyCollapsed` — runtime-состояние, persisted в Realm
  - `long parentFolderId`
  - `int position` — общий ordered-index внутри папки (см. R3)
- **R2.** `TaskObject` получает поле `long sectionId` (default `0` = "без секции"). Также добавляется поле `int position` для совместного упорядочивания с секциями.
- **R3.** Внутри одной папки секции и задачи **сосуществуют в одном ordered-списке** по `position`. Задачи без секции (`sectionId == 0`) могут быть в любом месте — сверху, между секциями, снизу. Никакой неявной группировки "free tasks вверху".
- **R4.** Задачи **внутри секции** (`sectionId != 0`) тоже упорядочены по своей `position`, drag-order, **без принудительного `done ASC`**.

### Realm migration
- **R5.** `RealmMigrations.SCHEMA_VERSION` поднимается до 4 (с текущего 3):
  - `schema.create("SectionObject")` со всеми полями R1
  - `schema.get("TaskObject").addField("sectionId", long.class)` (default 0)
  - `schema.get("TaskObject").addField("position", int.class)` — backfill: для каждой папки проставить `position = index` в текущем `RealmList` (сохраняет визуальный порядок старых БД)
- **R6.** После миграции старая БД отображается **как сейчас** — никаких header'ов, пока пользователь не создаст первую секцию.

### CRUD
- **R7.** Новый `SectionsRealmController`:
  - `getSections(long folderId)` — sorted by `position`
  - `addSection(long folderId, String name, boolean collapsedByDefault, int position)`
  - `editSection(SectionObject, String name, boolean collapsedByDefault)`
  - `deleteSection(SectionObject)` — задачи внутри получают `sectionId=0` (сохраняют свою `position`)
  - `setCurrentlyCollapsed(SectionObject, boolean)`
  - `moveTaskToSection(TaskObject task, long sectionId, int newPosition)`
  - `reorderItems(folderId, List<ItemMove>)` — пересчёт `position` после drag-n-drop (учитывает оба типа: section header + task)

### UI
- **R8.** В `SmallTasksFragment` / `TasksRecyclerViewAdapter` — multi-view-type RecyclerView:
  - `VIEW_TYPE_TASK` (существующий)
  - `VIEW_TYPE_DONE_FOOTER` (существующий)
  - `VIEW_TYPE_SECTION_HEADER` (новый): имя, индикатор collapsed/expanded
  - Список после flatten: `[item1, item2, sectionA_header, task_in_A, task_in_A, item3, sectionB_header, ...]` где item = task или header.
- **R9.** **Тап** на section header → toggle `currentlyCollapsed` (persisted сразу). Свёрнутая секция скрывает свои задачи из RecyclerView.
- **R10.** **Long-click** на section header → сразу открывает диалог редактирования секции (поля `name`, `collapsedByDefault` switch, кнопка `Delete`).
- **R11.** **Кнопка "Добавить секцию"** — пункт в **toolbar overflow menu** фрагмента. Открывает тот же диалог редактирования (создание).
- **R12.** Перемещение задач между секциями — **через drag-n-drop**. Расширить существующий `ItemTouchHelper`: при пересечении границы header'а задача меняет свой `sectionId` на id секции под которой остановилась.
- **R13.** **Drag header'а** двигает секцию **вместе со всеми её задачами** как единый блок (атомарный move с пересчётом `position`).
- **R14.** Диалог редактирования секции (`ui/dialog/section_dialog/SectionEditDialog.java` + layout) переиспользует визуальный язык `dialog_edit_task.xml`.

### Scope
- **R15.** Применяется ко **всем трём task-табам** (Tasks pos 1, Tasks2 pos 2, Tasks3 pos 3 после task-003). Notes — нет.
- **R16.** В `isDaily` папках секции тоже работают (никаких исключений).

## Затронутые файлы
- `realmmodel/task/` — `SectionObject.java` + изменения в `TaskObject.java` (поля `sectionId`, `position`)
- `RealmMigrations.java` + `RealmConfiguration` — `SCHEMA_VERSION=4`
- `realmcontrollers/taskcontroller/` — новый `SectionsRealmController`, изменения в `TasksRealmController` (учитывать `sectionId`/`position` в `getTasks`, `addTask`)
- `ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java` — toolbar menu "Add section", multi-type RecyclerView
- `TasksRecyclerViewAdapter.java` — viewTypes, expand/collapse, drag-with-section
- `ui/dialog/section_dialog/` — новый диалог
- ItemTouchHelper callback — расширение для cross-section drag и section-as-block drag

## Design

### 0. Координация миграции с task-003
Обе задачи поднимают `SCHEMA_VERSION` с 3 → 4 **одним блоком** `if (oldVersion < 4)`. Этот документ — истина по содержимому блока 4. task-003 наследует только свой подпункт (folderOfTasksList3); все остальные подпункты (Section schema, TaskObject.sectionId, TaskObject.position, backfill) принадлежат task-002.

### 1. Realm schema diff (SCHEMA_VERSION 3 → 4)

В `RealmMigrations.java` после существующего `if (oldVersion < 3)` блока добавить:

```
if (oldVersion < 4) {
    // (a) task-003: third Tasks tab folder list
    schema.get("RealmFoldersContainer")
            .addRealmListField("folderOfTasksList3", schema.get("FolderTaskObject"));

    // (b) task-002: SectionObject schema
    RealmObjectSchema sectionSchema = schema.create("SectionObject")
            .addField("id", long.class, FieldAttribute.PRIMARY_KEY)
            .addField("name", String.class, FieldAttribute.REQUIRED)
            .addField("collapsedByDefault", boolean.class)
            .addField("currentlyCollapsed", boolean.class)
            .addField("parentFolderId", long.class, FieldAttribute.INDEXED)
            .addField("position", int.class);

    // (c) task-002: TaskObject.sectionId (default 0 == "no section")
    schema.get("TaskObject")
            .addField("sectionId", long.class);

    // (d) task-002: TaskObject.position (backfilled below)
    schema.get("TaskObject")
            .addField("position", int.class);

    // (e) Backfill TaskObject.position per folder using current RealmList order.
    // We must iterate FolderTaskObject (each with its folderTasks RealmList) and stamp
    // position = index. DynamicRealm operates on dynamic instances; access RealmList
    // via DynamicRealmObject.getList("folderTasks").
    RealmResults<DynamicRealmObject> folders =
            realm.where("FolderTaskObject").findAll();
    for (DynamicRealmObject folder : folders) {
        RealmList<DynamicRealmObject> list = folder.getList("folderTasks");
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setInt("position", i);
        }
    }
    // sectionId defaults to 0 (no section). No backfill needed.
}
```

Notes:
- `parentFolderId` is `@Indexed` because sections are queried per-folder hot path.
- `name` is `@Required` (Realm cannot store null for the String).
- No `@Index` on `TaskObject.sectionId` — every query already scopes to a single folder's RealmList; full-table scans aren't an issue.

### 2. `SectionObject` Realm class (full signature)

```
package com.shumidub.todoapprealm.realmmodel.task;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class SectionObject extends RealmObject {
    @PrimaryKey private long id;
    @Required  private String name;
    private boolean collapsedByDefault;
    private boolean currentlyCollapsed;
    @Index     private long parentFolderId;
    private int position;

    // getters/setters for all fields (Realm-style, no logic)
}
```

`SectionObject` is **not** referenced from `FolderTaskObject` via RealmList — relation is by `parentFolderId` (mirroring the existing pattern where `TaskObject.taskFolderId` is a long, not a back-reference). This keeps migration simple and is consistent with how task-folder ownership is already modelled.

### 3. `SectionsRealmController` API

Lives in `realmcontrollers/taskcontroller/SectionsRealmController.java`. All methods static, mirror the style of `TasksRealmController`. All write paths call `App.initRealm()` then `App.realm.executeTransaction(...)`.

```
// Reads (Realm objects, live results)
public static RealmResults<SectionObject> getSections(long folderId);
    // .equalTo("parentFolderId", folderId).sort("position", Sort.ASCENDING)

public static SectionObject getSection(long sectionId);

// Mutations
public static SectionObject addSection(long folderId, String name,
                                       boolean collapsedByDefault, int position);
    // Generates timestamp-based id (same getIdForNextValue pattern as TasksRealmController).
    // currentlyCollapsed initialized = collapsedByDefault.
    // After insert, calls compactPositions(folderId) to ensure contiguous ordering
    // (see § 4 — positions are dense ints across both sections and tasks).
    // Returns the managed SectionObject.

public static void editSection(SectionObject s, String name,
                               boolean collapsedByDefault);
    // Trims name; rejects empty / >40 chars by throwing IllegalArgumentException
    // (caller in the dialog already validates; controller is the second line).

public static void deleteSection(SectionObject s);
    // Inside one transaction:
    //  1. snapshot section's id + position before deletion
    //  2. find all TaskObject where taskFolderId == s.parentFolderId
    //     AND sectionId == s.id; set sectionId = 0 (positions kept as-is —
    //     tasks remain at their existing global positions; the header just
    //     disappears from the flatten stream)
    //  3. s.deleteFromRealm()
    //  4. compactPositions(folderId) to close the gap left by the header

public static void setCurrentlyCollapsed(SectionObject s, boolean collapsed);
    // Single-field write; immediate persist (R9).

public static void moveTaskToSection(TaskObject task, long newSectionId, int newPosition);
    // Sets task.sectionId = newSectionId, task.position = newPosition,
    // then compactPositions(task.getTaskFolderId()).

// Drag-n-drop reorder — heart of R12/R13.
public static void reorderItems(long folderId, java.util.List<ItemMove> moves);
    // ItemMove = { ItemKind kind (SECTION|TASK), long id, int newPosition,
    //              long newSectionId /* tasks only; -1 = unchanged */ }
    // Single transaction. After applying all moves, runs compactPositions().

// Helper — re-stamps position = 0..N-1 in current sorted order, mixing
// sections and free tasks (sectionId == 0). Tasks inside a section keep
// their relative order but the section header carries the section's
// global position; tasks-inside-section have their own local positions.
private static void compactPositions(long folderId);
```

**Position model** (key invariant): inside one folder there are **two position spaces**:
1. **Outer space** — ordered list of mixed items where each item is either a `SectionObject` or a `TaskObject` with `sectionId == 0`. Their `position` field is the dense index in this outer list.
2. **Inner space** — for each section S, ordered list of tasks where `sectionId == S.id`. Those tasks' `position` field is the dense index inside that section.

This separation is critical because R13 says "section drags as a block" — moving section S only shuffles outer-space; tasks-in-S keep their inner positions. And R3 ("free tasks can sit anywhere — top, between sections, bottom") fits naturally: free tasks share the outer-space numbering with section headers.

### 4. Adapter flatten algorithm

New flat-item type for the adapter (Java has no sealed types pre-17 sealed; use a small tagged class):

```
public final class AdapterItem {
    public enum Kind { TASK, SECTION_HEADER, DONE_FOOTER }
    public final Kind kind;
    public final TaskObject task;          // non-null when kind=TASK
    public final SectionObject section;    // non-null when kind=SECTION_HEADER
    // factory methods: ofTask(t), ofSection(s), doneFooter()
}
```

Flatten input: `List<SectionObject> sections` (sorted by outer position), `List<TaskObject> tasks` (all not-done tasks in folder). Output: `List<AdapterItem> items`.

```
List<AdapterItem> flatten(List<SectionObject> sections, List<TaskObject> tasks):
    // Step 1: split tasks
    Map<Long, List<TaskObject>> tasksBySection = group(tasks, t -> t.sectionId);
    //   tasksBySection.get(0L) == free tasks
    //   tasksBySection.get(S.id)  == tasks inside S (sorted by t.position asc)
    each list internally sorted by TaskObject.position ASC.

    // Step 2: walk outer space
    List<OuterItem> outer = merge(sections, tasksBySection.get(0L))
        sorted by .position ASC;
        // OuterItem wraps either SectionObject or TaskObject(sectionId==0)

    List<AdapterItem> out = new ArrayList<>();
    for (OuterItem it : outer) {
        if (it.isTask) {
            out.add(AdapterItem.ofTask(it.task));
        } else {
            SectionObject s = it.section;
            out.add(AdapterItem.ofSection(s));
            if (!s.isCurrentlyCollapsed()) {
                for (TaskObject t : tasksBySection.get(s.id)) {
                    out.add(AdapterItem.ofTask(t));
                }
            }
            // collapsed => skip; tasks remain in Realm but absent from RV
        }
    }
    out.add(AdapterItem.doneFooter());  // existing footer behaviour preserved
    return out;
```

`TasksRecyclerViewAdapter.tasks` field is replaced by `List<AdapterItem> items`. Public-but-mutable access from `ItemTouchHelperAttacher` is refactored to a method `getItem(int adapterPosition)`. `getItemViewType` reads `items.get(pos).kind` and returns one of:
- `VIEW_TYPE_TASK = 1`
- `VIEW_TYPE_SECTION_HEADER = 2`
- `VIEW_TYPE_DONE_FOOTER = 123` (existing `FOOTER_VIEW`)

`onCreateViewHolder` inflates new `section_header_card_view.xml` for headers. ViewHolder = `SectionHeaderViewHolder` (TextView for name + ImageView chevron rotating on collapse state).

`onBindViewHolder` for section header:
- `tv.setText(section.getName())`
- chevron rotation: 0deg when expanded, 180deg (or -90deg) when collapsed
- `itemView.setOnClickListener` → toggle `currentlyCollapsed` via controller, then `smallTasksFragment.setTasksAndNotifyDataSetChanged()` to re-flatten (R9)
- `itemView.setOnLongClickListener` → open `SectionEditDialog.forEdit(section)` (R10)
- apply palette via `applyPaletteIfNeeded` extension to header (text, surface, divider)

### 5. Drag-n-drop logic

`ItemTouchHelperAttacher` is the right home — extend its `ItemTouchHelper.SimpleCallback`. Drag types are distinguished by the `viewHolder` class.

#### 5a. `getMovementFlags`
- For `NormalViewHolder` (task) → UP|DOWN (as today).
- For `SectionHeaderViewHolder` → UP|DOWN (sections draggable).
- For `FooterViewHolder` → 0.

#### 5b. `isLongPressDragEnabled`
- Tasks: keep current behaviour.
- Section headers: `false` — long-press on a header opens the edit dialog (R10). Section-as-block drag is initiated via an explicit drag handle on the header (small grip icon, right side). The handle calls `itemTouchHelper.startDrag(headerViewHolder)` from its `onTouch` ACTION_DOWN.

#### 5c. Task move (`onMove` when both source and target are tasks)
- Reorder logic same as today (`notifyItemMoved`), but **at drop time** (`clearView`), the move is committed via `SectionsRealmController.reorderItems` with a single `ItemMove(kind=TASK, id=taskId, newPosition=...)`.
- **Cross-section detection** (R12): compute the target task's "container":
  - Walk `items` from `toPos` upward looking for the nearest preceding `SECTION_HEADER` (or none → free area, sectionId=0).
  - If that container's sectionId differs from the moved task's `sectionId`, the `ItemMove` carries `newSectionId = containerSectionId` (else `-1` = unchanged).
  - The `newPosition` is computed in the correct space (outer if container is free area; inner if a section).
- Edge case — drop directly **on** a section header (toPos points to a header): place the task as the **first child** of that section (newSectionId = header.id, position = 0). If the header is collapsed, expand it first (call `setCurrentlyCollapsed(s, false)`) so the user sees the result.

#### 5d. Section-as-block drag (R13)
When the dragged viewHolder is `SectionHeaderViewHolder`:
- **Block boundaries**: from the header's adapter position, the block spans `[headerPos, headerPos + childCount]` where `childCount` = tasks visible under that header in `items` (0 if collapsed; the inner tasks aren't in `items` when collapsed).
- During drag, the adapter's `onMove` returns `true` only when the target is **not inside** the moving block (we don't want a section to drop inside itself). For visual purposes only the header moves; on `clearView` the controller re-stamps positions.
- **Atomic move at drop**: in `clearView`, build a single `reorderItems` call:
  - `ItemMove(kind=SECTION, id=sectionId, newPosition=newOuterPos)` — outer position chosen by walking the new `items` list and counting outer items up to drop point.
  - Inner tasks **don't move**: their `position` is inner-space and unchanged; their `sectionId` already equals the section being moved.
- **Collapsed section behaviour during drag**: collapsed section's children aren't in `items` (per flatten), so the user drags only the 1-row header. This is intentional — no "shadow" of children is shown. (Decision: simpler UX; if a user wants to see what they're moving, they can expand first.) Children are still re-attached to the section after drop because relationship is by `sectionId`, which is not modified.

#### 5e. `onMove` updates `items` in-memory; commit happens in `clearView`
Same pattern as current code (dragFrom / dragTo). Replace `reallyMoved(taskTarget, taskTargetPosition)` with the controller call described above. The current `tasks.size()` / `task.isDone()` guard is replaced by checks against `items` and item kinds.

### 6. Toolbar menu — "Add section"

`SmallTasksFragment` does **not** currently host a menu (`setHasOptionsMenu` not called). The menu lives in the parent `FolderSlidingPanelFragment.onCreateOptionsMenu`. Two viable hosts; pick the parent for consistency with existing "add folder" / "sync" items:

- In `FolderSlidingPanelFragment.onCreateOptionsMenu`, append a third item (overflow, not always-shown):
  ```
  MenuItem addSection = menu.add(2, 4, 2, R.string.add_section);
  addSection.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
  addSection.setOnMenuItemClickListener(v -> {
      // only meaningful when the sliding panel is expanded (a folder is open)
      if (slidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
          return true;
      }
      SmallTasksFragment current =
          (SmallTasksFragment) smallTaskFragmentPagerAdapter.getItem(viewPagerSmallTasks.getCurrentItem());
      SectionEditDialog.forCreate(current.getTasksFolderId())
          .show(getActivity().getSupportFragmentManager(), "addsection");
      return true;
  });
  ```
- The item is **always visible in overflow** for all 3 task tabs (R15); behaviour is no-op when panel is collapsed.

### 7. `SectionEditDialog`

Path: `ui/dialog/section_dialog/SectionEditDialog.java` + `res/layout/dialog_section_edit.xml`.

Layout — clone of `dialog_edit_task.xml` (reuse same `MaterialAlertDialogBuilder` skin, same paper-card background driven by current palette), simplified content:
- `EditText et_section_name` (inputType text, maxLength 40)
- `SwitchCompat sw_collapsed_default` ("Свёрнута по умолчанию")
- Buttons: Cancel / Delete (edit mode only) / Save

Class skeleton:
```
public class SectionEditDialog extends DialogFragment {
    private static final String ARG_MODE = "mode";          // "create" | "edit"
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_SECTION_ID = "sectionId";

    public static SectionEditDialog forCreate(long folderId) { /* set args, mode=create */ }
    public static SectionEditDialog forEdit(SectionObject s) { /* args: mode=edit, sectionId */ }

    @Override
    public Dialog onCreateDialog(Bundle b) {
        // inflate dialog_section_edit, build MaterialAlertDialogBuilder with the
        // palette-aware ThemeOverlay chosen the same way as dialog_edit_task
        // (Cornflower for tab 1, Canary for tab 3 — driven by SmallTasksFragment palette).
        //
        // Validation rule (run on Save click; if invalid, show TextInputLayout
        // error and DO NOT dismiss):
        //   String n = et.getText().toString().trim();
        //   if (n.isEmpty() || n.length() > 40) → reject
        //
        // Create-mode save: SectionsRealmController.addSection(folderId, n, sw.isChecked(),
        //   nextOuterPosition(folderId));  then notify host fragment to re-flatten.
        // Edit-mode save: SectionsRealmController.editSection(section, n, sw.isChecked());
        // Edit-mode delete: confirm dialog → SectionsRealmController.deleteSection(section).
    }
}
```

Host re-flatten: dialog calls `((SmallTasksFragment) getParentFragment().getChildFragmentManager()...).setTasksAndNotifyDataSetChanged()` — or, simpler, sends a result via `FragmentResultListener` keyed by `"section_changed"`. Picking the latter to avoid fragile parent chain casts.

### 8. Changes to `TasksRealmController` (callers audit — see § 11 Risks)

- `getTasks(long folderId)` — current sort is `"done" ASC`. Replace with: return managed list sorted by `position` ASC, leaving `done` filtering to the existing footer/visibility logic. The adapter's flatten step relies on stable `position` ordering. Done-task list (`getDoneTasks(folderId)`) keeps current behaviour (footer is sectionless).
- `addTask(...)` — append: compute `nextPosition = max(existing position in folder) + 1` and write to `task.position`; `task.sectionId = 0` for "free" by default. When adding from a context where a section is active (future enhancement, not part of this task), the caller passes a target sectionId — for now always 0.
- `changeOrder` — deprecated in favour of `SectionsRealmController.reorderItems`. Keep the method for now (other call-sites in sync/restore code may use it; mark `@Deprecated` and route to the new path).
- `deleteTask` — no change needed (positions become non-contiguous, controller's `compactPositions` is called from the flatten/refresh path; alternatively, call it from `deleteTask` for symmetry — TBD impl-phase).

### 9. Coordination with sibling tasks

- **task-001 (BottomSheet)** — independent. BottomSheet editor edits a single task; touches `text/count/priority/cycling` only. No conflict with section model. Order: task-002 may merge before or after task-001 with no rebase.
- **task-003 (Tasks3 tab)** — shares the SCHEMA_VERSION 3 → 4 migration block. **Whichever task lands first writes the full block** described in § 1 (with both folderOfTasksList3 and the section/task fields). The second task's PR must verify the migration block in `RealmMigrations.java` matches this design and trim duplicate additions if needed.
- Recommended merge order: task-002 first (it owns the larger schema change), then task-003 (smaller diff, only adds palette + folder list pointer; migration block already in place).
- Both tasks are safe to **develop in parallel** in worktrees — only `RealmMigrations.java` and conceivably `RealmFoldersContainer.java` are shared. Conflict resolution is mechanical.

### 10. Files to create / modify (consolidated)

Create:
- `realmmodel/task/SectionObject.java`
- `realmcontrollers/taskcontroller/SectionsRealmController.java`
- `ui/fragment/task_section/small_tasks_fragment/AdapterItem.java`
- `ui/dialog/section_dialog/SectionEditDialog.java`
- `res/layout/dialog_section_edit.xml`
- `res/layout/section_header_card_view.xml`
- `res/values/strings.xml` keys: `add_section`, `section_name_hint`, `section_collapsed_default`, `delete_section_confirm`

Modify:
- `RealmMigrations.java` (SCHEMA_VERSION = 4, migration block)
- `realmmodel/task/TaskObject.java` (add `sectionId`, `position` + accessors)
- `realmcontrollers/taskcontroller/TasksRealmController.java` (sort by position; addTask assigns position; deprecate changeOrder)
- `ui/fragment/task_section/small_tasks_fragment/TasksRecyclerViewAdapter.java` (multi-view-type, items list, palette for header)
- `ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java` (flatten on refresh, FragmentResultListener for section dialog)
- `ui/fragment/task_section/small_tasks_fragment/ItemTouchHelperAttacher.java` (per § 5)
- `ui/fragment/task_section/folder_panel_sliding_fragment/fragment/FolderSlidingPanelFragment.java` (add overflow menu item per § 6)

### 11. Risks

1. **Sort order regression**: existing `getTasks(folderId)` sorts by `done ASC`, putting done tasks last (used in the "Done X tasks" footer flow). After switch to `position ASC`, every caller must be audited:
   - `TasksRecyclerViewAdapter` — re-flattens in the fragment; OK.
   - `JsonSyncUtil`, `LocalSyncUtil` — likely iterate tasks for export; order shouldn't matter functionally but verify backup round-trip.
   - `getDoneTasks(folderId)`, `getNotDoneTasks(folderId)` — still filter by `done`; need to add explicit `.sort("position", ASC)` to preserve drag order in the footer expansion.
   - `TaskActionModeCallback` and other multi-select sites — accessed by adapter index; OK once adapter is correct.
2. **Position drift from external mutations** (sync import, restore from backup): backups created before SCHEMA_VERSION 4 won't carry `position` / `sectionId`. The migration backfill covers existing DB; for **JSON sync imports** in `JsonSyncUtil`, ensure imported tasks get sequential positions stamped at insert time (extend the import loop to call the same nextPosition logic).
3. **Drag UX gotcha on collapsed sections**: dropping a task on a collapsed section header expands it (§ 5c edge case). If a user drags a section across a collapsed section, no expansion happens — they may "lose" the collapsed section's children visually, but data is intact. Acceptable per R9.
4. **`changeOrder` is called inline from `ItemTouchHelperAttacher` today**: callsite is rewritten in § 5; if any other code (sync? action mode?) calls it, those callers need a parallel update.
5. **Realm RealmList vs explicit position field**: we now have two sources of truth for ordering — the existing `FolderTaskObject.folderTasks` RealmList **index** and the new `TaskObject.position` field. Decision: keep RealmList as the storage (deletion uses it), but ordering for display is driven exclusively by `position`. Adds/removes must keep both in sync; the migration backfill aligns them initially. Document this invariant in `SectionsRealmController` Javadoc.
6. **`name` REQUIRED on existing rows**: irrelevant — SectionObject is new (no pre-existing rows in v3 DB).

### 12. Design clarifications (Phase 3 user feedback — 2026-05-17)

- **Sort within section** → **финал: undone сортируется по `position` ASC (drag-order), done всегда внизу секции.** Уточняет R4. Реализация: при flatten каждой секции отсортировать `tasks.where().equalTo("sectionId", id).sort("done", ASC, "position", ASC)`. Аналогично для задач без секции (`sectionId=0`). Risk 1 audit остаётся в силе: `getDoneTasks` / `getNotDoneTasks` callers получают `.sort("position", ASC)`.
- **Drop на свёрнутую секцию** → **финал: при hover/drop на header свёрнутой секции — авто-expand.** Реализация в `ItemTouchHelperAttacher`/`SectionDragController`:
  - При `onChildDraw` отслеживать, что target = collapsed section header.
  - После N мс hover (≈400 мс) → `setCurrentlyCollapsed(section, false)` (persist), `notifyItemRangeInserted` для её задач, перерисовка.
  - При drop на header (если ещё свёрнут) — раскрыть до выполнения move.
  - Section-as-block drag по свёрнутому header'у — отдельный кейс: блок состоит только из самого header'а (задачи скрыты, но persist'ятся как принадлежащие секции), переносим только header в Realm-позиции; список содержимого следует за ним автоматически после следующего flatten.
- Updated **Risk 3**: больше не "пользователь не видит результата" — теперь видит, секция раскрывается под курсором.

## Tests
_skip (manual QA)_

## Implementation
_TBD фаза 5_

## Review
_TBD фаза 6_
