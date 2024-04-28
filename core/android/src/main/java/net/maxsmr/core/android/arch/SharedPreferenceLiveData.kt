package net.maxsmr.core.android.arch

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer


abstract class SharedPreferenceLiveData<T>(
    private val sharedPrefs: SharedPreferences,
    private val key: String,
    private val defValue: T,
) : MutableLiveData<T>(), Observer<T> {

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == this.key) {
            value = getValueFromPreferences(sharedPrefs, key, defValue)
        }
    }

    abstract fun getValueFromPreferences(sharedPrefs: SharedPreferences, key: String, defValue: T): T

    protected abstract fun setValueToPreferences(sharedPrefs: SharedPreferences, key: String, value: T)

    override fun onActive() {
        super.onActive()
        observeForever(this)
        value = getValueFromPreferences(sharedPrefs, key, defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        removeObserver(this)
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        super.onInactive()
    }

    override fun onChanged(value: T) {
        setValueToPreferences(sharedPrefs, key, value)
    }

    @Suppress("unused")
    class SharedPreferenceIntLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Int) :
            SharedPreferenceLiveData<Int>(sharedPrefs, key, defValue) {

        override fun getValueFromPreferences(sharedPrefs: SharedPreferences, key: String, defValue: Int): Int =
            sharedPrefs.getInt(key, defValue)

        override fun setValueToPreferences(sharedPrefs: SharedPreferences, key: String, value: Int) {
            sharedPrefs.edit().putInt(key, value).apply()
        }
    }

    @Suppress("unused")
    class SharedPreferenceStringLiveData(sharedPrefs: SharedPreferences, key: String, defValue: String) :
            SharedPreferenceLiveData<String>(sharedPrefs, key, defValue) {

        override fun getValueFromPreferences(sharedPrefs: SharedPreferences, key: String, defValue: String): String =
            sharedPrefs.getString(key, defValue).orEmpty()

        override fun setValueToPreferences(sharedPrefs: SharedPreferences, key: String, value: String) {
            sharedPrefs.edit().putString(key, value).apply()
        }
    }

    @Suppress("unused")
    class SharedPreferenceBooleanLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Boolean) :
            SharedPreferenceLiveData<Boolean>(sharedPrefs, key, defValue) {

        override fun getValueFromPreferences(
            sharedPrefs: SharedPreferences,
            key: String,
            defValue: Boolean,
        ): Boolean = sharedPrefs.getBoolean(key, defValue)

        override fun setValueToPreferences(sharedPrefs: SharedPreferences, key: String, value: Boolean) {
            sharedPrefs.edit().putBoolean(key, value).apply()
        }
    }

    @Suppress("unused")
    class SharedPreferenceFloatLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Float) :
            SharedPreferenceLiveData<Float>(sharedPrefs, key, defValue) {

        override fun getValueFromPreferences(sharedPrefs: SharedPreferences, key: String, defValue: Float): Float =
            sharedPrefs.getFloat(key, defValue)

        override fun setValueToPreferences(sharedPrefs: SharedPreferences, key: String, value: Float) {
            sharedPrefs.edit().putFloat(key, value).apply()
        }
    }

    @Suppress("unused")
    class SharedPreferenceLongLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Long) :
            SharedPreferenceLiveData<Long>(sharedPrefs, key, defValue) {

        override fun getValueFromPreferences(sharedPrefs: SharedPreferences, key: String, defValue: Long): Long =
            sharedPrefs.getLong(key, defValue)

        override fun setValueToPreferences(sharedPrefs: SharedPreferences, key: String, value: Long) {
            sharedPrefs.edit().putLong(key, value).apply()
        }
    }

    @Suppress("unused")
    class SharedPreferenceStringSetLiveData(sharedPrefs: SharedPreferences, key: String, defValue: Set<String>) :
            SharedPreferenceLiveData<Set<String>>(sharedPrefs, key, defValue) {

        override fun getValueFromPreferences(
            sharedPrefs: SharedPreferences,
            key: String,
            defValue: Set<String>,
        ): Set<String> = sharedPrefs.getStringSet(key, defValue).orEmpty()

        override fun setValueToPreferences(sharedPrefs: SharedPreferences, key: String, value: Set<String>) {
            sharedPrefs.edit().putStringSet(key, value).apply()
        }
    }
}