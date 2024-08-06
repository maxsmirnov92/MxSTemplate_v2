package net.maxsmr.core.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.maxsmr.commonutils.live.postValueIfNew
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.network.NetworkStateManager.asStateLiveData
import net.maxsmr.core.android.network.NetworkStateManager.hasConnection
import java.io.Serializable

/**
 * Менеджер доступности сетевого подключения.
 * Получение текущего значения - [hasConnection], подписка - [asStateLiveData]
 */
object NetworkStateManager {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(NetworkStateManager::class.java)

    private val connectivityManager =
        baseApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val connectionLiveData: ConnectionLiveData = ConnectionLiveData()

    fun asLiveData(): LiveData<ConnectionInfo> = connectionLiveData

    fun asStateLiveData() = connectionLiveData.map { it.has }

    fun asFlow(scope: CoroutineScope): StateFlow<ConnectionInfo> = callbackFlow {

        val callback = object : BaseNetworkCallback() {

            override fun onConnectivityChanged(newState: Boolean) {
                trySend(if (newState) {
                    getConnectionInfo()
                } else {
                    ConnectionInfo()
                })
            }
        }

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().run {
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            build()
        }, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), getConnectionInfo())

    fun asStateFlow(scope: CoroutineScope): Flow<Boolean> = asFlow(scope).map { it.has }

    /**
     * Принудительно обновить LD. **Не использовать** - см. описание класса.
     */
    fun updateConnectionLiveDataState() {
        connectionLiveData.postValue(getConnectionInfo())
    }

    fun hasConnection() = getConnectionInfo().has

    fun getConnectionInfo(): ConnectionInfo {
        val capabilities = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork) ?: return ConnectionInfo()
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val has = hasInternet && (hasCellular || hasWifi)
        return ConnectionInfo(has, has && hasCellular, has && hasWifi)
    }

    private fun ConnectivityManager.unregisterNetworkCallbackSafe(callback: ConnectivityManager.NetworkCallback) {
        try {
            unregisterNetworkCallback(callback)
        } catch (ignored: IllegalArgumentException) {
        }
    }

    data class ConnectionInfo(
        val has: Boolean,
        val hasCellular: Boolean? = null,
        val hasWiFi: Boolean? = null,
    ) : Serializable {

        constructor() : this(false, false, false)
    }

    private class ConnectionLiveData : MutableLiveData<ConnectionInfo>(getConnectionInfo()) {

        private val callback = object : BaseNetworkCallback() {

            override fun onConnectivityChanged(newState: Boolean) {
                //Методы могут вызываться не в Main потоке, поэтому post
                this@ConnectionLiveData.postValueIfNew(
                    if (newState) {
                        getConnectionInfo()
                    } else {
                        ConnectionInfo()
                    }
                )
            }
        }

        override fun onActive() {
            super.onActive()

            connectivityManager.unregisterNetworkCallbackSafe(callback)
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder().run {
                addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                build()
            }, callback)

            if (callback.activeNetworks.isEmpty()) {
                //при отсутствии WiFi и Cellular коллбек после регистрации не срабатывает,
                //на initial тоже нельзя полагаться, т.к. возможен кейс:
                //1. Открыли апп при наличии сети
                //2. Закрыли апп, LD стала неактивна, слушатель снят
                //3. Отключили сеть
                //4. Переоткрыли апп
                //ФР: LD стала активна, но не пересоздается (остается в памяти), initial не влияет, коллбек не срабатывает
                postValueIfNew(getConnectionInfo())
            }
            logger.d("registerNetworkCallback")
        }

        override fun onInactive() {
            super.onInactive()
            connectivityManager.unregisterNetworkCallbackSafe(callback)
            callback.activeNetworks.clear()
            logger.d("unregisterNetworkCallback")
        }

        override fun getValue(): ConnectionInfo {
            return super.getValue() ?: ConnectionInfo()
        }
    }

    private abstract class BaseNetworkCallback : ConnectivityManager.NetworkCallback() {

        val activeNetworks: MutableSet<Network> = mutableSetOf()

        val hasActiveNetworks: Boolean get() = activeNetworks.isNotEmpty()

        final override fun onAvailable(network: Network) {
            super.onAvailable(network)
            synchronized(activeNetworks) {
                activeNetworks.add(network)
                logger.d("Connection $network available. Active: ${activeNetworks.joinToString()}")
                onConnectivityChanged(hasActiveNetworks)
            }
        }

        final override fun onLost(network: Network) {
            super.onLost(network)
            synchronized(activeNetworks) {
                activeNetworks.remove(network)
                logger.d("Connection $network lost. Active: ${activeNetworks.joinToString()}")
                onConnectivityChanged(hasActiveNetworks)
            }
        }

        abstract fun onConnectivityChanged(newState: Boolean)
    }
}