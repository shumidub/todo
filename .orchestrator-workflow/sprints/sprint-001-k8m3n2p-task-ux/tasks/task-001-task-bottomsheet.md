# task-001 · BottomSheet редактор задачи

**Sprint**: sprint-001-k8m3n2p-task-ux
**Status**: Phase 2 complete → Phase 3 (architecture)
**Owner**: bg-agent (TBD)

## User-facing описание
По одиночному клику на task-карточку открывается BottomSheet, в котором можно:
- Прочитать полный текст задачи (на карточке может быть обрезан)
- Полностью отредактировать задачу (текст + все остальные поля)
- Увидеть список категорий: сверху — те, к которым задача уже относится; ниже — остальные категории (быстрое добавление одним тапом)
- Свернуть/закрыть шторку свайпом вниз или тапом за пределы

## Requirements (финал, Phase 2 confirmed)

### Триггеры и поведение шторки
- **R1.** Одиночный тап по тексту задачи в `TasksRecyclerViewAdapter` (любой таб: Tasks/Tasks2/Tasks3) открывает новый `BottomSheetDialogFragment`. Заменяет текущий `MaterialAlertDialogBuilder.setMessage(task.getText())` в `SmallTasksFragment.onItemClicked`.
- **R2.** Долгий тап → `TaskActionModeCallback` (multi-select) остаётся как сейчас.
- **R3.** Done-задачи (когда раскрыты через footer) тоже открывают BottomSheet с **полным функционалом** (не read-only).
- **R4.** Высота — **half-expanded** при старте, свайпом вверх раскрывается полностью.
- **R5.** Закрытие — свайп вниз и тап вовне (стандарт `BottomSheetDialogFragment`).
- **R6.** На закрытии — **autosave** изменённых полей; категории сохраняются **live** при каждом тапе. После закрытия видимая карточка перерисовывается.

### Поля редактирования (full edit, заменяет `EditTaskDialog`)
- **R7.** Редактируемые поля: text, value (count), maxAccumulate, priority, cycling, **done/undone toggle**.
- **R8.** EditText текста: `inputType=textCapSentences|textMultiLine`, `maxLines=10` (увеличено с 4 в старом диалоге), скролл внутри поля при превышении.
- **R9.** Со стороны контроллера для текстовых полей переиспользуется `TasksRealmController.editTask(...)`; для done-toggle — существующий `setTaskDone(...)` / `setTaskUndone(...)`.

### Категории
- **R10.** Под полями редактирования — список **всех** `FolderTaskObject` (нет фильтрации по `isDaily`), разделённый на две группы:
  - **Текущие** (сверху): категории, к которым задача относится (`TasksRealmController.getCategoryIds(task)` — primary + extras).
  - **Остальные** (ниже): все прочие папки, в том же порядке, что и в основном списке (`FolderTaskRealmController.getFoldersList(taskGroup)`).
- **R11.** Тап по неактивной категории → **добавляет** её к extras (N:N). Тап по активной → убирает. Запрет: нельзя снять последнюю активную (тап игнорируется).
- **R12.** Изменения категорий применяются **live** через `setTaskCategories(task, folderIds)`.

### Совместимость с табами и палитрами
- **R13.** BottomSheet применяется ко всем 3 task-табам. При открытии получает текущую палитру (`null` для Tasks, `CornflowerPalette` для Tasks2, `CanaryPalette` для Tasks3) и применяет accent/bg/text/divider к своим элементам.
- **R14.** Категории берутся из соответствующего `taskGroup` (Tasks=0, Tasks2=1, Tasks3=2 — после task-003).

## Затронутые файлы
- `TasksRecyclerViewAdapter.java` / `SmallTasksFragment.java` — onClick → открыть BottomSheet вместо `MaterialAlertDialogBuilder`
- Новый: `ui/dialog/task_bottomsheet/TaskEditorBottomSheet.java` (extends `BottomSheetDialogFragment`)
- Новый: `res/layout/bottomsheet_task_editor.xml`
- `EditDelFolderDialog` / `dialog_edit_task.xml` — справочно (донор UI/стиля)
- `TasksRealmController` — reuse `editTask`, `setTaskCategories`, `setTaskDone/Undone`

## Design

### 1. Class diagram (текстом)

**Новые классы** (пакет `com.shumidub.todoapprealm.ui.dialog.task_bottomsheet`):

