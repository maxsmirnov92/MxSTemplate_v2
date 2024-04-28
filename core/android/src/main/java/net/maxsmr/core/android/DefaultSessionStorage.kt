package net.maxsmr.core.android

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.arch.SharedPreferenceLiveData
import net.maxsmr.core.network.SessionStorage


open class DefaultSessionStorage(private val prefs: SharedPreferences) : SessionStorage {

    override var session: String?
        get() {
            return prefs.getString(SESSION, EMPTY_STRING)
        }
        set(value) {
            prefs.edit().putString(SESSION, value).apply()
        }


    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    override fun clear() {
        prefs.edit().clear().commit()
    }

    fun observe(): LiveData<String> =
        SharedPreferenceLiveData.SharedPreferenceStringLiveData(prefs, SESSION, EMPTY_STRING)

    companion object {

        private const val SESSION = "session"
    }
}