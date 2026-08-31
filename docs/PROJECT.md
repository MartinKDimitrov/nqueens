# N-Queens Puzzle — Project Description

A description of what this repository contains today. Work that is absent is named as absent —
§3 for what the assignment asks and §4 for what was left out on purpose, with §8 giving the
order it would arrive in.

## 1. Purpose

An Android puzzle game based on the [N-Queens problem](https://en.wikipedia.org/wiki/Eight_queens_puzzle):
the player places `n` queens on an `n×n` board so that no two threaten each other — no shared
row, column, or diagonal.

Language: Kotlin. UI: Jetpack Compose. DI: Hilt. Build: Gradle with a version catalog.

## 2. What works today

- **Setup screen.** The board drawn at the chosen size, above a stepper that moves between 4
  and 12 and starts at 8. A variant row shows the only puzzle that exists (Queens). "Start"
  carries the size to the game route.
- **Game screen.** The board at the chosen size; tapping places a queen and tapping again takes
  her back. Queens that threaten each other are marked as the board changes, a counter shows how
  many are still to place, a clock counts beside it, a strip names the trouble in words, and reset
  clears the board.
- **The solved board.** The clock stops, the board stops answering — a finger's tap and a screen
  reader's alike — and a card over it names the size, the variant and the finishing time, says by
  how much it beat the best board of that size and variant before it, and offers another game or
  the records. The solved board is written down once; a card that cannot be written costs the
  record and not the game.
- **Records screen.** Reached from Setup and from the win card: a card per board size, every
  solve inside it fastest first, with the moment each was finished, and the total in the header.
  A row is deleted on its own; everything is cleared at once behind a confirmation. With nothing
  solved the screen says so; while the table has not answered it claims nothing; and if it stops
  answering it says that instead.
- **The celebration.** Eighteen pieces burst out of the middle over a solved board, between the
  scrim and the card, and are gone within three and a half seconds.
- **Reaching it without sight or a steady hand.** Every control is at least 48 dp to a finger
  whatever it is drawn at. A record is spoken as one sentence rather than as a rank, a time and a
  day arriving a swipe apart, and the delete button beside it keeps its own voice. Both screen
  titles are headings, and the win card announces the whole result the moment it arrives, without
  being touched. Placing a piece and solving the board are felt; there is no sound.

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
| `:app`         | One package per screen with its own layers — `setup/`, `game/`, `history/` — plus `puzzle/` for which puzzle this is, `storage/` for the one database their tables live in, `format/` for what more than one screen prints, and a shared `theme/`. | `:core:domain`, `:core:data` |

`:core:domain` holds what every screen shares, including `MIN_BOARD_SIZE`: a board of two or
three has no solution at all, and one of one is a single square, so four is where the puzzle
starts being one — a fact about it, not about the app. A screen's own rules stay with the
screen — `setup/domain` names the size the stepper starts on, and `LARGEST_PLAYABLE_BOARD`, in
`puzzle/` beside the variant, names the largest board this app will play — both the stepper and
the game route answer to it, and neither is something the puzzle cares about. The domain holds boards far larger — up to the point where one
entry per square stops being a grid worth building.

A feature that keeps something owns its table, its queries and the repository over them; the
database they live in is the app's, one for all of them, and sits in `storage/` because Room needs
every entity declared in one place (TRADEOFFS D4). `history/` states what it needs as
`SolveRepository` in its `domain` and implements it over Room in its `data`, so what a view model
is handed is the interface and a second source later is a change behind it.

The module boundary is the only layering the build enforces: `:core:domain` compiles against
the Kotlin standard library alone, so the compiler cannot see Android from it. Inside `:app` the
layers are packages held by convention; the view models keep their state in Compose's
`mutableStateOf`, so `presentation` is not a framework-free layer (TRADEOFFS D12, D13).

### 5.3 Screens

| Screen    | State                                    | Pattern | Why                                            |
|-----------|------------------------------------------|---------|------------------------------------------------|
| **Setup** | `SetupUiState`, in `SetupViewModel`      | MVVM    | two inputs and two buttons; MVI would be ceremony |
| **Game**  | `GameState` → `GameUiState`, in `GameViewModel` | MVI | one `onAction` over the domain's `reduce`; the screen reads a projection, never the state |
| **Scores** | `ScoresUiState`, in `ScoresViewModel`   | MVVM    | a list read from a repository and two commands over it |

### 5.4 Dependency injection

**Hilt, for what the screens cannot make themselves.** It started as one binding and is now six,
across five modules in three files: the `Variant`, the database connection, the puzzle's
database, its DAO, the repository over it and the clock a record is stamped with. `GameViewModel` needs to know which
puzzle it is playing, and `PuzzleModule` provides the `Variant` — the piece it is played with, the lines that piece
threatens along, and the words the game says about it: its name, the counter's label, what a
square announces to a screen reader, the idle prompt and the conflict plural. Both view models
receive it, so the glyph, the header, the counter, the strip, Setup's subtitle and the
descriptions of the squares a piece stands on all come from one place instead of being spelled
out on each screen.
Every id on it is annotated — `@StringRes`, `@PluralsRes`, `@DrawableRes` — so putting a plain
string where the plural belongs is a failed build rather than a crash at the first conflict.
What stays is the app's own vocabulary: the board summary's shape, reset, back, the clock, the
empty square, everything the win card says, everything the records screen says, and Setup's title,
which names the app rather than the puzzle.

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

Conflicts are **soft**: a queen under attack is placed and highlighted, never refused. No rule of
the puzzle refuses a tap, so there is no reject path and no blocked or given squares — the one
thing that does refuse is a finished game, where the board and the top bar stop answering
altogether. A
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
tested there. What a rendered test can see is asserted at seven window shapes: that a square is on
the screen and big enough to hit at a phone's window, a landscape one and one too short for the
board, and that the details sit under the board or beside it at the first two.

The pixels are read too, in two test classes: Robolectric rasterises for real under
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

Three belong to the records screen. No test presses `Best times` on a running app, so the route
from Setup to the list — and the one from the win card — is wired but never walked; the seven
tests that launch `MainActivity` stop at the board. The day a record carries is asserted in one
time zone, which the test pins itself, and in whatever locale the test runs under, which it does
not: what the date reads as elsewhere is the platform's business and nothing here proves it. And
that day carries no year, so a board solved last August and one solved this August read alike.

Two more belong to what a solved board writes down. The view model is tested against a fake
repository and the repository against a real database, and no test crosses that seam — the Hilt
graph itself is built by the compiler and exercised by the seven tests that launch the app, but
that a solved board's row reaches the real table is not asserted anywhere. And the clock that
stamps a record is the system's only in production; every test hands the view model one of its
own.

Five things are known and left as they are. **Two colour pairings stay below AA**: the win
card's "New best" line is `success` on `surface` at 3.45:1 and the records screen's **Clear all**
is `conflict` on `background` at 4.20:1, against the 4.5:1 the guideline asks of text. The tokens
carry no darker green or red, and both places say in words what the colour says, so nothing is
carried by colour alone — but the two are under the bar and stay there.
**Nothing prunes the table**: every solve is listed
and every one can be deleted, one lazy row at a time, so what is composed is bounded and what is
kept is not — a board solved a thousand times is a thousand rows to scroll. **A failed write, a
failed delete and a failed clear are invisible**: they cost the record silently, because the app
has nowhere to say "not saved"; only a failed *read* is reported, and only on the records screen.
**The ticker never stops waking**: it is a `while (true)` in the view model's scope, and on a
solved board it stops counting rather than sleeping. And **the win card counts in seconds**: a
board beaten by nine minutes reads "New best — 540s faster than before" while every other
duration on the screen is minutes and seconds.

Three smaller ones, in the tests themselves rather than the code. The counter's label is substituted
in the variant test with the back button's string, so the test cannot tell the label being read
from the variant apart from that one string being hard-coded in its place. The large-type stepper test presses the button by its
semantics node, which lands whatever the button's size; what it is too small to hit is caught
separately, by measuring the button rather than pressing it. And `BoardSnapshot.piecesUnderAttack` is asserted
through the sentence the strip prints rather than in the domain that computes it.

Type size is the one the screens are measured against. Robolectric only measures text at all in
its native-graphics mode, so all three screen test classes ask for it, and the assertions there are
about real glyphs: that the board's squares keep their size, and that the control which grows
the board still takes a tap when the type and the display are both at their largest — a
combination that used to leave it nought points wide. The top bar's pill and the status strip
take a minimum height rather than a fixed one, and the status mark scales with the type, so all
three grow with the setting rather than clipping what is inside them.

## 7. Where each concern is decided

| Concern                | Decided by                                  | Domain touched? |
|------------------------|---------------------------------------------|-----------------|
| what a piece threatens | `LineRules`                                  | that class only |
| what a move does       | a `GameAction` and one `reduce` branch       | reducer only    |
| what the screen shows  | a field on `BoardSnapshot`, when it is a function of the state | projection only |
| what is kept           | a repository in the feature's `data`         | no              |
| how it is drawn        | Compose and a `UiState`                      | no              |

The first row is the one the domain is built around: `LineRules` supplies the lines a piece
threatens along, and `conflicts` counts occupancy per line, so the validator says nothing about
queens and nothing about which puzzle is being played.

It carries the **threats** and not the **goal**. The domain says so in its own signature:
`piecesLeft(pieces, target)` takes the target from the caller, and `snapshotOf` is the one place
that decides it is the board size. The vocabulary follows — the domain knows pieces, lines and
conflicts, and the word "queen" survives only in what the screens say, which the `Variant` holds
in one place: its name and its piece, and on its `VariantText` the subtitle, the counter's label,
the two square descriptions, the prompt and the plural. Where the queen does survive in the code
is in names — `BoardColors.queen`, `LightQueen`, `DarkQueen`, `queenTint`, `QUEEN_SCALE`, the
drawable `ic_queen`, and in the domain the pairwise oracle `NQueens` beside `NQueensLines`. What
the rules do not carry is the goal: that the target is one piece per row lives in that call and in
`BoardSnapshot.isSolved`, so a puzzle counting to something other than `n` is a change to those
two and not to the rules. Nor does the domain's shape reach the screen: Setup's variant row shows
the one puzzle there is and opens nothing.

`LineRules` says what it says and no more: it describes threats that *are* lines. A threat that is
not — a knight's move — cannot be expressed in it at all. `PuzzleRules` states such a rule pair by pair and the property
test already uses it, but no production code consumes it: making one playable would mean adding
a pairwise conflict function beside `conflicts` and choosing between the two. That is a change
to the domain, not a new class in it.

## 8. Next

The placement animation — a queen that lands, a shake when she comes under attack — and a pass
over sound and touch targets.

The records are grouped by board size alone while the best time is asked for a size **and** a
variant — two spellings of what is comparable, and only one of them is on screen. With one puzzle
they agree; they are written down here because nothing in the code holds them together.

Where time lives is settled and worth stating, because it looks like a violation of §5.1 and is
not: `elapsedSeconds` is a field on `GameState` and grows through a `Tick` action, so a tap, a
reset and a passing second all go through `reduce` and there is one state. The domain stays
deterministic — `Tick` carries no clock, only the instruction to count one — and what is neither
pure nor deterministic, the coroutine deciding when a tick happens, stays in the view model.

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
| Layout              | at seven window shapes: no square below 24 dp, the whole board reachable, and the details under the board or beside it; Setup's Start still reachable when the screen is short |
| Wiring              | `MainActivity` launched for real: Start opens a board at the chosen size, takes a queen, marks an attack, resets and goes back |
| Route guard         | a size the app cannot play sends the player back to Setup instead of reaching the board |
| Win                 | a solved board is covered by the card, which names it and the finishing time and says by how much it beat the best before it; the squares under the card offer no tap to a finger or to TalkBack; the clock stops with the board and the solve is written down once |
| The clock and the record | a solved board is written down once and no oftener, with the time it took; a board nobody solved is not written down at all; a table that refuses the write leaves the game standing and claims no record, and the next game is still written; a board played again does not inherit the finished game's best time; and the best time it is compared against belongs to that size **and** that variant |
| The database over time | a version this build cannot migrate is refused rather than emptied, against a real file, with the rows still there afterwards |
| The celebration     | the motion as a pure function of one number: in the middle and invisible at the start, out where the design puts it, grown, turned and gone at the end, and clamped outside; and that it is drawn over a solved board and over no other |
| The records screen  | the boards are grouped by size and ordered by time on the screen itself, every solve keeps a delete button that names the moment it was finished, the solve time survives the largest font on a narrow phone, clearing stays within reach however long the table is, clearing everything is asked about first and cancelling clears nothing, and the three empty states — not answered yet, nothing solved, cannot be read — each say their own thing |
| Painting            | the two board colours, a piece's glyph, both the background and the glyph of a square under attack, the dark palette, and the leader's badge on the records screen — read as pixels off a rasterised screen |
| Elapsed time        | zero, padding, past an hour, and a negative that reads as the start rather than as `00:-1` |
| Records             | against a real database, not a mock of one: a solved board survives the round trip, a delete takes its own row and no other, clearing empties the table, and a best time belongs to its own size; and the connection itself, against a table `:core:data` declares in its own tests |

168 tests: 40 in `:core:domain`, 3 in `:core:data`, 125 in `:app`. All three screens are tested as composables, run on
the JVM under Robolectric, so `check` needs no device. What the tests do not yet cover is
written down in §6.

The gate is one command, `make check`, running formatting, static analysis, warnings-as-errors,
the tests, both coverage floors, dependency hygiene and Android lint. Each row can actually
fail; `docs/PLAN.md` describes them and `docs/TRADEOFFS.md` D9 explains why that matters.
