package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun FilledBox(
    fraction: Float,
    isHorizontal: Boolean = true,
    modifier: Modifier = Modifier,
    fillModifier: Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = with(fillModifier) {
                if (isHorizontal) {
                    fillMaxWidth(fraction)
                } else {
                    fillMaxHeight(fraction)
                }
            }
        )
    }
}

@Preview
@Composable
fun FilledBoxPreview() {
    Row(Modifier.fillMaxWidth()) {
        FilledBox(
            0.5f,
            fillModifier = Modifier
                .fillMaxHeight()
                .background(Color.Red),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.Gray)
        )
    }
}