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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import net.maxsmr.commonutils.isAtLeastMarshmallow
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.network.NetworkStateManager.asLiveData
import net.maxsmr.core.android.network.NetworkStateManager.hasConnection

/**
 * Менеджер доступности сетевого подключения.
 * Получение текущего значения - [hasConnection], подписка - [asLiveData]
 */
object NetworkStateManager {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(NetworkStateManager::class.java)

    private val connectivityManager = baseApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val connectionLiveData: ConnectionLiveData = ConnectionLiveData()

    fun asLiveData(): LiveData<Boolean> = connectionLiveData

    fun asFlow(scope: CoroutineScope): StateFlow<Boolean> = callbackFlow {

        var isConnected = hasConnection()

        val callback = object : BaseNetworkCallback() {

            override fun onConnectivityChanged(newState: Boolean) {
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

    fun hasConnection(): Boolean {
        return if (isAtLeastMarshmallow()) {
            val capabilities = connectivityManager
                .getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            hasInternet && (hasCellular || hasWifi)
        } else {
            connectivityManager.activeNetworkInfo?.let { it.isConnected && it.isAvailable } ?: false
        }
    }

    private fun ConnectivityManager.unregisterNetworkCallbackSafe(callback: ConnectivityManager.NetworkCallback) {
        try {
            unregisterNetworkCallback(callback)
        } catch (ignored: IllegalArgumentException) {
        }
    }


    private class ConnectionLiveData : MutableLiveData<Boolean>(hasConnection()) {

        private val callback = object : BaseNetworkCallback() {

            override fun onConnectivityChanged(newState: Boolean) {
                //Методы могут вызываться не в Main потоке, поэтому post
                this@ConnectionLiveData.postValue(newState)
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
                postValue(hasConnection())
            }
            logger.d("registerNetworkCallback")
        }

        override fun onInactive() {
            super.onInactive()
            connectivityManager.unregisterNetworkCallbackSafe(callback)
            callback.activeNetworks.clear()
            logger.d("unregisterNetworkCallback")
        }

        override fun getValue(): Boolean {
            return super.getValue() ?: false
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