# Task 004 — Фикс: выполненные задачи автоматически скрываются вне режима show-completed

- **Sprint:** sprint-002-0456a7c
- **Task token:** 5d0d033
- **Project:** todo100android
- **Cross-project ref:** —
- **Feature flag:** (определить на Phase 3, возможно не нужен — это bugfix)
- **Status:** pending

## Acceptance criteria

- [ ] При выключенном режиме "показывать выполненные задачи" (default) — задачи с `isCompleted == true` **скрыты** в списке задач категории.
- [ ] При включённом режиме — выполненные задачи **видны** (как сейчас работает на скриншоте Image #2 — там видно `2/2` и галочку).
- [ ] Переключение режима (где бы оно ни было — toolbar/меню) работает: список перерисовывается мгновенно.
- [ ] Поведение совпадает на всех трёх табах (Tasks/Tasks2/Tasks3).
- [ ] Сломалось после sprint-001 (введение sections и BottomSheet) — найти причину в коммитах `5cbcea9` (task-003 Wave 1) или `7a9d7ae` (task-001+task-002 Wave 2).
- [ ] Регрессия должна быть зафиксирована и в случае комбинации с секциями (внутри секций тоже выполненные скрыты в default-режиме).

## Requirements

### Контекст (как сейчас в коде)

- Тоггл "показывать выполненные" — это **футер-строка** `task_card_view_done_tasks` ("Done N tasks"), клик по которой вызывает `SmallTasksFragment.showAllTasks()` (см. `SmallTasksFragment.java:278-306`). Меню для этого нет (в `MainActivity.onCreateOptionsMenu` есть только `dayScopeMenu`).
- Состояние тоггла хранится в `SmallTasksFragment.isAllTaskShowing` (boolean). По умолчанию `false` (выставляется в `onViewCreated` строка 151).
- При `isAllTaskShowing == false`: `tasks = TasksRealmController.getNotDoneTasks(folderId)` (фильтр `equalTo("done", false)` корректен — см. `TasksRealmController.java:81-86`).
- При `isAllTaskShowing == true`: `tasks = TasksRealmController.getTasks(folderId)` (все done+undone, сортировка `done ASC, position ASC` — done тонут вниз).
- `showAllTasks()` пересоздаёт адаптер с новым набором `tasks` → конструктор адаптера вызывает `rebuildItems()` → `flatten()` корректно перестраивает список.

### R1. Default-режим (`isAllTaskShowing == false`)

- Задачи с `isDone() == true` **не отображаются** ни как свободные (`sectionId == 0`), ни внутри секций.
- Это работает за счёт того, что `tasks` (источник для `flatten()`) уже отфильтрован Realm-запросом до undone-only.
- Футер "Done N tasks" виден всегда, если в категории есть хотя бы одна задача (любого статуса) — рендерится по `it.kind == DONE_FOOTER` в `onBindViewHolder`, текст `Done <smallTasksFragment.doneTasks.size()> tasks`.

### R2. Show-completed-режим (`isAllTaskShowing == true`)

- Done и undone задачи **обе** видны в списке.
- Порядок: внутри каждой секции сначала undone (отсортированы по `position`), потом done (отсортированы по `position`). Снаружи (`sectionId == 0`) — аналогично. Это даёт сортировку Realm `done ASC, position ASC` из `getTasks(folderId)`.
- Done-задачи **внутри секции** должны идти **в самом низу секции**, после всех её undone (как и сейчас работает через сортировку `done ASC` в Realm-запросе + bucketing по `sectionId` в `flatten`).
- Done-задачи без секции (`sectionId == 0`) — в самом низу внешнего списка, перед футером.
- Тоггл выключается повторным кликом по тому же футеру.

### R3. Переключение режима — мгновенный rerender

- При клике на футер список перерисовывается сразу (`showAllTasks()` пересоздаёт адаптер и `rvTasks.setAdapter(...)` — работает уже сейчас).

### R4. Все три таба (Tasks/Tasks2/Tasks3)

- Поведение идентично на всех трёх вкладках. `FolderSlidingPanelFragment` каждой вкладки внутри использует `SmallTasksFragment` — фильтрация done живёт **в одном месте** (адаптер + `tasks` поле), палитра не влияет на логику.

### R5. Done-задачи внутри секций

- В default-режиме done-задачи скрыты — **даже если** секция содержит только done-задачи (т.е. ни одной undone). В этом случае секция формально пустая для `flatten()` (см. task-002 этого спринта, Q3 — `bySection.get(sectionId) == null` если нет undone). Интеракция с placeholder "Empty" из task-002 — **Open Question** (Q1 ниже).

### R6. Done-фильтр не зависит от collapsed/expanded

- Свёрнутые секции и так не показывают inner-tasks (см. `emitSection`, `!s.isCurrentlyCollapsed()`). Done-фильтр работает ортогонально: даже если секция expanded и в ней есть только done-задачи, в default-режиме показывается только хэдер (+ опционально "Empty" — см. R5/Q1).

### Diagnosis hints

**Регрессия — корневая причина (предварительно):**

- `SmallTasksFragment.notifyDataChanged()` (строки 252-275) вызывает `tasksRecyclerViewAdapter.notifyDataSetChanged()`, но **НЕ вызывает `rebuildItems()`**. Соответственно `items: List<AdapterItem>` остаётся stale-снимком.
- До Wave 2 (commit `7a9d7ae`) адаптер биндил элементы напрямую из `tasks.get(position)` — а `tasks` это **`RealmResults` proxy с фильтром `done=false`**, живой и сжимающийся при изменении флага `done`. То есть как только пользователь чекал task → `setTaskDoneOrParticullaryDone` → Realm обновляется → `tasks.size()` уменьшается → `notifyDataSetChanged` → задача исчезает.
- После Wave 2 адаптер биндит из `items` (snapshot-копия `ArrayList<AdapterItem>`, построенная в `flatten()`). Этот snapshot **не обновляется** автоматически — нужен явный `rebuildItems()` (или конструктор нового адаптера).
- Результат: после чека task пользователь видит её всё ещё в списке, но уже со state `done == true` (т.е. зачёркнута / с галочкой) — пока не произойдёт фрагмент `onResume`, или клик на секцию (`bindSectionHeader` вызывает `setTasksAndNotifyDataSetChanged` который делает rebuild), или поворот экрана.

**Файлы и приблизительные строки:**

- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java`
  - **`notifyDataChanged()` строки 252-275** — здесь нет вызова `tasksRecyclerViewAdapter.rebuildItems()` перед `notifyDataSetChanged()`. Это главный кандидат на починку.
- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/TasksRecyclerViewAdapter.java`
  - **`bindTask()` строки 312-325** — checkbox-handler вызывает `smallTasksFragment.notifyDataChanged()` вместо `setTasksAndNotifyDataSetChanged()`. Альтернативное место фикса.
  - **`flatten()` строки 126-172** — корректно фильтрует ничто (полагается на done-filter в `tasks`), но если решат сделать filtering на уровне адаптера — править здесь.
- `app/src/main/java/com/shumidub/todoapprealm/ui/actionmode/task/TaskActionModeCallback.java`
  - **строки 156, 282, 299** — `notifyDataChanged()` после delete / categories / edit. Те же грабли: items не перестраивается. Если фикс делается в `notifyDataChanged()` — эти места починятся автоматически.

**Подтверждающие коммиты:**
- Wave 1 (`5cbcea9`, sprint-001 task-003): добавил `sectionId`/`position` в `TaskObject` через миграцию, но адаптер не трогал — регрессии **не вносил**.
- Wave 2 (`7a9d7ae`, sprint-001 task-001+task-002): **здесь** регрессия. Адаптер переведён с `tasks.get(position)`-биндинга на `items`-snapshot-биндинг (`flatten` + `AdapterItem`); код `setTasksAndNotifyDataSetChanged` добавлен правильно (он делает rebuild), но **старый `notifyDataChanged` остался в неизменном виде и теперь не делает rebuild** — а его всё ещё дёргают checkbox и ActionMode.

**Что НЕ сломано (исключения из подозрений):**
- `TasksRealmController.getNotDoneTasks(folderId)` — фильтр `equalTo("done", false)` присутствует.
- `showAllTasks()` toggle — пересоздаёт адаптер, работает корректно.
- Сортировка done-задач вниз — `getTasks(folderId)` сортирует по `done ASC, position ASC`, что даёт правильный порядок в show-completed-режиме.

## Open Questions

1. **Q1 — Взаимодействие с "Empty" placeholder из task-002.** В default-режиме, если секция expanded и содержит только done-задачи (ни одной undone), то по логике task-004 показывается только хэдер (done скрыты). Но task-002 (sprint-002) планирует "Empty" placeholder для пустых expanded-секций. Считается ли такая секция "пустой" с точки зрения placeholder?
   - Вариант A: показывать "Empty" (только undone считаются — текущая инклинация task-002 Q3, рекомендация "оставить как только undone").
   - Вариант B: не показывать "Empty" (done-задачи "есть", просто скрыты тоггом — секция не пустая концептуально).
   - **Рекомендация:** Вариант A (соответствует task-002 Q3). Но требуется явное подтверждение пользователя, чтобы зафиксировать поведение в обоих task'ах.

2. **Q2 — Должен ли тоггл `isAllTaskShowing` сохраняться между сессиями фрагмента / переходами между папками / re-creation активити?**
   - Сейчас: `onViewCreated` всегда сбрасывает `isAllTaskShowing = false`. Это значит при возврате в категорию режим сбросится.
   - Считается ли это багом task-004 (тоже "регрессия после sprint-001")?
   - **Рекомендация:** считать out-of-scope для task-004 (это давнее поведение, не Wave2-регрессия), вынести в отдельный тикет при необходимости.

3. **Q3 — Где именно класть фикс?**
   - Вариант A: добавить `tasksRecyclerViewAdapter.rebuildItems()` внутрь `SmallTasksFragment.notifyDataChanged()` перед `notifyDataSetChanged()`. Чинит все call-sites одной строкой.
   - Вариант B: заменить call-sites `notifyDataChanged()` на `setTasksAndNotifyDataSetChanged()` (checkbox-handler, TaskActionModeCallback × 3).
   - Вариант C: убрать `notifyDataChanged()` совсем, везде использовать `setTasksAndNotifyDataSetChanged()`.
   - **Рекомендация:** Вариант A (минимальный diff, низкий риск). Решение зафиксировать на Phase 3.

4. **Q4 — Тоггл футера в показывающем-completed-режиме — должна ли надпись измениться?** Сейчас при `isAllTaskShowing == true` футер всё ещё пишет "Done N tasks" и клик по нему скрывает done. Есть ли UX-требование сменить текст на "Hide done" / "Скрыть выполненные"? **Рекомендация:** out-of-scope, фиксим только баг скрытия.

5. **Q5 — При показывающем-completed-режиме и пустой секции — done-задачи этой секции должны рендериться?** В текущем `flatten` они попадут в bucket по `sectionId` и отрендерятся под хэдером (если их sectionId совпадает). Проблема: задача когда-то была в секции, потом помечена done — её `sectionId` всё ещё указывает на секцию? Нужна проверка через manual QA / в Phase 3.

## Design

<заполняется на Phase 3 — после diagnosis>

## Tests

Manual QA — но рекомендуется добавить unit-тест на фильтрацию задач, если есть удобная точка изоляции (определить на Phase 3).

## Implementation

<заполняется на Phase 5>

## Review

<заполняется на Phase 6>

## Manual verification

<заполняется на Phase 7>
