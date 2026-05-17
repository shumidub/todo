# task-002 · Секции внутри категории

**Sprint**: sprint-001-k8m3n2p-task-ux
**Status**: Phase 2 — clarification
**Owner**: bg-agent (TBD)

## User-facing описание
Внутри категории (folder) можно создавать секции для группировки задач.
- У секции есть **имя** и настройка **"свёрнута по дефолту"** (collapsed-by-default)
- При открытии категории секция отображается как заголовок-разделитель; задачи внутри могут быть скрыты, если свёрнута
- Настройки секции редактируются по **long click** по заголовку секции

## Затронутые файлы (предварительно)
- Realm: новая модель `Section` (id, name, collapsedByDefault, parentFolderId, position) — `realmmodel/task/`
- Realm: задача (`Task`) — добавить опциональный `sectionId`
- `realmcontrollers/taskcontroller/` — CRUD секций + перепривязка задач
- `SmallTasksFragment` + `TasksRecyclerViewAdapter` — рендеринг секций-заголовков, collapse/expand
- Новый: диалог редактирования секции (`ui/dialog/section_dialog/`)
- UI: кнопка "Добавить секцию" внутри категории

## Realm migration
Новая модель + новое поле в `Task` → Realm migration step. Версия схемы +1.

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
