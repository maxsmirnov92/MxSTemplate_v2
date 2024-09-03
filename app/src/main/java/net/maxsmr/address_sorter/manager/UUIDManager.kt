package net.maxsmr.address_sorter.manager

import android.content.SharedPreferences
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.util.UUID

/**
 * Генерация, кеширование [java.util.UUID]
 *
 * @since 1.0
 */

class UUIDManager(preferences: SharedPreferences) {

    val uuid: String

    init {
        if (preferences.contains(KEY_CURRENT_UUID)) {
            uuid = preferences.getString(KEY_CURRENT_UUID, EMPTY_STRING).orEmpty()
        } else {
            uuid = UUID.randomUUID().toString()
            preferences.edit().putString(KEY_CURRENT_UUID, uuid).apply()
        }
    }

    companion object {

        private const val KEY_CURRENT_UUID = "current_uuid"
    }
}