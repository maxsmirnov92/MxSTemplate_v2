package net.maxsmr.core.android.content.pick

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.maxsmr.commonutils.flatten
import net.maxsmr.permissionchecker.PermissionsHelper

@Parcelize
class IntentWithPermissions(
    val intent: Intent,
    val permissions: Array<String>,
) : Parcelable {

    fun flatten(context: Context): List<IntentWithPermissions> {
        val intents = intent.flatten(context)
        val result: List<Intent> = intents.ifEmpty { listOf(intent) }
        return result.map { IntentWithPermissions(it, permissions) }
    }
}