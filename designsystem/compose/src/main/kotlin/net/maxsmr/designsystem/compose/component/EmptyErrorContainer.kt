package net.maxsmr.designsystem.compose.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmptyErrorContainer(
    emptyErrorMessage: String,
    @StringRes errorButtonResId: Int?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emptyErrorMessage,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Default,
        )
        Spacer(modifier = Modifier.size(10.dp))
        if (errorButtonResId != null) {
            TextButton(
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colors.onPrimary
                ),
                onClick = {
                    onClick?.invoke()
                }) {
                Text(
                    text = stringResource(errorButtonResId),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
        }
    }
}