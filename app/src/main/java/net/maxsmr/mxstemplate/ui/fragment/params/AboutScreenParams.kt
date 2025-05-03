package net.maxsmr.mxstemplate.ui.fragment.params

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import net.maxsmr.core.android.screen.ScreenParams
import net.maxsmr.core.android.screen.ScreenParamsParceler

@Parcelize
@Serializable
class AboutScreenParams(
    val isForRate: Boolean
): ScreenParams {

    companion object : ScreenParamsParceler<AboutScreenParams> {

        override val serializer = serializer()
    }
}