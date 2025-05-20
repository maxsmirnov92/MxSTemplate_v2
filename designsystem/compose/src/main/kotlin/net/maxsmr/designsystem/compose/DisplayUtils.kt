package net.maxsmr.designsystem.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

@ReadOnlyComposable
@Composable
fun Dp.toPx() = with(LocalDensity.current) { this@toPx.toPx() }

fun Dp.toPx(context: Context): Float {
    val density = context.resources.displayMetrics.density
    return this.value * density
}

@ReadOnlyComposable
@Composable
fun Float.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

@ReadOnlyComposable
@Composable
fun Int.toDp() = with(LocalDensity.current) { this@toDp.toDp() }

@ReadOnlyComposable
@Composable
fun TextUnit.toDp() = with(LocalDensity.current) { this@toDp.toDp() }