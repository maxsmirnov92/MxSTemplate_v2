package net.maxsmr.core.android.content.pick

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.maxsmr.commonutils.flatten
import net.maxsmr.commonutils.isAtLeastTiramisu
import net.maxsmr.permissionchecker.PermissionsHelper

@Parcelize
class IntentWithPermissions(
    val intent: Intent,
    val permissions: Array<String>,
) : Parcelable {

    fun flatten(context: Context, permissionsHelper: PermissionsHelper): List<IntentWithPermissions> {
        val intents: List<Intent> = if (!isAtLeastTiramisu()
                // на 33 без этого пермишна queryIntentActivities вернёт пустой список
                // или надо прописывать <queries> в манифесте своего аппа
                || permissionsHelper.hasPermissions(
                    context,
                    android.Manifest.permission.QUERY_ALL_PACKAGES
                )
        ) {
            intent.flatten(context)
        } else {
            listOf(intent)
        }
        return intents.map { IntentWithPermissions(it, permissions) }
    }
}