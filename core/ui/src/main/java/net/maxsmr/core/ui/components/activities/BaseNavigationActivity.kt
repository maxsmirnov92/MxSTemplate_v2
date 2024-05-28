package net.maxsmr.core.ui.components.activities

import android.os.Build
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.NavigationRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.components.fragments.INavigationHost

abstract class BaseNavigationActivity : BaseActivity(), INavigationHost,
        NavController.OnDestinationChangedListener {

    @get:NavigationRes
    protected abstract val navigationGraphResId: Int

    protected open val startDestinationArgs: Bundle? = null

    @LayoutRes
    protected open val contentViewResId: Int = R.layout.activity_navigation

    protected var currentNavDestinationId = NAV_ID_NONE
        private set

    protected lateinit var navController: NavController
        private set
    private lateinit var navHostFragment: NavHostFragment

    private lateinit var appBarConfiguration: AppBarConfiguration

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

        navController = navHostFragment.navController

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

    @CallSuper
    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        currentNavDestinationId = destination.id
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

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }

    companion object {

        private const val NAV_ID_NONE = -1
    }
}