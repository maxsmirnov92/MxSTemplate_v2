package net.maxsmr.feature.rate.dialog

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.gui.disableTalkback
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.live.setValueIfNew
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.Alert.Answer.Companion.findByTag
import net.maxsmr.core.ui.alert.representation.BaseCustomBottomSheetDialog
import net.maxsmr.core.ui.views.sendAnnouncementEvent
import net.maxsmr.feature.rate.R

abstract class BaseRateBottomSheetDialog<D : BaseRateBottomSheetDialog.RateData>(
    context: Context,
    @LayoutRes
    layoutResId: Int,
    alert: Alert,
    private val lifecycleOwner: LifecycleOwner,
    private val orderRating: MutableLiveData<Int>,
    private val orderRatingIgnore: MutableLiveData<Boolean>?,
    private val rateData: D,
) : BaseCustomBottomSheetDialog(context, layoutResId = layoutResId, alert = alert) {

    protected abstract val titleTextView: TextView
    protected abstract val ratingBar: RatingBar
    protected abstract val rateButton: Button
    protected open val descriptionTextView: TextView? = null
    protected open val ignoreCheckbox: CheckBox? = null

    protected abstract val title: CharSequence
    protected open val message: CharSequence? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleTextView.text = title
        descriptionTextView?.setTextOrGone(message)

        orderRating.observe(lifecycleOwner) {
            ratingBar.rating = it.toFloat()
            rateButton.isEnabled = it > 0
        }

        fun sendDescription(rating: Int, fromBar: Boolean) {
            val description = if (rating > 0) {
                context.getString(R.string.rate_star_content_description_format, rating)
            } else {
                if (!fromBar) {
                    context.getString(R.string.rate_star_content_description_empty_instruction)
                } else {
                    context.getString(R.string.rate_star_content_description_empty)
                }
            }
            context.sendAnnouncementEvent(description)
        }

        ratingBar.disableTalkback {
            // при отлючении озвучивания процентов
            // вьюху надо озвучивать отдельно при выборе
            if (it) {
                sendDescription(orderRating.value ?: 0, false)
            }
        }
        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                val r = rating.toInt()
                sendDescription(r, true)
                orderRating.setValueIfNew(r)
            }
        }

//        ratingBar.setMinRating(rateData.minRating)
        ratingBar.numStars = rateData.maxRating

        orderRatingIgnore?.observe(lifecycleOwner) {
            ignoreCheckbox?.isChecked = it
        }
        ignoreCheckbox?.setOnCheckedChangeListener { _, isChecked ->
            orderRatingIgnore?.setValueIfNew(isChecked)
            alert?.answers?.findByTag(RateAction.IGNORE_CHECKED)?.select?.invoke()
        }
        ignoreCheckbox?.isVisible = rateData.canIgnore

        rateButton.setOnAnswerClickListener(alert?.answers?.findByTag(RateAction.ACCEPT))
        setOnAnswerCancelListener(alert?.answers?.findByTag(RateAction.DECLINE))
    }

    override fun dismiss() {
        super.dismiss()
        orderRating.removeObservers(lifecycleOwner)
        orderRatingIgnore?.removeObservers(lifecycleOwner)
    }

    open class RateData(
        val canIgnore: Boolean,
        val minRating: Int = 0,
        val maxRating: Int = MAX_RATING_DEFAULT,
    ) : java.io.Serializable {

        init {
            require(minRating >= 0)
            require(maxRating > 0)
            require(maxRating > minRating)
        }

        companion object {

            const val MAX_RATING_DEFAULT = 5
        }
    }

    enum class RateAction {
        ACCEPT,
        DECLINE,
        IGNORE_CHECKED
    }
}