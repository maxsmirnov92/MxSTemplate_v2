package net.maxsmr.feature.preferences.ui

import android.Manifest
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.flow.observeLatest
import net.maxsmr.commonutils.getManageOverlayPermissionIntent
import net.maxsmr.commonutils.isAtLeastTiramisu
import net.maxsmr.commonutils.openBatteryOptimizationSettings
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.permissions.ICanAskPermissions
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.permissionchecker.PermissionsHelper

/**
 * @param targetAction true - если спрашивалось ранее
 */
fun CacheDataStoreRepository.doOnBatteryOptimizationAsk(
    viewModel: BaseViewModel,
    context: Context,
    dialogTag: String,
    targetAction: suspend (Boolean) -> Unit,
) {
    viewModel.doOnAnyAskOption(
        flow = batteryOptimizationAsked,
        setAskedFunc = {
            // пустой, т.к. выставляем в диалоге
        }
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
        }
        targetAction(it)
    }
}

/**
 * @param targetAction true - если спрашивалось ранее
 */
fun CacheDataStoreRepository.doOnCanDrawOverlaysAsked(
    viewModel: BaseViewModel,
    context: Context,
    targetAction: (Boolean) -> Unit,
) {
    canDrawOverlaysAsked?.let { flow ->
        viewModel.doOnAnyAskOption(
            flow = flow,
            setAskedFunc = {
                viewModel.viewModelScope.launch {
                    this@doOnCanDrawOverlaysAsked.setCanDrawOverlaysAsked()
                }
            }
        ) {
            if (!it) {
                context.startActivity(getManageOverlayPermissionIntent(context))
            }
            targetAction.invoke(it)
        }
    } ?: targetAction.invoke(false)
}

fun <T> CacheDataStoreRepository.doOnPostNotificationPermissionResult(
    host: T,
    onlyWhenGranted: Boolean,
    targetAction: () -> Unit,
) where T : ICanAskPermissions, T : LifecycleOwner {
    observePostNotificationPermissionAsked(
        host,
        true,
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
        }
    )
}

@JvmOverloads
fun <T> CacheDataStoreRepository.observePostNotificationPermissionAsked(
    host: T,
    shouldObserveOnce: Boolean,
    onPostNotificationGranted: (() -> Unit)? = null,
    onPostNotificationDenied: (() -> Unit)? = null,
    onPostNotificationAlreadyAsked: ((Boolean) -> Unit)? = null,
) where T : ICanAskPermissions, T : LifecycleOwner {
    /**
     * после получения разрешения или отказа пользователя получать уведомления - не показывать этот запрос снова
     */
    fun setPostNotificationAsked() {
        host.lifecycleScope.launch {
            this@observePostNotificationPermissionAsked.setPostNotificationAsked()
        }
    }

    postNotificationAsked?.let {
        if (shouldObserveOnce) {
            it.take(1)
        } else {
            it
        }
    }?.observeLatest(host.lifecycleScope) { asked ->
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
                // проверка по факту уже была ранее
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