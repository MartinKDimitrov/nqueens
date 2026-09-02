package com.mdimitrov.puzzles.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public val BoardInset: Dp = 6.dp

public object Spacing {
    public val xs: Dp = 4.dp
    public val sm: Dp = 8.dp
    public val md: Dp = 16.dp
    public val lg: Dp = 24.dp
    public val xl: Dp = 32.dp
}

/**
 * The smallest a control may be to a finger, whatever it is painted at. Not a design token: the
 * accessibility guidelines ask for it and the design does not name it.
 */
public val TouchTarget: Dp = 48.dp

/** The hairline the design draws around every card and panel. */
public val HairlineBorder: Dp = 1.dp

/**
 * The corner radii. `sm` and `md` are `docs/design/tokens.json`; `lg` is not a token — the design
 * draws the win card and the records' own cards rounder than anything the tokens name, and this is
 * where that number lives.
 */
public object Radii {
    public val sm: Dp = 6.dp
    public val md: Dp = 12.dp
    public val lg: Dp = 20.dp
}

/** The elevations from docs/design/tokens.json. */
public object Elevation {
    public val high: Dp = 12.dp
}
