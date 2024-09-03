package net.maxsmr.feature.address_sorter.data

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import net.maxsmr.commonutils.getViewIntent
import net.maxsmr.core.domain.entities.feature.address_sorter.Address

private const val ROUTE_DOUBLEGIS = "https://2gis.ru/directions/points/%s"

private const val ROUTE_YANDEX_NAVI = "yandexnavi://build_route_on_map?%s"

fun getDoubleGisRouteIntent(locations: Set<Address.Location>, fromCurrent: Boolean): Intent? {
    if (locations.isEmpty()) return null
    val points = locations.map { "${it.longitude},${it.latitude}" }
    val result = TextUtils.join("|", points)
    return getViewIntent(
        Uri.parse(
            String.format(
                ROUTE_DOUBLEGIS,
                if (fromCurrent) {
                    "|$result"
                } else {
                    result
                }
            )
        )
    )
}

fun getYandexNaviRouteIntent(locations: Set<Address.Location>, fromCurrent: Boolean): Intent? {
    if (locations.isEmpty()) return null
    val result = StringBuilder()
    var viaIndex = 0
    locations.forEachIndexed { index, item ->
        if (result.isNotEmpty()) {
            result.append("&")
        }
        when {
            index == 0 && !fromCurrent -> {
                result.append("lat_from=${item.latitude}&")
                result.append("lon_from=${item.longitude}")
            }
            index < locations.size - 1 -> {
                result.append("lat_via_$viaIndex=${item.latitude}&")
                result.append("lon_via_$viaIndex=${item.longitude}")
                viaIndex++
            }
            else -> {
                result.append("lat_to=${item.latitude}&")
                result.append("lon_to=${item.longitude}")
            }
        }
    }
    return getViewIntent(
        Uri.parse(
            String.format(
                ROUTE_YANDEX_NAVI,
                result
            )
        )
    )
}