package net.maxsmr.feature.rate.dialog

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.maxsmr.commonutils.gui.disableTalkback
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.dialog.BaseCustomDialog
import net.maxsmr.core.ui.views.sendAnnouncementEvent
import net.maxsmr.feature.rate.R

class RateDialog(
    fragment: Fragment,
    alert: Alert,
    private val listener: RateListener,
) : BaseCustomDialog(fragment.requireContext(), R.style.RateDialogTheme, R.layout.layout_dialog_rate, alert) {

    private val tvTitle: TextView by lazy { findViewById(R.id.tvRateTitle) }
    private val ratingBar: RatingBar by lazy { findViewById(R.id.ratingBar) }
    private val btnPositive: Button by lazy { findViewById(R.id.btRatePositive) }
    private val btnNegative: Button by lazy { findViewById(R.id.btRateNegative) }

    private val viewModel = ViewModelProvider(fragment)[RateAppViewModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tvTitle.text = alert.title?.get(context)?.takeIf { it.isNotEmpty() }
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        alert.answers[0].let {
            btnPositive.text = it.title.get(context)
            btnPositive.setOnAnswerClickListener(it) { listener.onRateSelected(viewModel.rating) }
        }
        alert.answers[1].let {
            btnNegative.text = it.title.get(context)
            btnNegative.setOnAnswerClickListener(it) {  }
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
            if (it) {
                sendDescription(viewModel.rating, false)
            }
        }
        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser) {
                val r = rating.toInt()
                sendDescription(r, true)
                viewModel.rating = r
            }
        }
        ratingBar.rating = viewModel.rating.toFloat()
    }


    class RateAppViewModel : ViewModel() {

        var rating = RATING_DEFAULT
    }

    interface RateListener {

        fun onRateSelected(rating: Int)
    }


    companion object {

        const val RATING_DEFAULT = 5
        const val RATE_THRESHOLD_DEFAULT = 4
    }
}