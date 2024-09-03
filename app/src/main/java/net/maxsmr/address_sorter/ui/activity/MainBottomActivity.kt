package net.maxsmr.address_sorter.ui.activity

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.address_sorter.App
import net.maxsmr.address_sorter.R
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.ui.components.activities.BaseBottomNavigationActivity

@AndroidEntryPoint
class MainBottomActivity : BaseBottomNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val topLevelDestinationIds =
        setOf(R.id.navigationAddressSorter, R.id.navigationSettings, R.id.navigationAbout)

    override val navigationMenuResId: Int = R.menu.menu_navigation_main

    override val backPressedOverrideMode: BackPressedMode = BackPressedMode.PRESS_TWICE_CURRENT

    override val canUseFragmentDelegates: Boolean
        get() {
            val app = baseApplicationContext as App
            return app.isActivityFirstAndSingle(MainBottomActivity::class.java)
        }

    override fun setupBottomNavigationView() {
        super.setupBottomNavigationView()
        bottomNavigationView.setOnItemSelectedListener { item ->
            navController.navigateWithGraphFragments(
                item,
                currentNavFragment
            )
        }
    }
}