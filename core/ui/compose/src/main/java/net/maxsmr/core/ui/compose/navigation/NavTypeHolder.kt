package net.maxsmr.core.ui.compose.navigation

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.getParcelableCompat
import net.maxsmr.commonutils.getSerializableCompat
import net.maxsmr.core.utils.kotlinx.serialization.encodeToStringOrNull
import java.io.Serializable

/**
 * Содержит маппинг с [NavType]
 * соответствующего типа аргумента от [Parcelable] для навигации
 */
object NavTypeHolder {

    lateinit var json: Json

    @PublishedApi
    internal val navTypeMap = mutableMapOf<Class<*>, NavType<*>>()

    @PublishedApi
    internal inline fun <reified T : Parcelable> registerParcelableNavType(): NavType<T> {
        return object : NavType<T>(false) {

            override fun get(bundle: Bundle, key: String): T? {
                return bundle.getParcelableCompat(key)
            }

            override fun parseValue(value: String): T {
                return json.decodeFromString(value)
            }

            override fun put(bundle: Bundle, key: String, value: T) {
                bundle.putParcelable(key, value)
            }
        }.also {
            navTypeMap[T::class.java] = it
        }
    }

    @PublishedApi
    internal inline fun <reified T : Serializable> registerSerializableNavType(): NavType<T> {
        return object : NavType<T>(false) {

            override fun get(bundle: Bundle, key: String): T? {
                return bundle.getSerializableCompat(key)
            }

            override fun parseValue(value: String): T {
                return json.decodeFromString(value)
            }

            override fun put(bundle: Bundle, key: String, value: T) {
                bundle.putSerializable(key, value)
            }
        }.also {
            navTypeMap[T::class.java] = it
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Parcelable> getParcelableNavType(): NavType<T> {
        return navTypeMap[T::class.java] as? NavType<T> ?: registerParcelableNavType<T>()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Serializable> getSerializableNavType(): NavType<T> {
        return navTypeMap[T::class.java] as? NavType<T> ?: registerSerializableNavType<T>()
    }

    inline fun <reified T> encodeArg(arg: T): String {
        return Uri.encode(json.encodeToStringOrNull(arg)).orEmpty()
    }
}