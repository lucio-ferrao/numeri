package org.ferrao.numeri

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.ferrao.numeri.game.Board
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64
import kotlin.random.Random

enum class Sound { ERASE, SUCCESS }

/** O que o AppDelegate.m do original fazia: botões, gravação, puzzle seguinte em segundo plano. */
class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("numeri", Context.MODE_PRIVATE)

    var board by mutableStateOf(Board(COLUMNS + 1, ROWS + 1))
        private set

    /** Muda sempre que o tabuleiro muda, para o ecrã se redesenhar. */
    var revision by mutableIntStateOf(0)
        private set
    var solvedCount by mutableIntStateOf(prefs.getInt(KEY_SOLVED, 0))
        private set
    var celebrating by mutableStateOf(false)
        private set
    var waitingForNext by mutableStateOf(false)
        private set
    var animating by mutableStateOf(false)
        private set
    var showHidden by mutableStateOf(false)
        private set
    var showHelp by mutableStateOf(false)
        private set

    private val _sounds = MutableSharedFlow<Sound>(extraBufferCapacity = 4)
    val sounds: SharedFlow<Sound> = _sounds

    private var nextBoard: Board? = null
    private var prevBoard: ByteArray? = null
    private var showCongrats = true
    private var firstTime = false

    init {
        val saved = prefs.getString(KEY_BOARD, null)?.let(::decode)
        if (saved != null && saved[0].toInt() == COLUMNS + 1 && saved[1].toInt() == ROWS + 1) {
            board = Board.fromBinary(saved).apply { solution = prefs.getString(KEY_SOLUTION, null)?.let(::decode) }
        } else {
            firstTime = true
            showHelp = true
        }
        showCongrats = !board.isSolved()
        startGeneration()
    }

    private fun changed() {
        revision++
    }

    private fun save() {
        prefs.edit {
            putString(KEY_BOARD, encode(board.toBinary()))
            putString(KEY_SOLUTION, board.solution?.let(::encode))
        }
    }

    private fun startGeneration() {
        viewModelScope.launch {
            nextBoard = withContext(Dispatchers.Default) {
                Board(COLUMNS + 1, ROWS + 1).apply {
                    generatePuzzle((COLUMNS * ROWS * (0.5 + 0.02 * Random.nextInt(10))).toInt(), hard = true)
                }
            }
            if (waitingForNext) {
                waitingForNext = false
                newPuzzle()
            }
        }
    }

    private fun newPuzzle() {
        val next = nextBoard ?: run {
            waitingForNext = true
            return
        }
        _sounds.tryEmit(Sound.ERASE)
        celebrating = false
        showCongrats = true
        board = next
        nextBoard = null
        showHidden = false
        startGeneration()
        board.clear()
        save()
        changed()
    }

    fun onCellDown(x: Int, y: Int) = toggle(x, y)

    fun onCellEnter(x: Int, y: Int) = toggle(x, y)

    private fun toggle(x: Int, y: Int) {
        if (animating) return
        board.cells[x][y].switchAll()
        changed()
    }

    fun onRelease() {
        if (animating) return
        save()
        if (board.isSolved() && showCongrats) {
            showCongrats = false
            solvedCount++
            prefs.edit { putInt(KEY_SOLVED, solvedCount) }
            celebrating = true
            _sounds.tryEmit(Sound.SUCCESS)
        }
    }

    /** "<": desfaz a solução mostrada, ou limpa o tabuleiro. */
    fun onPrev() {
        if (animating) return
        val previous = prevBoard
        if (previous != null) {
            board.loadBinary(previous)
            prevBoard = null
        } else {
            board.clear()
        }
        save()
        changed()
        _sounds.tryEmit(Sound.ERASE)
    }

    /** ">": puzzle novo se este está resolvido, senão mostra a solução casa a casa. */
    fun onNext() {
        if (animating) return
        if (board.isSolved() || board.solution == null) {
            prevBoard = null
            newPuzzle()
        } else {
            prevBoard = board.toBinary()
            showSolution()
        }
    }

    private fun showSolution() {
        showCongrats = false
        animating = true
        viewModelScope.launch {
            for (cell in board.numberHintList()) {
                cell.switchAll()
                changed()
                delay(40)
            }
            save()
            animating = false
        }
    }

    fun openHelp() {
        if (!animating) showHelp = true
    }

    fun closeHelp() {
        showHelp = false
        if (firstTime) {
            firstTime = false
            onNext()
        }
    }

    /** Telemóvel virado para baixo: mostra os números escondidos. */
    fun setFaceDown(faceDown: Boolean) {
        if (showHidden == faceDown) return
        showHidden = faceDown
        _sounds.tryEmit(Sound.ERASE)
    }

    private companion object {
        const val COLUMNS = 5
        const val ROWS = 6
        const val KEY_BOARD = "board"
        const val KEY_SOLUTION = "solution"
        const val KEY_SOLVED = "solved"

        fun encode(data: ByteArray): String = Base64.getEncoder().encodeToString(data)
        fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)
    }
}
