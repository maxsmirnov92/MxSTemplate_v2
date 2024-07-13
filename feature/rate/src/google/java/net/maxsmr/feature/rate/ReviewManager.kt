package net.maxsmr.feature.rate

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import net.maxsmr.mobile_services.IMobileServicesAvailability

/**
 * Пользование нежелательно,
 * т.к. непоявление гугловского диалога и действия на нём невозможно отследить
 */
class ReviewManager(
    private val activity: Activity,
    private val availability: IMobileServicesAvailability,
    private val callbacks: Callbacks
) {

    fun requestReviewFlow() {
        if (availability.isGooglePlayServicesAvailable) {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener {
                if (it.isSuccessful) {
                    // We got the ReviewInfo object
                    val reviewInfo = request.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { _ ->
                        // The flow has finished. The API does not indicate whether the user
                        // reviewed or not, or even whether the review dialog was shown. Thus, no
                        // matter the result, we continue our app flow.
                        callbacks.onReviewSuccess()
                    }
                } else {
                    // There was some problem, continue regardless of the result.
                    callbacks.onReviewFailed()
                }
            }
        } else {
            callbacks.onReviewFailed()
        }
    }

    interface Callbacks {

        fun onReviewSuccess()

        fun onReviewFailed()
    }
}