```
TaskEditorBottomSheet  extends  com.google.android.material.bottomsheet.BottomSheetDialogFragment
  ┌─ args (Bundle):
  │   ARG_TASK_ID   : long
  │   ARG_TASK_GROUP: int   (0=Tasks, 1=Tasks2, 2=Tasks3)
  │
  ├─ public static TaskEditorBottomSheet newInstance(long taskId, int taskGroup)
  ├─ public void setOnDismissListener(Runnable onDismiss)   // фрагмент дергает notifyDataChanged()
  │
  ├─ private fields:
  │   long taskId; int taskGroup;
  │   TaskObject task;                       // resolved в onViewCreated, не хранится надолго
  │   Palette palette;                       // см. ниже, обёртка
  │   CategoriesAdapter categoriesAdapter;
  │   EditText etText; TextView tvValue, tvMaxAcc, tvPriority, tvCycling; CheckBox cbDone;
  │   Runnable onDismiss;
  │
  ├─ onCreateView() → inflate R.layout.bottomsheet_task_editor
  ├─ onViewCreated() → resolveTask, resolvePalette, bindFields, bindCategories, applyPalette
  ├─ onStart() → настроить BottomSheetBehavior: state=STATE_HALF_EXPANDED, peekHeight=screenH*0.55, skipCollapsed=true
  ├─ onDismiss(DialogInterface) → applyTextEditsToRealm(); onDismiss?.run()
  └─ onSaveInstanceState() → сохранить текущий unsaved-text из etText (KEY_DRAFT_TEXT) — restore в onViewCreated

CategoriesAdapter  extends  RecyclerView.Adapter<CategoryVH>
  ┌─ List<Row> rows;  // unified list, см. ниже
  │   sealed Row = HeaderRow(String label) | CategoryRow(FolderTaskObject folder, boolean active)
  ├─ getItemViewType() → разделяет header / cell
  ├─ onBindViewHolder(): renderActive vs renderInactive (см. Palette mapping)
  └─ click → onCategoryTap(folderId)

Palette  (private inner static class или просто Map<String,Integer>)
  ┌─ int accent, bg, surface, text, textSoft, divider, counter, inputText;
  ├─ static Palette forGroup(Context ctx, int group):
  │       group==0 → defaults (R.color.*)
  │       group==1 → from CornflowerPalette
  │       group==2 → from CanaryPalette  (создаётся в task-003; до этого fallback на defaults)
```

**Модифицируемые классы:**
- `SmallTasksFragment` — `setTasksListClickListeners()`: `onItemClicked` лямбда заменяется. Вместо `MaterialAlertDialogBuilder.setMessage(...)` создаётся `TaskEditorBottomSheet.newInstance(idTask, resolveTaskGroup())` и `show(getChildFragmentManager(), "task_editor")`. Передаётся `setOnDismissListener(this::notifyDataChanged)`. `resolveTaskGroup()` повторяет логику `isInCornflowerTab()` и расширяется на Tasks3 (когда придёт task-003).
- `TasksRecyclerViewAdapter` — изменений API нет; click-listener тот же.

**Reused (не модифицируется):**
- `TasksRealmController.editTask(task, text, count, maxAccumulation, cycling, priority)` — для текстовых полей.
- `TasksRealmController.setTaskDoneOrParticullaryDone(task, done)` — для done-toggle (в проекте нет отдельных `setTaskDone/Undone`; этот метод корректно обрабатывает обе ветки, см. **R9 уточнение** в Open Questions).
- `TasksRealmController.getCategoryIds(task)`, `setTaskCategories(task, folderIds)`.
- `FolderTaskRealmController.getFoldersList(taskGroup)`.

### 2. Lifecycle BottomSheet

1. **Open**: из `SmallTasksFragment.onItemClicked` →
   `TaskEditorBottomSheet.newInstance(taskId, taskGroup).show(getChildFragmentManager(), TAG)`.
   Группа резолвится один раз в фрагменте (используем `getParentFragment()` → `FolderSlidingPanelFragment.getTaskGroup()`).
