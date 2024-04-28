package net.maxsmr.core.android.content.pick.concrete

import android.os.Parcelable

/**
 * Абстракция параметров запроса взятия контента с конкретного пикера [ConcretePicker]
 */
interface ConcretePickerParams : Parcelable {

    /**
     * Уникальный тип параметров запроса. Используется для того, чтобы при получении результата взятия
     * контента можно было понять, какой конкретный пикер должен его обработать
     */
    val type: ConcretePickerType

    /**
     * Массив разрешений, которые должно иметь приложение, чтобы иметь возможность получать контент
     * с этими параметрами.
     */
    val requiredPermissions: Array<String>
}