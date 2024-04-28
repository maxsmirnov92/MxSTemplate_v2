package net.maxsmr.core.utils

import net.maxsmr.commonutils.logger.BaseLogger
import java.nio.charset.Charset

fun String?.charsetForNameOrNull() =
    try {
        Charset.forName(this)
    } catch (e: IllegalArgumentException) {
        null
    }