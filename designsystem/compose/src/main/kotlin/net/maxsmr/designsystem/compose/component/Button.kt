package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.maxsmr.designsystem.compose.theme.AppColors
import net.maxsmr.designsystem.compose.theme.AppTypography


@Composable
fun ButtonSmallRounded(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        shape = RoundedCornerShape(60.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = AppColors.Black10
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp
        ),
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = AppColors.Neutrals500,
            style = AppTypography.labelMdSemibold
        )
    }
}