package net.maxsmr.feature.rate

import android.content.DialogInterface
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asYesNoNeutralDialog
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.utils.hasTimePassed
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository

class RateAppReminderFragmentDelegate(
    override val fragment: BaseVmFragment<*>,
    override val viewModel: BaseViewModel,
    private val interval: Long,
    private val repo: CacheDataStoreRepository,
    private val navigateToRate: () -> Unit,
) : IFragmentDelegate {

    init {
        check(interval >= 0) {
            "Reminder interval should be non negative"
        }
    }

    override fun onViewCreated(delegate: AlertFragmentDelegate<*>) {
        val scope = viewModel.viewModelScope
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

    override fun onResume() {
        super.onResume()
        viewModel.viewModelScope.launch {
            val info = repo.getAppRateInfo()
            if (info.isRated || info.notAskAgain) return@launch
            if (hasTimePassed(info.timestamp, interval)) {
                viewModel.showYesNoDialog(
                    DIALOG_TAG_RATE_APP_REMINDER,
                    TextMessage(R.string.rate_dialog_app_reminder_message),
                    TextMessage(R.string.rate_dialog_app_reminder_title),
                    R.string.rate_dialog_app_reminder_positive,
                    R.string.rate_dialog_app_reminder_negative,
                    R.string.rate_dialog_app_reminder_neutral,
                    {
                        setUniqueStrategy(AlertQueueItem.UniqueStrategy.Ignore)
                    }
                )
            }
        }
    }

    override fun onViewDestroyed() {
        super.onViewDestroyed()
        viewModel.hideDialog(DIALOG_TAG_RATE_APP_REMINDER)
    }

    companion object {

        private const val DIALOG_TAG_RATE_APP_REMINDER = "rate_app_reminder"
    }
}