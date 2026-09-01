package com.mdimitrov.nqueens.theme

import androidx.compose.ui.unit.dp

internal val BoardInset = 6.dp

internal object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/**
 * The smallest a control may be to a finger, whatever it is painted at. Not a design token: the
 * accessibility guidelines ask for it and the design does not name it.
 */
internal val TouchTarget = 48.dp

/** The hairline the design draws around every card and panel. */
internal val HairlineBorder = 1.dp

/** The corner radii from design/tokens.json. */
internal object Radii {
    val sm = 6.dp
    val md = 12.dp
    val lg = 20.dp
}

/** The elevations from design/tokens.json. */
internal object Elevation {
    val high = 12.dp
}
