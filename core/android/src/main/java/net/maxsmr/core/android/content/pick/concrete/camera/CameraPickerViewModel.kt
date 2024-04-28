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
import net.maxsmr.core.android.content.storage.FileContentStorage
import net.maxsmr.core.android.content.storage.UriContentStorage
import net.maxsmr.core.android.content.storage.UriStorageAdapter
import net.maxsmr.core.android.content.storage.app_private.ExternalFileStorage
import net.maxsmr.core.android.content.storage.app_private.InternalFileStorage
import net.maxsmr.core.android.content.storage.shared.SharedStorage
import java.util.Date

internal class CameraPickerViewModel(
    state: SavedStateHandle,
) : BaseViewModel(state) {

    private var photoResultUri: Uri? by persistableValue()
    private var videoResultUri: Uri? by persistableValue()

    private var storage: UriContentStorage? = null
    private var storageType: CameraPickerParams.StorageType? = null

    fun init(params: CameraPickerParams, context: Context) {
        if (storageType != null && storageType == params.storageType) return
        storageType = params.storageType
        val contentType = params.pickType.toContentType()
        storage = when (params.storageType) {
            CameraPickerParams.StorageType.INTERNAL ->
                UriStorageAdapter(InternalFileStorage(FileContentStorage.Type.PERSISTENT), context = context)
            CameraPickerParams.StorageType.EXTERNAL ->
                UriStorageAdapter(
                    ExternalFileStorage(FileContentStorage.Type.PERSISTENT, contentType, context),
                    context = context)
            CameraPickerParams.StorageType.SHARED ->
                SharedStorage.create(contentType, context)
        }
    }

    fun createCameraBox(params: CameraPickerParams, context: Context): Uri? {
        return storage?.create(params.fileName(), params.subPath, context)?.onSuccess {
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

    fun onPickCancelled(context: Context) {
        storage?.let { storage ->
            photoResultUri?.let { storage.delete(it, context) }
            videoResultUri?.let { storage.delete(it, context) }
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