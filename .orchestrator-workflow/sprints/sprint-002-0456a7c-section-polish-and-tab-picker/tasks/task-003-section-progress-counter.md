# Task 003 — Счётчик прогресса справа от названия секции

- **Sprint:** sprint-002-0456a7c
- **Task token:** 31310a0
- **Project:** todo100android
- **Cross-project ref:** —
- **Feature flag:** not required (визуальное изменение, низкорискованное, revert коммитом)
- **Status:** pending

## Acceptance criteria

- [ ] На той же горизонтальной линии, что и название секции (`section_header_card_view`), у правого края экрана отображается счётчик в формате **`выполнено/всего`** (например `2/5`).
- [ ] Подсчёт: **всего** — все задачи в этой секции; **выполнено** — задачи с `isCompleted == true` (см. модель TaskObject).
- [ ] Считается одинаково и в свёрнутом, и в развёрнутом состоянии секции (изменение при collapse/expand не происходит).
- [ ] Счётчик обновляется при: добавлении задачи в секцию / удалении / тапе чекбокса (done/undone) / перемещении задачи между секциями.
- [ ] Стиль текста — нейтральный (текст белый или светло-серый, аналог "Done 5 tasks" внизу экрана). Размер чуть меньше заголовка секции.
- [ ] При нулевом total показывается `0/0` (можно опустить — определить на Phase 2).

## Requirements

> **Feature flag:** not required. Изменение визуальное, низкорискованное, легко откатывается через revert коммита.

### Формат и расчёт
- **R1. Формат счётчика:** `X/Y`, где `X` = число выполненных задач в секции, `Y` = общее число задач в секции. Пример: `2/5`. Разделитель — обычный слэш `/`, без пробелов вокруг.
- **R2. Что считается «задачей секции»:** все `TaskObject` с `taskFolderId == parentFolder.id` И `sectionId == section.id` (живые managed-объекты Realm, не tombstoned). Cycling-задачи, priority-задачи, accumulation-задачи — считаются как обычные.
- **R3. Что считается «выполненной» (X):** `TaskObject.isDone() == true` (поле `done` из модели; см. `TaskObject.java:100`). «Particullary done» (накопительный прогресс через `setTaskDoneOrParticullaryDone`) не выделяется в отдельный класс — учитываем только финальное `done == true`. Cycling-задачи, которые автоматически разчекаются в новый день, естественно уменьшат `X` при следующем `rebuildItems()`.
- **R4. Счётчик считается одинаково и в collapsed, и в expanded состоянии секции.** Collapse/expand не меняет цифры, только видимость списка задач.
- **R5. Source-of-truth:** счётчик читает данные из Realm заново при каждом ребилде items (`rebuildItems()` / `flatten()` в `TasksRecyclerViewAdapter`). Никакого in-memory кеша на стороне адаптера — это упростит инвалидацию.

### Расположение и стиль
- **R6. Позиция:** в той же горизонтальной строке, что и chevron + section_name, у **правого края контейнера** (с учётом существующего `paddingEnd="12dp"` родителя `section_header_root`). Вертикально — центрирован относительно текста имени (тот же `gravity:center_vertical`).
- **R7. Layout-механика:** добавляется новый `TextView` (id `section_progress`) как последний child `LinearLayout` в `section_header_card_view.xml`. Имя секции (`section_name`) сохраняет `layout_weight=1`, что заставит его сжиматься/эллипсизироваться при длинном имени, а счётчик всегда остаётся справа. `View` с id `section_divider` (сейчас `visibility:gone`) остаётся перед счётчиком — task-003 не зависит от его судьбы, т.к. task-002 layout header'а не трогает (см. R15).
- **R8. Типографика:**
  - `textSize` — `12sp` (на 2sp меньше, чем `14sp` у `section_name`).
  - `textStyle` — `normal` (не bold; не `textAllCaps`; не моноширинный — изменения редкие, jitter не критичен).
  - `textColor` — полупрозрачный белый `#B3FFFFFF` (alpha 70%). Визуально опускает счётчик относительно жирного uppercase имени секции, акцент остаётся на названии. При активной палитре (Cornflower/Canary) — без подмены.
  - `maxLines` — `1`. Эллипс не нужен (формат `X/Y` короткий; даже трёхзначные значения занимают <8 символов).
  - `paddingStart` — `8dp`, чтобы был зазор от имени, если оно вплотную упёрлось в счётчик.

