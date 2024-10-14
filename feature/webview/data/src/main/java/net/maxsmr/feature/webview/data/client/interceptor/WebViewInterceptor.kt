package net.maxsmr.feature.webview.data.client.interceptor

/**
 * Перехватывает (или нет) указанную урлу каждый раз в соот-ии с подставленным условием
 */
class WebViewInterceptor: IWebViewInterceptor {

    override fun shouldIntercept(
        url: String,
        interceptCondition: () -> IWebViewInterceptor.InterceptedUrl?,
    ): IWebViewInterceptor.InterceptedUrl? = interceptCondition()
}