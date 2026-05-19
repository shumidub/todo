# Sprint state

**Sprint**: sprint-001-k8m3n2p-task-ux
**Current phase**: 5 (implementation — Phase 4 TDD skipped per user, manual QA only)

## Tasks
| id | title | phase | status |
|----|-------|-------|--------|
| task-001 | BottomSheet редактор задачи | 5 | design locked, awaiting Wave 2 |
| task-002 | Секции внутри категории | 5 | design locked, awaiting Wave 2 |
| task-003 | Tasks3 таб + Canary палитра | 5 | dispatched as Wave 1 (includes shared migration) |

## Phase log
- 2026-05-17 Phase 1 complete (sprint + 3 tasks scaffolded, architecture.md + feature-flags.md created)
- 2026-05-17 Phase 2 complete — 31 question collected, all answered by user, Requirements locked in each task md
- 2026-05-17 Phase 3 complete — designs locked, 4 design-level clarifications resolved with user (save strategy, keyboard expand, sort-with-done-bottom, drop-on-collapsed auto-expand)
- 2026-05-17 Phase 4 skipped (no TDD, manual QA)
- 2026-05-17 Phase 5 started, two-wave strategy:
  - **Wave 1**: task-003 — palette + colors + ThemeOverlay + MainPagerAdapter + MainActivity/Fragment plumbing + RealmFoldersContainer.folderOfTasksList3 + **single migration v3→v4 (включает все поля task-002: SectionObject schema, TaskObject.sectionId, TaskObject.position + backfill)** + FolderTaskRealmController.group=2
  - **Wave 2** (after Wave 1 commit): parallel task-001 (BottomSheet) + task-002 (sections impl, БЕЗ migration — она уже в Wave 1)

## Cross-task dependencies
- task-002 + task-003 трогают общую Realm migration → совместная `SCHEMA_VERSION=4`. Phase 3 должна спроектировать миграцию одним шагом, чтобы Phase 5 (implementation) могла мерджиться без race.
- task-001 BottomSheet принимает текущую палитру; зависит от того, что `CanaryPalette` уже доступна → task-003 палитру делает первой.
- Секции (task-002) работают во всех 3 task-табах, включая Tasks3 (task-003) — UI-агенты должны учитывать обоих.
