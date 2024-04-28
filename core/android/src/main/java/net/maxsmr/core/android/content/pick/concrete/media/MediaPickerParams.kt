package net.maxsmr.core.android.content.pick.concrete.media

import kotlinx.parcelize.Parcelize
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerType

@Parcelize
class MediaPickerParams(
        val contentType: ContentType,
) : ConcretePickerParams {

    override val type: ConcretePickerType
        get() = ConcretePickerType.MEDIA

    override val requiredPermissions: Array<String>
        get() = emptyArray()
}