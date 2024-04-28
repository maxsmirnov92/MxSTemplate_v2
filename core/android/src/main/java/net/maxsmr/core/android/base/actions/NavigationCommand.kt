package net.maxsmr.core.android.base.actions

import android.os.Bundle
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

sealed class NavigationCommand {

    abstract class ToDirection: NavigationCommand() {

        abstract val navOptions: NavOptions?
        abstract val navigatorExtras: Navigator.Extras?
    }

    data class ToDirectionWithNavDirections(
        // сгенерированный NavDirections предпочтительнее,
        // т.к. исключает неверную комбинацию actionId + Bundle
        val directions: NavDirections,
        override val navOptions: NavOptions? = null,
        override val navigatorExtras: Navigator.Extras?  = null
    ): ToDirection()

    data class ToDirectionWithRoute(
        val route: String,
        override val navOptions: NavOptions? = null,
        override val navigatorExtras: Navigator.Extras?  = null
    ): ToDirection()

    data object Back : NavigationCommand()
}