package net.maxsmr.mobile_services.update.ui

import android.content.IntentSender
import androidx.fragment.app.Fragment
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.actions.ToastDuration
import net.maxsmr.core.android.base.actions.ToastExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.mobile_services.R
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.MobileBuildType
import net.maxsmr.mobile_services.update.CommonInAppUpdateChecker
import net.maxsmr.mobile_services.update.InAppUpdateChecker
import java.lang.IllegalArgumentException

class InAppUpdatesFragmentDelegate(
    fragment: Fragment,
    updateRequestCode: Int,
    availability: IMobileServicesAvailability,
    mobileBuildType: MobileBuildType,
) : IFragmentDelegate, CommonInAppUpdateChecker.Callbacks {

    private val checker: InAppUpdateChecker = if (mobileBuildType == MobileBuildType.COMMON) {
        CommonInAppUpdateChecker(availability, fragment, updateRequestCode, this)
    } else {
        throw IllegalArgumentException("Unknown MobileBuildType: $mobileBuildType")
    }

    private lateinit var viewModel: BaseViewModel

    override fun onViewCreated(
        fragment: BaseVmFragment<*>,
        viewModel: BaseViewModel,
        delegate: AlertFragmentDelegate<*>,
    ) {
        this.viewModel = viewModel
        checker.onStartChecking()
    }

    override fun onViewDestroyed() {
        checker.onStopChecking()
    }

    override fun onUpdateDownloadNotStarted(isCancelled: Boolean) {
        if (!isCancelled) {
            viewModel.showToast(TextMessage(R.string.mobile_services_toast_update_not_started_message))
        }
    }

    override fun onUpdateDownloading() {
        viewModel.showToast(TextMessage(R.string.mobile_services_toast_update_downloading_message))
    }

    override fun onUpdateDownloaded(completeAction: () -> Unit) {
        viewModel.showSnackbar(
            TextMessage(R.string.mobile_services_snackbar_update_downloaded_action),
            answer = Alert.Answer(R.string.mobile_services_snackbar_update_downloaded_action).onSelect {
                completeAction()
            }
        )
    }

    override fun onStartUpdateFlowException(exception: IntentSender.SendIntentException) {
        exception.message?.takeIf { it.isNotEmpty() }?.let {
            viewModel.showToast(TextMessage(it), ToastExtraData(duration = ToastDuration.LONG))
        }
    }
}