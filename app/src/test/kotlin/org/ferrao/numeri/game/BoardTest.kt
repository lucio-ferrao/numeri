package org.ferrao.numeri.game

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BoardTest {

    private fun generate(seed: Int): Board = Board(6, 7).apply {
        val random = Random(seed)
        generatePuzzle(count = 15 + random.nextInt(6), hard = true, random = random)
    }

    @Test
    fun solutionIsASingleLoopThatFulfilsEveryNumber() {
        repeat(20) { seed ->
            val board = generate(seed)
            val solution = Board.fromBinary(board.solution!!)
            solution.cells.flatten().forEach { it.hidden = false }
            assertTrue("semente $seed\n$solution", solution.isSolved())
        }
    }

    @Test
    fun puzzleHidesNumbersAndHasExactlyTheGeneratedSolution() {
        repeat(20) { seed ->
            val board = generate(seed)
            assertTrue("semente $seed não esconde números", board.cells.flatten().any { it.hidden })
            val solutions = solveByBruteForce(board)
            assertEquals("semente $seed\n$board", 1, solutions.size)
            assertEquals(Board.fromBinary(board.solution!!).insideCells().map { it.toList() }, solutions[0])
        }
    }

    /**
     * Conta as soluções sem usar o resolvedor do jogo: experimenta todas as escolhas de casas de
     * dentro, cortando quando um número visível deixa de ser possível. Uma solução é um conjunto
     * de casas ligado, sem buracos e sem casas que só se toquem num canto (é o que dá uma única
     * linha fechada).
     */
    private fun solveByBruteForce(board: Board): List<List<List<Boolean>>> {
        val w = board.columns
        val h = board.rows
        val inside = Array(w) { arrayOfNulls<Boolean>(h) }
        fun at(x: Int, y: Int): Boolean? = if (x !in 0 until w || y !in 0 until h) false else inside[x][y]
        fun feasible(x: Int, y: Int): Boolean {
            if (x !in 0 until w || y !in 0 until h) return true
            val cell = board.cells[x][y]
            if (cell.hidden) return true
            var diff = 0
            var open = 0
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val a = at(x, y)
                val b = at(x + dx, y + dy)
                if (a == null || b == null) open++ else if (a != b) diff++
            }
            return cell.number in diff..diff + open
        }
        fun connected(target: Boolean): Boolean {
            // Com target = false junta-se a moldura exterior, para apanhar buracos.
            val cells = (-1..w).flatMap { x -> (-1..h).map { y -> x to y } }
                .filter { (x, y) -> if (x !in 0 until w || y !in 0 until h) !target else inside[x][y] == target }
            if (cells.isEmpty()) return !target
            val set = cells.toHashSet()
            val seen = hashSetOf(cells[0])
            val queue = ArrayDeque(listOf(cells[0]))
            while (queue.isNotEmpty()) {
                val (x, y) = queue.removeFirst()
                for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val n = (x + dx) to (y + dy)
                    if (n in set && seen.add(n)) queue += n
                }
            }
            return seen.size == set.size
        }
        fun touchesOnlyAtCorner(): Boolean = (0..w).any { x ->
            (0..h).any { y ->
                val a = at(x - 1, y - 1)!!
                val b = at(x, y - 1)!!
                val c = at(x - 1, y)!!
                val d = at(x, y)!!
                a == d && b == c && a != b
            }
        }
        val solutions = mutableListOf<List<List<Boolean>>>()
        fun search(i: Int) {
            if (solutions.size > 1) return
            if (i == w * h) {
                if (connected(true) && connected(false) && !touchesOnlyAtCorner()) {
                    solutions += inside.map { column -> column.map { it!! } }
                }
                return
            }
            val x = i % w
            val y = i / w
            for (value in listOf(false, true)) {
                inside[x][y] = value
                if (feasible(x, y) && feasible(x - 1, y) && feasible(x + 1, y) && feasible(x, y - 1) && feasible(x, y + 1)) {
                    search(i + 1)
                }
            }
            inside[x][y] = null
        }
        search(0)
        return solutions
    }

    @Test
    fun replayingTheHintListSolvesTheBoard() {
        val board = generate(7)
        board.clear()
        assertFalse(board.isSolved())
        board.numberHintList().forEach { it.switchAll() }
        assertTrue(board.isSolved())
        assertTrue(board.numberHintList().isEmpty())
    }

    @Test
    fun binaryRoundTrip() {
        val board = generate(3)
        board.cells[1][2].switchAll()
        assertArrayEquals(board.toBinary(), Board.fromBinary(board.toBinary()).toBinary())
    }
}
