package net.maxsmr.core.android.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import net.maxsmr.core.android.R
import java.util.Locale

fun Context.getPermissionName(permission: String): String {
    return when (permission) {
        Manifest.permission.CAMERA ->
            getString(R.string.permission_camera)
        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE ->
            getString(R.string.permission_storage)
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION ->
            getString(R.string.permission_location)
        Manifest.permission.RECORD_AUDIO ->
            getString(R.string.permission_microphone)
        Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR ->
            getString(R.string.permission_calendar)
        else -> try {
            //в этом случае название не совпадает
            val pm = packageManager
            pm.getPermissionInfo(permission, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            permission
        }
    }
}

fun Context.formatDeniedPermissionsMessage(perms: Collection<String>): String {
    val sb = StringBuilder()
    for (perm in perms) {
        val permission = getPermissionName(perm)
        if (!sb.toString().contains(permission)) {
            sb.append(permission).append(", ")
        }
    }
    sb.delete(sb.length - 2, sb.length)
    return getString(
        R.string.permission_request,
        sb.toString().lowercase(Locale.getDefault()))
}