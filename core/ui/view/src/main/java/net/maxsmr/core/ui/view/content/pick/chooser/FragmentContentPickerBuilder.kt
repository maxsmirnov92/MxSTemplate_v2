package net.maxsmr.core.ui.view.content.pick.chooser

import net.maxsmr.core.android.content.pick.ContentPicker
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.permissionchecker.PermissionsHelper

class FragmentContentPickerBuilder(private val fragment: BaseVmFragment<*>) : ContentPicker.Builder(fragment,
    object : ContentPicker.PermissionHandler {

        override val permissionHelper: PermissionsHelper
            get() = fragment.permissionsHelper

        override fun handle(
            requestCode: Int,
            permissions: Set<String>,
            onDenied: (Set<String>) -> Unit,
            onGranted: () -> Unit,
        ) {
            fragment.doOnPermissionsResult(
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
            fragment,
            AppIntentChooserData(code, title, intents)
        )
    })