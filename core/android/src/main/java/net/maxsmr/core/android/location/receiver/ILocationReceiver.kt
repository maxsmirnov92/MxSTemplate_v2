package net.maxsmr.core.android.location.receiver

import android.location.Location
import android.os.Looper
import net.maxsmr.core.android.location.LocationCallback

/**
 * Супертип, определяющий поведение делегатов для получения геопозиции
 *
 */
interface ILocationReceiver {

    val lastKnownPosition: Location?

    val locationCallback: LocationCallback?

    val isRegistered: Boolean get() = locationCallback != null

    fun registerLocationUpdates(
        callback: LocationCallback,
        params: LocationParams,
        looper: Looper,
    )

    fun unregisterLocationUpdates()
}
