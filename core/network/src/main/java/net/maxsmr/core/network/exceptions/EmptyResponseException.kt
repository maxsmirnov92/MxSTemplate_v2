package net.maxsmr.core.network.exceptions

import android.content.Context
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.R

class EmptyResponseException(message: String = EMPTY_STRING): RuntimeException(message) {

    constructor(context: Context) : this(context.getString(R.string.error_empty_response))
}