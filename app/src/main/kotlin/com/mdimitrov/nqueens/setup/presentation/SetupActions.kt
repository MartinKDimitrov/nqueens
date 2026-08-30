package com.mdimitrov.nqueens.setup.presentation

/** Everything the Setup screen asks of the app around it. */
internal data class SetupActions(
    val onShrink: () -> Unit,
    val onGrow: () -> Unit,
    val onStart: (Int) -> Unit,
    val onScores: () -> Unit,
)
