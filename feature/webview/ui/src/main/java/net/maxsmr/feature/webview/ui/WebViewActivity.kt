package net.maxsmr.feature.webview.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseNavigationActivity

@AndroidEntryPoint
class WebViewActivity : BaseNavigationActivity() {

    override val startDestinationArgs: Bundle?
        get() = intent?.extras

    override val navigationGraphResId: Int = R.navigation.navigation_webview
}

fun Context.webViewActivityIntent(
    customizer: WebViewCustomizer,
): Intent = Intent(this, WebViewActivity::class.java)
    .putExtras(WebViewFragmentArgs(customizer).toBundle())