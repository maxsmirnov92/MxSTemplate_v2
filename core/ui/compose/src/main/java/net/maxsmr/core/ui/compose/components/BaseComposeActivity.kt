package net.maxsmr.core.ui.compose.components

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.commonutils.flow.observe
import net.maxsmr.commonutils.flow.observeEvents
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.result.ICanRegisterForActivityResult
import net.maxsmr.core.android.permissions.DialogDeniedPermissionsHandler
import net.maxsmr.core.android.permissions.ICanAskPermissions
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.ConnectionHandler
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.handleEvents
import net.maxsmr.core.ui.compose.alert.delegate.ComposableAlertDelegate
import net.maxsmr.core.ui.message.toast.ToastActorImpl
import net.maxsmr.core.ui.navigation.NavigationActorImpl
import net.maxsmr.designsystem.compose.component.AppBackground
import net.maxsmr.permissionchecker.BaseDeniedPermissionsHandler
import net.maxsmr.permissionchecker.PermissionsCallbacks
import net.maxsmr.permissionchecker.PermissionsHelper

/**
 * [BaseActivity] для использования экранов в виде Composable-функций
 */
abstract class BaseComposeActivity<VM : BaseViewModel> : BaseActivity(),
        ICanAskPermissions, ICanRegisterForActivityResult, IComposableViewModelsContainer {

    override val attachedContext: Context by lazy { this }

    override val attachedActivity: ComponentActivity by lazy { this }

    protected abstract val viewModel: VM

    private val toastActor by lazy { ToastActorImpl(this) }

    private val delegates: List<IComponentDelegate<*>> by lazy {
        if (canUseComponentDelegates) {
            createActivityDelegates()
        } else {
            listOf()
        }
    }

    private val permanentlyDeniedPermissionsHandler: BaseDeniedPermissionsHandler by lazy {
        DialogDeniedPermissionsHandler(viewModel, this@BaseComposeActivity)
    }

    private val activityScreenComponents = mutableListOf<ScreenComponents>()

    /**
     * key - route экрана,
     * value - [ScreenComponents] для него
     */
    private val screenComponentsMap = mutableMapOf<String, ScreenComponents>()

    @Composable
    abstract fun SetScreenContent(dependencies: ComposableDependencies)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposableDependencies(
                rememberNavController(),
                SnackbarHostState()
            ).apply {
                RegisterActivityComponents(this)
                AppBackground {
                    SetScreenContent(this)
                }
            }
        }

        delegates.forEach {
            it.onCreated()
        }
    }

    override fun onResume() {
        super.onResume()
        delegates.forEach {
            it.onResumed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        delegates.forEach {
            it.onDestroyed()
        }
        activityScreenComponents.forEach {
            it.unregister()
        }
        screenComponentsMap.values.forEach {
            it.unregister()
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
        return doOnPermissionsResult(
            permissionsHelper,
            rationale,
            code,
            permissions.toSet(),
            handler
        )
    }

    override fun <VM : BaseViewModel> getFactoryForViewModel(
        viewModelClass: Class<VM>,
        args: IComposableViewModelsContainer.IFactoryArgs<VM>?,
    ): ViewModelProvider.Factory? {
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <VM : BaseViewModel> getViewModelForRoute(route: String): VM? {
        return screenComponentsMap[route]?.viewModel as? VM
    }

    @Composable
    final override fun registerViewModelWithRoute(
        route: String,
        key: String?,
        viewModel: BaseViewModel,
        dependencies: ComposableDependencies,
    ): Boolean {
        // Добавление происходит динамически,
        // т.к. не по всем экранам VM известны на момент инициализации Activity
        val component = screenComponentsMap[route]
        if (component == null || component.viewModel != viewModel) {
            component?.unregister()
            screenComponentsMap[route] = ScreenComponents(
                viewModel,
                key,
                getAlertDelegateForViewModel(viewModel, lifecycleScope, dependencies.snackbarHostState),
                getConnectionHandlerForViewModel(viewModel, lifecycleScope, dependencies.snackbarHostState)
            ).also {
                it.Register(dependencies.navHostController)
            }
            return true
        }
        return false
    }

    override fun unregisterViewModelWithRoute(
        route: String,
        viewModel: BaseViewModel,
    ) {
        screenComponentsMap[route]?.let {
            it.key?.let { key ->
                savedStateRegistry.unregisterSavedStateProvider(key)
            }
            it.unregister()
            screenComponentsMap.remove(route)
        }
    }

    protected open fun getAlertDelegateForActivityViewModel(
        scope: CoroutineScope,
        hostState: SnackbarHostState,
    ): ComposableAlertDelegate<VM> {
        return ComposableAlertDelegate(
            this@BaseComposeActivity, viewModel, scope, hostState
        )
    }

    protected open fun getConnectionHandlerForActivityViewModel(
        scope: CoroutineScope,
        hostState: SnackbarHostState,
    ): ConnectionHandler? {
        return null
    }

    protected open fun <VM : BaseViewModel> getAlertDelegateForViewModel(
        viewModel: VM,
        scope: CoroutineScope,
        hostState: SnackbarHostState,
    ): ComposableAlertDelegate<VM> {
        return ComposableAlertDelegate(
            this@BaseComposeActivity, viewModel, scope, hostState
        )
    }

    protected open fun <VM : BaseViewModel> getConnectionHandlerForViewModel(
        viewModel: VM,
        scope: CoroutineScope,
        hostState: SnackbarHostState,
    ): ConnectionHandler? {
        return getConnectionHandlerForActivityViewModel(scope, hostState)
    }

    @Composable
    @CallSuper
    protected open fun HandleComposableAlerts(
        viewModel: BaseViewModel,
        delegate: ComposableAlertDelegate<*>,
    ) {
        delegate.HandleAlertDialogs()
    }

    protected open fun handleStandardAlerts(viewModel: BaseViewModel, delegate: ComposableAlertDelegate<*>) {
        delegate.handleSnackbarAlerts()
        delegate.handleToastAlerts()
    }

    @CallSuper
    protected open fun handleVmEvents(viewModel: BaseViewModel, navController: NavController): List<Job> {
        return viewModel.handleEvents(this, NavigationActorImpl(this, navController), toastActor)
    }

    protected open fun createActivityDelegates(): List<IComponentDelegate<*>> = listOf()

    /**
     * Дополнительные [ScreenComponents] к основному для [VM],
     * при наличии других расшаренных [BaseViewModel] на этой активности
     */
    protected open fun getExtraActivityScreenComponents(dependencies: ComposableDependencies): List<ScreenComponents> =
        listOf()

    @Deprecated("", replaceWith = ReplaceWith(expression = "StateFlow"))
    protected inline fun <T> LiveData<T>.observe(
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(this@BaseComposeActivity) { onNext(it) }
    }

    @Deprecated("", replaceWith = ReplaceWith(expression = "StateFlow"))
    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(this@BaseComposeActivity) {
            it.get()?.let(onNext)
        }
    }

    protected inline fun <T : Any> Flow<T>.observeSafe(
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: T) -> Unit,
    ) {
        observe(this@BaseComposeActivity, lifecycleState, action)
    }

    protected inline fun <T : Any> StateFlow<VmEvent<T>?>.observeEventsSafe(
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: T) -> Unit,
    ) {
        observeEvents(this@BaseComposeActivity, lifecycleState, action)
    }

    private fun getActivityScreenComponents(dependencies: ComposableDependencies): ScreenComponents {
        return ScreenComponents(
            viewModel,
            getKeyForViewModel(viewModel::class.java),
            getAlertDelegateForActivityViewModel(lifecycleScope, dependencies.snackbarHostState),
            getConnectionHandlerForActivityViewModel(lifecycleScope, dependencies.snackbarHostState)
        )
    }

    private fun ScreenComponents.observeNetworkConnectionHandlerState() {
        viewModel.connectionManager?.let { manager ->
            connectionHandler?.onNetworkStateChanged?.let {
                manager.statusFlow.observeSafe { state ->
                    it.invoke(state)
                }
            }
        }
    }

    @Composable
    private fun RegisterActivityComponents(dependencies: ComposableDependencies) {
        with(activityScreenComponents) {
            clear()
            add(getActivityScreenComponents(dependencies))
            addAll(getExtraActivityScreenComponents(dependencies))
            forEach {
                it.Register(dependencies.navHostController)
            }
        }
    }

    @Composable
    private fun ScreenComponents.Register(navController: NavController) {
        // FIXME по остальным нет возможности отписаться
        observeNetworkConnectionHandlerState()
        HandleComposableAlerts(viewModel, alertDelegate)
        handleStandardAlerts(viewModel, alertDelegate)
        disposables.addAll(handleVmEvents(viewModel, navController))
    }

    private fun ScreenComponents.unregister() {
        disposables.let {
            it.forEach { j ->
                j.cancel()
            }
        }
    }

    protected class ScreenComponents(
        val viewModel: BaseViewModel,
        val key: String?,
        val alertDelegate: ComposableAlertDelegate<*>,
        val connectionHandler: ConnectionHandler? = null,
    ) {

        val disposables = mutableListOf<Job>()
    }
}