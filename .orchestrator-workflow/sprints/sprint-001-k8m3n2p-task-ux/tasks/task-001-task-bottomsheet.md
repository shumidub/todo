# task-001 · BottomSheet редактор задачи

**Sprint**: sprint-001-k8m3n2p-task-ux
**Status**: Phase 2 — clarification
**Owner**: bg-agent (TBD)

## User-facing описание
По одиночному клику на task-карточку открывается BottomSheet, в котором можно:
- Прочитать полный текст задачи (на карточке может быть обрезан)
- Отредактировать текст (inline-edit)
- Увидеть список категорий: сверху — те, к которым задача уже относится; ниже — остальные категории (быстрое добавление одним тапом)
- Свернуть/закрыть шторку свайпом вниз или тапом за пределы

## Затронутые файлы (предварительно)
- `TasksRecyclerViewAdapter.java` — onClick → открыть BottomSheet вместо текущего поведения
- Новый: `ui/dialog/task_bottomsheet/TaskEditorBottomSheet.java` (extends `BottomSheetDialogFragment`)
- Новый: `res/layout/bottomsheet_task_editor.xml`
- Возможно: `EditDelFolderDialog` — донор части UI

## Requirements
_TBD bg-агент в фазе 2_

## Design
_TBD фаза 3_

## Tests
_skip (manual QA)_

## Implementation
_TBD фаза 5_

## Review
_TBD фаза 6_

## Open Questions
_TBD bg-агент в фазе 2_
