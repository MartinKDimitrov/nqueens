# N-Queens — Mobile UI Design Reference

Portrait-first (390 × 844) visual reference for an Android N-Queens puzzle game.
All colors, type sizes, spacing, radii and elevation come from
[`tokens.json`](./tokens.json) — it is the single source of truth. The SVGs in
`screens/` and the live React mockups in the preview are both rendered from
those values; never introduce a color that is not in `tokens.json`.

- Icon: [`queen.svg`](./queen.svg) — single self-contained icon, `fill="currentColor"`,
  legible down to ~14 px (12×12 board on a 360 dp screen).
- Screens: `screens/setup.svg`, `game.svg`, `game-conflicts.svg`, `win.svg`,
  `scores.svg`, `legend.svg` (light theme; swap each hex for its `colors.dark`
  counterpart for the dark theme).

## Design principles

- Chess-inspired, not skeuomorphic: two low-chroma neutral squares, no wood.
- Board sizes 4×4 – 12×12 share one layout; the board is always a square that
  fills the content width minus `spacing.md` gutters, cells are `size / N`.
- State is never signalled by color alone: conflict adds an outlined tile and a
  glow, hint adds a dashed ring, blocked adds a center dot, fixed adds a border.
- Text and state colors meet WCAG AA against their backgrounds in both themes.

## Screens

### Setup (`setup.svg`)
Title block, a live 8×8 preview of the chosen size, a board-size stepper
(`−` / `N × N` / `+`, min 4, default 8, max 12) and a variant dropdown
(Queens, Rooks, Bishops, Amazons). A full-width `primary` **Start** button sits
above the safe area, with the current best time for the selected size below it.

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
| `BLOCKED` | Dimmed tile with a center dot, not tappable | `blocked` `#97A2AE` | `blocked` `#55636F` |
| `FIXED` | Given queen, muted glyph + solid border, locked | `fixed` `#4A5A69` on `surfaceAlt` `#E9EDF2` | `fixed` `#8FA1AE` on `surfaceAlt` `#1B262F` |
| `HINT` | Dashed ring + 22 % tinted fill + ghost queen | `hint` `#54C3CB` | `hint` `#2FBFC8` |

## Implementation notes (Jetpack Compose)

- Map `colors.light` / `colors.dark` onto a custom `NQueensColors` immutable
  class provided through a `CompositionLocal`; the names match 1:1.
- `typography.scale` maps to `TextStyle(fontSize, fontWeight, lineHeight,
  letterSpacing)` in sp; `spacing`, `radii` and `elevation` are dp.
- Board: `LazyVerticalGrid`/`Layout` with `GridCells.Fixed(n)` and
  `aspectRatio(1f)` cells; state per cell is one of the six enum values above.
- The queen path in `queen.svg` fits a 24×24 viewport — reuse it as an
  `ImageVector` tinted by state.
