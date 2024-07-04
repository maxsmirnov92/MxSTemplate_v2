package net.maxsmr.justupdownloadit.ui.fragment.params

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import net.maxsmr.core.android.screen.ScreenParams
import net.maxsmr.core.android.screen.ScreenParamsParceler
import net.maxsmr.feature.webview.ui.WebViewCustomizer

@Parcelize
@Serializable
class WebViewScreenParams(
    val customizer: WebViewCustomizer
): ScreenParams {

    companion object : ScreenParamsParceler<WebViewScreenParams> {

        override val serializer = serializer()
    }
}