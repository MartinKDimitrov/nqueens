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
  `docs/`, and `docs/design/` inside it, live in the same repo.

## Initial commit (developer, before Phase 0)

The repo is initialised empty on `main`. The developer makes the **first commit** with the
already-present `docs/` (PROJECT, TRADEOFFS, PLAN, DESIGN and the Lovable reference under
`design/`: tokens, screen SVGs, queen icon) and `.gitignore` — no code yet.

Suggested message: `docs: project, tradeoffs, plan and design reference`.

## The `check` command

One command runs everything a commit must pass. The pre-commit hook runs it, and so does CI
(`.github/workflows/check.yml`) — the same one command rather than a list of its own, so a check
added in one place cannot go missing in the other. The hook is local and can be skipped; CI is
what a branch has to pass.

`make check` → `./gradlew check buildHealth`, in fail-not-rewrite mode. Every row below fails
on a real defect:

| Concern              | Tool                                                             |
|----------------------|-----------------------------------------------------------------|
| format               | Spotless (ktlint) — `spotlessCheck`                             |
| static analysis      | detekt (built upon its default ruleset)                         |
| type strictness      | Kotlin `allWarningsAsErrors`; `explicitApi()` on every `:core:` module |
| tests                | JUnit4 + `kotlin.test`; property tests use a **seeded** `Random`|
| coverage floor       | `jacocoTestCoverageVerification` for the domain, and `viewModelCoverageInputs` + `viewModelCoverageVerification` in each module that holds a view model |
| dependency hygiene   | `dependency-analysis` `buildHealth`, applied to **every module**, failing on any advice |
| Android resources    | `lint` with `warningsAsErrors`, over the test sources as well; version advisories excluded |

**Vulnerability audit (not built).** OWASP dependency-check needs an NVD API key and a large
local database, which makes it too slow and flaky for a per-commit gate. It is not wired at all
rather than wired and skipped. A conscious deviation from the "everything in check" rule.

Coverage floors: **`:core:boardlogic` 90% line and 90% branch**, both counters named explicitly
because JaCoCo otherwise measures instructions; **`*ViewModel*` classes 85% line**, in each
module that holds one — `:app` at the time, and since step 9.7 the three feature modules. No
screen is measured at all: code run under Robolectric is invisible to JaCoCo, so the screens
appear in no coverage report and are checked by running them.

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
- Gradle wrapper pinned to 8.11.1; `settings.gradle.kts` with `:core:boardlogic`, `:app`.
- `gradle/libs.versions.toml` starts with AGP 8.7.3, Kotlin 2.0.21, androidx-activity, JUnit,
  detekt, Spotless and dependency-analysis; everything else joins it in the step that first
  needs it.
- Root build wiring Spotless and detekt across the modules, and dependency-analysis into each
  of them — applied to the root alone it reports on nothing. `:core:boardlogic` gets JaCoCo floors;
  `make check` → `./gradlew check buildHealth`.
- `:app` starts as a minimal Android shell and `:core:boardlogic` as an empty pure-Kotlin library.
  Both compile under `allWarningsAsErrors`.
- Commit gate hook installed (`core.hooksPath = .githooks`).
- **Check:** `make check` is green on the skeleton (verified).

---

## Phase 1 — Domain: rules and validation

**Step 1.1 — `Cell` and the pairwise oracle.** *(built)*
- `:core:boardlogic`: `Cell` (with both diagonal identifiers), `fun interface PairwiseRules` (named `PuzzleRules` at the time),
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
  `NewGame`); pure `reduce(state, action)`. `NewGame` went again in 9.27: the screen starts a new
  board by going back to Setup, so nothing outside its own tests ever sent it.
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
  Material theme carrying `PuzzleTypography`, with the
  board's own colours beside it in `BoardColors` through a `CompositionLocal` (light + dark).
  Both are transcribed from `docs/design/tokens.json` by hand; nothing reads it at build time.
- **Check:** the app builds and opens on the Setup screen, checked by hand on an emulator.

---

## Phase 5 — Setup screen (MVVM) — *first in user flow*

**Step 5.1 — Setup view model and screen.** *(built)*
- A stepper between the smallest and largest board the puzzle offers (step 9.4 moved both onto
  `Puzzle.sizes`), a board drawn at the chosen size, a
  variant row with the one puzzle that exists, and a "Start" that carries the size to the game
  route.
- Tests: the view model clamps at both ends and starts at the default; coverage-gated at 85%.
  The screen is rendered under Robolectric inside `check`: the stepper's ends, and that Start
  carries the chosen size.

---

## Phase 6 — Game screen (MVI)

**Step 6.1 — Board rendering + tap → toggle.** *(built)*
- Hilt arrives here: `PlayViewModel` is the first class with a dependency to inject, and
  `PuzzleModule` provides the `Variant` that carries the rules. *(Both were renamed in 9.4 and
  the binding became a set in 9.9; this bullet records the step as it was taken.)*
