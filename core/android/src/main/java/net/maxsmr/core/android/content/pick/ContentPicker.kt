package net.maxsmr.core.android.content.pick

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import net.maxsmr.commonutils.flow.observeEvents
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.R
import net.maxsmr.core.android.base.delegates.FragmentViewBindingDelegate.Companion.onViewLifecycleCreated
import net.maxsmr.core.android.base.result.ActivityResultRegisterer
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.android.content.pick.concrete.camera.CameraPicker
import net.maxsmr.core.android.content.pick.concrete.camera.CameraPickerParams
import net.maxsmr.core.android.content.pick.concrete.media.MediaPicker
import net.maxsmr.core.android.content.pick.concrete.media.MediaPickerParams
import net.maxsmr.core.android.content.pick.concrete.saf.SafPicker
import net.maxsmr.core.android.content.pick.concrete.saf.SafPickerParams
import net.maxsmr.core.android.permissions.PermissionsRequester

/**
 * Фасад для взятия контента из разных источников и обработки разрешений.
 *
 * Для использования необходимо выполнить следующие шаги:
 * 1. Создать во фрагменте поле пикера с помощью [Builder]
 *      * Добавить 1 или несколько запросов взятия контента, которые используются на фрагменте ([Builder.addRequest])
 *      * Ключевыми для каждого запроса являются параметры requestCode и лямбда onSuccess для обработки результата
 * 1. В нужный момент вызвать метод [pick], передав в параметры 1 из requestCode'ов, с которыми был создан этот пикер.
 * Запрашивать разрешения не требуется, пикер делает это внутри самостоятельно.
 */
