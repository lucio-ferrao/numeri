package org.ferrao.numeri.game

/** Estado de um segmento; o código é o byte usado no formato binário do tabuleiro. */
enum class Status(val code: Int) {
    UNKNOWN(0), SET(1), UNSET(2);

    companion object {
        fun of(code: Int): Status = entries[code]
    }
}

enum class Result { INCOMPLETE, INVALID, CHANGED, TERMINAL, TWO_SOLUTIONS }

/** Segmento entre dois pontos, que separa duas casas. */
class Connector(var status: Status) {
    var dot1: Dot? = null
    var dot2: Dot? = null
    var cell1: Cell? = null
    var cell2: Cell? = null

    fun isNear(c: Connector): Boolean =
        cell1 === c.cell2 || cell2 === c.cell1 || cell1 === c.cell1 || cell2 === c.cell2 ||
            dot1 === c.dot2 || dot2 === c.dot1 || dot1 === c.dot1 || dot2 === c.dot2

    fun commonDot(c: Connector): Dot? = when {
        dot1 === c.dot1 || dot1 === c.dot2 -> dot1
        dot2 === c.dot1 || dot2 === c.dot2 -> dot2
        else -> null
    }

    fun switch() {
        status = if (status == Status.SET) Status.UNSET else Status.SET
    }

    fun setStatus(s: Status, changed: MutableList<Connector>?): Result {
        changed?.add(this)
        status = s
        if (dot1?.simplify(changed) == Result.INVALID) return Result.INVALID
        if (dot2?.simplify(changed) == Result.INVALID) return Result.INVALID
        if (cell1?.simplify(changed) == Result.INVALID) return Result.INVALID
        if (cell2?.simplify(changed) == Result.INVALID) return Result.INVALID
        return Result.CHANGED
    }
}

/** Ponto da grelha, com os 4 segmentos que dele partem. */
class Dot(val x: Int, val y: Int) {
    lateinit var right: Connector
    lateinit var down: Connector
    lateinit var up: Connector
    lateinit var left: Connector
    var parent: Dot? = null

    /** Pela mesma ordem do original: a ordem conta em [replace]. */
    private val sides by lazy(LazyThreadSafetyMode.NONE) { listOf(right, left, up, down) }

    fun count(s: Status): Int = sides.count { it.status == s }

    fun valid(): Boolean {
        val set = count(Status.SET)
        return set < 3 && !(set == 1 && count(Status.UNKNOWN) == 0)
    }

    /** Ponto com um número ímpar de segmentos, ou com 4: marca-se a vermelho. */
    fun showMarker(): Boolean {
        val set = count(Status.SET)
        return !(set == 0 || set == 2)
    }

    private fun replace(s1: Status, s2: Status, changed: MutableList<Connector>?): Result {
        for (c in sides) {
            if (c.status == s1 && c.setStatus(s2, changed) == Result.INVALID) return Result.INVALID
        }
        return Result.INCOMPLETE
    }

    fun simplify(changed: MutableList<Connector>?): Result {
        val set = count(Status.SET)
        val unknown = count(Status.UNKNOWN)
        if (set > 2 || (set == 1 && unknown == 0)) return Result.INVALID
        if (unknown == 0) return Result.TERMINAL
        if (set == 2) return replace(Status.UNKNOWN, Status.UNSET, changed)
        if (unknown == 1) {
            return if (set == 1) replace(Status.UNKNOWN, Status.SET, changed)
            else replace(Status.UNKNOWN, Status.UNSET, changed)
        }
        return Result.INCOMPLETE
    }
}

/**
 * Casa da grelha com o seu número (0 a 3). A casa "exterior" do tabuleiro não tem segmentos
 * ([sides] vazio) e serve de vizinha aos segmentos da borda.
 */
class Cell(val x: Int, val y: Int, private val sides: List<Connector>) {
    var number = 0
    var hidden = true
    var parent: Cell? = null

    /** true quando esta casa e a mãe estão do mesmo lado da linha. */
    var parentSameSide = false

    /** Ordem dos segmentos em [sides]: direita, esquerda, cima, baixo (a do original). */
    val left: Connector get() = sides[1]

    fun count(s: Status): Int = sides.count { it.status == s }

    fun strictValid(): Boolean = number == count(Status.SET)

    fun valid(): Boolean {
        val c = count(Status.SET)
        val u = count(Status.UNKNOWN)
        return hidden || (number >= c && number <= c + u)
    }

    fun fulfilled(): Boolean = hidden || count(Status.SET) == number

    fun switchAll() = sides.forEach { it.switch() }

    private fun replace(s1: Status, s2: Status, changed: MutableList<Connector>?): Result {
        for (c in sides) {
            if (c.status == s1 && c.setStatus(s2, changed) == Result.INVALID) return Result.INVALID
        }
        return Result.CHANGED
    }

    fun simplify(changed: MutableList<Connector>?): Result {
        val set = count(Status.SET)
        val unknown = count(Status.UNKNOWN)
        if (!(set < 4 && (hidden || (number >= set && number <= set + unknown)))) return Result.INVALID
        if (unknown == 0) return Result.TERMINAL
        if (hidden) return Result.INCOMPLETE
        if (number == set) return replace(Status.UNKNOWN, Status.UNSET, changed)
        if (unknown == number - set) return replace(Status.UNKNOWN, Status.SET, changed)
        return Result.INCOMPLETE
    }
}