### Доступность
- **R8a. ContentDescription для TalkBack:** на счётчике установить `contentDescription` через локализованную строку `R.string.section_progress_a11y` формата `"%1$d of %2$d tasks done"` (англ.) и соответствующего перевода ru. Имя секции остаётся с собственным contentDescription, не объединяется.

### Триггеры обновления
- **R9. Счётчик пересчитывается, и UI обновляется при следующих событиях** (всё проходит через существующий `SmallTasksFragment.setTasksAndNotifyDataSetChanged()` → `adapter.rebuildItems()` → `notifyDataChanged()`):
  1. Тап на чекбокс задачи (done/undone) — путь `bindTask → checkBox.OnClickListener → smallTasksFragment.notifyDataChanged()`.
  2. Добавление новой задачи в секцию (`TasksRealmController.addTask` → fragment refresh).
  3. Удаление задачи (одиночное и multi-select через `TaskActionModeCallback`).
  4. Перемещение задачи между секциями через drag-n-drop (`SectionsRealmController.moveTaskToSection` / `reorderItems`).
  5. Перемещение задачи в секцию из «свободной зоны» (`sectionId 0 → S.id`) и наоборот.
  6. Удаление секции (`SectionsRealmController.deleteSection` сбрасывает sectionId детей в 0; секция исчезает, поэтому счётчик исчезает вместе с ней — пересчёт не требуется, но re-flatten происходит).
  7. Edit секции (rename / toggle collapsedByDefault) — счётчик не зависит от этих полей, но re-flatten всё равно случится.
- **R10. Не триггерит обновление:** изменение priority задачи, изменение текста, изменение count/accumulation. (Они не влияют на X/Y, поэтому если адаптер не сделает rebuild — норма.)

### Edge cases
- **R11. Пустая секция (`Y == 0`):** счётчик показывается как `0/0` **всегда**. Скрывать счётчик нельзя — пустая секция в этом спринте получает Empty placeholder (см. task-002), и видимый `0/0` явно показывает, что счётчик «жив», просто задач нет.
- **R12. Все задачи выполнены (`X == Y`, `Y > 0`):** счётчик показывается как есть (например `5/5`). Никакой особой подсветки или замены текста в этом тикете — только цифры.
- **R13. Длинное имя секции:** имя эллипсизируется (поведение из `section_name`: `maxLines=1`, `ellipsize=end`), счётчик не сжимается и всегда видим.
- **R14. Невалидная секция в Realm:** в `bindSectionHeader` уже стоит guard `section != null && section.isValid()`. Если объект отозван — биндинг ранний return, счётчик не показывается (виновник — race с deleteSection; следующий refresh уберёт строку из списка).

### Координация с task-002 (rails + empty)
- **R15.** task-002 использует **отдельные view types** в адаптере (rails + empty placeholder рендерятся как самостоятельные RecyclerView items) и **не трогает** `section_header_card_view.xml`. task-003 имеет полный контроль над layout-ом header'а — мерж-конфликта нет, задачи могут идти параллельно.
- **R16.** Атомарность коммитов: task-002 коммитит изменения адаптера и новые layout-ы для rails/empty; task-003 коммитит только `section_header_card_view.xml` + bind-логику счётчика в `TasksRecyclerViewAdapter.bindSectionHeader` + строку в `strings.xml`. Пересечения по файлам нет.
- **R17.** Расчёт `X/Y` использует только sectionId/done — не зависит от rails или Empty placeholder из task-002. Логика независима.

### Scope
- **R18. Применяется ко всем трём task-табам** (Tasks pos 1, Tasks2 pos 2, Tasks3 pos 3 после task-003 из sprint-001). Notes — нет (там секций нет).
- **R19. На «свободных» задачах (без секции, `sectionId == 0`) счётчик не показывается** — у них нет header-строки, считать нечего.

## Open Questions

All questions resolved in Phase 2 — see commit history.

## Design

### Approach

