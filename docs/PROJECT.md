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
  and 12 and starts at 8. A puzzle row shows the only puzzle this build was assembled with (Queens). "Start"
  carries the size to the game route.
- **Play screen.** The board at the chosen size; tapping places a queen and tapping again takes
  her back. Queens that threaten each other are marked as the board changes, a counter shows how
  many are still to place, a clock counts beside it, a strip names the trouble in words, and reset
  clears the board.
- **The solved board.** The clock stops, the board stops answering — a finger's tap and a screen
  reader's alike — and a card over it names the size, the puzzle and the finishing time, says by
  how much it beat the best board of that size and puzzle before it, and offers another game or
  the records. The solved board is written down once; a card that cannot be written costs the
  record and not the game.
- **Records screen.** Reached from Setup and from the win card: a card per board size, every
  solve inside it fastest first, with the moment each was finished, and the total in the header.
  A row is deleted on its own; everything is cleared at once behind a confirmation. With nothing
  solved the screen says so; while the table has not answered it claims nothing; and if it stops
  answering it says that instead.
- **The celebration.** Eighteen pieces burst out of the middle over a solved board, between the
  scrim and the card, and are gone within three and a half seconds. They are drawn under the
  card, which covers the middle of the screen, so what a player sees is about a dozen of them at
  the fullest moment and none at all for the first second, while they are still behind it.
- **Reaching it without sight or a steady hand.** Every control around the board is at least
  48 dp to a finger whatever it is painted at, held to one `TouchTarget` in `theme/Dimens.kt`
  and checked by walking every control on every screen rather than by naming them one at a time.
  The board's own squares are the exception and floor at 24 dp instead, because a square that
  kept the target would put a 12 × 12 board off the screen. A record is spoken as one sentence rather than as a rank, a time
  and a day arriving a swipe apart, and the delete button beside it keeps its own voice. The
  records screen's title and the win card's "Solved!" are headings, and the card announces the
  whole result the moment it arrives, without being touched.
- **What the board answers with.** A piece set down and one lifted are felt and heard, each with
  a sound of its own; a move that puts a piece under attack adds a third, and a solved board a
  fourth. The alarm belongs to the move that causes the attack, not to the state, so easing the
  trouble without ending it is silent. The four are synthesised rather than recorded — struck
  bodies, a burst of noise into a handful of decaying resonances — so nothing in `res/raw/` comes
  from a third party or carries a licence. The game is quiet while the phone is.
- **How a piece moves.** A queen lands rather than appears: she arrives at 70% of her size,
  overshoots to 114% and settles, in a little over a quarter of a second. She flinches when a
  line opens on her — three passes, each smaller than the last — on the move that puts her under
  attack rather than for as long as the attack lasts, so a queen drawn already under attack is
  still. Both are drawn in a graphics layer, so nothing is measured again while they play.

## 3. What the assignment asks for that is not built

Nothing. What is absent from here is absent on purpose, and section 4 says why.

## 4. What is deliberately out of scope

- Three things are remembered between runs: a solved board, the board being played, and which
  palette the player chose. A board the system reclaimed the process from comes back with its
  pieces, its clock and whether its win was written down; a board left by pressing back does not,
  because leaving is a choice.
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

