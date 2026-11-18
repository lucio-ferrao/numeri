package org.ferrao.numeri.game

import kotlin.random.Random

/**
 * Tabuleiro com [maxX] x [maxY] pontos, ou seja (maxX-1) x (maxY-1) casas.
 * Tradução direta de CBoard.m do jogo original para iPhone.
 */
class Board(val maxX: Int, val maxY: Int) {

    val outside = Cell(-1, -1, emptyList())
    val dots: Array<Array<Dot>> = Array(maxX) { x -> Array(maxY) { y -> Dot(x, y) } }
    val cells: Array<Array<Cell>>

    /** Segmentos que podem mudar (os da borda exterior ficam sempre por marcar). */
    val connectors = ArrayList<Connector>()

    /** Tabuleiro resolvido, no formato de [toBinary]. */
    var solution: ByteArray? = null

    val columns get() = maxX - 1
    val rows get() = maxY - 1

    init {
        for (x in 0 until maxX) {
            for (y in 0 until maxY) {
                val dot = dots[x][y]
                dot.right = if (x + 1 == maxX) Connector(Status.UNSET)
                else Connector(Status.UNKNOWN).also { connectors += it }
                dot.down = if (y + 1 == maxY) Connector(Status.UNSET)
                else Connector(Status.UNKNOWN).also { connectors += it }
                dot.left = if (x == 0) Connector(Status.UNSET)
                else dots[x - 1][y].right.also { it.dot1 = dot; it.dot2 = dots[x - 1][y] }
                dot.up = if (y == 0) Connector(Status.UNSET)
                else dots[x][y - 1].down.also { it.dot1 = dot; it.dot2 = dots[x][y - 1] }
            }
        }
        cells = Array(maxX - 1) { x ->
            Array(maxY - 1) { y ->
                Cell(
                    x, y,
                    listOf(dots[x + 1][y].down, dots[x][y].down, dots[x][y].right, dots[x][y + 1].right),
                )
            }
        }
        for (x in 0 until maxX) {
            for (y in 0 until maxY - 1) {
                dots[x][y].down.cell1 = if (x > 0) cells[x - 1][y] else outside
                dots[x][y].down.cell2 = if (x + 1 < maxX) cells[x][y] else outside
            }
        }
        for (x in 0 until maxX - 1) {
            for (y in 0 until maxY) {
                dots[x][y].right.cell1 = if (y > 0) cells[x][y - 1] else outside
                dots[x][y].right.cell2 = if (y + 1 < maxY) cells[x][y] else outside
            }
        }
    }

    private inline fun forEachCell(action: (Cell) -> Unit) {
        for (column in cells) for (cell in column) action(cell)
    }

    private inline fun forEachDot(action: (Dot) -> Unit) {
        for (column in dots) for (dot in column) action(dot)
    }

    /** Casa da grelha marcada com 3 durante a geração; fora da grelha conta como a casa exterior. */
    private fun increasedNumber(x: Int, y: Int): Int =
        if (x < 0 || y < 0 || x + 1 >= maxX || y + 1 >= maxY) outside.number else cells[x][y].number

    private fun countHidden(): Int {
        var count = 0
        forEachCell { if (it.hidden) count++ }
        return count
    }

    fun generatePuzzle(count: Int, hard: Boolean, random: Random = Random.Default) {
        val around = arrayOf(
            -1 to -1, 0 to -1, 1 to -1, 1 to 0, 1 to 1, 0 to 1, -1 to 1, -1 to 0, -1 to -1,
        )
        var remaining = count
        cells[random.nextInt(maxX - 1)][random.nextInt(maxY - 1)].number = 3

        var tries = maxX * maxY * 20
        while (remaining > 0 && tries-- > 0) {
            val posX = random.nextInt(maxX - 1)
            val posY = random.nextInt(maxY - 1)
            if (cells[posX][posY].number == 3) continue
            // Só cresce a partir de uma casa já escolhida.
            if (increasedNumber(posX - 1, posY) != 3 && increasedNumber(posX + 1, posY) != 3 &&
                increasedNumber(posX, posY - 1) != 3 && increasedNumber(posX, posY + 1) != 3
            ) continue

            var prev = increasedNumber(posX + around[0].first, posY + around[0].second)
            var borderCount = 0
            var fillCount = 0
            var valid = true
            for ((dx, dy) in around) {
                val n = increasedNumber(posX + dx, posY + dy)
                if (n == 3) {
                    fillCount++
                    if (fillCount > 4 || (fillCount == 3 && random.nextBoolean())) {
                        valid = false // Evita encher a zona toda.
                        break
                    }
                }
                if (n != prev) {
                    borderCount++
                    if (borderCount > 2) {
                        valid = false // Tocou noutra parte: evita ciclos.
                        break
                    }
                }
                prev = n
            }
            if (valid) {
                cells[posX][posY].number = 3
                remaining--
            }
        }
        for (x in 0 until maxX) {
            for (y in 0 until maxY) {
                dots[x][y].down.status =
                    if (increasedNumber(x - 1, y) != increasedNumber(x, y)) Status.SET else Status.UNSET
                dots[x][y].right.status =
                    if (increasedNumber(x, y - 1) != increasedNumber(x, y)) Status.SET else Status.UNSET
            }
        }
        forEachCell {
            it.number = it.count(Status.SET)
            it.hidden = false
        }

        val solution = toBinary()
        this.solution = solution

        connectors.forEach { it.status = Status.UNKNOWN }
        reduce(hard, verticalSymmetry = false, random)
        val hidden1 = countHidden()
        val board1 = toBinary()

        loadBinary(solution)
        connectors.forEach { it.status = Status.UNKNOWN }
        reduce(hard, verticalSymmetry = true, random)
        if (countHidden() < hidden1) loadBinary(board1)
    }

