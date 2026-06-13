# Data-Interop Spec — Java Realm → Kotlin Repository Layer

> **Phase 1** of the Compose migration. Wrap the existing Java Realm controllers in a thin
> Kotlin repository layer that emits **detached DTO snapshots** as `Flow`, so Compose never
> holds a live Realm object. Writes delegate to the existing controllers unchanged.
>
> **Hard invariants (from `_GAPS.md`):**
> - **G1** — no live Realm object (RealmObject / RealmResults / RealmList) ever crosses into
>   composition. Repos emit `copyFromRealm`-detached DTOs only.
> - **G2** — after a JSON restore replaces the container, repos re-query and re-emit so the
>   UI converges to the new state (last-good snapshot shown until then).
> - **Realm stays main-thread.** `allowQueriesOnUiThread` / `allowWritesOnUiThread` remain on.
>   **Schema version is NOT bumped** (stays at `SCHEMA_VERSION = 5`).
> - Java controllers + models **stay**. Kotlin is added alongside; nothing is deleted this phase.

---

## 1. Current data layer

### 1.1 Realm bootstrap & threading (`App.java`)

- `App extends Application`. `onCreate()` runs `Realm.init(this)` then sets a single default
  `RealmConfiguration`:
  - `.schemaVersion(RealmMigrations.SCHEMA_VERSION)` → **5**
  - `.migration(new RealmMigrations())`
  - `.allowWritesOnUiThread(true)`
  - `.allowQueriesOnUiThread(true)`
- **One process-lifetime UI-thread Realm.** `App.realm` is a `public static Realm`, opened
  once by `initRealm()` (`if (realm == null) realm = Realm.getDefaultInstance();`) and
  **never closed**. The static `RealmList` references (`realmFoldersContainer`,
  `folderOfNotesContainerList`) stay valid only while this instance is open — re-acquiring
  would leak a ref-counted instance. **Implication for repos:** use this same Realm; do not
  open/close your own instance, and stay on the main thread.
- Deliberate debt (documented in `App.java`): the whole app reads/writes Realm on the main
  thread. The UI-thread flags suppress Realm's thread guards. A large query/write can block
  the UI. Repos do **not** fix this in Phase 1 (would crash existing call sites); they keep
  reads on the main thread but hand Compose detached snapshots.

### 1.2 Root singleton — `RealmFoldersContainer`

- `App.realmFoldersContainer` is the single root object. Obtained/created in
  `initContainers()` inside a transaction:
  - if `FolderTaskRealmController.containerOfFolderIsExist()` is false →
    `realm.createObject(RealmFoldersContainer.class)`
  - else → `realm.where(RealmFoldersContainer.class).findFirst()`
- `rebindContainers()` re-points the statics after a restore replaces the container
  (`realmFoldersContainer = where(...).findFirst()`; `folderOfNotesContainerList = …folderOfNotesList`).
  **This is the G2 hook** — repos must re-emit after this runs.
