package net.maxsmr.core.android.base.connection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.live.zipNotNull
import net.maxsmr.core.android.R
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem

/**
 * Класс определяет логику обработки состояния сети. Хранится во ViewModel
 */
class ConnectionManager() {

    private val manualCheck: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    private val networkStateManager by lazy { NetworkStateManager }

    /**
     * Эмитит признак доступности соединения
     */
    val asLiveData: LiveData<Boolean> by lazy {
        zipNotNull(
            manualCheck,
            networkStateManager.asLiveData()
        ) { manual, status ->
            manual == true || status == true
        }
    }

    /**
     * Признак доступности соединения
     */
    val has: Boolean
        get() = asLiveData.value == true


    var queue: AlertQueue? = null
        private set

    /**
     * Конструктор для создания менеджера, дополнительно помещающего алерты об отсутствии сети в [queue]
     *
     * @param queue очередь сообщений, куда помещаются алерты об отсутствии сети. Null, если алерты не нужны
     * @param builder опциональный билдер на случай нестандартного алерта
     */
    @Suppress("DEPRECATION")
    constructor(queue: AlertQueue, builder: AlertQueueItem.Builder? = null) : this() {
        this.queue = queue
        asLiveData.observeForever {
            if (it) {
                queue.removeAllWithTag(TAG_CONNECTIVITY)
            } else {
                builder?.build()
                    ?: AlertQueueItem.Builder(TAG_CONNECTIVITY, queue)
                        .setTitle(R.string.no_connection)
                        .setAnswers(Alert.Answer(R.string.try_again_internet).also { it.select = ::check })
                        .setUniqueStrategy(AlertQueueItem.UniqueStrategy.Ignore)
                        .build()
            }
        }
    }

    /**
     * Запускает проверку доступности соединения
     */
    fun check() {
        manualCheck.postValue(networkStateManager.hasConnection())
    }


    companion object {

        const val TAG_CONNECTIVITY = "TAG_CONNECTIVITY"
    }
}