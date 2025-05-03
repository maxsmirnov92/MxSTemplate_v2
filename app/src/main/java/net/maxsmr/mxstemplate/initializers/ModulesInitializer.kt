package net.maxsmr.mxstemplate.initializers

import android.content.Context
import androidx.startup.Initializer
import kotlinx.serialization.json.Json
import net.maxsmr.core.android.initBaseAppName
import net.maxsmr.core.android.initBaseJson
import net.maxsmr.core.di.BaseJson
import net.maxsmr.mxstemplate.R
import net.maxsmr.mxstemplate.di.InitializerEntryPoint
import javax.inject.Inject

class ModulesInitializer : Initializer<Unit> {

    @Inject
    @BaseJson
    lateinit var json: Json

    override fun create(context: Context) {
        InitializerEntryPoint.resolve(context).inject(this)
        // Инициализация модулей
        initBaseAppName(context.getString(R.string.app_name))
        initBaseJson(json)
        // #####################
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        TimberInitializer::class.java,
    )
}