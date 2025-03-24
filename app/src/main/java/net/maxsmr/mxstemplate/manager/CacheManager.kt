package net.maxsmr.mxstemplate.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext context: Context,
) {

    val dirPath: String by lazy {
        File(context.cacheDir, "OkHttpCache").path
    }

    var disableCache: Boolean = false
}