Добавить новый `TextView` (`@+id/section_progress`) как последний child корневого `LinearLayout` в `section_header_card_view.xml`. Биндить его в `TasksRecyclerViewAdapter.bindSectionHeader(...)` строкой формата `X/Y`, где `X` — число выполненных задач секции, `Y` — общее число задач секции.

**Ключевая находка (data source-of-truth).** В default-режиме адаптера поле `tasks` содержит **только не-done** задачи (источник — `SmallTasksFragment.setTasksAndNotifyDataSetChanged()` → `TasksRealmController.getNotDoneTasks(folderId)`, `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java:204-211`). Поле `doneTasks` адаптера присутствует, но используется только для футера. В режиме `isAllTaskShowing` (после тапа на «Done N tasks») поле `tasks` получает `getTasks(folderId)` (все). Семантика счётчика по R4/R9 — `Y = ВСЕ задачи секции независимо от режима`, поэтому считать только из `adapter.tasks` нельзя: в default-моде получим `X = 0`, `Y = count(undone)` — неверно.

**Решение: предрасчёт счётчиков в `rebuildItems()`.** В `TasksRecyclerViewAdapter` добавить приватное поле:

```
/** sectionId -> [doneCount, totalCount]; rebuilt on every flatten(). */
private Map<Long, int[]> sectionCounts = new HashMap<>();
```

Заполнять его внутри `flatten()` (или в начале `rebuildItems()`) одним запросом по таблице задач папки:

```
sectionCounts.clear();
if (folderId != 0) {
    RealmResults<TaskObject> all = TasksRealmController.getTasks(folderId); // already exists, returns all (done+undone)
    for (TaskObject t : all) {
        long sid = t.getSectionId();
        if (sid == 0) continue; // free zone — нет header'а (R19)
        int[] pair = sectionCounts.get(sid);
        if (pair == null) { pair = new int[]{0, 0}; sectionCounts.put(sid, pair); }
        pair[1]++;                 // total
        if (t.isDone()) pair[0]++; // done
    }
}
```

В `bindSectionHeader` после `holder.tvName.setText(...)`:

```
int[] pair = sectionCounts != null ? sectionCounts.get(sectionId) : null;
int done = pair == null ? 0 : pair[0];
int total = pair == null ? 0 : pair[1];
holder.tvProgress.setText(done + "/" + total);
holder.tvProgress.setContentDescription(
        activity.getString(R.string.section_progress_a11y, done, total));
```

**Почему именно так:**
- Один Realm-запрос на rebuild вместо N (по числу секций) при binding'е.
- Корректно в обоих режимах (default + showAllTasks): источник = таблица задач, а не отфильтрованный `tasks`.
- Авто-обновление: `rebuildItems()` вызывается из `setTasksAndNotifyDataSetChanged()`, который дёргается из всех точек R9 (тап чекбокса, add/delete/move task, toggle showAllTasks). Никаких дополнительных observer'ов не нужно.
- Кеш живёт ровно один цикл flatten — нет stale-данных, нет ручной инвалидации (R5).

**Альтернативы, отвергнутые:**
- Запрос по секции внутри `bindSectionHeader` (по одному `equalTo("sectionId", s.id)` на каждый header) — лишние Realm-запросы при скролле/recycle, хотя они и lazy.
- Передавать map из `SmallTasksFragment` через сеттер — лишний контракт между fragment и adapter; adapter уже знает `folderId` через `smallTasksFragment.getTasksFolderId()` и сам способен посчитать.
- Использовать `adapter.tasks + adapter.doneTasks` объединённо — в `showAllTasks` режиме `tasks` уже содержит done, получим двойной учёт. Хрупко.

### Affected files

