package net.maxsmr.core.ui.components.activities

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import net.maxsmr.core.ui.R

/**
 * Реализация [BaseNavigationActivity] с DrawerLayout и NavigationView в разметке
 */
abstract class BaseDrawerNavigationActivity: BaseNavigationActivity() {

    @LayoutRes
    override val contentViewResId: Int = R.layout.activity_navigation_drawer

    @IdRes
    protected open val navigationViewResId: Int = R.id.navigation_view

    @IdRes
    protected open val drawerLayoutResId: Int = R.id.drawer_layout

    protected val navigationView: NavigationView by lazy {
        findViewById(navigationViewResId)
    }

    protected val drawerLayout: DrawerLayout by lazy {
        findViewById(drawerLayoutResId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNavigationView()
    }

    override fun createAppBarConfiguration(): AppBarConfiguration {
        return AppBarConfiguration(navController.graph, drawerLayout)
    }

    @CallSuper
    protected open fun setupNavigationView() {
        navigationView.setupWithNavController(navController)
    }
}