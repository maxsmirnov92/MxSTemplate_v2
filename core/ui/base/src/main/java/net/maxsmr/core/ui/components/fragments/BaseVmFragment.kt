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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.ResettableLazy
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.live.observeOnce
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.resettableLazy
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.connection.ConnectionHandler
import net.maxsmr.core.android.base.connection.ConnectionManager
import net.maxsmr.core.android.base.result.ICanRegisterForActivityResult
import net.maxsmr.core.android.coroutines.collectEventsWithOwner
import net.maxsmr.core.android.coroutines.collectWithOwner
import net.maxsmr.core.android.coroutines.repeatOnLifecycle
import net.maxsmr.core.android.permissions.DialogDeniedPermissionsHandler
import net.maxsmr.core.android.permissions.ICanAskPermissions
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.BaseAlertDelegate
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
        ICanAskPermissions, ICanRegisterForActivityResult {

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    override val attachedContext: Context by lazy { requireContext() }

    override val attachedActivity: ComponentActivity by lazy { requireActivity() }

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
        handleAlerts(alertDelegate.value)
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

    /**
     * Основной коллбек, в котором можно делать подписки на VM и инициализацию View в производных классах
     */
    protected abstract fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: VM,
    )

    @CallSuper
    protected open fun handleAlerts(delegate: BaseAlertDelegate<VM>) {
        delegate.handleAlerts()
    }

    @CallSuper
    protected open fun handleVmEvents() {
        viewModel.handleEvents(this@BaseVmFragment, navigationActor, toastActor)
    }

    protected open fun createFragmentDelegates(): List<IComponentDelegate<*>> = listOf()

    @JvmOverloads
    protected inline fun <T> LiveData<T>.observe(
        owner: LifecycleOwner = viewLifecycleOwner,
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(owner) { onNext(it) }
    }

    @JvmOverloads
    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(
        owner: LifecycleOwner = viewLifecycleOwner,
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(owner) {
            it.get()?.let(onNext)
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

    fun doOnAnyAskOption(
        flow: Flow<Boolean>?,
        setAskedFunc: suspend () -> Unit,
        targetAction: (Boolean) -> Unit,
    ) {
        flow?.asLiveData()?.observeOnce(this) {
            if (!it) {
                this.lifecycleScope.launch {
                    setAskedFunc()
                }
            }
            targetAction(it)
        } ?: targetAction.invoke(true)
    }

    protected inline fun <T : Any> Flow<T>.collectSafely(
        owner: LifecycleOwner = viewLifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: T) -> Unit,
    ) {
        collectWithOwner(owner, lifecycleState, action)
    }

    protected inline fun <T : Any> StateFlow<VmEvent<T>?>.collectEvent(
        owner: LifecycleOwner = viewLifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: T) -> Unit,
    ) {
        collectEventsWithOwner(owner, lifecycleState, action)
    }

    protected inline fun <T : Any> Flow<PagingData<T>>.collectPaging(
        owner: LifecycleOwner = viewLifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: PagingData<T>) -> Unit,
    ) {
        repeatOnLifecycle(owner, lifecycleState) { this.collectLatest { action(it) } }
    }

    private fun observeNetworkConnectionHandler() {
        connectionHandler?.onNetworkStateChanged?.let { onStateChanged ->
            viewModel.connectionManager.asLiveData.observe {
                onStateChanged(it)
            }
        }
        connectionHandler?.alertsMapper?.let { mapper ->
            viewModel.connectionManager.queue?.let {
                // queue разные: snackbarQueue вместо dialogQueue
                alertDelegate.value.bindAlert(it, ConnectionManager.SNACKBAR_TAG_CONNECTIVITY) { alert ->
                    mapper(alert)
                }
            }
        }
    }
}