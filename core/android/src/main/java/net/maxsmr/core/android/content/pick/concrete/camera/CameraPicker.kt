package net.maxsmr.core.android.content.pick.concrete.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import net.maxsmr.core.android.content.pick.concrete.ConcretePicker

/**
 * Пикер для взятия фото или видео с камеры.
 */
internal class CameraPicker(
    fragment: Fragment,
) : ConcretePicker<CameraPickerParams> {

    private val viewModel: CameraPickerViewModel =
        ViewModelProvider(fragment)[CameraPickerViewModel::class.java]

    override fun intent(params: CameraPickerParams, context: Context): Intent {
        return Intent(params.pickType.intentAction).also { viewModel.init(params, context) }
    }

    fun addExtrasToIntent(intent: Intent, params: CameraPickerParams, context: Context) {
        //Метод вызывается уже после выбора приложения камеры. Сразу в intent создать файл для камеры нельзя,
        //т.к. это происходит до запроса разрешений и на некоторых девайсах это фейлится.
        //Также на некоторых девайсах вызов метода создает пустой "битый" файл, если юзер выберет не камеру,
        // этот файл так и останется
        intent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel.createCameraBox(params, context))
    }

    override fun requiredPermissions(params: CameraPickerParams, context: Context): Array<String> {
        return viewModel.requiredPermissions(params, context)
    }

    override fun onPickResult(
        params: CameraPickerParams,
        uri: Uri?,
        needPersistableAccess: Boolean,
        context: Context,
    ): Uri? {
        return viewModel.onPickResult(params, uri)
    }

    override fun onPickCancelled(context: Context) {
        viewModel.onPickCancelled(context)
    }
}