class ContentPicker<T> private constructor(
    private val host: T,
    private val requests: Set<PickRequest>,
    private val permissionHandler: PermissionHandler,
    private val showChooserAction: (Int, TextMessage, Map<ConcretePickerParams, IntentWithPermissions>) -> Unit,
) where T : PermissionsRequester, T : ActivityResultRegisterer, T : ViewModelStoreOwner, T : LifecycleOwner {

    private val viewModel: ContentPickerViewModel by lazy {
        // VM шарится между ContentPicker и AppIntentChooser
        ViewModelProvider(host.requireActivity)[ContentPickerViewModel::class.java]
    }

    private val cameraPicker by lazy { CameraPicker(host) }
    private val mediaPicker by lazy { MediaPicker() }
    private val safPicker by lazy { SafPicker() }

    private val resultLaunchers = mutableMapOf<Int, ActivityResultLauncher<Intent>>()

    private val context by lazy { host.requireContext }

    init {
        // регистрируем launcher'ы для всех реквестов в этом пикере
        requests.forEach {
            val launcher = host.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                PickerResultHandler(it)
            )
            resultLaunchers[it.requestCode] = launcher
        }
    }

    private inner class PickerResultHandler(val request: PickRequest) : ActivityResultCallback<ActivityResult> {

        /**
         * Метод обработки результата. **Важно**! Срабатывает для result != [Activity.RESULT_OK],
         * чтобы подчищать файлы камеры в случае возврата назад без фото.
         */
        override fun onActivityResult(result: ActivityResult) {
            val pickerType = viewModel.selectedPickerType ?: return
            val resultCode = result.resultCode
            val data = result.data
            if (resultCode != Activity.RESULT_OK) {
                when (pickerType) {
                    request.takePhotoParams?.type, request.takeVideoParams?.type -> cameraPicker.onPickCancelled()
                    request.mediaParams?.type -> mediaPicker.onPickCancelled()
                    request.safParams?.type -> safPicker.onPickCancelled()
                    else -> throw IllegalStateException("Unexpected params type $pickerType")
                }
                return
            }
            val uri = when (pickerType) {
                request.takePhotoParams?.type -> cameraPicker.onPickResult(
                    request.takePhotoParams,
                    data?.data,
                    request.permission,
                    context.contentResolver
                )

                request.takeVideoParams?.type -> cameraPicker.onPickResult(
                    request.takeVideoParams,
                    data?.data,
                    request.permission,
                    context.contentResolver
                )

                request.mediaParams?.type -> mediaPicker.onPickResult(
                    request.mediaParams,
                    data?.data,
                    request.permission,
                    context.contentResolver
                )

                request.safParams?.type -> safPicker.onPickResult(
                    request.safParams,
                    data?.data,
                    request.permission,
                    context.contentResolver
                )

                else -> throw IllegalStateException("Unexpected params type $pickerType")
            }
            if (uri == null) {
                viewModel.onError(request.requestCode, request.errorMessage)
            } else {
                viewModel.onSuccess(request.requestCode, uri, pickerType)
            }
        }
    }

    init {
        host.onViewLifecycleCreated {
            // Наблюдаем за выбором аппа пользователем, запускаем выбранное приложение либо запрашиваем необходимые разрешения
            viewModel.appChoicesEvent.observeEvents(host) { choice ->
                val requiredPermissions = choice.requiredPermissions().toSet()
                permissionHandler.handle(choice.requestCode, requiredPermissions,
                    onDenied = {
                        // здесь нет ToastActionImpl :(
                        Toast.makeText(
                            context,
                            context.getString(R.string.pick_no_permissions),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onGranted = { choice.select() }
                )
            }
            //Наблюдаем за результатом, вызываем соответствующие методы в случае успеха или неуспеха на нужном запросе
            viewModel.pickResultEvent.observeEvents(host) { result ->
                val request = requests.find { it.requestCode == result.requestCode }
                    ?: return@observeEvents
                when (result) {
                    is PickResult.Success -> request.onSuccess(result)
                    is PickResult.Error -> request.onError?.invoke(result)
                        ?: Toast.makeText(
                            context,
                            result.reason.get(context),
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }
        }
    }

    /**
     * Метод для взятия контента
     *
     * @param requestCode код запроса для взятия контента. **Обязательно** должен быть 1 из значений,
     * переданных пикеру при его создании
     */
    fun pick(requestCode: Int, context: Context) {
        val request = requests.first { it.requestCode == requestCode }
        val intents = request.intentsWithPermissions()

        val permissionsHelper = host.permissionsHelper

        val flatIntents = intents.flatMap { (params, srcIntent) ->
            srcIntent.flatten(context).map { params to it }
        }
        if (flatIntents.isEmpty()) {
            viewModel.onError(request.requestCode, TextMessage(R.string.pick_no_apps))
            return
        }
        //Чузер также надо показать, если приложение 1, но оно не может быть использовано из-за запрета
        // разрешения с опцией "Больше не спрашивать", т.к. на чузере есть возможность перехода к настройкам для дачи разрешения
        val needShowChooser = flatIntents.size > 1 ||
                permissionsHelper.filterDeniedNotAskAgain(
                    context,
                    flatIntents.first().second.permissions.toList()
                ).isNotEmpty()
        if (needShowChooser) {
            showChooserAction(requestCode, request.chooserTitle, intents)
        } else {
            flatIntents.first().let { (params, intent) ->
                viewModel.onAppChoice(ContentPickerViewModel.AppChoice(requestCode, params, intent))
            }
        }
    }

    private fun ContentPickerViewModel.AppChoice.requiredPermissions() = when (params) {
        is CameraPickerParams -> cameraPicker.requiredPermissions(params, context)
        is MediaPickerParams -> mediaPicker.requiredPermissions(params, context)
        is SafPickerParams -> safPicker.requiredPermissions(params, context)
        else -> throw IllegalArgumentException("Unexpected params ${this::class.java.simpleName}")
    }

    private fun ContentPickerViewModel.AppChoice.select() {
        val launcher = resultLaunchers[requestCode] ?: return
        viewModel.selectedPickerType = params.type
        launcher.launch(intentWithPermissions.intent.also {
            if (params is CameraPickerParams) {
                cameraPicker.addExtrasToIntent(it, params, context)
            }
        })
    }

    private fun PickRequest.intentsWithPermissions(): Map<ConcretePickerParams, IntentWithPermissions> {
        val intentList: MutableMap<ConcretePickerParams, IntentWithPermissions> = mutableMapOf()

        if (takePhotoParams != null) {
            intentList[takePhotoParams] =
                cameraPicker.intentWithPermissions(takePhotoParams, context)
        }
        if (takeVideoParams != null) {
            intentList[takeVideoParams] =
                cameraPicker.intentWithPermissions(takeVideoParams, context)
        }
        if (mediaParams != null) {
            intentList[mediaParams] = mediaPicker.intentWithPermissions(mediaParams, context)
        }
        if (safParams != null) {
            intentList[safParams] = safPicker.intentWithPermissions(safParams, context)
        }
        return intentList
    }


    /**
     * Билдер для получения инстанса [ContentPicker].
     *
     * Для создания обязателен хотя бы 1 вызов [addRequest].
     */
    open class Builder<T>(
        protected val host: T,
        private val permissionHandler: PermissionHandler,
        private val showChooserAction: (Int, TextMessage, Map<ConcretePickerParams, IntentWithPermissions>) -> Unit,
    ) where T : PermissionsRequester, T : ActivityResultRegisterer, T : ViewModelStoreOwner, T : LifecycleOwner {

        private val requests: MutableSet<PickRequest> = mutableSetOf()

        fun addRequest(request: PickRequest) = apply {
            requests.add(request)
        }

        fun build(): ContentPicker<T> {
            check(requests.isNotEmpty()) {
                "Content picker without provided requests is useless"
            }
            return ContentPicker(host, requests, permissionHandler, showChooserAction)
        }
    }

    interface PermissionHandler {

        fun handle(requestCode: Int, permissions: Set<String>, onDenied: (Set<String>) -> Unit, onGranted: () -> Unit)
    }
}