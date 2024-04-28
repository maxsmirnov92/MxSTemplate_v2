package net.maxsmr.mobile_services

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.api.HuaweiApiAvailability

class MobileServicesAvailability(context: Context) : IMobileServicesAvailability {

    override val isGooglePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS

    /**
     * Доступеность HMS в этом варианте сборке
     */
    override val isHuaweiApiServicesAvailable =
        HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) == ConnectionResult.SUCCESS
}