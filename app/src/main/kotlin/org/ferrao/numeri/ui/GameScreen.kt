package org.ferrao.numeri.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ferrao.numeri.R
import org.ferrao.numeri.game.Board

@Composable
fun GameScreen(
    board: Board,
    revision: Int,
    showHidden: Boolean,
    celebrating: Boolean,
    solvedCount: Int,
    busy: Boolean,
    waitingForNext: Boolean,
    onCellDown: (Int, Int) -> Unit,
    onCellEnter: (Int, Int) -> Unit,
    onRelease: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onHelp: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { Wordmark(height = 34.dp) }
            val help = stringResource(R.string.help)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onHelp)
                    .semantics { contentDescription = help },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .border(1.5.dp, Palette.textSecondary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("?", color = Palette.textSecondary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            BoardCanvas(
                board = board,
                revision = revision,
                showHidden = showHidden,
                celebrating = celebrating,
                enabled = !busy,
                onCellDown = onCellDown,
                onCellEnter = onCellEnter,
                onRelease = onRelease,
                modifier = Modifier.widthIn(max = 560.dp),
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 28.dp, top = 8.dp),
        ) {
            RoundButton(
                description = stringResource(R.string.clear),
                enabled = !busy,
                onClick = onPrev,
                modifier = Modifier.align(Alignment.CenterStart),
            ) { Chevron(forward = false) }

            androidx.compose.animation.AnimatedVisibility(
                visible = celebrating,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Surface(
                    onClick = onNext,
                    shape = RoundedCornerShape(50),
                    color = Palette.success.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Palette.success.copy(alpha = 0.45f)),
                ) {
                    Text(
                        "$solvedCount ✓",
                        color = Palette.success,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
                    )
                }
            }

            RoundButton(
                description = stringResource(R.string.next),
                enabled = !busy && !waitingForNext,
                onClick = onNext,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                if (waitingForNext) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Palette.text, strokeWidth = 2.5.dp)
                } else {
                    Chevron(forward = true)
                }
            }
        }
    }
}

/** O nome manuscrito do original, vetorizado. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, height: Dp) {
    Image(
        painterResource(R.drawable.logo_numeri),
        contentDescription = "Numeri",
        colorFilter = ColorFilter.tint(Palette.text),
        modifier = modifier.height(height),
    )
}

@Composable
private fun RoundButton(
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Palette.buttonFill)
            .border(1.dp, Palette.buttonBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Chevron(forward: Boolean) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val (x1, x2) = if (forward) 0.36f * w to 0.68f * w else 0.64f * w to 0.32f * w
        val stroke = 2.6.dp.toPx()
        drawLine(Palette.text, Offset(x1, 0.18f * h), Offset(x2, 0.5f * h), stroke, StrokeCap.Round)
        drawLine(Palette.text, Offset(x2, 0.5f * h), Offset(x1, 0.82f * h), stroke, StrokeCap.Round)
    }
}

/** Posição, em help_text, do parágrafo que explica os números (igual nas 4 línguas). */
private const val RULE_PARAGRAPH = 2

@Composable
fun HelpScreen(paragraphs: List<String>, onPlay: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .weight(1f)
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Wordmark(Modifier.padding(top = 36.dp, bottom = 12.dp), height = 56.dp)
            paragraphs.forEachIndexed { i, paragraph ->
                Text(paragraph, color = Palette.text.copy(alpha = 0.86f), fontSize = 17.sp, lineHeight = 25.sp)
                // Como no original, os exemplos vêm a seguir ao parágrafo que explica os números.
                if (i == RULE_PARAGRAPH) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)) {
                        (1..3).forEach { RuleExample(it, Modifier.size(84.dp)) }
                    }
                }
            }
        }
        Surface(
            onClick = onPlay,
            shape = RoundedCornerShape(50),
            color = Palette.line,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 28.dp)
                .widthIn(min = 200.dp),
        ) {
            Text(
                stringResource(R.string.play),
                color = Palette.backgroundTop,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