| Module             | Contains                                                                 | Depends on |
|--------------------|--------------------------------------------------------------------------|------------|
| `:core:boardlogic` | `Cell`, `GameState`, `GameAction`, `reduce`, `LineRules`, `conflicts`/`Conflicts`, `BoardSnapshot`/`snapshotOf`. The family, not one puzzle: no puzzle's rules are here. Pure Kotlin, **no Android**. | — |
| `:core:ui`         | The palette, the type scale, the measures, and `formatElapsed`. No screen, no state. | — |
| `:core:solves`     | `RecordSolve` and `SolvedBoard`: one verb, no table, no Android. | — |
| `:core:database`   | `PuzzleDatabase`, every `@Entity` and every `@Dao`, and how a database is opened. Depends on no feature: the tables are declared here and the features are handed their own accessor. | — |
| `:core:settings`   | `ThemeChoice` and `Themes` — which palette the player asked for, over a preferences file. Null is not a third palette: it is a player who has not answered. | — |
| `:core:scope`      | One qualifier and the scope behind it, for work that must outlive the screen that started it. | — |
| `:core:puzzletype` | `Puzzle` and `PuzzleText` — key, words, glyph, sizes, **how many pieces solve a board**, rules — and `Puzzles`, which refuses at assembly what a game module can get wrong. | `:core:boardlogic` |
| `:features:setup`  | Choosing what to play: the stepper, the puzzle picker, the board preview, its view model and its route — and the button that chooses the palette, which is the one thing it writes. | `:core:puzzletype`, `:core:boardlogic`, `:core:settings`, `:core:scope`, `:core:ui` |
| `:features:play`   | The board a puzzle is played on: the squares, the top bar, the win card, the celebration, the sounds, its view model and its route. | `:core:puzzletype`, `:core:solves`, `:core:boardlogic`, `:core:scope`, `:core:ui` |
| `:features:scores` | The boards seen solved: the repository over its table, the screen, and the implementation of `RecordSolve`. | `:core:database`, `:core:solves`, `:core:scope`, `:core:ui` |
| `:games:nqueens`   | One puzzle: `NQueensLines`, the words, the glyph, and one binding into the set. | `:core:puzzletype`, `:core:boardlogic` |
| `:app`             | `MainActivity`, `PuzzlesApplication` and the navigation host. Three files: no table, no query, no rule — what it does decide is which palette the resolved choice means, and everything Compose does not draw: the window behind the app and the two system bars over it. | every module above except `:core:boardlogic`, reached through `:core:puzzletype`, and `:core:scope`, reached through the features that write on it |

`:features:setup` and `:features:play` also take `:games:nqueens` as a **test** dependency: they are drawn and tested
with a real puzzle and compiles against none (TRADEOFFS D16).

**No two modules a person would work in at once depend on each other.** The shell depends on
`:core:puzzletype` and never on a game. A game module contributes its puzzle into a set with one
`@Provides @IntoSet`, the route carries a puzzle's key rather than an assumption
(`play/{puzzle}/{size}`), Setup offers whatever the set holds, and nothing outside a game module
names a puzzle. So a second one is that module and two lines: the `include` and `:app`'s dependency on it.

`Puzzles` is where a game module's mistakes are caught, at assembly rather than on a screen: a
key a route could not carry would make Start do nothing for ever, a transposed range would take
down Setup, and a size the domain refuses would throw during composition. All three are refused
with a message that names the puzzle.

The shell writes a solved board through `:core:solves`, which `:features:scores` implements: the
two features that exchange data depend on a contract neither of them owns, and neither can reach
the other's screens, tables or types.

`:core:boardlogic` holds what every puzzle of this family shares, and nothing that is true of only
one. `MIN_BOARD_SIZE` is a board of one square — the smallest thing that is a board — because
"below four there is no solution" is a fact about queens and not about boards: another puzzle may
have one on
every size. Which boards are worth playing, and how many pieces solve them, are the puzzle's own
answers and travel on `Puzzle.sizes` and `Puzzle.piecesToSolve`; the stepper, the route guard and
the win condition all ask the puzzle rather than assume. The domain's ceiling is where one entry
per square stops being a grid worth building.

Every table lives in `:core:database`, with the database they share and the DAOs over them,
because Room needs every entity declared in one place (TRADEOFFS D4, revised by D18). A feature
owns the repository over its own DAO and nothing below it: `:features:scores` turns rows into
solves and back, and never says how a database is opened.

The module graph is the layering, and the compiler enforces all of it: a dependency that is not
declared cannot be written by accident. `:core:boardlogic` and `:core:solves` compile against the
Kotlin standard library alone. Inside a module the layers are still packages held by convention;
the view models keep their state in Compose's `mutableStateOf`, so `presentation` is not a
framework-free layer (TRADEOFFS D12, D13).

### 5.3 Screens

| Screen    | State                                    | Pattern | Why                                            |
|-----------|------------------------------------------|---------|------------------------------------------------|
| **Setup** | `SetupUiState`, in `SetupViewModel`      | MVVM    | a stepper, a picker and three buttons, none of which changes what another means; MVI would be ceremony |
| **Play**  | `GameState` → `PlayUiState`, in `PlayViewModel` | MVI | one `onAction` over the domain's `reduce`; the screen reads a projection, never the state |
| **Scores** | `ScoresUiState`, in `ScoresViewModel`   | MVVM    | a list read from a repository and two commands over it |

