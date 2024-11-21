package net.maxsmr.notification_reader.ui.activity

import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.notification_reader.data.NotificationReaderListenerService
import net.maxsmr.notification_reader.R

internal fun NavController.navigateWithGraphFragmentsFromCaller(
    callerClass: Class<*>,
    currentNavFragment: BaseNavigationFragment<*, *>?,
) {
    if (callerClass.isAssignableFrom(NotificationReaderListenerService::class.java)) {
        navigateWithGraphFragments(
            R.id.navigationNotificationReader,
            currentNavFragment,
        )
    }
}

internal fun NavController.navigateWithGraphFragments(
    item: MenuItem,
    currentNavFragment: BaseNavigationFragment<*, *>?,
): Boolean {
    return navigateWithGraphFragments(
        item.itemId,
        currentNavFragment
    )
}

internal fun NavController.navigateWithGraphFragments(
    destinationId: Int,
    currentNavFragment: BaseNavigationFragment<*, *>?,
): Boolean {
    val targetAction = {
        navigateWithGraphFragments(destinationId)
    }
    val selected = currentBackStackEntry?.destination?.hierarchy?.any {
        it.id == destinationId
    } ?: false
    return if (!selected
            && currentNavFragment?.canNavigate(targetAction) != false) {
        targetAction.invoke()
        true
    } else {
        false
    }
}

private fun NavController.navigateWithGraphFragments(destinationId: Int) {
    fun navOptions() = androidx.navigation.navOptions {
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
    navigate(resId = destinationId, args = null, navOptions = navOptions())
}