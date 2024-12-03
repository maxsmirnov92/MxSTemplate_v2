package net.maxsmr.feature.notification_reader.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
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
import net.maxsmr.commonutils.live.zip
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.databinding.LayoutErrorContainerBinding
import net.maxsmr.feature.demo.DemoChecker
import net.maxsmr.feature.demo.strategies.AlertDemoExpiredStrategy
import net.maxsmr.feature.download.data.DownloadsViewModel
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

    override val viewModel: NotificationReaderViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            factory.create(it, downloadsViewModel)
        }
    }

    override val menuResId: Int = R.menu.menu_notification_reader

    protected val binding by viewBinding(FragmentNotificationReaderBinding::bind)

    private val downloadsViewModel: DownloadsViewModel by activityViewModels()

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
    lateinit var factory: NotificationReaderViewModel.Factory

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
    private var clearSuccessMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: NotificationReaderViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        viewModel.serviceTargetState.observe {
            if (it != null) {
                doStartOrStop(it.changedFromView)
            }
        }

        with(binding) {
            val errorBinding = LayoutErrorContainerBinding.bind(containerPackageListError.root)

            viewModel.notificationsItems.observe {
                adapter.items = it
                if (it.isNotEmpty()) {
                    rvNotifications.isVisible = true
                    tvNotificationsEmpty.isVisible = false
                } else {
                    rvNotifications.isVisible = false
                    tvNotificationsEmpty.isVisible = true
                }
                refreshClearSuccessMenuItem()
            }

            zip(viewModel.isRunning, viewModel.packageListLoadState) { isRunning, loadState ->
                isRunning to loadState
            }.observe {
                val (isRunning, loadState) = it
                val isSuccess = loadState?.isSuccessWithData() != false
                if (isRunning == true) {
                    containerPackageListState.isVisible = !isSuccess
                    if (loadState?.isLoading == true) {
                        containerPackageListLoading.isVisible = true
                        containerPackageListError.root.isVisible = false
                    } else {
                        containerPackageListLoading.isVisible = false
                        containerPackageListError.root.isVisible = !isSuccess
                        errorBinding.tvEmptyError.text =
                            loadState?.error?.message?.takeIf { message -> message.isNotEmpty() }?.let { message ->
                                getString(R.string.notification_reader_package_list_error_format, message)
                            } ?: getString(R.string.notification_reader_package_list_error)
                    }
                } else {
                    containerPackageListState.isVisible = false
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

            errorBinding.tvEmptyError.setTextColor(ContextCompat.getColor(requireContext(), net.maxsmr.core.ui.R.color.textColorError))
            errorBinding.btRetry.setOnClickListener {
                viewModel.onDownloadPackageListAction()
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
        clearSuccessMenuItem = menu.findItem(R.id.actionClearSuccess)
        refreshMenuItemsByRunning()
        refreshClearSuccessMenuItem()
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

            R.id.actionClearSuccess -> {
                viewModel.onClearSuccessAction()
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

    private fun refreshClearSuccessMenuItem() {
        clearSuccessMenuItem?.isVisible = viewModel.notificationsItems.value
            ?.any { it.status is NotificationReaderEntity.Success } == true
    }
}