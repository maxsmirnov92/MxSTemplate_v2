package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.feature.rate.fragment.BaseFeedbackFragment
import net.maxsmr.mxstemplate.ui.FeedbackViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class FeedbackFragment: BaseFeedbackFragment<FeedbackViewModel>() {

    override val viewModel: FeedbackViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper
}