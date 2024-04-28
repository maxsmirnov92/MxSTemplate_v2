package net.maxsmr.mobile_services.receiver

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import net.maxsmr.core.android.location.receiver.LocationParams
import net.maxsmr.core.android.location.receiver.ILocationReceiver

/**
 * Google реализация [ILocationReceiver],
 * работающая при наличии соответствующих сервисов
 */
internal class GoogleLocationReceiver(
    context: Context,
) : ILocationReceiver {

    override val lastKnownPosition: Location?
        @SuppressLint("MissingPermission")
        get() = if (!fusedLocationClient.lastLocation.isComplete) {
            null
        } else {
            fusedLocationClient.lastLocation.result
        }

    override var locationCallback: net.maxsmr.core.android.location.LocationCallback? = null
        private set

    private val settingsClient = LocationServices.getSettingsClient(context)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationGoogleCallback = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                locationCallback?.onLocationChanged(it)
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            locationCallback?.onLocationAvailabilityChanged(availability.isLocationAvailable)
        }
    }

    @SuppressLint("MissingPermission")
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
            LocationParams.Priority.HIGH -> Priority.PRIORITY_HIGH_ACCURACY
            LocationParams.Priority.BALANCED -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            else -> Priority.PRIORITY_PASSIVE
        }
        val locationRequest = LocationRequest.Builder(priority, params.interval)
            .setMinUpdateIntervalMillis(params.interval / 3)
            .setMinUpdateDistanceMeters(params.minDistance.toFloat())
            .build()

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest).build()

        // если возможно проверить настройки через клиент - проверяем;
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(locationSettingsRequest)

        locationSettingsResponseTask.addOnSuccessListener {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                locationGoogleCallback,
                looper)
        }
    }

    override fun unregisterLocationUpdates() {
        if (!isRegistered) return
        locationCallback = null
        fusedLocationClient.removeLocationUpdates(locationGoogleCallback)
    }
}