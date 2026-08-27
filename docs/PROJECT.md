# N-Queens Puzzle — Project Description

## 1. Purpose and context

An Android puzzle game based on the [N-Queens problem](https://en.wikipedia.org/wiki/Eight_queens_puzzle):
the player places `n` queens on an `n×n` board so that no two threaten each other
(no shared row, column, or diagonal).

Two goals shape the design:

1. **Scope is strictly the brief** — the smallest correct game that satisfies it.
2. **The architecture keeps its extension seams open**, so that a new requirement maps to
   one predictable place in the code (§7) rather than a rewrite.

Language: Kotlin. UI: Jetpack Compose. DI: Hilt. Animations: [Rive](https://rive.app).

## 2. Gameplay scope (v1)

Required by the assignment:

- Player selects a board size, `n ≥ 4` (below 4 there are no solutions).
- An interactive `n×n` board is rendered.
- Tap to place / remove a queen.
- Real-time validation: conflicting queens are highlighted as the board changes.
- A win state is shown when the puzzle is solved.

Nice-to-haves included in v1:

- Counter of queens left.
- Restart / reset.
- Elapsed-time display during play (not persisted in v1).
- Queen-placement and victory animation (§4.5): the victory celebration via Rive, the
  per-cell placement/conflict feedback via Compose.

A puzzle-**variant selector** ships on the Setup screen showing a single option (Queens).
It carries no behaviour beyond N-Queens; it exists so that adding a variant is a
domain-only change behind a UI that already expects it (§7).

## 3. Out of scope

Deferred deliberately, each behind an open seam (§7):

- **Best times / Scores screen** and any persistence (a repository seam).
- **Saved game / resume** (a repository seam).
- **Hints and dead-end detection** (the `Solver` exists and is tested, but has no UI, §4.4).
- **Puzzle variants** (Rooks, Bishops, Amazons, toroidal, blocked cells, given queens).
- Accounts, online/remote leaderboards, cloud sync, multiplayer.

## 4. Architecture overview

### 4.1 Central idea

**A pure state machine plus the rules as a strategy.** The domain is framework-free and
deterministic: `reduce(state, action, rules) -> state`. Every new behaviour is a new
action + one reducer branch (unit-testable in isolation), and every puzzle *variant* is a
new `PuzzleRules` implementation — the reducer never changes.

### 4.2 Modules (v1)

| Module         | Contains                                                        | Depends on     |
|----------------|----------------------------------------------------------------|----------------|
| `:core:domain` | `Cell`, `GameState`, `PuzzleRules`, `reduce`, selectors, `Solver`. Pure Kotlin, **no Android**. | —              |
| `:app`         | Compose UI, ViewModels, Hilt wiring, Rive glue. Feature packages: `setup`, `game`. | `:core:domain` |

`:core:domain` as a separate module lets the compiler **forbid Android in the domain** —
protecting testability and making the boundary visible. A `:core:data` module is added
when persistence lands (Scores / saved game, §8); it is not needed for v1.

### 4.3 Screens and per-screen pattern (v1)

| Screen    | Responsibility                                                    | Pattern | Why                                                            |
|-----------|------------------------------------------------------------------|---------|---------------------------------------------------------------|
| **Setup** | choose `n` (≥4) and the (single-option) variant, start game      | MVVM    | two inputs and a button; MVI would be ceremony                |
| **Game**  | board, conflict highlight, counter, elapsed timer, reset, win    | MVI     | a real state machine — many discrete actions + one-shot effects; this is where live extension happens |

Mixing is deliberate: MVI where behaviour is rich and will grow; MVVM where it is simple
input. Victory celebration is a **one-shot effect** (Channel/SharedFlow), kept out of the
render state so a recompose never re-fires it. (A Scores screen, MVVM, is added with
persistence in §8.)

### 4.4 Dependency injection

Hilt. Bindings of interest in v1:

- `PuzzleRules` — bound to `NQueens` (the single variant); a `@Binds`/qualifier seam for
  future variants.
- `Solver` — the bitmask solver, provided as a singleton; one solution per `n` computed
  once and cached. Built and tested in v1; its UI (hints) is deferred (§3).

### 4.5 Animations

| Animation                         | Carrier | Trigger                                   |
|-----------------------------------|---------|-------------------------------------------|
| Victory celebration               | Rive    | `GameEffect.Solved → Rive input "celebrate"` |
| Queen placement (bounce)          | Compose | placement recomposition                   |
| Conflict (shake / glow)           | Compose | `CellStatus.QUEEN_CONFLICT`               |

Rive contract: artboard `NQueens`, state machine `game`, trigger input `celebrate`. The
domain knows nothing about Rive; only the `ui` layer maps effect → Rive input. The `.riv`
asset is authored in the Rive editor (it cannot be generated from code); if it slips, the
victory animation falls back to Compose (see TRADEOFFS D6).

## 5. The vocabulary of verdicts

The game answers several distinct questions; conflating them is the main design risk.
The split is **hard rules** (the action is refused) vs **soft rules** (the action is
allowed and flagged).

| Question                              | Nature          | How it is expressed                        | Lives in  |
|---------------------------------------|-----------------|--------------------------------------------|-----------|
| "You cannot place here"               | hard            | `canPlace(cell)` → reducer rejects + feedback | reducer   |
| "This move conflicts / doesn't solve" | soft            | `conflicts()` selector → highlight         | selector  |
| "Does a solution still exist / dead end" | derived, costly | `Solver.hasSolution(state)`               | Solver    |
| "Is it solved"                        | derived         | `isSolved()` = `n` queens and 0 conflicts  | selector  |
| "Help / hint" (deferred UI)           | derived         | `safeCells()` cheap · `hintNextMove()` via Solver | selector / Solver |

Types fixed for v1:

```kotlin
sealed interface TapResult {
    data class Placed(val cell: Cell)  : TapResult
    data class Removed(val cell: Cell) : TapResult
    data class Rejected(val reason: RejectReason) : TapResult   // hard rule
}
enum class RejectReason { CELL_BLOCKED, CELL_FIXED }

enum class CellStatus { EMPTY, QUEEN, QUEEN_CONFLICT, BLOCKED, FIXED, HINT }
```

Conflicts are **soft**: a conflicting queen is placed and highlighted, never refused.
Turning a soft rule hard is a one-line policy change, not a rewrite. `BLOCKED`/`FIXED`/
`HINT` render states exist in the enum from v1 (the design legend covers them) but are
only produced by extensions.

## 6. Testing strategy

Logic is separated from UI precisely so it can be tested without a device.

| Layer                | What is tested                                                        |
|----------------------|----------------------------------------------------------------------|
| Conflict validation  | fast counter path vs the **pairwise oracle** (`attacks`) on seeded random boards (property test) |
| Solver               | solution counts vs **known golden values** (OEIS A000170: n=4→2, n=5→10, n=6→4, n=8→92) |
| Reducer              | table-driven `state × action → expected state` cases                 |
| Selectors            | `conflicts`, `queensLeft`, `isSolved` on hand-built boards            |
| ViewModel            | intent → state/effect; **coverage-gated** (§ check)                   |
| Compose UI           | board renders place / conflict / win states (`createComposeRule`)    |
| Manual pass          | a recorded emulator pass per screen step (rendering is not gated by `check`) |

A single `check` command (Gradle) runs format + lint + strict type checks + tests with
coverage floors; CI runs the same command. Real fixtures (the golden solution counts) are
part of the suite, not only author-invented inputs. Property tests use a **seeded**
`Random` for reproducibility.

## 7. Extension seams

Every plausible request maps to one of five seams:

| # | Kind of request              | Seam                                   | Domain touched? |
|---|------------------------------|----------------------------------------|-----------------|
| 1 | new variant / rule           | new `PuzzleRules` implementation       | new class only  |
| 2 | new player action            | new `GameAction` + reducer branch      | reducer only    |
| 3 | new displayed information    | new selector over `GameState`          | selector only   |
| 4 | new persistence              | `Repository` interface + implementation (`:core:data`) | no |
| 5 | new presentation             | Compose + `UiState` mapping            | no              |

See `docs/TRADEOFFS.md` for the reasoning behind each decision and `docs/PLAN.md` for the
step-by-step build.
