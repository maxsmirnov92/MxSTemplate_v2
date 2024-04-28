package net.maxsmr.core.android.content.pick

import android.net.Uri
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerType

/**
 * Результат взятия контента
 */
sealed class PickResult {

    abstract val requestCode: Int


    class Success(
            override val requestCode: Int,
            val uri: Uri,
            val pickerType: ConcretePickerType
    ) : PickResult()


    class Error(
        override val requestCode: Int,
        val reason: TextMessage,
        val exception: Throwable? = null,
    ) : PickResult()
}