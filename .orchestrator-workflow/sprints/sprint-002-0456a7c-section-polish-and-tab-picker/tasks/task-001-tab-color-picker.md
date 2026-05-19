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
  - `blue` → group 1 → Tasks2 (Cornflower-палитра)
  - `yellow` → group 2 → Tasks3 (Canary-палитра)
- Источник правды для текущей группы папки — `FolderTaskRealmController.getFolderGroup(folder)` (возвращает 0/1/2/-1).
- Перемещение — `FolderTaskRealmController.moveFolderToGroup(folder, targetGroup)`. Уже корректно обрабатывает все три случая (см. `FolderTaskRealmController.java:106-117`).

### R1. EditDelFolderDialog — замена checkbox на 3-way picker
- Удалить `CheckBox cbTasks2` (`@+id/checkbox_tasks2`) из `dialog_add_folder_layout.xml` и из `EditDelFolderDialog.java`.
- Вставить вместо неё 3-way picker (виджет — см. Open Question 1) с тремя опциями: `green` / `blue` / `yellow` (надписи + см. Open Question 2 по визуальной индикации цвета).
- Picker всегда видим в EDIT-режиме (в отличие от старого checkbox, который был VISIBLE только при `title == EDIT_LIST` — поведение сохраняется).
- Начальное состояние при открытии: выбран вариант, соответствующий `FolderTaskRealmController.getFolderGroup(folderObject)`. Если по какой-то причине `-1` (папка вне всех трёх контейнеров) — fallback на `green` (group 0).
- При нажатии Done: вычислить `targetGroup` из выбранного варианта, вызвать `FolderTaskRealmController.moveFolderToGroup(folderObject, targetGroup)` (текущий вызов с тернарником `cbTasks2.isChecked() ? 1 : 0` заменяется). Остальное поведение onPositiveClick (editFolder, finishActionMode, notifySmallTasksViewPagerListsChanged, hideSoftInput, showToast) — без изменений.

### R2. AddFolderDialog — picker при создании
- Добавить в `dialog_add_folder_layout.xml` тот же picker-виджет, что и для Edit (общий layout, общий `@+id` — это уже один файл `dialog_add_folder_layout`). В `AddFolderDialog` он используется для выбора целевого таба новой папки.
- Начальное состояние при открытии: выбран вариант, соответствующий `taskGroup` из `ARG_TASK_GROUP` (т.е. таб, с которого пользователь нажал "+"). Это сохраняет существующее предсказуемое поведение: "плюс" с Tasks → по умолчанию green, с Tasks2 → blue, с Tasks3 → yellow.
- При нажатии Add: вместо текущего `int group = getArguments().getInt(ARG_TASK_GROUP, 0)` использовать значение из picker'а. Вызов `FolderTaskRealmController.addFolder(text, isDaily, group)` — без изменений.
- Если пользователь оставил дефолт — поведение идентично текущему.

### R3. Daily checkbox — coexistence
- `CheckBox cbIsDaily` остаётся независимым виджетом и **выше** picker'а в layout (порядок сверху вниз: name → Daily checkbox → color picker). Picker не зависит от Daily и наоборот.
- В `editFolder(folderObject, text, cbIsDaily.isChecked())` ничего не меняется.

### R4. Edge case — смена цвета папки в EDIT
- При сохранении (Done) с изменённым цветом папка физически удаляется из старого `RealmList` и добавляется в новый (через `moveFolderToGroup`, уже существующий механизм).
- **Видимый таб не переключается автоматически.** Пользователь остаётся на текущем табе; папка просто исчезает оттуда. Это уже текущее поведение для checkbox-сценария Tasks↔Tasks2. См. Open Question 3 по UX — нужен ли тост вида "Moved to Tasks2".
- Все три fragment-instance (Tasks / Tasks2 / Tasks3 в `App.folderSlidingPanelFragments`) уже получают `notifySmallTasksViewPagerListsChanged()` в текущем коде → перерисуются корректно.

### R5. Edge case — папка с group=-1
- Не нормальный кейс, но защитный fallback: если папка по какой-то причине не находится ни в одном из трёх контейнеров (`getFolderGroup == -1`), picker показывает `green` (group 0), и при Done — `moveFolderToGroup(folder, 0)` добавит её в первый контейнер. Без падений.

