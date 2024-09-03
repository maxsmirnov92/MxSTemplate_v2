package net.maxsmr.address_sorter.initializers

import android.content.Context
import androidx.startup.Initializer
import net.maxsmr.address_sorter.R
import net.maxsmr.core.android.initBaseAppName
import net.maxsmr.core.android.initModuleCoreAndroidContext

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