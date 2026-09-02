package com.mdimitrov.puzzles.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
public data class BoardColors(
    public val boardLight: Color,
    public val boardDark: Color,
    public val queen: Color,
    public val conflict: Color,
    public val conflictGlow: Color,
    public val hint: Color,
    public val border: Color,
    public val surfaceAlt: Color,
    public val onSurfaceMuted: Color,
    public val success: Color,
)

public val LocalBoardColors: ProvidableCompositionLocal<BoardColors> =
    staticCompositionLocalOf { error("BoardColors used outside PuzzleTheme") }

public object PuzzleTheme {
    public val board: BoardColors
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
        hint = LightHint,
        success = LightSuccess,
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
        hint = DarkHint,
        success = DarkSuccess,
    )

@Composable
public fun PuzzleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBoardColors provides if (darkTheme) DarkBoard else LightBoard) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = PuzzleTypography,
            content = content,
        )
    }
}
