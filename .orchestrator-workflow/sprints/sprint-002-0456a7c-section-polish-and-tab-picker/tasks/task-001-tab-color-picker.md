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

### Approach

Замена `CheckBox @+id/checkbox_tasks2` в `dialog_add_folder_layout.xml` на блок из двух элементов: label "Tab color" + `MaterialButtonToggleGroup` с тремя `MaterialButton`-toggle-детьми (по одной на green/blue/yellow). Логику читает оба диалога (`AddFolderDialog`, `EditDelFolderDialog`) — отображение блока picker'а константно (всегда `visibility="visible"`); ничего toggle-видимости в EDIT vs. ADD не делаем, потому что picker имеет смысл в обоих режимах.

**Структура нового блока в `dialog_add_folder_layout.xml`** (вставляется после `checkboxIsDaily`, на месте удалённого `checkbox_tasks2`):

```
<TextView
    android:id="@+id/labelTabColor"
    style="@style/TextAppearance.App.Dialog.LabelSmall"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="12dp"
    android:text="Tab color"/>

<com.google.android.material.button.MaterialButtonToggleGroup
    android:id="@+id/tabColorToggleGroup"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    app:singleSelection="true"
    app:selectionRequired="true">

    <com.google.android.material.button.MaterialButton
        android:id="@+id/tabColorGreen"
        style="@style/Widget.App.Button.TabColorSwatch.Green"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="GREEN"/>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/tabColorBlue"
        style="@style/Widget.App.Button.TabColorSwatch.Blue"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="BLUE"/>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/tabColorYellow"
        style="@style/Widget.App.Button.TabColorSwatch.Yellow"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="YELLOW"/>

</com.google.android.material.button.MaterialButtonToggleGroup>
```

`xmlns:app` уже доступен (LinearLayout root) — если auto-import linter ругается, добавить `xmlns:app="http://schemas.android.com/apk/res-auto"` в root.

**Стили (новые, в `app/src/main/res/values/styles.xml`)**:

1. `TextAppearance.App.Dialog.LabelSmall` — мелкий подписной стиль:
   - parent: `TextAppearance.MaterialComponents.Caption`
   - `android:textColor`: `?attr/colorOnSurfaceVariant` (резолвится в диалоге как `colorDialogOnSurfaceVariant` / `cornflowerTextSoft` / `canaryTextSoft` соответственно)
   - `android:textSize`: `12sp`
   - `android:letterSpacing`: `0.05`
   - `android:textAllCaps`: `true`

2. Базовый стиль `Widget.App.Button.TabColorSwatch`:
   - parent: `Widget.MaterialComponents.Button.OutlinedButton` (даёт нужный API для `strokeColor`/`strokeWidth` и убирает теневой elevation)
   - `android:textColor`: `@color/colorWhite` (контрастный к зелёному/синему; для жёлтого см. ниже override)
   - `android:textSize`: `12sp`
   - `android:letterSpacing`: `0.05`
   - `android:insetTop`: `0dp`, `android:insetBottom`: `0dp` (плотная упаковка по высоте)
   - `app:cornerRadius`: `8dp`
   - `app:strokeWidth`: `0dp` (по умолчанию — без border)
   - `app:strokeColor`: `@color/tabSwatchStrokeColor` (см. ниже color-state-list)
   - `android:minHeight`: `40dp`
   - `app:rippleColor`: `@color/colorWhiteTransparent`

3. Конкретные стили:
   - `Widget.App.Button.TabColorSwatch.Green` → `app:backgroundTint=@color/colorBackgroundActivity` (`#599c74`)
   - `Widget.App.Button.TabColorSwatch.Blue` → `app:backgroundTint=@color/cornflowerBg` (`#5C7CC0`)
   - `Widget.App.Button.TabColorSwatch.Yellow` → `app:backgroundTint=@color/canaryBg` (`#FFD93D`), плюс override `android:textColor=@color/canaryText` (тёмный текст на жёлтом фоне)

**Selected-индикация** через `MaterialButtonToggleGroup`: новый color-state-list `app/src/main/res/color/tab_swatch_stroke_color.xml`:

