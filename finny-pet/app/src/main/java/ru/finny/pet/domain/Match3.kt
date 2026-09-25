package ru.finny.pet.domain

import kotlin.random.Random

/** Tile kinds mirror the game's money vocabulary: coins, mandatory (apple), optional (gift), savings (piggy), bonus star. */
enum class Tile { COIN, APPLE, GIFT, PIGGY, STAR }

data class Cell(val col: Int, val row: Int)

/** One animation step the UI plays back in order. */
sealed interface Step {
    data class Swap(val a: Cell, val b: Cell) : Step
    data class Match(val cells: Set<Cell>, val points: Int) : Step
    data class Fall(val moves: Map<Cell, Cell>) : Step             // from → to (existing tiles falling down)
    data class Refill(val cells: Map<Cell, Tile>) : Step           // new tiles entering from the top
}

data class Match3State(
    val width: Int,
    val height: Int,
    val tiles: List<Tile?>,          // row-major, index = row * width + col; null only transiently
    val score: Int,
    val movesLeft: Int,
    val bombs: Int,
    val seed: Long,
) {
    operator fun get(c: Cell): Tile? = tiles[c.row * width + c.col]
    val finished: Boolean get() = movesLeft <= 0
}

data class Turn(val state: Match3State, val steps: List<Step>)

/**
 * Deterministic match-3 rules, no Android. A board never starts with a match; a swap must produce a match
 * or it is rejected; matches cascade with a growing combo multiplier; a bomb clears a 3×3 square.
 * Scoring: 3 tiles = 10, each extra tile +10; cascade n multiplies by n.
 */
object Match3 {
    fun newGame(width: Int = 7, height: Int = 6, moves: Int = 15, bombs: Int = 0, seed: Long): Match3State {
        val rnd = Random(seed)
        val tiles = ArrayList<Tile?>(width * height)
        for (row in 0 until height) for (col in 0 until width) {
            var t: Tile
            do { t = Tile.entries[rnd.nextInt(Tile.entries.size)] } while (
                (col >= 2 && tiles[row * width + col - 1] == t && tiles[row * width + col - 2] == t) ||
                (row >= 2 && tiles[(row - 1) * width + col] == t && tiles[(row - 2) * width + col] == t)
            )
            tiles += t
        }
        return Match3State(width, height, tiles, 0, moves, bombs, rnd.nextLong())
    }

    fun adjacent(a: Cell, b: Cell): Boolean = (a.col == b.col && kotlin.math.abs(a.row - b.row) == 1) || (a.row == b.row && kotlin.math.abs(a.col - b.col) == 1)

    /** @return null when the swap is not adjacent or produces no match (the UI wiggles the tiles back). */
    fun swap(s: Match3State, a: Cell, b: Cell): Turn? {
        if (s.finished || !adjacent(a, b) || !inside(s, a) || !inside(s, b)) return null
        val swapped = s.tiles.toMutableList()
        val ia = a.row * s.width + a.col; val ib = b.row * s.width + b.col
        swapped[ia] = s.tiles[ib]; swapped[ib] = s.tiles[ia]
        val after = s.copy(tiles = swapped)
        if (matches(after).isEmpty()) return null
        val steps = mutableListOf<Step>(Step.Swap(a, b))
        val resolved = resolve(after.copy(movesLeft = s.movesLeft - 1), steps)
        return Turn(resolved, steps)
    }

    /** Explodes a 3×3 square around [at]; costs one bomb, not a move. */
    fun bomb(s: Match3State, at: Cell): Turn? {
        if (s.finished || s.bombs <= 0 || !inside(s, at)) return null
        val cells = buildSet {
            for (dr in -1..1) for (dc in -1..1) {
                val c = Cell(at.col + dc, at.row + dr); if (inside(s, c)) add(c)
            }
        }
        val steps = mutableListOf<Step>()
        val points = 10 * cells.size
        steps += Step.Match(cells, points)
        val cleared = clear(s.copy(bombs = s.bombs - 1, score = s.score + points), cells)
        val resolved = resolve(cleared, steps, combo = 2, dropFirst = true)
        return Turn(resolved, steps)
    }

