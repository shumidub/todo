# task-003 · Tasks3 таб с палитрой J2 Canary

**Sprint**: sprint-001-k8m3n2p-task-ux
**Status**: Phase 2 complete → Phase 3 (architecture)
**Owner**: bg-agent (TBD)

## User-facing описание
Добавить 4-й таб **Tasks3** в `MainActivity` ViewPager. Tasks3 = **независимый** список папок/задач (как Tasks vs Tasks2), использующий палитру J2 Canary и включающий все остальные новые фичи спринта (BottomSheet редактор из task-001, секции из task-002).

## Палитра J2 Canary
| token | value (#AARRGGBB) | usage |
|---|---|---|
| `canaryBg` | `#FBE34A` | основной фон экрана |
| `canarySurface` | `#FFFEEC` | бумажная карточка / поле ввода |
| `canarySurfaceMuted` | `#F0D63D` | нижняя плашка таба |
| `canaryAccent` | `#E94E3B` | коралловый акцент (применяется ко всем upstream-callers `accent`: линия под полем ввода, cursor, cycling indicator, ActionMode-бар) |
| `canaryText` | `#2E2A08` | основной текст |
| `canaryTextSoft` | `#A32E2A08` (alpha 0xA3 = 0.64) | плейсхолдеры, second-level |
| `canaryInputText` | `#2E2A08` | текст на белой карточке |
| `canaryCounter` | `#94882F` | счётчик "0 / 2" |
| `canaryDivider` | `#242E2A08` (alpha 0x24 = 0.14) | тонкая линия над полем ввода |

## Requirements (финал, Phase 2 confirmed)

### Tab integration
- **R1.** `MainPagerAdapter.getCount()` → 4. Position 3 → `FolderSlidingPanelFragment.newInstance(2)` (`taskGroup = 2`).
- **R2.** Дефолтный таб при старте — остаётся Tasks (position 1, `setCurrentItem(1)`).
- **R3.** Имя "Tasks3" пользователю **не показывается** напрямую: ActionBar title = имя текущей категории Tasks3 (как в Tasks/Tasks2). TabLayout не вводится.
- **R4.** Все места в `MainActivity`/`FolderSlidingPanelFragment`/`SmallTasksFragment`/адаптерах, где есть `if (taskGroup == 1)` для Cornflower, расширяются на `taskGroup == 2` для Canary. **Без** общего интерфейса `TabPalette` — параллельный класс (рефактор отдельной задачей).

### Палитра — артефакты
- **R5.** Новый `ui/theme/CanaryPalette.java` — зеркало `CornflowerPalette` (поля: `bg, surface, surfaceMuted, text, textSoft, inputText, counter, accent, divider`).
- **R6.** В `res/values/colors.xml` — все `canary*` ресурсы из таблицы выше.
- **R7.** В `res/values/styles.xml` — `ThemeOverlay.App.MaterialAlertDialog.Canary` по аналогии с Cornflower.
- **R8.** В `applyTabChrome` (MainActivity) для Canary-таба: `WindowInsetsControllerCompat.setAppearanceLightStatusBars(true)` и `setAppearanceLightNavigationBars(true)` — тёмные иконки на жёлтом фоне.

### Точки применения (зеркало Cornflower)
- **R9.** Каждый upstream-caller `CornflowerPalette` получает ветку для Canary:
  - `MainActivity.applyTabChrome` — статусбар/навбар/actionbar/фон
  - `MainActivity.dialogBuilder` / `dialogContext` — выбор `ThemeOverlay`
  - `MainActivity.tintActionModeBarIfCornflower` → расширить (имя оставить или переименовать в `tintActionModeBarForPalette`, на усмотрение Phase 3)
  - `FolderSlidingPanelFragment.applyCornflowerPalette(view)` → отдельный `applyCanaryPalette` (параллельный)
  - `FolderOfTaskRecyclerViewAdapter` — switch по `taskGroup` → выбор палитры
  - `TasksRecyclerViewAdapter.useCornflowerPalette(boolean)` → отдельный `useCanaryPalette(boolean)` (или один setter с enum, на усмотрение Phase 3 — минимизировать дифф)
  - `SmallTasksFragment.isInCornflowerTab()` → отдельный `isInCanaryTab()` + getter для палитры
  - `TaskActionModeCallback` — выбор `accent` цвета по `taskGroup`
- **R10.** В `onBackPressed` распознавать `currentFragmentItem == 3` и матчить `getTaskGroup() == 2` идентично существующему.

### Данные (независимый список)
- **R11.** Tasks3 — **независимый** список папок. В `RealmFoldersContainer` добавить `RealmList<FolderTaskObject> folderOfTasksList3`.
- **R12.** Realm migration: поднять `SCHEMA_VERSION` (координация с task-002 — обе задачи трогают migration; финальная версия = +1 от текущего 3, но единым шагом).
  - Через `schema.get("RealmFoldersContainer").addRealmListField("folderOfTasksList3", ...)`.
- **R13.** `FolderTaskRealmController` расширить:
  - `getFoldersList(int group)` — case `2` → `folderOfTasksList3`
  - `getFolderGroup(folderId)` — возвращает 2 для Tasks3-папок
  - `getAllFolders()` — включает Tasks3
  - `moveFolderToGroup` — поддержать `targetGroup == 2`

### Интеграция с task-001 (BottomSheet) и task-002 (секции)
- **R14.** Tasks3 использует тот же `SmallTasksFragment` + `TasksRecyclerViewAdapter`, что и Tasks/Tasks2 — значит BottomSheet редактор и секции там работают **автоматически**, при условии что:
  - BottomSheet принимает текущую палитру (`CanaryPalette` для Tasks3) — см. task-001 R13
  - Секции (task-002 R15) применяются ко всем 3 task-табам, включая Tasks3
- **R15.** Координация по порядку фаз 4-5: модельные изменения task-002 (миграция, новые controller) и task-003 (новый список папок) собираются в **единую миграцию** `SCHEMA_VERSION → 4` (планировщик Phase 3 это учитывает).

### Non-goals
- **R16.** Не вводится общий `TabPalette` интерфейс / `PaletteProvider` — параллельная структура классов. Рефактор отдельной задачей будущего спринта.
- **R17.** Не меняются иконки, dialog layouts (только цвета через ThemeOverlay).

## Затронутые файлы
- Новый: `ui/theme/CanaryPalette.java`
- `res/values/colors.xml`, `res/values/styles.xml` — `canary*` ресурсы, ThemeOverlay
- `MainPagerAdapter.java` — `getCount=4`, position 3
- `MainActivity.java` — `applyTabChrome`, `dialogBuilder`, `tintActionModeBar...`, `onBackPressed`
- `FolderSlidingPanelFragment.java` + дочерние адаптеры (`FolderOfTaskRecyclerViewAdapter`, `TasksRecyclerViewAdapter`) — ветка `taskGroup==2`
- `SmallTasksFragment.java` — `isInCanaryTab` + getter палитры
- `TaskActionModeCallback.java` — accent по taskGroup
- `realmmodel/RealmFoldersContainer` — `folderOfTasksList3`
- `RealmMigrations.java` + `RealmConfiguration` — координированно с task-002
- `FolderTaskRealmController.java` — поддержка `group=2`

## Design
_TBD фаза 3_

## Tests
_skip (manual QA)_

## Implementation
_TBD фаза 5_

## Review
_TBD фаза 6_
