# N-Queens Puzzle — Project Description

A description of what this repository contains today. Work that is absent is named as absent —
§3 for what the assignment asks and §4 for what was left out on purpose, with §8 giving the
order it would arrive in.

## 1. Purpose

An Android puzzle game based on the [N-Queens problem](https://en.wikipedia.org/wiki/Eight_queens_puzzle):
the player places `n` queens on an `n×n` board so that no two threaten each other — no shared
row, column, or diagonal.

The project is a take-home assignment followed by an interview in which the codebase will be
extended live. Two goals follow: the game itself, and an architecture whose extension points are
already open, so that a new requirement lands in one predictable place (§7).

Language: Kotlin. UI: Jetpack Compose. Build: Gradle with a version catalog.

## 2. What works today

- **Setup screen.** The board drawn at the chosen size, above a stepper that moves between 4
  and 12 and starts at 8. A variant row shows the only puzzle that exists (Queens). "Start"
  carries the size to the game route.
- **The full game logic, with no screen yet.** Placing and removing queens, live conflict
  detection, the queens-left count and the solved verdict are implemented and tested in
  `:core:domain`. Nothing in the app calls them yet.

The game route is a placeholder that prints the chosen size. The board is not playable.

## 3. What the assignment asks for that is not built

- The **game screen**: the interactive board, conflict highlighting, the queens-left counter,
  reset, the elapsed timer and the win state. The domain behind the board — placing, conflicts,
  the counter and the solved verdict — is written and tested; the timer belongs to the view model
  and does not exist yet either (§8).
- **Placement and victory animation**, and with it Rive.

## 4. What is deliberately out of scope

- Best times, and any persistence at all.
- Hints and dead-end detection, and the solver they would need. A tested solver that nothing
  calls is weight carried for a feature nobody asked for.
- Undo. Tapping a queen already takes her back, so undo corrects nothing a second tap does not.
- Accounts, online leaderboards, cloud sync, multiplayer.

## 5. Architecture

### 5.1 Central idea

**A pure state machine, with the rules as a strategy.** The domain is framework-free and
deterministic: `reduce(state, action)` returns the next state, and never throws. Every new
behaviour is one more action and one more branch. Every puzzle *variant* is a new rules
implementation, and the reducer does not change — conflicts are worked out separately, from the
rules in play.

### 5.2 Modules

| Module         | Contains                                                                 | Depends on     |
|----------------|--------------------------------------------------------------------------|----------------|
| `:core:domain` | `Cell`, `GameState`, `GameAction`, `reduce`, `PuzzleRules`/`LineRules`, `conflicts`, `BoardSnapshot`/`snapshotOf`. Pure Kotlin, **no Android**. | —              |
| `:app`         | One package per screen with its own layers — `setup/domain`, `setup/presentation` — plus a shared `theme/`. | `:core:domain` |

`:core:domain` holds what every screen shares, including `MIN_BOARD_SIZE`: below four the
puzzle has nothing to solve, which is a fact about it. A screen's own rules stay with the
screen — `setup/domain` names the size the stepper starts on, and `LARGEST_PLAYABLE_BOARD`, beside
`MainActivity`, names the largest board this app will play — both the stepper and the game
route answer to it, and neither is something the puzzle cares about. The domain holds boards far larger — up to the point where one
entry per square stops being a grid worth building.

The module boundary is the only layering the build enforces: `:core:domain` compiles against
the Kotlin standard library alone, so the compiler cannot see Android from it. Inside `:app` the
layers are packages held by convention; the view models keep their state in Compose's
`mutableStateOf`, so `presentation` is not a framework-free layer (TRADEOFFS D12, D13).

### 5.3 Screens

| Screen    | State                          | Pattern | Why                                                     |
|-----------|--------------------------------|---------|---------------------------------------------------------|
| **Setup** | board size, in `SetupViewModel`| MVVM    | two inputs and a button; MVI would be ceremony          |
| **Game**  | not built (§8)                 | MVI     | a real state machine, and the screen extended live      |

### 5.4 Dependency injection

**None.** `SetupViewModel` has no collaborators, so `viewModel()` constructs it and a container
would have nothing to hold. The seam that a container would guard is kept honest a cheaper way:
`conflicts` and `snapshotOf` take their rules with **no default**, so no call site can quietly
assume N-Queens. Hilt arrives with the game screen, which is where the first real binding is.

## 6. The vocabulary of verdicts

| Question                   | Nature   | Where the answer comes from                 |
|----------------------------|----------|---------------------------------------------|
| "This move conflicts"      | soft     | `conflicts()` → `CellStatus.QUEEN_CONFLICT` |
| "Is it solved"             | derived  | `BoardSnapshot.isSolved`                    |
| "How many are left"        | derived  | `BoardSnapshot.queensLeft`                  |
| "That move cannot be made" | ignored  | `reduce` returns the state unchanged        |

```kotlin
public enum class CellStatus { EMPTY, QUEEN, QUEEN_CONFLICT }
```

Conflicts are **soft**: a queen under attack is placed and highlighted, never refused. Nothing
in this scope can refuse a tap, so there is no reject path and no blocked or given squares. A
move that cannot be carried out — a tap outside the board, a board too small to have a solution
— leaves the state alone rather than throwing, so the reducer cannot crash the screen it drives.

The same holds one layer up: a board size arriving on the game route that the app cannot play
sends the player back to Setup to choose one, rather than raising. Nothing exercises that branch
— the stepper cannot produce such a size and there is no deep link — so it is read and reasoned
about rather than tested. `NQueensNavHost` takes its controller as a parameter, which is what a
test would need, but the test dependencies for driving a composable are not in the build.

## 7. Extension seams

| # | Kind of request           | Seam                                    | Domain touched? |
|---|---------------------------|-----------------------------------------|-----------------|
| 1 | new variant / rule        | new `LineRules`                         | new class only  |
| 2 | new player action         | new `GameAction` + one `reduce` branch  | reducer only    |
| 3 | new displayed information | new field on `BoardSnapshot`            | projection only |
| 4 | new persistence           | a repository in the feature's `data`    | no              |
| 5 | new presentation          | Compose + `UiState`                     | no              |

Seam 1 is the one the design is built around: `LineRules` supplies the lines a piece threatens
along, and `conflicts` counts occupancy per line, so N-Rooks and N-Bishops are a few lines each
and the validator is untouched.

It carries the **threats** and not the **goal**. That the target is one piece per row lives in
`queensLeft` and in `BoardSnapshot.isSolved`, not in the rules, so a puzzle counting to something
other than `n` would touch those two as well. And "new class only" describes the domain: putting
a variant in front of a player also means somewhere to choose it, which the Setup screen does not
have — its variant row shows one puzzle and opens nothing.

The seam covers puzzles whose threats *are* lines, which is every chess variant worth offering
here. A puzzle threatening along something that is not a line — a knight's move — cannot be
expressed as `LineRules` at all. `PuzzleRules` states such a rule pair by pair and the property
test already uses it, but no production code consumes it: making one playable would mean adding
a pairwise conflict function beside `conflicts` and choosing between the two. That is a change
to the domain, not a new class in it.

## 8. Next

In order: the game board with tap-to-toggle and live conflict highlighting; the queens-left
counter, reset and elapsed timer; the win state; the placement and victory animation. Hilt
arrives with the game view model, which is the first class with a dependency worth injecting.

## 9. Testing

| Layer               | What is tested                                                              |
|---------------------|------------------------------------------------------------------------------|
| Conflict detection  | counting agrees with a **pairwise oracle** over 500 seeded random boards      |
| Reducer             | table-driven `state × action → expected state`; that a move it cannot make leaves the state alone; that the state handed to it is not written through |
| Board invariants    | a board below the minimum and a queen off the board are both refused          |
| Projection          | per-cell statuses, the counter, the solved verdict, and that row and column are not transposed |
| View model          | the size clamps at both ends; **85% line coverage gated** on `*ViewModel*`    |

39 tests: 35 in `:core:domain`, 4 in `:app`. The Setup screen itself has no automated test —
`check` does not run a composable, so the screens are checked by hand on an emulator.

The gate is one command, `make check`, running formatting, static analysis, warnings-as-errors,
the tests, both coverage floors, dependency hygiene and Android lint. Each row can actually
fail; `docs/PLAN.md` describes them and `docs/TRADEOFFS.md` D9 explains why that matters.
