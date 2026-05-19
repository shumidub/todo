# Task 001 — Выбор green/blue/yellow таба для папки

- **Sprint:** sprint-002-0456a7c
- **Task token:** 7382aca
- **Project:** todo100android
- **Cross-project ref:** —
- **Feature flag:** (определить на Phase 3)
- **Status:** pending

## Acceptance criteria

- [ ] В диалоге редактирования папки (`EditDelFolderDialog`) вместо чекбокса "On Tasks2 tab" появляется выбор одного из трёх вариантов: **green** / **blue** / **yellow**.
- [ ] Эти три значения соответствуют существующим табам Tasks (group=0, зелёный), Tasks2 (group=1, синий/Cornflower), Tasks3 (group=2, жёлтый/Canary).
- [ ] При создании новой папки тоже доступен этот выбор (`AddFolderDialog` или эквивалент).
- [ ] Текущие папки в БД должны корректно маппиться: group=0 → green, group=1 → blue, group=2 → yellow. Никакой Realm-миграции (поле group уже есть).
- [ ] При смене значения папка перемещается в соответствующий таб (как раньше с чекбоксом).
- [ ] UI — компактный (3 опции в строку или один радио-блок), не ломает остальной layout диалога.

## Requirements

### Терминология (важно для Phase 3+)
- "Цвет папки" в рамках этой задачи **=** табовая принадлежность папки. Поле `group` в модели **не существует** как отдельный column в `FolderTaskObject` — членство в группе определяется тем, в какой `RealmList` контейнера папка лежит (`folderOfTasksListFromContainer` / `...List2...` / `...List3...`).
- Маппинг (зафиксирован существующим кодом):
  - `green` → group 0 → Tasks (зелёный таб, `colorPrimary=#267c52`, `colorBackgroundActivity=#599c74`)
  - `blue` → group 1 → Tasks2 (Cornflower-палитра, `cornflowerBg`)
  - `yellow` → group 2 → Tasks3 (Canary-палитра, `canaryBg`)
- Источник правды для текущей группы папки — `FolderTaskRealmController.getFolderGroup(folder)` (возвращает 0/1/2/-1).
- Перемещение — `FolderTaskRealmController.moveFolderToGroup(folder, targetGroup)`. Уже корректно обрабатывает все три случая (см. `FolderTaskRealmController.java:106-117`).

### R1. EditDelFolderDialog — замена checkbox на 3-way picker
- Удалить `CheckBox cbTasks2` (`@+id/checkbox_tasks2`) из `dialog_add_folder_layout.xml` и из `EditDelFolderDialog.java`.
- Вставить вместо неё `MaterialButtonToggleGroup` с тремя toggle-кнопками в горизонтальный ряд, режим **single-select** (`app:singleSelection="true"`, `app:selectionRequired="true"`). Опции: `green` / `blue` / `yellow`. Каждая кнопка имеет цветной fill (см. R9) и текстовую надпись внутри (`GREEN` / `BLUE` / `YELLOW` — финальный регистр/сокращение определит Phase 3 design).
- Picker всегда видим в EDIT-режиме (в отличие от старого checkbox, который был VISIBLE только при `title == EDIT_LIST` — это поведение сохраняется: picker виден ровно тогда, когда раньше был виден checkbox).
- Начальное состояние при открытии: выбран вариант, соответствующий `FolderTaskRealmController.getFolderGroup(folderObject)`. Если по какой-то причине `-1` (папка вне всех трёх контейнеров) — fallback на `green` (group 0).
- При нажатии Done: вычислить `targetGroup` из выбранной кнопки toggle-group, вызвать `FolderTaskRealmController.moveFolderToGroup(folderObject, targetGroup)` (текущий вызов с тернарником `cbTasks2.isChecked() ? 1 : 0` заменяется). Остальное поведение onPositiveClick (editFolder, finishActionMode, notifySmallTasksViewPagerListsChanged, hideSoftInput, showToast) — без изменений.

### R2. AddFolderDialog — picker при создании
- Добавить в `dialog_add_folder_layout.xml` тот же `MaterialButtonToggleGroup`-виджет, что и для Edit (общий layout, общий `@+id` — это уже один файл `dialog_add_folder_layout`). В `AddFolderDialog` он используется для выбора целевого таба новой папки.
- Начальное состояние при открытии: выбран вариант, соответствующий `taskGroup` из `ARG_TASK_GROUP` (т.е. таб, с которого пользователь нажал "+"). Это сохраняет существующее предсказуемое поведение: "плюс" с Tasks → по умолчанию green, с Tasks2 → blue, с Tasks3 → yellow.
- При нажатии Add: вместо текущего `int group = getArguments().getInt(ARG_TASK_GROUP, 0)` использовать значение из picker'а. Вызов `FolderTaskRealmController.addFolder(text, isDaily, group)` — без изменений.
- Если пользователь оставил дефолт — поведение идентично текущему.

### R3. Daily checkbox — coexistence
- `CheckBox cbIsDaily` остаётся независимым виджетом и **выше** picker'а в layout (порядок сверху вниз: name → Daily checkbox → color picker). Picker не зависит от Daily и наоборот.
- В `editFolder(folderObject, text, cbIsDaily.isChecked())` ничего не меняется.

