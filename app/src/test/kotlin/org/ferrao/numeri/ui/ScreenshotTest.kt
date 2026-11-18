package org.ferrao.numeri.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.runtime.Composable
import org.ferrao.numeri.game.Board
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_8_PRO)

    private fun puzzle(): Board = Board(6, 7).apply {
        generatePuzzle(18, hard = true, random = Random(11))
        clear()
    }

    @Composable
    private fun Game(board: Board, showHidden: Boolean = false, celebrating: Boolean = false, waiting: Boolean = false) {
        GameScreen(
            board = board, revision = 0, showHidden = showHidden, celebrating = celebrating, solvedCount = 12,
            busy = false, waitingForNext = waiting,
            onCellDown = { _, _ -> }, onCellEnter = { _, _ -> }, onRelease = {},
            onPrev = {}, onNext = {}, onHelp = {},
        )
    }

    @Test
    fun start() = paparazzi.snapshot { Game(puzzle()) }

    @Test
    fun inProgress() {
        val board = puzzle()
        board.numberHintList().take(9).forEach { it.switchAll() }
        paparazzi.snapshot { Game(board) }
    }

    @Test
    fun crossing() {
        val board = puzzle()
        board.cells[1][1].switchAll()
        board.cells[2][2].switchAll()
        board.cells[3][4].switchAll()
        paparazzi.snapshot { Game(board, waiting = true) }
    }

    @Test
    fun revealed() {
        val board = puzzle()
        board.numberHintList().take(9).forEach { it.switchAll() }
        paparazzi.snapshot { Game(board, showHidden = true) }
    }

    @Test
    fun solved() {
        val board = puzzle()
        board.numberHintList().forEach { it.switchAll() }
        paparazzi.snapshot { Game(board, celebrating = true) }
    }

    @Test
    fun help() = paparazzi.snapshot {
        HelpScreen(
            listOf(
                "Bem-vindo ao jogo Numeri.",
                "O objetivo do jogo é desenhar uma única linha fechada que deixe todos os dígitos válidos (brancos).",
                "Cada dígito indica o número de segmentos da linha à volta desse dígito.",
                "Toque numa casa, ou arraste o dedo por várias, para trocar os seus lados.",
                "Use '>' para ver a solução, ou para criar um novo puzzle. Use '<' para limpar o puzzle.",
                "Pode virar o telemóvel para baixo para revelar os dígitos omissos na grelha.",
                "Para técnicas do jogo procure por Slitherlink na Wikipédia.",
                "Bom jogo!",
            ),
            onPlay = {},
        )
    }
}
