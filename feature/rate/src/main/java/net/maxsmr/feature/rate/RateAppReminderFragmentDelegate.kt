package net.maxsmr.feature.rate

import android.content.DialogInterface
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asYesNoNeutralDialog
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository

class RateAppReminderFragmentDelegate(
    private val interval: Long,
    private val repo: CacheDataStoreRepository,
    private val navigateToRate: () -> Unit,
) : IFragmentDelegate {

    init {
        check(interval >= 0) {
            "Reminder interval should be non negative"
        }
    }

    override fun onCreated(
        fragment: BaseVmFragment<*>,
        viewModel: BaseViewModel,
        delegate: AlertFragmentDelegate<*>,
    ) {
        val scope = viewModel.viewModelScope
        scope.launch {
            val info = repo.getAppRateInfo()
            if (info.isRated || info.notAskAgain) return@launch
            val currentTime = System.currentTimeMillis()
            if (currentTime - info.timestamp >= interval) {
                viewModel.showYesNoDialog(
                    DIALOG_TAG_RATE_APP_REMINDER,
                    TextMessage(R.string.rate_dialog_app_reminder_message),
                    TextMessage(R.string.rate_dialog_app_reminder_title),
                    R.string.rate_dialog_app_reminder_positive,
                    R.string.rate_dialog_app_reminder_negative,
                    R.string.rate_dialog_app_reminder_neutral,
                )
            }
        }
        delegate.bindAlertDialog(DIALOG_TAG_RATE_APP_REMINDER) {
            it.asYesNoNeutralDialog(fragment.requireContext(), onCancel = {
                scope.launch {
                    repo.setAppNotRated(false)
                }
            }) {
                scope.launch {
                    when (it) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            // на случай, если юзер не станет оценивать в самом диалоге
                            repo.setAppNotRated(false)
                            navigateToRate()
                        }

                        DialogInterface.BUTTON_NEGATIVE -> {
                            repo.setAppNotRated(true)
                        }

                        DialogInterface.BUTTON_NEUTRAL -> {
                            repo.setAppNotRated(false)
                        }
                    }
                }
            }
        }
    }

    companion object {

        private const val DIALOG_TAG_RATE_APP_REMINDER = "rate_app_reminder"
    }
}