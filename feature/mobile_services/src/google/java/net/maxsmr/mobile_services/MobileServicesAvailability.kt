package net.maxsmr.mobile_services

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class MobileServicesAvailability(context: Context) : IMobileServicesAvailability {

    /**
     * Доступность GMS в этом варианте сборке
     */
    override val isGooglePlayServicesAvailable =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    /**
     * HMS исключен из сборки **этого модуля** для Google play
     */
    override val isHuaweiApiServicesAvailable = false

}