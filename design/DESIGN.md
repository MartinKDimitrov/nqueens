# N-Queens — Mobile UI Design Reference

Portrait-first (390 × 844) visual reference for an Android N-Queens puzzle game.
All colors, type sizes, spacing, radii and elevation come from
[`tokens.json`](./tokens.json) — it is the single source of truth. The SVGs in `screens/` are
drawn from those values; never introduce a color that is not in `tokens.json`.

- Icon: [`queen.svg`](./queen.svg) — single self-contained icon, `fill="currentColor"`,
  legible down to ~14 px (12×12 board on a 360 dp screen).
- Screens: `screens/setup.svg`, `game.svg`, `game-conflicts.svg`, `win.svg`,
  `scores.svg`, `legend.svg` (light theme; swap each hex for its `colors.dark`
  counterpart for the dark theme).

## Design principles

- Chess-inspired, not skeuomorphic: two low-chroma neutral squares, no wood.
- Board sizes 4×4 – 12×12 share one layout: a square whose cells are `side / N`. On Setup the
  square is a fixed 262 dp, matching the card in `setup.svg`.
- State is never signalled by color alone: conflict adds an outlined tile and a
  glow, hint adds a dashed ring, blocked adds a center dot, fixed adds a border.
- Contrast is not uniform, and the pairings below are text against its own background at AA's
  4.5:1. Dark falls short at `blocked` on `surface` (2.79:1), `conflict` on `boardLight` (4.21:1)
  and `fixed` on `boardLight` (4.38:1) — the last two clear the 3:1 threshold that applies to
  marks rather than letters. Light falls short at `conflict` on `conflictGlow` (3.07:1), `hint`
  on `surface` (2.09:1), `success` on `surface` (3.45:1) and `blocked` on `surface` (2.59:1) and
  on `boardDark` (1.44:1). The conflict pairing is the one that matters,
  since it is the state the game relies on — which is why a conflicting queen is also given an
  outline and a glow rather than a colour alone.

## Screens

### Setup (`setup.svg`)
Title block, a preview of the board at the chosen size, a board-size stepper
(`−` / `N × N` / `+`, min 4, default 8, max 12) and a variant dropdown
(Queens, Rooks, Bishops, Amazons). A full-width `primary` **Start** button sits
above the safe area, with the current best time for the selected size below it.

*Implemented differently:* the app has one puzzle, so the variant control is a row that shows it
rather than a dropdown, and there is no best time because nothing is stored. The screen is a
plain column that does not scroll, so Start follows the controls rather than being anchored to
the bottom.

### Game (`game.svg`)
Top bar on `surface`: queens-left counter, elapsed timer, reset action — three
equal pills of `radii.md`. The board is centered with a `surface` frame and
`radii.md` corners. A status strip above the bottom bar carries the hint text.
Bottom bar: two equal-width secondary buttons, **Undo** and **Hint**.

### Game — conflicts (`game-conflicts.svg`)
Identical layout; attacking queens switch to `QUEEN_CONFLICT` (conflict-tinted
tile, 2 px `conflict` border, glowing queen glyph) and the status strip becomes
a `conflict` banner naming the attack line. Non-attacking queens stay `QUEEN`.

### Win overlay (`win.svg`)
The solved board stays visible under a 55 % `onSurface` scrim. Layers are kept
discrete so a Rive celebration can replace the static confetti layer:
`scrim → confetti slot → card → badge → content`. The card shows a success
queen badge, “Solved!”, the finishing time, a best-time delta, a **Play again**
primary button and a **View scores** text action.

### Scores (`scores.svg`)
Best times grouped by board size, one `surface` card per size, rows of
rank badge / monospaced time / date. Rank 1 uses the `primary` badge. A
**New game** button closes the screen.

### Legend (`legend.svg`)
Every cell state at real board scale with its name and backing token.

## Cell states

| State | Appearance | Light tokens | Dark tokens |
| --- | --- | --- | --- |
| `EMPTY` | Alternating square, no piece | `boardLight` `#E6EAEF` / `boardDark` `#B9C2CE` | `boardLight` `#2C3A45` / `boardDark` `#1A252E` |
| `QUEEN` | Queen glyph on the plain square | `queen` `#12212B` | `queen` `#EAF1F5` |
| `QUEEN_CONFLICT` | Tinted tile + 2 px border + glowing glyph | `conflict` `#D93A3A` on `conflictGlow` `#F7C9C9` | `conflict` `#FF6B6B` on `conflictGlow` `#4A1E22` |
| `BLOCKED` *(not implemented)* | Dimmed tile with a center dot, not tappable | `blocked` `#97A2AE` | `blocked` `#55636F` |
| `FIXED` *(not implemented)* | Given queen, muted glyph + solid border, locked | `fixed` `#4A5A69` on `surfaceAlt` `#E9EDF2` | `fixed` `#8FA1AE` on `surfaceAlt` `#1B262F` |
| `HINT` *(not implemented)* | Dashed ring + 22 % tinted fill + ghost queen | `hint` `#54C3CB` | `hint` `#2FBFC8` |

## How the app uses this

The palette is transcribed by hand into `theme/Color.kt`; nothing reads `tokens.json` at build
time, so the two are kept in step by review rather than by a check.

- Material's own roles — primary, surface, background, error — go into the Compose colour
  scheme. The colours Material has no name for travel beside it in `BoardColors`, through a
  `CompositionLocal`: `boardLight`, `boardDark`, `queen`, `conflict`, `conflictGlow`, `border`,
  `surfaceAlt`, `onSurfaceMuted`. The tokens for states that do not exist yet — `blocked`,
  `fixed`, `hint`, `success`, `queenOn` — are not in the code.
- `typography.scale` maps onto the Material text styles by size, weight and line height.
  `letterSpacing` is not carried across; the one place the app sets it, the Setup title, uses a
  wider value than the token because the design draws the title spaced out.
- `spacing` and `radii` become `Spacing` and `Radii` in `theme/Dimens.kt`, in dp, each carrying
  every step the tokens define. `elevation` is unused: nothing in the app casts a shadow.
- `queen.svg` is converted to `res/drawable/ic_queen.xml`, tinted by the caller. The same paths,
  white on the brand teal, make `res/drawable/ic_launcher.xml`.
- Cell state in code is one of three values — `EMPTY`, `QUEEN`, `QUEEN_CONFLICT`.
