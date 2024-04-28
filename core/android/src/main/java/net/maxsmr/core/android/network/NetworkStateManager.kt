package net.maxsmr.core.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.baseApplicationContext

/**
 * Менеджер доступности сетевого подключения.
 * Получение текущего значения - [hasConnection], подписка - [asLiveData]
 */
object NetworkStateManager {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(NetworkStateManager::class.java)

    private val connectivityManager = baseApplicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val connectionLiveData: ConnectionLiveData = ConnectionLiveData()

    private val callback = object : ConnectivityManager.NetworkCallback() {

        val activeNetworks: MutableSet<Network> = mutableSetOf()

        //Методы могут вызываться не в Main потоке, поэтому post
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            activeNetworks.add(network)
            connectionLiveData.postValue(activeNetworks.isNotEmpty())
            logger.d("connection $network available. Active: ${activeNetworks.joinToString()}")
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            activeNetworks.remove(network)
            connectionLiveData.postValue(activeNetworks.isNotEmpty())
            logger.d("connection $network lost. Active: ${activeNetworks.joinToString()}")
        }
    }

    suspend fun observeConnectivity(scope: CoroutineScope) = callbackFlow {
        var isConnected = hasConnection()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onConnectivityChanged(true)
            }

            override fun onLost(network: Network) {
                onConnectivityChanged(false)
            }

            fun onConnectivityChanged(newState: Boolean) {
                val wasConnected = isConnected
                isConnected = newState
                if (wasConnected != newState) {
                    trySend(newState)
                }
            }
        }

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().run {
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            build()
        }, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), hasConnection())

    /**
     * Принудительно обновить LD. **Не использовать** - см. описание класса.
     */
    fun updateConnectionLiveDataState() {
        connectionLiveData.postValue(hasConnection())
    }

    fun asLiveData(): LiveData<Boolean> = connectionLiveData

    @JvmStatic
    fun hasConnection(): Boolean {
        val capabilities = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        return hasInternet && (hasCellular || hasWifi)
    }

    private fun ConnectivityManager.unregisterNetworkCallbackSafe(callback: ConnectivityManager.NetworkCallback) {
        try {
            unregisterNetworkCallback(callback)
            this@NetworkStateManager.callback.activeNetworks.clear()
        } catch (ignored: IllegalArgumentException) {
        }
    }

    private class ConnectionLiveData : MutableLiveData<Boolean>(hasConnection()) {

        override fun onActive() {
            super.onActive()
            connectivityManager.unregisterNetworkCallbackSafe(callback)
            connectivityManager.registerDefaultNetworkCallback(callback)
            if (callback.activeNetworks.isEmpty()) {
                //при отсутствии WiFi и Cellular коллбек после регистрации не срабатывает,
                //на initial тоже нельзя полагаться, т.к. возможен кейс:
                //1. Открыли апп при наличии сети
                //2. Закрыли апп, LD стала неактивна, слушатель снят
                //3. Отключили сеть
                //4. Переоткрыли апп
                //ФР: LD стала активна, но не пересоздается (остается в памяти), initial не влияет, коллбек не срабатывает
                postValue(hasConnection())
            }
            logger.d("registerNetworkCallback")
        }

        override fun onInactive() {
            super.onInactive()
            connectivityManager.unregisterNetworkCallbackSafe(callback)
            logger.d("unregisterNetworkCallback")
        }

        override fun getValue(): Boolean {
            return super.getValue() ?: false
        }
    }
}