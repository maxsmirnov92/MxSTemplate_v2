package net.maxsmr.feature.preferences.ui

import android.Manifest
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.isAtLeastTiramisu
import net.maxsmr.commonutils.live.observeOnce
import net.maxsmr.commonutils.openBatteryOptimizationSettings
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.permissions.ICanAskPermissions
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.permissionchecker.PermissionsHelper

fun CacheDataStoreRepository.doOnBatteryOptimizationAsk(
    viewModel: BaseViewModel,
    context: Context,
    dialogTag: String,
    targetAction: () -> Unit,
) {
    viewModel.doOnAnyAskOption(
        batteryOptimizationAsked,
        {}
    ) {
        if (!it) {
            viewModel.showOkDialog(
                dialogTag,
                net.maxsmr.core.ui.R.string.dialog_battery_optimization_message,
                configBlock = {
                    this.setUniqueStrategy(AlertQueueItem.UniqueStrategy.Ignore)
                }
            ) {
                viewModel.viewModelScope.launch {
                    setBatteryOptimizationAsked()
                    context.openBatteryOptimizationSettings()
                }
            }
        } else {
            targetAction()
        }
    }
}

fun <T>  CacheDataStoreRepository.doOnPostNotificationPermissionResult(
    host: T,
    onlyWhenGranted: Boolean,
    targetAction: () -> Unit,
) where T: ICanAskPermissions, T: LifecycleOwner {
    observeOncePostNotificationPermissionAsked(host,
        targetAction,
        onPostNotificationDenied = {
            if (!onlyWhenGranted) {
                targetAction()
            }
        },
        onPostNotificationAlreadyAsked = {
            if (!onlyWhenGranted || it) {
                targetAction()
            }
        })
}

@JvmOverloads
fun <T> CacheDataStoreRepository.observeOncePostNotificationPermissionAsked(
    host: T,
    onPostNotificationGranted: (() -> Unit)? = null,
    onPostNotificationDenied: (() -> Unit)? = null,
    onPostNotificationAlreadyAsked: ((Boolean) -> Unit)? = null,
) where T: ICanAskPermissions, T: LifecycleOwner {
    /**
     * после получения разрешения или отказа пользователя получать уведомления - не показывать этот запрос снова
     */
    fun setPostNotificationAsked() {
        host.lifecycleScope.launch {
            this@observeOncePostNotificationPermissionAsked.setPostNotificationAsked()
        }
    }
    postNotificationAsked?.asLiveData()?.observeOnce(host) { asked ->
        if (!asked) {
            host.doOnPermissionsResult(
                BaseActivity.REQUEST_CODE_PERMISSION_NOTIFICATIONS,
                PermissionsHelper.withPostNotificationsByApiVersion(emptySet()),
                onDenied = {
                    setPostNotificationAsked()
                    onPostNotificationDenied?.invoke()
                }
            ) {
                setPostNotificationAsked()
                onPostNotificationGranted?.invoke()
            }
        } else {
            if (isAtLeastTiramisu()) {
                onPostNotificationAlreadyAsked?.invoke(
                    host.permissionsHelper.hasPermissions(
                        host.attachedContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
        }
    } ?: onPostNotificationGranted?.invoke()
}

@JvmOverloads
fun <T> CacheDataStoreRepository.observePostNotificationPermissionAsked(
    host: T,
    onPostNotificationGranted: (() -> Unit)? = null,
    onPostNotificationDenied: (() -> Unit)? = null,
    onPostNotificationAlreadyAsked: (() -> Unit)? = null,
) where T: ICanAskPermissions, T: LifecycleOwner {
    /**
     * после получения разрешения или отказа пользователя получать уведомления - не показывать этот запрос снова
     */
    fun setPostNotificationAsked() {
        host.lifecycleScope.launch {
            this@observePostNotificationPermissionAsked.setPostNotificationAsked()
        }
    }
    postNotificationAsked?.asLiveData()?.observe(host) { asked ->
        if (!asked) {
            host.doOnPermissionsResult(
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
        } else {
            onPostNotificationAlreadyAsked?.invoke()
        }
    } ?: onPostNotificationGranted?.invoke()
}