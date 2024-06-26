package net.maxsmr.mxstemplate.ui.activity

import android.view.MenuItem
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.download.ui.DownloadsPagerFragmentDirections
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.R

internal fun NavController.navigateWithMenuFragments(
    item: MenuItem,
    lifecycleScope: LifecycleCoroutineScope,
    settingsRepo: SettingsDataStoreRepository,
    currentNavDestinationId: Int,
    currentNavFragment: BaseNavigationFragment<*>?,
): Boolean {
    val targetAction = {
        navigateWithMenuFragments(item, lifecycleScope, settingsRepo)
    }
    return if (item.itemId != currentNavDestinationId
            && currentNavFragment?.canNavigate(targetAction) != false
    ) {
        targetAction.invoke()
        true
    } else {
        false
    }
}

private fun NavController.navigateWithMenuFragments(
    item: MenuItem,
    lifecycleScope: LifecycleCoroutineScope,
    settingsRepo: SettingsDataStoreRepository,
) {
    if (item.itemId == R.id.navigationWebView) {
        lifecycleScope.launch {
            // FIXME за счёт этого криво будет работать navigateUp с других фрагментов на WebView
            navigate(
                DownloadsPagerFragmentDirections.actionToWebViewFragment(
                    WebViewCustomizer.Builder()
                        .setUrl(settingsRepo.getSettings().startPageUrl)
                        .setCanInputUrls(true)
                        .build()
                )
            )
        }
    } else {
        navigate(item.itemId)
    }
}