- Dynamic `n×n` Compose board; `PlayViewModel` holds `GameState` in a `mutableStateOf`
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
- *(Superseded by step 9.13 and D18: the tables live here now.)* `:core:database` owns how a
  database is opened and nothing else: no table, no query, no name of its
  own. The app has one database, and a feature adds its table, its queries and the repository over
  them; the database itself sits above the features because Room needs all its entities declared
  together (TRADEOFFS D4).
- The feature — `scores/` since step 9.21 — keeps one row per solved board: the size, the variant, the finishing time
  and when it was finished. Only solved boards are recorded — an abandoned game is not a result.
  Its `domain` states what the feature needs — a record, and a `SolveRepository` that adds one,
  deletes one, clears everything and reports the best time for a size; its `data` implements that
  over Room, and the view models are given the interface, never the database. A second source —
  a server, a backup — is a collaborator of the repository, not a change to the screens. Nothing
  is on screen yet. The variant is kept under the puzzle's own key (TRADEOFFS D14).
- Tests: the repository against an **in-memory database**, so the queries run rather than a mock of
  them — a record survives the round trip, a delete removes its own row and no other, clearing
  empties the table, and the best time is the smallest for that size and that variant; and `:core:database`
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
- The screen follows the design: `docs/design/screens/scores.svg` groups by board size, and so does this.
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
- A solved board bursts into eighteen pieces: the six `docs/design/screens/win.svg` places and a dozen
  more, because six read as a diagram rather than a celebration once they move. All but two come
  to rest clear of the card, so they are seen and not merely drawn. They are drawn between the
  scrim and the card so the layers stay in the design's order: out from the middle, growing and
  turning,
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
- Placement and the win are felt as well as seen; the sound arrives in 8.2.
- Contrast is measured and three pairings are left below AA on purpose, recorded in `PROJECT.md`.

**Step 8.2 — Sound.** *(built)*
- Four synthesised sounds, owing nothing to a third party: a wooden clack when a piece is set
  down, a lighter one when it is lifted, a dull beat when a move puts a piece under attack, and a
  chord when the board is solved.
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
- A flinch cut short by the trouble ending puts the queen back where she stands: a cancelled
  animation keeps the value it had reached, and without a hand back to rest she stays crooked.
- The win announces itself once. The composition is rebuilt by a rotation and by the trip to the
  records and back; the win happened only the first time.

---

---

## Phase 9 — One module per puzzle

The goal is that a second puzzle is a module somebody copies, and that two people working on
different modules never have to agree about anything but a contract. Everything a second puzzle
would not rewrite moved down into `:core:*`; what is left above is the puzzle itself.

**Step 9.1 — `:core:ui`.** *(built)*
- The palette, the type scale, the measures and `formatElapsed`. Nothing else can leave `:app`
  while the theme is in it: 35 imports reach for it.
- Everything crossing the boundary says `public` and states its type, as both `:core:` modules
  already did.

**Step 9.2 — `:core:solves`.** *(built)*
- One interface: `RecordSolve.record(SolvedBoard): Int?` — write the board down, answer with the
  best time for that puzzle and size before it. No table, no Android, no screen.
- The read-before-write ordering moved out of `PlayViewModel` and into the implementation, where
  it is one place rather than a rule a caller must remember, and gained its own tests.
- The game feature stopped importing anything from the history feature.

**Step 9.3 — `:features:scores`.** *(built)*
- The table, its queries, the screen and the implementation of `RecordSolve`.
- The app declares the database and hands the feature its DAO, rather than the feature reaching
  up for the database's type — which also ends the `storage` ↔ `history.data` cycle.
- The feature declares `scoresDestination`, as the game already declared its own.
- `resourcePrefix = "scores_"`, so a name that could collide with another module's fails the
  build rather than winning a silent merge.

**Step 9.4 — `:core:puzzletype`.** *(built)*
- `Variant` became `Puzzle` and moved out: key, name, glyph, **sizes**, rules, words.
- The largest playable board stopped being a global and became the puzzle's own answer; the
  stepper and the route guard both ask it.

**Step 9.5 — `:features:play`.** *(built)*
- Setup and the game: their view models, the board, the top bar, the win card, the celebration,
  the motion, the sounds and the routes. It knows what a puzzle is and never which one.
- Its two previews went with the split: a preview needs a concrete puzzle, and the shell has
  none. The screens are still rendered by tests.

**Step 9.6 — `:games:nqueens`.** *(built)*
- Seven files at the time; nine since step 9.22 gave the module two more tests. The puzzle, its
  rules, one binding into the set, its words, its glyph, its
  build file and its test.
- The Hilt Gradle plugin is not applied here — the module contributes one binding and needs the
  annotations and the compiler, not the plugin that aggregates components.

