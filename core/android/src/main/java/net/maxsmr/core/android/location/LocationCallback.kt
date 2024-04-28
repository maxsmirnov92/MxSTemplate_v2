package net.maxsmr.core.android.location

import android.content.Context
import android.location.Location
import net.maxsmr.commonutils.location.isGpsAvailable
import net.maxsmr.commonutils.location.isGpsEnabled

interface LocationCallback {

    fun onLocationChanged(location: Location)

    fun onLocationAvailabilityChanged(isAvailable: Boolean)

    fun onGpsNotAvailable()

    fun onGpsProviderNotEnabled()

    fun checkLocationEnabled(context: Context, isGpsOnly: Boolean, checkOnly: Boolean = false): Boolean {
        if (!isGpsAvailable(isGpsOnly, context)) {
            if (!checkOnly) {
                onGpsNotAvailable()
            }
            return false
        } else if (!isGpsEnabled(isGpsOnly, context)) {
            if (!checkOnly) {
                onGpsProviderNotEnabled()
            }
            return false
        }
        return true
    }
}
