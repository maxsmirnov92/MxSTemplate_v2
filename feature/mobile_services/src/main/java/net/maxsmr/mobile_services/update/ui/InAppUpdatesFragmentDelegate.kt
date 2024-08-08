package net.maxsmr.mobile_services.update.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.isAtLeastNougat
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.actions.ToastDuration
import net.maxsmr.core.android.base.actions.ToastExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.utils.hasTimePassed
import net.maxsmr.feature.mobile_services.R
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.MobileBuildType
import net.maxsmr.mobile_services.update.CommonInAppUpdateChecker
import net.maxsmr.mobile_services.update.InAppUpdateChecker
import net.maxsmr.mobile_services.update.RuStoreInAppUpdateChecker
import net.maxsmr.mobile_services.update.StubInAppUpdateChecker

class InAppUpdatesFragmentDelegate(
    override val fragment: BaseVmFragment<*>,
    override val viewModel: BaseViewModel,
    private val cacheRepo: CacheDataStoreRepository,
    private val interval: Long,
    updateRequestCode: Int,
    availability: IMobileServicesAvailability,
    mobileBuildType: MobileBuildType,
) : IFragmentDelegate, InAppUpdateChecker.Callbacks {

    init {
        check(interval >= 0) {
            "Incorrect interval: $interval"
        }
    }

    // by lazy нельзя из-за registerForActivityResult в CommonInAppUpdateChecker
    private val checker: InAppUpdateChecker = when (mobileBuildType) {
        MobileBuildType.COMMON -> {
            CommonInAppUpdateChecker(availability, fragment, updateRequestCode, this)
        }

        MobileBuildType.RUSTORE -> {
            if (isAtLeastNougat()) {
                RuStoreInAppUpdateChecker(fragment, this)
            } else {
                StubInAppUpdateChecker()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.viewModelScope.launch {
            if (hasTimePassed(cacheRepo.getLastCheckInAppUpdate(), interval)) {
                checker.doCheck()
            }
        }
    }

    override fun onUpdateCheckSuccess() {
        viewModel.viewModelScope.launch {
            // с целью не одолевать юзера на каждый возврат диалогом доступности обновления
            cacheRepo.setCurrentLastCheckInAppUpdate()
        }
    }

    override fun onUpdateDownloadNotStarted(isCancelled: Boolean) {
        if (!isCancelled) {
            viewModel.showToast(TextMessage(R.string.mobile_services_toast_update_download_not_started_message))
        }
    }

    override fun onUpdateDownloadStarted() {
        viewModel.showSnackbar(
            TextMessage(R.string.mobile_services_snackbar_update_downloading_message),
            SnackbarExtraData(SnackbarExtraData.SnackbarLength.INDEFINITE),
            priority = AlertQueueItem.Priority.HIGHEST,
            putInQueueHead = true
        )
    }

    override fun onUpdateDownloaded(completeAction: () -> Unit) {
        viewModel.viewModelScope.launch {
            // если пользовать вдруг откажется завершать -
            // проверять и показывать снек каждый раз
            cacheRepo.clearLastCheckInAppUpdate()
        }
        viewModel.showSnackbar(
            TextMessage(R.string.mobile_services_snackbar_update_downloaded_message),
            SnackbarExtraData(SnackbarExtraData.SnackbarLength.INDEFINITE),
            Alert.Answer(R.string.mobile_services_snackbar_update_downloaded_action).onSelect {
                completeAction()
            },
            priority = AlertQueueItem.Priority.HIGHEST,
            putInQueueHead = true
        )
    }

    override fun onUpdateFailed() {
        viewModel.showToast(TextMessage(R.string.mobile_services_toast_update_failed_message))
    }

    override fun onUpdateCancelled() {
        viewModel.showToast(TextMessage(R.string.mobile_services_toast_update_cancelled_message))
    }

    override fun onStartUpdateFlowFailed(throwable: Throwable) {
        throwable.message?.takeIf { it.isNotEmpty() }?.let {
            viewModel.showToast(TextMessage(it), ToastExtraData(duration = ToastDuration.LONG))
        }
    }
}