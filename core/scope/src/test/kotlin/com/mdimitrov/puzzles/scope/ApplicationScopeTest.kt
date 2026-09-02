package com.mdimitrov.puzzles.scope

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Enough handovers that a pool of threads cannot finish them in order by chance. */
private const val HANDOVERS = 1000

class ApplicationScopeTest {
    @Test
    fun `work of one shape is begun in the order it was handed over`() {
        // Two presses of the palette button in one second are two writes of the same preference,
        // and the one the player pressed last is the one they expect to keep. On a pool of threads
        // the second can overtake the first and the app remembers the palette nobody chose. The
        // handovers here are all one shape, which is what the button's are; the scope makes no
        // promise about two of different shapes, and its KDoc says which case that leaves open.
        val scope = ApplicationScopeModule.scope()
        val order = Collections.synchronizedList(mutableListOf<Int>())

        val handed =
            (1..HANDOVERS).map { n ->
                scope.launch {
                    yield()
                    order += n
                }
            }

        runBlocking { handed.joinAll() }
        assertEquals((1..HANDOVERS).toList(), order)
    }

    @Test
    fun `the scope does not hand its work to the shared pool`() {
        // The test above distinguishes one thread from many by racing them, which a machine with
        // one core cannot do: there, the unrestricted pool would pass it too and the guard would
        // quietly stop guarding. This asks the structural question instead, and holds on any
        // machine.
        assertNotEquals(
            Dispatchers.Default,
            ApplicationScopeModule.scope().coroutineContext[CoroutineDispatcher],
            "work handed over here goes to the pool every other coroutine in the process shares",
        )
    }
}
