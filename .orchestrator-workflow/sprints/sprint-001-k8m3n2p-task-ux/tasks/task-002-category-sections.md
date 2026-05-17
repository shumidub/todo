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
_TBD фаза 3_

## Tests
_skip (manual QA)_

## Implementation
_TBD фаза 5_

## Review
_TBD фаза 6_
