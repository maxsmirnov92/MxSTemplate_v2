package net.maxsmr.core.android.permissions

import android.content.Context
import androidx.activity.ComponentActivity
import net.maxsmr.permissionchecker.PermissionsHelper

interface PermissionsRequester {
    
    val requireContext: Context

    val requireActivity: ComponentActivity
    
    val permissionsHelper: PermissionsHelper

    fun doOnPermissionsResult(
        code: Int,
        permissions: Collection<String>,
        shouldShowPermanentlyDeniedDialog: Boolean = true,
        onDenied: ((Set<String>) -> Unit)? = null,
        onAllGranted: () -> Unit,
    ): PermissionsHelper.ResultListener?
}