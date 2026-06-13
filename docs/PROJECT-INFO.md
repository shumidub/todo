# Project Info — todo100android (pre-Compose snapshot)

> Snapshot of the codebase **as of 2026-06-13**, captured before the Jetpack Compose
> migration. This is the "before" picture; the target architecture lives in
> [COMPOSE-MIGRATION-PLAN.md](COMPOSE-MIGRATION-PLAN.md).

## What the app is

A personal to-do / notes Android app ("100todo"). Local-first: all data in a
**Realm-java** database on device, with optional sync/backup to JSON files and
Firebase Realtime Database.

- Package / namespace: `com.shumidub.todoapprealm`
- applicationId: `com.shumidub.todoapprealm.alpha8`
- versionName `v8-claude`, versionCode 4
- minSdk **24**, compileSdk / targetSdk **35**, Java **17**
- Single `MainActivity` (AppCompat) + `ViewPager` of 5 fragments. Portrait only,
  `adjustResize`.

## Tech stack (current)

| Concern | Library |
|---|---|
| Language | Java (100%, no Kotlin yet) |
| Persistence | **realm-android 10.19** (`apply plugin: 'realm-android'`), schema **v5** |
| DI | **Dagger 2.50** (annotation processor) |
| Async | **RxJava2** (2.2.21) + RxAndroid |
| Sliding folder panel | **com.sothree.slidinguppanel 3.4.0** |
| Keyboard insets | **net.yslibrary.keyboardvisibilityevent 3.0.0-RC3** |
| Serialization | **Gson 2.10.1** |
| Cloud | **Firebase** BoM 33.7.0 (Auth email/password + Realtime Database) |
| UI toolkit | AppCompat 1.6.1, Material 1.11.0, RecyclerView, CardView, ConstraintLayout, MultiDex |

## Pager / tab model — `Tabs.java` (single source of truth)

- `PAGE_COUNT = 5`, `GROUP_COUNT = 4`, `START_PAGE = 1` (app opens on Tasks1).
- Page 0 = **old Notes** (`FolderNoteFragment`). Pages 1..4 = task tabs.
- Task groups map to pages via `positionForGroup(g) = g + 1`; `groupForPosition(p) = p-1` (or -1 for page 0).
- Group → identity: 0 = Tasks1 (default chrome), 1 = Tasks2 (Cornflower),
  2 = Tasks3 (Canary), 3 = Notes (Indigo).
- Adding a tab touches three places: `Tabs` counts, `RealmFoldersContainer.tasksListForGroup`, `Palette.forGroup`.

## Theming — `ui/theme/Palette.java`

- One immutable `Palette` with **9 colour tokens**: `bg, surface, surfaceMuted, text,
  textSoft, inputText, counter, accent, divider`.
- `Palette.forGroup(ctx, group)` returns a per-tab palette (Cornflower / Canary /
  Indigo) or `null` for default chrome (group 0).
- `Palette.dialogDefault(ctx)` is the fallback dialog chrome.
- Colours come from `res/values` colour resources (`cornflower*`, `canary*`, `indigo*`, `colorDialog*`).

## Data model (`realmmodel/`)

- `RealmFoldersContainer` — root singleton; holds the per-group folder lists
  (`folderOfTasksList`, `folderOfTasksList2`, …) + notes folders.
- Tasks: `FolderTaskObject` → `SectionObject` → `TaskObject` (with `ITaskObject` iface).
  `TaskObject.extraFolderIds: RealmList<Long>` = multi-category membership.
- Notes: `FolderNotesObject` → `NoteObject`.
- Reports: `ReportObject` / `IReportObject` — **data kept, screen dropped** in the migration.
- `RealmInteger` — boxed-int helper for RealmLists.

### Realm schema migrations — `RealmMigrations.java` (`SCHEMA_VERSION = 5`)
- v<2: `TaskObject.extraFolderIds` added (multi-category).
- v<3: `RealmFoldersContainer.folderOfTasksList2` added (2nd Tasks tab).
- v<4: `SectionObject` created (id PK, name, collapsedByDefault, currentlyCollapsed,
  parentFolderId indexed, position).
- v<5: (later step — see file).
- **Migration must NOT bump the schema version**; existing on-device `.realm` DBs and
  old JSON backups must keep opening.

## Realm controllers (`realmcontrollers/`)

Java controllers wrap all reads/writes (run on main thread):
`RealmDb`, `ContainersRealmController`, `FolderTaskRealmController`,
`SectionsRealmController`, `TasksRealmController`, `FolderNotesRealmController`,
`ReportRealmController`.

## Sync (`sync/`)

- `JsonSyncUtil` — export/import full DB to JSON (Gson). Backups land in Downloads via
  MediaStore (API 29+) or `WRITE_EXTERNAL_STORAGE` (legacy).
- `FirebaseSyncUtil` — push/pull to Firebase Realtime DB; `FirebaseAuthDialog` for
  email/password.
- `FileWritter`, `LocalSyncUtil` — file plumbing.
- Backup compatibility reference dumps: `REALM_BD_JSON.txt`, `backup-eb1660ef/`.

## UI layer (`ui/`) — what gets replaced

- **Activity**: `MainActivity`, `BaseActivity`, `CustomViewPager`, `MainPagerAdapter`.
- **Fragments**:
  - Notes page 0: `FolderNoteFragment` + `FolderNotesRecyclerViewAdapter` / `NotesRecyclerViewAdapter`.
  - Tasks: `FolderSlidingPanelFragment` (+ presenter/iface, MVP) hosting the folder panel,
    `SmallTasksFragment` + `TasksRecyclerViewAdapter` + `AdapterItem` +
    `ItemTouchHelperAttacher` + `SlideInDownItemAnimator` + pager adapters.
  - Reports: `ReportFragment` + adapter — **to be deleted**.
- **Dialogs**: notes (Add/Edit/Dell), folders (`AddFolderDialog`, `EditDelFolderDialog`,
  `TabColorPickerHelper`), `SectionEditDialog`, `TaskEditorBottomSheet`, `SyncDialog`,
  `FirebaseAuthDialog`, report dialogs (**to be deleted**).
- **ActionMode** (system CAB) callbacks: `EditDeleteActionModeCallback` (base),
  `EmptyActionModeCallback`, per-entity callbacks for note/folder/section/task/report.
- ~35 XML layouts in `res/layout/` (see `git ls-files app/src/main/res/layout`).

## Key behaviours to preserve (parity checklist)

- 5-page pager, START_PAGE=1, per-tab palette applied to chrome (status/nav bar).
- Folder panel: sliding panel with ~24dp peek + smooth offset (fades footer), folder CRUD
  + color picker + drag-reorder + action-mode + bottom add-task panel with toggles
  (count / max / priority / cycling).
- Tasks: collapsible sections, done/partial checkboxes, params, task editor sheet (half
  for tasks / full for Notes group 3), section edit dialog, multi-category, "Done N"
  footer, drag within/between sections with auto-expand on hover (~400ms), daily reset.
- Notes (page 0): folders→notes nav, CRUD, drag-reorder, action-mode, "add list" card.
- Sync: JSON export/import, Firebase push/pull, share, auth dialog.
- **Back-press chain**: panel expanded→collapse → action-mode→dismiss → notes-list→folders
  → double-tap to exit.
- Existing `.realm` DB opens; old JSON backups restore.

## Build / run

- `./gradlew assembleDebug` → install → launch.
- Firebase requires `app/google-services.json` (present). See `FIREBASE_SETUP.md`.