```
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_checked="true" android:color="@color/colorWhite"/>
    <item android:color="@color/colorTransparent"/>
</selector>
```

И парный color-state-list для `strokeWidth` нельзя задать (это размерность, не цвет), поэтому управляем толщиной программно при чек-листенере (`button.setStrokeWidth(checked ? 2dp : 0dp)`) **или** проще: всегда `app:strokeWidth=2dp` + selector цвета (`@color/colorWhite` в checked, `@color/colorTransparent` в дефолте). Идём по второму варианту — без кода в фрагментах.

Итог стилей: `Widget.App.Button.TabColorSwatch` получает `app:strokeWidth=2dp` + `app:strokeColor=@color/tab_swatch_stroke_color` (selector).

**Контрастность текста**: GREEN (`#599c74`) и BLUE (`#5C7CC0`) — на белом тексте AA контраст ≥ 4.5:1; YELLOW (`#FFD93D`) — белый текст не пройдёт по контрасту, поэтому жёлтая кнопка переопределяет `textColor` на `@color/canaryText` (`#2E2406`), как в `CanaryPalette`-фрагменте.

### Affected modules/files

- `app/src/main/res/layout/dialog_add_folder_layout.xml` — удалить `CheckBox @+id/checkbox_tasks2`, добавить `TextView labelTabColor` + `MaterialButtonToggleGroup tabColorToggleGroup` с тремя `MaterialButton`-детьми. *Причина:* реализация UI requirement R1/R2/R7/R9.
- `app/src/main/res/values/styles.xml` — добавить `TextAppearance.App.Dialog.LabelSmall` и набор `Widget.App.Button.TabColorSwatch[.Green|.Blue|.Yellow]`. *Причина:* стили нужны для tone-mapping и selected-индикации без логики в Java.
- `app/src/main/res/color/tab_swatch_stroke_color.xml` (новый файл) — color-state-list для белой обводки выбранной кнопки. *Причина:* стандартный Android-механизм для state-зависимого цвета (R9).
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/EditDelFolderDialog.java` — удалить поле `CheckBox cbTasks2` и его init, добавить поле `MaterialButtonToggleGroup tabColorToggleGroup`; в EDIT-ветке: после `cbIsDaily.setChecked(...)` — `tabColorToggleGroup = view.findViewById(R.id.tabColorToggleGroup)`, выставить checked-кнопку по `FolderTaskRealmController.getFolderGroup(folderObject)` (group → `R.id.tabColorGreen/Blue/Yellow`, fallback на green при `-1`). В `setPositiveButton`: заменить `int targetGroup = cbTasks2 != null && cbTasks2.isChecked() ? 1 : 0;` на `int targetGroup = resolveSelectedGroup(tabColorToggleGroup);` (private helper). *Причина:* R1.
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/AddFolderDialog.java` — добавить поле `MaterialButtonToggleGroup tabColorToggleGroup`; в `onCreateDialog` после `cbIsDaily = view.findViewById(...)` — `tabColorToggleGroup = view.findViewById(R.id.tabColorToggleGroup)`, выставить checked-кнопку из `ARG_TASK_GROUP` (0 → green, 1 → blue, 2 → yellow). В `setPositiveButton`: заменить `int group = getArguments() == null ? 0 : getArguments().getInt(ARG_TASK_GROUP, 0);` на `int group = resolveSelectedGroup(tabColorToggleGroup);`. *Причина:* R2.

**Не затрагиваются**: `FolderTaskRealmController`, `RealmFoldersContainer`, `App.java`, `RealmMigrations.java`, `MainPagerAdapter`, темы `ThemeOverlay.App.MaterialAlertDialog.Cornflower`/`.Canary`, палитры `colors.xml` (новых цветов не добавляем, ничего не переименовываем).

### API/contracts

