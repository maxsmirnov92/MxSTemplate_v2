package net.maxsmr.core.ui.compose.components

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.connection.ConnectionHandler
import net.maxsmr.core.android.base.connection.ConnectionManager
import net.maxsmr.core.android.base.result.ICanRegisterForActivityResult
import net.maxsmr.core.android.coroutines.collectEventsWithOwner
import net.maxsmr.core.android.coroutines.collectWithOwner
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.BaseAlertDelegate
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.handleAlerts
import net.maxsmr.core.ui.components.handleEvents
import net.maxsmr.core.ui.message.toast.ToastActorImpl
import net.maxsmr.core.ui.navigation.NavigationActorImpl
import net.maxsmr.core.ui.permission.DialogDeniedPermissionsHandler
import net.maxsmr.designsystem.compose.component.AppBackground
import net.maxsmr.permissionchecker.BaseDeniedPermissionsHandler
import net.maxsmr.permissionchecker.PermissionsCallbacks
import net.maxsmr.permissionchecker.PermissionsHelper

/**
 * [BaseActivity] для использования экранов в виде Composable-функций
 */
abstract class BaseComposeActivity<VM : BaseViewModel> : BaseActivity(), ICanRegisterForActivityResult {

    override val attachedActivity: ComponentActivity by lazy { this }

    abstract val permissionsHelper: PermissionsHelper

    protected abstract val viewModel: VM

    /**
     * key - route экрана,
     * value - [ScreenComponents] для него
     */
    protected abstract val screenComponentsMap: Map<String, ScreenComponents<*>>

    protected val navigationActor by lazy { NavigationActorImpl(this, navController) }

    protected val toastActor by lazy { ToastActorImpl(this) }

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

    protected lateinit var scope: CoroutineScope
    protected lateinit var navController: NavHostController

    @Composable
    abstract fun SetScreenContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            scope = rememberCoroutineScope()
            navController = rememberNavController()

            screenComponentsMap.values.forEach {
                it.observeNetworkConnectionHandler()
                handleAlerts(it.viewModel, it.alertDelegate)
                handleVmEvents(it.viewModel)
            }

            AppBackground {
                SetScreenContent()
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
    }

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
        return doOnPermissionsResult(
            permissionsHelper,
            rationale,
            code,
            permissions.toSet(),
            handler
        )
    }

    @CallSuper
    protected open fun handleAlerts(viewModel: BaseViewModel, delegate: BaseAlertDelegate<*>) {
        delegate.handleAlerts()
    }

    @CallSuper
    protected open fun handleVmEvents(viewModel: BaseViewModel) {
        viewModel.handleEvents(this, navigationActor, toastActor)
    }

    protected open fun createActivityDelegates(): List<IComponentDelegate<*>> = listOf()

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

    private fun ScreenComponents<*>.observeNetworkConnectionHandler() {
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

    protected class ScreenComponents<VM : BaseViewModel>(
        val viewModel: VM,
        val alertDelegate: BaseAlertDelegate<VM>,
        val connectionHandler: ConnectionHandler? = null,
    )
}