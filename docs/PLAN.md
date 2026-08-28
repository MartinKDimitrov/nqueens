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

One command runs everything a commit must pass; CI runs the same command.

`make check` → `./gradlew check buildHealth`, in fail-not-rewrite mode:

| Concern              | Tool                                                             |
|----------------------|-----------------------------------------------------------------|
| format               | Spotless (ktlint) — `spotlessCheck`                             |
| static analysis      | detekt (built upon its default ruleset)                         |
| type strictness      | Kotlin `allWarningsAsErrors`; `explicitApi()` on `:core:domain` |
| tests                | JUnit4 + `kotlin.test`; property tests use a **seeded** `Random`|
| coverage floor       | JaCoCo `jacocoTestCoverageVerification` — fails the build       |
| Compose UI tests     | `createComposeRule` board render tests (added in the Game steps)|
| dependency hygiene   | `com.autonomousapps.dependency-analysis` `buildHealth` (unused/undeclared) |

**Vulnerability audit (deferred from the commit gate).** OWASP dependency-check needs an
NVD API key and a large local database, which makes it too slow and flaky for a
per-commit gate. It is provided as a separate `make audit` target and is not part of
`check`. A conscious deviation from the "everything in check" rule, recorded here.

Coverage floors: **`:core:domain` 90%** (line + branch); **`:app` ViewModel packages 85%**
(gated via a JaCoCo rule on `**/*ViewModel*`); other `:app` code reported, not gated.
Floors activate once each target has code.

The gate runs `check` only. The template's second rule — refusing a commit that changes code
without documenting it — was removed for this project.

Commit gate (run once, Phase 0):

```bash
mkdir -p .githooks && cp ~/.claude/templates/pre-commit .githooks/
chmod +x .githooks/pre-commit
git config core.hooksPath .githooks
```

---

## Phase 0 — Skeleton and gate

**Step 0.1 — Gradle, modules, `check`, commit gate.** *(built)*
- Gradle wrapper pinned to 8.11.1; `settings.gradle.kts` with `:core:domain`, `:app`.
- `gradle/libs.versions.toml`: AGP 8.7.3, Kotlin 2.0.21, androidx-activity, JUnit, detekt,
  Spotless, dependency-analysis. Compose, Hilt and Rive are added in the phases that use them.
- Root build wiring Spotless (all modules) + detekt (code modules) + dependency-analysis
  (root); `:core:domain` gets JaCoCo with a 90% floor; `make check` → `./gradlew check buildHealth`.
- `:app` is a minimal Android shell (empty `ComponentActivity`); `:core:domain` is a pure
  Kotlin library with a smoke test. Both compile under `allWarningsAsErrors`.
- Commit gate hook installed (`core.hooksPath = .githooks`).
- **Check:** `make check` is green on the skeleton (verified).
- **Commit:** `chore: gradle skeleton, modules and check gate`.

---

## Phase 1 — Domain: rules and validation

**Step 1.1 — `Cell` and `PuzzleRules` with the pairwise oracle.** *(built)*
- `:core:domain`: `Cell` (with both diagonal identifiers), `fun interface PuzzleRules`,
  `NQueens` — the pairwise reference.
- Tests: each of the four lines of attack, a non-attacking pair, symmetry, the identity pair.
- **Commit:** `Puzzle rules`.

**Step 1.2 — Conflict detection by counting line occupancy.** *(built)*
- `LineKind`/`Line`/`LineRules` with `NQueensLines`; `conflicts()`, `queensLeft()`,
  `isSolved()`. Counting replaces the four fixed axes the plan first assumed, so the geometry
  stays in the rules rather than in the validator (TRADEOFFS D1).
- Tests: worked examples plus a **property test vs the pairwise oracle** over 500 seeded
  random boards.
- **Commit:** `feat(domain): counter-based conflict validation`.

---

## Phase 2 — Domain: state machine

**Step 2.1 — `GameState`, `GameAction`, `reduce`.** *(built)*
- `GameState(size, queens)` with its invariants; sealed `GameAction` (`Toggle`, `Reset`,
  `NewGame`); pure `reduce(state, action)`.
- Scope narrowed against the original plan: no `history`/`Undo`, no `fixed`/`blocked`, no
  reject path. Nothing in scope produces them, and unused fields in the domain cost more than
  absent ones. Each is one member plus one reducer branch when it is wanted.
