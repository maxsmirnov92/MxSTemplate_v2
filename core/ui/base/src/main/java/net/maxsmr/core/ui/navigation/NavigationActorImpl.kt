package net.maxsmr.core.ui.navigation

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.base.actions.NavigationAction.NavigationCommand

class NavigationActorImpl(
    private val activity: Activity,
    private val navController: NavController
) : NavigationAction.INavigationActor {

    constructor(fragment: Fragment): this(
        fragment.requireActivity(),
        fragment.findNavController()
    )

    override fun doNavigate(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.ToDirectionWithNavDirections -> navController.navigate(
                command.directions.actionId,
                command.directions.arguments,
                command.navOptions,
                command.navigatorExtras,
            )

            is NavigationCommand.ToDirectionWithRoute -> navController.navigate(
                command.route,
                command.navOptions,
                command.navigatorExtras,
            )

            is NavigationCommand.Back -> {
//                activity.onBackPressed()
                navigateUp()
            }

            else -> {
                throw IllegalArgumentException("Unknown command: $command")
            }
        }
    }

    fun navigateUp() {
        if (!navController.navigateUp()) { // (requireActivity() as BaseNavigationActivity).appBarConfiguration
            activity.finish()
        }
    }
}