# Cross-Cutting Gaps & Risks — Compose Migration

> Completeness-critic pass over all subsystem specs + the migration plan. These are
> behaviors / edge cases / cross-subsystem risks **not** fully covered by any single spec.
> Resolve each before or during the phase it touches. Severity: HIGH = address explicitly.

## HIGH

### G1. Realm transaction atomicity vs Compose recomposition
Realm writes are **synchronous on the main thread**; Compose recompositions fire at
arbitrary times. A recomposing screen can read a `RealmResults`/`RealmList` mid-transaction
or just after a write but before the new UI state is emitted.
**Mitigation (decide in Phase 1):** UI must *never* read live Realm objects. Repositories
emit **detached `copyFromRealm` snapshots** only; ViewModels hold immutable DTO state.
Writes go through controllers and the change-listener re-emits the next snapshot. No live
Realm object crosses into composition.

### G2. Live-object invalidation during JSON restore
`MainActivity.refreshAfterRestore()` deletes all containers and reimports. Every tab holds
live Realm pointers that go invalid mid-restore; the pager pre-creates neighbors, so a
neighbor can recompose against stale data → crash/empty.
**Mitigation:** Restore runs as a single transaction; afterwards repositories re-query and
re-emit. With G1 (detached snapshots) the UI shows last-good state until the new snapshot
arrives. Add an explicit "restoring" gate state that suspends list rendering.

### G3. Back-press state-machine atomicity (panel + action-mode + pager)
The cascade (panel collapse → action-mode dismiss → note-detail back → double-tap exit) is
under-specified when states overlap: back during panel mid-animation, action-mode active
*and* panel expanding, pager mid-swipe + back.
**Mitigation:** Model one explicit `BackTarget` priority resolved from a single snapshot of
all states each press; `BackHandler(enabled=…)` layered so only the top-priority handler is
active. Define behavior during animation (treat in-flight panel as expanded).

## MEDIUM-HIGH

### G4. Daily-reset boundary (timezone / DST / app kept alive)
Reset compares `DAY_OF_YEAR + YEAR` and is checked only in `onResume()`. Unspecified: which
timezone defines "day", DST transitions, leap-year wrap, and behavior when the app stays
foregrounded across midnight (counter goes stale until next resume).
**Mitigation:** Pin "day" to **device-local midnight**; document it. Re-check on
`ON_RESUME` *and* on a midnight alarm/`Lifecycle` tick if the app is alive. Verify Feb 29 /
DST explicitly.

## MEDIUM

### G5. Multi-category task position consistency
`position` is per-task, not per-category. Drag-reorder in folder A doesn't reorder the same
task in extra-categories B/C; deleting an extra folder leaves `position` semantics
undefined.
**Decision needed:** Define intended behavior — position is **per-primary-folder** and
extras render in primary order (simplest), or positions are per-membership (needs schema).
Recommend the former (no schema change). Document in `tasks` spec.

### G6. Old-backup JSON compatibility / no version field
Restore relies on Gson leaving missing fields null (`normalizeExtraFolderIds()` patches the
multi-category case). There's **no version field** in the JSON, so future fields will
silently default and could corrupt queries.
**Mitigation now:** keep the null-coalesce normalizers; **add a `formatVersion` int** to new
backups (back-compat: absent = legacy) so future migrations can branch. Do NOT change how
old backups are read. Covered partly in `sync` spec — make it explicit.

### G7. Section auto-expand-on-hover (~400ms) may be infeasible in Compose drag libs
`sh.calvin.reorderable` exposes `onMove`, not hover/proximity dwell. The 400ms
ItemTouchHelper dwell-to-expand isn't directly portable.
**Decision:** Treat as "delight, not requirement." Fallback: dragging over a collapsed
header for >N onMove ticks triggers expand, or require manual expand first. Pick during
Phase 2; don't block parity on it.

## Phase ordering implications
- G1 is a **Phase 1 architectural invariant** (detached snapshots) — everything downstream
  depends on it. Get it right first.
- G3 spans the shell (Phase 0/2) — implement the resolver when the panel + action-mode land
  in Phase 2, not before.
- G6 touches Phase 4 (sync); G4/G5/G7 touch Phase 2 (tasks).
