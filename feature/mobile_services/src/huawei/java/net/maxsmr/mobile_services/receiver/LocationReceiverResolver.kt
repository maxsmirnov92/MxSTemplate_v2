package net.maxsmr.mobile_services.receiver

import android.content.Context
import net.maxsmr.core.android.location.receiver.ILocationReceiver
import net.maxsmr.mobile_services.IMobileServicesAvailability

class LocationReceiverResolver(context: Context, mobileServicesAvailability: IMobileServicesAvailability) :
        BaseLocationReceiverResolver(context, mobileServicesAvailability) {

    override fun systemLocationReceiver(): ILocationReceiver = SystemLocationReceiver(context)
    override fun huaweiLocationReceiver(): ILocationReceiver = HuaweiLocationReceiver(context)
    override fun googleLocationReceiver(): ILocationReceiver = GoogleLocationReceiver(context)
}