### 5.4 Dependency injection

**Hilt, for what the screens cannot make themselves.** It started as one binding and is now
nine, spread across the modules that own what they bind: the `Puzzle`, the database, its DAO,
the repository over it, the verb a game writes a record through, the clock a record is stamped
with, the scope a write outlives its screen in, the preference file, and the palette read out of
it.

The one that carries the architecture is the first. `:games:nqueens` contributes a single
`@Provides @IntoSet fun puzzle(): Puzzle = Queens`, and that is the whole of what a game module
has to do to exist. Into a **set**, not on its own: two games binding one `Puzzle` would be a
duplicate binding and a failed build, which is the difference between a second puzzle being an
addition and being a replacement. What reads the set is `Puzzles`; the shell asks it for the
puzzle a route named. Setup is given the whole set, because choosing needs it; the board is
  given one puzzle and never learns there are others.

The glyph, the header, the counter, the strip, Setup's subtitle and title, the sizes the stepper
moves between and the descriptions of the squares a piece stands on all come from that one place
instead of being spelled out on each screen.
Every id on it is annotated — `@StringRes`, `@PluralsRes`, `@DrawableRes` — so putting a plain
string where the plural belongs is a failed build rather than a crash at the first conflict.
What stays is each screen's own vocabulary, in its own resources: the board summary's shape,
reset, back, the clock, the empty square and everything the win card says. The records screen's
words are its feature's. Setup's title is the puzzle's own name, so a build of another game is
not headed with this one's.

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
that — `playDestination` in `play/presentation` declares the route, the argument's type and the
guard together, and reads the sizes off the puzzle it was handed, so the policy sits with the
screen that enforces it rather than with the host that joins the screens. The reducer's totality
is the domain's own guarantee; this one is the guard's alone, and `PlayViewModel` still raises
if the size never arrives at all.

Both sides of that are tested from the outside. Seven tests launch `MainActivity` and press Start,
so the Hilt graph, the navigation host and the route argument are exercised as the app assembles
them — including that the rules it was given are the ones that mark an attack, that reset reaches
the state and that back leaves the game. Four more drive the navigation host with its own
controller and send it a size the app cannot play and a puzzle it was not assembled with, which
is the only way to reach the guard, since the stepper cannot produce either and there is no deep
link.

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
puzzle row take a minimum height rather than a fixed one, so the size and the range beneath it
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

A queen's own motion goes one step further and stops one step short. Its two curves are pure
functions of a number between 0 and 1 and are tested at their ends, past them and at the shape
between — that the landing overshoots and settles, that each pass of the flinch is smaller than
the one before. What drives them is tested too, by reading what the composables return frame by
frame: that a queen who arrives lands and one already standing does not, and that a flinch
belongs to the move that attacks her rather than to the attack. What is **not** asserted is the
last inch — that `Square` hands those two numbers to its graphics layer. Motion drawn in a layer
moves nothing a test can measure, so a board that computed the whole animation and drew none of
it would pass every one of them. The same blindness covers the celebration's guard against
bursting a second time on the way back from the records: the flag is there and nothing asserts
it. And the date in a record row keeps `weight(1f)` so that it, rather than the solve time, gives
way when the row runs out of width — measured at the narrowest window and the largest type, that
modifier changes nothing, so no test pins it either.

Three belong to the records screen. The route from Setup to the list is walked — one test presses
`Best times`, comes back through `New game`, and asserts the board it left with — but the one from
the win card is not. The day a record carries is asserted in one
time zone, which the test pins itself, and in whatever locale the test runs under, which it does
not: what the date reads as elsewhere is the platform's business and nothing here proves it. And
that day carries no year, so a board solved last August and one solved this August read alike.

Two more belong to what a solved board writes down. The view model is tested against a fake
repository and the repository against a real database, and no test crosses that seam — the Hilt
graph itself is built by the compiler and exercised by every test that launches the app, but
that a solved board's row reaches the real table is not asserted anywhere. And the clock that
stamps a record is the system's only in production; every test hands the view model one of its
own. The same is true of the speaker: the game screen provides the one that reaches the device
and every screen test provides a listener, so what is asserted is which sound the board asked
for and when, never that anything was audible. What the device does with the request is covered
once, against Robolectric's own pool: that a sound reaches it, that a phone on silent gets
none, and that no two sounds name the same file. What no test reaches is the wiring itself: the
game screen is the only place that provides the real speaker, and deleting that one line leaves
an app that makes no noise and a suite that stays green.