    fun toBinary(): ByteArray {
        val buffer = ByteArray(2 + (maxX - 1) * (maxY - 1) + 2 * maxX * maxY)
        var pos = 0
        buffer[pos++] = maxX.toByte()
        buffer[pos++] = maxY.toByte()
        for (x in 0 until maxX - 1) for (y in 0 until maxY - 1) {
            val cell = cells[x][y]
            buffer[pos++] = (if (cell.hidden) 4 + cell.number else cell.number).toByte()
        }
        for (x in 0 until maxX) for (y in 0 until maxY) buffer[pos++] = dots[x][y].right.status.code.toByte()
        for (x in 0 until maxX) for (y in 0 until maxY) buffer[pos++] = dots[x][y].down.status.code.toByte()
        return buffer
    }

    /** Carrega o estado de [toBinary]; devolve true se algum segmento mudou. */
    fun loadBinary(
        data: ByteArray,
        clearUnknown: Boolean = false,
        changed: MutableList<Connector>? = null,
        ignoreUnknown: Boolean = false,
    ): Boolean {
        var pos = 2
        var anyChange = false
        for (x in 0 until maxX - 1) for (y in 0 until maxY - 1) {
            val n = data[pos++].toInt()
            cells[x][y].hidden = n > 3
            cells[x][y].number = if (n > 3) n - 4 else n
        }
        fun load(c: Connector) {
            val s = Status.of(data[pos++].toInt())
            val differs = if (!ignoreUnknown) c.status != s else (c.status == Status.SET) != (s == Status.SET)
            if (differs) {
                c.status = s
                changed?.add(c)
                anyChange = true
            }
            if (clearUnknown && c.status == Status.UNKNOWN) c.status = Status.UNSET
        }
        for (x in 0 until maxX) for (y in 0 until maxY) load(dots[x][y].right)
        for (x in 0 until maxX) for (y in 0 until maxY) load(dots[x][y].down)
        return anyChange
    }

    fun valid(): Boolean {
        forEachCell { if (!it.valid()) return false }
        forEachDot { if (!it.valid()) return false }
        return true
    }

    fun count(s: Status): Int = connectors.count { it.status == s }

    /** Segue a linha a partir de [start]; devolve o comprimento se fechar, ou null. */
    private fun cycleLength(start: Connector): Int? {
        var current = start
        var lastDot: Dot? = null
        var length = 0
        while (true) {
            val nextDot = (if (current.dot1 === lastDot) current.dot2 else current.dot1)!!
            if (!nextDot.valid()) return null
            if (nextDot.count(Status.SET) == 1) return null
            val from = current
            current = when {
                nextDot.up.status == Status.SET && nextDot.up !== from -> nextDot.up
                nextDot.left.status == Status.SET && nextDot.left !== from -> nextDot.left
                nextDot.right.status == Status.SET && nextDot.right !== from -> nextDot.right
                nextDot.down.status == Status.SET && nextDot.down !== from -> nextDot.down
                else -> from
            }
            length++
            if (current === start) return length
            lastDot = nextDot
        }
    }

    private fun anySetConnector(): Connector? = connectors.firstOrNull { it.status == Status.SET }

    fun isSolved(): Boolean {
        val start = anySetConnector() ?: return false
        val length = cycleLength(start) ?: return false
        if (length != count(Status.SET)) return false
        forEachCell { if (!it.fulfilled()) return false }
        return true
    }

    /** Volta a pôr todos os segmentos por marcar (o "limpar" do modo de casas). */
    fun clear() = connectors.forEach { it.status = Status.UNSET }