    fun hasMove(s: Match3State): Boolean {
        for (row in 0 until s.height) for (col in 0 until s.width) {
            val c = Cell(col, row)
            for (n in listOf(Cell(col + 1, row), Cell(col, row + 1))) {
                if (!inside(s, n)) continue
                val t = s.tiles.toMutableList()
                val i = c.row * s.width + c.col; val j = n.row * s.width + n.col
                t[i] = s.tiles[j]; t[j] = s.tiles[i]
                if (matches(s.copy(tiles = t)).isNotEmpty()) return true
            }
        }
        return false
    }

    /** Reshuffles when no move exists (keeps score/moves); the UI shows "перемешиваю". */
    fun reshuffle(s: Match3State): Match3State {
        var cur = s
        var seed = s.seed
        repeat(50) {
            val fresh = newGame(s.width, s.height, s.movesLeft, s.bombs, seed)
            cur = fresh.copy(score = s.score)
            if (hasMove(cur)) return cur
            seed = fresh.seed
        }
        return cur
    }

    // ---- internals

    private fun inside(s: Match3State, c: Cell) = c.col in 0 until s.width && c.row in 0 until s.height

    /** All cells that are part of a horizontal or vertical run of ≥ 3 equal tiles, grouped per run. */
    fun matches(s: Match3State): List<Set<Cell>> {
        val runs = mutableListOf<Set<Cell>>()
        for (row in 0 until s.height) {
            var start = 0
            for (col in 1..s.width) {
                if (col == s.width || s[Cell(col, row)] != s[Cell(start, row)] || s[Cell(start, row)] == null) {
                    if (col - start >= 3) runs += (start until col).map { Cell(it, row) }.toSet()
                    start = col
                }
            }
        }
        for (col in 0 until s.width) {
            var start = 0
            for (row in 1..s.height) {
                if (row == s.height || s[Cell(col, row)] != s[Cell(col, start)] || s[Cell(col, start)] == null) {
                    if (row - start >= 3) runs += (start until row).map { Cell(col, it) }.toSet()
                    start = row
                }
            }
        }
        return runs
    }

    private fun clear(s: Match3State, cells: Set<Cell>): Match3State {
        val t = s.tiles.toMutableList()
        cells.forEach { t[it.row * s.width + it.col] = null }
        return s.copy(tiles = t)
    }

    private fun resolve(start: Match3State, steps: MutableList<Step>, combo: Int = 1, dropFirst: Boolean = false): Match3State {
        var s = start
        var mult = combo
        var first = dropFirst
        while (true) {
            if (!first) {
                val runs = matches(s)
                if (runs.isEmpty()) break
                val cells = runs.flatten().toSet()
                val points = runs.sumOf { 10 + (it.size - 3) * 10 } * mult
                steps += Step.Match(cells, points)
                s = clear(s.copy(score = s.score + points), cells)
                mult++
            }
            first = false
            // gravity
            val t = s.tiles.toMutableList()
            val falls = mutableMapOf<Cell, Cell>()
            for (col in 0 until s.width) {
                var write = s.height - 1
                for (row in s.height - 1 downTo 0) {
                    val tile = t[row * s.width + col] ?: continue
                    if (write != row) {
                        t[write * s.width + col] = tile; t[row * s.width + col] = null
                        falls[Cell(col, row)] = Cell(col, write)
                    }
                    write--
                }
            }
            if (falls.isNotEmpty()) steps += Step.Fall(falls)
            // refill
            val rnd = Random(s.seed)
            val fresh = mutableMapOf<Cell, Tile>()
            for (row in 0 until s.height) for (col in 0 until s.width) {
                if (t[row * s.width + col] == null) {
                    val tile = Tile.entries[rnd.nextInt(Tile.entries.size)]
                    t[row * s.width + col] = tile; fresh[Cell(col, row)] = tile
                }
            }
            if (fresh.isNotEmpty()) steps += Step.Refill(fresh)
            s = s.copy(tiles = t, seed = rnd.nextLong())
        }
        return if (!s.finished && !hasMove(s)) reshuffle(s) else s
    }
}
