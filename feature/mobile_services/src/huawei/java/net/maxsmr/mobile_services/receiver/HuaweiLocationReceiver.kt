package net.maxsmr.mobile_services.receiver

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.huawei.hms.location.*
import net.maxsmr.core.android.location.receiver.LocationParams
import net.maxsmr.core.android.location.receiver.ILocationReceiver

/**
 * Huawei реализация [ILocationReceiver],
 * работающая при наличии соответствующих сервисов
 */
internal class HuaweiLocationReceiver(
    context: Context,
) : ILocationReceiver {

    override val lastKnownPosition: Location?
        get() = if (!fusedLocationClient.lastLocation.isComplete) {
            null
        } else {
            fusedLocationClient.lastLocation.result
        }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)!!
    override var locationCallback: net.maxsmr.core.android.location.LocationCallback? = null
        private set

    private val settingsClient = LocationServices.getSettingsClient(context)!!

    private val locationHuaweiCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult?.let {
                locationCallback?.onLocationChanged(it.lastLocation)
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability?) {
            locationCallback?.onLocationAvailabilityChanged(availability?.isLocationAvailable == true)
        }
    }

    override fun registerLocationUpdates(
        callback: net.maxsmr.core.android.location.LocationCallback,
        params: LocationParams,
        looper: Looper
    ) {
        if (isRegistered) {
            unregisterLocationUpdates()
        }
        locationCallback = callback
        val priority: Int = when (params.priority) {
            LocationParams.Priority.HIGH -> LocationRequest.PRIORITY_HIGH_ACCURACY
            LocationParams.Priority.BALANCED -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            else -> LocationRequest.PRIORITY_NO_POWER
        }
        val locationRequest =
            LocationRequest.create()
                .setInterval(params.interval)
                .setFastestInterval(params.interval / 3)
                .setSmallestDisplacement(params.minDistance.toFloat())
                .setPriority(priority)

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest).build()

        // если возможно проверить настройки через клиент - проверяем;
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(locationSettingsRequest)

        locationSettingsResponseTask.addOnSuccessListener {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationHuaweiCallback,
                looper)
        }
    }

    override fun unregisterLocationUpdates() {
        if (!isRegistered) return
        locationCallback = null
        fusedLocationClient.removeLocationUpdates(locationHuaweiCallback)
    }

}