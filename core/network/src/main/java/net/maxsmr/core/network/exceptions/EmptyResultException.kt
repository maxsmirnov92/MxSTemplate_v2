package net.maxsmr.core.network.exceptions

import android.content.Context
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.R

class EmptyResultException(message: String = EMPTY_STRING): RuntimeException(message) {

    constructor(context: Context) : this(context.getString(R.string.no_data))
}