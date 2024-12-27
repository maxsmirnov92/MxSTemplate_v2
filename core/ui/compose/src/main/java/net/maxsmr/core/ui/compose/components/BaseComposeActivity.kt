package net.maxsmr.core.ui.compose.components

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.designsystem.compose.component.AppBackground

abstract class BaseComposeActivity: BaseActivity() {

    // TODO delegates

    @Composable
    abstract fun SetScreenContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppBackground {
                SetScreenContent()
            }
        }
    }
}