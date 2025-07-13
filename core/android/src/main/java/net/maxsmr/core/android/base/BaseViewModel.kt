package net.maxsmr.core.android.base

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.lifecycle.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import net.maxsmr.commonutils.flow.observeLatest
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.isAtLeastR
import net.maxsmr.commonutils.live.doOnNext
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.states.ILoadState
import net.maxsmr.core.android.R
import net.maxsmr.core.android.base.BaseViewModel.*
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.base.actions.NavigationAction.NavigationCommand
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.actions.SnackbarExtraData.SnackbarLength
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.base.actions.ToastExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem.UniqueStrategy
import net.maxsmr.core.android.base.alert.showOkAlert
import net.maxsmr.core.android.base.alert.showYesNoAlert
import net.maxsmr.core.android.base.connection.ConnectionManager
import net.maxsmr.core.android.content.pick.PickResult
import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.NetworkException

abstract class BaseViewModel(
    val state: SavedStateHandle,
    context: Context,
) : ViewModel() {

    /**
     * Для навигации по фрагментам графа (в этом же модуле, иначе не будет сгенерированных Action),
     * привязанного к NavHostFragment,
     * в котором имеется данный [BaseNavigationFragment], кто будет обозревать ивенты
     */
    val navigationCommand by lazy { _navigationCommand.asStateFlow() }

    val toastCommand by lazy { _toastCommand.asStateFlow() }

    // MutableStateFlow подходит лучше, чем MutableSharedFlow, т.к. гарантированно будет хранить последнее значение;
    // в то время как tryEmit у MutableSharedFlow может не сработать при переполнении буфера, т.к. не является suspend-функцией

    /**
     * Очередь для показа диалогов
     */
    val dialogQueue: AlertQueue by lazy { AlertQueue() }

    /**
     * Очередь сообщений для показа снекбаров
     */
    val snackbarQueue: AlertQueue by lazy { AlertQueue() }

    /**
     * Очередь сообщений для показа тостов
     */
    val toastQueue: AlertQueue by lazy { AlertQueue() }

    /**
     * Определяет логику обработки событий состояния сети. Переопределите, если требуется обработка.
     */
    val connectionManager: ConnectionManager by lazy { ConnectionManager(context, viewModelScope, snackbarQueue) }

    protected val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(javaClass)

    private val _navigationCommand = MutableStateFlow<VmEvent<NavigationAction>?>(null)

    private val _toastCommand = MutableStateFlow<VmEvent<ToastAction>?>(null)

    init {
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
    protected open fun onInitialized() {}

    /**
     * Не использовать для показа ошибок запросов. Для этого используйте [showErrorDialog]
     */
    @JvmOverloads
    fun showOkDialog(
        tag: String,
        @StringRes messageResId: Int,
        @StringRes title: Int? = null,
        configBlock: (AlertQueueItem.Builder.() -> Unit)? = null,
        onConfirmClick: (() -> Unit)? = null,
    ) {
        AlertDialogBuilder(tag).showOkAlert(messageResId, title, configBlock, onConfirmClick)
    }

    /**
     * Не использовать для показа ошибок запросов. Для этого используйте [showErrorDialog]
     */
    @JvmOverloads
    fun showOkDialog(
        tag: String,
        message: TextMessage,
        title: TextMessage? = null,
        configBlock: (AlertQueueItem.Builder.() -> Unit)? = null,
        onConfirmClick: (() -> Unit)? = null,
    ) {
        AlertDialogBuilder(tag).showOkAlert(message, title, configBlock, onConfirmClick)
    }

    fun showYesNoPermissionDialog(
        message: TextMessage,
        onSelect: ((Int) -> Unit)? = null,
    ) {
        showYesNoDialog(
            DIALOG_TAG_PERMISSION_YES_NO,
            message,
            onSelect = onSelect
        )
    }

    fun showYesNoDialog(
        tag: String,
        message: TextMessage?,
        title: TextMessage? = null,
        @StringRes positiveAnswerResId: Int = R.string.yes,
        @StringRes negativeAnswerResId: Int = R.string.no,
        @StringRes neutralAnswerResId: Int? = null,
        configBlock: (AlertQueueItem.Builder.() -> Unit)? = null,
        onSelect: ((Int) -> Unit)? = null,
    ) {
        AlertDialogBuilder(tag).showYesNoAlert(
            message,
            title,
            positiveAnswerResId,
            negativeAnswerResId,
            neutralAnswerResId,
            configBlock,
            onSelect
        )
    }

    fun showCustomDialog(tag: String, configBlock: AlertQueueItem.Builder.() -> Unit) {
        AlertDialogBuilder(tag).also {
            configBlock(it)
            it.build()
        }
    }

    fun hideDialog(tag: String) {
        dialogQueue.removeAllWithTag(tag)
    }

    /**
     * Использовать для показа ошибок запрсов.
     */
    protected fun showErrorDialog(message: TextMessage?, error: Exception?) {
//        if (error?.getErrorCode() in ErrorCode.ERRORS_DIALOGS_HANDLEABLE) {
//            return
//        }

        when (error) {
            is NetworkException -> showNoInternetDialog()
            is ApiException -> showOkDialog(
                DIALOG_TAG_SERVER_ERROR,
                message ?: TextMessage(R.string.error_unexpected)
            )
//            is EkmpException -> showOkDialog(DIALOG_TAG_UNKNOWN_ERROR, message ?: error.textMessage)
            else -> showOkDialog(DIALOG_TAG_UNKNOWN_ERROR, message ?: TextMessage(R.string.error_unexpected))
        }
    }

    private fun showNoInternetDialog() {
        AlertDialogBuilder(DIALOG_TAG_NO_INTERNET)
            .setTitle(net.maxsmr.core.network.R.string.error_server_unavailable)
            .setMessage(net.maxsmr.core.network.R.string.error_no_connection)
            .setAnswers(Alert.Answer(R.string.understand))
            .build()
    }

//    fun <T : Any> checkConnectionAndRun(targetAction: () -> T?): T? {
//        return if (checkConnection()) targetAction() else null
//    }
//
//    fun checkConnection(): Boolean = NetworkStateManager.hasConnection().also {
//        if (!it) showNoInternetDialog()
//    }

    fun navigate(command: NavigationCommand.ToDirection) {
        _navigationCommand.tryEmit(VmEvent(NavigationAction(command)))
    }

    fun navigate(command: NavigationCommand.ToDirectionWithNavDirections) {
        _navigationCommand.tryEmit(VmEvent(NavigationAction(command)))
    }

    fun navigateBack() {
        _navigationCommand.tryEmit(VmEvent(NavigationAction(NavigationCommand.Back)))
    }

    fun showSnackbar(
        message: TextMessage,
        data: SnackbarExtraData = SnackbarExtraData(),
        answer: Alert.Answer? = null,
        uniqueStrategy: UniqueStrategy = UniqueStrategy.None,
        priority: AlertQueueItem.Priority = AlertQueueItem.Priority.NORMAL,
        putInQueueHead: Boolean = false,
    ) {
        if (uniqueStrategy == UniqueStrategy.None && data.length == SnackbarLength.INDEFINITE) {
            throw IllegalArgumentException("uniqueStrategy cannot be 'None' with 'INDEFINITE' length")
        }
        AlertSnackbarBuilder(SNACKBAR_TAG_QUEUE)
            .setMessage(message)
            .setExtraData(data)
            .setUniqueStrategy(uniqueStrategy)
            .setPriority(priority, putInQueueHead)
//            .setOneShot(data.length != SnackbarExtraData.SnackbarLength.INDEFINITE)
            .also { b ->
                answer?.let {
                    b.setAnswers(answer)
                }
            }.build()

    }

    fun removeSnackbarsFromQueue() {
        snackbarQueue.removeAllWithTag(SNACKBAR_TAG_QUEUE)
    }

    fun showToast(
        message: TextMessage,
        data: ToastExtraData = ToastExtraData(),
        uniqueStrategy: UniqueStrategy = UniqueStrategy.None,
    ) {
        if (isAtLeastR()) {
            AlertToastBuilder(TOAST_TAG_QUEUE)
                .setMessage(message)
                .setExtraData(data)
                .setUniqueStrategy(uniqueStrategy.takeIf { it != UniqueStrategy.Replace } ?: UniqueStrategy.None)
                .setOneShot(uniqueStrategy == UniqueStrategy.Replace)
                .build()
        } else {
            // для API ниже 30 addCallback отсутствует,
            // соот-но тосты будут оставаться в очереди после скрытия;
            // пользуем способ с VmEvent
            _toastCommand.tryEmit(VmEvent(ToastAction(message, data)))
        }
    }

    fun removeToastsFromQueue() {
        if (isAtLeastR()) {
            toastQueue.removeAllWithTag(TOAST_TAG_QUEUE)
        }
    }

    fun onPickerResultError(error: PickResult.Error) {
        showOkDialog(
            DIALOG_TAG_PICKER_ERROR,
            TextMessage(R.string.pick_result_error_format, error.reason)
        )
    }

    fun doOnAnyAskOption(
        flow: Flow<Boolean>,
        setAskedFunc: suspend () -> Unit,
        targetAction: suspend (Boolean) -> Unit,
    ) {
        flow.take(1).observe {
            if (!it) {
                setAskedFunc()
            }
            targetAction(it)
        }
    }

    protected fun AlertQueue.toggle(
        add: Boolean,
        tag: String,
        message: TextMessage,
    ) {
        toggle(add, tag) {
            setMessage(message)
        }
    }

    /**
     * Добавляет, либо удаляет диалог с тегом [tag] из очереди в зависимости от параметра [add]
     */
    protected fun AlertQueue.toggle(
        add: Boolean,
        tag: String,
        builderConfig: (AlertDialogBuilder.() -> Unit)? = null,
    ) {
        if (add) {
            AlertDialogBuilder(tag, this).apply { builderConfig?.invoke(this) }.build()
        } else {
            removeAllWithTag(tag)
        }
    }

    protected fun <T, S : ILoadState<T>> LiveData<S>.bindProgress(
        tag: String = DIALOG_TAG_PROGRESS,
        message: TextMessage,
    ) = bindProgress(tag) {
        setMessage(message)
    }

    protected fun <T, S : ILoadState<T>> LiveData<S>.bindProgress(
        tag: String = DIALOG_TAG_PROGRESS,
        builderConfig: (AlertDialogBuilder.() -> Unit)? = null,
    ): LiveData<S> =
        doOnNext {
            if (it?.isLoading == true) {
                AlertDialogBuilder(tag).apply { builderConfig?.invoke(this) }.build()
            } else {
                hideDialog(tag)
            }
        }

    protected inline fun <T> Flow<T>.observe(
        crossinline onNext: suspend (T) -> Unit,
    ) = observeLatest(viewModelScope, onNext)

    /**
     * Нужен для того, чтобы создавать алерты можно было только во ViewModel, но не во фрагменте.
     * Создаваемые алерты помещаются в очередь, хранимую во ViewModel. При создании алерта во
     * фрагменте с использованием лямбд (они держат ссылку на внешний класс, т.е. фрагмент) получаем
     * утечку памяти при смене конфигурации (т.к. алерт лежит в очереди VM и содержит лямбду,
     * которая содержит ссылку на фрагмент)
     */
    inner class AlertDialogBuilder(
        tag: String, queue: AlertQueue = dialogQueue,
    ) : AlertQueueItem.Builder(tag, queue)

    inner class AlertSnackbarBuilder(
        tag: String, queue: AlertQueue = snackbarQueue,
    ) : AlertQueueItem.Builder(tag, queue)

    inner class AlertToastBuilder(
        tag: String, queue: AlertQueue = toastQueue,
    ) : AlertQueueItem.Builder(tag, queue)

    companion object {

        const val DIALOG_TAG_NO_INTERNET = "no_internet"
        const val DIALOG_TAG_SERVER_ERROR = "server_error"
        const val DIALOG_TAG_UNKNOWN_ERROR = "unknown_error"
        const val DIALOG_TAG_PROGRESS = "progress"
        const val DIALOG_TAG_PERMISSION_YES_NO = "permission_yes_no"
        const val DIALOG_TAG_PICKER_ERROR = "content_picker_error"
        const val DIALOG_TAG_BATTERY_OPTIMIZATION = "battery_optimization"

        const val SNACKBAR_TAG_QUEUE = "snackbar_queue"
        const val TOAST_TAG_QUEUE = "toast_queue"
    }
}