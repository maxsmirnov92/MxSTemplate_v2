package net.maxsmr.feature.preferences.ui

import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.permissionchecker.PermissionsHelper

/**
 * Обозревать при инициализации главного экрана
 */
@JvmOverloads
fun CacheDataStoreRepository.observePostNotificationPermissionAsked(
    fragment: BaseVmFragment<*>,
    onPostNotificationGranted: (() -> Unit)? = null,
    onPostNotificationDenied: (() -> Unit)? = null,
) {
    /**
     * после получения разрешения или отказа пользователя получать уведомления - не показывать этот запрос снова
     */
    fun setPostNotificationAsked() {
        fragment.lifecycleScope.launch {
            this@observePostNotificationPermissionAsked.setPostNotificationAsked()
        }
    }
    postNotificationAsked?.asLiveData()?.observe(fragment.viewLifecycleOwner) { asked ->
        if (!asked) {
            fragment.doOnPermissionsResult(
                BaseActivity.REQUEST_CODE_PERMISSION_NOTIFICATIONS,
                PermissionsHelper.withPostNotificationsByApiVersion(emptySet()),
                onDenied = {
                    setPostNotificationAsked()
                    onPostNotificationGranted?.invoke()
                }
            ) {
                setPostNotificationAsked()
                onPostNotificationDenied?.invoke()
            }
        }
    }
}