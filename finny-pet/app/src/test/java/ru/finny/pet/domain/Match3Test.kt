package ru.finny.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Match3Test {
    @Test
    fun `new board has no matches and a possible move`() {
        repeat(20) { seed ->
            val s = Match3.newGame(seed = seed.toLong())
            assertTrue(Match3.matches(s).isEmpty())
            assertTrue(Match3.hasMove(s))
            assertEquals(s.width * s.height, s.tiles.size)
        }
    }

    @Test
    fun `swap without a match is rejected and costs nothing`() {
        val s = Match3.newGame(seed = 1)
        // find a swap that does not match
        var rejected = 0
        for (row in 0 until s.height) for (col in 0 until s.width - 1) {
            if (Match3.swap(s, Cell(col, row), Cell(col + 1, row)) == null) rejected++
        }
        assertTrue(rejected > 0)
        assertNull(Match3.swap(s, Cell(0, 0), Cell(2, 0))) // not adjacent
    }

    @Test
    fun `a matching swap scores, costs a move and refills the board`() {
        val s = Match3.newGame(seed = 1)
        var turn: Turn? = null
        loop@ for (row in 0 until s.height) for (col in 0 until s.width) {
            for (n in listOf(Cell(col + 1, row), Cell(col, row + 1))) {
                turn = Match3.swap(s, Cell(col, row), n) ?: continue
                break@loop
            }
        }
        val t = checkNotNull(turn)
        assertEquals(s.movesLeft - 1, t.state.movesLeft)
        assertTrue(t.state.score >= 10)
        assertTrue(t.steps.first() is Step.Swap)
        assertTrue(t.steps.any { it is Step.Match })
        assertTrue(t.steps.any { it is Step.Refill })
        assertTrue(t.state.tiles.none { it == null })
        assertTrue(Match3.matches(t.state).isEmpty())
    }

    @Test
    fun `bomb clears a 3x3 square and needs a bomb`() {
        val s = Match3.newGame(seed = 3, bombs = 1)
        val t = checkNotNull(Match3.bomb(s, Cell(3, 3)))
        val cleared = (t.steps.first() as Step.Match).cells
        assertEquals(9, cleared.size)
        assertEquals(0, t.state.bombs)
        assertEquals(s.movesLeft, t.state.movesLeft)
        assertTrue(t.state.score >= 90)
        assertNull(Match3.bomb(t.state, Cell(3, 3)))
        assertEquals(4, (checkNotNull(Match3.bomb(s, Cell(0, 0))).steps.first() as Step.Match).cells.size)
    }

    @Test
    fun `game ends when moves run out`() {
        var s = Match3.newGame(seed = 7, moves = 2)
        repeat(2) {
            var turn: Turn? = null
            loop@ for (row in 0 until s.height) for (col in 0 until s.width) {
                for (n in listOf(Cell(col + 1, row), Cell(col, row + 1))) {
                    turn = Match3.swap(s, Cell(col, row), n) ?: continue
                    break@loop
                }
            }
            s = checkNotNull(turn).state
        }
        assertTrue(s.finished)
        assertFalse(Match3.swap(s, Cell(0, 0), Cell(1, 0)) != null)
    }

    @Test
    fun `same seed gives the same board`() {
        assertEquals(Match3.newGame(seed = 42).tiles, Match3.newGame(seed = 42).tiles)
    }
}
