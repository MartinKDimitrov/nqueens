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
- Board sizes 4×4 – 12×12 share one layout: a square whose cells divide what is left of its side
  after the frame's inset. On Setup the square is capped at 262 dp, matching the card in
  `setup.svg`, and takes the width when the screen is narrower.
- State is never signalled by color alone: in the mockups conflict adds an outlined tile and a
  glow, hint adds a dashed ring, blocked adds a center dot, fixed adds a border. None of those
  marks is in the app: it tints the conflicting square and the queen standing on it, and what
  carries the state non-visually is the square's content description, which reads "queen under
  attack".
- Contrast is not uniform, and the pairings below are text against its own background at AA's
  4.5:1. The pairing the app actually draws is `conflict` on `conflictGlow` — 3.07:1 in light and
  5.05:1 in dark — because a square under attack always takes the glow behind the glyph; a
  conflict colour is never drawn on a plain board square in either theme. The rest of the list is
  about states the mockups define and the app does not: dark falls short at `blocked` on
  `surface` (2.79:1) and `fixed` on `boardLight` (4.38:1), light at `hint` on `surface` (2.09:1),
  `success` on `surface` (3.45:1), which the win card's "new best" line does draw, `conflict` on
  `background` (4.20:1), which the records screen's **Clear all** draws, `blocked` on `surface`
  (2.59:1) and on `boardDark`
  (1.44:1). The conflict pairing is the one that matters, since it is the state the game relies
  on. The shipped square marks it with a tinted background
  and a tinted glyph; the outline the mockup draws is not there, so the non-visual carrier is
  the content description alone.

## Screens

### Setup (`setup.svg`)
Title block, a preview of the board at the chosen size, a board-size stepper
(`−` / `N × N` / `+`, min 4, default 8, max 12) and a variant dropdown
(Queens, Rooks, Bishops, Amazons). A full-width `primary` **Start** button sits
above the safe area, with the current best time for the selected size below it.

*Implemented differently:* the app has one puzzle, so the variant control is a row that shows it
rather than a dropdown, and where the mockup prints the best time the app puts an outlined
**Best times** button that opens the records instead. The preview draws
an empty board at the chosen size; the mockup draws a solved eight-queen one. Start follows the
controls rather than being anchored to the bottom, and the column scrolls, so on a screen too
short for the whole of it — a phone held sideways — the button is still reachable.

### Game (`game.svg`)
Top bar on `surface`: queens-left counter, elapsed timer, reset action — three
equal pills of `radii.md`. The board is centered with a `surface` frame and
`radii.md` corners. A status strip above the bottom bar carries the hint text.
Bottom bar: two equal-width secondary buttons, **Undo** and **Hint**.

*Implemented differently:* on a screen wider than it is tall the board sits at the left with the
summary, the strip and the hint beside it; the mockup only draws the tall arrangement. The top
bar carries a back button and three pills — the counter, the clock and reset — beside each other
on a window at least 340 dp wide, and the back button above them on anything narrower, where the
back button and the three pills cannot share a line without breaking the clock across two. The pills share a height rather than having a fixed one, so they
grow together when the player raises the system font size, and the status strip's mark is sized
in `sp` so it tracks the type rather than outgrowing it. `game.svg` does not draw the back
button, because the mockup
was updated in the design tool and the SVG was not regenerated. The board summary sits under the
bar rather than in it. The status strip carries an idle prompt or the number of queens under
attack; there is no hint feature, so it never carries hint text. The summary reads the puzzle's
name from the variant, so it is "12 × 12 · Queens" rather than the mockup's uppercase. There is
no bottom bar. The
board never draws a square smaller than 24 dp. It takes the width when the screen is tall and
the height when it is wide, and when neither leaves that much room it keeps the floor and the
screen scrolls instead. The details move beside the board only when what is left is wide enough
to read the status message in; otherwise they stay under it.

### Game — conflicts (`game-conflicts.svg`)
Identical layout; attacking queens switch to `PIECE_CONFLICT` (conflict-tinted
tile, 2 px `conflict` border, glowing queen glyph) and the status strip becomes
a `conflict` banner naming the attack line. Non-attacking queens stay `PIECE`.

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

*Implemented differently:* the times are drawn in the app's own type scale — no font family from
the tokens reaches the code, so nothing is monospaced. Each row carries a delete button and the
header a **Clear all**, neither of which the mockup draws, because a record nobody can remove is
permanent; the label of that button names the moment the board was finished, since two equally
fast solves of one board are otherwise indistinguishable to a screen reader. A card lists every
solve of its size a row at a time, and **New game** stays on screen while the list scrolls under
it.

### Legend (`legend.svg`)
Every cell state at real board scale with its name and backing token.

## Cell states

| State | Appearance | Light tokens | Dark tokens |
| --- | --- | --- | --- |
| `EMPTY` | Alternating square, no piece | `boardLight` `#E6EAEF` / `boardDark` `#B9C2CE` | `boardLight` `#2C3A45` / `boardDark` `#1A252E` |
| `PIECE` | Piece glyph on the plain square | `queen` `#12212B` | `queen` `#EAF1F5` |
| `PIECE_CONFLICT` *(border and glow not implemented)* | Tinted tile + 2 px border + glowing glyph | `conflict` `#D93A3A` on `conflictGlow` `#F7C9C9` | `conflict` `#FF6B6B` on `conflictGlow` `#4A1E22` |
| `BLOCKED` *(not implemented)* | Dimmed tile with a center dot, not tappable | `blocked` `#97A2AE` | `blocked` `#55636F` |
| `FIXED` *(not implemented)* | Given queen, muted glyph + solid border, locked | `fixed` `#4A5A69` on `surfaceAlt` `#E9EDF2` | `fixed` `#8FA1AE` on `surfaceAlt` `#1B262F` |
| `HINT` *(not implemented)* | Dashed ring + 22 % tinted fill + ghost queen | `hint` `#54C3CB` | `hint` `#2FBFC8` |

## How the app uses this

The palette is transcribed by hand into `theme/Color.kt`; nothing reads `tokens.json` at build
time, so the two are kept in step by review rather than by a check.

- Material's own roles — primary, surface, background, error — go into the Compose colour
  scheme. The colours Material has no name for travel beside it in `BoardColors`, through a
  `CompositionLocal`: `boardLight`, `boardDark`, `queen`, `conflict`, `conflictGlow`, `hint`,
  `border`, `surfaceAlt`, `onSurfaceMuted`, `success`. `hint` marks the status strip while the
  board is quiet; `success` colours the win card's badge, its "new best" line and the confetti.
  The tokens for states that do not exist — `blocked`, `fixed`, `queenOn` — are not in the code.
  `success` and `hint` also take a third of the confetti each, beside `primary`.
- `typography.scale` maps onto the Material text styles by size, weight and line height.
  `letterSpacing` is not carried across; the one place the app sets it, the Setup title, uses a
  wider value than the token because the design draws the title spaced out.
- `spacing` and `radii` become `Spacing` and `Radii` in `theme/Dimens.kt`, in dp. `Radii` also
  carries an `lg` of 20 dp that the tokens do not define — it is the corner `win.svg` draws on the
  card, and the clear-all dialog uses it too. Of `elevation`, only `high` is transcribed, for the
  win card's shadow; `low` is not in the code.
- `queen.svg` is converted to `res/drawable/ic_queen.xml`, tinted by the caller. The same paths,
  white on the brand teal, make `res/drawable/ic_launcher.xml`.
- Cell state in code is one of three values — `EMPTY`, `PIECE`, `PIECE_CONFLICT`.
