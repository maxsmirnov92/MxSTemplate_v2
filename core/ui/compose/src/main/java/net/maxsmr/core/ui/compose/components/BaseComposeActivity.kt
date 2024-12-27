package net.maxsmr.core.ui.compose.components

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.designsystem.compose.component.AppBackground
import net.maxsmr.permissionchecker.PermissionsHelper

abstract class BaseComposeActivity: BaseActivity() {

    abstract val permissionsHelper: PermissionsHelper

    private val delegates: List<IComponentDelegate<*>> by lazy {
        if (canUseComponentDelegates) {
            createActivityDelegates()
        } else {
            listOf()
        }
    }

    @Composable
    abstract fun SetScreenContent()

    // TODO compose реализация AlertDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppBackground {
                SetScreenContent()
            }
        }
        delegates.forEach {
            it.onCreated()
        }
    }

    override fun onResume() {
        super.onResume()
        delegates.forEach {
            it.onResumed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        delegates.forEach {
            it.onDestroyed()
        }
    }

    protected open fun createActivityDelegates(): List<IComponentDelegate<*>> = listOf()
}