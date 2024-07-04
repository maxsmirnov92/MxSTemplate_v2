package net.maxsmr.justupdownloadit.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.di.DI_NAME_VERSION_CODE
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_IN_APP_UPDATES
import net.maxsmr.core.ui.view.alert.delegate.CombinedViewFragmentAlertDelegate
import net.maxsmr.feature.about.ReleaseNotesComponentDelegate
import net.maxsmr.feature.about.alert.view.ReleaseNotesFragmentAlertDelegate
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.BaseDownloadsPagerFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.RateAppReminderComponentDelegate
import net.maxsmr.feature.rate.alert.view.RateAppReminderFragmentAlertDelegate
import net.maxsmr.justupdownloadit.CHECK_IN_APP_UPDATES_INTERVAL
import net.maxsmr.justupdownloadit.RATE_APP_ASK_INTERVAL
import net.maxsmr.justupdownloadit.RELEASE_NOTES_ASSETS_FOLDER_NAME_EN
import net.maxsmr.justupdownloadit.RELEASE_NOTES_ASSETS_FOLDER_NAME_RU
import net.maxsmr.justupdownloadit.mobileBuildType
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.update.ui.InAppUpdatesComponentDelegate
import net.maxsmr.justupdownloadit.ui.fragment.params.AboutScreenParams
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainDownloadsPagerFragment : BaseDownloadsPagerFragment() {

    private val appUpdateDelegate by lazy {
        InAppUpdatesComponentDelegate(
            this,
            viewModel,
            cacheRepo,
            CHECK_IN_APP_UPDATES_INTERVAL,
            REQUEST_CODE_IN_APP_UPDATES,
            availability,
            mobileBuildType,
        )
    }

    private val releaseNotesDelegate by lazy {
        ReleaseNotesComponentDelegate(
            requireContext(),
            viewModel,
            versionCode,
            versionName,
            mapOf(
                "en" to RELEASE_NOTES_ASSETS_FOLDER_NAME_EN,
                "ru" to RELEASE_NOTES_ASSETS_FOLDER_NAME_RU
            ),
            cacheRepo,
        )
    }

    private val rateReminderDelegate by lazy {
        RateAppReminderComponentDelegate(
            requireContext(),
            viewModel,
            RATE_APP_ASK_INTERVAL,
            cacheRepo
        ) {
            viewModel.navigate(
                NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                    MainDownloadsPagerFragmentDirections.actionToAboutFragment(AboutScreenParams(true))
                )
            )
        }
    }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var availability: IMobileServicesAvailability

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    @Named(DI_NAME_VERSION_CODE)
    @JvmField
    var versionCode: Int = 0

    @Inject
    @Named(DI_NAME_VERSION_NAME)
    lateinit var versionName: String

    override fun createAlertDelegate(): CombinedViewFragmentAlertDelegate<DownloadsViewModel> {
        return CombinedViewFragmentAlertDelegate(
            listOf(
                ReleaseNotesFragmentAlertDelegate(this, viewModel),
                RateAppReminderFragmentAlertDelegate(rateReminderDelegate, this, viewModel)
            ), this, viewModel
        )
    }

    override fun createFragmentDelegates(): List<IComponentDelegate<*>> {
        return listOf(appUpdateDelegate, releaseNotesDelegate, rateReminderDelegate)
    }
}