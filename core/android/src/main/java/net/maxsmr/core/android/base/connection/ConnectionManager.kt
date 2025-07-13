package net.maxsmr.core.android.base.connection

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.maxsmr.commonutils.flow.observe
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.core.network.R

/**
 * Класс отслеживает текущее состояние сети
 * и отображет snackbar при её отсутствии
 */
class ConnectionManager(
    private val networkStateManager: NetworkStateManager,
    private val viewModel: BaseViewModel,
) {

    val statusFlow: StateFlow<Boolean> by lazy {  _statusFlow.asStateFlow() }

    val status: Boolean
        get() = statusFlow.value

    private val _statusFlow = MutableStateFlow(false)

    init {
        networkStateManager.asStatusFlow().observe(viewModel.viewModelScope) {
            _statusFlow.value = it
            if (it) {
                viewModel.hideSnackbars()
            } else {
                viewModel.showSnackbar(
                    R.string.error_no_connection,
                    SnackbarExtraData(SnackbarExtraData.SnackbarLength.INDEFINITE),
                    Alert.Answer(net.maxsmr.core.android.R.string.check_again)
                        .also { alert -> alert.onSelect(/*false*/) { check() } },
                    AlertQueueItem.UniqueStrategy.Replace,
                )
            }
        }
    }

    fun check() {
        _statusFlow.value = networkStateManager.hasConnection()
    }
}