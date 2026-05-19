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
  - Толщина: `1dp` (рекомендация — субтильно, как hairline-divider; см. Open Question Q1).
  - Горизонтальные margin'ы: `marginStart=8dp`, `marginEnd=8dp` (совпадает с `tv.marginStart=8dp` в `task_card_view.xml` чтобы визуально продолжать линию текста задач).
  - Vertical: рекомендуется `marginTop=2dp` (примыкает к `section_header_card_view.marginBottom=2dp`) и `marginBottom=2dp` до первой задачи.

### R2. Bottom rail (нижняя полоса)
- **Когда рисуется:** для каждой развёрнутой секции, **после** последней задачи секции (или после "Empty"-плейсхолдера если секция пустая).
- **Не рисуется:** для свёрнутой секции; перед `done_footer`-ом, если только это не нижняя rail секции.
- **Стиль:** идентичен top rail (цвет, толщина, горизонтальные margins).
- **Vertical margins:** рекомендуется `marginTop=2dp` (после последней задачи / "Empty") и `marginBottom=6dp` (визуальный отступ до следующего хэдера / выше item'ов вне секции). Точные значения — Open Question Q2.

### R3. Empty placeholder
- **Когда:** секция expanded и в ней **нет ни одной задачи** (ни done, ни undone — определяется по `tasks` в адаптере; uncertain about done tasks — см. Q3).
- **Текст:** `"Empty"` (строка вынести в `strings.xml` как `R.string.section_empty`).
- **Расположение:** между top rail и bottom rail, центрировано по горизонтали в контейнере задач.
- **Типографика:** "минорный текст" — цвет `Color.WHITE` с альфой ~60% (либо `#FFFFFFFF` с `alpha=0.6`), italic, размер `14sp` (на 2sp меньше body-task 16sp). Точные значения — Open Question Q4.
- **Высота строки:** `wrap_content`, vertical padding ~`12dp` сверху/снизу, чтобы блок дышал между rails.

### R4. Collapsed секция
- Рисуется только `section_header_card_view`. Rails и "Empty" не показываются. Поведение совпадает с текущим (collapsed скрывает inner tasks из `items` через `emitSection`).

### R5. Свободные задачи (free tasks, `sectionId == 0`)
- Rails не рисуются. Empty-плейсхолдер тоже не рисуется (если в папке вообще нет задач — отображается существующий `LinearLayout emptyState` папки, см. `SmallTasksFragment.setEmptyStateIfNeed`).

### R6. Палитра
- Полосы и текст "Empty" — белые на всех трёх tab'ах (green/blue/yellow). Не зависят от Cornflower/Canary палитры (как и текст заголовка секции, который зашит `Color.WHITE`).
- Контраст: на yellow (Canary) tab белая полоса 1dp на ярко-жёлтом фоне может быть слабо видна — Open Question Q5.

### R7. Координация с task-003 (counter в хэдере)
- Task-003 добавляет `TextView` справа от названия секции **внутри** `section_header_card_view.xml` (counter `2/5`). Это в **хэдере**, rails — **под** хэдером. Файл `section_header_card_view.xml` будет шарен между task-002 и task-003.
- **Решение по координации (для Phase 3):**
  - Если rails реализуются как **отдельные view-types в RecyclerView** (см. Q6) — конфликта нет, layout хэдера не трогается task-002.
  - Если rails реализуются **внутри layout-обёртки** хэдера (top rail в `section_header_card_view.xml`) — task-002 и task-003 трогают один файл, требуется sequential merge (rebase второго).
- **Рекомендация:** реализовать rails как отдельные view-types (`VIEW_TYPE_RAIL_TOP=3`, `VIEW_TYPE_RAIL_BOTTOM=4` или единый `VIEW_TYPE_RAIL=3` с флагом). Это:
  - Не трогает `section_header_card_view.xml` → нет конфликта с task-003.
  - Симметрично: bottom rail тоже как отдельный item, не привязан к last-task-in-section.
  - Корректно работает с drag-n-drop (rails не должны быть draggable — `getMovementFlags=0` в `ItemTouchHelperAttacher`).

### R8. Затронутые файлы (предварительно — финализация на Phase 3)
- **Создать:** `res/layout/section_rail_view.xml` (1dp white line + horizontal margins) и `res/layout/section_empty_card_view.xml` (centered "Empty" textview).
- **Модифицировать:**
  - `TasksRecyclerViewAdapter.java` — новые view types (RAIL_TOP, RAIL_BOTTOM, EMPTY_PLACEHOLDER), расширить `AdapterItem.Kind` (`RAIL_TOP`, `RAIL_BOTTOM`, `SECTION_EMPTY`), обновить `flatten()` для вставки rails/empty вокруг каждой expanded секции.
  - `AdapterItem.java` — добавить новые kinds.
  - `ItemTouchHelperAttacher.java` — rails и empty не draggable (`getMovementFlags=0`), drop через rail в секцию — тот же edge-case что drop на header (cross-section move).
  - `strings.xml` — добавить `section_empty`.
- **Не трогать:** `section_header_card_view.xml`, `SectionObject.java`, `SectionsRealmController.java`, миграция (Realm-схема без изменений — rails чисто UI).

### R9. Acceptance criteria mapping
- AC1 (rails сверху/снизу expanded секции) → R1 + R2.
- AC2 (Empty placeholder + обе rails при пустой секции) → R3 + R1 + R2.
- AC3 (collapsed → нет rails) → R4.
- AC4 (белые 1-2dp, на всю ширину контейнера задач) → R1 (1dp, marginStart=8dp / marginEnd=8dp).
- AC5 (одинаково на всех 3 табах) → R6.

## Open Questions

1. **Q1 — толщина rail.**
   **Answer (default):** `1dp` hairline.
2. **Q2 — vertical margins вокруг rails.**
   **Answer (default):** Top rail `marginTop=2dp / marginBottom=2dp`; bottom rail `marginTop=2dp / marginBottom=6dp`. Корректировка после визуального просмотра в Phase 7.
3. **Q3 — "пустая секция" учитывает done tasks?**
   **Answer (default — coordinated with task-004 Q1):** "Empty" показывается ⇔ `flatten()` не эмиттит ни одной task-item под секцией. В default-режиме (done скрыты) секция, в которой все задачи done, считается пустой и показывает "Empty". В show-completed-режиме те же done-задачи отрисуются под хэдером по `sectionId`, и "Empty" не показывается. Это согласовано с task-004 Var A.
4. **Q4 — типографика "Empty".**
   **Answer (default):** `14sp italic`, цвет `#99FFFFFF` (white alpha 60%), `gravity=center_horizontal`, vertical padding `12dp` сверху и снизу. Без иконок.
5. **Q5 — контраст на Canary (yellow) tab.**
   **Answer (default):** Оставить белой везде (per AC4). При слабом контрасте в Phase 7 — поднимем отдельный тикет.
6. **Q6 — реализация rails: view type vs часть header layout.**
   **Answer (default):** Отдельные view types в RecyclerView (`VIEW_TYPE_RAIL_TOP`, `VIEW_TYPE_RAIL_BOTTOM`, `VIEW_TYPE_SECTION_EMPTY`). `section_header_card_view.xml` не трогается — task-002 и task-003 не конфликтуют.
7. **Q7 — bottom rail у последней секции списка.**
   **Answer (default):** Всегда рисовать. Предсказуемая структура секции.
8. **Q8 — drag задачи через rail.**
   **Answer (default):** Rails — `getMovementFlags=0` (не двигаются и не accept drop как самостоятельные цели). При drag над rail item — задача попадает в ту же секцию (top rail → первой в секции, bottom rail → последней). Детали логики drop-target — на Phase 3 design.
9. **Q9 — анимация при collapse/expand.**
   **Answer (default):** `notifyDataSetChanged` (как сейчас в `setTasksAndNotifyDataSetChanged`). Оптимизация под `notifyItemRangeRemoved/Inserted` — out-of-scope.

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
