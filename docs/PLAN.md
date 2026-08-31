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
| type strictness      | Kotlin `allWarningsAsErrors`; `explicitApi()` on both `:core:` modules |
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
- Elapsed time in the top bar, between the counter and reset, restarted with the board. The count is part of
  `GameState` and grows through a `Tick` action, so every change — a tap, a reset, a second — goes
  through `reduce` and there is one state. What stays outside is the clock itself: a coroutine in
  the view model decides when a tick happens, which is the part that is neither pure nor
  deterministic.
- Tests: the reducer counts and resets; the view model's ticker is driven by a test scheduler.

**Step 6.4 — The store for solved boards.** *(built)*
- `:core:data` owns how a database is opened and nothing else: no table, no query, no name of its
  own. The app has one database, and a feature adds its table, its queries and the repository over
  them; the database itself sits above the features because Room needs all its entities declared
  together (TRADEOFFS D4).
- The `history/` feature keeps one row per solved board: the size, the variant, the finishing time
  and when it was finished. Only solved boards are recorded — an abandoned game is not a result.
  Its `domain` states what the feature needs — a record, and a `SolveRepository` that adds one,
  deletes one, clears everything and reports the best time for a size; its `data` implements that
  over Room, and the view models are given the interface, never the database. A second source —
  a server, a backup — is a collaborator of the repository, not a change to the screens. Nothing
  is on screen yet. The variant is kept under the puzzle's own key (TRADEOFFS D14).
- Tests: the repository against an **in-memory database**, so the queries run rather than a mock of
  them — a record survives the round trip, a delete removes its own row and no other, clearing
  empties the table, and the best time is the smallest for that size and that variant; and `:core:data`
  against a database the test itself declares, so what opens a connection is exercised without
  a feature to lend it one.

**Step 6.5 — Win state.** *(built)*
- `BoardSnapshot.isSolved` is read at last: the board stops taking taps, the solved board is
  recorded once — not once per recomposition — and the win card names the size, the variant, the
  finishing time and how it stands against the best time before it. `Play again` starts the same
  board over; the clock stops with the board.
- Tests: view model (a solved board is solved once and stays solved until reset, is written to the
  store exactly once, and stops the clock); **Compose UI** (the card appears, the board refuses a
  tap under it).

**Step 6.6 — The records screen.** *(built)*
- Reached from Setup and from the win card's second action: the solved boards in a card per board
  size, fastest first inside it and the smallest board first, each row carrying its rank, its time
  and the day it was finished. A row is deleted on its own; everything is cleared at once behind a
  confirmation, since two taps is the right price for losing every record. With nothing solved the
  screen says so and offers nothing to clear.
- The screen follows the design: `screens/scores.svg` groups by board size, and so does this.
  Three things the design does not answer are ours — deleting one row, clearing everything, and
  the way in from Setup — because without them the screen would be unreachable and the records
  permanent. A card lists every solve of its size, one lazy row at a time, so a board solved a
  hundred times composes only what fits on the screen and each of those solves keeps a delete
  button of its own.
- Tests: the view model (grouped by size smallest first, fastest first inside, deleting takes its
  own row, clearing empties the list); **Compose UI** (the order on the screen is the order in the
  state, a row reports which record to forget, clearing asks first and cancelling clears nothing,
  and the empty screen says so).

*(The design reference shows a bottom bar with Undo and Hint. Neither ships: tapping a queen
already removes it, so Undo corrects nothing that a second tap does not, and hints depend on
the deferred solver. The bar is left out rather than shown with dead buttons.)*

---

## Phase 7 — The victory celebration

**Step 7.1 — The celebration, drawn in Compose.** *(built)*
- A solved board bursts into eighteen pieces — the six `design/screens/win.svg` places and a dozen more,
  because six read as a diagram rather than a celebration once they move — drawn between the scrim
  and the card so the layers stay in the design's order: out from the middle, growing and turning,
  the last of them gone at 3.4 seconds. They travel fastest at the start, leave in a spread rather
  than in formation, and fade only at the end, because the card sits in the middle of the screen:
  a piece that travels evenly spends its brightest moment hidden behind it, which is what the
  first build did.
- No third-party animation library is a dependency: Compose's own `animation-core` produces the
  number, the motion is a pure function of it, and an artifact nothing executes is weight rather
  than readiness (TRADEOFFS D6).
- Tests: the motion is a **pure function** of one number — where a piece is, how big, how turned
  and how visible at a moment between 0 and 1 — so it is unit-tested at both ends and outside
  them; **Compose UI** (the celebration is over a solved board and over no other). What it looks
  like is judged by eye.

---

## Phase 8 — Polish and handover

**Step 8.1 — Accessibility pass.** *(built)*
- Every control carries a description, and on the game board every square says its row, column
  and what stands on it. Setup's preview is described as one board rather than square by square.
- The stepper's buttons and the delete button are 48 dp to a finger whatever they are drawn at.
- A record is spoken as one sentence — rank, time and day — rather than as three items a swipe
  apart, and the delete button beside it keeps its own voice.
- "Best times" and "Solved!" are headings; the win card announces the whole result on its own.
- Placement and the win are felt rather than heard: there is still no sound.
- Contrast is measured and two pairings are left below AA on purpose, recorded in `PROJECT.md`.

**Step 8.2 — Sound.** *(built)*
- Four sounds, generated by `design/sounds.py`, which is checked in beside them: a wooden clack
  when a piece is set down, a lighter one when it is lifted, a dull beat when a move puts a piece
  under attack, and a chord when the board is solved.
- The conflict sound belongs to the *transition* — the move that creates a new attack — not to
  the state, so easing the trouble without ending it is silent.
- Loaded once into a `SoundPool` and released with the composition; silent when the phone is.
- The game screen owns the speaker and everything under it asks for a sound by name, so a test
  hands the board a listener instead.

**Step 8.3 — Placement animation.** *(built)*
- A queen lands rather than appears: she arrives at 70% of her size, overshoots to 114% and
  settles, in a little over a quarter of a second.
- She flinches when a line opens on her — three passes left and right, each smaller than the
  last, over three tenths of a second. The flinch belongs to the move that puts her under attack,
  not to the state, so she does not shake for as long as the attack lasts, and a queen drawn
  already under attack is still.
- Both are pure functions of one number between 0 and 1, as the celebration's motion is, so both
  are unit-tested at their ends, past them, and at the shape between: that the landing overshoots
  and settles, and that each pass of the flinch is smaller than the one before.
- The motion is drawn in a graphics layer, so nothing is measured again as it plays.

---
