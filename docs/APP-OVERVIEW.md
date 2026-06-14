# App Overview — todo100android (current state)

> Living description of the app **as it actually works now**, after the Jetpack Compose
> migration and the subsequent UI rework. For the historical migration plan see
> [COMPOSE-MIGRATION-PLAN.md](COMPOSE-MIGRATION-PLAN.md); the pre-Compose snapshot is in
> [PROJECT-INFO.md](PROJECT-INFO.md); the `specs/` folder documents the **legacy** Fragment
> UI (kept for reference). This file is the source of truth for present-day behaviour.

---

## 1. Functionality

A points-based to-do app organised as **4 tabs (task groups)**, each holding **categories
(folders)**, each holding **tasks** optionally grouped into **sections**.

### Tabs (groups 0–3)
- Default titles **Tasks 1 / Tasks 2 / Tasks 3 / Notes**; group 3 is the "Notes" tab
  (no checkboxes / points — items are plain notes).
- Titles are **renameable** (overflow menu → "Переименовать таб"); persisted in
  SharedPreferences (`data/TabNames.kt`), not Realm.
- Switch tabs by swiping left/right (a `HorizontalPager` in `MainScreen`).
- The top bar shows the tab title, the global **day-score** number, and a `⋮` overflow menu
  (**Синхронизация** / **Переименовать таб**).

### Category list (per tab)
- Each category is a white card showing its name + a points counter `done/all`
  (sum of `countAccumulation·countValue` over `sum of maxAccumulation·countValue`).
- **Add category**, and **drag-and-drop reorder** (long-press a card to drag); the order is
  persisted by rearranging the per-group `RealmList<FolderTaskObject>`.
- Tap a card → full-screen **category detail**.

### Category detail
- A `HorizontalPager` over the tab's categories — swipe to move between categories; opens at
  the tapped one. Top bar: back, category name, folder `⋮` menu (add section / rename / move
  to another tab / delete).
- Body: the category's **tasks**, optionally under **section** headers, scrolling above a
  pinned bottom **add-task panel**.

### Tasks
- Rendered as **white cards**. Layout: text on the left; then `points accum/max`
  (e.g. `2 1/5` = 2 points each, done 1 of 5); then the **checkbox on the right edge**.
- A leading `!`/`!!`/`!!!` marks priority.
- **Checkbox colour**: accent border when the task is **cyclic**, grey otherwise; filled
  accent when done.
- **Multi-category strip**: a task that belongs to more than one category shows a small strip
  under its checkbox — accent if cyclic, grey if not.
- **Section vs free tasks**: tasks inside a section have their *card* shifted right a little;
  free (outer-space) tasks sit flush left.
- **Tap** a task → full-screen task editor. **Long-press** → drag to reorder + selects it for
  the action bar (delete).

