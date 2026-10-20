package uz.namozvaqti.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TasbehTest {
    private fun taps(n: Int, target: Int, from: TasbehCounter = TasbehCounter()): TasbehCounter {
        var c = from
        repeat(n) { c = c.tap(target) }
        return c
    }

    @Test fun countsUpToTargetAndClosesRound() {
        val c = taps(33, 33)
        assertEquals(33, c.count)
        assertEquals(1, c.rounds)
        assertEquals(33, c.total)
        assertTrue(c.isClosed(33))
    }

    @Test fun nextTapStartsNewRoundFromOne() {
        val c = taps(34, 33)
        assertEquals(1, c.count)
        assertEquals(1, c.rounds)
        assertEquals(34, c.total)
        assertFalse(c.isClosed(33))
    }

    @Test fun threeRoundsOf33() {
        val c = taps(99, 33)
        assertEquals(33, c.count)
        assertEquals(3, c.rounds)
        assertEquals(99, c.total)
    }

    @Test fun unlimitedNeverCloses() {
        val c = taps(500, 0)
        assertEquals(500, c.count)
        assertEquals(0, c.rounds)
        assertFalse(c.isClosed(0))
    }

    @Test fun undoStepsBackAndReopensClosedRound() {
        val closed = taps(33, 33)
        val back = closed.undo(33)
        assertEquals(32, back.count)
        assertEquals(0, back.rounds)
        assertEquals(32, back.total)
        // oddiy qadam orqaga
        val mid = taps(10, 33).undo(33)
        assertEquals(9, mid.count)
        assertEquals(9, mid.total)
    }

    @Test fun undoAtZeroDoesNothing() {
        assertEquals(TasbehCounter(), TasbehCounter().undo(33))
    }

    @Test fun resetKeepsLifetimeTotal() {
        val c = taps(70, 33).reset()
        assertEquals(0, c.count)
        assertEquals(0, c.rounds)
        assertEquals(70, c.total)
    }

    @Test fun progressWrapsAndIsFullAtTarget() {
        assertEquals(0f, TasbehCounter().progress(33))
        assertEquals(1f, taps(33, 33).progress(33))
        assertEquals(0.5f, taps(50, 100).progress(100))
        // cheksiz rejimda har 33 tada aylanadi
        assertEquals(1f, taps(33, 0).progress(0))
        assertEquals(1f / 33f, taps(34, 0).progress(0), 1e-6f)
    }

    @Test fun presetsAreConsistent() {
        assertEquals(Tasbeh.dhikrs.size, Tasbeh.dhikrs.map { it.key }.toSet().size)
        assertTrue(Tasbeh.dhikrs.all { it.defaultTarget in Tasbeh.targets })
        assertEquals("subhanalloh", Tasbeh.byKey("yo'q").key)
        assertEquals("lailaha", Tasbeh.byKey("lailaha").key)
        assertEquals("∞", Tasbeh.targetLabel(0))
        assertEquals("99", Tasbeh.targetLabel(99))
    }
}
