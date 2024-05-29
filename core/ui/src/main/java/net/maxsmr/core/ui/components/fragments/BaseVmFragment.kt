package net.maxsmr.core.ui.components.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.getAppSettingsIntent
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PROGRESS
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation
import net.maxsmr.core.android.base.connection.ConnectionHandler
import net.maxsmr.core.android.base.connection.ConnectionManager
import net.maxsmr.core.android.content.pick.ContentPicker
import net.maxsmr.core.android.permissions.formatDeniedPermissionsMessage
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.content.pick.chooser.AppIntentChooserData
import net.maxsmr.core.ui.content.pick.chooser.AppIntentChooserDialog
import net.maxsmr.permissionchecker.BaseDeniedPermissionsHandler
import net.maxsmr.permissionchecker.PermissionsCallbacks
import net.maxsmr.permissionchecker.PermissionsHelper


/**
 * Фрагмент с конкретным типом VM и базовыми методами для подписки
 */
abstract class BaseVmFragment<VM : BaseHandleableViewModel> : Fragment() {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    @get:LayoutRes
    protected abstract val layoutId: Int

    abstract val viewModel: VM

    abstract val permissionsHelper: PermissionsHelper

    /**
     * Отвечает за реакцию фрагмента на появление/отсутствие сети
     *
     * @see BaseViewModel.connectionManager
     */
    protected open val connectionHandler: ConnectionHandler? = null

    /**
     * [BaseDeniedPermissionsHandler] c хостовой активити
     * Вызывать только после аттача!
     */
    val permanentlyDeniedPermissionsHandler: BaseDeniedPermissionsHandler by lazy {
        DialogDeniedPermissionsHandler()
    }

    private var alertFragmentDelegate: AlertFragmentDelegate<VM>? = null

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(layoutId, container, false)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val delegate = AlertFragmentDelegate(this, viewModel)
        alertFragmentDelegate = delegate

        observeNetworkConnectionHandler()
        handleAlerts(delegate)
        handleEvents()

        onViewCreated(view, savedInstanceState, viewModel)
    }

    @CallSuper
    protected open fun handleAlerts(delegate: AlertFragmentDelegate<VM>) {
        viewModel.handleAlerts(requireContext(), delegate)
    }

    @CallSuper
    protected open fun handleEvents() {
        viewModel.handleEvents(this@BaseVmFragment)
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        alertFragmentDelegate = null
    }

    /**
     * Основной коллбек, в котором можно делать подписки на VM и инициализацию View в производных классах
     */
    protected abstract fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: VM,
    )

    @JvmOverloads
    protected inline fun <T> LiveData<T>.observe(
        owner: LifecycleOwner = viewLifecycleOwner,
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(owner, Observer { onNext(it) })
    }

    @JvmOverloads
    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(
        owner: LifecycleOwner = viewLifecycleOwner,
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(owner, Observer {
            it.get()?.let(onNext)
        })
    }

    /**
     * Вызов обёрнутой функции привязки для показа диалогов только при проинициализированном [AlertFragmentDelegate]
     */
    fun bindAlertDialog(tag: String, representationFactory: (Alert) -> AlertRepresentation?) {
        alertFragmentDelegate?.bindAlertDialog(tag, representationFactory)
    }

    fun bindAlertDialog(alertQueue: AlertQueue, tag: String, representationFactory: (Alert) -> AlertRepresentation?) {
        alertFragmentDelegate?.bindAlertDialog(alertQueue, tag, representationFactory)
    }

    /**
     * Стандартная реализация progress, нужно вызвать по месту на конкретном экране;
     * только при проинициализированном [AlertFragmentDelegate]
     */
    @JvmOverloads
    fun bindDefaultProgress(
        tag: String = DIALOG_TAG_PROGRESS,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null,
    ) {
        alertFragmentDelegate?.bindDefaultProgress(tag, cancelable, onCancel)
    }

    @JvmOverloads
    fun doOnPermissionsResult(
        code: Int,
        permissions: Collection<String>,
        shouldShowPermanentlyDeniedDialog: Boolean = true,
        onDenied: ((Set<String>) -> Unit)? = null,
        onAllGranted: () -> Unit,
    ): PermissionsHelper.ResultListener? {
        val rationale = getString(R.string.get_permission)
        val handler = PermissionsCallbacks(
            onPermanentlyDeniedPermissions = if (shouldShowPermanentlyDeniedDialog) { set ->
                permanentlyDeniedPermissionsHandler.showMessage(code, rationale, set, onDenied)
            } else {
                null
            },
            onDenied = onDenied,
            onAllGranted = onAllGranted
        )
        return (requireActivity() as BaseActivity).doOnPermissionsResult(
            permissionsHelper,
            rationale,
            code,
            permissions.toSet(),
            handler,
        )
    }

    protected fun <T : Any> Flow<T>.collect(
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        action: suspend (value: T) -> Unit,
    ) {
        collectFlowSafely(lifecycleState) { this.collectLatest { action(it) } }
    }

    protected fun <T : Any> Flow<PagingData<T>>.collectPaging(
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        action: suspend (value: PagingData<T>) -> Unit,
    ) {
        collectFlowSafely(lifecycleState) { this.collectLatest { action(it) } }
    }

    private fun collectFlowSafely(
        lifecycleState: Lifecycle.State,
        collect: suspend () -> Unit,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(lifecycleState) {
                collect()
            }
        }
    }

    private fun observeNetworkConnectionHandler() {
        connectionHandler?.onNetworkStateChanged?.let { onStateChanged ->
            viewModel.connectionManager?.asLiveData?.observe {
                onStateChanged(it)
            }
        }
        connectionHandler?.alertsMapper?.let { mapper ->
            viewModel.connectionManager?.queue?.let {
                // queue разные: snackbarQueue вместо dialogQueue
                bindAlertDialog(it, ConnectionManager.SNACKBAR_TAG_CONNECTIVITY) { alert ->
                    mapper(alert)
                }
            }
        }
    }

    inner class FragmentContentPickerBuilder() : ContentPicker.Builder(this,
        object : ContentPicker.PermissionHandler {

            override fun filterDeniedNotAskAgain(context: Context, permissions: Collection<String>): Set<String> {
                return permissionsHelper.filterDeniedNotAskAgain(context, permissions)
            }

            override fun handle(
                requestCode: Int,
                permissions: Set<String>,
                onDenied: (Set<String>) -> Unit,
                onGranted: () -> Unit,
            ) {
                doOnPermissionsResult(requestCode, permissions, false, onDenied = onDenied, onAllGranted = onGranted)
            }
        }, { code, title, intents ->
            AppIntentChooserDialog.show(this, AppIntentChooserData(code, title, intents))
        })

    private inner class DialogDeniedPermissionsHandler : BaseDeniedPermissionsHandler() {

        override fun doShowMessage(
            requestCode: Int,
            message: String,
            deniedPerms: Set<String>,
            negativeAction: ((Set<String>) -> Unit)?,
        ) {
            viewModel.showYesNoPermissionDialog(
                TextMessage(message),
                { requireActivity().startActivityForResult(getAppSettingsIntent(requireActivity()), requestCode) },
                { negativeAction?.invoke(deniedPerms) })
        }

        override fun formatDeniedPermissionsMessage(perms: Collection<String>): String =
            requireActivity().formatDeniedPermissionsMessage(perms)
    }
}