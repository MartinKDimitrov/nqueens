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

Language: Kotlin. UI: Jetpack Compose. DI: Hilt. Build: Gradle with a version catalog.

## 2. What works today

- **Setup screen.** The board drawn at the chosen size, above a stepper that moves between 4
  and 12 and starts at 8. A variant row shows the only puzzle that exists (Queens). "Start"
  carries the size to the game route.
- **Game screen.** The board at the chosen size; tapping places a queen and tapping again takes
  her back. Queens that threaten each other are marked as the board changes, a counter shows how
  many are still to place, a strip names the trouble in words, and reset clears the board.

## 3. What the assignment asks for that is not built

- **Placement animation**: a queen that lands with a bounce, and a shake when she comes under
  attack. The victory celebration is built; the per-cell motion is not.

## 4. What is deliberately out of scope

- Nothing is remembered between runs except a solved board: the game itself, the chosen size and
  a board in progress are all lost when the process dies.
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
| `:core:data`   | `Databases` and the Room implementation behind it: how a database is opened. No entity, no DAO, no query. | —              |
| `:app`         | One package per screen with its own layers — `setup/`, `game/`, `history/` — plus `puzzle/` for which puzzle this is, `format/` for what both screens print, and a shared `theme/`. | `:core:domain`, `:core:data` |

`:core:domain` holds what every screen shares, including `MIN_BOARD_SIZE`: a board of two or
three has no solution at all, and one of one is a single square, so four is where the puzzle
starts being one — a fact about it, not about the app. A screen's own rules stay with the
screen — `setup/domain` names the size the stepper starts on, and `LARGEST_PLAYABLE_BOARD`, in
`puzzle/` beside the variant, names the largest board this app will play — both the stepper and
the game route answer to it, and neither is something the puzzle cares about. The domain holds boards far larger — up to the point where one
entry per square stops being a grid worth building.

A feature that keeps something brings its own `@Database`, its tables and the queries it runs,
and asks `:core:data` for the connection: `history/` owns `puzzle.db`, states what it needs as
`SolveRepository` in its `domain` and implements it over Room in its `data`, so what a view model
is handed is the interface and a second source later is a change behind it (TRADEOFFS D4).

The module boundary is the only layering the build enforces: `:core:domain` compiles against
the Kotlin standard library alone, so the compiler cannot see Android from it. Inside `:app` the
layers are packages held by convention; the view models keep their state in Compose's
`mutableStateOf`, so `presentation` is not a framework-free layer (TRADEOFFS D12, D13).

### 5.3 Screens

| Screen    | State                                    | Pattern | Why                                            |
|-----------|------------------------------------------|---------|------------------------------------------------|
| **Setup** | `SetupUiState`, in `SetupViewModel`      | MVVM    | two inputs and a button; MVI would be ceremony |
| **Game**  | `GameState` → `GameUiState`, in `GameViewModel` | MVI | one `onAction` over the domain's `reduce`; the screen reads a projection, never the state |

### 5.4 Dependency injection

**Hilt, for one binding.** `GameViewModel` needs to know which puzzle it is playing, and
`PuzzleModule` provides the `Variant` — the piece it is played with, the lines that piece
threatens along, and the words the game says about it: its name, the counter's label, what a
square announces to a screen reader, the idle prompt and the conflict plural. Both view models
receive it, so a second variant is another `Variant` and one changed binding, and the glyph, the
header, the counter, the strip, Setup's subtitle and the descriptions of the squares a piece
stands on follow from it.
Every id on it is annotated — `@StringRes`, `@PluralsRes`, `@DrawableRes` — so putting a plain
string where the plural belongs is a failed build rather than a crash at the first conflict.
What stays is the app's own vocabulary: the board summary's shape, reset, back, the empty square,
and Setup's title, which names the app rather than the puzzle.

The binding is what a container is for, but it is not the only thing keeping the seam honest:
`conflicts` and `snapshotOf` take their rules with **no default**, so no call site can quietly
assume N-Queens even without injection.

## 6. The vocabulary of verdicts

| Question                   | Nature   | Where the answer comes from                 |
|----------------------------|----------|---------------------------------------------|
| "This move conflicts"      | soft     | `conflicts()` → `CellStatus.PIECE_CONFLICT` |
| "Is it solved"             | derived  | `BoardSnapshot.isSolved`                    |
| "How many are left"        | derived  | `BoardSnapshot.piecesLeft`                  |
| "How many are attacked"    | derived  | `BoardSnapshot.piecesUnderAttack`           |
| "That move cannot be made" | ignored  | `reduce` returns the state unchanged        |

```kotlin
public enum class CellStatus { EMPTY, PIECE, PIECE_CONFLICT }
```

Conflicts are **soft**: a queen under attack is placed and highlighted, never refused. Nothing
in this scope can refuse a tap, so there is no reject path and no blocked or given squares. A
move that cannot be carried out — a tap outside the board, a board too small to have a solution
— leaves the state alone rather than throwing, so the reducer cannot crash the screen it drives.

