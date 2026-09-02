# N-Queens

An Android puzzle game: place `n` queens on an `n×n` board so that no two threaten each other —
no shared row, column, or diagonal.

## State of the work

**The game plays.** Choose a board between 4 and 12 on the Setup screen, then place and remove
queens by tapping. Queens that threaten each other are marked as you go, a counter tracks how
many are still to place, a clock runs beside it, and reset clears the board. Solve it and the
board is covered by a card that names the size, the puzzle and the finishing time, says by how
much it beat your best, and offers another game or the records. Solved boards are kept in a
database and listed under "Best times", where a row is deleted on its own and everything can be
cleared at once. A queen lands with a bounce and flinches when a line opens on her, and every
move is felt and heard — a piece set down, a piece lifted, a piece falling under attack, a board
solved — with the game quiet while the phone is.

What is deliberately absent, and why, is in [`docs/PROJECT.md`](docs/PROJECT.md).

## Build and run

Requirements: **JDK 17**, an Android SDK with **API 35**, and either Android Studio or the
`ANDROID_HOME` environment variable. Gradle itself comes with the wrapper (8.11.1) — nothing to
install.

The SDK is found through `ANDROID_HOME` or through `local.properties`, which is not in the
repository — Android Studio writes it when the project is opened. If neither is set, point the
build at the SDK once:

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

Then, with an emulator running or a device attached:

```bash
./gradlew :app:installDebug
```

Or open the project in Android Studio and press Run.

## Test

One command runs everything a commit has to pass — formatting, static analysis,
warnings-as-errors, the tests, both coverage floors, dependency hygiene and Android lint:

```bash
make check
```

Formatting is fixed rather than reported by:

```bash
./gradlew spotlessApply
```

Just the tests, which need no device:

```bash
./gradlew test
```

That is a warm build. The first run downloads Robolectric's Android runtime (a few
hundred megabytes, cached afterwards) and compiles everything including annotation processing,
which takes a few minutes.

Reports land under each module's `build/reports/` — tests and detekt everywhere, coverage where
JaCoCo runs (`core/boardlogic/`, and the three modules that hold a view model), and Android lint on
every Android module and on the two JVM modules that apply `com.android.lint` for it,
`core/puzzletype/` and `core/scope/`.

To have the gate run before every commit:

```bash
git config core.hooksPath .githooks
```

The hook is local and can be skipped; CI (`.github/workflows/check.yml`) runs the same one command
on `main` and on every pull request, so a check added in one place cannot go missing in the other.

Four tests answer what a workstation cannot — whether the platform's own decoder takes the four
sounds, and what the system draws around the app — and need a device or an emulator, which is why
they are not in `check`. They all live in `:app`, which is what the gate names: a library module
has this task whether or not it has a test to run, and an empty instrumentation APK crashes rather
than reporting nothing.

```bash
make check-device
```

**269 tests**, each module carrying its own — every module but `:core:solves`, which holds
one interface and one data class, and has nothing to run:

| Module             | Tests |
|--------------------|-------|
| `:features:play`   |    98 |
| `:features:scores` |    51 |
| `:core:boardlogic` |    41 |
| `:features:setup`  |    26 |
| `:app`             |    17 |
| `:core:settings`   |    10 |
| `:core:puzzletype` |     9 |
| `:games:nqueens`   |     6 |
| `:core:ui`         |     5 |
| `:core:database`   |     4 |
| `:core:scope`      |     2 |

The domain is gated at 90% line and 90% branch coverage and currently sits at 100% and 98.65%;
view models are gated at 85% line, in each of the three modules that holds one. The screens are covered by
running them, not by counting their lines: code executed under Robolectric is invisible to
JaCoCo, so their coverage reads as zero and means nothing.

## Architecture

Twelve modules, in three groups: **`core/`** is shared and has no screens, **`features/`** is one
capability the player sees, and **`games/`** is one puzzle, contributed into a set. No two modules
a person would work in at the same time depend on each other: what they share is a contract module
neither owns.

A module earns its place if removing it would give somebody access they do not need, or reverse an
arrow. That test has removed one: `:core:database` was briefly a single interface with a single
caller and was folded into the app, and came back only when it was given the tables to hold.

| Module             | What it holds                                                              |
|--------------------|----------------------------------------------------------------------------|
| `:core:boardlogic` | What every board here shares: squares, moves, conflicts, and what is solved |
| `:core:ui`         | The palette, the type, the measures, and the elapsed time two screens print |
| `:core:solves`     | One verb — hand a finished board on, be told the best time before it        |
| `:core:database`   | Every table the app stores, the one database they live in, and its schema   |
| `:core:puzzletype` | What a game module must provide, and which types this build was given       |
| `:core:settings`   | What the player chose and the app remembers between runs: the palette       |
| `:core:scope`      | Where work goes when it must outlive the screen that started it             |
| `:features:setup`  | Choosing what to play: which puzzle, and on how big a board                 |
| `:features:play`   | The board it is played on: placing, conflicts, the clock, winning it        |
| `:features:scores` | The records: the table, the screen, and the one verb it answers             |
| `:games:nqueens`   | One puzzle — queens: its rules, its words, its glyph, and one binding       |
| `:app`             | The navigation, and whatever games this build was assembled with            |

`:core:boardlogic` compiles against the Kotlin standard library alone: the compiler cannot see Android
from it. The rest of the boundaries are the module graph itself, so a dependency that is not
declared cannot be written by accident.

