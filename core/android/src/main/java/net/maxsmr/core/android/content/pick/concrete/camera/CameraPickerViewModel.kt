package net.maxsmr.core.android.content.pick.concrete.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.github.kittinunf.result.getOrNull
import com.github.kittinunf.result.onSuccess
import net.maxsmr.commonutils.format.formatDate
import net.maxsmr.commonutils.media.toContentUri
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.content.storage.ContentStorage.Companion.createUriStorage
import net.maxsmr.core.android.content.storage.UriContentStorage
import java.util.Date

internal class CameraPickerViewModel(
    state: SavedStateHandle,
) : BaseViewModel(state) {

    private var photoResultUri: Uri? by persistableValue()
    private var videoResultUri: Uri? by persistableValue()

    private var storage: ContentStorage<Uri>? = null
    private var storageType: ContentStorage.StorageType? = null

    fun init(params: CameraPickerParams, context: Context) {
        if (storageType != null && storageType == params.storageType) return
        storageType = params.storageType
        val contentType = params.pickType.toContentType()
        storage = createUriStorage(params.storageType, contentType, context)
    }

    fun createCameraBox(params: CameraPickerParams, context: Context): Uri? {
        return storage?.create(params.fileName(), params.subPath)?.onSuccess {
            when (params.pickType) {
                CameraPickerParams.PickType.PHOTO -> photoResultUri = it
                CameraPickerParams.PickType.VIDEO -> videoResultUri = it
            }
        }?.getOrNull()?.toContentUri(context)
    }

    fun onPickResult(params: CameraPickerParams, uri: Uri?): Uri? = when (params.pickType) {
        //для фото uri не приходит в onActivityResult
        CameraPickerParams.PickType.PHOTO -> photoResultUri
        //а для видео приходит, используем ее
        CameraPickerParams.PickType.VIDEO -> uri
    }

    fun onPickCancelled() {
        storage?.let { storage ->
            photoResultUri?.let { storage.delete(it) }
            videoResultUri?.let { storage.delete(it) }
        }
    }

    fun requiredPermissions(params: CameraPickerParams, context: Context): Array<String> {
        if (storage == null) {
            init(params, context)
        }
        return storage?.requiredPermissions(read = false, write = true)?.let {
            params.requiredPermissions + it
        } ?: params.requiredPermissions
    }

    private fun CameraPickerParams.fileName(): String {
        return if (unique) {
            val uniquePart = formatDate(Date(System.currentTimeMillis()), UNIQUE_FILE_PART_FORMAT)
            "$namePrefix$uniquePart$extension"
        } else {
            "$namePrefix$extension"
        }
    }

    companion object {

        private const val UNIQUE_FILE_PART_FORMAT = "ddMMyyyy_HHmmss"
    }
}