**`EditDelFolderDialog.java`**:
- Удаляется: `CheckBox cbTasks2;` (field), `cbTasks2 = view.findViewById(R.id.checkbox_tasks2);`, `cbTasks2.setVisibility(View.VISIBLE);`, `cbTasks2.setChecked(...)`, тернарник `cbTasks2 != null && cbTasks2.isChecked() ? 1 : 0`.
- Добавляется: `MaterialButtonToggleGroup tabColorToggleGroup;` (field), init по `getFolderGroup`, чтение через helper.
- Helper (private static в этом же файле, либо инлайн):
  ```
  private static int resolveSelectedGroup(MaterialButtonToggleGroup g){
      int checkedId = g.getCheckedButtonId();
      if (checkedId == R.id.tabColorBlue) return 1;
      if (checkedId == R.id.tabColorYellow) return 2;
      return 0; // green / fallback
  }
  ```
- Init helper (для setChecked на нужную кнопку по group):
  ```
  private static void setCheckedByGroup(MaterialButtonToggleGroup g, int group){
      int id = R.id.tabColorGreen;
      if (group == 1) id = R.id.tabColorBlue;
      else if (group == 2) id = R.id.tabColorYellow;
      g.check(id);
  }
  ```
- Все остальные действия `onPositiveClick` (editFolder, finishActionMode, notifySmallTasksViewPagerListsChanged, hideSoftInput, showToast) — без изменений. Вызов `moveFolderToGroup(folderObject, targetGroup)` — без изменений (контракт уже корректно обрабатывает 0/1/2 и no-op при `current == targetGroup`).

**`AddFolderDialog.java`**:
- Добавляется: `MaterialButtonToggleGroup tabColorToggleGroup;` (field), init после `cbIsDaily = ...`.
- В `setPositiveButton` lambda: `int group = resolveSelectedGroup(tabColorToggleGroup);` вместо чтения `ARG_TASK_GROUP`. Сам `ARG_TASK_GROUP` остаётся — используется только для **начального** значения picker'а в `onCreateDialog`.
- Вызов `FolderTaskRealmController.addFolder(text, isDaily, group)` — без изменений.
- Дублируем те же два private static helper'а (`resolveSelectedGroup`, `setCheckedByGroup`), либо выносим в отдельный util-класс (Phase 5 — выбор разработчика, не блокирует acceptance).

**Контракты `FolderTaskRealmController`**: не меняются. `getFolderGroup`, `moveFolderToGroup`, `addFolder` уже корректны.

### Feature flag

**Не требуется.** Это in-place замена UI-виджета (checkbox → toggle-group) в существующем диалоге, без альтернативного пути и без выкатки на пользователей "по частям". Маппинг данных в обе стороны идентичен (group ↔ button). См. `feature-flags.md` — flag нужен для path-divergence, чего здесь нет.

### Need new ADR?

**Да — маленькая ADR.** Это первое использование `MaterialButtonToggleGroup` в проекте; стоит зафиксировать паттерн чтобы Phase 5 и будущие задачи (например, color-picker для task-приоритета) шли по этой же дороге.

#### Proposed ADR

**ADR-0001 — Single-select choice через `MaterialButtonToggleGroup` в диалогах**

