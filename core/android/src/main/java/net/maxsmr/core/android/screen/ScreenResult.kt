package net.maxsmr.core.android.screen

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.serialization.KSerializer
import net.maxsmr.core.android.baseJson

/**
 * Общий предок для всех результатов экрана.
 *
 * **Обязательные** правила для наследников:
 * 1. Пометка аннотацией [@Serializable][kotlinx.serialization.Serializable]
 * 2. Пометка аннотацией [@Parcelize][kotlinx.parcelize.Parcelize]
 * 3. Реализация контракта [ScreenResultParceler] через **companion object**
 *
 * Все вложенные объекты должны помечаться аннотацией [@SerialName][kotlinx.serialization.SerialName]
 *
 * Используется для передачи результатов между экранами через механизм Fragment Result API.

 * @author shmoilov.ee
 * */
interface ScreenResult : Parcelable

/**
 * Контракт механизма парселизации результатов экрана.
 * Нужен для корректной передачи результатов между экранами.
 *
 * Сериализует данные через [KotlinX Serialization][kotlinx.serialization.Serializable],
 * поэтому вложенные объекты не обязаны наследоваться от [Parcelable].
 * */
interface ScreenResultParceler<T : ScreenResult> : Parceler<T> {

    val serializer: KSerializer<T>

    override fun create(parcel: Parcel): T {
        val json = requireNotNull(parcel.readString()) {
            "Unable to restore ScreenResult $this"
        }
        return baseJson.decodeFromString(serializer, json)
    }

    override fun T.write(parcel: Parcel, flags: Int) {
        parcel.writeString(baseJson.encodeToString(serializer, this))
    }
}
