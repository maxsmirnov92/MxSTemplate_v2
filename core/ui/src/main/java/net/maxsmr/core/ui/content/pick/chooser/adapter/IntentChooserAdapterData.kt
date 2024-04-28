package net.maxsmr.core.ui.content.pick.chooser.adapter

import android.graphics.drawable.Drawable
import net.maxsmr.core.android.content.pick.IntentWithPermissions
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams

internal sealed class IntentChooserAdapterData {

    class App(
        val params: ConcretePickerParams,
        val intentWithPermissions: IntentWithPermissions,
        val icon: Drawable,
        val label: CharSequence,
    ) : IntentChooserAdapterData()

    class Permissions(
            val permissions: List<String>
    ): IntentChooserAdapterData()
}