**Step 9.7 — the app shrinks.** *(built)*
- What is left is the database, the navigation and the list of games that are installed.
- The view-model coverage floor moved with the view models. `:app` no longer applies it, and the
  gate on that gate is what said so: with no view model left to measure, it refused rather than
  passing.

**Step 9.8 — a second puzzle.** *(not built, deliberately)*
- The proof of the shape is `cp -r games/nqueens games/<second>`, changing the rules, the words and
  the glyph, and adding two lines — the `include` and `:app`'s dependency on it. It is not built because the assignment
  asks for one game.
- What made that true rather than plausible is 9.9: an audit tried the copy, and the build refused
  it.

**Step 9.9 — a set rather than a binding.** *(built, after an audit refused the copy)*
- A game contributes `@Provides @IntoSet`, and `Puzzles` reads the set. Two games no longer clash
  as a duplicate binding.
- The route carries a puzzle — `game/{puzzle}/{size}` — so nothing outside a game module names
  one. The app injects `Puzzles` and hands it to the navigation host.
- Setup offers what is installed and opens inside the chosen puzzle's own range.
- `Puzzles` refuses at assembly what a game module can get wrong: a key a route could not carry,
  a transposed range, a size the domain will not build, two puzzles under one key, or none at all.
  Each of those failed quietly on a screen before, and each now names the puzzle that caused it.
- The queens' rules moved out of `:core:boardlogic` into `:games:nqueens`, where three documents
  already said they were. The domain's tests carry their own copy, because the domain cannot
  depend on a game, and the pairwise oracle went to the test sources with them.
- A second puzzle lives in the shell's test sources — one puzzle's rules over another's words
  — so the shell is exercised with more than one installed rather than only asserted to allow it.

**Step 9.10 — what six audits sent back.** *(built)*
- **A library has no application theme**, so a screen test's activity took the platform default —
  which has an action bar and costs 48 dp of height the app never loses. Two tests had been
  weakened to fit that smaller window; the modules now declare the app's bare theme for their own
  debug variant, and both assertions are back as they were written. The app says
  `tools:replace="android:theme"`, so what ships is still its own.
- The refusals in `Puzzles` printed their own source: an escaping mistake left `${puzzle.key}` as
  text. Nothing caught it, because the tests passed a label to `assertFailsWith` rather than
  reading the message. They read it now.
- **The puzzle's key was compared to itself** on the write path, in two modules: the tests fed
  `"queens"` and expected `"queens"`, so hard-coding the key passed. They use a key that is not
  the queens' one.
- Nothing asserted that the table had answered, so an empty table could have left a player at a
  blank list for ever, green.
- The board's 24 dp floor was drawn only on wide windows; a twelve-board on a 320 dp phone, which
  is where the floor is first reached, is now drawn.
- The overshoot was pinned as "there is one" rather than how high; the puzzle row was checked for
  size but never for whether it should be a choice at all; and nothing opened the app's own
  database, since each feature now tests its table against one of its own.

**Step 9.11 — the goal, and a name that is not one puzzle's.** *(built)*
- `Puzzle` gained `piecesToSolve(size)`. `snapshotOf` takes that number instead of assuming the
  board's own, so a puzzle solved by something other than `n` pieces
  can say so. It was the last thing a copied module could not state for itself.
- `Puzzles` refuses a goal outside `1..size²`: a game that cannot be finished is caught where the
  games are assembled rather than by a player counting down for ever.
- `MIN_BOARD_SIZE` became one square. "Below four there is no solution" is a fact about queens,
  not about boards, and now lives in `Queens.sizes` with the reason beside it.
- Everything outside `:games:nqueens` stopped being named after queens: the package is
  `com.mdimitrov.puzzles`, the application, the navigation host and the theme are `Puzzles*` and
  `Puzzle*`, and the family's own tests name no puzzle either — the pairwise oracle is
  `FourLinesByPairs`. What still says N-Queens is the app's label and this repository's name,
  because the build that ships plays exactly one puzzle.

**Step 9.12 — core modules named for what they do.** *(built)*
- `:core:domain` and `:core:data` were named after layers. A layer is a thing a *feature* has;
  `core` holds shared pieces, and a shared piece is worth finding by what it does. They are
  `:core:boardlogic` — the puzzle family — and `:core:database` — how a database is opened. If a REST
  connection is ever shared, it gets its own module beside them rather than a second meaning for
  `data`.
- The packages moved with the modules: `com.mdimitrov.puzzles.boardlogic` and
  `com.mdimitrov.puzzles.database`.
- `rootProject.name` is `Puzzles`, which is what the build is. The app's label is still N-Queens,
  because that is still the only puzzle it ships; it changes when a second one is assembled in.

**Step 9.13 — every table in one module, and a hollow app.** *(built)*
- `:app` was importing `history.data.SolveRow` and `SolveDao` to assemble `PuzzleDatabase`, which
  forced two classes public in a package where everything else is `internal`. The tables moved to
  `:core:database` instead, together with the database, how it is opened, and the schema — the
  shape *Now in Android* uses. A feature is injected the accessor for its own table and nothing
  else.
