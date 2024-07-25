package net.maxsmr.feature.webview.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.webkit.WebView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class WebSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs) {

    private val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            isEnabled = // Если WebView находится в верхней части и происходит прокрутка вниз
                webView?.scrollY == 0 && distanceY < 0
            return false
        }
    })


    var webView: WebView? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        webView?.let {
//            if (it.scrollY > 0) {
//                return false
//            }
            gestureDetector.onTouchEvent(ev)
        }
        return super.onInterceptTouchEvent(ev)
    }
}
