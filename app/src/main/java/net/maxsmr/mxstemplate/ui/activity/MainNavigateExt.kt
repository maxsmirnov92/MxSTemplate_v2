package net.maxsmr.mxstemplate.ui.activity

import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
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
            currentNavFragment,
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
    fun navOptions() = navOptions {
        // убирает все до startDestinationId, на них сработает onDestroy
        popUpTo(graph.findStartDestination().id) { // startDestinationId
            saveState = true
        }
        // проверка currentNavDestinationId уже была
        launchSingleTop = true
        restoreState = true
        // при navigate с saveState + restoreState будет переиспользоваться тот же инстанс фрагмента и VM,
        // но на нём также при уходе будет вызываться onDestroyView, а при переходе onCreateView
        // (т.е. viewLifecycleOwner в любом случае другой);
        // если один из флагов false - каждый раз будет новый инстанс (в т.ч. VM, которая by viewModels):
        // при этом на новом будет вызван onCreate,
        // а на предыдущем не вызван onDestroy (если только не попадает в popupTo)
    }

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
                ),
                navOptions = navOptions()
            )
        }
    } else {
        navigate(resId = destinationId, args = null, navOptions = navOptions())
    }
}