package net.maxsmr.core.android.content.pick

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.maxsmr.commonutils.flatten

@Parcelize
class IntentWithPermissions(
    val intent: Intent,
    val permissions: Array<String>,
) : Parcelable {

    fun flatten(context: Context) = intent.flatten(context)
        .map { IntentWithPermissions(it, permissions) }
}