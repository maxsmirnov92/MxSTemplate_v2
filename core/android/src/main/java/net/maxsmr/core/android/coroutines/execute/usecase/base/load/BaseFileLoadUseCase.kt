package net.maxsmr.core.android.coroutines.execute.usecase.base.load

import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.core.android.coroutines.execute.usecase.FlowResultUseCase
import java.io.OutputStream

abstract class BaseFileLoadUseCase<P : BaseFileLoadUseCase.IFileParams, Result> : FlowResultUseCase<P, Result>(Dispatchers.IO) {

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