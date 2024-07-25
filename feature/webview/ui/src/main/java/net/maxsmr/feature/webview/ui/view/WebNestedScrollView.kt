package net.maxsmr.feature.webview.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import androidx.core.widget.NestedScrollView

class WebNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : NestedScrollView(context, attrs) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (childCount > 0) {
            val child = getChildAt(0)
            if (child is WebView) {
                if (child.scrollY > 0) {
                    return false // Не перехватывать событие, если WebView не вверху
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}