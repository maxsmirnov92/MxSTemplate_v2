package net.maxsmr.mobile_services.receiver

import android.content.Context
import net.maxsmr.core.android.location.receiver.LocationReceiver
import net.maxsmr.mobile_services.IMobileServicesAvailability

interface BaseLocationReceiverResolver {

    val context: Context
    val mobileServicesAvailability: IMobileServicesAvailability

    fun huaweiLocationReceiver(): LocationReceiver

    fun googleLocationReceiver(): LocationReceiver

    fun systemLocationReceiver(): LocationReceiver = SystemLocationReceiver(context)

    fun resolve(): LocationReceiver {
        return when {
            mobileServicesAvailability.isGooglePlayServicesAvailable -> googleLocationReceiver()
            mobileServicesAvailability.isHuaweiApiServicesAvailable -> huaweiLocationReceiver()
            else -> systemLocationReceiver()
        }
    }
}