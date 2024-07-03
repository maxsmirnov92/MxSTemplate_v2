package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.feature.rate.fragment.BaseFeedbackFragment
import net.maxsmr.mxstemplate.ui.MainFeedbackViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainFeedbackFragment: BaseFeedbackFragment<MainFeedbackViewModel>() {

    override val viewModel: MainFeedbackViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper
}