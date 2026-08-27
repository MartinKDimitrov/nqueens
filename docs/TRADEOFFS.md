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

**Why.** The UI needs the conflict *set*, not a boolean, which rules out D. B is fast but
would put queen geometry in the validator, so a variant would no longer be one new rule. C
keeps both: `O(k)` detection, and the geometry supplied by whichever rule is in play.

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

**Decision.** **Bitmask backtracking**, run **once per `n` and cached**. Built and tested
in v1; its consumer (hints) is a deferred extension (D10).

**Why.** For one solution the cheapest solver is the one you rarely run — precompute and
cache. Bitmask keeps that single run fast. The explicit formula is a real bug surface for
marginal gain; skipped.

**Revisit if.** We must complete a player's *partial* board — seed the masks from placed
queens, or fall back to set-based backtracking.

---

## D3 — State management: MVI for Game, MVVM for Setup (v1)

**Decision.** **MVI on Game, MVVM on Setup.** The Scores screen (deferred) is MVVM when it
lands.

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

## D5 — Constraints: conflicts soft, occupancy/blocked/fixed hard

**Decision.** Conflicts are **soft** (placed and highlighted, never refused). Occupied
cells toggle-remove; **blocked** and **fixed** cells are **hard** (tap rejected with
feedback). `BLOCKED`/`FIXED` are produced only by extensions, but the enum and the reject
path exist from v1.

**Why.** Standard N-Queens UX (place freely, see conflicts), and it isolates the extension
point: variants with given/forbidden cells plug into the hard path (`canPlace`), leaving
the soft highlight path untouched.

**Revisit if.** A variant needs conflicts to be hard — flip one policy flag.

---

## D6 — Animations: Rive for the win, Compose for the rest

**Context.** The brief asks for placement and victory animation.

**Decision.** **Rive drives the victory celebration** (one `.riv`, trigger `celebrate`),
in v1. **Placement bounce and conflict shake are Compose** (per-cell, cheap, no asset).
**Compose-native celebration is the fallback** if the `.riv` is not ready.

**Why.** Matches the reviewers' stack where it has the most payoff (full-screen win) with a
single asset, and keeps the domain animation-agnostic (only the `ui` layer maps effect →
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

**Decision.** **Hilt.**

**Why.** Standard on modern Android, integrates with Compose and ViewModel, and makes the
swappable seams (`PuzzleRules`, `Solver`, future repositories) explicit bindings. Manual DI
would be fine for this size but Hilt reads as production-idiomatic.

**Revisit if.** Annotation-processing cost becomes a problem — Koin is the fallback.

---

## D8 — Counter storage: HashMap first, IntArray later

**Decision.** `HashMap` counters first (readable, no size assumptions); `IntArray` with a
diagonal offset kept as a documented optimisation. Identical asymptotics, so the array
version is a pure constant-factor change to reach for only if measured.

---

## D9 — Testing: pairwise oracle, golden counts, gated ViewModel + Compose UI

**Decision.** Validate the fast paths against the pairwise `attacks` **oracle**; validate
the solver against **known solution counts** (OEIS A000170, incl. n=5→10). **ViewModel
coverage is gated**; **Compose UI tests** assert the board's place/conflict/win rendering;
screen rendering beyond that is a **recorded manual emulator pass**, since `check` does not
launch the app.

**Why.** The oracle is simple enough to trust and catches divergence in the fast code; the
solution counts are a real external fixture. Gating ViewModel coverage and adding Compose
UI tests closes the "green check but untested UI" gap the alignment review flagged.

---

## D10 — Hints and solvability deferred

**Decision.** The `Solver` exists and is tested, but "hint that guarantees solvability" and
"dead-end warning" are **deferred extensions**, not v1 features; cheap `safeCells`
highlighting is available when the UI is added.

**Why.** Guaranteed-solvable hints need solver + UI wiring beyond a comfortable window.
Keeping the solver ready but the feature deferred matches v1 scope while leaving the seam
open.

---

## D11 — No signing / release step

**Decision.** No signing/release step in the plan.

**Why.** A conscious omission for a take-home: the deliverable is source + a short demo
video, not a Play Store artifact. Noted so it is a decision, not a gap.
