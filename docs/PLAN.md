# N-Queens Puzzle — Implementation Plan

Derived from `docs/PROJECT.md` and `docs/TRADEOFFS.md`. Steps are ordered so each one is
independently verifiable; a step's `check` must pass before the next begins.

## Conventions

- **Code delivery.** Code is generated here and typed into Android Studio by hand: one
  executable unit per block, in the order below; wait for the result of each before the
  next. Each block says which module/file it belongs to.
- **Definition of done (every step).** `check` is green → **commit + push** (done manually
  by the developer). Documentation moves with the code in the same commit; when a step
  changes behaviour, the relevant doc/docstring is updated in that commit.
- **Commit messages.** English, imperative, no authorship/co-author trailer.
- **Repo root.** The `nqueens` project directory, opened as the Android Studio project;
  `docs/` and `design/` live in the same repo.

## Initial commit (developer, before Phase 0)

The repo is initialised empty on `main`. The developer makes the **first commit** with the
already-present `docs/` (PROJECT, TRADEOFFS, PLAN) and `design/` (Lovable reference:
tokens, screen SVGs, queen icon) and `.gitignore` — no code yet.

Suggested message: `docs: project, tradeoffs, plan and design reference`.

## The `check` command

One command runs everything a commit must pass. There is no CI: the pre-commit hook is what
runs it.

`make check` → `./gradlew check buildHealth`, in fail-not-rewrite mode. Every row below fails
on a real defect:

| Concern              | Tool                                                             |
|----------------------|-----------------------------------------------------------------|
| format               | Spotless (ktlint) — `spotlessCheck`                             |
| static analysis      | detekt (built upon its default ruleset)                         |
| type strictness      | Kotlin `allWarningsAsErrors`; `explicitApi()` on `:core:domain` |
| tests                | JUnit4 + `kotlin.test`; property tests use a **seeded** `Random`|
| coverage floor       | `jacocoTestCoverageVerification` for the domain, `viewModelCoverageInputs` + `viewModelCoverageVerification` for `:app` |
| dependency hygiene   | `dependency-analysis` `buildHealth`, applied to **every module**, failing on any advice |
| Android resources    | `lint` with `warningsAsErrors`; version advisories excluded            |

**Vulnerability audit (not built).** OWASP dependency-check needs an NVD API key and a large
local database, which makes it too slow and flaky for a per-commit gate. It is not wired at all
rather than wired and skipped. A conscious deviation from the "everything in check" rule.

Coverage floors: **`:core:domain` 90% line and 90% branch**, both counters named explicitly
because JaCoCo otherwise measures instructions; **`*ViewModel*` classes in `:app` 85% line**.
No other `:app` code is measured at all: there is no `jacocoTestReport` in `:app`, so the
screens appear in no coverage report. They are checked by running them.

The hook in `.githooks/pre-commit` runs `check` and nothing else. It began as a template that
also refused a commit changing code without documenting it; that rule was taken out, so the file
in this repository is the one to use — copying the template over it would put the rule back.

Point git at it once:

```bash
git config core.hooksPath .githooks
```

---

## Phase 0 — Skeleton and gate

**Step 0.1 — Gradle, modules, `check`, commit gate.** *(built)*
- Gradle wrapper pinned to 8.11.1; `settings.gradle.kts` with `:core:domain`, `:app`.
- `gradle/libs.versions.toml` starts with AGP 8.7.3, Kotlin 2.0.21, androidx-activity, JUnit,
  detekt, Spotless and dependency-analysis; everything else joins it in the step that first
  needs it.
- Root build wiring Spotless and detekt across the modules, and dependency-analysis into each
  of them — applied to the root alone it reports on nothing. `:core:domain` gets JaCoCo floors;
  `make check` → `./gradlew check buildHealth`.
- `:app` starts as a minimal Android shell and `:core:domain` as an empty pure-Kotlin library.
  Both compile under `allWarningsAsErrors`.
- Commit gate hook installed (`core.hooksPath = .githooks`).
- **Check:** `make check` is green on the skeleton (verified).

---

## Phase 1 — Domain: rules and validation

**Step 1.1 — `Cell` and `PuzzleRules` with the pairwise oracle.** *(built)*
- `:core:domain`: `Cell` (with both diagonal identifiers), `fun interface PuzzleRules`,
  `NQueens` — the pairwise reference.
- Tests: each of the four lines of attack, a non-attacking pair, symmetry, the identity pair.

**Step 1.2 — Conflict detection by counting line occupancy.** *(built)*
- `LineKind`/`Line`/`LineRules` with `NQueensLines`; `conflicts()` and `queensLeft()`. Conflicts
  are found by counting occupancy per line, so the geometry stays in the rules rather than in the
  validator (TRADEOFFS D1).
- Tests: worked examples plus a **property test vs the pairwise oracle** over 500 seeded
  random boards.

---

## Phase 2 — Domain: state machine

