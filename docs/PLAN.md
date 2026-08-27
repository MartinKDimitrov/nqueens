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

**Step 1.1 — `Cell` and `PuzzleRules` with the pairwise oracle.**
- `:core:domain`: `Cell`, `fun interface PuzzleRules { fun attacks(a, b): Boolean }`,
  `NQueens` implementation (the pairwise reference).
- Tests: rows, columns, both diagonals; a non-attacking pair.
- **Check / Commit:** `feat(domain): puzzle rules and pairwise oracle`.

**Step 1.2 — Counter-based validator.**
- HashMap counters over row, col, `r−c`, `r+c`; `conflicts()`, `isValidMove`,
  `queensLeft`, `isSolved`.
- Tests: **property test vs the oracle** over seeded random boards; edge cases.
- **Check / Commit:** `feat(domain): counter-based conflict validation`.

---

## Phase 2 — Domain: state machine

**Step 2.1 — `GameState`, `GameAction`, `reduce`.**
- `GameState(size, queens, fixed, blocked, history)`; sealed `GameAction`
  (`Toggle`, `Undo`, `Reset`, `NewGame`); `reduce(state, action, rules)`.
- Hard rules in the reducer (`canPlace`; reject blocked/fixed); conflicts stay soft.
- Tests: table-driven `state × action → expected state`, incl. rejects and undo.
- **Check / Commit:** `feat(domain): pure reducer and game state`.

**Step 2.2 — Selectors and view verdicts.**
- `TapResult`, `CellStatus`; selector producing the per-cell status grid + counter + solved.
- Tests: hand-built boards → expected grids.
- **Check / Commit:** `feat(domain): board selectors and cell status`.

---

## Phase 3 — Domain: solver

**Step 3.1 — Bitmask solver with cache.**
- `Solver` interface; bitmask backtracking; one solution per `n` cached; `hasSolution`,
  `safeCells`, `hintNextMove` (built and tested; UI deferred).
- Tests: **solution counts vs golden values** (OEIS A000170: 4→2, 5→10, 6→4, 8→92);
  every produced solution re-checked against the oracle.
- **Check / Commit:** `feat(domain): bitmask solver validated against known counts`.

---

## Phase 4 — App scaffold

**Step 4.1 — Hilt, navigation, theme, design tokens.**
- `:app`: `Application` with Hilt; Compose Navigation host with `setup` and `game`
  destinations (screens stubbed); Material theme; `NQueensColors`/`NQueensTypography` from
  `design/tokens.json` via `CompositionLocal` (light + dark).
- Hilt modules binding `PuzzleRules` (→ `NQueens`) and `Solver`.
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

**Step 6.5 — Undo in the UI.**
- Wire the `Undo` action to a button.
- Tests: ViewModel/UI — Undo dispatches and state restores the previous placement.
- **Check / Commit:** `feat(game): undo`.

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
