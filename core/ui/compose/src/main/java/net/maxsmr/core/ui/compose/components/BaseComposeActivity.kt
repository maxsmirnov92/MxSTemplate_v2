package net.maxsmr.core.ui.compose.components

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.connection.ConnectionHandler
import net.maxsmr.core.android.base.connection.ConnectionManager
import net.maxsmr.core.android.base.result.ICanRegisterForActivityResult
import net.maxsmr.core.android.coroutines.collectEventsWithOwner
import net.maxsmr.core.android.coroutines.collectWithOwner
import net.maxsmr.core.android.permissions.DialogDeniedPermissionsHandler
import net.maxsmr.core.android.permissions.ICanAskPermissions
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.BaseAlertDelegate
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.handleAlerts
import net.maxsmr.core.ui.components.handleEvents
import net.maxsmr.core.ui.compose.alert.ComposeActivityAlertDelegate
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
    final override fun registerViewModelByRoute(
        route: String,
        viewModel: BaseViewModel,
        dependencies: ComposableDependencies,
    ): Boolean {
        // Добавление происходит динамически,
        // т.к. не по всем экранам VM известны на момент инициализации Activity
        val component = screenComponentsMap[route]
        if (component == null || component.viewModel != viewModel) {
            LaunchedEffect(Unit) {
                component?.unregister()
                screenComponentsMap[route] = ScreenComponents(
                    viewModel,
                    getAlertDelegateForViewModel(viewModel, dependencies)
                ).also {
                    it.register(dependencies.navHostController)
                }
            }
            return true
        }
        return false
    }

    protected open fun getAlertDelegateForActivityViewModel(dependencies: ComposableDependencies): ComposeActivityAlertDelegate<VM> {
        return ComposeActivityAlertDelegate(
            this@BaseComposeActivity, lifecycleScope, dependencies.snackbarHostState, viewModel
        )
    }

    protected open fun <VM : BaseViewModel> getAlertDelegateForViewModel(
        viewModel: VM,
        dependencies: ComposableDependencies,
    ): ComposeActivityAlertDelegate<VM> {
        return ComposeActivityAlertDelegate(
            this@BaseComposeActivity, lifecycleScope, dependencies.snackbarHostState, viewModel
        )
    }

    @CallSuper
    protected open fun handleAlerts(viewModel: BaseViewModel, delegate: BaseAlertDelegate<*>) {
        delegate.handleAlerts()
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

    protected inline fun <T> LiveData<T>.observe(
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(this@BaseComposeActivity) { onNext(it) }
    }

    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(
        crossinline onNext: (T) -> Unit,
    ) {
        this.observe(this@BaseComposeActivity) {
            it.get()?.let(onNext)
        }
    }

    protected inline fun <T : Any> Flow<T>.collectSafely(
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: T) -> Unit,
    ) {
        collectWithOwner(this@BaseComposeActivity, lifecycleState, action)
    }

    protected inline fun <T : Any> StateFlow<VmEvent<T>?>.collectEvent(
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline action: suspend (value: T) -> Unit,
    ) {
        collectEventsWithOwner(this@BaseComposeActivity, lifecycleState, action)
    }

    private fun getActivityScreenComponents(dependencies: ComposableDependencies): ScreenComponents {
        return ScreenComponents(
            viewModel,
            getAlertDelegateForActivityViewModel(dependencies)
        )
    }

    private fun ScreenComponents.observeNetworkConnectionHandler() {
        connectionHandler?.onNetworkStateChanged?.let { onStateChanged ->
            viewModel.connectionManager.asLiveData.observe {
                onStateChanged(it)
            }
        }
        connectionHandler?.alertsMapper?.let { mapper ->
            viewModel.connectionManager.queue?.let {
                // queue разные: snackbarQueue вместо dialogQueue
                alertDelegate.bindAlert(it, ConnectionManager.SNACKBAR_TAG_CONNECTIVITY) { alert ->
                    mapper(alert)
                }
            }
        }
    }

    @Composable
    private fun RegisterActivityComponents(dependencies: ComposableDependencies) {
        LaunchedEffect(Unit) {
            with(activityScreenComponents) {
                clear()
                add(getActivityScreenComponents(dependencies))
                addAll(getExtraActivityScreenComponents(dependencies))
                forEach {
                    it.register(dependencies.navHostController)
                }
            }
        }
    }

    private fun ScreenComponents.register(navController: NavController) {
        // FIXME по остальным нет возможности отписаться
        observeNetworkConnectionHandler()
        handleAlerts(viewModel, alertDelegate)
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
        val alertDelegate: ComposeActivityAlertDelegate<*>,
        val connectionHandler: ConnectionHandler? = null,
    ) {

        val disposables = mutableListOf<Job>()
    }
}