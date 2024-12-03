package net.maxsmr.core.network.exceptions

import android.content.Context
import net.maxsmr.core.network.R
import okhttp3.Response

class IncorrectContentTypeException(
    val mimeType: String,
    response: Response?,
    exceptionMessage: String? = null,
    withBody: Boolean = false,
) : HttpProtocolException(response, exceptionMessage, withBody) {

    constructor(
        context: Context,
        mimeType: String,
        response: Response? = null,
        withBody: Boolean = false,
    ) : this(
        mimeType,
        response,
        if (mimeType.isNotEmpty()) {
            context.getString(R.string.error_http_incorrect_content_type_format, mimeType)
        } else {
            context.getString(R.string.error_http_incorrect_content_type)
        },
        withBody
    )
}