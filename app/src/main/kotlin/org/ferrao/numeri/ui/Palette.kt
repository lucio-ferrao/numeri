package org.ferrao.numeri.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Cores do jogo: fundo quase preto como no original, linha branca, erros em vermelho suave. */
object Palette {
    val backgroundTop = Color(0xFF0A0C11)
    val backgroundBottom = Color(0xFF131826)
    val background = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom))

    val gridDot = Color.White.copy(alpha = 0.30f)
    val gridLine = Color.White.copy(alpha = 0.06f)

    val line = Color(0xFFF3F5FA)
    val accent = Color(0xFF6E8BFF)
    val success = Color(0xFF45D99B)

    val number = Color(0xFFEEF1F7)
    val numberWrong = Color(0xFFFF8080)
    val wrongGlow = Color(0xFFFF4545)

    val text = Color(0xFFE8EBF2)
    val textSecondary = Color.White.copy(alpha = 0.62f)
    val buttonFill = Color.White.copy(alpha = 0.06f)
    val buttonBorder = Color.White.copy(alpha = 0.12f)
}
