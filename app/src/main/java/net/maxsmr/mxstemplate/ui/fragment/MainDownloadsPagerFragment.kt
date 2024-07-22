package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_IN_APP_UPDATES
import net.maxsmr.feature.about.ReleaseNotesFragmentDelegate
import net.maxsmr.feature.download.ui.BaseDownloadsPagerFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.RateAppReminderFragmentDelegate
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.update.ui.InAppUpdatesFragmentDelegate
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.CHECK_IN_APP_UPDATES_INTERVAL
import net.maxsmr.mxstemplate.RATE_APP_ASK_INTERVAL
import net.maxsmr.mxstemplate.RELEASE_NOTES_ASSETS_FOLDER_NAME
import net.maxsmr.mxstemplate.mobileBuildType
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainDownloadsPagerFragment: BaseDownloadsPagerFragment() {

    override val delegates: List<IFragmentDelegate> by lazy { listOf(rateDelegate, appUpdateDelegate, releaseNotesDelegate) }

    private val appUpdateDelegate by lazy {
        InAppUpdatesFragmentDelegate(
            this,
            viewModel,
            cacheRepo,
            CHECK_IN_APP_UPDATES_INTERVAL,
            REQUEST_CODE_IN_APP_UPDATES,
            availability,
            mobileBuildType,
        )
    }

    private val rateDelegate by lazy {
        RateAppReminderFragmentDelegate(
            this,
            viewModel,
            RATE_APP_ASK_INTERVAL,
            cacheRepo
        ) {
            viewModel.navigate(
                NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                    MainDownloadsPagerFragmentDirections.actionToAboutFragment(true)
                )
            )
        }
    }

    private val releaseNotesDelegate by lazy {
        ReleaseNotesFragmentDelegate(
            this,
            viewModel,
            BuildConfig.VERSION_CODE,
            BuildConfig.VERSION_NAME,
            RELEASE_NOTES_ASSETS_FOLDER_NAME,
            requireContext().assets.list(RELEASE_NOTES_ASSETS_FOLDER_NAME)?.toSet().orEmpty(),
            cacheRepo,
        )
    }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var availability: IMobileServicesAvailability

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository
}