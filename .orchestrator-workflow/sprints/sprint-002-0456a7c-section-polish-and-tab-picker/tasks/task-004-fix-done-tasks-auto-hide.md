# Task 004 — Фикс: выполненные задачи автоматически скрываются вне режима show-completed

- **Sprint:** sprint-002-0456a7c
- **Task token:** 5d0d033
- **Project:** todo100android
- **Cross-project ref:** —
- **Feature flag:** not required (bugfix)
- **Status:** pending

## Acceptance criteria

- [ ] При выключенном режиме "показывать выполненные задачи" (default) — задачи с `isCompleted == true` **скрыты** в списке задач категории.
- [ ] При включённом режиме — выполненные задачи **видны** (как сейчас работает на скриншоте Image #2 — там видно `2/2` и галочку).
- [ ] Переключение режима (где бы оно ни было — toolbar/меню) работает: список перерисовывается мгновенно.
- [ ] Поведение совпадает на всех трёх табах (Tasks/Tasks2/Tasks3).
- [ ] Сломалось после sprint-001 (введение sections и BottomSheet) — найти причину в коммитах `5cbcea9` (task-003 Wave 1) или `7a9d7ae` (task-001+task-002 Wave 2).
- [ ] Регрессия должна быть зафиксирована и в случае комбинации с секциями (внутри секций тоже выполненные скрыты в default-режиме).

## Requirements

> **Feature flag:** not required (bugfix).
> **Session persistence of `isAllTaskShowing`:** out-of-scope for this task (текущее поведение `onViewCreated → isAllTaskShowing = false` — это не Wave2-регрессия; вынести в отдельный тикет если понадобится).
> **Footer text:** out-of-scope. Текст "Done N tasks" остаётся без изменений.

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
- Done-задачи с `sectionId != 0` рендерятся под хэдером своей секции в bucket (как и предусмотрено `flatten()`). В этом режиме "Empty" placeholder **не показывается** для таких секций — done-задачи заполняют bucket. Проверка — Phase 7 manual QA: (a) включить show-completed, (b) убедиться что done с sectionId оказываются внутри своей секции, (c) что секция не пустая визуально.

### R3. Переключение режима — мгновенный rerender

- При клике на футер список перерисовывается сразу (`showAllTasks()` пересоздаёт адаптер и `rvTasks.setAdapter(...)` — работает уже сейчас).

### R4. Все три таба (Tasks/Tasks2/Tasks3)

- Поведение идентично на всех трёх вкладках. `FolderSlidingPanelFragment` каждой вкладки внутри использует `SmallTasksFragment` — фильтрация done живёт **в одном месте** (адаптер + `tasks` поле), палитра не влияет на логику.

### R5. Done-задачи внутри секций + взаимодействие с "Empty" (coordinated with task-002 Q3, var A)

- В default-режиме done-задачи скрыты — **даже если** секция содержит только done-задачи (т.е. ни одной undone). В этом случае секция формально пустая для `flatten()` (см. task-002 этого спринта, Q3 — `bySection.get(sectionId) == null` если нет undone).
- Для секции, у которой в default-режиме нет ни одной undone-задачи (только done или вообще пусто), показывается placeholder **"Empty"** под хэдером — вариант A, согласованный с task-002 Q3.
- В show-completed-режиме done-задачи такой секции отрисуются под её хэдером, и **"Empty" не показывается** (bucket не пустой).

### R6. Done-фильтр не зависит от collapsed/expanded

- Свёрнутые секции и так не показывают inner-tasks (см. `emitSection`, `!s.isCurrentlyCollapsed()`). Done-фильтр работает ортогонально: даже если секция expanded и в ней есть только done-задачи, в default-режиме показывается только хэдер + "Empty" (см. R5).

### R7. Fix point — где класть фикс

- **Решение:** добавить `tasksRecyclerViewAdapter.rebuildItems()` внутрь `SmallTasksFragment.notifyDataChanged()` **перед** `tasksRecyclerViewAdapter.notifyDataSetChanged()`.
- **Файл:** `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java`, строки **252-275** (метод `notifyDataChanged()`).
- **Почему здесь:** минимальный diff, чинит все call-sites одной строкой — checkbox-handler (`TasksRecyclerViewAdapter.bindTask()` строки 312-325) и `TaskActionModeCallback` (строки 156, 282, 299 — delete / categories / edit) дёргают `notifyDataChanged()` после мутаций Realm. После вызова `rebuildItems()` snapshot `items` пересоберётся из актуального `tasks` (RealmResults proxy с фильтром `done=false` в default-режиме), и done-задачи исчезнут как раньше.

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

All questions resolved in Phase 2 — see commit history.

## Design

### Approach

Одностроковый фикс в `SmallTasksFragment.notifyDataChanged()` (файл `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java`, метод на строках 252-275).

Вставить вызов `tasksRecyclerViewAdapter.rebuildItems();` **внутри `else`-ветки**, непосредственно **перед** `tasksRecyclerViewAdapter.notifyDataSetChanged();`. То есть в текущей структуре:

```
if (tasksRecyclerViewAdapter == null) {
    tasksRecyclerViewAdapter = new TasksRecyclerViewAdapter(...);   // конструктор уже вызывает rebuildItems()
    rvTasks.setAdapter(tasksRecyclerViewAdapter);
} else {
    tasksRecyclerViewAdapter.rebuildItems();          // <-- НОВАЯ СТРОКА
    tasksRecyclerViewAdapter.notifyDataSetChanged();
}
```

Null-guard уже присутствует (`if (tasksRecyclerViewAdapter == null)` на строке 259) — отдельная проверка не нужна. В ветке `null` конструктор `TasksRecyclerViewAdapter` сам зовёт `rebuildItems()` (строка 118 адаптера), так что snapshot всегда консистентен после `notifyDataChanged()`.

`rebuildItems()` читает текущее поле `tasksRecyclerViewAdapter.tasks` (RealmResults proxy). На момент вызова `notifyDataChanged()` из checkbox-handler или TaskActionMode это поле уже отражает актуальное состояние Realm (фильтр `done=false` живой), поэтому snapshot `items` пересоберётся из undone-only — done-задача исчезнет.

### Affected files

- `app/src/main/java/com/shumidub/todoapprealm/ui/fragment/task_section/small_tasks_fragment/SmallTasksFragment.java` — единственный модифицируемый файл, одна строка добавляется в методе `notifyDataChanged()` (строки 252-275).

Никаких других файлов трогать не нужно. Все call-sites `notifyDataChanged()` (checkbox в `TasksRecyclerViewAdapter.bindTask()` строка 319; `TaskActionModeCallback` строки 156, 282, 299) починятся автоматически — это и есть цель выбранной точки фикса.

### API / contracts

- Никаких изменений в public API.
- `rebuildItems()` уже `public` в `TasksRecyclerViewAdapter` (строка 122) — подтверждено grep'ом. Используется в:
  - конструкторе адаптера (строка 118),
  - `SmallTasksFragment.setTasksAndNotifyDataSetChanged()` (строка 209).
- Сигнатура: `public void rebuildItems()` — без аргументов, без return.

### Feature flag

Не требуется. Это bugfix регрессии Wave 2 (sprint-001, commit `7a9d7ae`).

### ADR

Новый ADR не нужен. Это код-смел фикс (snapshot rebuilt missing), а не архитектурное решение. Связь со снапшот-моделью адаптера уже зафиксирована в sprint-001 task-001/002.

### Risks

1. **Stale data при гонке с Realm-транзакцией.** `rebuildItems()` читает snapshot из `tasks` (RealmResults proxy). Если Realm-транзакция ещё не закоммичена в момент вызова `notifyDataChanged()`, snapshot может оказаться stale. **Новый риск НЕ появляется** — этот же путь уже работает в `setTasksAndNotifyDataSetChanged()` (строки 204-212), который вызывается из `onResume`, `onTaskEditorDismissed`, и section-dialog listener. Все существующие call-sites `notifyDataChanged()` вызываются **после** Realm-мутации (`setTaskDoneOrParticullaryDone` в checkbox-handler, `delete`/`edit`/`category` в ActionMode), так что транзакция уже закоммичена.

2. **NPE на early lifecycle.** Если адаптер ещё не создан (`tasksRecyclerViewAdapter == null`), ветка `if` создаёт новый адаптер (который сам зовёт `rebuildItems()` в конструкторе). Ветка `else` гарантирует адаптер не null → безопасно. Дополнительный null-guard НЕ требуется.

3. **Двойной rebuild в `setTasksAndNotifyDataSetChanged()`.** После фикса этот метод сначала вызовет `rebuildItems()` (строка 209), потом `notifyDataChanged()` → который снова позовёт `rebuildItems()`. Идемпотентно (rebuild читает текущее состояние `tasks`, результат тот же), оверхед минимален (один проход по `tasks` + `sections`). Допустимо — упрощать call-site не входит в задачу.

4. **Show-completed-mode (`showAllTasks()`) — не затронут.** Этот метод пересоздаёт адаптер с новой ссылкой на `tasks` (строки 283/294) и НЕ зовёт `notifyDataChanged()` — поэтому фикс на toggle-режим никак не влияет.

5. **Infinite recursion — отсутствует.** `rebuildItems()` не вызывает `notifyDataChanged()`. `notifyDataSetChanged()` — это RecyclerView.Adapter API, тоже не зовёт обратно во фрагмент. Цикла нет.

6. **Tabs (Tasks/Tasks2/Tasks3).** У каждого таба свой `SmallTasksFragment` с собственным адаптером и `tasks`. Фикс работает идентично для всех трёх — палитра (cornflower/canary) к фильтрации done не относится.

### Test plan (Phase 7 — manual QA)

1. **Default-mode, чекбокс.** Открыть категорию с несколькими undone-задачами. Тапнуть чекбокс одной задачи → задача должна **исчезнуть мгновенно**, без скролла/поворота/перехода между табами. Счётчик "Done N tasks" в футере увеличивается на 1.
2. **Default-mode, delete через ActionMode.** Long-press по задаче → ActionMode → delete → задача исчезает мгновенно.
3. **Default-mode, edit через ActionMode.** Long-press → edit → изменить title → сохранить → список перерисовывается, без stale-снимка.
4. **Default-mode, move category через ActionMode.** Long-press → categories → переместить в другую папку → задача исчезает из текущего списка.
5. **Show-completed-mode, чекбокс на done.** Включить show-completed (тап по футеру). Тапнуть на чекбокс выполненной задачи → задача **остаётся в списке** (uncheck снимает done-флаг), переезжает в undone-bucket секции/корня.
6. **Show-completed-mode, чекбокс на undone.** В том же режиме тапнуть на undone-задачу → задача **остаётся в списке** (она теперь done), переезжает вниз своего bucket'а (за счёт `done ASC` сортировки в Realm-запросе).
7. **Tabs.** Повторить пункт 1 (чекбокс на undone в default-режиме) на Tasks2 и Tasks3 — поведение идентичное.
8. **Sections + default-режим.** Создать секцию с двумя undone-задачами. Чекнуть последнюю undone → секция остаётся (хедер виден), bucket пустой → показывается placeholder **"Empty"** (R5).
9. **Sections + show-completed.** В том же сценарии включить show-completed → done-задачи появляются под хэдером своей секции, "Empty" исчезает (R5 / task-002 Q3 var A).
10. **Toggle round-trip.** Default → show-completed → default → done-задачи должны быть скрыты в обоих default-фазах, видны только в show-completed.

Unit-тест на фильтрацию не добавляем: `notifyDataChanged()` тесно связан с Android-фреймворком (RecyclerView + LinearLayoutManager + Fragment lifecycle), изоляция нерациональна для одностроковой правки. Будет покрыто manual QA.

## Tests

Manual QA — но рекомендуется добавить unit-тест на фильтрацию задач, если есть удобная точка изоляции (определить на Phase 3).

## Implementation

<заполняется на Phase 5>

## Review

<заполняется на Phase 6>

## Manual verification

<заполняется на Phase 7>
