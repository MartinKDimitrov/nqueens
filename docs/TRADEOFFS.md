# N-Queens Puzzle — Trade-offs and Decisions

Each decision is recorded as: context, options, decision, why, and when to revisit.
This is the reasoning behind the shape of the code.

---

## D1 — Conflict validation: counting line occupancy

**Context.** On every tap the board must be re-validated and the *set* of conflicting queens
returned, so the UI can highlight them. Whatever does this must not hard-code the geometry of
a queen, or adding a variant stops being a domain-only change.

**Options.**
- **A — Pairwise** over `PuzzleRules.attacks`: `O(k²)`. Works for any rule, including ones
  that are not lines at all (a knight's move).
- **B — Counters over the four queen axes** (row, col, `r−c`, `r+c`): `O(k)`, but the axes are
  N-Queens geometry written into the validator.
- **C — Counters over lines the *rules* supply**: `O(k)`, and the geometry stays in the rule.
- **D — Bitmask**: fastest, but answers "is this square attacked" with a boolean rather than
  naming *which* queens conflict.

**Decision.** **C.** Rules have two forms: `PuzzleRules.attacks(a, b)` is the definition and
the general case; `LineRules.linesThrough(cell)` is the structural form the game actually runs.
`conflicts()` counts how many queens occupy each line and flags those on a line holding more
than one. **A is kept as the test oracle. Bitmask is not used for validation.**

**Why.** The UI needs the conflict *set*, not a boolean, which rules out D. B is fast but puts
queen geometry in the validator, so a variant stops being one new rule. C keeps both: `O(k)`
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

**Decision.** **MVI on Game, MVVM on Setup.** Best times are out of scope, so there is no
Scores screen; were one built it would be MVVM, being a list read from a repository.

**Why.** Game is a state machine with many discrete actions and one-shot effects, and is
the screen extended live — MVI's "one action, one reducer branch" uniformity pays off.
Setup is simple input; MVI would be overhead. Match the pattern to the complexity.

**Revisit if.** Setup grows real interactive complexity (e.g. a puzzle editor).

---

## D4 — Modularisation: `:core:domain` + `:app` for v1

**Options.** Single module with packages; domain + app; add a data module now; full
per-feature modules.

**Decision.** **Two modules for v1**: `:core:domain` (pure) and `:app` (feature packages).
A **`:core:data`** module is added only when persistence lands (best times, saved game).

**Why.** A separate `:core:domain` lets the compiler forbid Android in the domain. Adding
`:core:data` before there is anything to persist would be a module with no content and a
coverage gate over nothing. Full per-feature modules would be gold-plating for two screens.

**Revisit if.** Persistence lands (add `:core:data`) or independent features need isolated
build times.

---

## D5 — Conflicts are soft, and impossible actions are ignored rather than refused

**Decision.** A queen under attack is **placed and highlighted, never refused**; tapping her
again takes her back. There is **no hard rule at all** in this scope — no `canPlace`, no reject
path, and `CellStatus` has three values — because nothing in the brief forbids a square.

An action that cannot be carried out — a tap outside the board, a board too small to have a
solution — **leaves the state unchanged**. `reduce` is total: no input can make it throw. The
game route holds the same line: a size it cannot play sends the player back to Setup instead of
raising, so neither a deep link nor a back stack restored after the process died can crash the
app.

**Why.** Placing freely and seeing the conflicts is how the puzzle is played. Building the
reject path before a variant needs it would be a branch nothing takes and no test could justify.

**Cost, accepted.** Blocked or given squares are therefore *not* a one-line change: they touch
`GameState` (a field), `reduce` (a branch), `CellStatus` (values) and `snapshotOf`. Four places,
not one — better stated plainly than claimed as an isolation that does not exist.

---

## D6 — Animations: Rive for the win, Compose for the rest *(not built)*

**Context.** The brief asks for placement and victory animation.

**Decision, for when the game screen exists.** **Rive drives the victory celebration** (one
`.riv`, trigger `celebrate`). **Placement bounce and conflict shake are Compose** (per-cell,
cheap, no asset). **Compose-native celebration is the fallback** if the `.riv` is not ready.
Nothing of this is built: there is no Rive dependency and no `.riv` in the repository.

**Why.** Matches the reviewers' stack where it has the most payoff (full-screen win) with a
single asset, and keeps the domain animation-agnostic (only the presentation layer maps effect →
Rive input). Per-cell effects are a poor fit for one shared Rive artboard, so they stay in
Compose.

**Risk / ownership.** A `.riv` is a binary authored in the Rive editor — it cannot be
generated from code. It is authored by a designer (or in the Rive editor by us), using
`design/queen.svg` and `design/tokens.json` as the visual source, against the fixed
contract: artboard `NQueens`, state machine `game`, trigger `celebrate`. A Rive Community
confetti file can serve as a placeholder.

**Revisit if.** The `.riv` slips — ship the Compose celebration for v1, swap in Rive later.

---

## D7 — Dependency injection: Hilt

**Decision.** **Hilt, when there is a dependency to inject — not yet.** `SetupViewModel` has no
collaborators, so a container would hold nothing. The first real binding is `LineRules` for the
game screen.

**Why wait.** Wiring a framework before anything needs it produces annotations a reviewer has to
read past to find the code. Meanwhile the seam is kept honest a cheaper way: `conflicts` and
`snapshotOf` take rules with **no default**, so no call site can silently assume N-Queens.

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
solution counts (OEIS A000170). Compose UI tests arrive with the board they would assert;
screen rendering beyond that is checked by hand on an emulator, since `check` does not launch
the app.

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
would leave tested code with no caller, so the seam is left open and nothing is written for it.

---

## D11 — No signing / release step

**Decision.** No signing/release step in the plan.

**Why.** A conscious omission for a take-home: the deliverable is source + a short demo
video, not a Play Store artifact. Noted so it is a decision, not a gap.


---

## D12 — What the layering actually enforces

**Context.** `:app` is laid out one package per screen, each holding its own `domain`,
`presentation` and — when it needs one — `data`, with the shared theme beside them. It reads as
Clean Architecture.

**Decision.** Keep the layout, and **state plainly which boundary is enforced and which is a
convention.**

- `:core:domain` is a separate Gradle module with no Android on its compile classpath. The
  compiler makes a violation impossible. This one is real.
- A feature's `domain` and `presentation` are packages. Nothing stops a composable from doing
  arithmetic a view model should do, and the view models import
  `androidx.compose.runtime.mutableStateOf`, so `presentation` is not a framework-free layer.

**Why say so.** The valuable boundary here is not the one the folder names advertise: it is
`SetupScreen` (bound to a view model) split from `SetupContent` (everything passed in), which is
what lets each state be drawn by passing values in: `SetupContent` takes its state as a
parameter, and the two previews render it without a view model. Not *only* values, though — it
reads the board palette from a `CompositionLocal` that throws outside `NQueensTheme`, so anything
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
