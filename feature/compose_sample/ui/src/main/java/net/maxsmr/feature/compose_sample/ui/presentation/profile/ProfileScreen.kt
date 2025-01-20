package net.maxsmr.feature.compose_sample.ui.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.feature.compose_sample.ui.presentation.main.TextWithCounter

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val snackCount = rememberSaveable {
        mutableIntStateOf(0)
    }
    Column {
        TextWithCounter("Profile")
        Spacer(Modifier.height(6.dp))
        Button({
            viewModel.showSnackbar(
                TextMessage("text: ${snackCount.intValue}"),
                data = SnackbarExtraData(length = SnackbarExtraData.SnackbarLength.INDEFINITE),
                answer = Alert.Answer("answer"),
                uniqueStrategy = AlertQueueItem.UniqueStrategy.Replace
            )
            snackCount.intValue++
        }) {
            Text(text = "Snack!")
        }
    }
}