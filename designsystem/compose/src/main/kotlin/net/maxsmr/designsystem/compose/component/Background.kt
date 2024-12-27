package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.maxsmr.designsystem.compose.theme.AppTheme

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AppTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colors.surface // background
        ) {
            content()
        }
    }
}