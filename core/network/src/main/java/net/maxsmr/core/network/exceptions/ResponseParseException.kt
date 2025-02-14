package net.maxsmr.core.network.exceptions

import net.maxsmr.core.network.CustomErrorCode

class ResponseParseException(
    message: String? = null,
): NetworkException(CustomErrorCode.RESPONSE_PARSE.code, null, message)