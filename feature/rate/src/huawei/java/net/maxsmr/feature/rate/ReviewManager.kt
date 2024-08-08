package net.maxsmr.feature.rate

import android.app.Activity
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
        // TODO implementation
    }

    interface Callbacks {

        fun onReviewSuccess()

        fun onReviewFailed()
    }
}