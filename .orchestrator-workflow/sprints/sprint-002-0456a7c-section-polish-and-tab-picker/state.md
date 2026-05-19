# Sprint 002 state

- **Sprint:** sprint-002-0456a7c-section-polish-and-tab-picker
- **Current phase:** 7-manual-verification (handoff to user)
- **Updated:** 2026-05-19

## Tasks

| Task | Phase | Status | Notes |
|------|-------|--------|-------|
| task-001 — tab color picker | 7 | impl + review clean, awaiting user verification | MaterialButtonToggleGroup + ADR-0001; 1 review round (2 low nits fixed) |
| task-002 — section rails + Empty | 7 | impl + review CLEAN, awaiting user verification | 3 new view types, no merge conflict with task-003 |
| task-003 — section progress counter | 7 | impl + review CLEAN, awaiting user verification | X/Y in header, semi-transparent white |
| task-004 — fix done tasks auto-hide | 7 | impl + review CLEAN, awaiting user verification | one-line fix in SmallTasksFragment.notifyDataChanged |

## Phase log

- 2026-05-19 Phase 1 complete (sprint + 4 tasks scaffolded, commit d2bd0cc)
- 2026-05-19 Phase 2 complete — 28 questions; 2 user / 26 default; requirements finalized (commits 10d345f + per-task finalization)
- 2026-05-19 Phase 3 complete — all 4 designs READY: clean (commits 9940301, ff100b6, cad3e07, 642a130)
- 2026-05-19 sprint-001 finishing touches committed as baseline (f2300cd)
- 2026-05-19 Phase 5 complete — Wave 1 (task-001 c38b124+6c8c745+6f40df2, task-002 51ab10b+8cee11c, task-004 d5fc462+7d68a7e) + Wave 2 (task-003 3b9c891+a3e4e74). All builds green.
- 2026-05-19 Phase 6 complete — reviewers: task-002 CLEAN, task-003 CLEAN, task-004 CLEAN, task-001 2 low nits → fixed (4ba7c97+9ee21f7) → CLEAN.

## Two-wave implementation strategy

- **Wave 1** (parallel): task-001, task-002, task-004 — disjoint file sets
  - task-001: `dialog_add_folder_layout.xml`, `EditDelFolderDialog.java`, `AddFolderDialog.java`, `colors.xml`, `styles.xml`, new `color/tab_swatch_stroke_color.xml`
  - task-002: `TasksRecyclerViewAdapter.java` (flatten + view types), `AdapterItem.java` (Kind enum), `ItemTouchHelperAttacher.java`, new layouts `section_rail_top_view.xml` / `section_rail_bottom_view.xml` / `section_empty_card_view.xml`, `strings.xml`
  - task-004: `SmallTasksFragment.java` only
- **Wave 2** (after task-002 commit): task-003 — rebases on top of task-002's flatten() changes
  - task-003: `section_header_card_view.xml`, `TasksRecyclerViewAdapter.bindSectionHeader` (+ sectionCounts integration into flatten), `strings.xml` (+ section_progress_a11y)

## Review-fix rounds

- task-001: 1 (2 low nits — externalize button labels, extract TabColorPickerHelper)
- task-002: 0 (clean)
- task-003: 0 (clean)
- task-004: 0 (clean)

## Blocked

—

## Risks

- ~~Рабочая директория грязная~~ — RESOLVED (committed as f2300cd baseline).
- ~~task-002 + task-003 трогают section_header_card_view~~ — RESOLVED in Phase 3: task-002 uses separate view types and doesn't touch header layout; only flatten() is shared, handled via wave strategy.
- ADR-0001 (MaterialButtonToggleGroup as picker primitive) — proposed by task-001 design. Will be saved by Wave 1 agent at `.orchestrator-workflow/adr/ADR-0001-material-button-toggle-group.md`.
