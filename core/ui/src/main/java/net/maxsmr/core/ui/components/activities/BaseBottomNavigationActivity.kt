package net.maxsmr.core.ui.components.activities

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.maxsmr.core.ui.R

/**
 * Реализация [BaseNavigationActivity] с BottomNavigationView в разметке
 */
abstract class BaseBottomNavigationActivity: BaseNavigationActivity() {

    protected abstract val topLevelDestinationIds: Set<Int>

    @LayoutRes
    override val contentViewResId: Int = R.layout.activity_navigation_bottom

    @IdRes
    protected open val bottomNavigationViewResId: Int = R.id.bottom_nav_view

    protected val bottomNavigationView: BottomNavigationView by lazy {
        findViewById(bottomNavigationViewResId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBottomNavigationView()
    }

    override fun createAppBarConfiguration(): AppBarConfiguration {
//        return AppBarConfiguration(menuInflater.inflate(topLevelMenuResId))
        return AppBarConfiguration(topLevelDestinationIds)
    }

    @CallSuper
    protected open fun setupBottomNavigationView() {
        bottomNavigationView.setupWithNavController(navController)
    }
}