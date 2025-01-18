package net.maxsmr.mxstemplate.initializers

import android.content.Context
import androidx.startup.Initializer
import kotlinx.serialization.json.Json
import net.maxsmr.core.di.BaseJson
import net.maxsmr.mxstemplate.di.InitializerEntryPoint
import net.maxsmr.core.ui.compose.navigation.NavTypeHolder
import javax.inject.Inject

class NavTypeHolderInitializer : Initializer<Unit> {

    @Inject
    @BaseJson
    lateinit var json: Json

    override fun create(context: Context) {
        InitializerEntryPoint.resolve(context).inject(this)
        NavTypeHolder.json = json
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
}