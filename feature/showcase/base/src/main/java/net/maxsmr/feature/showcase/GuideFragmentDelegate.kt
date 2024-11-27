package net.maxsmr.feature.showcase

import android.view.View
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import smartdevelop.ir.eram.showcaseviewlib.GuideView

@MainThread
class GuideFragmentDelegate @JvmOverloads constructor(
    override val fragment: BaseVmFragment<*>,
    override val viewModel: BaseViewModel,
    private val checker: GuideChecker,
    items: List<GuideItem> = emptyList(),
) : IFragmentDelegate {

    private val shownItems = mutableListOf<GuideItem>()

    private val isShowing: Boolean get() = guideView?.isShowing == true

    var items: List<GuideItem> = items
        set(value) {
            field = value
            doStart()
        }

    var wasStarted: Boolean = false
        private set

    private var guideView: GuideView? = null

    override fun onViewCreated(delegate: AlertFragmentDelegate<*>) {
        super.onViewCreated(delegate)
        doStart()
    }

    override fun onViewDestroyed() {
        super.onViewDestroyed()
        doStop()
    }

    fun doStart(): Boolean {
        if (!fragment.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            return false
        }
        if (items.isEmpty() || checker.isCompleted) {
            return false
        }

        doStop()
        wasStarted = true

        showNextItem()
        return true
    }

    fun doStop() {
        wasStarted = false
        if (isShowing) {
            guideView?.let {
                it.dismiss()
                guideView = null
            }
        }
        shownItems.clear()
    }

    private fun onNext() {
        if (!wasStarted) return

        shownItems.lastOrNull()?.let { previous ->
            checker.setChecked(previous.key)
        }

        showNextItem()
    }

    private fun showNextItem() {
        if (isShowing) {
            return
        }
        val item = nextItem() ?: run {
            checker.isCompleted = true
            return
        }

        val context = fragment.requireActivity()
        with(GuideView.Builder(context)) {
            item.builder(this)
            setTargetView(item.view)
//            buttonText(if (hasNextItem()) {
//                context.getString(R.string.showcase_button_next)
//            } else {
//                context.getString(R.string.showcase_button_done)
//            })
            setGuideListener {
                guideView = null
                onNext()
            }
            guideView = build().also {
                it.show()
            }
        }

        shownItems.add(item)
    }

    private fun hasNextItem() = nextItem() != null

    private fun nextItem() = items.find {
        !shownItems.map { item -> item.key }.contains(it.key)
                && !checker.isChecked(it.key)
    }

    interface GuideChecker {

        var isCompleted: Boolean

        fun isChecked(key: String): Boolean = false

        fun setChecked(key: String) {}
    }

    class GuideItem(
        val key: String,
        val view: View,
        val builder: GuideView.Builder.() -> Unit,
    )
}