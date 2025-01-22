package net.maxsmr.feature.rate

import android.app.Activity
import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.alert.view.dialog.RateDialog
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.MobileBuildType

/**
 * @param availability null, если использование [ReviewManager] не предусматривается
 */
abstract class BaseRateAppComponentDelegate(
    override val host: Activity,
    override val viewModel: BaseViewModel,
    private val availability: IMobileServicesAvailability?,
    private val mobileBuildType: MobileBuildType,
    private val repo: CacheDataStoreRepository,
) : IComponentDelegate<Activity>, ReviewManager.Callbacks {

    override val context: Context by lazy { host }

    private var reviewManager: ReviewManager? = null

    override fun onCreated() {
        this.reviewManager = availability?.let {
            ReviewManager(
                host,
                availability,
                this
            )
        }
    }

    override fun onDestroyed() {
        super.onDestroyed()
        viewModel.hideDialog(DIALOG_TAG_RATE_APP)
        reviewManager = null
    }

    override fun onReviewSuccess() {
        viewModel.viewModelScope.launch {
            repo.setAppRated()
        }
    }

    override fun onReviewFailed() {
        showRateDialog()
    }

    abstract fun navigateToMarket()

    abstract fun navigateToFeedback(shouldNavigateToMarket: Boolean)

    fun onRateAppSelected(rating: Int) {
        viewModel.viewModelScope.launch {
            repo.setAppRated()
        }
        if (rating >= RateDialog.RATE_THRESHOLD_DEFAULT) {
            navigateToMarket()
        } else {
            navigateToFeedback(true)
        }
    }

    @CallSuper
    fun doRateApp() {
        val manager = reviewManager
        if (manager != null
                && mobileBuildType == MobileBuildType.COMMON
                && availability?.isAnyServiceAvailable == true
        ) {
            manager.requestReviewFlow()
        } else {
            showRateDialog()
        }
    }

    /**
     * Показ внутриаппового диалога оценки
     */
    private fun showRateDialog() {
        viewModel.showCustomDialog(DIALOG_TAG_RATE_APP) {
            setTitle(R.string.rate_dialog_app_title)
            setAnswers(
                Alert.Answer(R.string.rate_dialog_app_button_positive),
                Alert.Answer(R.string.rate_dialog_app_button_negative),
            )
            setUniqueStrategy(AlertQueueItem.UniqueStrategy.Ignore)
        }
    }

    companion object {

        const val DIALOG_TAG_RATE_APP = "rate_app"
    }
}