- `:app` is now `MainActivity`, `PuzzlesApplication` and `PuzzleNavHost`, and holds no storage.
  Its only edge into a feature is that feature's own navigation destination.
- With one module holding both, the `Databases` interface had one implementation and one caller,
  so it went: `connect` is a function beside the module that uses it. The test that matters
  survives — a version this build cannot migrate throws rather than empties the table, which is
  what fails if anyone reaches for `fallbackToDestructiveMigration`.
- The modules were weighed against collapsing everything into `:app` as packages and kept; the
  reasons are measured in D17 rather than asserted, and the trade this step makes is D18.

**Step 9.14 — the shell is a feature, and the build files stopped repeating themselves.** *(built)*
- `:core:play` became `:features:play`. It held two screens and their view models; `core/`
  in this build holds what has no screens. The namespace and the Kotlin packages did not move, so
  no source changed.
- The twenty-odd lines every Android module repeated — `compileSdk`, `minSdk`, `compileOptions`,
  the `lint` block, the Debug-only test filter and the Kotlin compiler options — are set once in
  the root build, per plugin id. What differs between modules stayed in them.
- Seven build files shrank; `:features:play` and `:features:scores` lost 29 lines each. The gate
  and the test count are unchanged: 222.

**Step 9.15 — setup and the game became two modules.** *(built)*
- They shared a module and nothing else: not one import between the two packages, no shared
  string, and no call in either direction — `:app` joins them through the route. They are now
  `:features:setup` and `:features:play`, beside `:features:scores`.
- `:features:setup` no longer declares `:core:solves`. The screen that chooses a board cannot
  write a solved one, and now that is the compiler's answer rather than a convention.
- The tests split 21 / 87 and run in parallel; the total is unchanged at 222. `play_` became
  `setup_` and `game_` across 22 strings, 2 drawables and 4 sounds.
- The touch-target walk is one class per module rather than one covering both screens. It states
  what it skips: the board's own squares floor at 24 dp by `docs/PROJECT.md` §2, and
  `PlayScreenTest` is what holds that floor.

**Step 9.16 — the design reference moved under `docs/`.** *(built)*
- Nothing builds from it, so it was the one directory at the root that was not a module and read
  as though it were. It is `docs/design/` — `tokens.json`, the six screen SVGs and the queen icon
  — with `docs/DESIGN.md` beside it, and the nine comments in the code that cite a token or an
  artboard measurement now name the path that exists.

**Step 9.17 — two modules named for what they are for.** *(built)*
- `:core:records` read as the records screen, which is `:features:scores`; it holds neither a
  table nor a screen. It is `:core:solves` — the word its table, its rows and its repository
  already use — and its package is `com.mdimitrov.puzzles.solves`.
- `:core:puzzle` did not say whether it was *a* puzzle or *what a puzzle is*, with `:games:nqueens`
  beside it being one and the application called Puzzles. It is `:core:puzzletype`: what a game
  module must provide, and which types this build was assembled with.
- `SecondPuzzle.kt` moved out of a `puzzle` test package into the package of the two tests that
  use it, which removed both imports.

**Step 9.18 — `:core:board` became `:core:boardlogic`.** *(built)*
- The name said "a board" while the module holds what can be done to one and what follows:
  `GameState` with `GameAction` and `reduce`, `Conflicts` for who threatens whom, `Line` as the
  vocabulary every puzzle writes its rules in, and `BoardSnapshot` as what a screen is handed.
- Package `com.mdimitrov.puzzles.boardlogic`. Nothing else changed: it is still the one module
  with no Android on its compile classpath.

**Step 9.19 — `feature/` became `features/`.** *(built)*
- It holds three of them. The directory is a grouping rather than a module — there is no build
  file at `features/` — so the change is the directory, the `include` lines and the references.

**Step 9.20 — `game/` became `games/`.** *(built)*
- For the same reason as `features/`, and one more: `game/` and `features/game` were two different
  things sharing a word. The group holding every puzzle module is now `games/`, and `game` on its
  own means the screen a puzzle is played on.

**Step 9.21 — the two screens named for what they are.** *(built)*
- `:features:game` shared a word with the `games/` group, and inside it the distinction between
  `:core:boardlogic`'s `GameState`/`GameAction` and the screen's own `GameUiState`/`GameActions`
  rested on a plural. It is `:features:play`: `PlayScreen`, `PlayViewModel`, `PlayUiState`,
  `PlayActions`, `PlayRoute` with `PLAY_ROUTE` and `playRoute`, `PlaySound` and `PlaySounds`, and
  the resource prefix `play_`. `GameState` and `GameAction` did not move — they belong to the
  board and are named for it.
