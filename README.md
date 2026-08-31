# N-Queens

An Android puzzle game: place `n` queens on an `n×n` board so that no two threaten each other —
no shared row, column, or diagonal.

## State of the work

**The game plays.** Choose a board between 4 and 12 on the Setup screen, then place and remove
queens by tapping. Queens that threaten each other are marked as you go, a counter tracks how
many are still to place, a clock runs beside it, and reset clears the board. Solve it and the
board is covered by a card that names the size, the variant and the finishing time, says by how
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

Just the tests, which need no device and take about twenty-five seconds:

```bash
./gradlew :core:domain:test :core:data:testDebugUnitTest :app:testDebugUnitTest
```

That is a warm build. The first run downloads Robolectric's Android runtime (a few
hundred megabytes, cached afterwards) and compiles everything including annotation processing,
which takes a few minutes.

Reports land under each module's `build/reports/` — tests and detekt everywhere, coverage where
JaCoCo runs (`core/domain/` and `app/`, not `core/data/`), and Android lint under `app/` and
`core/data/`, the two modules it runs on.

To have the gate run before every commit:

```bash
git config core.hooksPath .githooks
```

**186 tests**: 40 in the domain, 3 in the data module, 143 in the app. The domain is gated at 90% line and 90% branch
coverage and currently sits at 100% and 98.89%; view models are gated at 85% line. The screens
are covered by running them, not by counting their lines: code executed under Robolectric is
invisible to JaCoCo, so their coverage reads as zero and means nothing.

## Architecture

Three modules that hold code, two of them under a `:core` container:

| Module         | What it holds                                                                  |
|----------------|--------------------------------------------------------------------------------|
| `:core:domain` | The whole game as pure Kotlin: board, rules, state machine, conflict detection  |
| `:core:data`   | How a database is opened, and nothing else: no table, no query                  |
| `:app`         | Compose UI, one package per screen with its own layers, plus the shared theme   |

`:core:domain` compiles against the Kotlin standard library alone. The compiler cannot see
Android from it, which is the only layering boundary the build actually enforces — inside
`:app`, the layers are packages held by convention. A feature that stores something brings its
own tables and queries and asks `:core:data` for the connection.

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
500 seeded random boards from 4×4 to 12×12. Agreement is then evidence, rather than a
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

## Layout

```
core/domain/          the game, in pure Kotlin
core/data/            how a database is opened, and nothing else
app/
  MainActivity.kt
  NQueensApplication.kt
  NQueensNavHost.kt
  puzzle/             which puzzle this is — its name, its piece, its rules —
                      and the largest board the app will play
  format/             what more than one screen prints: the elapsed time
  storage/            the one database the features' tables live in
  theme/              colours, type and dimensions from design/tokens.json
  setup/
    domain/           the size the stepper starts on
    presentation/     ui state, actions, view model, screen, board thumbnail
  game/
    presentation/     ui state, actions, view model, screen, top bar, board,
                      how a square is painted, the win card and the celebration
                      over a solved board, how a piece lands and flinches, the
                      sounds it makes, and the route that carries the size
  history/
    domain/           a solved board, the repository that keeps them, and the
                      clock a record is stamped with
    data/             the table, its queries, the repository over them and
                      the bindings
    presentation/     the records screen: view model, state, screen and the
                      moment a record is drawn with
app/schemas/          the database's schema history, checked in
design/               design reference: tokens, screen SVGs, the queen icon,
                      and the script that generates the four sounds
docs/                 what this is, why it is shaped this way, and the plan
```

## Use of code generation tools

Claude was used throughout: drafting code, reviewing it, and checking the documentation against
what the code does. I made the design decisions and reviewed every change before taking it.
