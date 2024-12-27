package net.maxsmr.feature.rate

import android.content.Context
import android.content.DialogInterface
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.utils.hasTimePassed
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository

class RateAppReminderComponentDelegate(
    override val host: Context,
    override val viewModel: BaseViewModel,
    private val interval: Long,
    private val repo: CacheDataStoreRepository,
    private val navigateToRate: () -> Unit,
) : IComponentDelegate<Context> {

    init {
        check(interval >= 0) {
            "Reminder interval should be non negative"
        }
    }

    private val scope by lazy { viewModel.viewModelScope }

    override val context: Context = host

    override fun onResumed() {
        super.onResumed()
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

    override fun onDestroyed() {
        super.onDestroyed()
        viewModel.hideDialog(DIALOG_TAG_RATE_APP_REMINDER)
    }

    fun onConfirmReminder(dialogChoice: Int) {
        scope.launch {
            when (dialogChoice) {
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

    fun onCancelReminder() {
        scope.launch {
            repo.setAppNotRated(false)
        }
    }

    companion object {

        const val DIALOG_TAG_RATE_APP_REMINDER = "rate_app_reminder"
    }
}