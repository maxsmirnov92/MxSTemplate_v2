package net.maxsmr.core.android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException.PreferableType
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер доступности сетевого подключения.
 * Получение текущего значения - [hasConnection], подписка - [asFlow]/[asStatusLiveData]
 */
@Singleton
class NetworkStateManager @Inject constructor(@ApplicationContext context: Context) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(NetworkStateManager::class.java)

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val connectionLiveData: ConnectionLiveData by lazy { ConnectionLiveData() }

    fun asFlow(): Flow<ConnectionInfo> = callbackFlow {

        val callback = object : BaseNetworkCallback() {

            override fun onConnectivityChanged(newInfo: ConnectionInfo) {
                trySend(newInfo)
            }
        }

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().run {
            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            build()
        }, callback)

        if (callback.activeNetworks.isEmpty()) {
            trySend(getConnectionInfo())
        }
        logger.d("NetworkCallback registered")

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
            logger.d("NetworkCallback unregistered")
        }
    }/*.stateIn(scope, SharingStarted.WhileSubscribed(5_000), getConnectionInfo())*/

    fun asStatusFlow(): Flow<Boolean> = asFlow().map { it.has }

    @Deprecated("", replaceWith = ReplaceWith(expression = "asFlow"))
    fun asLiveData(): LiveData<ConnectionInfo> = connectionLiveData

    @Deprecated("", replaceWith = ReplaceWith(expression = "asStatusFlow"))
    fun asStatusLiveData() = connectionLiveData.map { it.has }

    fun hasPreferableConnection(types: Set<PreferableType>): Boolean {
        var hasPreferableConnection = true
        val connectionInfo = getConnectionInfo()
        if (connectionInfo.has &&
                (connectionInfo.hasWiFi != null || connectionInfo.hasCellular != null)
                && types.isNotEmpty()
        ) {
            // предпочтительные типы указаны и в API информация возвращается
            hasPreferableConnection = false
            run breaking@{
                types.forEach {
                    when (it) {
                        PreferableType.CELLULAR -> if (connectionInfo.hasCellular == true) {
                            hasPreferableConnection = true
                            return@breaking
                        }

                        PreferableType.WIFI -> if (connectionInfo.hasWiFi == true) {
                            hasPreferableConnection = true
                            return@breaking
                        }
                    }
                }
            }
        }
        return hasPreferableConnection
    }

    fun hasConnection() = getConnectionInfo().has

    fun getConnectionInfo(): ConnectionInfo {
        val capabilities = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork) ?: return ConnectionInfo()
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        return ConnectionInfo(hasInternet, hasInternet && hasCellular, hasInternet && hasWifi)
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

    private inner class ConnectionLiveData : MutableLiveData<ConnectionInfo>(getConnectionInfo()) {

        private val callback = object : BaseNetworkCallback() {

            override fun onConnectivityChanged(newInfo: ConnectionInfo) {
                //Методы могут вызываться не в Main потоке, поэтому post
                this@ConnectionLiveData.postValue(newInfo)
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
                postValue(getConnectionInfo())
            }
            logger.d("NetworkCallback registered")
        }

        override fun onInactive() {
            super.onInactive()
            connectivityManager.unregisterNetworkCallbackSafe(callback)
            callback.activeNetworks.clear()
            logger.d("NetworkCallback unregistered")
        }

        override fun getValue(): ConnectionInfo {
            return super.getValue() ?: ConnectionInfo()
        }
    }

    private abstract inner class BaseNetworkCallback : ConnectivityManager.NetworkCallback() {

        val activeNetworks: MutableSet<Network> = mutableSetOf()

        val hasActiveNetworks: Boolean get() = activeNetworks.isNotEmpty()

        private var lastInfo: ConnectionInfo? = null

        final override fun onAvailable(network: Network) {
            super.onAvailable(network)
            synchronized(activeNetworks) {
                activeNetworks.add(network)
                logger.d("Connection $network available. Active: ${activeNetworks.joinToString()}")
                onNewConnectivity()
            }
        }

        final override fun onLost(network: Network) {
            super.onLost(network)
            synchronized(activeNetworks) {
                activeNetworks.remove(network)
                logger.d("Connection $network lost. Active: ${activeNetworks.joinToString()}")
                onNewConnectivity()
            }
        }

        // не главный поток
        abstract fun onConnectivityChanged(newInfo: ConnectionInfo)

        private fun onNewConnectivity() {
            val newInfo = if (hasActiveNetworks) {
                // значение отсюда может быть с опозданием:
                // например, включен Wi-Fi поверх мобильной сети -
                // нужно ждать более поздний onLost
                getConnectionInfo()
            } else {
                ConnectionInfo(false)
            }
            if (lastInfo != newInfo) {
                logger.d("Connectivity changed from '$lastInfo to '$newInfo'")
                this.lastInfo = newInfo
                onConnectivityChanged(newInfo)
            }
        }
    }
}