### Sections
- Collapsible headers (tap to expand/collapse) with a points counter on the right
  (sum done / sum all of the section's tasks).
- **Long-press a section** → action bar with **delete** and a **default-collapse toggle**
  (`UnfoldMore`/`UnfoldLess`): whether the section starts expanded or collapsed *when the app
  opens* (`collapsedByDefault`; applied by the per-session reset, does not change the current
  state).
- An empty expanded section shows a **"Перетащите сюда"** placeholder so a task can be dragged
  into it.
- Drag-reorder moves tasks within/between sections and the free zone; section headers can also
  be dragged to reorder.

### Done tasks
- Hidden by default. A single **"Выполнено: N"** footer at the end of the list toggles them
  (one footer per list, not per section).

### Task editor (full-width dialog)
- Text field (multi-line for Notes); **points (×) / repeats (/) / cyclicity (↻)** chips
  (no priority chip here); a **Категории** list with checkboxes that **save instantly** on
  every toggle. "Готово" / "Удалить".

### Add-task panel (bottom band)
- A slightly **darker shade** band (`surfaceMuted`). Compact controls: `×N` points, `/N`
  repeats (both cycle **1…10**), `↻` cyclicity; a filled **accent `+`** button; a full-width
  cream input (`maxLines = 7`). Unselected chips use the **tab's own colour**, accent when on.

### Other
- **Day-score**: global points accumulated today, shown in the top bar.
- **Daily reset**: cyclic tasks not completed today are reset (`TasksRepository.runDailyResetIfNeeded`).
- **Backup / sync** (`SyncDialog` / `SyncManager`): JSON file export/import + Firebase.
- **Legacy reference app**: the pre-migration Fragment UI is shipped as a *separate* app
  **"100 OLD"** (`applicationId …legacy`, its own empty Realm) — see [compose memory] and
  COMPOSE-MIGRATION-PLAN.md. Data is not shared with the current app.

---

## 2. Architecture

Compose UI on top of the **existing Java Realm data layer** (schema **v5, untouched**).

```
ComposeHostActivity (launcher)
  └─ MainScreen()                       // Todo100Theme + HorizontalPager over 4 groups
       ├─ TasksScreen(group)            // category list (reorderable) + overflow menu + day-score
       └─ CategoryDetailScreen(group)   // pager over a group's folders
            └─ FolderTasksPage(folder)  // sections + tasks (reorderable) + done-footer + add panel
```

### UI layer (`ui/compose/`)
- `MainScreen.kt` — two-level shell: group pager (category lists) → full-screen category
  detail. Owns the top bar, the contextual action bar (CAB), tab-rename dialog, and the
  reactive day-score.
- `TasksScreen.kt` — the category list, the category detail pager, `FolderTasksPage`, all task
  / section / editor / add-panel composables, the reorder resolver, and shared dialogs.
- `TasksViewModel.kt` — one instance per group (keyed `tasks-<group>`); exposes a
  `StateFlow<GroupUiState>` and forwards user actions to the repository. No Dagger graph in
  source — uses a plain `viewModel(factory = …)`.
- `ui/theme/` — `Todo100Theme`, `TabPalette`, `paletteForGroup`.

### Data interop (`data/`)
- `Dtos.kt` — immutable **detached** snapshots: `TaskDto`, `SectionDto`, `FolderDto`,
  `FolderRef`, `GroupUiState`, `ReorderEntry`. Invariant: no managed Realm object crosses into
  composition.
- `TasksRepository.kt` — the bridge. **Reads**: `groupFlow(group)` / `dayScoreFlow()` are
  `callbackFlow`s driven by a single `RealmChangeListener` on the process-wide UI-thread Realm;
  every committed transaction re-reads and re-emits detached DTOs. **Writes**: delegate
  straight to the static Java controllers (synchronous main-thread transactions); the change
  listener then drives the next emission.
- `TabNames.kt` — SharedPreferences-backed tab titles.

### Java domain layer (kept from the legacy app)
- `realmcontrollers/taskcontroller/` — `FolderTaskRealmController` (folders + group lists +
  `reorderFolders`), `TasksRealmController` (tasks), `SectionsRealmController` (sections,
  position model, `rearrangeOuterSpace` / `rearrangeTasksInContainer` for drag persistence).
- `realmmodel/` — `FolderTaskObject`, `TaskObject`, `SectionObject`, `ReportObject` (Report
  screen dropped, but the model stays so old data/backups keep working). Realm schema **v5**.
- `App`, `RealmDb`, `FileWritter`, `LocalSyncUtil`, Firebase sync.

### Reorder persistence (the tricky part)
- The visible rows are resolved (`resolveReorder`) into an **outer order** (section headers +
  free tasks) and a **per-section inner order**, then persisted via the controller's
  `rearrangeOuterSpace` / `rearrangeTasksInContainer`. Only the **dragged** task may change
  container; other tasks keep their `sectionId`, so unrelated free/done tasks aren't absorbed.
- **Gotcha**: `combinedClickable(onLongClick=…)` and `longPressDraggableHandle` both grab the
  long-press and conflict — the drag handle is the *sole* long-press owner; a plain `clickable`
  handles tap-to-edit.

### Stack
Kotlin 2.0.21, Compose BOM 2024.12.01 + Material 3, `sh.calvin.reorderable:2.4.3`, Realm
(Java) schema v5. **Build with JDK 21** (`./gradlew :app:assembleDebug`); the default JDK 25
breaks Gradle.

---

## 3. Themes

One **`TabPalette`** per tab (`ui/theme/TabPalette.kt`), selected by `paletteForGroup(group)`
and provided via `Todo100Theme`. Tokens: `bg`, `surface`, `surfaceMuted`, `text`, `textSoft`,
`inputText`, `counter`, `accent`, `divider`, `systemBar`, `darkSystemIcons`.

Token roles:
- `bg` — tab background (also the unselected add-panel chip colour).
- `surface` — white/cream cards, the editor, the add-input.
- `surfaceMuted` — the slightly-darker bottom add-panel band.
- `text` — on-`bg` text (top bar, category cards, section headers).
- `inputText` — on-`surface` text (task cards, editor).
- `accent` — `+` button, active chips, checked checkbox, cyclic accents, day-score.
- `systemBar` — status/nav bar tint; `darkSystemIcons` set for light backgrounds.

| Group | Name | bg | surface | surfaceMuted | accent | dark icons |
|------|------|------|---------|--------------|--------|------------|
| 0 | Default (green) | `#599C74` | `#ECF5EF` | `#4F8A68` | `#E47C5D` | no |
| 1 | Cornflower (blue) | `#5C7CC0` | `#EEF1F8` | `#5274B7` | `#E8B85C` | no |
| 2 | Canary (yellow) | `#F3C551` | `#F6F0E5` | `#EAB63E` | `#DF5C55` | yes |
| 3 | Indigo | `#3D52A0` | `#ECEEF8` | `#34468F` | `#F4A742` | no |

The Canary (yellow) palette was re-derived from a design reference: goldenrod background,
cream cards, coral-red accent.

> Detailed palette token history / legacy mapping lives in
> [specs/theme-palette.md](specs/theme-palette.md).