- `:features:history` was the only thing in that module called history: its screen, its route, its
  view model and its resource prefix all said scores. It is `:features:scores`, and the one file
  that carried the old word is `ScoresModule.kt`.
- No class was renamed for the second change and no behaviour changed for either. 222 tests.

**Step 9.22 — what six audits found.** *(built)*
- **The queens' rules were untested.** The property test with 500 boards checks `conflicts`
  against a pairwise oracle, but over the *domain's own* copy of the rules; deleting a line from
  `NQueensLines` — the rules the player plays by — left all 222 tests green. `:games:nqueens` now
  cross-checks its own rules the same way, and each of the four lines fails when removed.
- **A board now survives the process being taken.** `announced` and `burst` were saved while the
  board they describe was not, so a player returning to an app the system had reclaimed solved
  the board again in silence: no sound, no haptic, no confetti. The board itself is saved now —
  its pieces, its clock, and whether the win was written down — and the flags are right again
  because what they describe comes back with them.
- **A table that stops answering after it has answered once** left the rows it had already given
  on screen for ever and never said why, while still offering to clear a table nothing could be
  read from. The screen says so over the rows now, and the button is gone while it cannot.
- **The gate could not fail on module structure.** `onAny` covers what a module declares, not
  what it is, so `buildHealth` returned zero while `:core:puzzletype` and `:core:ui` failed their
  own `projectHealth`. `onModuleStructure` is a failure now. `:core:puzzletype` was the honest
  half of that advice and is a JVM module: nothing in it is Android, and `androidx.annotation` is
  a plain jar. `:core:ui` carries Compose and is ignored by name, with the reason beside it.
- **Four gaps that were claimed rather than held:** the shell reading `Puzzle.piecesToSolve` (a
  revert to the board's own size passed), the 48 dp in `TouchTarget` (every assertion compared
  against the constant, so the number itself was free to move), the two swallowed write failures
  on the scores screen, and whether a game module's words take the arguments the screens pass
  them — which no compiler can see, since a resource id crosses a module boundary as an `Int`.
- **`Bishops` claimed coverage it did not have.** The fixture's own KDoc said it exercised the
  counter and the win condition against a goal the shell could not guess; it was in Setup's tests,
  where neither lives, and nothing referenced it. It is in the shell's tests now, and it is what
  holds the `piecesToSolve` wiring.
- Around forty documented claims were corrected — counts, module names, paths and two overclaims
  in D18 that no arrangement of code could have made true.

**Step 9.23 — what the second round of audits found, including in the first round's fixes.** *(built)*
- **The fix for the lost win had made it worse.** `recorded` was written to the handle in the same
  step that started the write, so a process taken mid-write left a board that came back solved,
  said so, and never wrote its row — where before it came back empty and the player wrote it by
  playing again. It is claimed now only once the row is on the table, an unsaved `writing` flag
  stops a second attempt inside one process, and a board restored solved but unwritten hands the
  row over on the next launch. What remains is the mirror window — the row landing and the process
  going before one assignment — which writes the same board twice; narrowing that further means an
  idempotent write, which is a schema decision rather than this one.
- **The gate still could not fail on module structure.** `onAny` already covered it, and
  `buildHealth` drops `moduleAdvice` when it aggregates. `check` depends on each module's own
  `projectHealth` now, which was verified by making a module Android that should not be and
  watching the gate go red.
- **`:core:puzzletype` had lost Android lint** by becoming a JVM module — the one module that
  declares `@StringRes`, `@DrawableRes` and `@PluralsRes`. It applies `com.android.lint`, which is
  that check without the rest of AGP.
- **A row on a table that has stopped answering is no longer offered for deletion.** Hiding
  "Clear all" had closed one door of two: a delete would have landed and the row would have stayed
  on screen, because the list is the last one the screen was given.
- **A board saved at one size is discarded whole rather than read onto another.** An index names a
  square of the board it was written for; read against a different size it does not recover a
  piece, it invents one. `generation` stopped being saved at all: it is only compared against a
  write the same instance launched.
- **Four tests were weaker than their names**, and each is now held by the mutation it missed: the
  plural test compared two formatted words rather than two wordings; two restore tests never
  touched the board after it came back; and the two swallowed-write tests could not tell a
  swallowed failure from one that escaped the coroutine and would have ended the process.
- `GameStateTest` states the largest board as a number rather than in terms of the constant it is
  testing, and refuses a piece off either edge rather than only off the side.

**Step 9.24 — no puzzle the build does not ship.** *(built)*
- Two fixtures invented games — a rooks puzzle for Setup's picker and a bishops one for the
  counter's goal — and four tests played them. A test against a game nobody wrote proves the test.
  They are gone, and so is every mention of a piece this build does not have: in the tests an
  arbitrary second key is now `"another-puzzle"`, and the prose that used rooks and bishops to
  explain why the goal sits on the puzzle says so without naming one.