Nine things are known and left as they are. **A board written down while the records are being
cleared can outlive the clearing.** Both writes go to `:core:scope`, which runs one at a time but
is not a queue: writing a record is a read and then a write, clearing is one write, and a coroutine
that suspends twice can finish after one handed over later that suspends once. The player has to
solve a board and clear the table in one movement, and the window is the milliseconds the record
spends between its two halves — but the row does survive an emptied table. Closing it means the two
writes knowing about each other, which costs more than the case does. **Three colour pairings stay below AA**, against
the 4.5:1 the guideline asks of text: the status strip's message, `conflict` on `conflictGlow` at
3.07:1, which is the one the game draws most; the win card's "New best" line, `success` on
`surface` at 3.45:1; and the records screen's **Clear all**, `conflict` on `background` at 4.20:1.
The tokens carry no darker green or red, and all three say in words what the colour says, so
nothing is carried by colour alone — but the three are under the bar and stay there.
**The clock counts while the app is away**: it is a tick counter with no lifecycle of its own, so
on a device that does not freeze the process, minutes spent on another app are added to the
elapsed time and written to the records with it.
**Nothing prunes the table**: every solve is listed
and every one can be deleted, one lazy row at a time, so what is composed is bounded and what is
kept is not — a board solved a thousand times is a thousand rows to scroll. **A failed write, a
failed delete and a failed clear are invisible**: they cost the record silently, because the app
has nowhere to say "not saved"; only a failed *read* is reported, and only on the records screen.
**The first sound of a session may not be heard**: `SoundPool.load`
decodes in the background and a sample played before it is ready is dropped without a word, so
the tap that opens the board — and the one after every return from the records, since the pool is
built per screen — can be silent. Robolectric's pool has no loading at all, so no test can see it.
**Eight modules have no coverage floor**: the domain is gated at 90% line and branch and the view
models at 85% line, in the three modules that hold one. `:core:database`, `:core:ui`, `:core:solves`,
`:core:puzzletype`, `:core:settings`, `:core:scope`, `:games:nqueens` and `:app` are gated by their
own tests alone, and one has none at all: `:core:solves` is one interface and one data class,
with nothing to
run. **The ticker never stops waking**: it is a `while (true)` in the view model's scope, and on a
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
`piecesLeft(pieces, target)` takes the target from the caller, and `snapshotOf` passes on the
number it was handed rather than deciding one; the puzzle decides, on `piecesToSolve`. The vocabulary follows — the domain knows pieces, lines and
conflicts, and the word "queen" survives only in what a puzzle says, which `Puzzle` holds in one
place: its name and its piece, and on its `PuzzleText` the subtitle, the counter's label, the two
square descriptions, the prompt and the plural. Where the queen does survive in the code is in
three names the palette owns — `BoardColors.queen`, `LightQueen`, `DarkQueen` — which are the
shell's word for whatever piece it is asked to draw, and under which a second puzzle's piece would
be drawn. It survived in two more places until the fifth round of audits: `queenTint`/`QUEEN_SCALE`
and the whole of `SquareMotion`'s prose, which described a queen landing because that is what it was
written watching. Both speak of a piece now.

The rules carry the **threats**; the **goal** is beside them on the puzzle. `Puzzle.piecesToSolve`
answers "how many pieces solve a board of this size", `snapshotOf` takes that number rather than
assuming the board's own, and `Puzzles` refuses a goal nobody could reach. The queens say
`{ size -> size }`; a puzzle whose pieces threaten differently would say something else, and the
shell would count it down and declare it solved without knowing what its piece is. The domain's
own tests draw a board with a goal that is not its size, so the number is exercised there — what
is not exercised anywhere is the shell reading it, because no second puzzle exists to read.

`LineRules` describes threats as lines, and this document said until recently that a threat which
is not a line — a knight's move — could not be expressed in it at all. That was wrong, and the
mistake is worth keeping written down because it was believed for a while and repeated by every
reader of it.

