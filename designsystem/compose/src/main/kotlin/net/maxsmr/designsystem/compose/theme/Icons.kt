package net.maxsmr.designsystem.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

object Icons {

    val Chevron: Painter
        @Composable
        get() = painterResource(id = net.maxsmr.designsystem.shared_res.R.drawable.ic_chevron)
}