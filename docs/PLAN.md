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
| Android resources    | `lint` with `warningsAsErrors`, over the test sources as well; version advisories excluded |

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

## The review pass

`check` is what a commit must pass. It is not what makes the code right — it cannot read a
comment that has gone stale, notice a test that would pass on broken code, or find a claim in
a document that the code stopped honouring. That is a separate pass, run before handing the
work over rather than on every commit, by seven independent readers that see only the
repository on disk and never this conversation:

| Reader                     | What it attacks                                                                       |
|----------------------------|---------------------------------------------------------------------------------------|
| `adversarial-code`         | logic: off-by-one and overflow, state that goes stale, two paths that must agree with nothing holding them together |
| `adversarial-architecture` | whether the code has the shape the documents claim — the import graph, layering, dead code |
| `adversarial-style`        | what a linter cannot: comments that have gone wrong, misleading names, error messages nobody can act on |
| `test-quality`             | mutates the code in a copy and reports which mutants the suite lets through            |
| `regression-hunt`          | what the last round of changes broke, including the interactions between the fixes     |
| `verify-findings-closed`   | breaks each reported defect again to see whether the fix actually bites                |
| `docs-claims-audit`        | every factual claim in the documentation against what the code does                    |

Each finding is then either fixed and proved — by breaking the fix and watching the suite fail —
or written down as a known gap in the document that would otherwise overstate the work.

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
- `LineKind`/`Line`/`LineRules` with `NQueensLines`; `conflicts()` and `piecesLeft()`. Conflicts
  are found by counting occupancy per line, so the geometry stays in the rules rather than in the
  validator (TRADEOFFS D1).
- Tests: worked examples plus a **property test vs the pairwise oracle** over 500 seeded
  random boards.

---

## Phase 2 — Domain: state machine

**Step 2.1 — `GameState`, `GameAction`, `reduce`.** *(built)*
- `GameState(size, pieces)` with its invariants; sealed `GameAction` (`Toggle`, `Reset`,
  `NewGame`); pure `reduce(state, action)`.
- No `history`/`Undo`, no `fixed`/`blocked`, no reject path: nothing in scope produces them, and
  each is one member plus one reducer branch when it is wanted.
- `reduce` takes no rules and is total: conflicts are soft, so a move is never refused for being
  attacked, and an impossible action leaves the state unchanged rather than throwing.
- Tests: table-driven `state × action → expected state`; invariants; purity.

**Step 2.2 — Board snapshot and cell status.** *(built)*
- `CellStatus` (`EMPTY`, `PIECE`, `PIECE_CONFLICT`); `BoardSnapshot` (row-major grid,
  pieces left, solved) produced by `snapshotOf(state, rules)` — computed once per state change so
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
- `:app`: Compose Navigation host with `setup` and `game` destinations;
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
  The screen is rendered under Robolectric inside `check`: the stepper's ends, and that Start
  carries the chosen size.

---

## Phase 6 — Game screen (MVI)

**Step 6.1 — Board rendering + tap → toggle.** *(built)*
- Hilt arrives here: `GameViewModel` is the first class with a dependency to inject, and
  `PuzzleModule` provides the `Variant` that carries the rules.
- Dynamic `n×n` Compose board; `GameViewModel` holds `GameState` in a `mutableStateOf`
  (TRADEOFFS D13) and derives the `BoardSnapshot` from it; one `onAction` dispatches `Toggle`;
  queens rendered.
- Tests: view model (tap places and removes; the board follows the state); **Compose UI** (a
  square carrying a queen says so, and a tap reports the square it was made on).

**Step 6.2 — Conflict highlight + queens-left counter.** *(built)*
- `PIECE_CONFLICT` styling; the queens-left pill; a status strip that names the trouble in
  words; a top bar carrying the back button and the reset action, with the board summary under it.
- Tests: view model (conflicting placement flagged; the counter follows; reset clears the
  board; the rules it was given are the ones it plays by); **Compose UI** (conflicting squares
  say so, the counter follows the board, the strip names the trouble, both controls report their
  taps, and the board survives a screen wider than it is tall); **painting** (the square's colours
  and the piece's presence as a pure function, and the same decisions read back as pixels off a
  rasterised board); **navigation** (`MainActivity` launched
  for real: Start opens a board at the chosen size, it takes a queen, an attack is marked, reset
  gives the board back and back returns to Setup; and the host itself refuses a size the app
  cannot play).

**Step 6.3 — Elapsed timer.** *(built)*
- Elapsed time in the top bar's third pill, reset together with the board. The count is part of
  `GameState` and grows through a `Tick` action, so every change — a tap, a reset, a second — goes
  through `reduce` and there is one state. What stays outside is the clock itself: a coroutine in
  the view model decides when a tick happens, which is the part that is neither pure nor
  deterministic.
- Tests: the reducer counts and resets; the view model's ticker is driven by a test scheduler.

**Step 6.4 — The store for solved boards.** *(built)*
- `:core:data` owns how a database is opened and nothing else: no table, no query, no name of its
  own. A feature brings its own `@Database`, the tables it needs and the queries it runs against
  them, and asks `:core:data` for the connection — so it carries its persistence with it
  (TRADEOFFS D4).
- The `history/` feature keeps one row per solved board: the size, the variant, the finishing time
  and when it was finished. Only solved boards are recorded — an abandoned game is not a result.
  Its `domain` states what the feature needs — a record, and a `SolveRepository` that adds one,
  deletes one, clears everything and reports the best time for a size; its `data` implements that
  over Room, and the view models are given the interface, never the database. A second source —
  a server, a backup — is a collaborator of the repository, not a change to the screens. Nothing
  is on screen yet. The variant is kept as its string resource id (TRADEOFFS D14).
- Tests: the repository against an **in-memory database**, so the queries run rather than a mock of
  them — a record survives the round trip, a delete removes its own row and no other, clearing
  empties the table, and the best time is the smallest for that size alone; and `:core:data`
  against a database the test itself declares, so what opens a connection is exercised without
  a feature to lend it one.

**Step 6.5 — Win state.**
- `BoardSnapshot.isSolved` is read at last: the board stops taking taps, the solved board is
  recorded once — not once per recomposition — and the win card names the size, the variant, the
  finishing time and how it stands against the best time before it. `Play again` starts the same
  board over; the clock stops with the board.
- Tests: view model (a solved board is solved once and stays solved until reset, is written to the
  store exactly once, and stops the clock); **Compose UI** (the card appears, the board refuses a
  tap under it).

**Step 6.6 — The records screen.**
- Reached from Setup; the solved boards listed, a row deleted on its own, and everything cleared
  at once behind a confirmation. The win card's second action leads here.
- Tests: **Compose UI** (a row is listed, deleting removes it, clearing empties the list, and the
  empty state says so).

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

**Step 8.1 — Accessibility pass.** *(partly built: the README is written, every control carries
a description, and on the game board every square says its row, column and what stands on it.
Setup's preview is described as one board rather than square by square. There is no sound.)*
- Optional SFX for placement and win; a pass over contrast and touch targets.

---