### R4. Edge case — смена цвета папки в EDIT
- При сохранении (Done) с изменённым цветом папка физически удаляется из старого `RealmList` и добавляется в новый (через `moveFolderToGroup`, уже существующий механизм).
- **Видимый таб не переключается автоматически и никакой явной обратной связи (toast/snackbar/auto-swipe) не показывается.** Пользователь остаётся на текущем табе; папка просто исчезает оттуда — пользователь сам свайпнет на нужный таб. Это сохраняет текущее поведение checkbox-сценария Tasks↔Tasks2.
- Все три fragment-instance (Tasks / Tasks2 / Tasks3 в `App.folderSlidingPanelFragments`) уже получают `notifySmallTasksViewPagerListsChanged()` в текущем коде → перерисуются корректно. Cold-start других fragments через offscreenPageLimit не требует доп. обработки — лайфцикл сам подтянет данные.

### R5. Edge case — папка с group=-1
- Не нормальный кейс, но защитный fallback: если папка по какой-то причине не находится ни в одном из трёх контейнеров (`getFolderGroup == -1`), picker показывает `green` (group 0), и при Done — `moveFolderToGroup(folder, 0)` добавит её в первый контейнер. Без падений.

### R6. Realm миграция
- **Не требуется.** Изменений в Realm-схеме нет, `SCHEMA_VERSION` не инкрементируется, `RealmMigrations.java` не трогаем.
- Поле `group` в модели `FolderTaskObject` **не добавляется** — group остаётся производным значением, вычисляемым из членства папки в `RealmList`-ах контейнера `RealmFoldersContainer` (`folderOfTasksList` / `folderOfTasksList2` / `folderOfTasksList3`). Эти три списка уже существуют в схеме после `SCHEMA_VERSION=4` и используются текущим кодом.
- Acceptance criterion в шапке про "поле group уже есть" имеет в виду именно это (RealmList membership как источник истины, а не отдельная column).

### R7. UI констрейнты
- Picker — компактный горизонтальный `MaterialButtonToggleGroup` (3 кнопки в одну строку). Высота кнопок — не больше 48dp (Material touch target), ширина — `0dp` + `layout_weight=1` на каждую (равные доли) либо `wrap_content` (финал — Phase 3 design).
- **Над picker'ом — текстовая label "Tab color"** (`?attr/textAppearanceLabelSmall` или эквивалент, в стиле остальных подписей диалога), чтобы дать явный контекст выбора.
- В диалоге уже `paddingHorizontal=24dp`, `paddingTop=8dp`, `paddingBottom=4dp` — picker встаёт в этот же container.
- Picker не должен ломать существующий visual hierarchy (TextInputLayout → Daily CheckBox → "Tab color" label → toggle-group). `layout_marginTop` для блока picker'а — порядка `12dp` (как у Daily checkbox).

### R8. Точки касания кода (для Phase 3 — design)
- `app/src/main/res/layout/dialog_add_folder_layout.xml` — заменить `CheckBox @+id/checkbox_tasks2` на label + `MaterialButtonToggleGroup` с тремя `MaterialButton`-детьми.
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/EditDelFolderDialog.java` — удалить `cbTasks2`, добавить handle на toggle-group и init по `getFolderGroup`.
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/AddFolderDialog.java` — добавить handle на toggle-group и init по `ARG_TASK_GROUP`.
- Без изменений: `FolderTaskRealmController`, `MainPagerAdapter`, `RealmFoldersContainer`, `App.java`, `RealmMigrations.java`, палитры цветов в `colors.xml`/`themes.xml`.

### R9. Тематизация picker'а под текущий таб
- Диалог открывается через `MainActivity.dialogBuilder()` + `dialogContext()`, которые инжектят `ThemeOverlay.App.MaterialAlertDialog.Cornflower` или `...Canary` в зависимости от текущего таба. Picker эту тему **игнорирует** в плане окраски swatch'ей.
- Все три кнопки toggle-group **всегда показаны родными цветами палитр**, независимо от текущего overlay диалога:
  - green-кнопка → fill = `colorBackgroundActivity` базовой темы (`#599c74`)
  - blue-кнопка → fill = `cornflowerBg` (синий из Cornflower-палитры)
  - yellow-кнопка → fill = `canaryBg` (жёлтый из Canary-палитры)
- **Selected-индикация** — толстый белый border `~2dp` поверх fill выбранной кнопки (через `app:strokeColor=#FFFFFF` + `app:strokeWidth=2dp` на selected state). Не selected кнопки — без border (или с border = transparent). Эта индикация работает одинаково во всех трёх ThemeOverlay диалога.
- Текст на кнопках — контрастный к fill (белый или чёрный — финал по контрасту в Phase 3 design).

## Open Questions

All questions resolved in Phase 2 — see commit history for the original Q&A.

## Design

<заполняется на Phase 3>

## Tests

Manual QA — без TDD (как в sprint-001).

## Implementation

<заполняется на Phase 5>

## Review

<заполняется на Phase 6>

## Manual verification

<заполняется на Phase 7>
