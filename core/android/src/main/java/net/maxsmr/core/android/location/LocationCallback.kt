package net.maxsmr.core.android.location

import android.content.Context
import android.location.Location
import net.maxsmr.commonutils.location.hasLocationFeature
import net.maxsmr.commonutils.location.isLocationProviderEnabled

interface LocationCallback {

    fun onLocationChanged(location: Location)

    fun onLocationAvailabilityChanged(isAvailable: Boolean)

    fun onLocationNotSupported()

    fun onLocationProviderNotEnabled()

    fun checkLocationEnabled(
        context: Context,
        withGpsOnly: Boolean,
        withNotify: Boolean = true
    ): Boolean {
        if (!hasLocationFeature(withGpsOnly, context)) {
            if (withNotify) {
                onLocationNotSupported()
            }
            return false
        } else if (!isLocationProviderEnabled(withGpsOnly, context)) {
            if (withNotify) {
                onLocationProviderNotEnabled()
            }
            return false
        }
        return true
    }
}
