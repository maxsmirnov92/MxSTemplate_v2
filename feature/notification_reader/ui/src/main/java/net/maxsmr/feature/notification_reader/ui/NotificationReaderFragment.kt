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
import kotlinx.coroutines.launch
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.drag.DragAndDropTouchHelperCallback
import net.maxsmr.android.recyclerview.adapters.base.drag.OnStartDragHelperListener
import net.maxsmr.android.recyclerview.views.decoration.Divider
import net.maxsmr.android.recyclerview.views.decoration.DividerItemDecoration
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.graphic.createBitmapDrawable
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.gui.hideKeyboard
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.commonutils.live.zip
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.DialogRepresentation
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.databinding.LayoutErrorContainerBinding
import net.maxsmr.core.ui.views.ViewClickDelegate
import net.maxsmr.feature.demo.DemoChecker
import net.maxsmr.feature.demo.strategies.AlertDemoExpiredStrategy
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStartResult
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStopResult
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapter
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData
import net.maxsmr.feature.notification_reader.ui.adapter.PackageNamesAdapter
import net.maxsmr.feature.notification_reader.ui.databinding.DialogInputApiKeyBinding
import net.maxsmr.feature.notification_reader.ui.databinding.FragmentNotificationReaderBinding
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.preferences.ui.doOnCanDrawOverlaysAsked
import net.maxsmr.permissionchecker.PermissionsHelper
import java.util.concurrent.TimeUnit
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

    private val packageNamesAdapter = PackageNamesAdapter()
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
            this,
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
    private var downloadPackageListMenuItem: MenuItem? = null
    private var retryFailedMenuItem: MenuItem? = null
    private var clearSuccessMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: NotificationReaderViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        viewModel.resetServiceTargetStateViewFlag()
        viewModel.serviceTargetState.observe {
            if (it != null) {
                doStartOrStop(it.changedFromView)
            }
        }

        with(binding) {
            val errorBinding = LayoutErrorContainerBinding.bind(containerPackageListError.root)

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

            zip(viewModel.isRunning, viewModel.packageListLoadState) { isRunning, loadState ->
                isRunning to loadState
            }.observe {
                val (isRunning, loadState) = it
                if (isRunning == true) {
                    if (loadState?.isLoading == true) {
                        containerPackageListLoading.isVisible = true
                        containerPackageNames.isVisible = false
                        containerPackageListError.root.isVisible = false
                        containerPackageListState.isVisible = true
                    } else {
                        containerPackageListLoading.isVisible = false
                        if (loadState?.isSuccessWithData { state -> !state?.names.isNullOrEmpty() } == true) {
                            val data = loadState.data ?: return@observe
                            tvPackageNamesSubtitle.text = getString(
                                if (data.isWhiteList) {
                                    R.string.notification_reader_package_list_white_subtitle
                                } else {
                                    R.string.notification_reader_package_list_black_subtitle
                                }
                            )
                            packageNamesAdapter.items = data.names
                            containerPackageNames.isVisible = true
                            containerPackageListError.root.isVisible = false
                            containerPackageListState.isVisible = true
                        } else {
                            containerPackageNames.isVisible = false
                            if (loadState != null && loadState.isError()) {
                                errorBinding.tvEmptyError.text =
                                    loadState.error?.message?.takeIf { message -> message.isNotEmpty() }
                                        ?.let { message ->
                                            getString(R.string.notification_reader_package_list_error_format, message)
                                        } ?: getString(R.string.notification_reader_package_list_error)
                                containerPackageListError.root.isVisible = true
                            } else {
                                containerPackageListError.root.isVisible = false
                            }
                            containerPackageListState.isVisible = loadState != null
                        }
                    }
                } else {
                    containerPackageListState.isVisible = false
                }
            }
            zip(viewModel.isRunning, viewModel.settings) { isRunning, settings ->
                isRunning to settings
            }.observe {
                refreshDownloadPackageListMenuItem()
            }

            viewModel.packageListExpandedState.observe {
                ContextCompat.getDrawable(
                    requireContext(),
                    if (it) {
                        R.drawable.ic_arrow_up
                    } else {
                        R.drawable.ic_arrow_down
                    }
                )?.let { d ->
                    tvPackageNamesSubtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null,
                        null,
                        d.createBitmapDrawable(requireContext(), 30, 30),
                        null
                    )
                }
                rvPackageNames.isVisible = it
            }

            rvPackageNames.adapter = packageNamesAdapter
            rvNotifications.adapter = notificationsAdapter
            touchHelper.attachToRecyclerView(rvNotifications)
            notificationsAdapter.registerItemsEventsListener(this@NotificationReaderFragment)
            rvNotifications.addItemDecoration(
                DividerItemDecoration.Builder(requireContext())
                    .setDivider(Divider.Space(8), DividerItemDecoration.Mode.ALL)
                    .build()
            )

            tvPackageNamesSubtitle.setOnClickListener {
                viewModel.onTogglePackageListExpandedState()
            }

            errorBinding.tvEmptyError.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    net.maxsmr.core.ui.R.color.textColorError
                )
            )
            errorBinding.btRetry.setOnClickListener {
                viewModel.onDownloadPackageListAction()
            }

            viewModel.settings.observe {
                val apiKey = it.notificationsApiKey
                if (apiKey.isNotEmpty()) {
                    tvApiKeyValue.text = apiKey
                    tvApiKeyValue.isVisible = true
                    tvApiKeyEmpty.isVisible = false
                    ViewClickDelegate(
                        containerApiKeyState,
                        15,
                        TimeUnit.SECONDS.toMillis(4)
                    ).setOnClickListener {
                        viewModel.showInputApiKeyDialog()
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

        viewModel.doOnCanDrawOverlaysAsked(this, cacheRepo, settingsRepo) {
            if (it) {
                viewModel.showToast(TextMessage(R.string.notification_reader_toast_can_draw_overlays_settings))
            }
        }
    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<NotificationReaderViewModel>) {
        super.handleAlerts(delegate)

        bindAlertDialog(NotificationReaderViewModel.DIALOG_TAG_INPUT_API_KEY) {
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

            DialogRepresentation.Builder(requireContext(), it)
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
        downloadPackageListMenuItem = menu.findItem(R.id.actionDownloadPackageList)
        retryFailedMenuItem = menu.findItem(R.id.actionRetryFailed)
        clearSuccessMenuItem = menu.findItem(R.id.actionClearSuccess)
        refreshStateItemByServiceRunning()
        refreshDownloadPackageListMenuItem()
        refreshRetryFailedMenuItem()
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

    private fun refreshDownloadPackageListMenuItem() {
        downloadPackageListMenuItem?.isVisible = viewModel.isRunning.value == true
                && !viewModel.settings.value?.packageListUrl.isNullOrEmpty()
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