**Step 2.1 — `GameState`, `GameAction`, `reduce`.** *(built)*
- `GameState(size, queens)` with its invariants; sealed `GameAction` (`Toggle`, `Reset`,
  `NewGame`); pure `reduce(state, action)`.
- No `history`/`Undo`, no `fixed`/`blocked`, no reject path: nothing in scope produces them, and
  each is one member plus one reducer branch when it is wanted.
- `reduce` takes no rules and is total: conflicts are soft, so a move is never refused for being
  attacked, and an impossible action leaves the state unchanged rather than throwing.
- Tests: table-driven `state × action → expected state`; invariants; purity.

**Step 2.2 — Board snapshot and cell status.** *(built)*
- `CellStatus` (`EMPTY`, `QUEEN`, `QUEEN_CONFLICT`); `BoardSnapshot` (row-major grid,
  queens left, solved) produced by `snapshotOf(state, rules)` — computed once per state change so
  the UI decides nothing.
- Tests: empty, lone, conflicting and solved boards; row/column not transposed.

---

## Phase 3 — Domain: solver *(not built)*

**Step 3.1 — Bitmask solver with cache.** *(deferred)*
- A `Solver` exists only to serve hints and dead-end warnings, and neither is in scope. Built
  now it would be tested code that nothing calls, so it waits for the feature that needs it.
- When it lands: bitmask backtracking, one solution per `n` cached, verified against the known
  solution counts (OEIS A000170: 4→2, 5→10, 6→4, 8→92) with every solution re-checked against
  the pairwise oracle.

---

## Phase 4 — App scaffold

**Step 4.1 — Navigation, theme, design tokens.** *(built)*
- `:app`: Compose Navigation host with `setup` and `game` destinations (the game one stubbed);
  Material theme carrying `NQueensTypography`, with the
  board's own colours beside it in `BoardColors` through a `CompositionLocal` (light + dark).
  Both are transcribed from `design/tokens.json` by hand; nothing reads it at build time.
- **Check:** the app builds and opens on the Setup screen, checked by hand on an emulator.

---

## Phase 5 — Setup screen (MVVM) — *first in user flow*

**Step 5.1 — Setup view model and screen.** *(built)*
- A stepper between `MIN_BOARD_SIZE` and `LARGEST_PLAYABLE_BOARD`, a board drawn at the chosen size, a
  variant row with the one puzzle that exists, and a "Start" that carries the size to the game
  route.
- Tests: the view model clamps at both ends and starts at the default; coverage-gated at 85%.
  The screen itself has no automated test — `check` does not run a composable.

---

## Phase 6 — Game screen (MVI)

**Step 6.1 — Board rendering + tap → toggle.**
- Hilt arrives here: `GameViewModel` is the first class with a dependency to inject
  (`LineRules` → `NQueensLines`).
- Dynamic `n×n` Compose board; `GameViewModel` exposing `GameUiState` as `mutableStateOf`
  (TRADEOFFS D13) and one `onAction`; tap dispatches `Toggle`; queens rendered.
- Tests: ViewModel (tap places/removes; maps to `GameUiState`); **Compose UI** (queen
  renders on tapped cell).

**Step 6.2 — Conflict highlight + queens-left counter.**
- `QUEEN_CONFLICT` styling; live counter; Compose conflict shake.
- Tests: ViewModel (conflicting placement flagged; counter decrements); **Compose UI**
  (conflict styling shown).

**Step 6.3 — Elapsed timer + reset.**
- Elapsed-time display (not persisted); `Reset` action clears to `fixed` and restarts the
  timer.
- Tests: timer selector; reset clears board and timer.

**Step 6.4 — Win state + celebration effect.**
- Win detection → `GameEffect.Solved` (Channel, one-shot); win overlay.
- Tests: ViewModel (solved board emits Solved exactly once); **Compose UI** (win overlay).

*(The design reference shows a bottom bar with Undo and Hint. Neither ships: tapping a queen
already removes it, so Undo corrects nothing that a second tap does not, and hints depend on
the deferred solver. The bar is left out rather than shown with dead buttons.)*

---

## Phase 7 — Rive victory animation

**Step 7.1 — Rive glue with Compose fallback.**
- The presentation layer maps `GameEffect.Solved` → Rive state-machine input `celebrate` (artboard
  `NQueens`, machine `game`); Compose-native celebration behind a flag if the `.riv` is
  absent (TRADEOFFS D6).
- Tests: **unit test of the pure `GameEffect → Rive input` mapping**; **Compose UI** test
  that the fallback path renders. Visual polish is judged by eye.

---

## Phase 8 — Polish and handover

**Step 8.1 — Accessibility pass.** *(partly built: the README is written and the Setup screen
carries content descriptions; the board's are not, and there is no sound.)*
- Content descriptions for cells; optional SFX for placement/win; `README.md`
  (build/test/run + architecture decisions, pointing to `docs/`).

---