1. `app/src/main/res/layout/section_header_card_view.xml`
   - Удалить (или оставить — pointwise) пустой `View @+id/section_divider` (он `width=0/height=0/gone`, не мешает; R7 говорит — оставить); добавить **после** него (или после `section_name`, перед `section_divider` — порядок layout'а не критичен, т.к. divider gone+0dp) новый `TextView`:
     - `id` = `@+id/section_progress`
     - `layout_width="wrap_content"`, `layout_height="wrap_content"`
     - `layout_marginStart="8dp"` (R8: paddingStart 8dp от имени)
     - `textSize="12sp"` (R8)
     - `textStyle="normal"`, без `textAllCaps`
     - `textColor="#B3FFFFFF"` (R8, alpha 70%)
     - `maxLines="1"`
     - `gravity="center_vertical"` (наследуется от родителя, но явно)
     - `tools:text="0/0"` (для preview)
   - `section_name` с `layout_weight=1` уже сжимается при длинном имени (R13) — менять не надо.

2. `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/TasksRecyclerViewAdapter.java`
   - Добавить поле `private Map<Long, int[]> sectionCounts = new HashMap<>();` рядом с `items`.
   - В `flatten()` (вверху, после получения `folderId`, до построения buckets): пересобрать `sectionCounts` через `TasksRealmController.getTasks(folderId)` (см. snippet выше). В fall-back ветке (`folderId == 0`) — `sectionCounts.clear()` (всё равно header'ов не будет).
   - В `SectionHeaderViewHolder`: добавить `public final TextView tvProgress;` + `findViewById(R.id.section_progress)`.
   - В `bindSectionHeader(...)`: после установки имени — выставить `tvProgress.setText("X/Y")` и `setContentDescription(...)` (см. snippet). При невалидной секции (`!section.isValid()`) ранний return уже есть — счётчик не выставляется, всё ОК (R14).

3. `app/src/main/res/values/strings.xml`
   - Добавить строку:
     ```xml
     <string name="section_progress_a11y">%1$d of %2$d tasks done</string>
     ```
   - Файла `values-ru/strings.xml` сейчас **нет** в проекте (проверено: `find` показал только `values/strings.xml`). Для соблюдения R8a и единого стиля с остальными строками (которые тоже только на английском в `values/`) — ru-перевод **не добавляется**, ограничиваемся английской дефолтной строкой. Если orchestrator потребует ru-перевод — отдельный low-risk follow-up, не блокирует task-003.

4. `SmallTasksFragment.java` — **не меняется**. Все триггеры обновления (R9) уже идут через `setTasksAndNotifyDataSetChanged()` → `adapter.rebuildItems()`, чего достаточно для пересчёта `sectionCounts`.

### API / contracts

- Новых public методов адаптера нет. Поле `sectionCounts` — private, internal.
- Контракт layout'а: `SectionHeaderViewHolder` теперь требует наличия `R.id.section_progress` в `section_header_card_view.xml`. Других мест, инфлейтящих этот layout, в проекте нет (проверим grep на этапе implementation — если найдутся, добавим guard `if (tvProgress != null)`).
- `R.string.section_progress_a11y` — новый ресурс. Формат `%1$d of %2$d tasks done` — placeholder'ы позиционные, future-proof для ru.

### Feature flag

Не требуется (R-flag, p.21 task-md). Изменение визуальное, низкорискованное, легко откатывается через revert коммита.

### ADR

Новый ADR **не нужен**. Подход не вводит новых архитектурных решений — это локальное UI-расширение существующего header'а + один Realm-запрос в существующем rebuild-цикле.

### Risks & mitigations

1. **R-1. Двойной учёт в `showAllTasks`-режиме.** Источник `TasksRealmController.getTasks(folderId)` возвращает все задачи папки независимо от режима адаптера. Mitigation: единый источник — таблица Realm, а не `adapter.tasks`. Подтверждено выше.
2. **R-2. `taskFolderId`-фильтр.** Внутри `getTasks(folderId)` уже идёт `getFolderTasksRealmListFromFolder(folderId)` (`TasksRealmController.java:75`) — фильтр уже корректный, никакие чужие задачи не попадают.
3. **R-3. Race с `deleteSection` → tombstoned section.** `bindSectionHeader` уже делает `if (section == null || !section.isValid()) return;`. Если секция удалена между rebuildItems() и onBindViewHolder — header не отрисуется, следующий `notifyDataChanged()` уберёт строку. Безопасно (R14).
4. **R-4. Cycling-задачи в новый день.** При следующем `rebuildItems()` они уже будут с `done == false` — `X` уменьшится автоматически (R3). Mitigation: не нужна — это поведение запрошено в acceptance criteria.
5. **R-5. Сжатие имени при длинной строке.** `section_name` уже имеет `layout_weight=1` + `ellipsize=end` + `maxLines=1`; новый `section_progress` — `wrap_content` без weight, поэтому всегда отрисуется справа полностью (R13).
6. **R-6. Performance.** Дополнительный `getTasks(folderId)` в `flatten()` — это один Realm-запрос, lazy, на той же папке, на которой уже строится `bySection`-bucket. Для типичных папок (десятки задач) — O(N) overhead пренебрежимо мал. На случай очень больших папок — `flatten()` уже O(N), вторая итерация тех же данных не меняет асимптотику.
7. **R-7. Free-зона (`sectionId == 0`).** В `sectionCounts` ключ `0L` пропускается (R19). Если задача переходит в free-зону — её sectionId сбрасывается в 0 и она перестаёт учитываться (правильно, у free-задач нет header'а).
8. **R-8. Отсутствие `section_progress` в других layout'ах.** Перед implementation проверить `grep -r section_header_card_view` — если layout инфлейтится где-то ещё, добавить `if (tvProgress != null)` в bind. На текущий момент известна одна точка инфлейта — `onCreateViewHolder`, `TasksRecyclerViewAdapter.java:196`.

## Tests

Manual QA.

## Implementation

### Files changed

1. `app/src/main/res/layout/section_header_card_view.xml`
   - Added `xmlns:tools` namespace on root `LinearLayout` (for `tools:text` preview).
   - Added new `TextView @+id/section_progress` as the **last child** of the root `LinearLayout`, after the existing `View @+id/section_divider` (per Design R7 — divider is `width=0/height=0/gone`, so position is irrelevant for visual order).
   - Attributes per R8: `layout_width="wrap_content"`, `layout_height="wrap_content"`, `paddingStart="8dp"`, `gravity="center_vertical"`, `textSize="12sp"`, `textStyle="normal"`, `textColor="#B3FFFFFF"` (semi-transparent white, alpha 70%), `maxLines="1"`, `tools:text="0/0"`.
   - `section_name` keeps `layout_weight=1` — long names ellipsize, counter stays right-aligned (R13).

2. `app/src/main/res/values/strings.xml`
   - Added `<string name="section_progress_a11y">%1$d of %2$d tasks done</string>` under the sprint-002 task-003 section.
   - English-only (no `values-ru/` exists in the project — confirmed via find).

3. `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/TasksRecyclerViewAdapter.java`
   - Added private field `private Map<Long, int[]> sectionCounts = new HashMap<>();` next to `items`.
   - In `flatten()`: at the top, after computing `folderId`, before the early-return fallback branch, populate `sectionCounts` by iterating `TasksRealmController.getTasks(folderId)` (all tasks, done + undone). Skip `sectionId == 0` (free-zone per R19). For each task increment `total`; if `isDone()` increment `done`. Wrapped in `if (folderId != 0)` so folderless adapters get an empty map. `sectionCounts.clear()` runs unconditionally on every rebuild — no stale data.
   - In `SectionHeaderViewHolder`: added `public final TextView tvProgress;` initialized via `findViewById(R.id.section_progress)`.
   - In `bindSectionHeader(...)`: after `tvChevron.setText(...)`, look up `sectionCounts.get(sectionId)`, fall back to `[0,0]` if missing, set `tvProgress.setText(done + "/" + total)` and `setContentDescription(activity.getString(R.string.section_progress_a11y, done, total))`. Guarded by `if (holder.tvProgress != null)` for forward-compat (R-8).

### Wave 1 verification

- Wave 1 view types `VIEW_TYPE_RAIL_TOP=3`, `VIEW_TYPE_RAIL_BOTTOM=4`, `VIEW_TYPE_SECTION_EMPTY=5` and `AdapterItem.Kind`s `RAIL_TOP / RAIL_BOTTOM / SECTION_EMPTY` are untouched.
- Wave 1's `emitSection()` flow (header → rail_top → tasks/empty → rail_bottom) is untouched — `sectionCounts` is populated **before** the bucketing/emit phase.

### Build

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL in 10s** (35 actionable tasks: 9 executed, 26 up-to-date). No new warnings related to this change.

## Review

<заполняется на Phase 6>

## Manual verification

<заполняется на Phase 7>