2. **Args via Bundle**, не сеттеры — переживает recreate (rotation, process death).
3. **`onViewCreated`**:
   - `task = TasksRealmController.getTask(taskId)`; если null → `dismissAllowingStateLoss()`.
   - `palette = Palette.forGroup(ctx, taskGroup)`.
   - Биндинг полей из task; если есть `savedInstanceState`, для `etText` использовать `KEY_DRAFT_TEXT` (приоритет над `task.getText()`).
   - `applyPalette()` — см. секцию 5.
   - `categoriesAdapter = new CategoriesAdapter(...); rv.setAdapter(...)`.
4. **`onStart`**: получить `BottomSheetBehavior` из `((BottomSheetDialog) getDialog()).getBehavior()`, выставить:
   - `setState(STATE_HALF_EXPANDED)`
   - `setHalfExpandedRatio(0.55f)` (или 0.6 — подгоним при ручной проверке)
   - `setSkipCollapsed(true)` — свайп вниз сразу закрывает.
5. **Rotation / state**: единственный неперсистированный draft — текст в `etText`. Сохраняем в `onSaveInstanceState`. Остальное (priority, value, etc.) — пишется в Realm немедленно через хелперы, либо хранится в локальных primitive-полях, которые тоже сохраняем в bundle. См. Open Question Q3.
6. **Close**: свайп вниз / тап вовне → `onDismiss(DialogInterface)`:
   - `applyTextEditsToRealm()`: вызывает `editTask(task, etText.text, value, maxAcc, cycling, priority)` одной транзакцией.
   - `onDismiss.run()` → `SmallTasksFragment.notifyDataChanged()` + `invalidateOptionsMenu()` + уведомить `App.folderSlidingPanelFragments` как при чек-боксе.

### 3. Layout structure — `bottomsheet_task_editor.xml`

```
<androidx.coordinatorlayout.widget.CoordinatorLayout>     // не нужен; BottomSheetDialog оборачивает сам
  → root: <LinearLayout android:orientation="vertical"
                        android:id="@+id/bs_root"
                        android:paddingHorizontal="20dp"
                        android:paddingTop="12dp"
                        android:paddingBottom="16dp">

    1. Drag handle (mtrl_bottom_sheet_drag_handle стиль или View 32x4dp по центру)
       id=@+id/bs_drag_handle

    2. Header row (horizontal LinearLayout, как в dialog_edit_task.xml):
         @+id/bs_title          "Edit task"
         @+id/bs_task_value     (count, click → cycle 1..N — оставляем поведение существующего диалога)
         @+id/bs_task_max_acc
         @+id/bs_task_priority  "!" (click → priority cycle 0..3 как в адаптере)
         @+id/bs_task_cycling   "C" (click → toggle isCycling)
         @+id/bs_done_checkbox  (done toggle)

    3. TextInputLayout → TextInputEditText
         @+id/bs_et_text
         inputType=textCapSentences|textMultiLine
         maxLines=10                          // R8: было 4
         minLines=3
         scrollbars=vertical
         android:scrollHorizontally=false
         + nestedScrollingEnabled=true        // важно для half-expanded — внутренний скролл текста
                                                не мешает drag шторки
       layout_marginTop=12dp

    4. Categories block:
         @+id/bs_categories_label    (optional разделительный TextView, скрываем если 0 элементов)
         @+id/bs_rv_categories       (RecyclerView, nested-scrolling)
             layout_height=wrap_content (с max через постфикс — см. Q4)
             android:overScrollMode="never"

    5. (нет кнопок Save/Cancel — autosave on dismiss)
  </LinearLayout>
```

**Сосуществование 10-line EditText + half-expanded**: при `STATE_HALF_EXPANDED` шторка занимает ~55% экрана; EditText `maxLines=10` начинает скроллиться внутри, потому что parent `LinearLayout` обёрнут в `NestedScrollView` (если потребуется — обернём; решаем при имплементации, см. Q5). Альтернатива: при тапе в EditText форсировать `STATE_EXPANDED` (поднять шторку, освободив место под клавиатуру) — это и сделаем (см. Q5 — leaning yes).

### 4. UI категорий

**Структура**: `RecyclerView` с `LinearLayoutManager` (vertical). Один adapter, два типа viewType:
- `TYPE_HEADER` (item_bs_category_header.xml): TextView "Current" / "Other".
- `TYPE_CATEGORY` (item_bs_category.xml): горизонтальный layout — leading icon (`@drawable/ic_check_24` если active, иначе нейтральная иконка `ic_folder_24` или пусто) + TextView с названием папки. Tap на root → callback.

