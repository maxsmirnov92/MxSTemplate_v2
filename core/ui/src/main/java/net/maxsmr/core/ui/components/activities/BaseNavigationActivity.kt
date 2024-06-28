package net.maxsmr.core.ui.components.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.NavigationRes
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import net.maxsmr.commonutils.isAtLeastUpsideDownCake
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.components.fragments.INavigationHost

abstract class BaseNavigationActivity : BaseActivity(), INavigationHost,
        NavController.OnDestinationChangedListener {

    @get:NavigationRes
    protected abstract val navigationGraphResId: Int

    protected open val startDestinationArgs: Bundle? get() = intent?.extras

    @LayoutRes
    protected open val contentViewResId: Int = R.layout.activity_navigation

    protected open val backPressedOverrideMode: BackPressedMode = BackPressedMode.NO_CHANGE

    private val handler = Handler(Looper.getMainLooper())

    private val backPressedClearRunnable = Runnable {
        backPressedTriggered = false
    }

    protected lateinit var navController: NavHostController
        private set

    private lateinit var navHostFragment: NavHostFragment

    protected lateinit var appBarConfiguration: AppBarConfiguration
        private set

    private var backPressedTriggered = false

    protected val currentNavDestination
        get() = navController.currentDestination

    protected val currentNavDestinationId: Int
        get() = currentNavDestination?.id ?: NAV_ID_NONE

    protected val currentNavFragment: BaseNavigationFragment<*>?
        get() = navHostFragment.childFragmentManager.fragments.lastOrNull() as? BaseNavigationFragment<*>

    protected val navBackStackEntryCount: Int
        get() = navHostFragment.childFragmentManager.backStackEntryCount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentViewResId)

        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            ?: throw IllegalStateException("Wrong contentViewResId specified")

        val graph = navHostFragment.navController.navInflater.inflate(navigationGraphResId)
        onNavigationGraphInflated(graph)

        // Bundle из startDestinationArgs будет перекинут фрагменту со startDestination
        // для создания его аргументов (если предусмотрены)
        navHostFragment.navController.setGraph(graph, startDestinationArgs)

        navController = navHostFragment.navController as NavHostController

        this.appBarConfiguration = createAppBarConfiguration()
//        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onResume() {
        super.onResume()
        navController.addOnDestinationChangedListener(this)
    }

    override fun onPause() {
        super.onPause()
        navController.removeOnDestinationChangedListener(this)
    }

    override fun finish() {
        super.finish()
        if (isAtLeastUpsideDownCake()) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }

    override fun onBackPressed() {
        var handled = false
        val mode = backPressedOverrideMode
        if (mode != BackPressedMode.NO_CHANGE) {
            if (!navHostFragment.childFragmentManager.isStateSaved && !supportFragmentManager.isStateSaved) {
                val count = navBackStackEntryCount.takeIf { it > 0 } ?: supportFragmentManager.backStackEntryCount
                if (count == 0 || mode.isCurrent && count > 0) {
                    if (mode.isPressTwice && !backPressedTriggered) {
                        handler.removeCallbacks(backPressedClearRunnable)
                        backPressedTriggered = true
                        handler.postDelayed(backPressedClearRunnable, DELAY_PRESS_TWICE)
                        Toast.makeText(
                            this@BaseNavigationActivity,
                            R.string.toast_press_twice_to_quit_message,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        finish()
                    }
                    handled = true
                }
            }
        }
        if (!handled) {
            super.onBackPressed()
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?,
    ) {
        // do nothing
    }

    override fun onSupportNavigateUp(): Boolean {
        // не будет срабатывать, если с каждого фрагмента предоставляется свой тулбар (и на нём вызывается setNavigationOnClickListener)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun registerToolbarWithNavigation(toolbar: Toolbar, fragment: BaseNavigationFragment<*>) {
        setSupportActionBar(toolbar)
        val configuration = appBarConfiguration
        toolbar.setupWithNavController(navController, configuration)
        // листенер вызывается вручную, т.к. на конкретном фрагменте может быть своё поведение home button
        toolbar.setNavigationOnClickListener {
            if (!fragment.onUpPressed()) {
                NavigationUI.navigateUp(navController, configuration)
            }
        }
    }

    protected open fun createAppBarConfiguration(): AppBarConfiguration {
        // в дефолтной реализации без DrawerLayout
        return AppBarConfiguration(navController.graph)
    }

    open fun onNavigationGraphInflated(navGraph: NavGraph) {
        // do nothing
    }

    enum class BackPressedMode {
        NO_CHANGE,
        PRESS_TWICE_LAST,
        PRESS_TWICE_CURRENT,
        FINISH_LAST,
        FINISH_CURRENT;

        val isLast get() = this in listOf(PRESS_TWICE_LAST, FINISH_LAST)

        val isCurrent get() = this in listOf(PRESS_TWICE_CURRENT, FINISH_CURRENT)

        val isPressTwice get() = this in listOf(PRESS_TWICE_LAST, PRESS_TWICE_CURRENT)

        val isFinish get() = this in listOf(FINISH_LAST, FINISH_CURRENT)
    }

    companion object {

        private const val NAV_ID_NONE = -1

        private const val DELAY_PRESS_TWICE = 1000L
    }
}