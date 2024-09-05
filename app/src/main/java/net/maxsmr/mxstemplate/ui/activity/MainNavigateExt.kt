package net.maxsmr.mxstemplate.ui.activity

import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.feature.webview.ui.WebViewCustomizer.ExternalViewUrlStrategy
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.ui.fragment.MainDownloadsPagerFragmentDirections

internal fun NavController.navigateWithGraphFragmentsFromCaller(
    callerClass: Class<*>,
    lifecycleScope: LifecycleCoroutineScope,
    settingsRepo: SettingsDataStoreRepository,
    currentNavFragment: BaseNavigationFragment<*>?,
) {
    if (callerClass.isAssignableFrom(DownloadService::class.java)) {
        navigateWithGraphFragments(
            R.id.navigationDownloads,
            lifecycleScope,
            settingsRepo,
            currentNavFragment
        )
    }
}

internal fun NavController.navigateWithGraphFragments(
    item: MenuItem,
    lifecycleScope: LifecycleCoroutineScope,
    settingsRepo: SettingsDataStoreRepository,
    currentNavFragment: BaseNavigationFragment<*>?,
): Boolean {
    return navigateWithGraphFragments(
        item.itemId,
        lifecycleScope,
        settingsRepo,
        currentNavFragment
    )
}

internal fun NavController.navigateWithGraphFragments(
    @IdRes destinationId: Int,
    lifecycleScope: LifecycleCoroutineScope,
    settingsRepo: SettingsDataStoreRepository,
    currentNavFragment: BaseNavigationFragment<*>?,
): Boolean {
    val targetAction = {
        navigateWithGraphFragments(destinationId, lifecycleScope, settingsRepo)
    }
    val currentNavDestinationId = currentDestination?.id
    return if (destinationId != currentNavDestinationId
            && currentNavFragment?.canNavigate(targetAction) != false
    ) {
        targetAction.invoke()
        true
    } else {
        false
    }
}

private fun NavController.navigateWithGraphFragments(
    @IdRes destinationId: Int,
    lifecycleScope: LifecycleCoroutineScope,
    settingsRepo: SettingsDataStoreRepository,
) {
    if (destinationId == R.id.navigationWebView) {
        lifecycleScope.launch {
            val settings = settingsRepo.getSettings()
            navigate(
                MainDownloadsPagerFragmentDirections.actionToWebViewFragment(
                    WebViewCustomizer.Builder()
                        .setUrl(settings.startPageUrl)
                        .setViewUrlStrategy(
                            if (settings.openLinksInExternalApps) {
                                ExternalViewUrlStrategy.NonBrowserFirst
                            } else {
                                ExternalViewUrlStrategy.None
                            }
                        )
                        .build()
                )
            )
        }
    } else {
        navigate(destinationId)
    }
}