- `reduce` takes no rules: conflicts are soft, so a move is never refused for being attacked.
- Tests: table-driven `state × action → expected state`; invariants; purity.
- **Commit:** `feat(domain): pure reducer and game state`.

**Step 2.2 — Board snapshot and cell status.** *(built)*
- `CellStatus` (`EMPTY`, `QUEEN`, `QUEEN_CONFLICT`); `BoardSnapshot` (row-major grid,
  queens left, solved) produced by `snapshotOf(state)` — computed once per state change so
  the UI decides nothing.
- Tests: empty, lone, conflicting and solved boards; row/column not transposed.
- **Commit:** `feat(domain): board snapshot and cell status`.

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

**Step 4.1 — Hilt, navigation, theme, design tokens.**
- `:app`: `Application` with Hilt; Compose Navigation host with `setup` and `game`
  destinations (screens stubbed); Material theme; `NQueensColors`/`NQueensTypography` from
  `design/tokens.json` via `CompositionLocal` (light + dark).
- Hilt module binding `LineRules` (→ `NQueensLines`).
- **Check:** app builds and launches to an empty Setup route (recorded manual pass).
- **Commit:** `feat(app): hilt, navigation, theme and design tokens`.

---

## Phase 5 — Setup screen (MVVM) — *first in user flow*

**Step 5.1 — Setup ViewModel + UI.**
- Board-size input (`n ≥ 4`, validated) and a variant selector (single option: Queens);
  "Start" navigates to Game with the chosen `n`.
- Tests: ViewModel — invalid `n` rejected, valid selection emits start (coverage-gated).
- **Check / Commit:** `feat(setup): board size selection`.

---

## Phase 6 — Game screen (MVI)

**Step 6.1 — Board rendering + tap → toggle.**
- Dynamic `n×n` Compose board; `GameViewModel` exposing `StateFlow<GameUiState>` and
  `onAction`; tap dispatches `Toggle`; queens rendered.
- Tests: ViewModel (tap places/removes; maps to `GameUiState`); **Compose UI** (queen
  renders on tapped cell).
- **Check / Commit:** `feat(game): interactive board with place/remove`.

**Step 6.2 — Conflict highlight + queens-left counter.**
- `QUEEN_CONFLICT` styling; live counter; Compose conflict shake.
- Tests: ViewModel (conflicting placement flagged; counter decrements); **Compose UI**
  (conflict styling shown).
- **Check / Commit:** `feat(game): real-time conflict highlight and counter`.

**Step 6.3 — Elapsed timer + reset.**
- Elapsed-time display (not persisted); `Reset` action clears to `fixed` and restarts the
  timer.
- Tests: timer selector; reset clears board and timer.
- **Check / Commit:** `feat(game): elapsed timer and reset`.

**Step 6.4 — Win state + celebration effect.**
- Win detection → `GameEffect.Solved` (Channel, one-shot); win overlay.
- Tests: ViewModel (solved board emits Solved exactly once); **Compose UI** (win overlay).
- **Check / Commit:** `feat(game): win state and overlay`.

*(The design reference shows a bottom bar with Undo and Hint. Neither ships: tapping a queen
already removes it, so Undo corrects nothing that a second tap does not, and hints depend on
the deferred solver. The bar is left out rather than shown with dead buttons.)*

---

## Phase 7 — Rive victory animation

**Step 7.1 — Rive glue with Compose fallback.**
- `ui` maps `GameEffect.Solved` → Rive state-machine input `celebrate` (artboard
  `NQueens`, machine `game`); Compose-native celebration behind a flag if the `.riv` is
  absent (TRADEOFFS D6).
- Tests: **unit test of the pure `GameEffect → Rive input` mapping**; **Compose UI** test
  that the fallback path renders. Visual polish is a recorded manual note.
- **Commit:** `feat(app): rive victory celebration`.

---

## Phase 8 — Polish and handover

**Step 8.1 — Accessibility pass + README.**
- Content descriptions for cells; optional SFX for placement/win; `README.md`
  (build/test/run + architecture decisions, pointing to `docs/`).
- **Check / Commit:** `docs: readme and accessibility polish`.

---
