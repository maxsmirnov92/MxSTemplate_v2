package net.maxsmr.core.ui.view.content.pick.chooser

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import net.maxsmr.core.android.base.result.ICanRegisterForActivityResult
import net.maxsmr.core.android.content.pick.ContentPicker
import net.maxsmr.core.android.permissions.ICanAskPermissions

class HandlerContentPickerBuilder<T>(
    host: T,
) : ContentPicker.Builder<T>(
    host,
    object : ContentPicker.PermissionHandler {

        override fun handle(
            requestCode: Int,
            permissions: Set<String>,
            onDenied: (Set<String>) -> Unit,
            onGranted: () -> Unit,
        ) {
            host.doOnPermissionsResult(
                requestCode,
                permissions,
                false,
                onDenied = onDenied,
                onAllGranted = onGranted
            )
        }
    },
    { code, title, intents ->
        AppIntentChooserDialog.show(
            host,
            AppIntentChooserData(code, title, intents)
        )
    }) where T : ICanAskPermissions, T : ICanRegisterForActivityResult, T : ViewModelStoreOwner, T : LifecycleOwner