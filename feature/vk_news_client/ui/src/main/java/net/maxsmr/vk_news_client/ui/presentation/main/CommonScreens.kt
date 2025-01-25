package net.maxsmr.vk_news_client.ui.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.maxsmr.commonutils.gui.message.errorMessage
import net.maxsmr.commonutils.gui.message.formatMessage
import net.maxsmr.commonutils.states.ILoadState
import net.maxsmr.designsystem.compose.component.EmptyErrorContainer

@Composable
fun EmptyErrorScreen(
    state: ILoadState<*>,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    EmptyErrorContainer(
        if (state.isError) {
            state.error?.errorMessage().formatMessage(
                net.maxsmr.core.android.R.string.error_format,
                net.maxsmr.core.network.R.string.error_unexpected_try_again
            ).get(LocalContext.current).toString()
        } else {
            LocalContext.current.getString(net.maxsmr.core.android.R.string.no_data)
        },
        buttonResId = if (state.wasLoaded) {
            net.maxsmr.core.android.R.string.try_again
        } else {
            null
        },
        modifier = modifier
    ) {
        onRetry()
    }
}

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = stringResource(net.maxsmr.core.android.R.string.loading),
            fontSize = 16.sp,
            fontFamily = FontFamily.Default,
        )
    }
}