package net.maxsmr.feature.webview.data.client

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import net.maxsmr.commonutils.getViewUrlIntent
import net.maxsmr.commonutils.isAtLeastMarshmallow
import net.maxsmr.commonutils.queryIntentActivitiesCompat
import net.maxsmr.feature.webview.data.client.interceptor.IWebViewInterceptor
import net.maxsmr.feature.webview.data.client.interceptor.WebViewInterceptor
import okhttp3.OkHttpClient

@Deprecated("not working, use clients with FLAG_ACTIVITY_REQUIRE_NON_BROWSER")
class NonBrowserWebViewClient(
    context: Context,
    okHttpClient: OkHttpClient? = null,
    webViewInterceptor: IWebViewInterceptor = WebViewInterceptor(),
) : ExternalViewUrlWebViewClient(context, okHttpClient, webViewInterceptor) {

    override fun getViewUrlMode(url: String): ViewUrlMode {
        val flags = if (isAtLeastMarshmallow()) {
            PackageManager.MATCH_ALL
        } else {
            0
        }
        val browsableResults = context.queryIntentActivitiesCompat(
            getViewUrlIntent(url, null)
                .addCategory(Intent.CATEGORY_BROWSABLE),
            flags
        )

        var result = ViewUrlMode.INTERNAL

        if (browsableResults.isNotEmpty()) { // filter и metaData будут null
            if (!browsableResults.all {
                        val packageBrowserResults = context.queryIntentActivitiesCompat(
                            getViewUrlIntent(url, null)
                                .addCategory("android.intent.category.APP_BROWSER")
                                .setPackage(it.activityInfo.packageName),
                            flags
                        )
                        // скорее всего ничего не найдёт, хотя такие activity (как и с CATEGORY_LAUNCHER)
                        // точно есть в системе
                        packageBrowserResults.isNotEmpty()
                    }) {
                result = ViewUrlMode.EXTERNAL
            }
        }

        return result
    }
}