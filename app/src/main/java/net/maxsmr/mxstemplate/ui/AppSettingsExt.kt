package net.maxsmr.mxstemplate.ui

import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.feature.webview.data.client.ExternalViewUrlWebViewClient.ViewUrlMode
import net.maxsmr.feature.webview.ui.WebViewCustomizer.ViewUrlStrategy

fun AppSettings.getViewUrlStrategy(): ViewUrlStrategy {
    return ViewUrlStrategy(
        if (openLinksInExternalApps) {
            ViewUrlMode.NON_BROWSER
        } else {
            ViewUrlMode.INTERNAL
        }
    )
}