The same holds one layer up: a board size arriving on the game route that the app cannot play
sends the player back to Setup to choose one, rather than raising. The game destination owns
that — `gameDestination` in `game/presentation` declares the route, the argument's type and the
guard together, and takes the sizes it will play as a parameter, so the policy stays at the
composition root while the contract stays with the screen that reads it. The reducer's totality
is the domain's own guarantee; this one is the guard's alone, and `GameViewModel` still raises
if the size never arrives at all.

Both sides of that are tested from the outside. Seven tests launch `MainActivity` and press Start,
so the Hilt graph, the navigation host and the route argument are exercised as the app assembles
them — including that the rules it was given are the ones that mark an attack, that reset reaches
the state and that back leaves the game. Three more drive the navigation host with its own
controller and send it a size the app cannot play, which is the only way to reach the guard,
since the stepper cannot produce such a size and there is no deep link.

What a Compose test cannot see is what was painted: it finds squares by content description,
and that does not change when a colour does. So the square's decisions — which background, and
whether a queen is drawn and in which colour — are a pure function beside the composable, and
tested there. What a rendered test can see is asserted at four window shapes: that a square is on
the screen and big enough to hit at a phone's window, a landscape one and one too short for the
board, and that the details sit under the board or beside it at the first two.

The pixels are read too, in one test class: Robolectric rasterises for real under
`@GraphicsMode(NATIVE)`, the window is drawn into a bitmap, and the colour under a named square
is compared against the token it should carry — the two board colours, a piece's glyph, both the
background and the glyph of a square under attack, and the dark palette. A `Square` that ignored `SquarePaint` and flooded
the board with one colour would pass everything else and fails this. No golden images: colour
comparisons against `theme/Color.kt`.

What is built and not asserted is worth naming, because a green run is only worth what it
covers. Setup's preview is checked by the
description it carries rather than by the board it draws. The dark palette is chosen explicitly
wherever it is asserted, so the dark branch of the system setting is never taken. A disabled
stepper button is checked by its state, not by the dimming that shows it. Setup's stepper and
variant row take a minimum height rather than a fixed one, so the size and the range beneath it
survive the largest type on a narrow screen — where a fixed box dropped the range line entirely —
and nothing asserts that either. `launchSingleTop` on
the Start route has no test of its own. The counter's digits go through a string resource so they
follow the device's locale like every other number on the screen, which only a test under another
locale would prove. And the variant's words are pinned for every field but two: the conflict
plural, because substituting it would need a second plurals resource and the app has only one;
and the piece's own drawing, because asserting which glyph was painted means comparing shapes
pixel by pixel, which breaks whenever the icon is redrawn without protecting any behaviour.
Hard-coding either back to the queens' would pass.

The celebration is asserted as a number and as a presence, never as a picture: the tests know
where a piece should be at a given moment and that the layer is drawn over a solved board, but
nothing checks that anything was painted, in which colour, or that it moved at all on screen.

Two belong to the records screen. No test presses `Best times` on a running app, so the route
from Setup to the list — and the one from the win card — is wired but never walked; the seven
tests that launch `MainActivity` stop at the board. And the day a record carries is asserted in
one time zone and one locale, which the test pins itself: what the date reads as anywhere else
is the platform's business and nothing here proves it.

Two more belong to what a solved board writes down. Nothing joins the view model to the real
table: the view model is tested against a fake repository and the repository against a real
database, but no test crosses that seam, so a wrong binding in the Hilt graph would be found by
running the app rather than by the suite. And the clock that stamps a record is the system's
only in production — every test hands the view model one of its own.

Three smaller ones, in the tests themselves rather than the code. The counter's label is substituted
in the variant test with the back button's string, so the test cannot tell the label being read
from the variant apart from that one string being hard-coded in its place. The large-type stepper test presses the button by its
semantics node, which lands whatever the button's size, so it catches a control collapsed to
nothing and not one merely too small to hit. And `BoardSnapshot.piecesUnderAttack` is asserted
through the sentence the strip prints rather than in the domain that computes it.

Type size is the one the screens are measured against. Robolectric only measures text at all in
its native-graphics mode, so both screen test classes ask for it, and the assertions there are
about real glyphs: that the board's squares keep their size, and that the control which grows
the board still takes a tap when the type and the display are both at their largest — a
combination that used to leave it nought points wide. The top bar's pill and the status strip
take a minimum height rather than a fixed one, and the status mark scales with the type, so all
three grow with the setting rather than clipping what is inside them.

## 7. Extension seams

| # | Kind of request           | Seam                                    | Domain touched? |
|---|---------------------------|-----------------------------------------|-----------------|
| 1 | new variant / rule        | new `LineRules`                         | new class only  |
| 2 | new player action         | new `GameAction` + one `reduce` branch  | reducer only    |
| 3 | new displayed information | new field on `BoardSnapshot`, if it is a function of the state | projection only |
| 4 | new persistence           | a repository in the feature's `data`    | no              |
| 5 | new presentation          | Compose + `UiState`                     | no              |