    /**
     * Casas a trocar para chegar da jogada atual à solução, por uma ordem que encadeia
     * casas vizinhas (é a animação do botão ">").
     */
    fun numberHintList(): List<Cell> {
        val list = ArrayList<Cell>()
        if (isSolved()) return list
        val solutionData = solution ?: return list
        val sol = fromBinary(solutionData)
        for (y in 0 until maxY - 1) {
            var inside1 = false
            var inside2 = false
            for (x in 0 until maxX - 1) {
                if (cells[x][y].left.status == Status.SET) inside1 = !inside1
                if (sol.cells[x][y].left.status == Status.SET) inside2 = !inside2
                if (inside1 != inside2) list += cells[x][y]
            }
        }
        for (i in 0 until list.size - 1) {
            for (j in i + 1 until list.size) {
                val ci = list[i]
                val cj = list[j]
                if ((ci.x + 1 == cj.x && ci.y == cj.y) || (ci.x == cj.x && ci.y + 1 == cj.y)) {
                    list.removeAt(j)
                    list.add(i + 1, cj)
                    break
                }
            }
        }
        return list
    }

    /** Casas dentro da linha, pela paridade dos segmentos verticais à esquerda. */
    fun insideCells(): Array<BooleanArray> = Array(maxX - 1) { BooleanArray(maxY - 1) }.also { inside ->
        for (y in 0 until maxY - 1) {
            var isInside = false
            for (x in 0 until maxX - 1) {
                if (cells[x][y].left.status == Status.SET) isInside = !isInside
                inside[x][y] = isInside
            }
        }
    }

    fun simplify(
        guesses: Int,
        singleStep: Boolean,
        isTop: Boolean,
        changed: MutableList<Connector>?,
        random: Random = Random.Default,
    ): Result {
        var r = simplify(changed)
        if (guesses <= 0 || r == Result.INVALID || r == Result.TERMINAL) return r

        val savedBoard = toBinary()
        var anyChange = false
        val whatIf = ArrayList<Connector>()
        for (c in connectors) {
            if (c.status != Status.UNKNOWN) continue
            // Na primeira vez testa todos; depois só os vizinhos dos que mudaram
            // (no original a lista chega vazia às chamadas internas, por isso estas não testam nenhum).
            if (isTop || changed.orEmpty().any { c.isNear(it) }) {
                whatIf.add(random.nextInt(whatIf.size + 1), c)
            }
        }

        var terminalCount = 0
        for (c in whatIf) {
            if (c.status != Status.UNKNOWN) continue

            c.status = Status.UNSET
            r = simplify(guesses - 1, false, false, null, random)
            val unsetResult = toBinary()
            if (r == Result.TERMINAL && ++terminalCount > 1) return Result.TWO_SOLUTIONS
            if (r == Result.TWO_SOLUTIONS) return r
            if (r == Result.INVALID) {
                loadBinary(savedBoard)
                if (c.setStatus(Status.SET, changed) == Result.INVALID) return Result.INVALID
                anyChange = true
                break
            }
            loadBinary(savedBoard)

            c.status = Status.SET
            r = simplify(guesses - 1, false, false, null, random)
            if (r == Result.TERMINAL && ++terminalCount > 1) return Result.TWO_SOLUTIONS
            if (r == Result.TWO_SOLUTIONS) return r
            if (r == Result.INVALID) {
                loadBinary(savedBoard)
                if (c.setStatus(Status.UNSET, changed) == Result.INVALID) return Result.INVALID
                anyChange = true
                break
            }
            val setResult = toBinary()
            val merged = mergeBinaries(unsetResult, setResult)
            loadBinary(savedBoard)
            if (loadBinary(merged, changed = changed)) {
                anyChange = true
                break
            }
        }

        return when {
            !anyChange -> Result.INCOMPLETE
            singleStep -> Result.CHANGED
            else -> simplify(guesses, false, isTop, changed, random)
        }
    }

    fun simplify(changed: MutableList<Connector>?): Result {
        outside.parent = null
        var status = Result.TERMINAL
        fun accumulate(r: Result): Boolean {
            when (r) {
                Result.INVALID -> return false
                Result.INCOMPLETE -> if (status == Result.TERMINAL) status = Result.INCOMPLETE
                Result.CHANGED -> status = Result.CHANGED
                else -> {}
            }
            return true
        }
        forEachCell {
            it.parent = null
            if (!accumulate(it.simplify(changed))) return Result.INVALID
        }
        forEachDot {
            it.parent = null
            if (!accumulate(it.simplify(changed))) return Result.INVALID
        }

        do {
            val changeCount = changed?.size ?: 0
            for (c in connectors) {
                if (!mergeCells(c, changed)) return Result.INVALID
            }
        } while (changeCount != (changed?.size ?: 0))

        // Procura linhas fechadas.
        var loopLength = 0
        for (c in connectors) {
            if (c.status != Status.SET) continue
            loopLength++
            if (!mergeDots(c.dot1!!, c.dot2!!)) {
                // Fechou-se uma linha: valida a solução.
                if (status != Result.TERMINAL && !valid()) return Result.INVALID
                return if (loopLength == count(Status.SET)) Result.TERMINAL else Result.INVALID
            }
        }
        return status
    }

