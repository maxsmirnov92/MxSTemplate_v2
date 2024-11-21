package net.maxsmr.feature.notification_reader.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.notification_reader.data.NotificationReaderListenerService
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStartResult
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReaderFragment : BaseNavigationFragment<NotificationReaderViewModel>() {

    override val layoutId: Int = R.layout.fragment_notification_reader

    override val viewModel by viewModels<NotificationReaderViewModel>()

    override val menuResId: Int = R.menu.menu_notification_reader

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var manager: NotificationReaderSyncManager

    private var toggleServiceStateMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: NotificationReaderViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        viewModel.serviceTargetState.observe {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val context = requireContext()
                val result = if (it) {
                    val startResult = manager.doStart(context)
                    when (startResult) {
                        ManagerStartResult.SERVICE_START_FAILED -> {
                            viewModel.showSnackbar(TextMessage(R.string.notification_reader_snack_cannot_start_service))
                            false
                        }

                        ManagerStartResult.SETTINGS_NEEDED -> {
                            viewModel.showToast(TextMessage(R.string.notification_reader_toast_start_add_in_settings))
                            false
                        }

                        ManagerStartResult.NOT_IN_FOREGROUND -> {
                            false
                        }

                        ManagerStartResult.SUCCESS -> true
                    }
                } else {
                    manager.doStop(context).also { isRunning ->
                        if (isRunning) {
                            viewModel.showToast(TextMessage(R.string.notification_reader_toast_stop_remove_in_settings))
                        }
                    }
                }
                refreshServiceStateMenuItem(result)
            }
        }
        navigateToManageOverlay()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.serviceTargetState.value == true) {
            if (manager.doStart(requireContext()) == ManagerStartResult.SETTINGS_NEEDED) {
                viewModel.showToast(TextMessage(R.string.notification_reader_toast_start_add_in_settings))
            }
        } else {
            // при возврате с экрана настроек, когда разрешение было отозвано,
            // можно попытаться остановить ещё раз
            manager.doStop(requireContext())
        }
        refreshServiceStateMenuItem()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        toggleServiceStateMenuItem = menu.findItem(R.id.actionServiceStartStop)
        refreshServiceStateMenuItem()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.actionServiceStartStop -> {
                viewModel.toggleServiceTargetState()
                true
            }

            else -> {
                false
            }
        }
    }

    private fun navigateToManageOverlay() {
        val context = requireContext()
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.setData(uri)
            startActivity(intent)
        }
    }

    private fun refreshServiceStateMenuItem(
        isRunning: Boolean = viewModel.isServiceRunning()
    ) {
        toggleServiceStateMenuItem?.let { item ->
            item.setIcon(if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            item.setTitle((if (isRunning) {
                R.string.notification_reader_menu_action_service_stop
            } else {
                R.string.notification_reader_menu_action_service_start
            }))
        }
    }
}