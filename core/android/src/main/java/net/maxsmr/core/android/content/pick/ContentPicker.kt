package net.maxsmr.core.android.content.pick

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.live.filterNotNull
import net.maxsmr.core.android.R
import net.maxsmr.core.android.base.delegates.FragmentViewBindingDelegate.Companion.onViewLifecycleCreated
import net.maxsmr.core.android.content.pick.ContentPicker.Builder
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.android.content.pick.concrete.camera.CameraPickerParams
import net.maxsmr.core.android.content.pick.concrete.media.MediaPicker
import net.maxsmr.core.android.content.pick.concrete.saf.SafPicker
import net.maxsmr.core.android.content.pick.concrete.saf.SafPickerParams
import net.maxsmr.core.android.content.pick.concrete.camera.CameraPicker
import net.maxsmr.core.android.content.pick.concrete.media.MediaPickerParams

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
class ContentPicker private constructor(
    private val fragment: Fragment,
    private val requests: Set<PickRequest>,
    private val permissionHandler: PermissionHandler,
    private val showChooserAction: (Int, TextMessage, Map<ConcretePickerParams, IntentWithPermissions>) -> Unit,
) {

    private val viewModel: ContentPickerViewModel by lazy {
        // VM шарится между ContentPicker и AppIntentChooser
        ViewModelProvider(fragment.requireActivity())[ContentPickerViewModel::class.java]
    }

    private val cameraPicker by lazy { CameraPicker(fragment) }
    private val mediaPicker by lazy { MediaPicker() }
    private val safPicker by lazy { SafPicker() }

    private val resultLaunchers = mutableMapOf<Int, ActivityResultLauncher<Intent>>()

    init {
        // регистрируем launcher'ы для всех реквестов в этом пикере
        requests.forEach {
            val launcher = fragment.registerForActivityResult(
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
                    request.needPersistableUriAccess,
                    fragment.requireContext()
                )

                request.takeVideoParams?.type -> cameraPicker.onPickResult(
                    request.takeVideoParams,
                    data?.data,
                    request.needPersistableUriAccess,
                    fragment.requireContext()
                )

                request.mediaParams?.type -> mediaPicker.onPickResult(
                    request.mediaParams,
                    data?.data,
                    request.needPersistableUriAccess,
                    fragment.requireContext()
                )

                request.safParams?.type -> safPicker.onPickResult(
                    request.safParams,
                    data?.data,
                    request.needPersistableUriAccess,
                    fragment.requireContext()
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
        fragment.onViewLifecycleCreated {
            //Наблюдаем за выбором аппа пользователем, запускаем выбранное приложение либо запрашиваем необходимые разрешения
            viewModel.appChoices.observe(it) {
                it.get()?.let { choice ->
                    val requiredPermissions = choice.requiredPermissions().toSet()
                    permissionHandler.handle(choice.requestCode, requiredPermissions,
                        onDenied = {
                            Toast.makeText(
                                fragment.requireContext(),
                                fragment.requireContext().getString(R.string.pick_no_permissions),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onGranted = { choice.select() }
                    )
                }
            }
            //Наблюдаем за результатом, вызываем соответствующие методы в случае успеха или неуспеха на нужном запросе
            viewModel.pickResult
                .map { it?.get() }
                .filterNotNull()
                .observe(fragment.viewLifecycleOwner) { result ->
                    val request = requests.find { it.requestCode == result?.requestCode }
                        ?: return@observe
                    when (result) {
                        is PickResult.Success -> request.onSuccess(result)
                        is PickResult.Error -> request.onError?.invoke(result)
                            ?: Toast.makeText(
                                fragment.requireContext(),
                                result.reason.get(fragment.requireContext()),
                                Toast.LENGTH_SHORT
                            ).show()

                        else -> {}
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
                permissionHandler.filterDeniedNotAskAgain(
                    fragment.requireContext(),
                    flatIntents.first().second.permissions.toList()
                ).isNotEmpty()
        if (needShowChooser) {
            showChooserAction(requestCode, request.chooserTitle, intents)
        } else {
            flatIntents.first().let { (params, intent) ->
                viewModel.appChoices.value = VmEvent(ContentPickerViewModel.AppChoice(requestCode, params, intent))
            }
        }
    }

    private fun ContentPickerViewModel.AppChoice.requiredPermissions() = when (params) {
        is CameraPickerParams -> cameraPicker.requiredPermissions(params, fragment.requireContext())
        is MediaPickerParams -> mediaPicker.requiredPermissions(params, fragment.requireContext())
        is SafPickerParams -> safPicker.requiredPermissions(params, fragment.requireContext())
        else -> throw IllegalArgumentException("Unexpected params ${this::class.java.simpleName}")
    }

    private fun ContentPickerViewModel.AppChoice.select() {
        val launcher = resultLaunchers[requestCode] ?: return
        viewModel.selectedPickerType = params.type
        launcher.launch(intentWithPermissions.intent.also {
            if (params is CameraPickerParams) {
                cameraPicker.addExtrasToIntent(it, params, fragment.requireContext())
            }
        })
    }

    private fun PickRequest.intentsWithPermissions(): Map<ConcretePickerParams, IntentWithPermissions> {
        val intentList: MutableMap<ConcretePickerParams, IntentWithPermissions> = mutableMapOf()

        if (takePhotoParams != null) {
            intentList[takePhotoParams] =
                cameraPicker.intentWithPermissions(takePhotoParams, fragment.requireContext())
        }
        if (takeVideoParams != null) {
            intentList[takeVideoParams] =
                cameraPicker.intentWithPermissions(takeVideoParams, fragment.requireContext())
        }
        if (mediaParams != null) {
            intentList[mediaParams] = mediaPicker.intentWithPermissions(mediaParams, fragment.requireContext())
        }
        if (safParams != null) {
            intentList[safParams] = safPicker.intentWithPermissions(safParams, fragment.requireContext())
        }
        return intentList
    }


    /**
     * Билдер для получения инстанса [ContentPicker].
     *
     * Для создания обязателен хотя бы 1 вызов [addRequest].
     */
    open class Builder(
        private val fragment: Fragment,
        private val permissionHandler: PermissionHandler,
        private val showChooserAction: (Int, TextMessage, Map<ConcretePickerParams, IntentWithPermissions>) -> Unit,
    ) {

        private val requests: MutableSet<PickRequest> = mutableSetOf()

        fun addRequest(request: PickRequest) = apply {
            requests.add(request)
        }

        fun build(): ContentPicker {
            check(requests.isNotEmpty()) {
                "Content picker without provided requests is useless"
            }
            return ContentPicker(fragment, requests, permissionHandler, showChooserAction)
        }
    }

    interface PermissionHandler {

        fun filterDeniedNotAskAgain(context: Context, permissions: Collection<String>): Set<String>

        fun handle(requestCode: Int, permissions: Set<String>, onDenied: (Set<String>) -> Unit, onGranted: () -> Unit)
    }
}