**Почему RecyclerView, не ChipGroup**: у пользователя может быть 30+ категорий, list-style лучше для скана; FlexboxLayout/ChipGroup плохо рендерит длинные имена и сложнее с разделителями.

**Обозначение "текущих"**:
1. Section header "Current" сверху (только если есть).
2. Active row: текст цвета `palette.accent`, leading check-icon tinted accent, фон `palette.surface` (как обычно).
3. Inactive row: текст `palette.text`, иконки нейтральные (`palette.textSoft`), без чекбокса.
4. Между секциями — divider 1dp `palette.divider`.

**Построение `rows` списка** (в `bindCategories`):
```
activeIds = TasksRealmController.getCategoryIds(task)  // primary + extras, не пуст
allFolders = FolderTaskRealmController.getFoldersList(taskGroup)
current = allFolders.filter(id ∈ activeIds), сохраняя порядок activeIds (primary первым)
other   = allFolders.filter(id ∉ activeIds)

rows = []
if current.size > 0:  rows += HeaderRow("Current")
for f in current:     rows += CategoryRow(f, active=true)
if other.size > 0:    rows += HeaderRow("Other")
for f in other:       rows += CategoryRow(f, active=false)
```

### 5. Save flow

**Тексто-числовые поля (text, value, maxAccumulate, priority, cycling)**:
- Хранятся как локальный draft (либо читаются из view в момент dismiss).
- Priority/cycling меняются по tap в header сразу же — но **не пишутся** в Realm немедленно; локальный счётчик + перерисовка TextView. В `onDismiss` всё одним вызовом `editTask(task, text, value, maxAcc, cycling, priority)` — **одна Realm-транзакция**.
- Done-toggle (`cbDone`): пишется немедленно через `setTaskDoneOrParticullaryDone(task, checked)` — потому что меняет также `lastDoneDate`/accumulation, и логика не идемпотентна между open/close. См. Q1.

**Категории — live**:
- Tap на inactive → строим новый `List<Long>` (current ∪ {tappedId}, primary остаётся прежним), вызываем `setTaskCategories(task, list)`, обновляем `rows` и `notifyDataSetChanged()`.
- Tap на active при `current.size > 1` → удаляем id из list, `setTaskCategories(...)`. Если удаляемый id был primary — новым primary становится следующий в списке. Не вмешиваемся: `setTaskCategories` сам берёт `folderIds.get(0)` как primary.
- Tap на active при `current.size == 1` → **тап игнорируется** (no-op, никакого тоста — UX-решение из R11). См. Q2 (нужен ли visual feedback).

**Перерисовка карточки**: в `onDismiss` zовём `onDismissListener.run()` → фрагмент:
```
notifyDataChanged();
getActivity().invalidateOptionsMenu();
for (FolderSlidingPanelFragment p : App.folderSlidingPanelFragments) {
    p.notifyFolderOfTasksRVAdapterDataSetChanged();
}
```

### 6. Palette application — mapping

| View                       | Property                | Palette field         |
|---                         |---                      |---                    |
| `bs_root` (LinearLayout)   | backgroundColor / BS bg | `palette.bg`          |
| `bs_drag_handle`           | tint                    | `palette.textSoft`    |
| `bs_title`                 | textColor               | `palette.text`        |
| `bs_task_value/max_acc`    | textColor               | `palette.counter`     |
| `bs_task_priority` (text)  | textColor (when prio>0) | `palette.accent`      |
| `bs_task_priority` (when 0)| textColor               | `palette.textSoft`    |
| `bs_task_cycling`          | textColor (when active) | `palette.accent`      |
| `bs_done_checkbox`         | buttonTintList          | `palette.accent`      |
| TextInputLayout            | boxStrokeColor, hint    | `palette.accent`, `palette.textSoft` |
| `bs_et_text`               | textColor               | `palette.inputText`   |
| Section header             | textColor               | `palette.textSoft`    |
| Category row (active)      | textColor + icon tint   | `palette.accent`      |
| Category row (inactive)    | textColor + icon tint   | `palette.text`, `palette.textSoft` |
| Divider                    | background              | `palette.divider`     |

Для `taskGroup == 0` (Tasks, без палитры) — fallback на ресурсы: `R.color.colorAccent`, `colorDialogOnSurface`, `colorDialogOnSurfaceVariant`, `colorWhite`. Внутри `Palette.forGroup(ctx, 0)` собираем такой же объект, чтобы UI-код был один — без if-веток "palette == null".

