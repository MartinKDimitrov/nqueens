package com.mdimitrov.nqueens.puzzle

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.mdimitrov.nqueens.R
import com.mdimitrov.nqueens.domain.LineRules
import com.mdimitrov.nqueens.domain.MAX_BOARD_SIZE
import com.mdimitrov.nqueens.domain.NQueensLines

/**
 * The largest board this app will play. Above it the squares get too small to tap reliably and
 * the grid too large to draw comfortably.
 *
 * Clamped by the domain's own ceiling: a size the domain refuses would reach `GameState` through
 * the route guard and throw during composition, so the two are linked here rather than by hand.
 */
internal val LARGEST_PLAYABLE_BOARD: Int = minOf(12, MAX_BOARD_SIZE)

internal data class Variant(
    /**
     * How a record names this puzzle. It is the feature's own word and never a resource id:
     * ids are assigned when the resource table is built and move whenever a string is added or
     * removed, so a row written by one build would name a different puzzle in the next
     * (TRADEOFFS D14).
     */
    val key: String,
    @StringRes val name: Int,
    @DrawableRes val piece: Int,
    val rules: LineRules,
    val text: VariantText,
)

/** Everything the screens say about the piece this puzzle is played with. Resource ids. */
internal data class VariantText(
    @StringRes val subtitle: Int,
    @StringRes val counter: Int,
    @StringRes val cell: Int,
    @StringRes val cellUnderAttack: Int,
    @StringRes val idle: Int,
    @PluralsRes val conflict: Int,
)

internal val Queens: Variant =
    Variant(
        key = "queens",
        name = R.string.variant_queens,
        piece = R.drawable.ic_queen,
        rules = NQueensLines,
        text =
            VariantText(
                subtitle = R.string.variant_queens_subtitle,
                counter = R.string.variant_queens_counter,
                cell = R.string.variant_queens_cell,
                cellUnderAttack = R.string.variant_queens_cell_attacked,
                idle = R.string.variant_queens_idle,
                conflict = R.plurals.variant_queens_conflict,
            ),
    )