A line is whatever set of squares a rule says shares one, and nothing requires that set to be more
than a pair. Give every threatened pair its own line, named by the upper-left of the two and the
leap between them, and both squares compute the same `Line`, occupancy reaches two exactly when
both are occupied, and no third square is ever on it. A knight is then `linesThrough` returning at
most eight lines, `conflicts` is unchanged, and the cost stays `O(k)`. Checked rather than argued:
3,600 random boards from four to twelve, counted by lines and tested pair by pair, agree
everywhere; no pair line ever holds a third square; and the widest index on the largest board the
domain builds is 4,194,303, well inside an `Int`.

That was the last thing standing in the way, and it is gone: `LineKind` is an interface, and the
queens' four axes are declared beside the queens as `QueenAxis`. A game whose pieces threaten
along something else names its own kinds, two games' kinds can never be confused because a `Line`
carries the kind itself rather than a number standing for one, and the domain has stopped holding
a list of directions that were only ever one puzzle's.

`PairwiseRules` stays in the test sources, where it is the independent implementation the property
test is checked against. It is not the escape hatch; it is the oracle.

## 8. Next

The records are grouped by board size alone while the best time is asked for a size **and** a
puzzle — two answers to what is comparable, and only one of them is on screen. With one puzzle
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
| A square's colours  | which background a square takes, and whether a queen is drawn on it and in which colour — as a pure function, and again as pixels on a rasterised board |
| Layout              | at seven window shapes: no square below 24 dp, the whole board reachable, and the details under the board or beside it; Setup's Start still reachable when the screen is short |
| Wiring              | `MainActivity` launched for real: Start opens a board at the chosen size, takes a queen, marks an attack, resets and goes back |
| Route guard         | a size the app cannot play sends the player back to Setup instead of reaching the board |
| Win                 | a solved board is covered by the card, which names it and the finishing time and says by how much it beat the best before it; the squares under the card offer no tap to a finger or to TalkBack; the clock stops with the board and the solve is written down once |
| The clock and the record | a solved board is written down once and no oftener, with the time it took; a board nobody solved is not written down at all; a table that refuses the write leaves the game standing and claims no record, and the next game is still written; a board played again does not inherit the finished game's best time; and the best time it is compared against belongs to that size **and** that puzzle |
| The database over time | a version this build cannot migrate is refused rather than emptied, against a real file, with the rows still there afterwards |
| The celebration     | the motion as a pure function of one number: in the middle and invisible at the start, out where the design puts it, grown, turned and gone at the end, and clamped outside; and that it is drawn over a solved board and over no other |
| The records screen  | the boards are grouped by size and ordered by time on the screen itself, every solve keeps a delete button that names the moment it was finished, the solve time survives the largest font on a narrow phone, clearing stays within reach however long the table is, clearing everything is asked about first and cancelling clears nothing, and the three empty states — not answered yet, nothing solved, cannot be read — each say their own thing |
| Painting            | the two board colours, a piece's glyph, both the background and the glyph of a square under attack, the dark palette, and the leader's badge on the records screen — read as pixels off a rasterised screen |
| Elapsed time        | zero, padding, past an hour, and a negative that reads as the start rather than as `00:-1` |
| Records             | against a real database, not a mock of one: a solved board survives the round trip, a delete takes its own row and no other, clearing empties the table, and a best time belongs to its own size; and the connection itself, against a database the test declares, so what is exercised is how a file is opened rather than which tables are in it |

274 tests, each module carrying its own: 98 in `:features:play`, 52 in `:features:scores`, 44 in `:core:boardlogic`, 26 in `:features:setup`, 18 in `:app`, 10 in `:core:settings`, 9 in `:core:puzzletype`, 6 in `:games:nqueens`, 5 in `:core:ui`, 4 in `:core:database` and 2 in `:core:scope`. All three screens are tested as composables, run on
the JVM under Robolectric, so `check` needs no device. What the tests do not yet cover is
written down in §6.

The gate is one command, `make check`, running formatting, static analysis, warnings-as-errors,
the tests, both coverage floors, dependency hygiene and Android lint. Each row can actually
fail; `docs/PLAN.md` describes them and `docs/TRADEOFFS.md` D9 explains why that matters.