- **What that costs, stated rather than hidden.** Reverting `snapshotOf(…, puzzle.piecesToSolve(size))`
  to the board's own size passes the suite again: with one puzzle installed the two are the same
  number, so no test of this build can tell them apart. It becomes testable the day a second game
  module exists, and that module's own tests are what should close it. The other two named here at
  the time — the picker with more than one row, and `SetupViewModel.choose` moving a size into
  another puzzle's range — were closed two steps later, in 9.26, by the queens under a second key
  and a narrower range.
- `choose` is covered by the only choice this build can offer — the row already selected — so the
  view-model floor still measures something rather than being lowered to fit.

**Step 9.25 — a theme the player chooses, a CI that runs the gate, and tests only a device can answer.** *(built)*
- **`:core:settings`** keeps what the player has chosen between runs: `ThemeChoice` — light or
  dark — behind a two-verb `Themes`, over DataStore preferences. There is no third value for
  "follow the phone": a player who has not chosen has stored nothing, and null is that, resolved
  against the phone by the one place that can ask it — a screen. A name a later version wrote and
  this one does not know reads the same way, as no answer rather than as a failure.
  `MainActivity` dresses every screen with it; Setup carries the button, which names the palette
  in force rather than what pressing it does.
- **CI runs `make check`** and nothing else (`.github/workflows/check.yml`), so a check added to
  the hook and not to CI cannot exist. The hook is local and skippable; CI is what a branch passes.
- **The status bar was unreadable**, which a screenshot from a real phone showed and no test on a
  workstation could: the clock and the battery are drawn by the system in one of two colours, the
  app never said which, and the default is the light one — on a light theme. `MainActivity` now
  asks for edge to edge out loud and sets the bar's appearance from the palette in force.
- **The theme button was below the fold.** Setup scrolls, and the last thing on screen was "Best
  times"; a setting a player has to go looking for is a setting they will not find. It sits beside
  the title now, short on the button and the whole sentence to a screen reader.
- **`app/src/androidTest/`** holds two classes and will not grow without a reason: whether the
  platform's own decoder takes the four sounds — Robolectric's `SoundPool` accepts anything — and
  what the system draws around the app at API 35, where the bars are drawn edge to edge whether an
  app asks or not and nothing on a workstation renders them. They are not in `check`, because they
  need hardware; `make check-device` runs them.
- **`features/play/presentation` is four groups**: the screen with its route, state, actions, view
  model and top bar; `board/`, `win/` and `sound/` beneath it.

**Step 9.26 — what the third round of audits found.** *(built)*
- **A corrupt preferences file killed the app on the first press of the theme button**, and again
  on every launch after, because nothing repaired it. The write is swallowed now, like every other
  write in the app, and the store replaces a file it cannot parse.
- **Only the status bar had been corrected.** `enableEdgeToEdge` paints both bars for the phone's
  own mode; the palette in force is the player's, so the navigation bar was left with dark icons
  on a dark strip whenever the two disagreed — the same defect as the one a screenshot caught,
  one bar lower.
- **The palette is read before the first frame** as well as collected into it — the blocking read
  decides the frame, the collection follows the button — so no creation of the activity draws in
  the wrong one. The claim that this was only a first-launch cost was wrong: it happened on every
  rotation.
- **A delete or a clearing followed by leaving the screen was dropped.** Both go through
  `:core:scope` now, which is where the solved board already went. That scope moved out of
  `:features:play`'s `sound/` package — where it had nothing to do with sound — into a module of
  its own, because three features need the same one and a feature cannot reach another feature.
- **A board discarded for its size kept the previous game's clock, record and "already written
  down" flag**, so the next win on the new board was never written. The size is asked once now and
  answers for all four values.
- **`SetupViewModel.choose` dropped the palette** by building its state positionally, and no test
  could see it: the method had no live branch in the suite at all. It uses `copy` now, and is
  exercised with the queens under a second key and a narrower range — a configuration standing in
  for a second module, which is what the method is for.
- **The device test asserted the state before the fix.** It compared the bars against the *system's*
  night mode, which is what `enableEdgeToEdge` alone produces; it sets a palette through the real
  `Themes` and asserts against that now, and its second case asserts the app draws behind the bars
  rather than that the window reported any insets at all.
- Three things the gate itself found once it was asked: the redundant `androidTest` dependency and
  the exception that was hiding it, `:core:settings` publishing the preference store it should
  keep, and `hilt.core` as `compileOnly`.

**Step 9.27 — what the fourth round of audits found.** *(built)*
- **`make check-device` was red, and one of the two things it exists to check never ran.** The
  rewritten edge-to-edge test built a graph of its own for every test, and the second store opened
  on one preference file is one DataStore refuses. It reaches the app's own graph through the
  activity it injected now, and needs no test application at all. The gate also swept every module,
  and a library module with no test to run does not report nothing — its empty instrumentation APK
  crashes. It names `:app`, where both device tests live.
