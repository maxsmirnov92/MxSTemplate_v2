package net.maxsmr.designsystem.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.maxsmr.designsystem.compose.theme.AppColors.Black500
import net.maxsmr.designsystem.compose.theme.AppColors.Purple500
import net.maxsmr.designsystem.compose.theme.AppColors.Purple700
import net.maxsmr.designsystem.compose.theme.AppColors.Teal200

private val DarkColorPalette = darkColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.White,
    onSecondary = Black500,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Black500,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}