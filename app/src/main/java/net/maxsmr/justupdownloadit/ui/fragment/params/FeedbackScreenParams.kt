package net.maxsmr.justupdownloadit.ui.fragment.params

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import net.maxsmr.core.android.screen.ScreenParams
import net.maxsmr.core.android.screen.ScreenParamsParceler

@Parcelize
@Serializable
class FeedbackScreenParams(
    val emailAddress: String,
    val shouldNavigateToMarket: Boolean,
) : ScreenParams {

    companion object : ScreenParamsParceler<FeedbackScreenParams> {

        override val serializer = serializer()
    }
}