- **A readable preference file could kill the app at launch.** A value stored under the theme key
  as something other than a name — a later version's, or damage that still parses — throws when a
  string key is asked for it, and the guard sat above the read where nothing thrown by it could be
  seen. The answer is read out of the map now, so a value this build cannot use is no answer rather
  than a crash, and the flow that carries it survives: the guard below the read ends it, which
  would leave the palette right and the button dead.
- **Below API 29 the navigation bar was still the phone's.** `enableEdgeToEdge` was told the icon
  colour by hand and left to answer the scrim itself. It is handed the player's choice now and
  answers both, and the window behind the app is painted to match — which is what `values-night`
  could not do, because a resource cannot know what the player chose.
- **Two presses of the palette button could land in the wrong order.** The scope that outlives a
  screen ran on the whole thread pool. It takes one thread at a time now, so writes land in the
  order they were made; nothing on it waits on the CPU.
- **Four fixes from the third round were held by nothing.** The scope every write moved to was
  indistinguishable, in the fixtures, from the one it moved off; the recorder's rethrow of a
  cancelled read was called an equivalent mutant and is not; the guard against writing one finished
  board twice had no test; and neither had the preference file that cannot be read. Each is now
  killed by mutating exactly the line it defends.
- **A claim repeated without checking it.** Two pieces of screen state were reported as surviving
  process death by luck; both were already `rememberSaveable`. Reading them took one command.
- **`writing` is left standing after a refused write**, and stays that way: a solved board takes no
  taps, the reset beside it is disabled, and the card's own button goes through `Reset`, which
  clears it. There is no path that reaches the flag, so there is nothing to fix and nothing a test
  could hold.
- **The formatter could write code the gate rejects.** ktlint joined lines up to its own limit of
  140 and detekt refused them at 120. `.editorconfig` names 120 now, so `spotlessApply` cannot
  produce a file `make check` fails on.
- **`GameAction.NewGame` had no caller in the app**, only six tests of its own — production code
  kept alive by the suite that measured it. It is gone, with its branch in the reducer and its
  tests; a new board is started by going back to Setup, which is what the screen has always done.
- **`win/` read the screen's state from the package that draws it**, the one edge pointing back up
  through `features/play/presentation`. The card takes a `Win` of its own now, built at the call
  site, and the three packages under `presentation` depend only downwards.
- Documentation: the module table, the layout, the test counts, the lint claim, and this file's own
  account of what the blocking read replaced.

**Step 9.28 — what the fifth round of audits found.** *(built)*
- **"New game" on the records screen threw away the board the player had sized.** It navigated to
  Setup over the top of Setup, which is a second view model that has never heard of the choice; the
  system's own back gesture, one gesture away, came back to the right board. It pops back to Setup
  now, so both ways off that screen leave by the same door.
- **The win was never spoken.** The card carries a polite live region, and a live region is
  announced when a node the reader already knows about changes — this card and its region are born
  in the same frame as the win, so nothing was ever sent. A blind player placed the last piece and
  heard silence while the board went quiet and every control around it disabled itself. The card
  asks the window to say the result now, and a test with a reader switched on holds it.
- **One refused read left the palette button dead for the rest of the process.** `catch` emits and
  then ends the flow, taking both collectors with it: the button wrote the player's choice and
  nothing was left listening to redraw the app in it. A file that will not open is asked again a
  few times first, which is the shape the records screen already used for the same reason.
- **The scope's ordering claim was too strong, and is now the claim it can keep.** One thread at a
  time begins work of the same shape in order — which is what two presses of the palette button
  are — but it is not a queue, and a coroutine that suspends twice can finish after one handed over
  later that suspends once. The case that leaves open is written down in PROJECT §6 rather than
  implied away, and a second test asks the structural question, so the guard does not quietly stop
  guarding on a machine with one core.
- **Two fixes from 9.27 were held only in their bodies, not at their call sites.** The blocking
  read before the first frame and the `SideEffect` that follows the palette could each be deleted
  with both gates green, and deleting the second reproduced the defect a screenshot had caught. One
  test now stops the activity after `onCreate`, before anything is composed; another changes the
  palette on a running activity without creating it again.
- **The corruption handler had no test, and the fixture was why.** The suite built its own stores,
  which is right for what they asked — but a store built in a test is not the one production hands
  out, and the difference between them was exactly the handler. A test now writes damage into the
  app's own preference file and opens it through `PreferencesModule`.
- **The ordering test for the puzzle catalogue could not fail.** `setOf` was handed the two keys
  already sorted, so the assertion passed whether the list was sorted or handed straight back. They
  are handed over the wrong way round now.
- **One concept had three names across three modules**, and a comment explaining a name is a name
  that should have been changed: a puzzle was `puzzle` in `:core:solves`, `variant` in the table and
  the repository, and `key` on `Puzzle` itself. The Kotlin says `puzzle` throughout now; only the
  column still says `variant`, under a `@ColumnInfo` and for the reason D14 gives.
