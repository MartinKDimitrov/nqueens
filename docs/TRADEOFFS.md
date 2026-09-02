# N-Queens Puzzle — Trade-offs and Decisions

Each decision is recorded as: context, options, decision, why, and when to revisit.
This is the reasoning behind the shape of the code.

---

## D1 — Conflict validation: counting line occupancy

**Context.** On every tap the board must be re-validated and the *set* of conflicting queens
returned, so the UI can highlight them. Whatever does this must not hard-code the geometry of
a queen, or the validator has to be rewritten whenever the rules change.

**Options.**
- **A — Pairwise** over `PairwiseRules.attacks`: `O(k²)`. Works for any rule, including ones
  that are not lines at all (a knight's move).
- **B — Counters over the four queen axes** (row, col, `r−c`, `r+c`): `O(k)`, but the axes are
  N-Queens geometry written into the validator.
- **C — Counters over lines the *rules* supply**: `O(k)`, and the geometry stays in the rule.
- **D — Bitmask**: fastest, but answers "is this square attacked" with a boolean rather than
  naming *which* queens conflict.

**Decision.** **C.** Rules have two forms: `PairwiseRules.attacks(a, b)` is the definition and
the general case; `LineRules.linesThrough(cell)` is the structural form the game actually runs.
`conflicts()` counts how many queens occupy each line and flags those on a line holding more
than one. **A is kept as the test oracle. Bitmask is not used for validation.**

**Why.** The UI needs the conflict *set*, not a boolean, which rules out D. B is fast but puts
queen geometry in the validator, so the rules and the validator stop being separable. C keeps
both: `O(k)`
detection, and the geometry supplied by whichever rule is in play.

Counting also removes the identity-pair problem for free: a lone queen occupies each of its
lines once, and the test is `> 1`, so nothing has to compare a queen with itself.

**Costs, accepted.** Two rule interfaces instead of one. Rules whose threats are not lines —
Amazons, with its knight move — cannot use the fast path and must go through `attacks`.

**Bonus.** Having two independent implementations is what makes the property test meaningful:
counting and pairwise are different algorithms for the same question, so agreeing on hundreds
of random boards is a real check rather than a restatement of the code. A single
implementation could only be tested against itself.

**Revisit if.** Profiling on large `n` shows the `HashMap<Line, Int>` is the bottleneck — the
same counting works over an `IntArray` keyed by a packed line id.

---

## D2 — Solver: bitmask backtracking, computed once and cached

**Options.** Bitmask backtracking (fastest search); set/HashSet backtracking (clearest);
explicit `O(n)` construction (no search, but fiddly, one solution from empty).

**Decision.** **Not built.** Its only consumer would be hints, which are out of scope, and a
tested solver that nothing calls is weight carried for a feature nobody asked for. When hints
arrive: bitmask backtracking, run once per `n` and cached.

**Why.** For one solution the cheapest solver is the one you rarely run — precompute and
cache. Bitmask keeps that single run fast. The explicit formula is a real bug surface for
marginal gain; skipped.

**Revisit if.** We must complete a player's *partial* board — seed the masks from placed
queens, or fall back to set-based backtracking.

---

## D3 — State management: MVI for Game, MVVM for Setup (v1)

**Decision.** **MVI on Game, MVVM on Setup and on Scores** — the records are a list read from a
repository and two commands over it, which is what MVVM is for.

**Why.** Game is a state machine — every move is one action through one reducer — and it is where
the whole game happens, so "one action, one reducer branch" is the uniformity that pays off. The
win state added no one-shot effect after all: the card is drawn from `BoardSnapshot.isSolved`,
which the screen already reads, so there is still nothing an effect channel would carry.
Setup is simple input; MVI would be overhead. Match the pattern to the complexity.

**Revisit if.** Setup grows real interactive complexity (e.g. a puzzle editor).

---

## D4 — Modularisation: `:core:boardlogic` + `:app` for v1

*(Superseded by D16, which made every feature a module — the option this decision rejected as
gold-plating — and by D18, which moved every table into `:core:database`, leaving the app with no
table of its own and no query. What follows is the decision as it was taken, and why it held for as long as
there was one game.)*

**Options.** Single module with packages; domain + app; add a data module now; full
per-feature modules.

**Decision.** **Two modules for v1**: `:core:boardlogic` (pure) and `:app` (feature packages).
A **`:core:database`** module is added only when persistence lands (best times, saved game).

**Revised.** Persistence has landed: a solved board is recorded and the records are listed and
deleted on their own screen. The feature states what it needs as a repository in its `domain`
and implements it over **Room** in its `data` — the feature is a list with per-row and wholesale
deletion, which is a table, not a preferences file — and it is tested against an in-memory
database rather than a mock, so the queries themselves are exercised. What a view model is given
is the repository, so a second source later is a change behind it and not in front of it.

How a database is opened became a module of its own, holding **only that**: no entity, no DAO,
no query. Above the features sits one database, `puzzle.db`: a feature that wants storage adds
its table to it rather than opening a file of its own, because two Room databases pointed at one
file corrupt each other and a file per feature would multiply what has to be opened, migrated
and backed up.

What a feature owns is its table, its queries and the repository over them; what it does not own
is the database. Room needs every entity of a database declared in one place, so `PuzzleDatabase`
names each feature's table and hands out the accessor each reads its own through — the one place
in the app where a feature's names were known outside it, which is what D18 later removed by
moving the tables to the database rather than the database to the tables.

**Why.** A separate `:core:boardlogic` lets the compiler forbid Android in the domain. Adding
`:core:database` before there is anything to persist would be a module with no content and a
coverage gate over nothing. Full per-feature modules would be gold-plating for two screens.

**Revisit if.** Persistence lands (add `:core:database`) or independent features need isolated
build times.

---

## D5 — Conflicts are soft, and impossible actions are ignored rather than refused

**Decision.** A queen under attack is **placed and highlighted, never refused**; tapping her
again takes her back. There is **no hard rule at all** in this scope — no `canPlace`, no reject
path, and `CellStatus` has three values — because nothing in the brief forbids a square.

An action that cannot be carried out — a tap outside the board, a board too small to have a
solution — **leaves the state unchanged**. `reduce` is total: no input can make it throw. The
game route holds the same line: a size it cannot play sends the player back to Setup instead of
raising, so a back stack restored after the process died cannot crash the app, and neither could
a deep link if one were ever declared — the app declares none today.

**Why.** Placing freely and seeing the conflicts is how the puzzle is played. Building the
reject path with nothing to refuse would be a branch nothing takes and no test could justify.

**Cost, accepted.** Blocked or given squares are therefore *not* a one-line change: they touch
`GameState` (a field), `reduce` (a branch), `CellStatus` (values) and `snapshotOf`. Four places,
not one — better stated plainly than claimed as an isolation that does not exist.

---

## D6 — Animations are drawn in Compose, as pure functions of one number

**Context.** The brief asks for placement and victory animation.

**Decision.** **The victory celebration is drawn in Compose**, in a layer of its own between the
scrim and the card: eighteen pieces travel out from the middle, grow, turn half a circle and fade.
**No third-party animation library is a dependency.** The motion is small enough to state as a
function, and an artifact nothing executes is weight rather than readiness. What produces the
number is Compose's own `animation-core`: one `Animatable` runs from 0 to 1 and everything drawn
is a pure function of it.

**Why.** The full-screen win is where an animation pays off most, and the domain stays
animation-agnostic: what moves is a pure function of one number, in the presentation layer alone.
What is drawn can therefore be replaced without touching what decides when to draw it — the layer
is one composable and one number wide.

**And then the same shape again, per cell.** The placement bounce and the conflict flinch were
built last, in `SquareMotion.kt`, to the pattern the celebration set: `landingAt` and `shakeAt`
are pure functions of a number between 0 and 1, and two composables drive them. They were left
until last on purpose — per-cell motion earns its keep only once the board itself feels alive,
which is a later concern than a win the player waited for.

**Cost, accepted.** Motion drawn in a graphics layer moves nothing a test can measure. The curves
and their drivers are asserted; that `Square` passes the two numbers to the layer is not, so an
animation computed and never drawn would pass the suite. Recorded in `PROJECT.md` §6.

**Revisit if.** An authored animation arrives from a designer, or the celebration is asked to
carry more than a burst — then the layer is the seam it replaces, and the function is what tells
it when.

---

## D7 — Dependency injection: Hilt

**Decision.** **Hilt.** `QueensModule` contributes the `Puzzle` —
the puzzle's name, its piece and its rules — into a set. Neither view model receives a `Puzzle`:
both are given `Puzzles`, and the board resolves the one its route named.

**Why.** The container was wired in the step that needed it, not before: annotations a reviewer
has to read past to find the code are a cost. It carried one binding for as long as the app had
one dependency; it is now nine, spread across the modules that own what they bind — the puzzle,
the database, its DAO, the repository over it, the verb a game writes a record through, the clock
it is stamped with, the scope the write runs in, the preferences file, and the palette read out of
it. Opening the database is no longer one of them: `connect` is a function the database's own
provider calls. The seam does not rest on the container either — `conflicts` and `snapshotOf`
take rules with **no default**, so no call site can silently assume N-Queens even where nothing
is injected.

**Revisit if.** Annotation-processing cost becomes a problem — Koin is the fallback.

---

## D8 — Counter storage: HashMap first, IntArray later

**Decision.** `HashMap` counters first (readable, no size assumptions); `IntArray` with a
diagonal offset kept as a documented optimisation. Identical asymptotics, so the array
version is a pure constant-factor change to reach for only if measured.

---

## D9 — Testing: a pairwise oracle, and gates that can actually fail

**Decision.** Validate conflict detection against the pairwise `attacks` **oracle** over seeded
random boards. Gate the domain at **90% line and 90% branch**, naming both counters, and gate
`*ViewModel*` classes at **85% line**. When a solver is built, check it against the known
solution counts (OEIS A000170). Screens are rendered inside `check` under Robolectric: most
tests hand a board straight to the content composable, and a few launch `MainActivity` so the
container, the navigation host and the route argument are exercised as the app assembles them.
A rendered test reads the semantics tree, not the pixels, so the square's paint decisions are a
pure function tested on its own — and then two test classes do read the pixels: Robolectric
rasterises under `@GraphicsMode(NATIVE)`, the window is drawn into a bitmap, and the colour under
a named square is compared against the token it should carry. That is not screenshot testing:
there are no golden images to review or to keep, only integer comparisons against
`theme/Color.kt`, so a deliberate colour change touches one line rather than a folder of
approvals.

**Why.** The oracle is simple enough to trust and catches divergence in the fast code. Naming
the counters matters more than it looks: JaCoCo measures *instructions* by default, which a
branch-heavy predicate can satisfy while half its outcomes go untried.

**A gate that cannot fail is worse than no gate**, because it sits on the list and buys
confidence it has not earned. Three ways this happens, all guarded against here: applying
`dependency-analysis` to the root project alone, which analyses nothing; a JaCoCo limit with no
`counter`, which measures instructions rather than the lines and branches it appears to; and a
coverage rule over a class set that turns out to be empty, which passes without measuring
anything, or is skipped outright because its coverage data has moved — so
`viewModelCoverageInputs` checks both of the gate's inputs and fails before the floor is read.

---

## D10 — Hints and solvability deferred

**Decision.** Hints, "dead-end" warnings and the solver they rest on are **deferred**, together.
Neither the feature nor its machinery is built.

**Why.** A guaranteed-solvable hint needs a solver and its own UI. Building the solver first
would leave tested code with no caller, so nothing is written for it.

---

## D11 — No signing / release step

**Decision.** No signing/release step in the plan.

**Why.** A conscious omission: the deliverable is source and a short demo video, not a Play
Store artifact. Noted so it is a decision, not a gap.


---

## D12 — What the layering actually enforces

**Context.** Each feature is laid out one package per screen, each holding its own `domain`,
`presentation` and — when it needs one — `data`. It reads as Clean Architecture.

*(D16 later moved the features into modules of their own, which made most of what follows real
rather than conventional: an edge that is not declared in a build file cannot be written. What
stays a convention is the layering **inside** a module.)*

**Decision.** Keep the layout, and **state plainly which boundary is enforced and which is a
convention.**

- `:core:boardlogic` is a separate Gradle module with no Android on its compile classpath. The
  compiler makes a violation impossible. This one is real.
- A feature's `domain` and `presentation` are packages inside one module. Nothing stops a
  composable from doing arithmetic a view model should do, and the view models import
  `androidx.compose.runtime.mutableStateOf`, so `presentation` is not a framework-free layer.
- What is between features is enforced: `:features:play` cannot see `:features:scores`, because it
  does not depend on it (D16).

**Why say so.** The valuable boundary here is not the one the folder names advertise: it is
`SetupScreen` (bound to a view model) split from `SetupContent` (everything passed in), which is
what lets each state be drawn by passing values in: `SetupContent` takes its state as a
parameter, and every screen test renders it without a view model. Not *only* values, though — it
reads the board palette from a `CompositionLocal` that throws outside `PuzzleTheme`, so anything
rendering it has to wrap it, and that dependency is not in its signature. Claiming that every boundary
is equally strong invites the one question a reviewer is certain to ask.

**Not enforceable as written.** A Konsist or ArchUnit rule asserting "`presentation` may not
import Compose" would make the convention a boundary, but the view models hold their state in
`mutableStateOf` (D13), which is Compose. The package layout is a reading aid; the module is the
wall.


---

## D13 — Screen state: Compose's `mutableStateOf`, not `StateFlow`

**Options.** `mutableStateOf` (Compose's snapshot state); `StateFlow` (framework-neutral);
`LiveData` (older, no reason to reach for it here).

**Decision.** **`mutableStateOf`**, in every view model, Setup and Game alike.

**Why.** The screen reads the value and recomposes, with no collection to set up, no coroutine
to keep alive and no `collectAsStateWithLifecycle` at the call site. Tests read the property
directly. For a game whose state changes on a tap, that is the whole job.

**Costs, accepted.**
- The presentation layer imports `androidx.compose.runtime`, so it is not framework-free and
  cannot be policed as such (D12).
- The elapsed timer on the game screen is a coroutine in `viewModelScope` writing into
  `mutableStateOf`, rather than a flow that is naturally a stream.
- Should a view model ever need to be driven by something other than Compose, this is the
  decision to revisit first.

**Note on the pattern split.** Setup is MVVM and Game is MVI (D3): that difference is about how
changes are *expressed* — named methods versus one `onAction`. How the state is *observed* is a
separate axis, and it is the same on both screens.

---

## D14 — A solved board names its puzzle with a key of its own

**Options.** A stable key of the feature's own (`"queens"`); the `@StringRes` id that names the
puzzle on screen; the whole `Puzzle` serialised.

**Decision.** **The key.** `Puzzle.key` is written into the row and never leaves the feature;
what a screen shows is looked up from the `Puzzle` the route named, not from the row.

**Why not the resource id, which was the first decision here.** Resource ids are assigned when the
resource table is built, in alphabetical order of resource name, so adding or removing a string
that sorts before `variant_queens` renumbers it. That was a documented cost while the column was
inert. It stopped being a cost and became a defect once the best time was asked per size **and**
puzzle: after such a build every row written by the previous one fails to match, and the win card
silently never reports a best time again for a board the player has already solved.

**What is left of the older word.** The code called a puzzle a *variant* before it called it a
puzzle, and one place still does: the column. The Kotlin says `puzzle` everywhere — the row, the
query's parameter, the repository, the domain — and `@ColumnInfo(name = "variant")` keeps the bytes
where they are. Renaming the column changes the schema's identity, which means a migration for every
install that already holds the table, and it buys a player nothing.

**Cost.** None yet. Nothing has been released, so no file on any device holds the old column and
there is no migration to write — the first version is the one with the key. The schema is exported
to `core/database/schemas/`, which is what the first real migration will be written and tested
against.

---

## D15 — A solved board is written down outside the screen that solved it

**Options.** The view model's own `viewModelScope`; a `CoroutineScope` that lives as long as the
application, injected; `NonCancellable` around the two calls.

**Decision.** **An application-scoped `CoroutineScope`**, provided once as a `@Singleton` and
injected into `PlayViewModel` under an `@ApplicationScope` qualifier. The ticker and everything
else that only feeds the screen stay in `viewModelScope`.

**Why.** The record is not work for the screen; it is the only trace of what the player did.
`viewModelScope` is cancelled when the destination is popped, and the write begins with a read —
the first read of a session, which is the call that opens the database file. A back press inside
that window dropped the row in silence, and the cancellation was rethrown deliberately, so
nothing reported it. An audit reproduced it: the board was solved, the store cleared, and the
table never saw the row.

**Why not `NonCancellable`.** It is two lines and it protects the work already running, but it
says "ignore cancellation" where what is meant is "this does not belong to the screen". The scope
says which, and the qualifier names it at the call site.

**Cost.** One more binding, and a scope that is never cancelled — deliberately, since it lives as
long as the process. Its supervisor keeps one failed write from taking the next one with it.

**And a thread, which is the part worth watching.** The write left `Dispatchers.Main.immediate`
for `Dispatchers.Default`, and `record` reads `generation` to decide whether the answer still
belongs to the game on screen. That counter is the screen's own, written when a board is reset,
so the comparison is made back on the main thread rather than from the background: the row is
written off the screen, the question about which game it belongs to is asked where the game is
played. Read it from the other thread and the guard becomes a stale read of a field with no
ordering behind it — which is what the first version of this change did.

---

## D16 — One module per puzzle, and a contract between features

**Options.** Keep the features as packages inside `:app` and hold the boundaries by convention;
make each feature a module and let one depend on another where it has to; make each feature a
module and put what two of them share in a third that neither owns.

**Decision.** **The third.** Twelve modules. `:features:setup` and `:features:play` are the screens a
puzzle is played through and depend on `:core:puzzletype`, never on a game. `:games:nqueens` provides one `Puzzle` and depends on
nothing that draws it. The shell writes a finished board through `:core:solves` — one interface,
one method — which `:features:scores` implements. No two modules a person would work in at the
same time depend on each other.

**Why.** Two features that exchange data must share *something*; what is chosen is where that
something lives. In the contract module neither owns it, so the only thing two people have to
agree about is the contract, and changing it is a deliberate act rather than a surprise. The
previous shape had `game/presentation` importing `history/domain` — an edge nothing prevented,
nothing documented, and an audit found.

**Why not a bigger contract.** `:core:solves` could have carried the whole `SolveRepository`,
which is what the game used to receive: five methods, of which it needed two. A game could then
delete every record in the table. One verb is what the game does, so one verb is what it can do.

**Why the shell rather than a copy per game.** A puzzle module could have held its own screens,
which would make each game entirely independent — and would mean fixing the board, the
accessibility work or the animation N times. The line is drawn at "would a second puzzle rewrite
this": the rules, the words and the glyph, yes; the board, the timer, the win card, the sounds
and the records, no. A puzzle that stops fitting the shell copies the shell too and becomes
independent; that is the escape hatch, and it is why the shell is a module a game is handed to
rather than a framework a game is written against.

**Cost.** Twelve modules for about 4,000 lines is more machinery than this code needs today, and the
build file of a module is now something to keep honest — `dependency-analysis` fails the build on
an undeclared or unused dependency in each of them, which is the only reason that stays true.
Two Setup previews were lost: a preview needs a concrete puzzle and the shell has none.

**And one thing it cost that is worth naming.** The shell's tests are written against a real
puzzle — the two screen modules take `:games:nqueens` as a **test** dependency. Production has no such
edge and the release graph has no game in it, but the tests do, and they would have to be
rewritten against a stand-in if that ever became untrue.

---

## D17 — The modules stay

**Options.** Collapse the modules into `:app` with `core/`, `features/` and `games/` as packages
and hold the boundaries by convention; keep them.

**Decision.** **Keep them.** The reason is not that a game module could be lifted into another
build. It is what the boundaries do while the code is being worked on:

|               |                                                                                     |
|---------------|-------------------------------------------------------------------------------------|
| Roles         | The compiler refuses what convention only discourages. `:games:nqueens` **cannot** import a feature's package. As packages it could, and the import would look like every other import. |
| Tests         | `:core:boardlogic:test` runs 41 pure-JVM tests in **2.6 s**. The whole suite, cold, is **63.7 s** — and in one module the 41 could not be run without first compiling the app's entire test source set: Compose, Robolectric, and Hilt and Room annotation processing. |
| Gates         | The floors are per module: the domain at 90% line and 90% branch, view models at 85%, screens excluded because Robolectric is invisible to JaCoCo. One module is one number, which the screens drag to something that cannot fail. |
| A new puzzle  | Copy `games/nqueens`, change the rules and the words, contribute one binding. Two shared lines — the `include` and the app's dependency — and nothing else anyone is working in. |

**Evidence from this repository.** The coupling that D18 removes — `:app` reaching into a
feature's `data` package to name its table — was visible **because** the modules made it a
declared edge and forced two classes public. Under packages it would have been an ordinary
import with nothing to see.

**Cost.** Twelve build files to keep honest, which only `dependency-analysis` makes bearable: it
fails the build on an undeclared or unused dependency in each of them.

**Revisit if.** The Gradle plumbing costs more than the boundaries return — which would show up
as build files changed more often than the code in them.

---

## D18 — Every table in one module, the features above it

**Options.** The app declares the one database and names each feature's table; each feature opens
a database of its own; the tables move to `:core:database` and the features import them from
there.

**Decision.** **The third** — the shape Google's *Now in Android* uses. `:core:database` holds
`PuzzleDatabase`, every `@Entity` and every `@Dao`. A feature is injected the accessor for its own
table and never names the database, never opens a file, and is handed no other feature's rows.

**Why not the first.** It was the shape until now, and it made `:app` import
`history.data.SolveRow` and `SolveDao` — two classes that had to be `public` for no reason a
reader of the feature could see, while everything around them was `internal`. The application was
reaching into a feature's data layer to assemble something neither of them owned.

**Why not the second.** A file per feature multiplies what has to be opened, migrated and backed
up, for a coupling only the assembling module carried.

**Why "core knows the tables" is not the inversion it looks like.** `:core:database` depends on
no feature: it declares the tables, and the features depend on it. The reverse — the database
class in `core` with the tables left in the features — is a **cycle**, because `@Database` must
name its entities: `core → feature` to see the table, `feature → core` to be given the DAO.
Gradle refuses it. There are exactly two shapes, and this is the one where nothing is public
without a reason.

**What it cost.** A feature no longer owns its table, so `cp -r features/scores features/streaks`
no longer carries the storage with it, and a new feature that stores something edits
`:core:database` — a module everyone shares. That is the trade: the app is hollow and nothing
leaks upward, at the price of a shared file when a table is added.

**What it bought.** `:app` is three files — `MainActivity`, `PuzzlesApplication`, `PuzzleNavHost`
— and none of them writes a row or names a table. Its only hand-written edge into a feature is
that feature's own navigation destination, which is what a feature publishes on purpose.

**What it did not buy, and an audit had to say so.** Two things this decision reads as promising
are not true, and are not fixable by moving code:

- **`:app` still compiles against `SolveDao`.** Dagger's component lives at the composition root
  and must name every type in the graph, so the generated `DaggerPuzzlesApplication_…` imports
  it. That is a property of Hilt, not a leak this shape can close; what the move removed is the
  *hand-written* import, which is the one a reader meets.
- **"A feature cannot reach another feature's rows" holds only while there is one table.** Every
  DAO in `:core:database` is `public` in one package, so the second table is visible to the first
  feature the moment it is added. Kotlin has no package-private, so nothing but review will stop
  it; if that ever matters, the split is one module per table rather than one module for all.

**Revisit if.** Two features are being added at once and `PuzzleDatabase` becomes a file two
people edit in the same week.

---

## D19 — The shell is a feature, and every Android module is built the same way

**Two changes, both about legibility rather than behaviour.**

**`:core:play` became `:features:play`.** `core/` in this repository holds what has no screens:
the puzzle family, the theme, two contracts and the tables. `play` held two screens' worth of
composables and two view models — it is the application's largest feature, and having it under `core/` while
`history` sat under `features/` is what made the layout read as arbitrary. It is still what every
puzzle is played in; that is a fact about who uses it, not about where it belongs. Nothing moved
in Kotlin: the namespace and the packages are unchanged, so the rename is one line in
`settings.gradle.kts`, one directory, and one `project(...)` reference in `:app`.

**The shared half of the module build files moved to the root.** The library modules and the
application declared the same `compileSdk`, `minSdk`, `compileOptions`, `lint` block, Debug-only
test filter and Kotlin compiler options — the same twenty-odd lines, seven times, drifting one
comment at a time. They are now set once, in `subprojects { plugins.withId(...) }`.

**What deliberately stayed in the modules.** Only what actually differs: the namespace, the
`resourcePrefix`, whether the module draws, `testOptions.targetSdk` where Robolectric runs, the
`2g` heap where Compose rasterises, and `explicitApi()` where a module has consumers. Two
modules do not pin `testOptions.targetSdk`: `:core:ui`, which runs no Robolectric, and `:app`,
which does — an application declares a `targetSdk` of its own, and the root sets it, so a library
is the only kind of module that needs the line.

**Why the root rather than `buildSrc`.** A precompiled script plugin would be type-safe and
reusable across builds, at the cost of a second compilation unit and a full-build invalidation
whenever it changes. This build already configures spotless, detekt and dependency-analysis from
the root in exactly this way; a convention that lives beside them is one thing to read, not two.
`buildSrc` becomes worth it when a second build needs the same conventions.

**Cost.** A module's build file no longer states its whole configuration; `compileSdk` is now
somewhere else. The gate answers that objection — anything the root gets wrong fails every module
at once rather than one quietly.

---

## D20 — Setup and the game are two features, not one

**Options.** Keep both screens in one module, as they had been since they were written; split them
into `:features:setup` and `:features:play` beside `:features:scores`.

**Decision.** **Split.** The evidence said they were already two things that happened to share a
build file:

|                   |                                                                     |
|-------------------|---------------------------------------------------------------------|
| Kotlin            | not one import between the `setup` and `game` packages, in either the production or the test sources |
| Resources         | already separated by name — 9 `play_setup_*` strings against 13 `play_game_*`, and every drawable and sound the game's |
| Conversation      | none. Setup answers with a choice; `:app` turns it into `playRoute(...)`. The two never called each other |

**What the compiler now proves.** `:features:setup` does not declare `:core:solves`, so the screen
that chooses a board cannot write a solved one. Before the split nothing stopped it: the contract
was in the same module as the code that uses it.

**Test tasks.** 108 tests in one task became 87 and 21 in two, which Gradle runs in parallel.
Someone working on the puzzle picker runs 21 tests, not 108.

**Cost.** Two build files instead of one, two namespaces, two debug manifests, and the resource
prefix `play_` became `setup_` and `game_` — 22 strings, 2 drawables, 4 sounds and their
references. D21 later renamed `game_` back to `play_`, when the module took that name. The touch-target walk, which was one class covering both screens, is now one per
module: each still walks its own screen, and no single place says "every screen" any more.

**Two tests stopped borrowing.** `SetupScreenTest` and `PlayScreenTest` prove a screen speaks
whatever words the puzzle names it, by handing it a puzzle whose words are some other string. They
had been borrowing the *other screen's* strings, which the split made impossible. They now use the
platform's (`android.R.string.copy` and its neighbours) — a word the screen has no other reason to
show, which is what the test needed all along.

---

## D21 — What the three groups mean, and when a module earns its place

Written down because the question kept being asked of one module at a time.

**The groups.** `core/` is shared and has no screens. `features/` is one capability the player
sees. `games/` holds one module per puzzle, each contributed into a set and depending on nothing that draws it.

**The test.** A module earns its place if removing it would either give somebody access they do
not need, or reverse an arrow. Both halves matter: a module that only saves typing is machinery,
and one that only looks tidy is a directory.

**It is not "keep everything".** `:core:database` failed the test the day it was written about:
one interface, one caller, and no second caller coming while the app owned the database. It was
folded into `:app` and came back as a module only when D18 gave it the tables to hold.

**Two modules are small on purpose.** `:core:solves` is 24 lines and `:core:puzzletype` is 119, and
the smallness is the work rather than a shortfall: the less a contract holds, the less two people
have to agree about, and the less either side can do to the other. `:core:solves` has no home
that does not widen what a feature sees — in `:core:puzzletype` the records would gain the whole
puzzle model they went out of their way to know only as a string key; in a feature it becomes the
edge it exists to prevent; in `:app` the arrow reverses. `:core:puzzletype` has none either: in
`:core:boardlogic` every game module would gain the board's whole state machine to declare a
puzzle with, and merged with `:core:solves` the records would gain the puzzle model and a game
would gain the write. It is not that `Puzzle` carries `@StringRes` and `@DrawableRes` — an
audit checked, and `androidx.annotation` is a plain jar, which is why `:core:puzzletype` is a JVM
module and not an Android one.

**Inside `core/` there are two kinds**, and the tree does not separate them: contracts two sides
agree on and neither owns (`solves`, `scope`, and `Puzzle` itself), and shared implementations
that do something (`boardlogic`, `ui`, `database`, `settings`, and `Puzzles` with its refusals). `:core:puzzletype` is both,
which is why a `contract/` group would be half-true for it — a directory level for less than two
modules. `core` is also the word the ecosystem uses for this, which is worth more to a reader than
a private taxonomy.

---

## D22 — A palette the player chooses, in a preferences file rather than the database

**Options.** No setting at all, and follow the phone; a setting kept in memory for the visit; a
row in the one database; a preferences file of its own.

**Decision.** **A preferences file**, in `:core:settings`, over DataStore. D4 drew the line for
records — "a list with per-row and wholesale deletion, which is a table, not a preferences file" —
and this is the other side of it: one value, replaced rather than appended to, never queried and
never listed. A table for it would be a table with one row and no question to ask of it, and it
would put the palette behind the same migration cost as a record.

**Why there is no third value for "follow the phone".** `ThemeChoice` is `LIGHT` or `DARK`, and a
player who has not chosen has stored nothing. Null is that, and only a screen can resolve it,
because only a screen can ask the phone. So what is stored and what is drawn are different types,
which is what stops "unchosen" from quietly becoming a palette somebody has to maintain. A name
written by a later version and unknown to this one reads the same way — no answer rather than a
failure — so an upgrade and a downgrade both degrade to the phone's own setting.

**Why the write outlives the screen.** It goes through `:core:scope`, like a solved board. A
player may press the button and leave in the same breath, and a preference the app never wrote is
a choice it forgot. The same move fixed the records screen, where a delete followed by leaving was
being dropped in exactly that way.

**What it cost, found the hard way.** The first version had no handler on the write and no
corruption handler on the store. A half-written file — a power cut, a restored backup — read back
as "no choice" and then threw on the next write, straight into the uncaught handler: the app died
on the first press of the button, and again on every launch after, because nothing repaired the
file. It now swallows a refused write like every other write in the app, and the store replaces a
file it cannot parse.

**Where the palette is applied, and why it is read before the first frame.** `MainActivity` blocks
for the first value *and* collects the rest: the blocking read decides the frame, the collection
follows the button. They are two different questions and the interface asks them separately —
`chosen()` answers once and gives up, `choice` asks a busy file again a second at a time. Reading
the frame off the flow put those three seconds on the thread that draws, on every rotation as well
as every launch. Collecting alone means every creation of the activity — a rotation as much as
a cold start — draws one frame in the phone's palette before flipping to the player's, and on the
combination that disagrees that frame is the unreadable one. The file is small and the read is one
open. Its one cost is a file too damaged to parse, which is replaced on the main thread: once, on
a file already broken, and cheaper than the frame it buys.

**What the choice decides beyond what Compose draws.** The window behind the app and the two bars
the system draws over it, both set from the activity. `enableEdgeToEdge` answers the icon colour —
and below API 29 the scrim behind the navigation bar — from the phone's own mode unless it is told
otherwise, so it is told: a dark app on a light phone otherwise gets dark handles on a dark strip.
The window background is repainted for the same reason, because `values-night` can only answer
from the phone. What is left to `values-night` is the launch screen, drawn from the manifest before
the activity exists — the one frame the player's answer cannot reach, and the right answer for
every player who never opens the button.

**Revisit if.** A second preference lands and the module starts holding a settings *screen* rather
than one value, or the blocking read starts costing anything measurable at launch.
