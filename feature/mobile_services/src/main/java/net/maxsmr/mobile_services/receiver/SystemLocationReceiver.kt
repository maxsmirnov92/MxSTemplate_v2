package net.maxsmr.mobile_services.receiver

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.LocationManager.*
import android.os.Looper
import androidx.core.location.LocationListenerCompat
import net.maxsmr.core.android.location.LocationCallback
import net.maxsmr.core.android.location.receiver.ILocationReceiver
import net.maxsmr.core.android.location.receiver.LocationParams
import net.maxsmr.core.android.location.receiver.LocationParams.Priority.HIGH
import net.maxsmr.core.android.location.receiver.LocationParams.Priority.PASSIVE

/**
 * Инкапсулирует получение геопозиции через [LocationManager]
 */
@SuppressLint("MissingPermission")
internal class SystemLocationReceiver(context: Context) : ILocationReceiver {

    override var locationCallback: LocationCallback? = null
        private set

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val systemLocationListener = LocationListenerCompat { location ->
        // дополнительно можно сравнивать с точностью последней
        // и проверять по интервалу
        locationCallback?.onLocationChanged(location)
    }

    @Throws(SecurityException::class)
    override fun registerLocationUpdates(callback: LocationCallback, params: LocationParams, looper: Looper) {
        if (isRegistered) {
            unregisterLocationUpdates()
        }
        this.locationCallback = callback
        locationManager.requestLocationUpdates(
            getBestProvider(params.priority),
            params.interval,
            params.minDistance.toFloat(),
            systemLocationListener,
            looper
        )
    }

    @Throws(SecurityException::class)
    override fun unregisterLocationUpdates() {
        if (!isRegistered) return
        locationManager.removeUpdates(systemLocationListener)
    }

    override val lastKnownPosition: Location?
        get() {

            fun getLastKnownLocation(provider: String): Location? {
                if (!locationManager.isProviderEnabled(provider)) {
                    return null
                }
                return try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    null
                }
            }

            var result: Location? = null

            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                val location = getLastKnownLocation(provider)
                if (location != null && (result == null || result.accuracy < location.accuracy)) {
                    result = location
                }
            }
            return result
        }

    private fun getBestProvider(priority: LocationParams.Priority) = when {
        priority === PASSIVE -> PASSIVE_PROVIDER
        priority === HIGH && isProviderEnabled(GPS_PROVIDER) -> GPS_PROVIDER
        priority === HIGH && isProviderEnabled(NETWORK_PROVIDER) -> NETWORK_PROVIDER
        isProviderEnabled(NETWORK_PROVIDER) -> NETWORK_PROVIDER
        isProviderEnabled(GPS_PROVIDER) -> GPS_PROVIDER
        else -> PASSIVE_PROVIDER
    }

    private fun isProviderEnabled(provider: String) = locationManager.isProviderEnabled(provider)
}