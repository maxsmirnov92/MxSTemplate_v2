package net.maxsmr.designsystem.compose.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    @StringRes buttonResId: Int?,
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
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Default,
        )
        Spacer(modifier = Modifier.size(10.dp))
        if (buttonResId != null) {
            TextButton(
                onClick = {
                    onClick?.invoke()
                }) {
                Text(
                    text = stringResource(buttonResId),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )
            }
        }
    }
}