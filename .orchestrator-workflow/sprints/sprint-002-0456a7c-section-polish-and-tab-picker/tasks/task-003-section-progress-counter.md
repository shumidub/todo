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

<заполняется на Phase 3>

## Tests

Manual QA.

## Implementation

<заполняется на Phase 5>

## Review

<заполняется на Phase 6>

## Manual verification

<заполняется на Phase 7>
