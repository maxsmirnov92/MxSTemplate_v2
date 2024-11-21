package net.maxsmr.notification_reader.ui.activity

import android.view.MenuItem
import androidx.navigation.NavController
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment

internal fun NavController.navigateWithGraphFragments(
    item: MenuItem,
    currentNavFragment: BaseNavigationFragment<*>?,
): Boolean {
    return navigateWithGraphFragments(
        item.itemId,
        currentNavFragment
    )
}

internal fun NavController.navigateWithGraphFragments(
    destinationId: Int,
    currentNavFragment: BaseNavigationFragment<*>?,
): Boolean {
    val targetAction = {
        navigateWithGraphFragments(destinationId)
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

private fun NavController.navigateWithGraphFragments(destinationId: Int) {
    navigate(destinationId)
}