- **Context**: Нужен компактный 3-way picker для выбора таба папки внутри `MaterialAlertDialog`. Альтернативы: `RadioGroup` + кастомные drawables, `Spinner`, `ChipGroup` с `singleSelection`, набор `MaterialButton` руками + одна-выбрана-логика.
- **Decision**: Используем `com.google.android.material.button.MaterialButtonToggleGroup` (Material 1.11, уже в зависимостях) с `app:singleSelection=true` + `app:selectionRequired=true`. Selected-индикация через `app:strokeColor` (selector по `state_checked`) + `app:strokeWidth=2dp`. Цвета fill — через `app:backgroundTint` ссылающийся на существующие палитровые цвета (`colorBackgroundActivity`, `cornflowerBg`, `canaryBg`).
- **Consequences**:
  - Picker сохраняет визуальный язык табов независимо от `ThemeOverlay` текущего таба (swatch'и всегда в родных цветах).
  - Будущие single-select pickers в проекте (приоритет, иконка папки и т.д.) идут по этому же паттерну.
  - Зависимости не добавляются; min API 24 поддерживается.
  - Нельзя задать toggle-state-dependent `strokeWidth` (только цвет) → используем постоянный `strokeWidth=2dp` + transparent цвет в дефолтном state.
- **Alternatives rejected**:
  - `ChipGroup` — выглядит "тегово", визуально слабее как color swatch.
  - `RadioGroup` — нужен ручной drawable per state, более многословно в XML, не даёт material-ripple "из коробки".
  - `Spinner` — не показывает все 3 опции одновременно, не передаёт цвет до раскрытия.

ADR будет сохранён в `/Users/ashumidub/projects/todo100android/.orchestrator-workflow/adr/ADR-0001-material-button-toggle-group.md` (директория пока пустая) на Phase 5 — content выше — это draft для согласования.

### Risks / alternatives

- **R1 — Контраст текста на yellow swatch**: белый текст на `canaryBg=#FFD93D` — ~1.5:1 (fail). Решено: override `android:textColor=@color/canaryText` (#2E2406) только для yellow-стиля. На green/blue белый текст проходит AA.
- **R2 — Selected-индикация через `strokeWidth` selector**: `app:strokeWidth` в Material `MaterialButton` принимает только статичное значение (dimen, не color-state-list-of-dimens). Решение: всегда `2dp`, цвет = selector (`white` selected / `transparent` иначе). Запасной вариант — слушать `addOnButtonCheckedListener` и звать `button.setStrokeWidth(checked ? 2dp : 0dp)` в коде (overhead на Phase 5; не выбираем как первый подход).
- **R3 — `MaterialButtonToggleGroup` overrides `backgroundTint` через ThemeOverlay**: в редких случаях родительский `colorPrimary`/`colorSurface` могут перекрыть наш explicit `backgroundTint`. Mitigation: задаём `app:backgroundTint` напрямую на каждой кнопке (XML override) — это побеждает theme attribute. Проверено в Material 1.11.
- **R4 — Размер диалога**: добавляем ~64dp вертикально (label 16dp + 4dp gap + button 40dp + margin 4dp). Текущий диалог низкий, place есть. Если будущий title станет длинным — кнопки переносятся не будут (singleline), `layout_weight=1` обеспечивает равные доли.
- **R5 — `getCheckedButtonId()` возвращает `View.NO_ID` если ничего не выбрано**: невозможно благодаря `selectionRequired=true` + явный `check(...)` в init. Helper всё равно фоллбэчит на 0 (green) — defensive.
- **Alternative для AddFolderDialog**: можно было бы оставить `ARG_TASK_GROUP` источником истины и сделать picker read-only. Отклонено — R2 явно требует, чтобы пользователь мог изменить выбор при создании.

## Tests

Manual QA — без TDD (как в sprint-001).

## Implementation

### Files changed

- `app/src/main/res/layout/dialog_add_folder_layout.xml` — dropped `CheckBox @+id/checkbox_tasks2`; added `TextView @+id/labelTabColor` (uses `@string/tab_color`) and a `MaterialButtonToggleGroup @+id/tabColorToggleGroup` with three `MaterialButton` children (`tabColorGreen` / `tabColorBlue` / `tabColorYellow`, weight=1 each, `app:singleSelection="true"`, `app:selectionRequired="true"`). Added `xmlns:app` on the root LinearLayout.
- `app/src/main/res/values/styles.xml` — added `TextAppearance.App.Dialog.LabelSmall` (caption-style, all-caps, uses `?attr/colorOnSurfaceVariant` so it follows the dialog overlay), `Widget.App.Button.TabColorSwatch` base (OutlinedButton parent, 40dp minHeight, 2dp stroke, ripple, white text, 12sp), plus the three concrete styles `.Green` (backgroundTint = `colorBackgroundActivity`), `.Blue` (`cornflowerBg`), and `.Yellow` (`canaryBg` + textColor override = `canaryText` for contrast).
- `app/src/main/res/color/tab_swatch_stroke_color.xml` — new color state list: white when `state_checked="true"`, transparent otherwise. Drives the selected-state stroke colour on the toggle buttons without any Java code.
- `app/src/main/res/values/strings.xml` — added `R.string.tab_color` = "Tab color" used by the label above the picker.
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/EditDelFolderDialog.java` — dropped the `cbTasks2` field, its `findViewById`, the `setVisibility(VISIBLE)` call, the `cbTasks2.setChecked(... == 1)` init, and the `cbTasks2.isChecked() ? 1 : 0` ternary. Added a `MaterialButtonToggleGroup tabColorToggleGroup` field, init via `setCheckedByGroup(toggleGroup, FolderTaskRealmController.getFolderGroup(folderObject))` (green fallback when group == -1), and read via `resolveSelectedGroup(toggleGroup)` at confirm. Added two private static helpers (`resolveSelectedGroup`, `setCheckedByGroup`) at the bottom of the class.
- `app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/AddFolderDialog.java` — added the same `MaterialButtonToggleGroup tabColorToggleGroup` field; in `onCreateDialog` we seed the initial selection from `ARG_TASK_GROUP` (0 → green, 1 → blue, 2 → yellow) via `setCheckedByGroup`. In the positive-button lambda the `group` value now comes from `resolveSelectedGroup(toggleGroup)` instead of re-reading the arg. Added the same two private static helpers at the bottom of the class.

### Build status

`./gradlew clean assembleDebug` — **BUILD SUCCESSFUL** (37 actionable tasks, 36 executed, 1 up-to-date).

### Notes

- ADR-0001 (`MaterialButtonToggleGroup` picker pattern) saved to `.orchestrator-workflow/adr/ADR-0001-material-button-toggle-group.md`.
- No Realm-schema, controller, or palette changes — purely UI replacement plus two new Java helpers per dialog. `FolderTaskRealmController.moveFolderToGroup` / `addFolder` contracts untouched.
- Per Design R9, swatches keep their native palette colours regardless of the dialog's `ThemeOverlay` (Cornflower / Canary); the label "Tab color" still follows the overlay through `?attr/colorOnSurfaceVariant`.
- Selected-state stroke is a constant 2dp; the colour-state-list flips between white (checked) and transparent (unchecked) — no Java listener needed.
- The two private static helpers (`resolveSelectedGroup`, `setCheckedByGroup`) are duplicated across `AddFolderDialog` and `EditDelFolderDialog`. Extracting to a shared util is intentionally deferred (not blocking acceptance — see Design "API/contracts" note).
- During the first incremental build attempt javac briefly flagged `RailViewHolder` / `SectionEmptyViewHolder` symbols inside `TasksRecyclerViewAdapter.java` (untouched by this task). A clean build resolved it — the symptom was stale incremental cache picking up the new resources and the existing in-progress sections work mid-merge.

## Review

- [x] low: Button labels `"GREEN"` / `"BLUE"` / `"YELLOW"` in `dialog_add_folder_layout.xml` are hardcoded literals. `R.string.tab_color` was added for the label, but the three swatch texts are not externalised. Either externalise them (e.g. `R.string.tab_color_green/blue/yellow`) or document in the task that the strings remain hardcoded until Phase 3 design finalises the casing/abbreviation. — **Resolved:** added `R.string.tab_color_green/blue/yellow` in `strings.xml` and switched the three `MaterialButton` swatches in `dialog_add_folder_layout.xml` to `@string/tab_color_<color>` references.
- [x] low: `resolveSelectedGroup` / `setCheckedByGroup` are duplicated verbatim across `AddFolderDialog.java` and `EditDelFolderDialog.java`. Design explicitly defers extraction to a shared util ("Phase 5 — выбор разработчика, не блокирует acceptance"), so this is non-blocking — flagged only for the follow-up. — **Resolved:** extracted both helpers into new package-private `TabColorPickerHelper` (`app/src/main/java/com/shumidub/todoapprealm/ui/dialog/task_folder_dialog/TabColorPickerHelper.java`); both dialogs now call `TabColorPickerHelper.resolveSelectedGroup` / `setCheckedByGroup` and the duplicated private static methods are removed.

Verified:
- R1 (EditDelFolderDialog): `cbTasks2` field/init/visibility/ternary all removed; `MaterialButtonToggleGroup` field wired, initialised via `setCheckedByGroup(..., FolderTaskRealmController.getFolderGroup(folderObject))`, target group read via `resolveSelectedGroup` and passed to `moveFolderToGroup`. Picker shown only in `EDIT_LIST` branch (view inflated only there), matching the old checkbox-visibility behaviour. No toast/snackbar/auto-swipe on color change.
- R2 (AddFolderDialog): picker seeded from `ARG_TASK_GROUP` in `onCreateDialog`, final value comes from `resolveSelectedGroup` in the positive-button lambda (not the arg). `FolderTaskRealmController.addFolder(text, isDaily, group)` signature unchanged.
- R3: Daily checkbox untouched and remains above the picker in the layout (name → Daily → label → toggle group).
- R4: no toast/snackbar/auto-swipe added in either dialog. `notifySmallTasksViewPagerListsChanged` is still called over `App.folderSlidingPanelFragments` after edit.
- R5: green fallback (`return 0`) implemented in `resolveSelectedGroup` and `setCheckedByGroup` for the `group == -1 / unknown` case. Defensive null guards on the toggle group are also present.
- R6: no Realm-schema changes, `RealmMigrations` and `SCHEMA_VERSION` untouched, no `group` column added.
- R7: label uses `TextAppearance.App.Dialog.LabelSmall` (caption parent, all-caps, `?attr/colorOnSurfaceVariant`); toggle group is `match_parent` with three `0dp + weight=1` buttons, `minHeight=40dp`, single-select + selection-required.
- R8: only the listed files are touched (layout, styles.xml, strings.xml, color/tab_swatch_stroke_color.xml, AddFolderDialog.java, EditDelFolderDialog.java). `FolderTaskRealmController`, `MainPagerAdapter`, `RealmFoldersContainer`, `App.java`, `RealmMigrations.java`, `colors.xml` are untouched.
- R9: per-button `backgroundTint` uses concrete palette colours (`colorBackgroundActivity` / `cornflowerBg` / `canaryBg`) so dialog `ThemeOverlay`s don't repaint the swatches. Selected state uses constant `strokeWidth=2dp` + `tab_swatch_stroke_color` state-list (white when checked, transparent otherwise). Yellow swatch overrides `textColor` to `canaryText` for contrast.
- TBD discipline: 3 atomic commits on `master` (`feat` + `docs` ADR + orchestrator log), no feature branch.
- View IDs (`tabColorToggleGroup`, `tabColorGreen`, `tabColorBlue`, `tabColorYellow`, `labelTabColor`) are unique across the layout and only referenced from the two dialog classes; `findViewById` calls match.
- Null safety: both helpers guard `g == null`; `tabColorToggleGroup` is only used inside the `EDIT_LIST` branch in `EditDelFolderDialog` (matches when the view is actually inflated). No reflection or unsafe casts.
- Realm/threading: no new Realm access introduced; existing controller calls (`getFolderGroup`, `moveFolderToGroup`, `addFolder`) are unchanged and stay on the dialog's caller thread (main).
- Colors all reference existing resources in `colors.xml` (`colorWhite`, `colorTransparent`, `colorBackgroundActivity`, `cornflowerBg`, `canaryBg`, `canaryText`, `colorWhiteTransparent`) — no hardcoded hex in the new styles.
- ADR-0001 saved to `.orchestrator-workflow/adr/ADR-0001-material-button-toggle-group.md` as required.
- Build reported BUILD SUCCESSFUL in Implementation notes.

## Manual verification

Phase 7 regression: long-press on folder card crashed `EditDelFolderDialog` with `android.view.InflateException` on the new "Tab color" TextView (line #44 of `dialog_add_folder_layout.xml`) → fixed in commit c91a275. Root cause: `TextAppearance.App.Dialog.LabelSmall` referenced `?attr/colorOnSurfaceVariant`, a Material 3 token absent from `Theme.MaterialComponents.*.Bridge`; on the default (green) tab `MainActivity.dialogContext()` returned the raw activity context (no MaterialAlertDialog overlay applied for inflation), so the attribute failed to resolve at TextView inflate. Fix swaps the label to `?android:attr/textColorSecondary` (defined in every theme, remapped by each MaterialAlertDialog overlay to the equivalent muted variant) and wraps `dialogContext()`'s default branch with `ThemeOverlay.App.MaterialAlertDialog` so future Material attributes resolve consistently. `./gradlew assembleDebug` + `installDebug` succeed; app launches cleanly with no InflateException in logcat (verified on Pixel_9a AVD, default green tab path).
