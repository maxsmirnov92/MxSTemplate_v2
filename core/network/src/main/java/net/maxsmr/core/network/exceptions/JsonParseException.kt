package net.maxsmr.core.network.exceptions

import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.CustomErrorCode

class JsonParseException(
    message: String = EMPTY_STRING
): NetworkException(CustomErrorCode.JSON_PARSE.code, message)