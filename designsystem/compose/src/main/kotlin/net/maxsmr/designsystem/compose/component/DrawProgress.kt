package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RoundRectDrawProgress(
    modifier: Modifier = Modifier,
    fillColor: Color,
    progressFlow: StateFlow<Float>,
) {
    DrawProgress(
        modifier,
        progressFlow,
    ) {
        drawRoundRect(
            color = fillColor,
            size = size.copy(width = size.width * it),
            cornerRadius = CornerRadius(size.height / 2),
        )
    }
}

@Composable
fun DrawProgress(
    modifier: Modifier = Modifier,
    progressFlow: StateFlow<Float>,
    onDraw: DrawScope.(Float) -> Unit
) {
    val progress by progressFlow.collectAsState()
    Canvas(modifier = modifier) {
        onDraw(progress)
    }
}