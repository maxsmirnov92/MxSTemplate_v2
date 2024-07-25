package net.maxsmr.feature.webview.ui.view

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class ScrollableWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle,
    defStyleRes: Int = 0,
) : WebView(context, attrs, defStyleAttr, defStyleRes) {

    var scrollChangedCallback: ((Int, Int, Int, Int) -> Unit)? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        scrollChangedCallback?.invoke(l, t, oldl, oldt)
    }
}