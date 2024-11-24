package net.maxsmr.feature.notification_reader.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.android.recyclerview.views.decoration.Divider
import net.maxsmr.android.recyclerview.views.decoration.DividerItemDecoration
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStartResult
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapter
import net.maxsmr.feature.notification_reader.ui.databinding.FragmentNotificationReaderBinding
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.preferences.ui.doOnCanDrawOverlaysAsked
import net.maxsmr.feature.preferences.ui.observePostNotificationPermissionAsked
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReaderFragment : BaseNavigationFragment<NotificationReaderViewModel>() {

    override val layoutId: Int = R.layout.fragment_notification_reader

    override val viewModel by viewModels<NotificationReaderViewModel>()

    override val menuResId: Int = R.menu.menu_notification_reader

    private val binding by viewBinding(FragmentNotificationReaderBinding::bind)

    private val adapter = NotificationsAdapter()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var manager: NotificationReaderSyncManager

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    lateinit var settingsRepo: SettingsDataStoreRepository

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

                        ManagerStartResult.SUCCESS, ManagerStartResult.SUCCESS_PENDING -> true
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

        with(binding) {
            viewModel.notificationsItems.observe {
                adapter.items = it
                if (it.isNotEmpty()) {
                    rvNotifications.isVisible = true
                    tvNotificationsEmpty.isVisible = false
                } else {
                    rvNotifications.isVisible = false
                    tvNotificationsEmpty.isVisible = true
                }
            }

            rvNotifications.adapter = adapter
            rvNotifications.addItemDecoration(
                DividerItemDecoration.Builder(requireContext())
                    .setDivider(Divider.Space(8), DividerItemDecoration.Mode.ALL)
                    .build()
            )
        }

        settingsRepo.settingsFlow.collectSafely {
            if (!it.disableNotifications) {
                // post_notifications не является обязательным для работы сервиса,
                // но спрашиваем чтобы нотификации от двух сервисов были
                cacheRepo.observePostNotificationPermissionAsked(this)
            }
        }

        viewModel.doOnCanDrawOverlaysAsked(this, cacheRepo, settingsRepo) {
            if (it) {
                viewModel.showToast(TextMessage(R.string.notification_reader_toast_can_draw_overlays_settings))
            }
        }
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
            manager.doStop(requireContext(), false)
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
                viewModel.onToggleServiceTargetStateAction()
                true
            }
            R.id.actionDownloadPackageList -> {
                viewModel.onDownloadPackageListAction()
                true
            }

            else -> {
                false
            }
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