package net.maxsmr.mobile_services.receiver

import android.content.Context
import net.maxsmr.core.android.location.receiver.ILocationReceiver
import net.maxsmr.mobile_services.IMobileServicesAvailability

abstract class BaseLocationReceiverResolver(
    protected val context: Context,
    private val mobileServicesAvailability: IMobileServicesAvailability
) {

    protected abstract fun systemLocationReceiver(): ILocationReceiver

    protected abstract fun huaweiLocationReceiver(): ILocationReceiver

    protected abstract fun googleLocationReceiver(): ILocationReceiver

    fun resolve(): ILocationReceiver {
        return when {
            mobileServicesAvailability.isGooglePlayServicesAvailable -> googleLocationReceiver()
            mobileServicesAvailability.isHuaweiApiServicesAvailable -> huaweiLocationReceiver()
            else -> systemLocationReceiver()
        }
    }
}