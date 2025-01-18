package net.maxsmr.core.ui.compose.navigation

fun getRouteWithArgs(
    baseRoute: String,
    vararg args: String
): String {
    val result = StringBuilder(baseRoute)
    args.forEach { arg ->
        arg.takeIf { it.isNotEmpty() }?.let {
            result.append("/$it")
        }
    }
    return result.toString()
}