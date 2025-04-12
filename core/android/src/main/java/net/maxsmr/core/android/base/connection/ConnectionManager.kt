package net.maxsmr.core.android.base.connection

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.flow.observe
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.network.NetworkStateManager

/**
 * Класс определяет логику обработки состояния сети. Хранится во ViewModel
 */
class ConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    /**
     * Эмитит признак доступности соединения
     */
    val asStateFlow: StateFlow<Boolean> by lazy {
        combine(
            _manualCheck,
            networkStateManager.asStatusFlow()
        ) { manual, status ->
            manual || status
        }.stateIn(scope, SharingStarted.Eagerly, false)
    }

    /**
     * Признак доступности соединения
     */
    val has: Boolean
        get() = asStateFlow.value

    private val _manualCheck by lazy { MutableStateFlow(false) }

    private val networkStateManager by lazy { NetworkStateManager(context) }

    var queue: AlertQueue? = null
        private set

    /**
     * Конструктор для создания менеджера, дополнительно помещающего алерты об отсутствии сети в [queue]
     *
     * @param queue очередь сообщений, куда помещаются алерты об отсутствии сети. Null, если алерты не нужны
     * @param builder опциональный билдер на случай нестандартного алерта
     */
    constructor(
        context: Context,
        scope: CoroutineScope,
        queue: AlertQueue,
        builder: AlertQueueItem.Builder? = null,
    ) : this(context, scope) {
        this.queue = queue
        asStateFlow.observe(scope) {
            if (it) {
                queue.removeAllWithTag(SNACKBAR_TAG_CONNECTIVITY)
            } else {
                builder?.build()
                    ?: AlertQueueItem.Builder(SNACKBAR_TAG_CONNECTIVITY, queue)
                        .setTitle(net.maxsmr.core.network.R.string.error_no_connection)
                        .setAnswers(
                            Alert.Answer(net.maxsmr.core.android.R.string.check_again)
                                .also { alert -> alert.onSelect { check() } })
                        .setUniqueStrategy(AlertQueueItem.UniqueStrategy.Ignore)
                        .setExtraData(SnackbarExtraData(SnackbarExtraData.SnackbarLength.INDEFINITE))
                        .build()
            }
        }
    }

    /**
     * Запускает проверку доступности соединения
     */
    fun check() {
        scope.launch {
            _manualCheck.tryEmit(networkStateManager.hasConnection())
        }
    }

    companion object {

        const val SNACKBAR_TAG_CONNECTIVITY = "connectivity"
    }
}