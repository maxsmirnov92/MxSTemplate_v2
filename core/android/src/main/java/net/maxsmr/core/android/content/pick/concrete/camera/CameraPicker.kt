package net.maxsmr.core.android.content.pick.concrete.camera

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import net.maxsmr.core.android.content.pick.PersistablePermission
import net.maxsmr.core.android.content.pick.concrete.ConcretePicker

/**
 * Пикер для взятия фото или видео с камеры.
 */
internal class CameraPicker(viewModelStoreOwner: ViewModelStoreOwner) : ConcretePicker<CameraPickerParams> {

    private val viewModel: CameraPickerViewModel =
        ViewModelProvider(viewModelStoreOwner)[CameraPickerViewModel::class.java]

    override fun intent(params: CameraPickerParams, context: Context): Intent {
        return Intent(params.pickType.intentAction).also { viewModel.init(params, context) }
    }

    fun addExtrasToIntent(intent: Intent, params: CameraPickerParams, context: Context) {
        //Метод вызывается уже после выбора приложения камеры. Сразу в intent создать файл для камеры нельзя,
        //т.к. это происходит до запроса разрешений и на некоторых девайсах это фейлится.
        //Также на некоторых девайсах вызов метода создает пустой "битый" файл, если юзер выберет не камеру,
        // этот файл так и останется
        intent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel.createCameraUri(params, context))
    }

    override fun requiredPermissions(
        params: CameraPickerParams,
        context: Context,
    ): Array<String> {
        return viewModel.requiredPermissions(params, context)
    }

    override fun onPickResult(
        params: CameraPickerParams,
        uri: Uri?,
        permission: PersistablePermission,
        contentResolver: ContentResolver
    ): Uri? {
        require(permission == PersistablePermission.NONE) { "persistable permission must be NONE for CameraPicker" }
        return viewModel.onPickResult(params, uri)
    }

    override fun onPickCancelled() {
        viewModel.onPickCancelled()
    }
}