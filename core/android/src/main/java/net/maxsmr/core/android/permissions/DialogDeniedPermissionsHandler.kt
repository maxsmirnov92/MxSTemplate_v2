package net.maxsmr.core.android.permissions

import android.app.Activity
import android.content.DialogInterface
import net.maxsmr.commonutils.getAppSettingsIntent
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.permissionchecker.BaseDeniedPermissionsHandler

class DialogDeniedPermissionsHandler(
    private val viewModel: BaseViewModel,
    private val activity: Activity,
) : BaseDeniedPermissionsHandler() {

    override fun doShowMessage(
        requestCode: Int,
        message: String,
        deniedPerms: Set<String>,
        negativeAction: ((Set<String>) -> Unit)?,
    ) {
        viewModel.showYesNoPermissionDialog(
            TextMessage(message)
        ) {
            if (it == DialogInterface.BUTTON_POSITIVE) {
                activity.startActivityForResult(getAppSettingsIntent(activity), requestCode)
            } else {
                negativeAction?.invoke(deniedPerms)
            }
        }
    }

    override fun formatDeniedPermissionsMessage(perms: Collection<String>): String =
        activity.formatDeniedPermissionsMessage(perms)
}