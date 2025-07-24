package net.maxsmr.mobile_services.receiver

import android.content.Context
import net.maxsmr.core.android.location.receiver.BaseLocationReceiver
import net.maxsmr.mobile_services.IMobileServicesAvailability

class LocationReceiverResolver(
    override val context: Context,
    override val mobileServicesAvailability: IMobileServicesAvailability,
) : BaseLocationReceiverResolver {

    override fun huaweiLocationReceiver(): LocationReceiver = HuaweiLocationReceiver(context)
    override fun googleLocationReceiver(): LocationReceiver = GoogleLocationReceiver(context)
}