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

1. Какой виджет UX уместен для 3-way выбора цвета папки в существующем диалоге (RadioGroup с горизонтальной ориентацией / `MaterialButtonToggleGroup` с тремя toggle-button'ами / три чипа `Chip` с `chipBackgroundColor` / три кастомных `View`-swatch'а с border на selected) — и какие ограничения по высоте/touch target / accessibility (TalkBack labels) для каждого варианта?
2. Как визуально передать "цвет" каждой опции — только текстом ("Green" / "Blue" / "Yellow"), цветным background самой кнопки (зелёный/синий/жёлтый fill), цветным кружком-swatch'ом рядом с текстом, или зеркалом палитры таба (использовать `colorBackgroundActivity` / `cornflowerBg` / `canaryBg` напрямую)? Что лучше читается на разных табах (зелёный picker на зелёном фоне Tasks → нужен ли border)?
3. Когда пользователь меняет цвет папки в EditDelFolderDialog и Done — нужна ли явная обратная связь о перемещении (Toast "Moved to Tasks2" / Snackbar с действием Undo / автоматический переход на новый таб через `viewPager.setCurrentItem`), или достаточно текущего "Done"-тоста и того, что папка просто исчезает из видимого списка?
4. Нужна ли над picker'ом текстовая подпись-label (например "Tab" / "Color" / "Tasks group"), и если да — какой стиль текста использовать (`?attr/textAppearanceLabelSmall` Material / inline hint / без подписи если виджет очевиден)?
5. Как picker должен реагировать на per-tab ThemeOverlay диалога (Cornflower на Tasks2, Canary на Tasks3, дефолт на Tasks) — все три swatch'а всегда показаны "родными" цветами палитр независимо от темы диалога, или selected-индикация (рамка/тень) использует `colorAccent` текущего overlay, и в чём приоритет конфликта (например, выбран yellow swatch, диалог открыт из Cornflower-таба — выделение синее или жёлтое)?
6. Нужно ли в AddFolderDialog учитывать редкий кейс, когда пользователь сменил цвет, но Tasks/Tasks2/Tasks3 fragments ещё не инициализированы (offscreenPageLimit + холодный старт)? Существующий `notifySmallTasksViewPagerListsChanged()` итерирует только по живым fragments — есть ли риск, что новая папка появится в Tasks3, но при свайпе туда таб покажет stale state?
7. Acceptance criterion в шапке утверждает "поле group уже есть" — но в `FolderTaskObject.java` отдельного `group`-поля нет, group выводится из принадлежности к одному из трёх `RealmList` в `RealmFoldersContainer`. Это противоречие в формулировке (требует ли это явного добавления поля `group` в модель + миграции — что усложнит задачу — или нужно оставить как сейчас и просто переформулировать criterion)?

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
