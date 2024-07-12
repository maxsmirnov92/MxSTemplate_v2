package net.maxsmr.feature.rate

import android.app.Activity
import androidx.annotation.CallSuper
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.toRepresentation
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.dialog.RateDialog
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.MobileBuildType

abstract class BaseRateAppFragmentDelegate(
    private val availability: IMobileServicesAvailability,
    private val mobileBuildType: MobileBuildType,
    private val repo: CacheDataStoreRepository
): IFragmentDelegate, ReviewManager.Callbacks {

    protected var viewModel: BaseViewModel? = null
    protected var activity: Activity? = null
    private var reviewManager: ReviewManager? = null

    override fun onViewCreated(
        fragment: BaseVmFragment<*>,
        viewModel: BaseViewModel,
        delegate: AlertFragmentDelegate<*>,
    ) {
        val activity = fragment.requireActivity()

        this.viewModel = viewModel
        this.activity = activity
        this.reviewManager = ReviewManager(
            activity,
            availability,
            this
        )

        delegate.bindAlertDialog(DIALOG_TAG_RATE_APP) {
            RateDialog(delegate.fragment, it, object : RateDialog.RateListener {

                override fun onRateSelected(rating: Int) {
                    viewModel.viewModelScope.launch {
                        repo.setAppRated()
                    }
                    if (rating >= RateDialog.RATE_THRESHOLD_DEFAULT) {
                        navigateToMarket()
                    } else {
                        navigateToFeedback(true)
                    }
                }
            }).toRepresentation()
        }
    }

    override fun onViewDestroyed() {
        super.onViewDestroyed()
        viewModel = null
        activity = null
        reviewManager = null
    }

    override fun onReviewSuccess() {
        viewModel?.viewModelScope?.launch {
            repo.setAppRated()
        }
    }

    override fun onReviewFailed() {
        showRateDialog()
    }

    abstract fun navigateToMarket()

    abstract fun navigateToFeedback(shouldNavigateToMarket: Boolean)

    @CallSuper
    fun onRateAppSelected() {
        if (mobileBuildType == MobileBuildType.COMMON && availability.isAnyServiceAvailable) {
            reviewManager?.requestReviewFlow()
        } else {
            showRateDialog()
        }
    }

    /**
     * Показ внутриаппового диалога оценки
     */
    private fun showRateDialog() {
        viewModel?.showCustomDialog(DIALOG_TAG_RATE_APP) {
            setTitle(R.string.rate_dialog_app_title)
            setAnswers(
                Alert.Answer(R.string.rate_dialog_app_button_positive),
                Alert.Answer(R.string.rate_dialog_app_button_negative),
            )
        }
    }

    companion object {

        private const val DIALOG_TAG_RATE_APP = "rate_app"
    }
}