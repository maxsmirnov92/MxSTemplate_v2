package net.maxsmr.feature.preferences.ui

import android.Manifest
import android.content.Context
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.isAtLeastTiramisu
import net.maxsmr.commonutils.live.observeOnce
import net.maxsmr.commonutils.openBatteryOptimizationSettings
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
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

fun CacheDataStoreRepository.doOnPostNotificationPermissionResult(
    fragment: BaseVmFragment<*>,
    onlyWhenGranted: Boolean,
    targetAction: () -> Unit,
) {
    observeOncePostNotificationPermissionAsked(fragment,
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
fun CacheDataStoreRepository.observeOncePostNotificationPermissionAsked(
    fragment: BaseVmFragment<*>,
    onPostNotificationGranted: (() -> Unit)? = null,
    onPostNotificationDenied: (() -> Unit)? = null,
    onPostNotificationAlreadyAsked: ((Boolean) -> Unit)? = null,
) {
    /**
     * после получения разрешения или отказа пользователя получать уведомления - не показывать этот запрос снова
     */
    fun setPostNotificationAsked() {
        fragment.lifecycleScope.launch {
            this@observeOncePostNotificationPermissionAsked.setPostNotificationAsked()
        }
    }
    postNotificationAsked?.asLiveData()?.observeOnce(fragment.viewLifecycleOwner) { asked ->
        if (!asked) {
            fragment.doOnPermissionsResult(
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
                    fragment.permissionsHelper.hasPermissions(
                        fragment.requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
        }
    } ?: onPostNotificationGranted?.invoke()
}

@JvmOverloads
fun CacheDataStoreRepository.observePostNotificationPermissionAsked(
    fragment: BaseVmFragment<*>,
    onPostNotificationGranted: (() -> Unit)? = null,
    onPostNotificationDenied: (() -> Unit)? = null,
    onPostNotificationAlreadyAsked: (() -> Unit)? = null,
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
        } else {
            onPostNotificationAlreadyAsked?.invoke()
        }
    } ?: onPostNotificationGranted?.invoke()
}