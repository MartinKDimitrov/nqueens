package com.mdimitrov.nqueens.setup.presentation

import com.mdimitrov.nqueens.LARGEST_PLAYABLE_BOARD
import com.mdimitrov.nqueens.domain.MIN_BOARD_SIZE
import com.mdimitrov.nqueens.setup.domain.DEFAULT_BOARD_SIZE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetupViewModelTest {
    @Test
    fun `the board starts at the default size`() {
        assertEquals(DEFAULT_BOARD_SIZE, SetupViewModel().uiState.size)
    }

    @Test
    fun `growing and shrinking move the size by one`() {
        val viewModel = SetupViewModel()
        val start = viewModel.uiState.size

        viewModel.grow()
        assertEquals(start + 1, viewModel.uiState.size)

        viewModel.shrink()
        assertEquals(start, viewModel.uiState.size)
    }

    @Test
    fun `the size cannot shrink below the smallest board on offer`() {
        val viewModel = SetupViewModel()
        repeat(LARGEST_PLAYABLE_BOARD - MIN_BOARD_SIZE + 1) { viewModel.shrink() }

        assertEquals(MIN_BOARD_SIZE, viewModel.uiState.size)
        assertFalse(viewModel.uiState.canShrink)
        assertTrue(viewModel.uiState.canGrow)
    }

    @Test
    fun `the size cannot grow past the largest board offered`() {
        val viewModel = SetupViewModel()
        repeat(LARGEST_PLAYABLE_BOARD - MIN_BOARD_SIZE + 1) { viewModel.grow() }

        assertEquals(LARGEST_PLAYABLE_BOARD, viewModel.uiState.size)
        assertFalse(viewModel.uiState.canGrow)
        assertTrue(viewModel.uiState.canShrink)
    }
}
