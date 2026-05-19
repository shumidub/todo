# Sprint 001 · task-ux (token k8m3n2p)

**Project**: todo100android
**Created**: 2026-05-17
**Status**: Phase 1 (planning) → Phase 2 (requirements clarification)
**Testing approach**: manual QA, без TDD (Phase 4 пропущена)

## Цель
Улучшить UX работы с задачами: быстрый редактор по тапу, иерархия через секции внутри категорий, новая палитра Canary в третьем табе.

## Задачи
- **task-001-task-bottomsheet** — BottomSheet редактор задачи по тапу (текст + категории)
- **task-002-category-sections** — секции внутри категории для группировки задач (имя + collapsed-by-default, long-click для редактирования)
- **task-003-tasks3-tab-canary** — новый таб Tasks3 с палитрой J2 Canary

## Влияние
- Tabs: Adds 4-й tab (Tasks3). `MainPagerAdapter.getCount()` → 4
- DB schema: новая Realm-модель `Section` для группировки задач внутри folder
- UI: новый BottomSheet (Material BottomSheetDialogFragment), новые цвета `canary*`, новый `CanaryPalette`
- Backward compat: существующие Tasks/Tasks2 не ломаются