- `setDayScopeValue()` recomputes `App.dayScope` (today's accumulated points) from
  `TasksRealmController.getDoneAndPartiallyDoneTasks()` + each task's `dateCountAccumulation`.

### 1.3 Shared access helper — `RealmDb.java`

Single entry point all controllers use:

| Method | Behavior |
|---|---|
| `Realm realm()` | `App.initRealm()` then returns `App.realm`. |
| `void write(Runnable body)` | Runs `body` in a transaction; **reuses surrounding transaction if already inside** (Realm throws on nested `executeTransaction`). |
| `void write(Realm.Transaction body)` | Same, body gets the Realm handle. |
| `<T extends RealmModel> T findById(Class<T>, long id)` | `where(type).equalTo("id", id).findFirst()` (note: queries field literally named `"id"`). |
| `<T extends RealmModel> long newUniqueId(Class<T>)` | `System.currentTimeMillis()`, bumped past collisions via `findById`. |

**Transaction boundary rule:** every controller write goes through `RealmDb.write`, which is
re-entrant. Repos must **not** wrap controller writes in their own transaction.

### 1.4 Controller method inventory (real signatures)

All methods are `static`. Reads return **live** Realm types unless noted.

#### `ContainersRealmController`
| Method | Reads / Writes | Transaction |
|---|---|---|
| `static void deleteFromRealmAllContainers()` | Iterates every `RealmFoldersContainer`; deletes all notes (via `FolderNotesRealmController.delFolderNote`), all task folders (`FolderTaskRealmController.deleteFolder`), all reports (`ReportRealmController.delReport`); then `where(RealmFoldersContainer).findAll().deleteAllFromRealm()`. | Each delegate opens its own `RealmDb.write`; the final `deleteAllFromRealm()` is **not** wrapped in a transaction in this method (runs against live results). Used by restore. |

#### `FolderTaskRealmController`
| Method | Reads / Writes | Transaction |
|---|---|---|
| `static RealmList<FolderTaskObject> getFoldersList()` | tab 0 folders (legacy → `getFoldersList(0)`). | read |
| `static RealmList<FolderTaskObject> getFoldersList(int group)` | `App.realmFoldersContainer.tasksListForGroup(group)` (group 0..3). | read |
| `static int getFolderGroup(FolderTaskObject folder)` | which group (0..3) holds the folder; `-1` if none. | read |
| `static java.util.List<FolderTaskObject> getAllFolders()` | all folders across all 4 groups in group order. | read |
| `static FolderTaskObject getFolder(long listId)` | `RealmDb.findById(FolderTaskObject, listId)`. | read |
| `static long addFolder(String name, boolean isDaily)` | legacy → group 0. Returns new id. | write |
| `static long addFolder(String name, boolean isDaily, int group)` | creates `FolderTaskObject`, sets id/name/daily, appends to `getFoldersList(group)`. Returns id. | `RealmDb.write` |
| `static long editFolder(FolderTaskObject folder, String name, boolean isDaily)` | sets name + daily. Returns id. | `RealmDb.write` |
| `static long editFolder(long id, String name, boolean isDaily)` | resolves folder by id then edits. | `RealmDb.write` |
| `static void moveFolderToGroup(FolderTaskObject folder, int targetGroup)` | removes from all groups, appends to target. | `RealmDb.write` |
| `static void deleteFolder(FolderTaskObject folderObject)` | for each task: detach-or-delete (survives if it has other categories); removes folder from all groups; `deleteFromRealm`. | `RealmDb.write` |
| `static void deleteFolder(long idList)` | resolves then deletes. | `RealmDb.write` |
| `static boolean folderIsExist(FolderTaskObject list)` | `list.isValid()`. | read |
| `static boolean folderIsExist(long idList)` | folder != null && valid. | read |
| `static boolean listOfFolderIsEmpty()` | `where(FolderTaskObject).findAll().isEmpty()`. | read |
| `static boolean containerOfFolderIsExist()` | `where(RealmFoldersContainer).findFirst() != null`. | read |
| `static long getIdForNextValue()` (pkg-private) | `RealmDb.newUniqueId(FolderTaskObject)`. | read |

Private helpers (must run inside a transaction): `removeFromAllGroups(folder)`,
`detachOrDeleteTaskFromFolder(task, folderId)` — the multi-category detach logic.

#### `TasksRealmController`
| Method | Reads / Writes | Transaction |
|---|---|---|
| `static List<TaskObject> getTasks()` | all tasks, sort `done ASC, id ASC`. | read |
| `static List<TaskObject> getNotDoneTasks()` | `done=false`, sort `done ASC, id ASC`. | read |
| `static List<TaskObject> getDoneTasks()` | `done=true`, sort `done ASC, id ASC`. | read |
| `static List<TaskObject> getDoneAndPartiallyDoneTasks()` | `countAccumulation != 0`, sort `done ASC, id ASC`. | read |
| `static RealmResults<TaskObject> getTasks(long folderId)` | folder's `folderTasks` sorted `done ASC, position ASC` (drag order). | read |
| `static List<TaskObject> getNotDoneTasks(long folderId)` | folder tasks `done=false`, sort `position ASC`. | read |
| `static List<TaskObject> getDoneTasks(long folderId)` | folder tasks `done=true`, sort `position ASC`. | read |
| `static List<TaskObject> getDoneAndPartiallyDoneTasks(long folderId)` | `taskFolderId=folderId & countAccumulation!=0`, sort `done ASC, id ASC`. | read |
| `static TaskObject getTask(long idTask)` | `findById(TaskObject, idTask)`. | read |
| `static void addTask(String text, int count, int maxAccumulation, boolean cycling, int priority, long taskFolderId)` | creates task, stamps `sectionId=0` + `position = nextOuterPosition(folderId)`, appends to folder's `folderTasks`. | `RealmDb.write` |
| `static void editTask(TaskObject task, String text, int count, int maxAccumulation, boolean cycling, int priority)` | updates fields (text only if non-empty). | `RealmDb.write` |
| `static void setTaskDoneOrParticullaryDone(TaskObject task, boolean done)` | toggles done; on done stamps today into `dateCountAccumulation` + `lastDoneDate`, marks done only when `countAccumulation >= maxAccumulation`; on undone clears accumulation. | `RealmDb.write` |
| `static void deleteTask(TaskObject task)` | removes task from every folder referencing it (primary + extras), clears accumulation/extras, `deleteFromRealm`; logs result. | `RealmDb.write` |
| `static void deleteTask(long id)` | resolves then deletes. | `RealmDb.write` |
| `@Deprecated static void changeOrder(long folderId, TaskObject target, TaskObject targetPosition)` | legacy RealmList reorder. **Not used by repos** (superseded by `SectionsRealmController.reorderItems`). | direct list mutation |
| `static RealmList<TaskObject> getFolderTasksRealmListFromFolder(long folderId)` | `findById(FolderTaskObject, folderId).folderTasks`. | read |
| `static List<Long> getCategoryIds(TaskObject task)` | primary first, then deduped extras. Never empty. | read |
| `static void setTaskCategories(TaskObject task, List<Long> folderIds)` | first id → primary `taskFolderId`, rest → `extraFolderIds`; syncs folder membership lists. | `RealmDb.write` |
| `static void setTaskPriority(TaskObject taskObject, int priority)` | sets priority if 0..3. | `RealmDb.write` |
| `private static long getIdForNextValue()` | `RealmDb.newUniqueId(TaskObject)`. | read |

#### `SectionsRealmController` (final, all static)
| Method | Reads / Writes | Transaction |
|---|---|---|
| `static RealmResults<SectionObject> getSections(long folderId)` | `parentFolderId=folderId`, sort `position ASC`. | read |
| `static SectionObject getSection(long sectionId)` | `findById(SectionObject, sectionId)`. | read |
| `static SectionObject addSection(long folderId, String name, boolean collapsedByDefault, int position)` | validates name 1..40; creates section (PK id), then `compactPositions(folderId)`. Returns managed object. | `RealmDb.write` + follow-up `compactPositions` |
| `static void editSection(SectionObject s, String name, boolean collapsedByDefault)` | validates 1..40; updates name + default. | `RealmDb.write` |
| `static void deleteSection(SectionObject s)` | tasks in section become free (`sectionId=0`); deletes header; `compactPositions`. | `RealmDb.write` + `compactPositions` |
| `static void setCurrentlyCollapsed(SectionObject s, boolean collapsed)` | sets transient collapse state. | `RealmDb.write` |
| `static void resetAllCollapseStates()` | resets every section's `currentlyCollapsed` to `collapsedByDefault`. App-start call. | `RealmDb.write` |
| `static void moveTaskToSection(TaskObject task, long newSectionId, int newPosition)` | sets sectionId + position; `compactPositions(task.taskFolderId)`. | `RealmDb.write` + `compactPositions` |
| `static void reorderItems(long folderId, List<ItemMove> moves)` | applies drag moves (SECTION/TASK), then `compactPositions`. | `RealmDb.write` + `compactPositions` |
| `static void rearrangeTasksInContainer(long folderId, long sectionId, List<Long> orderedTaskIds)` | restamps positions 0..N within one container; `compactPositions`. | `RealmDb.write` + `compactPositions` |
| `static void rearrangeOuterSpace(long folderId, List<ItemMove> orderedEntries)` | outer-space restamp; tasks forced to `sectionId=0`; `compactPositions`. | `RealmDb.write` + `compactPositions` |
| `static void compactPositions(long folderId)` | re-stamps contiguous 0..N-1: outer space (sections + free tasks interleaved by position) and each inner section (sort `done ASC, position ASC`). | `RealmDb.write` |
| `static int nextOuterPosition(long folderId)` | `max(maxSectionPos, maxFreeTaskPos) + 1`. | read |
| `private static long getIdForNextValue()` | `RealmDb.newUniqueId(SectionObject)`. | read |

Nested: `SectionsRealmController.ItemMove` — public final class with
`enum Kind { SECTION, TASK }`, fields `Kind kind`, `long id`, `int newPosition`,
`long newSectionId` (`-1` = unchanged). Ctor `ItemMove(Kind, long id, int newPosition, long newSectionId)`.

#### `FolderNotesRealmController`
| Method | Reads / Writes | Transaction |
|---|---|---|
| `static FolderNotesObject getFolderNote(long id)` | `findById(FolderNotesObject, id)`. | read |
| `static long addFolderNote(String name)` | creates folder, sets id/name, appends to `App.folderOfNotesContainerList`. Returns id. | `RealmDb.write` |
| `static void editFolderNote(long id, String name)` | sets name. | `RealmDb.write` |
| `static void delFolderNote(long id)` | deletes all notes in folder + the folder. | `RealmDb.write` |
| `static void reorderFolderNote(int from, int to)` | reorders `App.folderOfNotesContainerList`. | `RealmDb.write` |
| `static long getNewValidFolderNotesId()` | `newUniqueId(FolderNotesObject)`. | read |
| `static RealmList<NoteObject> getNotesList(long idFolderNotesObject)` | folder's notes (`getTasks()`). | read |
| `static NoteObject getNote(long idNotesObject)` | `findById(NoteObject, idNotesObject)`. | read |
| `static long addNote(long idFolderNotesObject, String text)` | creates note (id/text/idFolder), appends to folder. Returns id. | `RealmDb.write` |
| `static void editNote(long idNotesObject, String text)` | sets text. | `RealmDb.write` |
| `static void delNote(long idNotesObject)` | removes note from its folder + `deleteFromRealm`. | `RealmDb.write` |
| `static void reorderNote(long idFolderNotesObject, long idNotesObject, int from, int to)` | reorders the folder's notes list. | `RealmDb.write` |
| `static long getNewValidNotesId()` | `newUniqueId(NoteObject)`. | read |

#### `ReportRealmController` (data kept, screen dropped per migration plan)
| Method | Reads / Writes | Transaction |
|---|---|---|
| `static List<ReportObject> getReportList()` | `App.realmFoldersContainer.reportObjectList`. | read |
| `static ReportObject getReport(long id)` | `findById(ReportObject, id)`. | read |
| `static long addReport(String date, int dayCount, String textReport, int soulRating, int healthRating, int phinanceRating, int englishRating, int socialRating, int famillyRating, boolean isWeekReport, int weekNumber)` | creates report with all ratings, appends to `reportObjectList`. Returns id. | `RealmDb.write` |
| `static void editReport(long id, String date, int dayCount, String textReport, int soulRating, int healthRating, int phinanceRating, int englishRating, int socialRating, int famillyRating, int weekNumber)` | updates fields (note: no `isWeekReport` param on edit). | `RealmDb.write` |
| `static void delReport(long id)` | removes from list + `deleteFromRealm`. | `RealmDb.write` |

> No Compose ViewModel/repo wraps Reports in Phase 1 (the screen is being dropped). The
> controller + model stay so old backups round-trip. A `ReportDto` is **not** required this
> phase; document only.

---

## 2. Model graph

### 2.1 Containment tree

```
RealmFoldersContainer (root singleton, no PK)
├─ folderOfTasksList   : RealmList<FolderTaskObject>   group 0 (Tasks1, default chrome)
├─ folderOfTasksList2  : RealmList<FolderTaskObject>   group 1 (Tasks2, Cornflower)  [schema v3]
├─ folderOfTasksList3  : RealmList<FolderTaskObject>   group 2 (Tasks3, Canary)      [schema v4]
├─ folderOfTasksList4  : RealmList<FolderTaskObject>   group 3 (Notes tab, Indigo)   [schema v5]
├─ folderOfNotesList   : RealmList<FolderNotesObject>
└─ reportObjectList    : RealmList<ReportObject>

FolderTaskObject
└─ folderTasks         : RealmList<TaskObject>

TaskObject
├─ dateCountAccumulation : RealmList<RealmInteger>
└─ extraFolderIds        : RealmList<Long>  (nullable, multi-category)

SectionObject            (standalone; linked to folder by parentFolderId, to tasks by sectionId)

FolderNotesObject
└─ notesObjectRealmList  : RealmList<NoteObject>   (accessor named getTasks())

ReportObject             (leaf, in reportObjectList)
```

`tasksListForGroup(int group)`: `0→list`, `1→list2`, `2→list3`, `3→list4`. The 4 numbered
fields stay separate for schema compatibility; callers address them by group (0..3).
`Tabs.GROUP_COUNT = 4`, `PAGE_COUNT = 5`, group N renders on pager page N+1; page 0 = old Notes.

### 2.2 Fields per model

**`RealmFoldersContainer`** (`implements Serializable`, **no PK, no indices**) — six RealmList
relations listed above. `tasksListForGroup(int)` helper.

**`FolderTaskObject`**
| Field | Type | Notes |
|---|---|---|
| `name` | String | |
| `id` | long | logical id (queried as `"id"`, **not** an annotated `@PrimaryKey`) |
| `folderTasks` | RealmList<TaskObject> | public field; accessor `getTasks()` |
| `isDaily` | boolean | public field; `isDaily()` / `setDaily()` |

**`TaskObject`** (no annotations; `id` is logical only)
| Field | Type | Notes |
|---|---|---|
| `id` | long | logical id |
| `text` | String | |
| `done` | boolean | |
| `taskFolderId` | long | **primary** category folder id |
| `priority` | int | 0..3 |
| `lastDoneDate` | int | encoded `DAY_OF_YEAR + YEAR` |
| `isCycling` | boolean | `isCycling()` / `setCycling()` |
| `countValue` | int | points per completion |
| `maxAccumulation` | int | cap on accumulation entries |
| `countAccumulation` | int | current count (== `dateCountAccumulation.size()`) |
| `dateCountAccumulation` | RealmList<RealmInteger> | per-completion date stamps |
| `extraFolderIds` | RealmList<Long> | **nullable** (absent in pre-multicategory backups) [schema v2] |
| `sectionId` | long | `0` = free (no section) [schema v4] |
| `position` | int | dense ordering index within folder [schema v4, backfilled] |

**`SectionObject`** (annotated)
| Field | Type | Notes |
|---|---|---|
| `id` | long | **`@PrimaryKey`** |
| `name` | String | **`@Required`** |
| `collapsedByDefault` | boolean | persisted preference |
| `currentlyCollapsed` | boolean | transient session state (reset at app start) |
| `parentFolderId` | long | **`@Index`** |
| `position` | int | dense ordering index |

**`RealmInteger`** — single field `myInteger : int`. Wraps a primitive int so it can live in a
`RealmList` (Realm-java cannot store `RealmList<int>`; primitive-list `Long` support came in
v2 for `extraFolderIds`, but `dateCountAccumulation` predates it and uses this wrapper).

**`FolderNotesObject`**
| Field | Type | Notes |
|---|---|---|
| `name` | String | |
| `id` | long | logical id |
| `notesObjectRealmList` | RealmList<NoteObject> | public field; accessor named **`getTasks()`** (legacy naming) |

**`NoteObject`**
| Field | Type |
|---|---|
| `id` | long |
| `idFolder` | long |
| `text` | String |

**`ReportObject`** (`implements IReportObject`) — `id:long`, `date:String`, `countOfDay:int`,
`reportText:String`, ratings `soulRating/healthRating/phinanceRating/englishRating/socialRating/famillyRating : int`,
`weekNumber:int`, `isWeekReport:boolean`. No annotations.

**`ITaskObject`** — empty marker interface (no members). `IReportObject` — `getId()` +
`setCountOfDay(int)` only.

### 2.3 `extraFolderIds` multi-category mechanism

A task belongs to **one primary folder** (`taskFolderId`) plus **zero or more extras**
(`extraFolderIds : RealmList<Long>`, nullable). Category set = `[taskFolderId] + extras`
(`getCategoryIds`, deduped, primary first, never empty). `setTaskCategories` makes the first
id primary, the rest extras, and syncs each `FolderTaskObject.folderTasks` membership.
Deleting a folder calls `detachOrDeleteTaskFromFolder`: if removing this folder empties the
category set the task is deleted; otherwise the first remaining id becomes primary and the
rest become extras. **G5**: `position` is per-task (per-primary-folder), not per-membership —
extras render in primary order; document as accepted (no schema change).

---

## 3. Detached DTOs (Kotlin, immutable)

Plain Kotlin `data class`es — only primitives / `String` / `List` / `Boolean`. **No Realm
types.** One file `data/dto/Dtos.kt` (or per-type). Mapping is field-for-field off the model;
do **not** expose `RealmInteger` or `RealmList` — flatten to `List<Int>` / `List<Long>`.

```kotlin
package com.shumidub.todoapprealm.data.dto

data class ContainerDto(
    val taskFoldersByGroup: Map<Int, List<FolderTaskDto>>, // 0..3 → folders in order
    val noteFolders: List<FolderNotesDto>,
)

data class FolderTaskDto(
    val id: Long,
    val name: String,
    val isDaily: Boolean,
    val group: Int,                 // 0..3, derived via getFolderGroup
    val tasks: List<TaskDto>,       // detached, in folderTasks order
)

data class SectionDto(
    val id: Long,
    val name: String,
    val collapsedByDefault: Boolean,
    val currentlyCollapsed: Boolean,
    val parentFolderId: Long,
    val position: Int,
)

data class TaskDto(
    val id: Long,
    val text: String,
    val done: Boolean,
    val taskFolderId: Long,         // primary category
    val priority: Int,
    val lastDoneDate: Int,
    val cycling: Boolean,           // from isCycling
    val countValue: Int,
    val maxAccumulation: Int,
    val countAccumulation: Int,
    val dateCountAccumulation: List<Int>,   // RealmList<RealmInteger> → List<Int> (myInteger)
    val extraFolderIds: List<Long>,         // null RealmList → emptyList()
    val sectionId: Long,            // 0 = free
    val position: Int,
) {
    /** Category set, primary first — mirrors TasksRealmController.getCategoryIds. */
    val categoryIds: List<Long>
        get() = (listOf(taskFolderId) + extraFolderIds).distinct()
}

data class FolderNotesDto(
    val id: Long,
    val name: String,
    val notes: List<NoteDto>,       // from notesObjectRealmList (getTasks())
)

data class NoteDto(
    val id: Long,
    val idFolder: Long,
    val text: String,
)
```

> `ReportDto` is intentionally omitted (screen dropped; data kept via the Java controller for
> backup round-trip). Add later only if a report view returns.

### 3.1 Mapping functions (Java model → DTO)

Live in `data/mapper/Mappers.kt`. **Always map from a `copyFromRealm` detached copy** (or read
each field eagerly while still in the read path) so the DTO holds no managed references.
`RealmInteger` → `it.myInteger`; null `RealmList<Long>` → `emptyList()`.

```kotlin
fun TaskObject.toDto() = TaskDto(
    id = id, text = text ?: "", done = isDone, taskFolderId = taskFolderId,
    priority = priority, lastDoneDate = lastDoneDate, cycling = isCycling,
    countValue = countValue, maxAccumulation = maxAccumulation,
    countAccumulation = countAccumulation,
    dateCountAccumulation = dateCountAccumulation?.map { it.myInteger } ?: emptyList(),
    extraFolderIds = extraFolderIds?.filterNotNull() ?: emptyList(),
    sectionId = sectionId, position = position,
)
```

---

## 4. Repository design

### 4.1 Pattern

- One repo per aggregate: `TaskRepository`, `FolderTaskRepository`, `SectionRepository`,
  `NotesRepository` (+ `ReportRepository` only if needed later).
- **Reads** = cold `Flow<List<Dto>>` built with `callbackFlow`:
  1. run the controller's read on the **main thread** to get the live `RealmResults`,
  2. emit the current snapshot via `realm.copyFromRealm(results)` → map to DTOs,
  3. register a `RealmResults.addChangeListener` that re-emits on every change,
  4. `awaitClose { results.removeAllChangeListeners() }`.
- **Writes** = thin pass-through to the existing static controller. No new transactions
  (controllers already use `RealmDb.write`). After a write the change-listener fires and the
  flow re-emits the next detached snapshot — satisfies **G1**.
- **G2 / restore**: expose a `refresh()` (or shared restore signal) that, after
  `App.rebindContainers()`, re-runs the query so the listener attaches to the new
  `RealmFoldersContainer` and re-emits. Container-level relations (`folderOfTasksList*`) need
  this because their backing list is replaced; a `RealmResults` query (e.g. tasks by folderId,
  sections by parentFolderId) auto-updates but should still be re-validated.

> **Threading:** the Realm + its results/listeners are bound to the **main (UI) thread Looper**
> — the same thread `App.realm` was opened on. `callbackFlow` collection must therefore run on
> the main dispatcher (`flowOn(Dispatchers.Main)` / collect from a `Main`-confined scope).
> `copyFromRealm` is what makes the emitted DTOs safe to read off-thread in Compose. Do **not**
> move the query/listener registration off the main thread (it would throw / never fire).

### 4.2 Concrete skeleton — `TaskRepository`

```kotlin
package com.shumidub.todoapprealm.data.repo

import com.shumidub.todoapprealm.App
import com.shumidub.todoapprealm.data.dto.TaskDto
import com.shumidub.todoapprealm.data.mapper.toDto
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.TasksRealmController
import com.shumidub.todoapprealm.realmcontrollers.taskcontroller.SectionsRealmController
import com.shumidub.todoapprealm.realmmodel.task.TaskObject
import io.realm.RealmResults
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TaskRepository {

    /** Detached, change-driven stream of one folder's tasks (done ASC, position ASC). */
    fun tasksForFolder(folderId: Long): Flow<List<TaskDto>> = callbackFlow {
        // 1. controller read → LIVE RealmResults (main thread, App.realm)
        val results: RealmResults<TaskObject> =
            TasksRealmController.getTasks(folderId)

        fun emitSnapshot(live: RealmResults<TaskObject>) {
            // 2. detach (G1) then map to DTO — never send live objects downstream
            val detached = App.realm.copyFromRealm(live)
            trySend(detached.map { it.toDto() })
        }

        // 3. initial emission + re-emit on every Realm change (covers writes & restore re-query)
        val listener = io.realm.RealmChangeListener<RealmResults<TaskObject>> { emitSnapshot(it) }
        results.addChangeListener(listener)
        emitSnapshot(results)

        // 4. cleanup
        awaitClose { results.removeAllChangeListeners() }
    }
    // collected on the main thread: .flowOn(Dispatchers.Main) at the call site / VM.

    // ---- Writes: delegate straight to the Java controller (no extra transaction) ----
    fun add(text: String, count: Int, maxAccumulation: Int, cycling: Boolean,
            priority: Int, folderId: Long) =
        TasksRealmController.addTask(text, count, maxAccumulation, cycling, priority, folderId)

    fun edit(taskId: Long, text: String, count: Int, maxAccumulation: Int,
             cycling: Boolean, priority: Int) {
        val t = TasksRealmController.getTask(taskId) ?: return
        TasksRealmController.editTask(t, text, count, maxAccumulation, cycling, priority)
    }

    fun setDone(taskId: Long, done: Boolean) {
        val t = TasksRealmController.getTask(taskId) ?: return
        TasksRealmController.setTaskDoneOrParticullaryDone(t, done)
    }

    fun delete(taskId: Long) = TasksRealmController.deleteTask(taskId)

    fun setPriority(taskId: Long, priority: Int) {
        val t = TasksRealmController.getTask(taskId) ?: return
        TasksRealmController.setTaskPriority(t, priority)
    }

    fun setCategories(taskId: Long, folderIds: List<Long>) {
        val t = TasksRealmController.getTask(taskId) ?: return
        TasksRealmController.setTaskCategories(t, folderIds)
    }
}
```

### 4.3 Repo method → controller method map

**`TaskRepository`**
| Repo | Controller |
|---|---|
| `tasksForFolder(folderId): Flow` | `getTasks(folderId)` + change listener |
| `add(...)` | `addTask(text, count, maxAccumulation, cycling, priority, folderId)` |
| `edit(...)` | `getTask` + `editTask(task, text, count, maxAccumulation, cycling, priority)` |
| `setDone(id, done)` | `getTask` + `setTaskDoneOrParticullaryDone` |
| `delete(id)` | `deleteTask(id)` |
| `setPriority(id, p)` | `getTask` + `setTaskPriority` |
| `setCategories(id, ids)` | `getTask` + `setTaskCategories` |

**`SectionRepository`**
| Repo | Controller |
|---|---|
| `sectionsForFolder(folderId): Flow<List<SectionDto>>` | `getSections(folderId)` + listener |
| `add(folderId, name, collapsedByDefault, position)` | `addSection` |
| `edit(sectionId, name, collapsedByDefault)` | `getSection` + `editSection` |
| `delete(sectionId)` | `getSection` + `deleteSection` |
| `setCurrentlyCollapsed(sectionId, collapsed)` | `getSection` + `setCurrentlyCollapsed` |
| `resetAllCollapseStates()` | `resetAllCollapseStates` |
| `moveTaskToSection(taskId, newSectionId, newPosition)` | `getTask` + `moveTaskToSection` |
| `reorderItems(folderId, moves)` | `reorderItems` |
| `rearrangeTasksInContainer(folderId, sectionId, ids)` | `rearrangeTasksInContainer` |
| `rearrangeOuterSpace(folderId, entries)` | `rearrangeOuterSpace` |
| `nextOuterPosition(folderId)` | `nextOuterPosition` |

**`FolderTaskRepository`**
| Repo | Controller |
|---|---|
| `foldersForGroup(group): Flow<List<FolderTaskDto>>` | `getFoldersList(group)` (RealmList) + listener; map each + nest tasks |
| `allFolders(): Flow<List<FolderTaskDto>>` | `getAllFolders()` |
| `add(name, isDaily, group)` | `addFolder(name, isDaily, group)` |
| `edit(id, name, isDaily)` | `editFolder(id, name, isDaily)` |
| `move(id, targetGroup)` | `getFolder` + `moveFolderToGroup` |
| `delete(id)` | `deleteFolder(id)` |
| `groupOf(id)` | `getFolder` + `getFolderGroup` |

> `RealmList` has no `addChangeListener` of its own in the same way as `RealmResults`; for the
> container lists, observe via `getFoldersList(group)` (a managed `RealmList` is observable in
> Realm-java 10.x via `addChangeListener(OrderedRealmCollectionChangeListener)`), or back the
> folder flow with a `where(FolderTaskObject)` `RealmResults` and filter by group using
> `getFolderGroup`. Prefer the `RealmResults`-backed flow for uniform change semantics.

**`NotesRepository`**
| Repo | Controller |
|---|---|
| `noteFolders(): Flow<List<FolderNotesDto>>` | observe `App.folderOfNotesContainerList` / `where(FolderNotesObject)` + listener |
| `notesForFolder(folderId): Flow<List<NoteDto>>` | `getNotesList(folderId)` + listener |
| `addFolder(name)` | `addFolderNote(name)` |
| `editFolder(id, name)` | `editFolderNote(id, name)` |
| `deleteFolder(id)` | `delFolderNote(id)` |
| `reorderFolder(from, to)` | `reorderFolderNote(from, to)` |
| `addNote(folderId, text)` | `addNote(folderId, text)` |
| `editNote(noteId, text)` | `editNote(noteId, text)` |
| `deleteNote(noteId)` | `delNote(noteId)` |
| `reorderNote(folderId, noteId, from, to)` | `reorderNote(...)` |

### 4.4 Restore (G2)

`MainActivity.refreshAfterRestore()` → `ContainersRealmController.deleteFromRealmAllContainers()`
+ reimport, then `App.rebindContainers()`. Repos expose a shared `restoreSignal` (e.g. a
`MutableSharedFlow<Unit>` in a small `RestoreCoordinator`) that the restore path emits to;
each flow re-runs its query on that signal and re-emits. Until the new snapshot lands the UI
keeps the last-good DTO list (G1 makes that safe — no invalid live objects in composition).
A "restoring" gate state (boolean) suspends list rendering during the swap.

---

## 5. Dagger wiring

**Current state:** `dagger:2.50` + `dagger-compiler:2.50` are on the classpath
(`app/build.gradle` lines 74–75), but **no Dagger graph exists in source** — zero `@Module`,
`@Component`, `@Inject`, `@Provides` across `app/src/main/java`. Controllers are static; `App`
does manual init. So Phase 1 adds a **minimal** graph.

### 5.1 Minimal additions

```kotlin
// data/di/RepositoryModule.kt
@Module
class RepositoryModule {
    @Provides @Singleton fun taskRepo() = TaskRepository()
    @Provides @Singleton fun folderTaskRepo() = FolderTaskRepository()
    @Provides @Singleton fun sectionRepo() = SectionRepository()
    @Provides @Singleton fun notesRepo() = NotesRepository()
}
```

ViewModel multibinding (so `viewModel(factory = …)` works in composables):

```kotlin
// data/di/ViewModelModule.kt
@MapKey @Retention(AnnotationRetention.RUNTIME)
annotation class ViewModelKey(val value: KClass<out ViewModel>)

@Module
abstract class ViewModelModule {
    @Binds @IntoMap @ViewModelKey(TasksViewModel::class)
    abstract fun tasksVm(vm: TasksViewModel): ViewModel
    // … one @Binds per ViewModel
}

// data/di/DaggerViewModelFactory.kt
@Singleton
class DaggerViewModelFactory @Inject constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return (creators[modelClass]
            ?: creators.entries.first { modelClass.isAssignableFrom(it.key) }.value)
            .get() as T
    }
}
```

```kotlin
// data/di/AppComponent.kt
@Singleton
@Component(modules = [RepositoryModule::class, ViewModelModule::class])
interface AppComponent {
    fun viewModelFactory(): DaggerViewModelFactory
}
```

ViewModels take repos via `@Inject constructor(...)`. `App.onCreate()` builds the component
(`DaggerAppComponent.create()`) and exposes it (`App.appComponent`). A composable obtains the
factory from the component and calls `viewModel(factory = App.appComponent.viewModelFactory())`.

> **Constraint preserved:** `realm-android` plugin is applied **before** the Kotlin plugins so
> Realm keeps its `annotationProcessor` path (Phase 0 decision). The new Kotlin/Dagger code
> must compile under that setup — Dagger's annotation processing already runs via
> `annotationProcessor 'dagger-compiler:2.50'`. If Kotlin code needs Dagger codegen, add
> `kapt`/`ksp` for the Kotlin sources **without** disturbing Realm's Java `annotationProcessor`
> (verify the build still produces the schema transform). Simplest path: keep Dagger glue Java
> or confirm kapt coexists.

---

## 6. Files to keep / convert / delete

**Keep (Java, untouched this phase):**
- Controllers: `RealmDb`, `ContainersRealmController`, `FolderTaskRealmController`,
  `SectionsRealmController`, `TasksRealmController`, `FolderNotesRealmController`,
  `ReportRealmController`.
- Models: `RealmFoldersContainer`, `RealmInteger`, `FolderTaskObject`, `TaskObject`,
  `SectionObject`, `ITaskObject`, `FolderNotesObject`, `NoteObject`, `ReportObject`,
  `IReportObject`.
- `RealmMigrations` (schema v5, unchanged), `App.java` (Realm bootstrap + static container refs),
  `Tabs.java`.

**New (Kotlin, added this phase):**
- DTOs: `data/dto/Dtos.kt` (`ContainerDto`, `FolderTaskDto`, `SectionDto`, `TaskDto`,
  `FolderNotesDto`, `NoteDto`).
- Mappers: `data/mapper/Mappers.kt`.
- Repos: `data/repo/{TaskRepository, FolderTaskRepository, SectionRepository, NotesRepository}.kt`.
- DI: `data/di/{RepositoryModule, ViewModelModule, DaggerViewModelFactory, AppComponent}.kt`.
- Restore coordination: `data/repo/RestoreCoordinator.kt` (shared restore signal for G2).

**Delete:** nothing this phase. (Reports screen, old fragments/adapters/XML, and 3 libraries
are removed in Phase 5 per the migration plan; `ReportObject` data stays regardless.)

---

## Verification checklist

- [ ] G1: no repo emits a `RealmObject`/`RealmResults`/`RealmList` — only DTOs (via `copyFromRealm`).
- [ ] G2: restore path re-emits; UI shows last-good until new snapshot; "restoring" gate added.
- [ ] Realm stays main-thread; `App.realm` reused (no new `getDefaultInstance`); listeners removed in `awaitClose`.
- [ ] Schema version unchanged (5); no model edits.
- [ ] Every write repo method maps to an existing controller method (§4.3) with no new transaction.
- [ ] Dagger graph builds; `viewModel(factory = …)` resolves each VM via multibinding.
