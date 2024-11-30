package net.maxsmr.feature.notification_reader.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.coroutines.launch
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.drag.DragAndDropTouchHelperCallback
import net.maxsmr.android.recyclerview.adapters.base.drag.OnStartDragHelperListener
import net.maxsmr.android.recyclerview.views.decoration.Divider
import net.maxsmr.android.recyclerview.views.decoration.DividerItemDecoration
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.demo.DemoChecker
import net.maxsmr.feature.demo.strategies.AlertDemoExpiredStrategy
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStartResult
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStopResult
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapter
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData
import net.maxsmr.feature.notification_reader.ui.databinding.FragmentNotificationReaderBinding
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.preferences.ui.doOnCanDrawOverlaysAsked
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

open class NotificationReaderFragment : BaseNavigationFragment<NotificationReaderViewModel>(),
        BaseDraggableDelegationAdapter.ItemsEventsListener<NotificationsAdapterData> {

    override val layoutId: Int = R.layout.fragment_notification_reader

    override val viewModel by viewModels<NotificationReaderViewModel>()

    override val menuResId: Int = R.menu.menu_notification_reader

    protected val binding by viewBinding(FragmentNotificationReaderBinding::bind)

    private val adapter = NotificationsAdapter()

    private val touchHelper: ItemTouchHelper = ItemTouchHelper(DragAndDropTouchHelperCallback(adapter)).also {
        adapter.startDragListener = OnStartDragHelperListener(it)
    }

    private val strategy: AlertDemoExpiredStrategy by lazy {
        AlertDemoExpiredStrategy(
            viewModel,
            this,
            confirmAction = AlertDemoExpiredStrategy.ConfirmAction.EXIT_PROCESS
        )
    }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var manager: NotificationReaderSyncManager

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    lateinit var settingsRepo: SettingsDataStoreRepository

    @Inject
    lateinit var demoChecker: DemoChecker

    private var toggleServiceStateMenuItem: MenuItem? = null
    private var downloadPackageListMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: NotificationReaderViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        viewModel.serviceTargetState.observe {
            if (it != null) {
                doStartOrStop(it.changedFromView)
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
            touchHelper.attachToRecyclerView(rvNotifications)
            adapter.registerItemsEventsListener(this@NotificationReaderFragment)
            rvNotifications.addItemDecoration(
                DividerItemDecoration.Builder(requireContext())
                    .setDivider(Divider.Space(8), DividerItemDecoration.Mode.ALL)
                    .build()
            )
        }

        viewModel.doOnCanDrawOverlaysAsked(this, cacheRepo, settingsRepo) {
            if (it) {
                viewModel.showToast(TextMessage(R.string.notification_reader_toast_can_draw_overlays_settings))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.lastStartResult == ManagerStartResult.SETTINGS_NEEDED
                || viewModel.lastStopResult == ManagerStopResult.SETTINGS_NEEDED
        ) {
            // продолжить после возврата с настроек
            // и не переходить в настройки для стопа
            doStartOrStop(false)
        }
        refreshMenuItemsByRunning()
        lifecycleScope.launch {
            demoChecker.check(strategy)
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        toggleServiceStateMenuItem = menu.findItem(R.id.actionServiceStartStop)
        downloadPackageListMenuItem = menu.findItem(R.id.actionDownloadPackageList)
        refreshMenuItemsByRunning()
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

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.unregisterItemsEventsListener(this)
    }

    override fun onItemRemoved(position: Int, item: NotificationsAdapterData) {
        viewModel.onRemoveSuccessNotification(item)
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int, item: NotificationsAdapterData) {
        throw UnsupportedOperationException("Move NotificationsAdapterData not supported")
    }

    private fun doStartOrStop(navigateToSettingsForStop: Boolean) {
        viewModel.doStartOrStop(this, navigateToSettingsForStop) { (isStarted, startResult, stopResult) ->
            // рефреш меню сразу в зав-ти от результата старт/стоп,
            // а не текущего состояния сервиса (ещё не успело измениться)
            refreshMenuItemsByRunning(isStarted)
        }
    }

    private fun refreshMenuItemsByRunning(
        isRunning: Boolean = viewModel.isServiceRunning(),
    ) {
        toggleServiceStateMenuItem?.let { item ->
            item.setIcon(if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            item.setTitle(
                (if (isRunning) {
                    R.string.notification_reader_menu_action_service_stop
                } else {
                    R.string.notification_reader_menu_action_service_start
                })
            )
        }
        downloadPackageListMenuItem?.let { item ->
            item.isVisible = isRunning
        }
    }
}