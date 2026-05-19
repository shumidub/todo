# Sprint 002 — Section polish + tab color picker + regression fix

- **Sprint ID:** sprint-002-0456a7c
- **Sprint token:** 0456a7c
- **Created:** 2026-05-19
- **Goal:** Полировка UI секций (rails + Empty + счётчик прогресса), выбор цвета таба для папки (green/blue/yellow вместо чекбокса "On Tasks2 tab"), фикс регрессии с автоскрытием выполненных задач.
- **Projects in sprint:** todo100android
- **Status:** planning
- **Testing approach:** manual QA (без TDD, как в sprint-001)

## Tasks (this project)

- [ ] task-001 — Выбор green/blue/yellow таба для папки — `tasks/task-001-tab-color-picker.md` — task-token: 7382aca
- [ ] task-002 — Rails в начале/конце секции + Empty placeholder — `tasks/task-002-section-rails-and-empty.md` — task-token: f81a0cb
- [ ] task-003 — Счётчик прогресса справа от названия секции — `tasks/task-003-section-progress-counter.md` — task-token: 31310a0
- [ ] task-004 — Фикс: выполненные задачи автоматически скрываются вне режима show-completed — `tasks/task-004-fix-done-tasks-auto-hide.md` — task-token: 5d0d033

## Notes

- В рабочей директории есть uncommitted changes от sprint-001 (TaskEditorBottomSheet, FolderSlidingPanelFragment, TasksRecyclerViewAdapter, несколько layout-ов, colors.xml, новый bg_bottomsheet_rounded.xml). Решить статус до начала Phase 5: либо коммит как baseline под sprint-001 fix, либо stash/revert.
- task-001 (tab picker) меняет UX редактирования папки: текущий чекбокс "On Tasks2 tab" заменяется на 3-way выбор. Поскольку tasks-табов 3 (Tasks/Tasks2/Tasks3 → group=0/1/2), нужно подумать о migration существующих папок (group=0 → green, group=1 → blue, group=2 → yellow по умолчанию).
- task-004 — баг, возникший после sprint-001. Возможно сломали в Wave 2 (sections + bottomsheet). Diagnostics нужен.
- task-002 и task-003 — оба про секции внутри категории (`section_header_card_view.xml`), вероятно один agent сможет аккуратно их сделать вместе, но в плане держим раздельно для трассируемости.
