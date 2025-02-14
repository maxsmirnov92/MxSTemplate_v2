package net.maxsmr.core.network.exceptions

open class ApiException(
    val code: Int,
    message: String? = null,
) : RuntimeException(message) {

    companion object {

        @JvmStatic
        fun Throwable.isApiException(code: Int) = this is ApiException && this.code == code
    }
}