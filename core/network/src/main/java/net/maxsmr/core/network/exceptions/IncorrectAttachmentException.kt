package net.maxsmr.core.network.exceptions

import android.content.Context
import net.maxsmr.core.network.R
import okhttp3.Response

class IncorrectAttachmentException(
    response: Response?,
    exceptionMessage: String? = null,
    withBody: Boolean = false,
) : HttpProtocolException(response, exceptionMessage, withBody) {

    constructor(
        context: Context,
        response: Response? = null,
        withBody: Boolean = false,
    ) : this(response, context.getString(R.string.error_http_incorrect_attachment), withBody)
}