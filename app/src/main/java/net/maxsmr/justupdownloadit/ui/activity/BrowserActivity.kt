package net.maxsmr.justupdownloadit.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.ui.components.activities.BaseNavigationActivity
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.feature.webview.ui.WebViewCustomizer.ExternalViewUrlStrategy
import net.maxsmr.justupdownloadit.App
import net.maxsmr.justupdownloadit.R
import net.maxsmr.justupdownloadit.ui.BrowserWebViewModel.Companion.ARG_WEB_CUSTOMIZER
import net.maxsmr.justupdownloadit.ui.fragment.BrowserWebViewFragmentDirections
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent.toWebViewCustomizer()?.let {
            navController.navigate(
                BrowserWebViewFragmentDirections.actionToWebViewFragment(it)
            )
        }
    }

    private fun Intent?.toWebViewCustomizer(): WebViewCustomizer? {
        val settings = runBlocking { settingsRepo.getSettings() }
        val uri: Uri? = this?.data ?: if (this?.action == Intent.ACTION_MAIN) {
            // стартовая урла, если был предполагаемый запуск из лаунчера
            settings.startPageUrl.toUri()
        } else {
            null
        }
        return if (uri != null) {
            WebViewCustomizer.Builder()
                .setUri(uri)
                .setViewUrlStrategy(
                    if (settings.openLinksInExternalApps) {
                        ExternalViewUrlStrategy.NonBrowserFirst
                    } else {
                        ExternalViewUrlStrategy.None
                    }
                )
                .build()
        } else {
            null
        }
    }
}