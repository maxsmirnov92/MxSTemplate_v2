package net.maxsmr.mxstemplate.ui.activity

import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
import kotlinx.coroutines.launch
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.ui.fragment.MainDownloadsPagerFragmentDirections
import net.maxsmr.mxstemplate.ui.fragment.params.WebViewScreenParams
import net.maxsmr.mxstemplate.ui.getViewUrlStrategy

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
    val selected = currentBackStackEntry?.destination?.hierarchy?.any {
        it.id == destinationId
    } ?: false
    return if (!selected
            && currentNavFragment?.canNavigate(false, targetAction) != false
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
            // 1. при saveState == true:
            // onSaveInstanceState будет вызван в т.ч. на дестроящемся фрагменте;
            // на VM фрагмента, с которого уходим, не будет вызван onCleared;
            // 2. при saveState == false:
            // onSaveInstanceState - не будет вызван (только если свернуть), onCleared - будет
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
        // а на предыдущем не вызван onDestroy (если только не попадает в popUpTo)
    }

    if (destinationId == R.id.navigationWebView) {
        lifecycleScope.launch {
            val settings = settingsRepo.getSettings()
            navigate(
                MainDownloadsPagerFragmentDirections.actionToWebViewFragment(
                    WebViewScreenParams(
                        WebViewCustomizer.Builder()
                            .setUrl(settings.startPageUrl)
                            .setViewUrlStrategy(settings.getViewUrlStrategy())
                            .build()
                    )
                ),
                navOptions = navOptions()
            )
        }
    } else {
        navigate(resId = destinationId, args = null, navOptions = navOptions())
    }
}