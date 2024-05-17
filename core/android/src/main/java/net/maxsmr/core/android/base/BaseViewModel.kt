package net.maxsmr.core.android.base

import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.lifecycle.*
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.doOnNext
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.states.ILoadState
import net.maxsmr.core.android.R
import net.maxsmr.core.android.base.BaseViewModel.*
import net.maxsmr.core.android.base.actions.NavigationCommand
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.base.connection.ConnectionManager
import net.maxsmr.core.android.base.delegates.getPersistableKey
import net.maxsmr.core.android.content.pick.PickResult
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.exceptions.NoConnectivityException
import kotlin.reflect.KProperty

/**
 * Базовая ViewModel для использования в приложении.
 *
 * @param state обертка над savedInstanceState: Bundle для возможности восстанавливать состояния полей VM
 * после смерти и возобновления процесса приложения. SavedInstanceState может быть использован как фрагмента
 * (если VM не расшаренная), так и activity (если расшаренная), см [net.maxsmr.app.common.gui.BaseVmFragment.isSharedViewModel].
 * НЕ НУЖНО сохранять все подряд, т.к. во-первых объем данных ограничен,
 * во-вторых можно прийти к неправильному поведению. Например, НЕ НУЖНО сохранять ответы запросов,
 * т.к. возобновление процесса может случиться через значительный промежуток времени,
 * и данные того запроса могут быть неактуальны, поэтому запросы лучше повторять. Хорошие кандидаты для
 * сохранения - данные полей, которые юзер мог долго вводить и потратить на это много времени.
 * Также для удобства хранения в state добавлены делегаты [PersistableLiveData], [PersistableLiveDataInitial],
 * [PersistableValue], [PersistableValueInitial]
 */
