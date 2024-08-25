package net.maxsmr.mxstemplate.initializers

import android.content.Context
import androidx.startup.Initializer
import net.maxsmr.core.android.initBaseAppName
import net.maxsmr.core.android.initModuleCoreAndroidContext
import net.maxsmr.mxstemplate.R

class ModulesInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // Инициализация модулей
        initModuleCoreAndroidContext(context)
        initBaseAppName(context.getString(R.string.app_name))
        // #####################
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        TimberInitializer::class.java,
        RetrofitInitializer::class.java
    )
}