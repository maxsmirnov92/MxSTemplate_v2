package net.maxsmr.mobile_services

interface IMobileServicesAvailability {

    /**
     * Доступен любой сервис
     */
    val isAnyServiceAvailable: Boolean
        get() = isGooglePlayServicesAvailable || isHuaweiApiServicesAvailable

    /**
     * GMS доступен
     */
    val isGooglePlayServicesAvailable: Boolean

    /**
     * HMS доступен
     */
    val isHuaweiApiServicesAvailable: Boolean

}