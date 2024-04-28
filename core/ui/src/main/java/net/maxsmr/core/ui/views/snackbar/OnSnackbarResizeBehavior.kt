package net.maxsmr.core.ui.views.snackbar

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

class OnSnackbarResizeBehavior(resizableView: View) : CoordinatorLayout.Behavior<View>() {
    private var initialMargin = 0

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("OnSnackbarResizeBehavior")

    init {
        initialMargin = resizableView.marginBottom
        logger.d("Dependent view ${resizableView::class.java.simpleName} initial marginBottom=$initialMargin")
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        return dependency is SnackbarLayout
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
        super.onDependentViewRemoved(parent, child, dependency)
        //коррекция view
        child.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = initialMargin
        }
        logger.d("Snackbar removed. Set dependent view marginBottom=${child.marginBottom}")
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
        val newMargin = initialMargin + dependency.height - dependency.translationY + dependency.marginBottom + dependency.marginTop
        logger.d("Snackbar changed. Set dependent view marginBottom=$newMargin")
        child.updateLayoutParams<MarginLayoutParams> {
            bottomMargin = newMargin.toInt()
        }
        return true
    }
}