abstract class BaseViewModel(
    val state: SavedStateHandle,
) : ViewModel(), LifecycleOwner {

    val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    protected val KProperty<*>.persistableKey: String
        get() = this@BaseViewModel.getPersistableKey(this)

    /**
     * Для навигации по фрагментам графа (в этом же модуле, иначе не будет сгенерированных Action),
     * привязанного к NavHostFragment,
     * в котором имеется данный [BaseNavigationFragment], кто будет обозревать ивенты
     */
    val _navigationCommands: MutableLiveData<VmEvent<NavigationCommand>> = MutableLiveData()

    val navigationCommands = _navigationCommands as LiveData<VmEvent<NavigationCommand>>

    val _toastCommands: MutableLiveData<VmEvent<ToastAction>> = MutableLiveData()

    val toastCommands = _toastCommands as LiveData<VmEvent<ToastAction>>

    /**
     * Очередь для показа диалогов
     */
    open val dialogQueue: AlertQueue by lazy { AlertQueue() }

    /**
     * Очередь сообщений для показа в снекбаре
     */
    open val snackbarQueue: AlertQueue by lazy { AlertQueue() }

    /**
     * Определяет логику обработки событий состояния сети. Переопределите, если требуется обработка.
     */
    open val connectionManager: ConnectionManager? by lazy { ConnectionManager(snackbarQueue) }

    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }

    init {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        Handler(Looper.getMainLooper()).post { onInitialized() }
    }


    /**
     * Метод вызывается после выполнения init блока конкретного класса. Полезен для задания логики
     * в подклассах BaseViewModel, имеющих собственных наследников.
     *
     * Кейс: абстрактный класс А с open методом d(). Его наследник B переопределяет метод d() с
     * обращением к своим полям. Если метод d() вызывается в init блоке A, то на этот момент
     * поля класса B еще не проинициализированы и при обращении к ним можно получить краш или баг.
     */
    @CallSuper
    protected open fun onInitialized() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    final override val lifecycle: LifecycleRegistry
        get() = lifecycleRegistry

    override fun onCleared() {
        super.onCleared()
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    /**
     * Не использовать для показа ошибок запросов. Для этого используйте [showErrorDialog]
     */
    @JvmOverloads
    fun showOkDialog(tag: String, message: String, title: String? = null) {
        showOkDialog(tag, TextMessage(message), title?.let { TextMessage(it) })
    }

    /**
     * Не использовать для показа ошибок запросов. Для этого используйте [showErrorDialog]
     */
    @JvmOverloads
    fun showOkDialog(tag: String, message: TextMessage, title: TextMessage? = null) {
        AlertBuilder(tag)
            .setTitle(title)
            .setMessage(message)
            .setAnswers(Alert.Answer((android.R.string.ok)))
            .build()
    }

    /**
     * Не использовать для показа ошибок запросов. Для этого используйте [showErrorDialog]
     */
    fun showNoInternetDialog() {
        AlertBuilder(DIALOG_TAG_NO_INTERNET)
            .setTitle(R.string.server_unavailable)
            .setMessage(R.string.no_internet)
            .setAnswers(Alert.Answer(R.string.understand))
            .build()
    }

    /**
     * Использовать для показа ошибок запрсов.
     */
    fun showErrorDialog(message: TextMessage?, error: Exception?) {
//        if (error?.getErrorCode() in ErrorCode.ERRORS_DIALOGS_HANDLEABLE) {
//            return
//        }

        when (error) {
            is NetworkException -> showNoInternetDialog()
            is NoConnectivityException -> showNoInternetDialog()
            is ApiException -> showOkDialog(
                DIALOG_TAG_SERVER_ERROR,
                message ?: TextMessage(R.string.unexpected_error)
            )
//            is EkmpException -> showOkDialog(DIALOG_TAG_UNKNOWN_ERROR, message ?: error.textMessage)
            else -> showOkDialog(DIALOG_TAG_UNKNOWN_ERROR, message ?: TextMessage(R.string.unexpected_error))
        }
    }

    fun showYesNoPermissionDialog(
        message: String,
        onPositiveSelect: () -> Unit,
        onNegativeSelect: () -> Unit
    ) {
        AlertBuilder(DIALOG_TAG_PERMISSION_YES_NO)
            .setMessage(message)
            .setAnswers(
                Alert.Answer(R.string.yes).onSelect {
                    onPositiveSelect()
                },
                Alert.Answer(R.string.no).onSelect {
                    onNegativeSelect()
                })
            .build()
    }

    fun <T : Any> checkConnectionAndRun(targetAction: () -> T?): T? {
        return if (checkConnection()) targetAction() else null
    }

    fun checkConnection(): Boolean = NetworkStateManager.hasConnection().also {
        if (!it) showNoInternetDialog()
    }

    fun navigate(command: NavigationCommand.ToDirection) {
        _navigationCommands.postValue(VmEvent(command))
    }

    fun navigateBack() {
        _navigationCommands.postValue(VmEvent(NavigationCommand.Back))
    }

    fun showToast(action: ToastAction) {
        _toastCommands.postValue(VmEvent(action))
    }

    fun onPickerResultError(error: PickResult.Error) {
        showOkDialog(
            DIALOG_TAG_PICKER_ERROR,
            TextMessage(R.string.pick_result_error_format, error.reason)
        )
    }

    /**
     * Добавляет, либо удаляет диалог с тегом [tag] из очереди в зависимости от параметра [add]
     */
    protected fun AlertQueue.toggle(add: Boolean, tag: String, builderConfig: (AlertBuilder.() -> Unit)? = null) {
        if (add) {
            AlertBuilder(tag, this).apply { builderConfig?.invoke(this) }.build()
        } else {
            removeAllWithTag(tag)
        }
    }

    protected fun <T> LiveData<ILoadState<T>>.bindProgress(
        tag: String = DIALOG_TAG_PROGRESS,
        @StringRes messageRes: Int? = null,
    ): LiveData<ILoadState<T>> =
        doOnNext {
            if (it?.isLoading == true) {
                AlertBuilder(tag, dialogQueue).setMessage(messageRes).build()
            } else {
                dialogQueue.removeAllWithTag(tag)
            }
        }

    @JvmOverloads
    protected inline fun <T> LiveData<T>.observe(
        owner: LifecycleOwner = this@BaseViewModel,
        crossinline onNext: (T) -> Unit,
    ): Observer<T> {
        val observer = Observer<T> { onNext(it) }
        this.observe(owner, observer)
        return observer
    }

    @JvmOverloads
    protected inline fun <T> LiveData<VmEvent<T>>.observeEvents(
        owner: LifecycleOwner = this@BaseViewModel,
        crossinline onNext: (T) -> Unit,
    ): Observer<VmEvent<T>> {
        val observer = Observer<VmEvent<T>> {
            it.get()?.let(onNext)
        }
        this.observe(owner, observer)
        return observer
    }

    /**
     * Нужен для того, чтобы создавать алерты можно было только во ViewModel, но не во фрагменте.
     * Создаваемые алерты помещаются в очередь, хранимую во ViewModel. При создании алерта во
     * фрагменте с использованием лямбд (они держат ссылку на внешний класс, т.е. фрагмент) получаем
     * утечку памяти при смене конфигурации (т.к. алерт лежит в очереди VM и содержит лямбду,
     * которая содержит ссылку на фрагмент)
     */
    @Suppress("DEPRECATION")
    inner class AlertBuilder(
        tag: String, queue: AlertQueue = dialogQueue,
    ) : AlertQueueItem.Builder(tag, queue)

    companion object {

        const val DIALOG_TAG_NO_INTERNET = "no_internet"
        const val DIALOG_TAG_SERVER_ERROR = "server_error"
        const val DIALOG_TAG_UNKNOWN_ERROR = "unknown_error"
        const val DIALOG_TAG_PROGRESS = "progress"
        const val DIALOG_TAG_PERMISSION_YES_NO = "permission_yes_no"
        const val DIALOG_TAG_PICKER_ERROR = "content_picker_error"
    }
}