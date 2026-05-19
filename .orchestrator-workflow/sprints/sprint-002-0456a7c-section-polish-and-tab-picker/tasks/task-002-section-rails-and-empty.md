# Task 002 — Rails в начале/конце секции + Empty placeholder

- **Sprint:** sprint-002-0456a7c
- **Task token:** f81a0cb
- **Project:** todo100android
- **Cross-project ref:** —
- **Feature flag:** (определить на Phase 3)
- **Status:** pending

## Acceptance criteria

- [ ] У каждой развёрнутой (expanded) секции внутри категории отображается горизонтальная полоса:
  - **сверху** — сразу под заголовком секции (`section_header_card_view`)
  - **снизу** — после последней задачи секции
- [ ] Если секция пустая (нет задач), вместо списка задач показывается надпись **"Empty"** (по центру, в стиле минорного текста), и обе полосы (сверху и снизу) тоже рисуются.
- [ ] Свёрнутая (collapsed) секция полос не рисует — только заголовок.
- [ ] Полосы — белого цвета (как заголовок секции на скриншоте), толщина соразмерна (1-2dp), длина — на всю ширину контейнера задач (с теми же горизонтальными paddings, что у задач).
- [ ] Раскраска и контраст работают одинаково на всех трёх табах (green/blue/yellow).

## Requirements

### Контекст (как сейчас в коде)
- `section_header_card_view.xml` — `LinearLayout` высотой `minHeight=38dp`, paddings `start=8dp / end=12dp / top=6dp / bottom=6dp`, `layout_marginTop=6dp / marginBottom=2dp`. Внутри: chevron ▼/▶ (20dp), name (bold caps, 14sp, белый). Полос (rails) **нет** — есть невидимый `View#section_divider` (`visibility=gone`).
- `TasksRecyclerViewAdapter` рендерит heterogeneous список `items: List<AdapterItem>` (`flatten()`): outer-space (sections + free tasks по `position`), под каждой expanded-секцией её inner tasks. View types: `VIEW_TYPE_TASK=1`, `VIEW_TYPE_SECTION_HEADER=2`, `FOOTER_VIEW=123`, `VIEW_TYPE_EMPTY=99` (используется только если `items.isEmpty()`).
- `TaskObject` карточка (`task_card_view.xml`) имеет `layout_marginBottom=4dp`, текст `marginStart=8dp`. Это и есть ширина "контейнера задач" — rails должны выровняться с этим внутренним padding.
- Tasks2 (Cornflower) и Tasks3 (Canary) — палитра меняет фон/акценты, но **заголовок секции остаётся белым текстом** (`Color.WHITE` явно установлен в `bindSectionHeader`). Rails должны быть тоже белыми, видимыми на всех трёх tab-палитрах.

### R1. Top rail (верхняя полоса)
- **Когда рисуется:** для каждой развёрнутой (`!isCurrentlyCollapsed()`) секции.
- **Где рисуется:** сразу под заголовком секции (`section_header_card_view`), **до** первой задачи секции. Если секция пустая — между заголовком и "Empty"-плейсхолдером.
- **Не рисуется:** для свёрнутой секции (только хэдер); для участков "free tasks" вне секций (R3 sprint-001 — задачи без секции не имеют rails).
- **Стиль:**
  - Цвет: `#FFFFFF` (белый, как текст заголовка). Альфа 100%.
  - Толщина: `1dp` (hairline-divider).
  - Горизонтальные margin'ы: `marginStart=8dp`, `marginEnd=8dp` (совпадает с `tv.marginStart=8dp` в `task_card_view.xml` чтобы визуально продолжать линию текста задач).
  - Vertical: `marginTop=2dp` (примыкает к `section_header_card_view.marginBottom=2dp`) и `marginBottom=2dp` до первой задачи.

