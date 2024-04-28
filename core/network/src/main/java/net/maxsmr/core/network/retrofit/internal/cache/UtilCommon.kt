package net.maxsmr.core.network.retrofit.internal.cache

import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path


internal fun FileSystem.deleteContents(directory: Path) {
    var exception: IOException? = null
    val files = try {
        list(directory)
    } catch (e: FileNotFoundException) {
        return
    }
    for (file in files) {
        try {
            if (metadata(file).isDirectory) {
                deleteContents(file)
            }

            delete(file)
        } catch (e: IOException) {
            if (exception == null) {
                exception = e
            }
        }
    }
    if (exception != null) {
        throw exception
    }
}

internal fun FileSystem.deleteIfExists(path: Path) {
    try {
        delete(path)
    } catch (e: FileNotFoundException) {
        return
    }
}