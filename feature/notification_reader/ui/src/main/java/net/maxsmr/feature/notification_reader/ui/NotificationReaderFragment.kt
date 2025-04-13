package net.maxsmr.feature.notification_reader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.drag.DragAndDropTouchHelperCallback
import net.maxsmr.android.recyclerview.adapters.base.drag.OnStartDragHelperListener
import net.maxsmr.android.recyclerview.views.decoration.Divider
import net.maxsmr.android.recyclerview.views.decoration.DividerItemDecoration
import net.maxsmr.commonutils.graphic.createBitmapDrawable
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.gui.hideKeyboard
import net.maxsmr.commonutils.gui.listeners.NumberedClickListener
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.message.errorMessage
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.commonutils.live.zip
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.ui.alert.BaseAlertDelegate
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.view.alert.delegate.CombinedViewFragmentAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.DialogViewAlertRepresentation
import net.maxsmr.core.ui.view.databinding.LayoutErrorContainerBinding
import net.maxsmr.feature.demo.DemoChecker
import net.maxsmr.feature.demo.strategies.AlertDemoExpiredStrategy
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStartResult
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStopResult
import net.maxsmr.feature.notification_reader.ui.adapter.AppInfoAdapter
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapter
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData
import net.maxsmr.feature.notification_reader.ui.databinding.DialogInputApiKeyBinding
import net.maxsmr.feature.notification_reader.ui.databinding.FragmentNotificationReaderBinding
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.preferences.ui.doOnCanDrawOverlaysAsked
import net.maxsmr.permissionchecker.PermissionsHelper
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class NotificationReaderFragment : BaseNavigationFragment<NotificationReaderViewModel, StandardAlertRepresentation>(),
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

    private val packageNamesAdapter = AppInfoAdapter()
    private val notificationsAdapter = NotificationsAdapter {
        viewModel.onRetryFailedNotification(it.id)
    }

    private val touchHelper: ItemTouchHelper =
        ItemTouchHelper(DragAndDropTouchHelperCallback(notificationsAdapter)).also {
            notificationsAdapter.startDragListener = OnStartDragHelperListener(it)
        }

    private val strategy: AlertDemoExpiredStrategy by lazy {
        AlertDemoExpiredStrategy(
            viewModel,
            requireActivity(),
            confirmAction = AlertDemoExpiredStrategy.ConfirmAction.EXIT_PROCESS
        )
    }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var factory: NotificationReaderViewModel.Factory

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    lateinit var settingsRepo: SettingsDataStoreRepository

    @Inject
    lateinit var demoChecker: DemoChecker

    private var toggleServiceStateMenuItem: MenuItem? = null
    private var downloadAppsListMenuItem: MenuItem? = null
    private var retryFailedMenuItem: MenuItem? = null
    private var clearSuccessMenuItem: MenuItem? = null

    override fun createAlertDelegate(): BaseAlertDelegate<NotificationReaderViewModel, StandardAlertRepresentation> =
        CombinedViewFragmentAlertDelegate(
            listOf(
                NotificationReaderFragmentAlertDelegate(this, viewModel),
                AlertDemoExpiredStrategy.DemoViewFragmentAlertDelegate(this, viewModel)
            ),
            this,
            viewModel
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: NotificationReaderViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        viewModel.resetServiceTargetStateViewFlag()
        viewModel.serviceTargetState.observe {
            if (it != null) {
                doStartOrStop(it.changedFromView)
            }
        }

        with(binding) {
            val errorBinding = LayoutErrorContainerBinding.bind(containerAppsListError.root)

            viewModel.notificationsItems.observe {
                notificationsAdapter.items = it
                if (it.isNotEmpty()) {
                    rvNotifications.isVisible = true
                    tvNotificationsEmpty.isVisible = false
                } else {
                    rvNotifications.isVisible = false
                    tvNotificationsEmpty.isVisible = true
                }
                refreshRetryFailedMenuItem()
                refreshClearSuccessMenuItem()
            }

            zip(viewModel.isRunning, viewModel.appsListLoadState) { isRunning, loadState ->
                isRunning to loadState
            }.observe {
                val (isRunning, loadState) = it
                if (isRunning == true) {
                    if (loadState?.isLoading == true) {
                        containerAppsListLoading.isVisible = true
                        containerAppsList.isVisible = false
                        containerAppsListError.root.isVisible = false
                        containerAppsListState.isVisible = true
                    } else {
                        containerAppsListLoading.isVisible = false
                        if (loadState?.isSuccessWithData { state -> !state?.infos.isNullOrEmpty() } == true) {
                            val data = loadState.data ?: return@observe
                            tvAppsListSubtitle.text = getString(
                                if (data.isWhiteList) {
                                    R.string.notification_reader_apps_list_white_subtitle
                                } else {
                                    R.string.notification_reader_apps_list_black_subtitle
                                }
                            )
                            packageNamesAdapter.items = data.infos
                            containerAppsList.isVisible = true
                            containerAppsListError.root.isVisible = false
                            containerAppsListState.isVisible = true
                        } else {
                            containerAppsList.isVisible = false
                            if (loadState != null && loadState.isError()) {
                                val error = loadState.error
                                errorBinding.tvEmptyError.text =
                                    if (error?.error !is CancellationException) {
                                        error?.errorMessage()?.get(requireContext())?.takeIf { message ->
                                            message.isNotEmpty()
                                        }?.let { message ->
                                            getString(R.string.notification_reader_apps_list_error_format, message)
                                        } ?: getString(R.string.notification_reader_apps_list_error)
                                    } else {
                                        getString(R.string.notification_reader_apps_list_cancelled)
                                    }
                                containerAppsListError.root.isVisible = true
                            } else {
                                containerAppsListError.root.isVisible = false
                            }
                            containerAppsListState.isVisible = loadState != null
                        }
                    }
                } else {
                    containerAppsListState.isVisible = false
                }
            }
            zip(viewModel.isRunning, viewModel.settings) { isRunning, settings ->
                isRunning to settings
            }.observe {
                refreshDownloadAppsListMenuItem()
            }

            viewModel.appsListExpandedState.observe {
                ContextCompat.getDrawable(
                    requireContext(),
                    if (it) {
                        R.drawable.ic_arrow_up
                    } else {
                        R.drawable.ic_arrow_down
                    }
                )?.let { d ->
                    tvAppsListSubtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        d.createBitmapDrawable(requireContext(), 30, 30),
                        null
                    )
                }
                rvAppList.isVisible = it
            }

            rvAppList.adapter = packageNamesAdapter
            rvNotifications.adapter = notificationsAdapter
            touchHelper.attachToRecyclerView(rvNotifications)
            notificationsAdapter.registerItemsEventsListener(this@NotificationReaderFragment)
            rvNotifications.addItemDecoration(
                DividerItemDecoration.Builder(requireContext())
                    .setDivider(Divider.Space(8), DividerItemDecoration.Mode.ALL)
                    .build()
            )

            tvAppsListSubtitle.setOnClickListener {
                viewModel.onToggleAppsListExpandedState()
            }

            errorBinding.tvEmptyError.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    net.maxsmr.designsystem.shared_res.R.color.textColorError
                )
            )
            errorBinding.btRetry.setOnClickListener {
                viewModel.onDownloadAppsListAction()
            }

            val clickListener = NumberedClickListener(15, TimeUnit.SECONDS.toMillis(4))

            viewModel.settings.observe {
                val apiKey = it.notificationsApiKey
                if (apiKey.isNotEmpty()) {
                    tvApiKeyValue.text = apiKey
                    tvApiKeyValue.isVisible = true
                    tvApiKeyEmpty.isVisible = false
                    containerApiKeyState.setOnClickListener {
                        if (clickListener.onClick()) {
                            viewModel.showInputApiKeyDialog()
                        }
                    }
                } else {
                    tvApiKeyValue.text = EMPTY_STRING
                    tvApiKeyValue.isVisible = false
                    tvApiKeyEmpty.isVisible = true
                    containerApiKeyState.setOnClickListener {
                        viewModel.showInputApiKeyDialog()
                    }
                }
            }
        }

        viewModel.doOnCanDrawOverlaysAsked(requireContext(), cacheRepo, settingsRepo) {
            if (it) {
                viewModel.showToast(TextMessage(R.string.notification_reader_toast_can_draw_overlays_settings))
            }
        }
    }

    override fun handleAlerts(delegate: BaseAlertDelegate<NotificationReaderViewModel, StandardAlertRepresentation>) {
        super.handleAlerts(delegate)

        delegate.bindAlertDialog(NotificationReaderViewModel.DIALOG_TAG_INPUT_API_KEY) {
            val positiveAnswer =
                it.answers.getOrNull(0) ?: throw IllegalStateException("Required positive answer is missing")

            val dialogBinding = DialogInputApiKeyBinding.inflate(LayoutInflater.from(requireContext()))
            dialogBinding.etApiKey.bindToTextNotNull(viewModel.inputApiKeyField)
            viewModel.inputApiKeyField.observeFromText(dialogBinding.etApiKey, viewLifecycleOwner) { value ->
                dialogBinding.ibClear.isVisible = value.isNotEmpty()
                value
            }

            val onAction: () -> Unit = {
                requireActivity().hideKeyboard()
                viewModel.onInputApiKeyDialogConfirm()
            }

            dialogBinding.etApiKey.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == IME_ACTION_DONE) {
                    positiveAnswer.select?.invoke()
                    onAction.invoke()
                    true
                } else {
                    false
                }
            }

            dialogBinding.ibClear.setOnClickListener {
                viewModel.inputApiKeyField.value = EMPTY_STRING
            }

            DialogViewAlertRepresentation.Builder(requireContext(), it)
                .setCustomView(dialogBinding.root) {
                    viewModel.inputApiKeyField.errorLive.observe { error ->
                        (this as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = error == null
                    }
                }
                .setCancelable(false)
                .setPositiveButton(positiveAnswer, onAction)
                .build()
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
        refreshStateItemByServiceRunning()
        lifecycleScope.launch {
            demoChecker.check(strategy)
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        toggleServiceStateMenuItem = menu.findItem(R.id.actionServiceStartStop)
        downloadAppsListMenuItem = menu.findItem(R.id.actionDownloadAppsList)
        retryFailedMenuItem = menu.findItem(R.id.actionRetryFailed)
        clearSuccessMenuItem = menu.findItem(R.id.actionClearSuccess)
        refreshStateItemByServiceRunning()
        refreshDownloadAppsListMenuItem()
        refreshRetryFailedMenuItem()
        refreshClearSuccessMenuItem()
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.actionServiceStartStop -> {
                viewModel.onToggleServiceTargetStateAction()
                true
            }

            R.id.actionDownloadAppsList -> {
                viewModel.onDownloadAppsListAction()
                true
            }

            R.id.actionRetryFailed -> {
                viewModel.onRetryFailedNotificationsAction()
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
        notificationsAdapter.unregisterItemsEventsListener(this)
    }

    override fun onItemRemoved(position: Int, item: NotificationsAdapterData) {
        viewModel.onRemoveSuccessNotification(item)
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int, item: NotificationsAdapterData) {
        throw UnsupportedOperationException("Move NotificationsAdapterData not supported")
    }

    private fun doStartOrStop(navigateToSettingsForStop: Boolean) {
        viewModel.doStartOrStop(this, navigateToSettingsForStop) { (isStarted, _, _) ->
            // рефреш меню сразу в зав-ти от результата старт/стоп,
            // а не текущего состояния сервиса (ещё не успело измениться)
            refreshStateItemByServiceRunning(isStarted)
        }
    }

    private fun refreshStateItemByServiceRunning(
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
    }

    private fun refreshDownloadAppsListMenuItem() {
        downloadAppsListMenuItem?.isVisible = viewModel.isRunning.value == true
                && !viewModel.settings.value?.appsListUrl.isNullOrEmpty()
    }

    private fun refreshRetryFailedMenuItem() {
        retryFailedMenuItem?.isVisible = viewModel.notificationsItems.value
            ?.any { it.status is NotificationReaderEntity.Failed } == true
                && viewModel.isRunning.value == true
    }

    private fun refreshClearSuccessMenuItem() {
        clearSuccessMenuItem?.isVisible = viewModel.notificationsItems.value
            ?.any { it.status is NotificationReaderEntity.Success } == true
    }
}