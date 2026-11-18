package org.ferrao.numeri.ui

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Matrix
import org.ferrao.numeri.game.Board
import org.ferrao.numeri.game.Connector
import org.ferrao.numeri.game.Dot
import org.ferrao.numeri.game.Status
import kotlin.math.floor
import kotlin.math.hypot

/**
 * O tabuleiro: casas dentro da linha preenchidas, a linha com cantos arredondados e um brilho
 * suave, e os números (a vermelho enquanto não batem certo).
 * Tocar ou arrastar sobre as casas chama [onCellDown] e depois [onCellEnter] a cada casa nova.
 */
@Composable
fun BoardCanvas(
    board: Board,
    revision: Int,
    showHidden: Boolean,
    celebrating: Boolean,
    enabled: Boolean,
    onCellDown: (Int, Int) -> Unit,
    onCellEnter: (Int, Int) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone by animateColorAsState(if (celebrating) Palette.success else Palette.accent, tween(600), label = "tone")
    val textMeasurer = rememberTextMeasurer()
    val currentEnabled by rememberUpdatedState(enabled)
    val down by rememberUpdatedState(onCellDown)
    val enter by rememberUpdatedState(onCellEnter)
    val release by rememberUpdatedState(onRelease)
    val loop = remember(board, revision) { loopPath(board) }

    Canvas(
        modifier
            .aspectRatio(board.columns.toFloat() / board.rows)
            .pointerInput(board) {
                fun cellAt(p: Offset): Pair<Int, Int>? {
                    val cell = size.width.toFloat() / board.columns
                    val x = floor(p.x / cell).toInt()
                    val y = floor(p.y / cell).toInt()
                    return if (x in 0 until board.columns && y in 0 until board.rows) x to y else null
                }
                awaitEachGesture {
                    val first = awaitFirstDown()
                    if (!currentEnabled) return@awaitEachGesture
                    var last = cellAt(first.position)
                    last?.let { (x, y) -> down(x, y) }
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == first.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val now = cellAt(change.position)
                        if (now != null && now != last) {
                            last = now
                            enter(now.first, now.second)
                        }
                    }
                    release()
                }
            },
    ) {
        val cell = size.width / board.columns
        drawGrid(board, cell)

        val path = Path().apply {
            addPath(loop)
            transform(Matrix().apply { scale(cell, cell) })
            fillType = PathFillType.EvenOdd
        }
        val lineWidth = cell * 0.07f
        drawPath(path, tone.copy(alpha = if (celebrating) 0.22f else 0.15f))
        glowPath(path, tone.copy(alpha = 0.8f), lineWidth * 2f, cell * 0.14f)
        drawPath(path, Palette.line, style = Stroke(lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawMarkers(board, cell)

        val fontSize = (cell * 0.46f).toSp()
        for (column in board.cells) for (c in column) {
            if (c.hidden && !showHidden) continue
            val ok = c.strictValid()
            val layout = textMeasurer.measure(
                c.number.toString(),
                TextStyle(
                    color = if (ok) Palette.number else Palette.numberWrong,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Light,
                    shadow = if (ok) null else Shadow(Palette.wrongGlow.copy(alpha = 0.8f), blurRadius = cell * 0.18f),
                ),
            )
            drawText(
                layout,
                topLeft = Offset(
                    (c.x + 0.5f) * cell - layout.size.width / 2f,
                    (c.y + 0.5f) * cell - layout.size.height / 2f,
                ),
            )
        }
    }
}

private fun DrawScope.drawGrid(board: Board, cell: Float) {
    val gap = cell * 0.12f
    val thin = 1.dp.toPx()
    for (x in 0 until board.maxX) for (y in 0 until board.maxY) {
        val p = Offset(x * cell, y * cell)
        if (x + 1 < board.maxX) drawLine(Palette.gridLine, p + Offset(gap, 0f), p + Offset(cell - gap, 0f), thin)
        if (y + 1 < board.maxY) drawLine(Palette.gridLine, p + Offset(0f, gap), p + Offset(0f, cell - gap), thin)
        drawCircle(Palette.gridDot, radius = maxOf(cell * 0.03f, 1.5.dp.toPx()), center = p)
    }
}

/** Cruz vermelha nos pontos onde a linha se cruza ou fica solta, como no original. */
private fun DrawScope.drawMarkers(board: Board, cell: Float) {
    val arm = cell * 0.09f
    for (column in board.dots) for (dot in column) {
        if (!dot.showMarker()) continue
        val c = Offset(dot.x * cell, dot.y * cell)
        glowCircle(c, arm * 1.4f, Palette.wrongGlow.copy(alpha = 0.8f), arm * 1.6f)
        val stroke = cell * 0.045f
        drawLine(Palette.numberWrong, c + Offset(-arm, -arm), c + Offset(arm, arm), stroke, StrokeCap.Round)
        drawLine(Palette.numberWrong, c + Offset(arm, -arm), c + Offset(-arm, arm), stroke, StrokeCap.Round)
    }
}

/**
 * Brilho difuso com desfoque gaussiano, em vez de traços largos semitransparentes (que deixam
 * anéis visíveis). Com aceleração por hardware o Android só desfoca a partir da API 28;
 * abaixo disso fica sem brilho.
 */
private inline fun DrawScope.glow(color: Color, blur: Float, draw: (Canvas, Paint) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
    drawIntoCanvas { canvas ->
        val paint = Paint().apply { this.color = color }
        paint.asFrameworkPaint().maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        draw(canvas, paint)
    }
}

private fun DrawScope.glowPath(path: Path, color: Color, width: Float, blur: Float) = glow(color, blur) { canvas, paint ->
    paint.style = PaintingStyle.Stroke
    paint.strokeWidth = width
    paint.strokeCap = StrokeCap.Round
    paint.strokeJoin = StrokeJoin.Round
    canvas.drawPath(path, paint)
}

private fun DrawScope.glowCircle(center: Offset, radius: Float, color: Color, blur: Float) =
    glow(color, blur) { canvas, paint -> canvas.drawCircle(center, radius, paint) }

private fun Connector.isHorizontal() = dot1!!.y == dot2!!.y

private fun Connector.other(dot: Dot) = if (dot1 === dot) dot2!! else dot1!!

/**
 * Percorre os segmentos marcados e devolve a linha como contornos fechados em unidades de casa,
 * com cantos arredondados. Num ponto com 4 segmentos prefere virar, para separar os contornos.
 */
internal fun loopPath(board: Board): Path {
    val path = Path()
    val pending = board.connectors.filterTo(LinkedHashSet()) { it.status == Status.SET }
    while (pending.isNotEmpty()) {
        val start = pending.first()
        pending.remove(start)
        val points = mutableListOf(start.dot2!!, start.dot1!!)
        var incoming = start
        var closed = false
        while (true) {
            val dot = points.last()
            val options = listOf(dot.up, dot.left, dot.right, dot.down).filter { it in pending }
            val next = options.firstOrNull { it.isHorizontal() != incoming.isHorizontal() } ?: options.firstOrNull()
                ?: break
            pending.remove(next)
            incoming = next
            val other = next.other(dot)
            if (other === points.first()) {
                closed = true
                break
            }
            points += other
        }
        if (closed) addRounded(path, points) else {
            path.moveTo(points[0].x.toFloat(), points[0].y.toFloat())
            points.drop(1).forEach { path.lineTo(it.x.toFloat(), it.y.toFloat()) }
        }
    }
    return path
}

private const val CORNER = 0.38f
private const val KAPPA = 0.55f

private fun addRounded(path: Path, points: List<Dot>) {
    val n = points.size
    fun at(i: Int) = points[(i + n) % n].let { Offset(it.x.toFloat(), it.y.toFloat()) }
    val start = (at(0) + at(1)) / 2f
    path.moveTo(start.x, start.y)
    for (i in 1..n) {
        val prev = at(i - 1)
        val v = at(i)
        val next = at(i + 1)
        val din = (v - prev) / hypot((v - prev).x, (v - prev).y)
        val dout = (next - v) / hypot((next - v).x, (next - v).y)
        if (din == dout) continue // Segue a direito.
        val a = v - din * CORNER
        val b = v + dout * CORNER
        path.lineTo(a.x, a.y)
        val c1 = a + din * (CORNER * KAPPA)
        val c2 = b - dout * (CORNER * KAPPA)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
    }
    path.lineTo(start.x, start.y)
    path.close()
}

/**
 * Exemplo da ajuda (as imagens 1, 2 e 3 do original): uma casa com o número [sides] e a linha
 * nesse número de lados, com as pontas soltas marcadas a vermelho.
 */
@Composable
fun RuleExample(sides: Int, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val points = when (sides) {
        1 -> listOf(Offset(0f, 1f), Offset(0f, 0f))
        2 -> listOf(Offset(0f, 1f), Offset(0f, 0f), Offset(1f, 0f))
        else -> listOf(Offset(1f, 0f), Offset(0f, 0f), Offset(0f, 1f), Offset(1f, 1f))
    }
    Canvas(modifier.aspectRatio(1f)) {
        val margin = size.width * 0.18f
        val cell = size.width - 2 * margin
        fun px(p: Offset) = Offset(margin + p.x * cell, margin + p.y * cell)

        val gap = cell * 0.12f
        val thin = 1.dp.toPx()
        val corners = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(1f, 1f), Offset(0f, 1f))
        corners.forEachIndexed { i, corner ->
            val next = corners[(i + 1) % 4]
            val dir = next - corner
            drawLine(Palette.gridLine, px(corner) + dir * gap, px(next) - dir * gap, thin)
            drawCircle(Palette.gridDot, radius = maxOf(cell * 0.03f, 1.5.dp.toPx()), center = px(corner))
        }

        val path = Path().apply {
            px(points.first()).let { moveTo(it.x, it.y) }
            for (i in 1 until points.size - 1) {
                val v = points[i]
                val din = v - points[i - 1]
                val dout = points[i + 1] - v
                val a = px(v - din * CORNER)
                val b = px(v + dout * CORNER)
                val c1 = a + din * (CORNER * KAPPA * cell)
                val c2 = b - dout * (CORNER * KAPPA * cell)
                lineTo(a.x, a.y)
                cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
            }
            px(points.last()).let { lineTo(it.x, it.y) }
        }
        val lineWidth = cell * 0.07f
        glowPath(path, Palette.accent.copy(alpha = 0.8f), lineWidth * 2f, cell * 0.14f)
        drawPath(path, Palette.line, style = Stroke(lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        for (end in listOf(points.first(), points.last())) {
            glowCircle(px(end), cell * 0.11f, Palette.wrongGlow.copy(alpha = 0.85f), cell * 0.12f)
            drawCircle(Palette.numberWrong, radius = cell * 0.085f, center = px(end))
        }

        val layout = textMeasurer.measure(
            sides.toString(),
            TextStyle(color = Palette.number, fontSize = (cell * 0.46f).toSp(), fontWeight = FontWeight.Light),
        )
        drawText(
            layout,
            topLeft = px(Offset(0.5f, 0.5f)) - Offset(layout.size.width / 2f, layout.size.height / 2f),
        )
    }
}
