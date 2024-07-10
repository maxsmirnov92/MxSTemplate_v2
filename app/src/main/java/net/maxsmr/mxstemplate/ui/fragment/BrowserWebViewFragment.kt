package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.di.DI_NAME_VERSION_CODE
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_IN_APP_UPDATES
import net.maxsmr.feature.about.ReleaseNotesFragmentDelegate
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.update.ui.InAppUpdatesFragmentDelegate
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.RELEASE_NOTES_ASSETS_FOLDER_NAME
import net.maxsmr.mxstemplate.mobileBuildType
import net.maxsmr.mxstemplate.ui.BrowserWebViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class BrowserWebViewFragment : BaseDownloadableWebViewFragment<BrowserWebViewModel>() {

    override val viewModel: BrowserWebViewModel by viewModels()

    override val delegates: List<IFragmentDelegate> by lazy { listOf(appUpdateDelegate, releaseNotesDelegate) }

    private val appUpdateDelegate by lazy {
        InAppUpdatesFragmentDelegate(
            this,
            REQUEST_CODE_IN_APP_UPDATES,
            availability,
            mobileBuildType
        )
    }

    private val releaseNotesDelegate by lazy {
        ReleaseNotesFragmentDelegate(
            versionCode,
            versionName,
            RELEASE_NOTES_ASSETS_FOLDER_NAME,
            requireContext().assets.list(RELEASE_NOTES_ASSETS_FOLDER_NAME)?.toSet().orEmpty(),
            cacheRepo
        )
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
    @JvmField
    var versionName: String = EMPTY_STRING
}