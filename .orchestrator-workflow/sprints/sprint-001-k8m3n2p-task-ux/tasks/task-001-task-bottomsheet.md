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
_TBD фаза 3_

## Tests
_skip (manual QA)_

## Implementation
_TBD фаза 5_

## Review
_TBD фаза 6_
