package net.maxsmr.mxstemplate.ui

import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.core.network.URL_SCHEME_HTTP
import net.maxsmr.core.network.URL_SCHEME_HTTPS
import net.maxsmr.feature.webview.data.client.ExternalViewUrlWebViewClient.ViewUrlMode
import net.maxsmr.feature.webview.ui.WebViewCustomizer.ViewUrlStrategy

fun AppSettings.getViewUrlStrategy(): ViewUrlStrategy {
    return if (openLinksInExternalApps) {
        // этот апп умеет обрабатывать кастомные схемы (vk) и сам является браузером,
        // но при этом не надо открывать http/https во внешних аппах,
        ViewUrlStrategy(
            ViewUrlMode.NON_BROWSER_EXTERNAL,
            ViewUrlStrategy.UrlMatcher.PartsUrlMatcher(
                schemes = listOf(URL_SCHEME_HTTPS, URL_SCHEME_HTTP) to false
            )
        )
    } else {
        ViewUrlStrategy(ViewUrlMode.INTERNAL)
    }
}