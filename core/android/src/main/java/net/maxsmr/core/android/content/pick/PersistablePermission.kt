package net.maxsmr.core.android.content.pick

import android.content.Intent

enum class PersistablePermission {
    NONE,
    READ_ONLY,
    WRITE_ONLY,
    READ_AND_WRITE,
    ;

    fun modeFlags() = when (this) {
        NONE -> null
        READ_ONLY -> Intent.FLAG_GRANT_READ_URI_PERMISSION
        WRITE_ONLY -> Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        READ_AND_WRITE -> Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}