Для `taskGroup == 2` (Tasks3 / Canary) — `CanaryPalette` появляется в task-003. До тех пор `Palette.forGroup(ctx, 2)` возвращает defaults (fallback на дефолтные цвета). Когда task-003 завершён — добавляем ветку `new CanaryPalette(ctx)`.

### 7. Координация с task-002 (секции) и task-003 (Tasks3)

**Что зависит от task-003 (Tasks3 tab):**
- Резолвинг `taskGroup` для Tasks3 (group=2) и `CanaryPalette` — нужны, чтобы R13/R14 работали в этом табе.
- Phase 5 task-001 **может стартовать независимо** для Tasks/Tasks2 (groups 0 и 1). Поведение для group=2 wires-up через тот же код пути, но без палитры — выглядит как Tasks. Когда task-003 готов, добавляем одну ветку в `Palette.forGroup`.

**Что зависит от task-002 (секции категорий):**
- Если task-002 вводит секции (группировку папок по типу — daily/regular/...), список "Other" в BottomSheet **может потребовать тот же группировочный header**. Phase 5 task-001 пишем сейчас без секций (flat "Other"). Когда task-002 готов — это будет mech follow-up на адаптере `CategoriesAdapter` (добавить дополнительные `HeaderRow` в `bindCategories`). Контракт `Row`-модели изначально расширяемый.

**Phase 5 task-001 независим**, если:
- Не трогаем `FolderTaskRealmController.getFoldersList` (используем как есть).
- Палитра-резолвер инкапсулирован за `Palette.forGroup(ctx, group)`.
- Резолвинг `taskGroup` в `SmallTasksFragment` готов к group=2 (return 2 если выяснится через parent fragment — можно сразу заложить, default 0).

### 8. Известные риски / Open Design Questions

- **Q1 (R9, done toggle)**: В коде нет `setTaskDone/Undone` — есть только `setTaskDoneOrParticullaryDone(task, done)`, и при `done=true` он применяет accumulation-логику (учитывает дату, инкрементит counter, ставит `done=true` только если `countAccumulation >= maxAccumulation`). Это **то же поведение**, что у тапа чек-бокса в адаптере. Решение: используем его же. **Risk**: для не-cycling задач один клик в BottomSheet может не сделать `done=true` сразу — это уже поведение существующего чекбокса, не регресс. Подтвердим при ручной QA.
- **Q2**: Игнорирование тапа на последней active категории — без visual feedback? Предлагаю короткий Snackbar/Toast "Last category" в первой итерации; финал — на ручной проверке.
- **Q3**: Хранить ли draft числовых полей (priority/value/cycling) в bundle при rotation? **Предлагаю**: писать их в Realm немедленно через мелкие хелперы (`setTaskPriority` уже есть), а в `onDismiss` делать только `editTask` для **текста**. Так и атомарность сохраняется, и rotation тривиален. **Меняет план "single Realm transaction в dismiss"** — компромисс, обсудить.
- **Q4**: Когда категорий 30+, `RecyclerView wrap_content` внутри `BottomSheet` — высота? Решение: `RecyclerView` берёт remaining-height в `LinearLayout` с `weight=1`, при STATE_EXPANDED скроллится; при HALF — частично виден, скроллится. Проверить вживую.
- **Q5**: При тапе в EditText — форсировать `STATE_EXPANDED`? Предлагаю **да** (через `OnFocusChangeListener` → `behavior.setState(STATE_EXPANDED)`), иначе клавиатура перекрывает текст в half-state.
- **Q6**: BottomSheet vs BottomSheetDialogFragment — берём `BottomSheetDialogFragment` (стандарт, поддерживает rotation, back-press, outside-tap). Подтверждено R5.
- **Q7**: Palette для group=0 — берём цвета из существующих ресурсов `R.color.colorAccent`, `colorDialogOnSurface`, `colorDialogOnSurfaceVariant`. Точный mapping defaults — может потребовать review дизайнером (тут — пользователем). Если что-то не совпадёт визуально с обычным диалогом — корректируем в Phase 6.

## Tests
_skip (manual QA)_

## Implementation
_TBD фаза 5_

## Review
_TBD фаза 6_
