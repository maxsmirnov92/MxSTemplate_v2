package net.maxsmr.mxstemplate.ui.activity

import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.android.network.isAnyNetScheme
import net.maxsmr.core.ui.components.activities.BaseNavigationActivity
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.ui.DownloadableWebViewModel.Companion.ARG_WEB_CUSTOMIZER
import net.maxsmr.mxstemplate.ui.fragment.DownloadableWebViewFragmentDirections
import javax.inject.Inject

@AndroidEntryPoint
class BrowserActivity : BaseNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_browser

    override val startDestinationArgs: Bundle?
        get() {
            return intent.toWebViewCustomizer()?.let { customizer ->
                Bundle().apply {
                    putSerializable(ARG_WEB_CUSTOMIZER, customizer)
                }
            } ?: super.startDestinationArgs.also {
                finish()
            }
        }

    @Inject
    lateinit var settingsRepo: SettingsDataStoreRepository

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toWebViewCustomizer()?.let {
            setIntent(intent)
            navController.navigate(
                DownloadableWebViewFragmentDirections.actionToWebViewFragment(it)
            )
        }
    }

    private fun Intent?.toWebViewCustomizer(): WebViewCustomizer? {
        val uri = this?.data
        val scheme = this?.scheme
        val url = if (uri == null || !scheme.isAnyNetScheme()) {
            if (this?.action == Intent.ACTION_MAIN) {
                // стартовая урла, если был предполагаемый запуск из лаунчера
                runBlocking { settingsRepo.getSettings().startPageUrl }
            } else {
                null
            }
        } else {
            uri.toString()
        }
        return if (url != null) {
            WebViewCustomizer.Builder().setUrl(url).build()
        } else {
            null
        }
    }
}