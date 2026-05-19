# Task 001 — Выбор green/blue/yellow таба для папки

- **Sprint:** sprint-002-0456a7c
- **Task token:** 7382aca
- **Project:** todo100android
- **Cross-project ref:** —
- **Feature flag:** (определить на Phase 3)
- **Status:** pending

## Acceptance criteria

- [ ] В диалоге редактирования папки (`EditDelFolderDialog`) вместо чекбокса "On Tasks2 tab" появляется выбор одного из трёх вариантов: **green** / **blue** / **yellow**.
- [ ] Эти три значения соответствуют существующим табам Tasks (group=0, зелёный), Tasks2 (group=1, синий/Cornflower), Tasks3 (group=2, жёлтый/Canary).
- [ ] При создании новой папки тоже доступен этот выбор (`AddFolderDialog` или эквивалент).
- [ ] Текущие папки в БД должны корректно маппиться: group=0 → green, group=1 → blue, group=2 → yellow. Никакой Realm-миграции (поле group уже есть).
- [ ] При смене значения папка перемещается в соответствующий таб (как раньше с чекбоксом).
- [ ] UI — компактный (3 опции в строку или один радио-блок), не ломает остальной layout диалога.

## Requirements

<заполняется на Phase 2>

## Open Questions

<заполняется на Phase 2>

## Design

<заполняется на Phase 3>

## Tests

Manual QA — без TDD (как в sprint-001).

## Implementation

<заполняется на Phase 5>

## Review

<заполняется на Phase 6>

## Manual verification

<заполняется на Phase 7>
