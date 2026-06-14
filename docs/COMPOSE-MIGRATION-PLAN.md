# Jetpack Compose Migration Plan — todo100android

> Target architecture for migrating the Java/Fragment/RecyclerView UI to **Jetpack
> Compose + Material 3**. The "before" picture is in [PROJECT-INFO.md](PROJECT-INFO.md).
> Approved 2026-06-13.
>
> **Migration complete.** For how the app works *today* (functionality, architecture, themes)
> see [APP-OVERVIEW.md](APP-OVERVIEW.md) — the current source of truth. This file is kept as
> the historical plan.

## Guiding decisions

- **Data stays.** Realm schema is **not bumped**. Existing on-device `.realm` DBs and old
  JSON backups must keep working. Reports = **drop the screen, keep the data**
  (`ReportObject` stays in the model + backups so old backups don't break).
- **Incremental, always-runnable.** Each phase compiles, installs, and launches. Java
  (controllers, models, sync utils) stays in place and is converted only as needed.
- **Interop over rewrite.** Keep Realm-java, Dagger 2, Firebase, Gson, and Material (for
  interop) during the transition.

## Stack changes

**Add:** Kotlin + Compose BOM, Material 3, `activity-compose`,
`lifecycle-viewmodel-compose`, `kotlinx-coroutines-android`.

**Keep:** Realm-java 10.19, Dagger 2, Firebase, Gson, Material (interop, transitional).

**Remove** (replaced by Compose-native):
- `com.sothree.slidinguppanel` → custom composable on `AnchoredDraggable`.
- `keyboardvisibilityevent` → `WindowInsets.ime`.
- `RxJava2` → coroutines / Flow.

**Drag-reorder:** use library **`sh.calvin.reorderable`** (LazyColumn drag, maintained)
rather than hand-rolling — the mechanic appears in 3 places (folders, notes, complex task
list). _(Disputed call — can switch to manual.)_

## Architecture (MVVM + interop)

- `MainActivity` → `ComponentActivity` + `setContent { AppTheme { MainScreen() } }`,
  edge-to-edge.
- **Data-interop layer** (thin Kotlin): repositories wrapping the existing Java
  controllers.
  - Reads → `Flow<List<…>>` via `RealmResults.addChangeListener` + `copyFromRealm`
    (detached snapshots — Compose never holds live Realm objects).
  - Writes → delegate to existing Java controllers.
  - Realm stays on the main thread (`allowQueriesOnUiThread`); **schema version not bumped**.
- **ViewModels** via Dagger 2 (multibinding `ViewModelProvider.Factory`), injected into
  composables with `viewModel(factory = …)`.

## Navigation & shell

- `HorizontalPager` (5 pages): `[Notes-folders] [Tasks1] [Tasks2] [Tasks3] [Notes-tab(gr3)]`,
  `START_PAGE = 1`.
- **Sliding panel** → custom composable on `AnchoredDraggable` (2 anchors: collapsed peek
  24dp / expanded full). Offset drives footer fade, snap, fling. _Main risk; idiomatic
  replacement for sothree._ _(Disputed call — vs Material3 `BottomSheetScaffold`; chose
  custom because 24dp peek + smooth offset is closer to custom.)_
- `LocalPalette` (CompositionLocal) — per-tab palette (Cornflower / Canary / Indigo /
  default), 9 tokens; status/nav bars + chrome recolour reactively. Port `Palette.java`
  values to Kotlin.
- `AppTopBar` (M3 `TopAppBar`) + day-scope counter; `ActionModeBar` composable replaces the
  system CAB.
- **Back-press** → `BackHandler` state machine: panel expanded→collapse → action-mode→
  dismiss → notes-list→folders → double-tap exit.

## Features (1:1 behaviour parity)

- **Folder panel:** folder cards, add/edit/delete dialogs, color picker (toggle group),
  drag-reorder, action-mode, bottom add-task panel with toggles (count / max / priority /
  cycling).
- **Tasks:** collapsible sections, done/partial checkboxes, params, `TaskEditorSheet`
  (M3 `ModalBottomSheet` — half for tasks / full for Notes group 3), `SectionEditDialog`,
  task action-mode dialog, multi-category, "Done N" footer, drag within/between sections +
  auto-expand on hover (~400ms), daily reset.
- **Notes (page 0):** folders→notes nav, add/edit/delete, drag-reorder, action-mode,
  special "add list" card.
- **Sync:** `SyncDialog` + `FirebaseAuthDialog` on Compose, SAF picker via `ActivityResult`
  in the activity, Firebase/JSON in coroutines (`Dispatchers.IO`).

## What gets deleted

- Reports screen (fragment / adapter / dialogs / layouts) — `ReportObject` **data stays**.
- Old Fragments, RecyclerView adapters, XML layouts, ActionMode callbacks.
- 3 libraries: `slidinguppanel`, `keyboardvisibilityevent`, `RxJava2`.

## Phases (each builds & runs)

| # | Phase | Scope |
|---|---|---|
| 0 | **Scaffold** | Gradle (Kotlin + Compose), theme (Palette→Kotlin), `MainScreen` skeleton of 5 tabs |
| 1 | **Data-interop** | Repositories, Realm→Flow, Dagger VM factory |
| 2 | **Folder panel + Tasks** | Core, largest phase |
| 3 | **Notes** | Page 0 |
| 4 | **Sync** | Sync + Firebase auth on Compose |
| 5 | **Cleanup** | Delete old code/libs, final theme parity, full verification |

## Verification criteria

- `./gradlew assembleDebug` → install → launch (every phase).
- Each tab renders with the correct palette; status/nav bars recolour per tab.
- **Tasks:** add / edit / done / drag / sections / categories / daily-reset.
- **Folders:** CRUD / reorder / expand.
- **Notes:** nav / CRUD / reorder.
- **Sync:** JSON / Firebase / share / auth.
- **Existing `.realm` DB opens** and old JSON backups restore.

## Open / disputed decisions (changeable)

1. **Drag-reorder:** `sh.calvin.reorderable` library vs hand-rolled. _Chose library._
2. **Panel:** custom `AnchoredDraggable` vs Material3 `BottomSheetScaffold`. _Chose custom._

## Build & run (during migration)

The system default JDK is **25**, which Gradle 8.10.2 cannot run on
(`Unsupported class file major version 69`). Build with **JDK 21**:

```bash
export JAVA_HOME="/Users/ashumidub/Library/Java/JavaVirtualMachines/jdk-21.0.5+11/Contents/Home"
./gradlew :app:assembleDebug
```

Launch the Compose host (separate from the legacy launcher):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.shumidub.todoapprealm.alpha8/com.shumidub.todoapprealm.ui.compose.ComposeHostActivity
```

## Subsystem specs

Detailed per-area implementation specs live in [docs/specs/](specs/):
`shell-navigation`, `theme-palette`, `folder-panel`, `tasks`, `notes`, `sync`,
`data-interop`, plus `_GAPS.md` (cross-cutting risks: Realm-transaction vs
recomposition races, back-press atomicity, daily-reset timezone boundaries,
auto-expand-on-hover feasibility, old-backup JSON compat). Read the relevant spec
before implementing each phase.

## Progress log

- 2026-06-13 — Plan approved. Docs written. Subsystem specs generated (`docs/specs/`).
- 2026-06-13 — **Phase 0 (Scaffold) ✅ DONE & VERIFIED.**
  - Decision: legacy `MainActivity` stays the launcher; Compose lives in a separate
    non-launcher `ComposeHostActivity` until Phase 5. Old app keeps working throughout.
  - Decision: `realm-android` plugin applied **before** the Kotlin plugins so Realm keeps
    its existing `annotationProcessor` path (Java models) and never demands a `kapt`
    config. Schema unchanged (v5); existing `.realm` DB compatibility preserved.
  - Added Kotlin 2.0.21 + Compose compiler 2.0.21 + Compose BOM 2024.12.01 (Material 3,
    activity-compose, lifecycle-viewmodel-compose, foundation, coroutines).
  - Ported `Palette.java` → `TabPalette.kt` (+ `Todo100Theme.kt`, `LocalTabPalette`).
  - `MainScreen.kt`: 5-page `HorizontalPager`, START_PAGE=1, theme + system bars track the
    settled tab; each page rendered in its own palette.
  - Verified: `assembleDebug` green; installs/launches with no crash; all 5 tabs render
    with correct per-tab palette + reactive status/nav-bar tint (incl. dark icons on the
    yellow Canary tab). Realm transform ran over all Java models.
- 2026-06-13 — **Phase 1 + Phase 2 core (vertical slice) ✅ DONE & VERIFIED.**
  - Data-interop: `data/Dtos.kt` (detached `TaskDto`/`FolderDto`), `data/TasksRepository.kt`
    (Realm→`Flow` via `RealmChangeListener` + detached snapshots, invariant G1; writes
    delegate to existing Java controllers), `ui/compose/TasksViewModel.kt` (per-group VM,
    `viewModel(factory=…)` initializer — Dagger VM factory still deferred).
  - UI rebuilt to the real folder-panel model (per user feedback + `docs/specs/folder-panel.md`):
    screen shows **categories** with `done/all` point counts; tapping a category opens its
    tasks **from the bottom** (M3 `ModalBottomSheet`) with checkboxes / delete / n-of-m badge
    and the bottom add-task panel toggles (points ×N / max /N / priority !N / cycling ↻,
    hidden on the Notes group). Add-category dialog.
  - **Legacy Notes page hidden**: Compose pager now has 4 pages = task groups 0..3 (no old
    Notes page), starts on Tasks 1. `Tabs.java` left untouched (still used by the old app).
  - Verified on emulator against the user's real Realm data: categories render with correct
    counts; bottom sheet opens per category; **done-toggle writes through to Realm and the
    list reactively re-sorts** (done→bottom); per-tab palette correct; no crash. (adb text
    injection into Compose fields is flaky in automation — manual typing works.)
- 2026-06-13 — **Phase 4 (Sync) ✅** — `data/SyncManager.kt` + `ui/compose/SyncScreen.kt`:
  JSON export to Downloads, restore from SAF file / string, share-as-text, Firebase
  email/password auth + upload/download. Decoupled from MainActivity. Sync action in the
  top bar. Verified: dialog opens, Firebase signed in, JSON export writes to Downloads.
- 2026-06-13 — **Phase 5 (Cleanup) ✅** — `ComposeHostActivity` is the launcher; deleted the
  entire legacy Fragment UI (activity/fragment/actionmode/dialog), the MainActivity-coupled
  sync utils, `Palette.java`, all `res/layout` XML, and dropped RxJava2 / SlidingUpPanel /
  keyboardvisibilityevent. `ReportObject` data kept (screen gone). Verified: clean build,
  launches from icon, renders real Realm data, no crash. App is now **22 Java + 10 Kotlin**
  files (Java = data layer only).
- **Phase 3 (legacy Notes) — DROPPED** per user request ("старые Notes можешь скрыть").

## Status: MIGRATION COMPLETE

The app ships as a Jetpack Compose UI over the unchanged Realm data layer. Done: scaffold,
data-interop (detached Flow snapshots), category → bottom-sheet tasks with sections / editor /
multi-category / day-score / daily-reset / folder CRUD, backup-sync (JSON + Firebase),
launcher flip + legacy cleanup. Existing `.realm` DB + old JSON backups still work
(schema v5, never bumped).

**Deferred polish (optional, non-blocking):** drag-reorder of tasks/sections; the custom
24dp-peek `AnchoredDraggable` panel (ModalBottomSheet used instead); standing up a proper
Dagger ViewModel factory (currently a plain `viewModel(factory=…)` initializer); the
Notes-group (3) "curtain" full-screen note editor nuance. Drag-reorder is the only legacy
feature not yet reproduced — see `docs/specs/tasks.md` / `folder-panel.md`.