- **Six more names that said the wrong thing.** `Nothing(state)` — in a language where `Nothing`
  means "does not return" — is `NoRecords`; the empty slot's key named the rarer of the two cases it
  covers; `FakeSolves.added` returned what the table holds *after* a delete, and is `rows`;
  `PLAY_ROUTE` still held `"game"`; `A_SQUARE` held a prefix; and `swallowing` was named for the
  thing it does not do to the one exception that matters, so it is `swallowingRefusals`.
- **The shell stopped naming queens.** `queenTint` and `QUEEN_SCALE` are `pieceTint` and
  `PIECE_SCALE`, and `SquareMotion`'s prose animates a piece. What is left is the palette's three
  colour names, which PROJECT §7 already called the shell's own word for whatever it draws.
- Documentation: two numbers this project's own previous step made stale — the domain's branch
  coverage and one module's test count — plus the CI trigger, what `sound/` holds, what `:core:solves`
  holds, what a second game costs in build files, and four source comments that had drifted from the
  code beside them, two of them written in 9.27.

**Step 9.29 — what the sixth round of audits found.** *(built)*
- **The previous round's own two fixes, put together, froze the launch.** `StoredThemes` gained
  three retries a second apart; `MainActivity` still read the first frame's palette off that flow
  with `runBlocking`. On a preference file that will not open, `onCreate` blocked the drawing thread
  for **three seconds** — measured, and on every creation of the activity, so every rotation too.
  Even the case the retry exists for cost a frozen second where it used to cost a frame. The
  interface now asks the two questions separately: `chosen()` answers once and gives up, `choice`
  keeps asking for the collectors that already have a screen up. A test counts the reads rather
  than the milliseconds, because `runTest` skips a delay and can never see the seconds.
- **The win said the result and never the record.** What the board beat comes back from the table
  after the card is drawn, so it cannot be part of the sentence the card announces on arrival. A
  sighted player watches the "New best" line appear; nobody else was told it had. It is announced
  on its own now, when it arrives.
- **A failed read undid a good one.** `catch` answered with null, which means "the player has not
  chosen" — so one refused read after a successful one handed the app back to the phone's palette
  and ended the flow, with nothing to put it right until the next launch. The last answer stands
  now.
- **The clock kept the old game's part-second.** `Reset` zeroed the count and not the tick, so a
  board begun nine tenths of the way through a second counted its first second in a tenth of one
  and every time it recorded was short by the remainder.
- **The phone's own answer was untested.** Both the fallback to the system's night mode and the
  reading of that mode could be inverted with all tests green — a first launch on a dark phone would
  have opened white.
- **`make check` could go red for a reason unrelated to the commit.** Spotless walked every module's
  `build/` tree to find six markdown files and died on whatever a parallel task removed underneath
  it. It is pointed at the six.
- **A test of mine leaked into two others**, and the reason I gave for it the round before was
  wrong. Switching a screen reader on leaves Compose's accessibility machinery running, and turning
  the clock off by hand leaves it off — either one leaves two of the board's animation tests waiting
  sixty seconds for a composition nothing advances. The card now announces through a seam the way it
  already plays sounds through one, no reader is switched on at all, and the clock is given back.
- **The exported Room schema was stale.** The `variant`→`puzzle` rename changed a `fieldPath` the
  build never rewrites while the file exists. Regenerated; the identity hash is unchanged, as D14
  says.
- Six names and two counts in the documents, `SquareMotion`'s pronouns — which the previous step
  declared renamed and were not — the §5.2 paragraph still describing the database shape D18
  replaced, and this file's own arithmetic in 9.28.

**Step 9.30 — a way off the records screen that is on it.** *(built)*
- The list is reached from two places and had one control leaving it, "New game", which always goes
  to Setup. A player who opened it from a board they had not finished could only get back to that
  board by the system's gesture, which nothing on the screen mentions. The header now carries a back
  button that does what the gesture does.
- Two ways out means `scoresDestination` takes two lambdas, and the screen's four callbacks became a
  `ScoresActions` — the shape `PlayActions` and `SetupActions` already had, and what detekt's
  parameter limit asks for.
- **Twice the layout said no, in two directions.** Beside the title, the button takes the width the
  subtitle needs and at the largest font the first record is pushed off the bottom. On a line of its
  own it takes 48dp of height, and in the shortest window this screen is drawn in there is none to
  give: `LazyColumn` is left with nothing to measure and the composition never settles — the test
  spun at full CPU until it was killed rather than failing. It sits beside the title, and the
  subtitle is bounded to two lines, which is what made room for it.
- The icon is `:features:play`'s, copied rather than shared: a feature cannot reach another feature,
  and one arrow is a smaller price than a module to hold it.
