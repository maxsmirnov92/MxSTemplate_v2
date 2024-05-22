package net.maxsmr.core.android.content.storage.shared

import android.content.Context
import android.net.Uri
import com.github.kittinunf.result.Result
import net.maxsmr.core.android.baseAppName
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.content.storage.UriContentStorage
import java.io.InputStream
import java.io.OutputStream

/**
 * Абстракция хранилища расшариваемого между приложениями контента.
 *
 * Переопределяет относительный path во всех методах, жестко заменяя его на имя приложения, таким образом
 * все фото будут находиться в Pictures/$baseAppName/, видео в Movies/$baseAppName/ и т.д.
 */
abstract class AbsSharedStorage(
    protected val contentType: ContentType,
    context: Context,
) : UriContentStorage(context) {

    /**
     * Название приложения в качестве поддиректории основной директории расшариваемых файлов.
     * Не менять.
     */
    val appDir by lazy { baseAppName }

    override fun exists(name: String, path: String?): Result<Boolean, Exception> {
        return super.exists(name, this.path)
    }

    override fun read(name: String, path: String?): Result<String, Exception> {
        return super.read(name, this.path)
    }

    override fun shareUri(name: String, path: String?): Result<Uri?, Exception> {
        return super.shareUri(name, this.path)
    }

    override fun getOrCreate(name: String, path: String?): Result<Uri, Exception> {
        return super.getOrCreate(name, this.path)
    }

    override fun write(content: String, name: String, path: String?): Result<Unit, Exception> {
        return super.write(content, name, this.path)
    }

    override fun delete(name: String, path: String?): Result<Boolean, Exception> {
        return super.delete(name, this.path)
    }

    override fun openInputStream(name: String, path: String?): Result<InputStream, Exception> {
        return super.openInputStream(name, this.path)
    }

    override fun openOutputStream(name: String, path: String?): Result<OutputStream, Exception> {
        return super.openOutputStream(name, this.path)
    }

    override fun copy(
        srcName: String,
        srcPath: String?,
        dstStorage: ContentStorage<*>,
        dstName: String,
        dstPath: String?,
    ): Result<Unit, Exception> {
        return super.copy(srcName, path, dstStorage, dstName, dstPath)
    }

}