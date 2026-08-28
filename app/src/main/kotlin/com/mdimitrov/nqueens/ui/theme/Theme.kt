package com.mdimitrov.nqueens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
internal data class BoardColors(
    val boardLight: Color,
    val boardDark: Color,
    val queen: Color,
    val conflict: Color,
    val conflictGlow: Color,
    val border: Color,
    val surfaceAlt: Color,
    val onSurfaceMuted: Color,
)

internal val LocalBoardColors =
    staticCompositionLocalOf<BoardColors> { error("BoardColors used outside NqueensTheme") }

internal object NqueensTheme {
    val board: BoardColors
        @Composable get() = LocalBoardColors.current
}

private val LightScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        error = LightConflict,
    )

private val DarkScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        error = DarkConflict,
    )

private val LightBoard =
    BoardColors(
        boardLight = LightBoardLight,
        boardDark = LightBoardDark,
        queen = LightQueen,
        conflict = LightConflict,
        conflictGlow = LightConflictGlow,
        border = LightBorder,
        surfaceAlt = LightSurfaceAlt,
        onSurfaceMuted = LightOnSurfaceMuted,
    )

private val DarkBoard =
    BoardColors(
        boardLight = DarkBoardLight,
        boardDark = DarkBoardDark,
        queen = DarkQueen,
        conflict = DarkConflict,
        conflictGlow = DarkConflictGlow,
        border = DarkBorder,
        surfaceAlt = DarkSurfaceAlt,
        onSurfaceMuted = DarkOnSurfaceMuted,
    )

@Composable
internal fun NqueensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBoardColors provides if (darkTheme) DarkBoard else LightBoard) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = NqueensTypography,
            content = content,
        )
    }
}