Two edges are worth reading. **The screens know no game**: `:features:play` depends on `:core:puzzletype`
and never on `:games:nqueens`. A game module contributes its puzzle into a set —

```kotlin
@Provides @IntoSet fun puzzle(): Puzzle = Queens
```

— and nothing outside it names a puzzle: the route carries a key (`play/{puzzle}/{size}`), Setup
lists what the set holds, and `Puzzles` refuses at assembly a key a route could not carry, a
board the domain will not build, or two puzzles under one key. So a second puzzle really is a
module and two shared lines — the `include` and the app's dependency on it. **A game knows no records**: `:features:play` writes a
solved board through `:core:solves`, one interface with one method, which `:features:scores`
implements. The two features that exchange data depend on a contract neither of them owns.

### The three decisions worth knowing

**The game is a pure function.** `reduce(state, action)` takes a board and a move and returns
the next board. No clock, no storage, no randomness, and no input can make it throw: a tap
outside the board or a board too small to solve leaves the state alone. Any sequence of moves
replays in a test without a device.

**Conflicts are counted, not compared.** The naive check compares every pair of queens, `O(k²)`.
Instead each queen reports the *lines* she stands on — her row, her column, her two diagonals —
and the board tallies how many queens occupy each line. A queen conflicts when one of her lines
holds more than one. That is `O(k)`, and it removes a whole class of bug for free: a lone queen
counts herself once, so nothing has to remember not to compare a queen with herself.

**The rules are a strategy, and there is no default.** Which lines a piece threatens along comes
from a `LineRules` implementation passed in at the call site, so the conflict detection never
knows which puzzle it is counting for. The parameter has no default value on purpose: a default
would let a call site quietly keep playing N-Queens after a second rule existed.

The trade-offs behind these, including the ones that were rejected, are recorded decision by
decision in [`docs/TRADEOFFS.md`](docs/TRADEOFFS.md).

### Testing approach

Conflict detection is checked against an **independent implementation**: the obvious comparison
of every pair, written in the tests on top of the rule stated pair by pair rather than as lines.
The two answer the same question by different routes, and the property test makes them agree on
500 seeded random boards from 1×1 to 12×12. Agreement is then evidence, rather than a
restatement of the code.

The reducer is table-driven — a list of `state × action → expected state` cases, each carrying
its own description so a failure names itself.

All three screens are tested as composables, run on the JVM under Robolectric so they stay inside
`make check` and need no device. The board's squares carry no text, only colour, so the tests
find them by the same content descriptions that make the board usable with a screen reader —
one piece of work serving both.

Seven of those tests start at `MainActivity` and press Start, so the container, the navigation
host and the route argument are exercised as the app assembles them rather than as a test
assembles them. Without that, the whole wiring could be removed and the suite would stay green.

The screens are drawn and tested with a real puzzle: they take `:games:nqueens` as a
**test** dependency, never a compile one, so its screens meet the words and rules a player meets
while their production code still knows no game. That is why neither screen module has a game on
its release classpath — `:app` does, which is what assembles the app — and why the tests still say
"queen under attack". No second *game* is invented to test against: this build ships one, and a
test that plays a puzzle nobody wrote proves the test rather than the code. Where a screen has to
meet more than one entry — the picker, and moving a size into another puzzle's range — it meets
the queens under a second key and a narrower range, which is a configuration rather than a game.
What is left unexercised is the rest: that the shell reads the goal off the puzzle rather than off
the board. The day a second module lands, its own tests are what turn that into a fact.

## Layout

```
core/boardlogic/      what every board here shares: a square, the lines a
                      rule is written in, what conflicts, and what a move
                      leaves behind — pure Kotlin
core/ui/              theme/   colours, type and measures from docs/design/tokens.json
                      format/  the elapsed time, which two screens print
core/solves/          one interface: hand a finished board on, and be told
                      the best time before it
core/database/        every table, the one database, and how it is opened
          schemas/    the database's schema history, checked in
core/settings/        what the player chose and the app remembers: the palette
core/scope/           where work goes when it must outlive its screen
core/puzzletype/      what a game module must provide — key, words, glyph,
                      sizes, what solves it, rules — and which types this
                      build was assembled with
features/setup/       the size the stepper starts on, the puzzle picker,
                      the board preview, the route and the view model
features/play/        the screen, its route, its state and its view model,
                      and the bar above the board
          board/      the squares, what colour each takes, how a piece
                      lands and flinches
          win/        the card and the celebration behind it
          sound/      the four sounds, and where a screen sends one
                      res/  its words, its two icons, its four sounds
features/scores/      domain/  a solved board, the repository, the clock
                      data/    the repository over its table, and the verb it
                               answers for the shell
                      presentation/ the records screen and its route
games/nqueens/        Queens.kt        the puzzle: words, glyph, sizes, goal
                      NQueensLines.kt  the four lines a queen threatens along
                      QueensModule.kt  one binding, and the app has a game
                      res/             its seven words and its glyph
app/                  MainActivity.kt, PuzzlesApplication.kt, PuzzleNavHost.kt
          androidTest/ the four that need a device
.github/workflows/    the gate again, where nobody can forget to install it
docs/                 what this is, why it is shaped this way, and the plan
          design/     the reference the screens were built from: tokens.json,
                      six screen SVGs, the queen icon
```

## Use of code generation tools

Claude was used throughout: drafting code, reviewing it, and checking the documentation against
what the code does. I made the design decisions and reviewed every change before taking it.