    private fun reduce(hard: Boolean, verticalSymmetry: Boolean, random: Random) {
        fun mirror(cell: Cell) = cells[maxX - 2 - cell.x][if (verticalSymmetry) cell.y else maxY - 2 - cell.y]

        val positions = ArrayList<Cell>()
        for (x in 0 until maxX / 2) {
            for (y in 0 until maxY - 1) {
                val cell = cells[x][y]
                if (cell.number == 0 || mirror(cell).number == 0 || positions.isEmpty()) {
                    positions.add(0, cell)
                } else {
                    positions.add(1 + random.nextInt(positions.size), cell)
                }
            }
        }

        val depth = if (hard) 7 else 4
        var savedBoard = toBinary()
        while (positions.isNotEmpty()) {
            val cell = positions[0]
            cell.hidden = true
            mirror(cell).hidden = true
            val r = simplify(depth, singleStep = false, isTop = true, changed = null, random = random)
            if (r == Result.TERMINAL) {
                loadBinary(savedBoard)
                cell.hidden = true
                mirror(cell).hidden = true
                savedBoard = toBinary()
            }
            positions.removeAt(0)
            loadBinary(savedBoard)
        }
        loadBinary(savedBoard)
    }

    private fun mergeDots(dot1: Dot, dot2: Dot): Boolean {
        var d1 = dot1
        var d2 = dot2
        var depth1 = 0
        var depth2 = 0
        while (true) { d1 = d1.parent ?: break; depth1++ }
        while (true) { d2 = d2.parent ?: break; depth2++ }
        if (d1 === d2) return false
        if (depth1 < depth2) d1.parent = d2 else d2.parent = d1
        return true
    }

    private fun mergeCells(c: Connector, changed: MutableList<Connector>?): Boolean {
        var parent1 = c.cell1!!
        var parent2 = c.cell2!!
        var depth1 = 0
        var depth2 = 0
        var border1 = 0
        var border2 = 0
        while (true) {
            val p = parent1.parent ?: break
            depth1++
            if (!parent1.parentSameSide) border1++
            parent1 = p
        }
        while (true) {
            val p = parent2.parent ?: break
            depth2++
            if (!parent2.parentSameSide) border2++
            parent2 = p
        }
        val shouldBe = if ((border1 + border2) % 2 == 0) Status.UNSET else Status.SET

        if (parent1 === parent2) {
            // As duas casas já estão no mesmo conjunto.
            return if (c.status == Status.UNKNOWN) c.setStatus(shouldBe, changed) != Result.INVALID
            else shouldBe == c.status
        }
        if (c.status == Status.UNKNOWN) return true // Sem informação para as ligar.

        if (depth1 < depth2) {
            parent1.parent = parent2
            parent1.parentSameSide = shouldBe == c.status
        } else {
            parent2.parent = parent1
            parent2.parentSameSide = shouldBe == c.status
        }
        return true
    }

    override fun toString(): String = buildString {
        val horizontal = mapOf(Status.UNSET to ' ', Status.SET to '-', Status.UNKNOWN to '?')
        val vertical = mapOf(Status.UNSET to ' ', Status.SET to '|', Status.UNKNOWN to '?')
        for (y in 0 until maxY - 1) {
            for (x in 0 until maxX - 1) append('+').append(horizontal[dots[x][y].right.status])
            append("+\n")
            for (x in 0 until maxX - 1) {
                append(vertical[dots[x][y].down.status])
                append(if (cells[x][y].hidden) ' ' else ('0' + cells[x][y].number))
            }
            append(vertical[dots[maxX - 1][y].down.status]).append('\n')
        }
        for (x in 0 until maxX - 1) append('+').append(horizontal[dots[x][maxY - 1].right.status])
        append("+\n")
    }

    companion object {
        fun fromBinary(data: ByteArray): Board = Board(data[0].toInt(), data[1].toInt()).apply { loadBinary(data) }

        /** Junta duas soluções parciais: fica o que é igual nas duas, o resto passa a desconhecido. */
        private fun mergeBinaries(board1: ByteArray, board2: ByteArray): ByteArray =
            ByteArray(board1.size) { i ->
                if (board1[i] != board2[i]) Status.UNKNOWN.code.toByte() else board1[i]
            }
    }
}
