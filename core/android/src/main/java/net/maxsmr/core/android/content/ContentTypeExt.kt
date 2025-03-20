package net.maxsmr.core.android.content

import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlin.IllegalStateException

val ContentType.mediaStoreId: String
    get() = when (this) {
        ContentType.IMAGE -> MediaStore.Images.Media._ID
        ContentType.VIDEO -> MediaStore.Video.Media._ID
        ContentType.AUDIO -> MediaStore.Audio.Media._ID
        ContentType.DOCUMENT -> MediaStore.Downloads._ID
    }

val ContentType.mediaStoreDisplayName: String
    get() = when (this) {
        ContentType.IMAGE -> MediaStore.Images.Media.DISPLAY_NAME
        ContentType.VIDEO -> MediaStore.Video.Media.DISPLAY_NAME
        ContentType.AUDIO -> MediaStore.Audio.Media.DISPLAY_NAME
        ContentType.DOCUMENT -> MediaStore.Downloads.DISPLAY_NAME
    }

val ContentType.mediaStoreExternalContentUri: Uri
    get() = when (this) {
        ContentType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ContentType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ContentType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        ContentType.DOCUMENT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            throw IllegalStateException("MediaStore.Downloads not available before Android Q")
        }
    }

val ContentType.rootDir: String
    get() = when (this) {
        ContentType.IMAGE -> Environment.DIRECTORY_PICTURES
        ContentType.VIDEO -> Environment.DIRECTORY_MOVIES
        ContentType.AUDIO -> Environment.DIRECTORY_MUSIC
        ContentType.DOCUMENT -> Environment.DIRECTORY_DOWNLOADS
    }

fun ContentType.contentUri(writable: Boolean): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val volumeName = if (writable) MediaStore.VOLUME_EXTERNAL_PRIMARY else MediaStore.VOLUME_EXTERNAL
    when (this) {
        ContentType.IMAGE -> MediaStore.Images.Media.getContentUri(volumeName)
        ContentType.VIDEO -> MediaStore.Video.Media.getContentUri(volumeName)
        ContentType.AUDIO -> MediaStore.Audio.Media.getContentUri(volumeName)
        ContentType.DOCUMENT -> MediaStore.Downloads.getContentUri(volumeName)
    }
} else {
    mediaStoreExternalContentUri
}