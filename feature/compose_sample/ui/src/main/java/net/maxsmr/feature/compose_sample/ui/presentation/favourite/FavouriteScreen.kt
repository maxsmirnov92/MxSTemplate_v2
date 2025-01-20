package net.maxsmr.feature.compose_sample.ui.presentation.favourite

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import net.maxsmr.commonutils.text.EMPTY_STRING

@Composable
fun FavouriteScreen(viewModel: FavouriteViewModel) {
    val field = viewModel.field
    Column {
        val valueState = field.valueLive.observeAsState(EMPTY_STRING)
        val hintState = field.hintLive.observeAsState(null)
        val errorState = field.errorLive.observeAsState(null)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = valueState.value,
            onValueChange = {
                field.value = it
            },
            singleLine = true,
            isError = field.hasError,
            label = {
                val hint = hintState.value?.get(LocalContext.current)?.toString()
                hint?.let {
                    Text(
                        modifier = Modifier.wrapContentWidth(),
                        text = hint,
//                                    color = MaterialTheme.colors.onSecondary
                    )
                }
            },
            placeholder = {
                val error = errorState.value?.get(LocalContext.current)?.toString()
                error?.let {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }
}