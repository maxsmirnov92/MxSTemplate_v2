package net.maxsmr.core.android.screen

import androidx.lifecycle.SavedStateHandle

const val KEY_SCREEN_PARAMS = "params"

inline fun <reified T : ScreenParams> SavedStateHandle.extractScreenParamsOrNull() = runCatching {
    extractScreenParams<T>()
}.getOrNull()

inline fun <reified T : ScreenParams> SavedStateHandle.extractScreenParams() = requireNotNull(get<T>(KEY_SCREEN_PARAMS)) {
    "Screen expects ${T::class} in arguments but it is missing"
}