### R2. Bottom rail (нижняя полоса)
- **Когда рисуется:** для каждой развёрнутой секции, **после** последней задачи секции (или после "Empty"-плейсхолдера если секция пустая). Всегда рисуется в том числе для последней секции списка — предсказуемая структура секции.
- **Не рисуется:** для свёрнутой секции.
- **Стиль:** идентичен top rail (цвет `#FFFFFF`, толщина `1dp`, `marginStart=8dp / marginEnd=8dp`).
- **Vertical margins:** `marginTop=2dp` (после последней задачи / "Empty") и `marginBottom=6dp` (визуальный отступ до следующего хэдера / выше item'ов вне секции).

### R3. Empty placeholder
- **Когда:** секция expanded и `flatten()` не эмиттит ни одного task-item под этой секцией. В default-режиме (done скрыты) секция, в которой все задачи done, считается пустой и показывает "Empty". В show-completed-режиме (task-004 Var A) те же done-задачи отрисуются под хэдером по `sectionId`, и "Empty" не показывается.
- **Текст:** `"Empty"` (строка вынести в `strings.xml` как `R.string.section_empty`).
- **Расположение:** между top rail и bottom rail, центрировано по горизонтали в контейнере задач (`gravity=center_horizontal`).
- **Типографика:** `14sp italic`, цвет `#99FFFFFF` (white alpha 60%), без иконок.
- **Высота строки:** `wrap_content`, vertical padding `12dp` сверху и снизу, чтобы блок дышал между rails.

### R4. Collapsed секция
- Рисуется только `section_header_card_view`. Rails и "Empty" не показываются. Поведение совпадает с текущим (collapsed скрывает inner tasks из `items` через `emitSection`).

### R5. Свободные задачи (free tasks, `sectionId == 0`)
- Rails не рисуются. Empty-плейсхолдер тоже не рисуется (если в папке вообще нет задач — отображается существующий `LinearLayout emptyState` папки, см. `SmallTasksFragment.setEmptyStateIfNeed`).

### R6. Палитра и контраст
- Полосы и текст "Empty" — белые на всех трёх tab'ах (green / blue / yellow). Не зависят от Cornflower/Canary палитры (как и текст заголовка секции, который зашит `Color.WHITE`).
- На Canary (yellow) — оставить белой; если в Phase 7 контраст окажется слабым, поднимется отдельный тикет.

### R7. Реализация rails — отдельные view-types в RecyclerView
- Rails реализуются как **отдельные view-types** в `TasksRecyclerViewAdapter`: `VIEW_TYPE_RAIL_TOP`, `VIEW_TYPE_RAIL_BOTTOM`, `VIEW_TYPE_SECTION_EMPTY` (либо единый `VIEW_TYPE_RAIL` с флагом — выбор на Phase 3).
- `section_header_card_view.xml` **не трогается** — task-002 и task-003 (counter в хэдере) не конфликтуют по файлам.
- Bottom rail — отдельный item, не привязан к last-task-in-section через bind-логику.

### R8. Drag-n-drop поведение
- Rails и Empty-плейсхолдер не draggable: `getMovementFlags=0` в `ItemTouchHelperAttacher`.
- При drag задачи **через** rail-item: задача попадает в ту же секцию (drop над top rail → первой в секции, над bottom rail → последней). Drop-target rail сам не двигается. Детали маппинга drop-position → section-position — на Phase 3 design.

### R9. Collapse/expand анимация
- Используется `notifyDataSetChanged` (как сейчас в `setTasksAndNotifyDataSetChanged`). Оптимизация под `notifyItemRangeRemoved/Inserted` — out-of-scope этой задачи.

### R10. Затронутые файлы (предварительно — финализация на Phase 3)
- **Создать:** `res/layout/section_rail_view.xml` (1dp white line + horizontal margins) и `res/layout/section_empty_card_view.xml` (centered "Empty" textview, italic 14sp #99FFFFFF).
- **Модифицировать:**
  - `TasksRecyclerViewAdapter.java` — новые view types (RAIL_TOP, RAIL_BOTTOM, SECTION_EMPTY), расширить `AdapterItem.Kind` (`RAIL_TOP`, `RAIL_BOTTOM`, `SECTION_EMPTY`), обновить `flatten()` для вставки rails/empty вокруг каждой expanded секции (с учётом критерия из R3 о count task-items под секцией).
  - `AdapterItem.java` — добавить новые kinds.
  - `ItemTouchHelperAttacher.java` — rails и empty не draggable (`getMovementFlags=0`), drop через rail в секцию — тот же edge-case что drop на header (cross-section move).
  - `strings.xml` — добавить `section_empty`.
- **Не трогать:** `section_header_card_view.xml`, `SectionObject.java`, `SectionsRealmController.java`, миграция (Realm-схема без изменений — rails чисто UI).

### R11. Acceptance criteria mapping
- AC1 (rails сверху/снизу expanded секции) → R1 + R2.
- AC2 (Empty placeholder + обе rails при пустой секции) → R3 + R1 + R2.
- AC3 (collapsed → нет rails) → R4.
- AC4 (белые 1-2dp, на всю ширину контейнера задач) → R1 (1dp, marginStart=8dp / marginEnd=8dp) + R2.
- AC5 (одинаково на всех 3 табах) → R6.

## Open Questions

All questions resolved in Phase 2 — see commit history.

## Design

### Approach

Реализуем rails и empty-плейсхолдер как **три новых view-types** в `TasksRecyclerViewAdapter` (вариант "отдельные view-types" из R7, без флага на едином `VIEW_TYPE_RAIL`). Это держит bind-логику простой (каждый ViewHolder отвечает за один layout) и легко расширяемо.

**Новые view-types:**
- `VIEW_TYPE_RAIL_TOP = 3` — белая 1dp линия под заголовком expanded-секции
- `VIEW_TYPE_RAIL_BOTTOM = 4` — белая 1dp линия после последней задачи / Empty
- `VIEW_TYPE_SECTION_EMPTY = 5` — текст "Empty" между rails при пустой секции

(Существующие константы: `VIEW_TYPE_TASK=1`, `VIEW_TYPE_SECTION_HEADER=2`, `VIEW_TYPE_EMPTY=99`, `FOOTER_VIEW=123` — не пересекаются.)

**Новые `AdapterItem.Kind`-ы:** `RAIL_TOP`, `RAIL_BOTTOM`, `SECTION_EMPTY`.

Каждый из трёх новых `AdapterItem` несёт ссылку на owning section (поле `section` уже есть — переиспользуем; либо тонкий новый long-field `sectionId`, см. ниже). Это нужно для drag-n-drop резолва секции при drop над rail-айтемом (R8).

### `flatten()` — структура эмита

Для каждой expanded-секции `S` (под её `SECTION_HEADER`) `flatten()` теперь эмитит:

```
SECTION_HEADER(S)
RAIL_TOP(S)
{ TASK(t1), TASK(t2), ... }    // если есть задачи под S
ИЛИ
SECTION_EMPTY(S)               // если bucket S пуст
RAIL_BOTTOM(S)
```

Для **collapsed**-секции эмитится только `SECTION_HEADER(S)` (без rails, без empty) — как сейчас.

Свободные задачи (`sectionId == 0`) и `DONE_FOOTER` рисуются без изменений (R5).

Реализация — расширение метода `emitSection()`:

```java
private void emitSection(List<AdapterItem> out, SectionObject s, Map<Long, List<TaskObject>> bySection) {
    out.add(AdapterItem.ofSection(s));
    if (!s.isCurrentlyCollapsed()) {
        out.add(AdapterItem.ofRailTop(s));
        List<TaskObject> inner = bySection.get(s.getId());
        if (inner == null || inner.isEmpty()) {
            out.add(AdapterItem.ofSectionEmpty(s));
        } else {
            for (TaskObject t : inner) out.add(AdapterItem.ofTask(t));
        }
        out.add(AdapterItem.ofRailBottom(s));
    }
}
```

Bucket `bySection.get(s.getId())` уже использует все undone-задачи — корректно отражает R3 ("в default-режиме секция с одними done-задачами считается пустой"). В show-completed-режиме (task-004 Var A) расширим bucket — out of scope здесь.

### Modified files (методы/изменения)

**`AdapterItem.java`** (~/projects/todo100android/app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/AdapterItem.java):
- Расширить enum `Kind` тремя значениями: `RAIL_TOP, RAIL_BOTTOM, SECTION_EMPTY`.
- Добавить три статических фабрики: `ofRailTop(SectionObject s)`, `ofRailBottom(SectionObject s)`, `ofSectionEmpty(SectionObject s)`. Каждая хранит `section` в существующем поле (для доступа к `sectionId` через `section.getId()` при drag-n-drop).
- Если в будущем понадобится отвязка от Realm-объекта (detached) — добавить отдельное `public final long sectionId`, но сейчас Realm-ссылка валидна в пределах bind/drag-callback тика и достаточна.

**`TasksRecyclerViewAdapter.java`** (строки ниже — текущие, после edit'а сдвинутся):
- Добавить 3 константы view-types (после строки 51): `VIEW_TYPE_RAIL_TOP = 3`, `VIEW_TYPE_RAIL_BOTTOM = 4`, `VIEW_TYPE_SECTION_EMPTY = 5`.
- `emitSection()` (строки 174–180) — переписать как показано выше.
- `onCreateViewHolder()` (строки 189–206) — добавить три ветки:
  - `VIEW_TYPE_RAIL_TOP` / `VIEW_TYPE_RAIL_BOTTOM` → inflate `R.layout.section_rail_view` → возвращать новый `RailViewHolder` (минимальный, наследник `ViewHolder`, без findViewById).
  - `VIEW_TYPE_SECTION_EMPTY` → inflate `R.layout.section_empty_card_view` → возвращать новый `SectionEmptyViewHolder`.
- `onBindViewHolder()` (строки 214–231) — для трёх новых kinds делать `return;` (статичный layout, ничего не бинд'ить). Можно явно: `if (it.kind == RAIL_TOP || RAIL_BOTTOM || SECTION_EMPTY) return;`.
- `getItemViewType()` (строки 337–346) — расширить `switch`:
  ```java
  case RAIL_TOP: return VIEW_TYPE_RAIL_TOP;
  case RAIL_BOTTOM: return VIEW_TYPE_RAIL_BOTTOM;
  case SECTION_EMPTY: return VIEW_TYPE_SECTION_EMPTY;
  ```
- Новые inner ViewHolders: `RailViewHolder extends ViewHolder` и `SectionEmptyViewHolder extends ViewHolder` (по аналогии с `FooterViewHolder` — без полей).

**`ItemTouchHelperAttacher.java`**:
- `getMovementFlags()` (строки 81–85) — добавить early-return `0` для трёх новых view-types, по аналогии с `FooterViewHolder`:
  ```java
  if (viewHolder instanceof TasksRecyclerViewAdapter.RailViewHolder) return 0;
  if (viewHolder instanceof TasksRecyclerViewAdapter.SectionEmptyViewHolder) return 0;
  ```
- `onMove()` (строки 95–134) — `toItem.kind` теперь может быть `RAIL_TOP / RAIL_BOTTOM / SECTION_EMPTY`. Не блокировать drop (rail — валидная drop-зона), но и не двигать сам rail-айтем (он не draggable, проверено через `getMovementFlags`). При перетаскивании задачи **на** rail-айтем — `notifyItemMoved` + `items.add/remove` отрабатывают как обычно; `containerSectionId` резолвится в `commitMove → computePositionInContainer` через walk-upward к ближайшему `SECTION_HEADER` (логика уже на месте, walk пропускает rails автоматически, т.к. они `!= SECTION_HEADER`).
- Доп. защита: исключить drop **на** `DONE_FOOTER` уже есть; для rails отдельной защиты не нужно — rail-айтем не имеет `task`/`section` для miscompute, walk-upward проходит сквозь него.
- `computeOuterPositionAt()` / `computePositionInContainer()` — не считают rails/empty (они != SECTION_HEADER и != TASK), значит индексы остаются корректными без правок.

**`res/values/strings.xml`**:
- Добавить `<string name="section_empty">Empty</string>`.
- `values-ru/` folder **отсутствует** (проверено `ls app/src/main/res/`) — добавлять русскую локаль не нужно. Если в будущем появится — string-key уже готов.

### New layouts

**`res/layout/section_rail_view.xml`** (новый файл, используется для обоих RAIL_TOP и RAIL_BOTTOM; vertical margins задаются на корне; верхний/нижний rail отличаются только адаптерной позицией):

```xml
<View xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="1dp"
    android:layout_marginStart="8dp"
    android:layout_marginEnd="8dp"
    android:layout_marginTop="2dp"
    android:layout_marginBottom="2dp"
    android:background="#FFFFFF" />
```

Замечание по vertical-margins: top rail хочет `marginTop=2dp / marginBottom=2dp` (R1), bottom rail — `marginTop=2dp / marginBottom=6dp` (R2). Различие в 4dp на нижнем margin'е. Варианты:
- (a) Два отдельных layout-файла (`section_rail_top_view.xml`, `section_rail_bottom_view.xml`) — самый прямолинейный. **Выбираем этот вариант** — простота для статичных layout'ов, имена явные.
- (b) Один layout + программно выставлять `bottomMargin` в `onCreateViewHolder` по типу — больше кода в адаптере.

Итог: создаём **два** layout-файла:
- `res/layout/section_rail_top_view.xml` — `marginTop=2dp`, `marginBottom=2dp`.
- `res/layout/section_rail_bottom_view.xml` — `marginTop=2dp`, `marginBottom=6dp`.

Корневой элемент — `<View>` (минимальный overhead, никаких children, RecyclerView сам аттачит к VH).

**`res/layout/section_empty_card_view.xml`** (новый файл):

```xml
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingTop="12dp"
    android:paddingBottom="12dp"
    android:gravity="center_horizontal"
    android:text="@string/section_empty"
    android:textSize="14sp"
    android:textStyle="italic"
    android:textColor="#99FFFFFF" />
```

Соответствует R3: italic 14sp, white alpha 60%, centered, vertical padding 12dp.

### Drag-n-drop детали (R8)

- `getMovementFlags = 0` для всех трёх новых VH — gestures игнорируются на самих rail/empty.
- При drag задачи **через** rail-айтем: `onMove` пересортирует `items[]` локально (rail остаётся в списке как обычный AdapterItem, но физически не двигается из-за `flags=0` — на самом деле двигается **протаскиваемая задача**, rail "обходит" её).
- В `commitMove → computePositionInContainer` walk-upward от drop-position до ближайшего `SECTION_HEADER` уже игнорирует не-SECTION_HEADER элементы — rails и empty не сбивают логику. Резолв container'а:
  - drop **над** `RAIL_TOP(S)` → walk upward → ближайший SECTION_HEADER = S → containerSectionId = S.id.
  - drop **над** `RAIL_BOTTOM(S)` → walk upward проходит сквозь tasks секции S → SECTION_HEADER = S → containerSectionId = S.id.
  - drop **над** `SECTION_EMPTY(S)` → walk upward → SECTION_HEADER = S.
- После `clearView` вызывается `setTasksAndNotifyDataSetChanged` → `flatten()` пересоздаёт rails/empty в правильных местах. Локальная мутация `items[]` в `onMove` — временная.

**Edge case**: после `onMove` rail может временно оказаться в "неправильной" позиции в `items[]` (например, between two tasks of S because user dragged a task over it). Это не вызывает crash (ViewHolder корректно рендерится — у rails нет state binding), и authoritative re-flatten в `clearView` всё чинит. Принимаем как acceptable trade-off.

### Feature flag

**Не требуется.** Изменения UI-only, low risk:
- Realm-схема не меняется.
- Адаптер расширяется новыми view-types — старые ветки нетронуты.
- Откат — revert одного коммита, никаких миграций.
- Pull-out-of-prod через flag добавил бы сложность без real win — задача мелкая и видимая через QA сразу.

### ADR

**Новый ADR не требуется.** Решение чисто реализационное (UI ratchet existing pattern из task-002 — heterogeneous adapter с view-types). Не вводит нового domain-понятия и не противоречит существующим ADR.

### Risks

1. **Rail как drop-target** (R8 edge): пользователь может ожидать визуального feedback при hover над rail. В текущем drag-API (`ItemTouchHelper.SimpleCallback`) hover-стейт rail не подсвечивается — drop "работает" незаметно. Принимаем; если будет UX-feedback в Phase 7 manual QA — поднимем отдельный тикет.
2. **`flatten()` сложность растёт**: было 3 kind'а — стало 6. Тесты-инварианты (если пишутся в Phase 4) должны покрывать порядок эмита для (a) collapsed S, (b) expanded S с задачами, (c) expanded S пустая, (d) free tasks вокруг секции.
3. **Координация с task-003** (counter в header): task-003 трогает `section_header_card_view.xml` и `bindSectionHeader`. Task-002 их **не трогает**. Конфликта по файлам нет. Sprint orchestrator: задачи можно мерджить независимо.
4. **Drag-`items[]` десинхрон**: локальная мутация `items[]` в `onMove` может оставить rails не в той позиции до `clearView`. Не приводит к crash (rails statеless), authoritative re-flatten фиксит. Принимаем.
5. **AdapterItem `section` хранит Realm-ссылку**: если `SectionObject` инвалидируется между bind и drag (нетипичный сценарий — Realm tx), `section.getId()` упадёт. Текущий код уже полагается на эту инвариантность (`bindSectionHeader` тоже использует `section.getId()`). Не регрессия.

## Tests

Manual QA.

## Implementation

### Files changed

**New layouts:**
- `app/src/main/res/layout/section_rail_top_view.xml` — 1dp white `<View>`, `marginStart/End=8dp`, `marginTop=2dp`, `marginBottom=2dp`, `background=#FFFFFF`.
- `app/src/main/res/layout/section_rail_bottom_view.xml` — same as top rail but `marginBottom=6dp`.
- `app/src/main/res/layout/section_empty_card_view.xml` — `<TextView>` with `text=@string/section_empty`, `gravity=center_horizontal`, `paddingTop/Bottom=12dp`, `textSize=14sp`, `textStyle=italic`, `textColor=#99FFFFFF`.

**Modified:**
- `app/src/main/res/values/strings.xml` — added `<string name="section_empty">Empty</string>` (sprint-002 task-002 block).
- `app/src/main/java/.../small_tasks_fragment/AdapterItem.java` — added three new `Kind` values (`RAIL_TOP`, `RAIL_BOTTOM`, `SECTION_EMPTY`) and three static factories (`ofRailTop`, `ofRailBottom`, `ofSectionEmpty`). Each carries owning `SectionObject` via the existing `section` field.
- `app/src/main/java/.../small_tasks_fragment/TasksRecyclerViewAdapter.java`:
  - New view-type constants: `VIEW_TYPE_RAIL_TOP=3`, `VIEW_TYPE_RAIL_BOTTOM=4`, `VIEW_TYPE_SECTION_EMPTY=5`.
  - `emitSection()` rewritten: for expanded sections emits `SECTION_HEADER` → `RAIL_TOP` → (tasks OR `SECTION_EMPTY` if bucket empty) → `RAIL_BOTTOM`. Collapsed sections emit only `SECTION_HEADER` (unchanged).
  - `onCreateViewHolder()` inflates the three new layouts and returns stateless `RailViewHolder` / `SectionEmptyViewHolder`.
  - `onBindViewHolder()` short-circuits on the three new kinds (static layouts, nothing to bind).
  - `getItemViewType()` switch extended with three new cases.
  - Two new inner classes: `RailViewHolder extends ViewHolder` and `SectionEmptyViewHolder extends ViewHolder` (no fields).
- `app/src/main/java/.../small_tasks_fragment/ItemTouchHelperAttacher.java`:
  - `getMovementFlags()` returns `0` for `RailViewHolder` and `SectionEmptyViewHolder` (non-draggable, per R8).
  - `commitMove()` walk-upward loop extended: if the item directly above the drop position is a rail/empty item, its `section` is used as the destination container (short-circuit). The original `SECTION_HEADER` resolution remains for drops outside any section body.

### Key decisions

- **Two rail layouts vs one + programmatic margin**: chose two static layout files (`section_rail_top_view.xml`, `section_rail_bottom_view.xml`) per Design — keeps adapter code free of margin manipulation; only difference is `marginBottom` (2dp vs 6dp).
- **ViewHolders for rails/empty are stateless**: no `findViewById`, no bind logic — `onBindViewHolder` short-circuits early.
- **Container-resolution short-circuit for rail/empty in drag drop**: walks upward and uses `above.section.getId()` when the first non-task above is a rail/empty item. Falls through to existing `SECTION_HEADER` resolution otherwise. Behaviour for free-task drops is unchanged.
- **Empty placeholder excluded from drag count helpers**: `computeOuterPositionAt` / `computePositionInContainer` count only `SECTION_HEADER` + `TASK` items, so rails/empty are ignored automatically — no changes needed there.
- **No Realm-schema changes**: rails/empty are pure UI structure. Feature flag not used (low-risk, revertable by one commit).

### Build status

`./gradlew assembleDebug` — **BUILD SUCCESSFUL in 9s** (no errors, no warnings introduced).

## Review

All clean. Verified against requirements / design / TBD / safety / edge cases.

- Requirements compliance:
  - R1 top rail: `section_rail_top_view.xml` is `<View>` height `1dp`, `marginStart/End=8dp` (matches `task_card_view` text inset of 8dp), `marginTop=2dp`, `marginBottom=2dp`, `background=#FFFFFF`. Matches AC4 + R1 exactly.
  - R2 bottom rail: `section_rail_bottom_view.xml` identical to top except `marginBottom=6dp`. Matches R2.
  - R3 Empty placeholder: `TextView` with `text=@string/section_empty`, `gravity=center_horizontal`, `paddingTop/Bottom=12dp`, `textSize=14sp`, `textStyle=italic`, `textColor=#99FFFFFF`. Matches R3 exactly.
  - R3 only-undone rule: `bySection` is built from `tasks` (undone) only — sections with only done tasks naturally hit the `inner.isEmpty()` branch and emit `SECTION_EMPTY`. Confirmed by reading bucket construction in `flatten()`.
  - R4 collapsed sections: `emitSection` only emits rails/empty when `!s.isCurrentlyCollapsed()`. Collapsed path unchanged.
  - R5 free tasks: outer-merge loop emits free tasks directly, no rails. Confirmed.
  - R6 palette: `#FFFFFF` and `#99FFFFFF` are hard-coded — survive Cornflower/Canary palette changes.
- Design compliance:
  - 3 new view-type constants (`VIEW_TYPE_RAIL_TOP=3`, `VIEW_TYPE_RAIL_BOTTOM=4`, `VIEW_TYPE_SECTION_EMPTY=5`), 3 new `AdapterItem.Kind` values, 3 factories carrying owning `SectionObject`.
  - Two separate rail layouts (chosen variant a) rather than one + programmatic margin.
  - `section_header_card_view.xml` UNTOUCHED in 51ab10b — last modified in unrelated task-003 commit 3b9c891.
  - `ItemTouchHelperAttacher`: `getMovementFlags=0` for `RailViewHolder` and `SectionEmptyViewHolder`; `commitMove()` walk-upward short-circuits on rail/empty kinds using `above.section.getId()`, falling through to `SECTION_HEADER` resolution as designed.
  - `onBindViewHolder()` short-circuits before any state binding for the three new kinds — stateless rails confirmed.
- TBD discipline: 5 atomic commits on master for task-002 (requirements draft, requirements finalized, design, implementation, log). All prefixed correctly.
- Test adequacy: manual QA only as per spec.
- Safety / view recycling:
  - `RailViewHolder` and `SectionEmptyViewHolder` have zero fields and zero bind logic — no stale state possible on recycle.
  - No `position` lookups inside the new ViewHolders.
  - No Realm access added on main thread — drag-resolve uses `above.section.getId()` and `isCurrentlyCollapsed()` which were already in use elsewhere in this method.
- Edge cases:
  - Section with 0 tasks (undone) → `inner == null || inner.isEmpty()` → `SECTION_EMPTY` between both rails. Correct.
  - Section with only done tasks (default-mode) → bucket is undone-only → empty → `SECTION_EMPTY`. Correct.
  - Drop near section boundary: walk-upward first hits `RAIL_BOTTOM(prev)` or `RAIL_TOP(next)` — both resolve to their owning section's id (RAIL_BOTTOM of prev → prev section; RAIL_TOP of next → next section). This matches expected drop-target semantics.
  - Collapsed-section safety: rail/empty short-circuit checks `!above.section.isCurrentlyCollapsed()` before assigning containerSectionId; rails are never emitted for collapsed sections anyway, so this is defensive belt-and-suspenders, but it doesn't introduce bugs.
- Readability:
  - Factory methods `ofRailTop/ofRailBottom/ofSectionEmpty` match the existing `ofSection/ofTask/doneFooter` style.
  - ViewHolders are minimal (single-line constructor). No dead code added.
  - Comments tagged `sprint-002 task-002:` make the intent traceable.
- Build: `assembleDebug BUILD SUCCESSFUL in 9s` (recorded in task md).

## Manual verification

<заполняется на Phase 7>

Phase 7 visual tweak: rails now span full width (marginStart/End=0); symmetric 8dp spacing top and bottom around each rail. Approved values to be confirmed by user. Commit: 0730e07.
