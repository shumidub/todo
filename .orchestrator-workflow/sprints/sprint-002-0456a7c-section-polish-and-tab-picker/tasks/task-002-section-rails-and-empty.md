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

<заполняется на Phase 3>

## Tests

Manual QA.

## Implementation

<заполняется на Phase 5>

## Review

<заполняется на Phase 6>

## Manual verification

<заполняется на Phase 7>
