package net.maxsmr.core.android.location

import android.content.Context
import android.location.Location
import net.maxsmr.commonutils.location.hasLocationFeature
import net.maxsmr.commonutils.location.isLocationProviderEnabled

interface LocationCallback {

    fun onLocationChanged(location: Location)

    fun onLocationAvailabilityChanged(isAvailable: Boolean)

    fun onGpsNotAvailable()

    fun onGpsProviderNotEnabled()

    fun checkLocationEnabled(
        context: Context,
        withGpsOnly: Boolean,
        withNotify: Boolean = false
    ): Boolean {
        if (!hasLocationFeature(withGpsOnly, context)) {
            if (!withNotify) {
                onGpsNotAvailable()
            }
            return false
        } else if (!isLocationProviderEnabled(withGpsOnly, context)) {
            if (!withNotify) {
                onGpsProviderNotEnabled()
            }
            return false
        }
        return true
    }
}