Seam 1 is the one the design is built around: `LineRules` supplies the lines a piece threatens
along, and `conflicts` counts occupancy per line, so N-Rooks and N-Bishops are a few lines each
and the validator is untouched.

It carries the **threats** and not the **goal**. The domain says so in its own signature:
`piecesLeft(pieces, target)` takes the target from the caller, and `snapshotOf` is the one place
that decides it is the board size. The vocabulary follows — the domain knows pieces, lines and
conflicts, and the word "queen" survives only in the strings on the screen, which is what a
second variant supplies for itself: its name and its piece travel on `Variant`, and the subtitle,
the counter's label, the two square descriptions, the prompt and the plural on its `VariantText`.
What is left of the queen in the code are the names: the palette, the paint helper and the glyph's
scale — `BoardColors.queen`, `LightQueen`, `DarkQueen`, `queenTint`, `QUEEN_SCALE` — the drawable
`ic_queen`, and in the domain the pairwise oracle `NQueens` beside `NQueensLines`. A second
variant would inherit all of them under names that no longer fit. What the variant does not carry is the goal:
that the target is one piece per row lives in that call
and in `BoardSnapshot.isSolved`, not in the rules, so a puzzle counting to something other than
`n` would touch those two as well. And "new class only" describes the domain: putting
a variant in front of a player also means somewhere to choose it, which the Setup screen does not
have — its variant row shows one puzzle and opens nothing.

The seam covers puzzles whose threats *are* lines, which is every chess variant worth offering
here. A puzzle threatening along something that is not a line — a knight's move — cannot be
expressed as `LineRules` at all. `PuzzleRules` states such a rule pair by pair and the property
test already uses it, but no production code consumes it: making one playable would mean adding
a pairwise conflict function beside `conflicts` and choosing between the two. That is a change
to the domain, not a new class in it.

## 8. Next

In order: the elapsed timer; the win state; the placement and victory animation.

The timer is the one that does not follow §7. A clock is neither a function of `GameState` nor
deterministic, so it cannot become a field on `BoardSnapshot` without putting time in the domain.
It belongs to the view model — a coroutine in `viewModelScope` writing into its own
`mutableStateOf`, beside the board rather than inside it.

## 9. Testing

| Layer               | What is tested                                                              |
|---------------------|------------------------------------------------------------------------------|
| Conflict detection  | counting agrees with a **pairwise oracle** over 500 seeded random boards      |
| Reducer             | table-driven `state × action → expected state`; that a move it cannot make leaves the state alone; that the state handed to it is not written through |
| Board invariants    | a board below the minimum and a queen off the board are both refused          |
| Projection          | per-cell statuses, the counter, the solved verdict, and that row and column are not transposed |
| View model          | input → board; **85% line coverage gated** on `*ViewModel*` classes           |
| Screens             | rendered under Robolectric inside `make check`, with no device: what each square says, that a tap reports the right one, that the counter follows the board, and that the stepper's ends hold |
| Painting            | which background a square takes, and whether a queen is drawn on it and in which colour — as a pure function, and again as pixels on a rasterised board |
| Layout              | at four window shapes: no square below 24 dp, the whole board reachable, and the details under the board or beside it; Setup's Start still reachable when the screen is short |
| Wiring              | `MainActivity` launched for real: Start opens a board at the chosen size, takes a queen, marks an attack, resets and goes back |
| Route guard         | a size the app cannot play sends the player back to Setup instead of reaching the board |
| Win                 | a solved board is covered by the card, which names it and the finishing time and says by how much it beat the best before it; the squares under the card offer no tap to a finger or to TalkBack; the clock stops with the board and the solve is written down once |
| The celebration     | the motion as a pure function of one number: in the middle and invisible at the start, out where the design puts it, grown, turned and gone at the end, and clamped outside; and that it is drawn over a solved board and over no other |
| The records screen  | the boards are grouped by size and ordered by time on the screen itself, a row reports which record to forget, clearing everything is asked about first and cancelling clears nothing, and an empty list says so |
| Records             | against a real database, not a mock of one: a solved board survives the round trip, a delete takes its own row and no other, clearing empties the table, and a best time belongs to its own size; and the connection itself, against a table `:core:data` declares in its own tests |

126 tests: 39 in `:core:domain`, 2 in `:core:data`, 85 in `:app`. Both screens are tested as composables, run on
the JVM under Robolectric, so `check` needs no device. What the tests do not yet cover is
written down in §6.

The gate is one command, `make check`, running formatting, static analysis, warnings-as-errors,
the tests, both coverage floors, dependency hygiene and Android lint. Each row can actually
fail; `docs/PLAN.md` describes them and `docs/TRADEOFFS.md` D9 explains why that matters.
