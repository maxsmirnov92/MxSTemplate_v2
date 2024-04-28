package net.maxsmr.core.ui.content.pick.chooser

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.content.pick.IntentWithPermissions
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams

@Parcelize
internal class AppIntentChooserData(
    val requestCode: Int,
    val title: TextMessage,
    val intents: Map<ConcretePickerParams, IntentWithPermissions>,
) : Parcelable