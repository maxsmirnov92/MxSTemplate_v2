package net.maxsmr.core.android.location.receiver

import android.location.Location
import android.os.Looper
import net.maxsmr.core.android.location.LocationCallback

class MockReceiver : ILocationReceiver {

    override val lastKnownPosition: Location? = null

    override val locationCallback: LocationCallback? = null

    override fun registerLocationUpdates(callback: LocationCallback, params: LocationParams, looper: Looper) {
    }

    override fun unregisterLocationUpdates() {
    }
}