### R6. Realm миграция
- **Не требуется.** Изменений в схеме нет. Acceptance criterion в шапке формулирует это как "поле group уже есть"; формально поля нет, но логически group выводится из контейнерных списков, и эти списки (`folderOfTasksList`, `folderOfTasksList2`, `folderOfTasksList3`) уже существуют в `RealmFoldersContainer` после `SCHEMA_VERSION=4`.

### R7. UI констрейнты
- Picker должен быть **компактным** — 3 опции в одну строку (горизонтально) внутри LinearLayout диалога. Высота не больше 48dp (mat. touch target). Подпись над picker'ом — см. Open Question 4 (нужна ли label вида "Tab color").
- В диалоге уже `paddingHorizontal=24dp`, `paddingTop=8dp`, `paddingBottom=4dp` — picker встаёт в этот же container.
- Picker не должен ломать существующий visual hierarchy (TextInputLayout → Daily CheckBox → Picker). `layout_marginTop` для picker'а — порядка `12dp` (как у Daily checkbox).

### R8. Точки касания кода (для Phase 3 — design)
- `app/src/main/res/layout/dialog_add_folder_layout.xml` — заменить `CheckBox @+id/checkbox_tasks2` на picker.
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/EditDelFolderDialog.java` — удалить `cbTasks2`, добавить picker-handle и init по `getFolderGroup`.
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/AddFolderDialog.java` — добавить picker-handle и init по `ARG_TASK_GROUP`.
- Без изменений: `FolderTaskRealmController`, `MainPagerAdapter`, `RealmFoldersContainer`, `App.java`, `RealmMigrations.java`, палитры.

### R9. Тематизация picker'а под текущий таб
- Диалог открывается через `MainActivity.dialogBuilder()` + `dialogContext()`, которые уже инжектят `ThemeOverlay.App.MaterialAlertDialog.Cornflower` или `...Canary` в зависимости от текущего таба. Picker должен корректно подхватывать `colorAccent` из этого overlay (RadioButton/MaterialButtonToggleGroup тинт). См. Open Question 5 по тому, как именно цветить три варианта (нейтральный текст vs. подкрашенные swatch'и).

## Open Questions

1. Какой виджет UX уместен для 3-way выбора цвета папки в существующем диалоге?
   **Answer (user):** `MaterialButtonToggleGroup` с тремя toggle-button'ами в горизонтальный ряд, single-select, цветной fill каждой кнопки. Selected — рамка/border.
2. Как визуально передать "цвет" каждой опции?
   **Answer (user):** Цветной background самой кнопки (green / blue / yellow fill), текстовая надпись внутри кнопки (`GREEN` / `BLUE` / `YELLOW` или короче — определит Phase 3 design).
3. Нужна ли явная обратная связь о перемещении папки на другой таб?
   **Answer (user):** Нет. Сохраняется текущее поведение — папка просто исчезает с текущего таба. Пользователь сам свайпнет на нужный.
4. Нужна ли над picker'ом текстовая подпись-label?
   **Answer (default):** Да, мини-label "Tab color" в стиле `?attr/textAppearanceLabelSmall` (или эквивалент), как у других полей диалога. Это даёт явный контекст что выбираем.
5. Реакция picker'а на per-tab ThemeOverlay диалога?
   **Answer (default):** Все три swatch'а **всегда показаны родными цветами палитр** (зелёный = `colorBackgroundActivity` из base theme, синий = `cornflowerBg`, жёлтый = `canaryBg`), независимо от темы текущего диалога. Selected-индикация — толстый белый border (~2dp) поверх fill, не зависит от текущего overlay.
6. AddFolderDialog cold-start risk с offscreenPageLimit?
   **Answer (default):** Игнорируем — существующий `notifySmallTasksViewPagerListsChanged()` работает корректно для живых fragments, остальные при создании сами подтянут данные через стандартный lifecycle. Не блокер.
7. Противоречие в acceptance criterion про "поле group уже есть"?
   **Answer (default):** Переформулируем: group выводится из RealmList membership (`folderOfTasksList` / `...List2` / `...List3` в `RealmFoldersContainer`). Поле в модели НЕ добавляется, миграция НЕ нужна. AC в шапке имел в виду именно это — формулировка просто была неточной, фиксим в R6.

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
