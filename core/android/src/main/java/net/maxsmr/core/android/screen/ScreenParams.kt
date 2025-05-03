package net.maxsmr.core.android.screen

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.serialization.KSerializer
import net.maxsmr.core.android.baseJson

/**
 * Общий предок для всех параметров экрана.
 *
 * **Обязательные** правила для наследников:
 * 1. Пометка аннотацией [@Serializable][kotlinx.serialization.Serializable]
 * 2. Пометка аннотацией [@Parcelize][kotlinx.parcelize.Parcelize]
 * 3. Реализация контракта [ScreenParamsParceler] через **companion object** (Пример: ShiftsEditorScreenParams)
 *
 * Все вложенные объекты должны помечаться аннотацией [@SerialName][kotlinx.serialization.SerialName]

 * @author shmoilov.ee
 * */
interface ScreenParams : Parcelable

/**
 * Контракт механизма парселизации параметров экрана.
 * Нужен для корректной работы с текущей навигацией
 *
 * Сериализует данные через [KotlinX Serialization][kotlinx.serialization.Serializable],
 * поэтому вложенные объекты не обязаны наследоваться от [Parcelable].
 *
 * */
interface ScreenParamsParceler<T : ScreenParams> : Parceler<T> {

    val serializer: KSerializer<T>

    override fun create(parcel: Parcel): T {
        val json = requireNotNull(parcel.readString()) {
            "Unable to restore ScreenParams from $this"
        }
        return baseJson.decodeFromString(serializer, json)
    }

    override fun T.write(parcel: Parcel, flags: Int) {
        parcel.writeString(baseJson.encodeToString(serializer, this))
    }
}
