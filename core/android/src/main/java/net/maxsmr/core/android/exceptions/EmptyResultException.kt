package net.maxsmr.core.android.exceptions

import android.content.Context
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.R

class EmptyResultException(message: String = EMPTY_STRING): RuntimeException(message) {

    constructor(context: Context, isResponse: Boolean) : this(context.getString(if (isResponse) {
        R.string.error_no_response_data
    } else {
        R.string.error_no_data
    }))
}