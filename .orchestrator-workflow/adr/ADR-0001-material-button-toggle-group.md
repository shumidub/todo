# ADR-0001 — Single-select choice through `MaterialButtonToggleGroup` in dialogs

- **Status:** Accepted
- **Date:** 2026-05-19
- **Sprint:** sprint-002-0456a7c
- **Task:** task-001-tab-color-picker

## Context

We need a compact 3-way picker for choosing a folder's tab (green / blue / yellow) inside a `MaterialAlertDialog`. This is the first time the project needs a single-select choice widget beyond the binary `CheckBox`. The choice we make here will set the precedent for future single-select pickers in the app (e.g. task priority, folder icon).

Alternatives considered:

- `RadioGroup` + custom drawables per state.
- `Spinner` — single visible item until expanded.
- `ChipGroup` with `app:singleSelection="true"`.
- A row of plain `MaterialButton`s with hand-rolled "exactly-one-checked" logic.

## Decision

Use `com.google.android.material.button.MaterialButtonToggleGroup` (already available via the Material 1.11 dependency we ship) with:

- `app:singleSelection="true"`
- `app:selectionRequired="true"` — ensures `getCheckedButtonId()` is never `View.NO_ID`.
- Three `MaterialButton` children styled via `Widget.App.Button.TabColorSwatch.{Green|Blue|Yellow}`.
- Fill colour through `app:backgroundTint` pointing at existing palette colours (`colorBackgroundActivity`, `cornflowerBg`, `canaryBg`) — explicit per-button override so dialog `ThemeOverlay`s don't repaint the swatches.
- Selected-state indication through a 2dp stroke whose colour is driven by a state-list (`tab_swatch_stroke_color.xml`): white when `state_checked="true"`, transparent otherwise. `strokeWidth` itself is constant (a dimen, not a colour-state-list).
- Text colour: white on green/blue swatches; `canaryText` on yellow (white fails WCAG AA against `#FFD93D`).

The pattern lives entirely in XML — no Java logic for the visual toggle behaviour. Java only reads `getCheckedButtonId()` at the confirm step and calls `g.check(id)` once during init.

## Consequences

- The picker keeps the visual language of the tabs regardless of which `ThemeOverlay` the dialog itself is themed under — swatches always render in their native palette colours.
- Future single-select pickers in the project follow the same shape: `MaterialButtonToggleGroup` + per-option `MaterialButton` style + state-list for selected indication.
- No new dependencies, no minSdk change (works on API 24+).
- The `strokeWidth` limitation (must be a single dimen, not state-list) is acceptable because a constant 2dp stroke combined with a transparent unchecked colour gives the same visual result without any code in the fragment.
- Yellow swatch needs an explicit text-colour override per swatch style — documented in `Widget.App.Button.TabColorSwatch.Yellow`.

## Alternatives rejected

- **`ChipGroup`** — chips look tag-like and read visually weaker as colour swatches. Less suited to "pick a colour" semantics.
- **`RadioGroup`** — needs a custom drawable per option per state; verbose XML, no built-in Material ripple, and the visual still ends up close to a toggle button.
- **`Spinner`** — hides options behind a tap, so the user can't see all three colours at once before choosing. Bad for a "pick a tab colour" task that's inherently about comparison.
- **Hand-rolled `MaterialButton` row** — duplicates the `singleSelection` behaviour `MaterialButtonToggleGroup` already gives us for free.
