package net.maxsmr.feature.showcase

import android.content.Context
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.components.IComponentDelegate
import smartdevelop.ir.eram.showcaseviewlib.GuideView

@MainThread
class GuideComponentDelegate @JvmOverloads constructor(
    override val host: ComponentActivity,
    override val viewModel: BaseViewModel,
    private val checker: GuideChecker,
    private val shouldAutoStart: Boolean = true,
    private val onNextListener: ((GuideItem, Int) -> Unit)? = null,
    items: List<GuideItem> = emptyList(),
) : IComponentDelegate<ComponentActivity> {

    override val context: Context by lazy { host }

    private val shownItems = mutableListOf<GuideItem>()

    private val isShowing: Boolean get() = guideView?.isShowing == true

    var items: List<GuideItem> = items
        set(value) {
            field = value
            if (wasStarted) {
                if (value.isNotEmpty()) {
                    doStart()
                } else {
                    doStop()
                }
            }
        }

    var wasStarted: Boolean = false
        private set

    private var guideView: GuideView? = null

    override fun onCreated() {
        super.onCreated()
        if (shouldAutoStart) {
            doStart()
        }
    }

    override fun onDestroyed() {
        super.onDestroyed()
        if (wasStarted) {
            doStop()
        }
    }

    fun doStart(): Boolean {
        if (!host.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
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
        val (item, index) = nextItem() ?: run {
            checker.isCompleted = true
            return
        }

        onNextListener?.invoke(item, index)

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

    private fun nextItem(): Pair<GuideItem, Int>? {
        var index = -1
        return items.find {
            index++
            !shownItems.map { item -> item.key }.contains(it.key)
                    && !checker.isChecked(it.key)
        }?.let {
            it to index
        }
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