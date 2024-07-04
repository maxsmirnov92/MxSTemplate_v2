package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_IN_APP_UPDATES
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewFragment
import net.maxsmr.mobile_services.update.ui.InAppUpdatesFragmentDelegate
import net.maxsmr.mxstemplate.mobileBuildType
import net.maxsmr.mxstemplate.mobileServicesAvailability
import net.maxsmr.mxstemplate.ui.BrowserWebViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class BrowserWebViewFragment : BaseDownloadableWebViewFragment<BrowserWebViewModel>() {

    override val viewModel: BrowserWebViewModel by viewModels()

    override val delegates: List<IFragmentDelegate> by lazy { listOf(appUpdateDelegate) }

    private val appUpdateDelegate = InAppUpdatesFragmentDelegate(
        this,
        REQUEST_CODE_IN_APP_UPDATES,
        mobileServicesAvailability,
        mobileBuildType
    )

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

}