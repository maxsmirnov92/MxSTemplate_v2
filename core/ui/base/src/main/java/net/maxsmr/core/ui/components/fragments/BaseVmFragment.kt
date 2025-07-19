package net.maxsmr.core.ui.components.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import net.maxsmr.commonutils.ResettableLazy
import net.maxsmr.commonutils.flow.observeEvents
import net.maxsmr.commonutils.flow.observeLatest
import net.maxsmr.commonutils.flow.repeatOnLifecycle
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.resettableLazy
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.result.ActivityResultRegisterer
import net.maxsmr.core.android.permissions.DialogDeniedPermissionsHandler
import net.maxsmr.core.android.permissions.PermissionsRequester
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.ConnectionHandler
import net.maxsmr.core.ui.alert.delegate.BaseAlertDelegate
import net.maxsmr.core.ui.alert.delegate.BaseViewAlertDelegate
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.handleAlerts
import net.maxsmr.core.ui.components.handleEvents
import net.maxsmr.core.ui.message.toast.ToastActorImpl
import net.maxsmr.core.ui.navigation.NavigationActorImpl
import net.maxsmr.permissionchecker.BaseDeniedPermissionsHandler
import net.maxsmr.permissionchecker.PermissionsCallbacks
import net.maxsmr.permissionchecker.PermissionsHelper

/**
 * Фрагмент с конкретным типом VM и базовыми методами для подписки
 */
abstract class BaseVmFragment<VM : BaseViewModel> : Fragment(),
        PermissionsRequester, ActivityResultRegisterer {

    override val requireContext: Context by lazy { requireContext() }

    override val requireActivity: ComponentActivity by lazy { requireActivity() }

    /**
     * Разметка для использования в чистом view либо с ComposeView
     */
    @get:LayoutRes
    protected abstract val layoutId: Int

    protected abstract val viewModel: VM

    /**
     * Отвечает за реакцию фрагмента на появление/отсутствие сети
     *
     * @see BaseViewModel.connectionManager
     */
    protected open val connectionHandler: ConnectionHandler? = null

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    protected val navigationActor by lazy { NavigationActorImpl(this) }

    protected val toastActor by lazy { ToastActorImpl(requireContext()) }

    private val alertDelegate: ResettableLazy<BaseAlertDelegate<VM>> = resettableLazy {
        createAlertDelegate()
    }

    private val delegates: List<IComponentDelegate<*>> by lazy {
        val activity = requireActivity() as BaseActivity
        if (activity.canUseComponentDelegates) {
            createFragmentDelegates()
        } else {
            listOf()
        }
    }

    /**
     * [BaseDeniedPermissionsHandler] c хостовой активити
     * Вызывать только после аттача!
     */
    private val permanentlyDeniedPermissionsHandler: BaseDeniedPermissionsHandler by lazy {
        DialogDeniedPermissionsHandler(viewModel, requireActivity())
    }

    protected abstract fun createAlertDelegate(): BaseAlertDelegate<VM>

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(layoutId, container, false)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNetworkConnectionHandler()
        // см. коммент к navigateWithGraphFragments;
        // viewLifecycleOwner другой, показ алертов не срабатывает
        alertDelegate.reset()
        val delegate = alertDelegate.value
        if (delegate is BaseViewAlertDelegate) {
            handleViewAlerts(delegate)
        }
        handleVmEvents()

        delegates.forEach {
            it.onCreated()
        }

        onViewCreated(view, savedInstanceState, viewModel)
    }

    override fun onResume() {
        super.onResume()
        delegates.forEach {
            it.onResumed()
        }
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        delegates.forEach {
            it.onDestroyed()
        }
    }

    override fun doOnPermissionsResult(
        code: Int,
        permissions: Collection<String>,
        shouldShowPermanentlyDeniedDialog: Boolean,
        onDenied: ((Set<String>) -> Unit)?,
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

    /**
     * Основной коллбек, в котором можно делать подписки на VM и инициализацию View в производных классах
     */
    protected abstract fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: VM,
    )

    @CallSuper
    protected open fun handleViewAlerts(delegate: BaseViewAlertDelegate<VM>) {
        delegate.handleAlerts()
    }

    @CallSuper
    protected open fun handleVmEvents() {
        viewModel.handleEvents(this@BaseVmFragment, navigationActor, toastActor)
    }

    protected open fun createFragmentDelegates(): List<IComponentDelegate<*>> = listOf()

    @Deprecated("", replaceWith = ReplaceWith(expression = "StateFlow"))
    @JvmOverloads
    protected inline fun <T> LiveData<T>.observe(
        owner: LifecycleOwner = viewLifecycleOwner,
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(owner) { onNext(it) }
    }

    @Deprecated("", replaceWith = ReplaceWith(expression = "StateFlow"))
    @JvmOverloads
    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(
        owner: LifecycleOwner = viewLifecycleOwner,
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(owner) {
            it.get()?.let(onNext)
        }
    }

    protected inline fun <T> Flow<T>.observeSafe(
        owner: LifecycleOwner = viewLifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: T) -> Unit,
    ) = observeLatest(owner, lifecycleState, action)

    protected inline fun <T> StateFlow<VmEvent<T>?>.observeEventsSafe(
        owner: LifecycleOwner = viewLifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: T) -> Unit,
    ) = observeEvents(owner, lifecycleState, action)

    protected inline fun <T : Any> Flow<PagingData<T>>.observePaging(
        owner: LifecycleOwner = viewLifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: PagingData<T>) -> Unit,
    ) = repeatOnLifecycle(owner, lifecycleState) { this.collectLatest { action(it) } }

    private fun observeNetworkConnectionHandler() {
        viewModel.connectionManager?.let { manager ->
            connectionHandler?.onNetworkStateChanged?.let {
                manager.statusFlow.observeSafe { state ->
                    it.invoke(state)
                }
            }
        }
    }
}