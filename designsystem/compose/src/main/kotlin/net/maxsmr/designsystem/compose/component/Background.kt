package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.maxsmr.designsystem.compose.theme.AppColors

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val color = AppColors.LightGrey

    Surface(
        color = color,
        modifier = modifier.fillMaxSize(),
    ) {
        content()
    }
}