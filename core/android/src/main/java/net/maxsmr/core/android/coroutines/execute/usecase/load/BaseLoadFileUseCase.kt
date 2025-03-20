package net.maxsmr.core.android.coroutines.execute.usecase.load

import android.content.Context
import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.core.android.coroutines.execute.usecase.UseCase
import java.io.OutputStream

abstract class BaseLoadFileUseCase<P : BaseLoadFileUseCase.IFileParams, Result>(
    private val context: Context
) : UseCase<P, Result>(Dispatchers.IO) {

    abstract suspend fun writeTo(outputStream: OutputStream)

    protected fun P.name(): String {
        val name = name
        require(name.isNotEmpty()) { "File name not specified" }
        return if (ext.isNotEmpty()) {
            name.appendExtension(ext)
        } else {
            name
        }
    }

    interface IFileParams {
        val name: String
        val ext: String
    }
}