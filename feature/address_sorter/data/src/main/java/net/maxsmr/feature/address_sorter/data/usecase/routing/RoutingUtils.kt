package net.maxsmr.feature.address_sorter.data.usecase.routing

import android.graphics.PointF
import net.maxsmr.commonutils.location.distance
import net.maxsmr.core.domain.entities.feature.address_sorter.Address

fun getDirectDistanceByLocation(
    first: Address.Location,
    second: Address.Location,
): Float? {
    fun Address.Location.toPointF() = PointF(latitude, longitude)
    val locationPoint = first.toPointF()
    // distance2 выдаёт тот же результат, но считает по-другому и учитывает высоту
    return distance(second.toPointF(), locationPoint).takeIf { it > 0 }
}