package net.maxsmr.mxstemplate.session

import net.maxsmr.core.network.SessionStorage
import net.maxsmr.mxstemplate.BuildConfig


class RadarIoSessionStorage() : SessionStorage {

    override var session: String?
        get() = BuildConfig.AUTHORIZATION_RADAR_IO
        set(value) {
            throw UnsupportedOperationException("Change session is not supported")
        }


    override fun clear() {
        throw UnsupportedOperationException("Clear session is not supported")
    }
}