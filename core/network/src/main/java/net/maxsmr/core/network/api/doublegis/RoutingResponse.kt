package net.maxsmr.core.network.api.doublegis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.StringId
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.network.retrofit.converters.api.BaseDoubleGisRoutingResponse
import net.maxsmr.core.utils.kotlinx.serialization.serializers.StringIdEnumSerializer

/**
 * @param routes Массив найденных маршрутов. Для каждой последовательной пары точек находится ровно один маршрут.
 * Таким образом, в случае P-P здесь будет один маршрут, а в общем случае с N точек будет N-1 маршрут
 */
@Serializable
class RoutingResponse(
    val routes: List<Route>,
) : BaseDoubleGisRoutingResponse() {

    fun asDomain(toAddressIdFunc: (Long) -> Long): Map<Long, Pair<AddressRoute?, Route.Status>> {
        val result = mutableMapOf<Long, Pair<AddressRoute?, Route.Status>>()
        routes.forEach { r ->
            val id = toAddressIdFunc(r.targetId)
            if (id < 0) return@forEach
            val route = if (r.distance >= 0) {
                AddressRoute(
                    id,
                    r.distance.toFloat(),
                    r.duration.takeIf { it >= 0 },
                )
            } else {
                null
            }
            result[id] = Pair(
                route,
                if (r.status == Route.Status.OK && route == null) {
                    // вручную меняем статус на FAIL, если был возвращён отрицательный distance
                    Route.Status.FAIL
                } else {
                    r.status
                }
            )
        }
        return result
    }

    /**
     * @param status Статус обработки запроса
     * @param sourceId Индекс точки отправления (из массива points)
     * @param targetId Индекс точки прибытия (из массива points)
     * @param distance Длина маршрута в метрах
     * @param duration Время в пути в секундах
     */
    @Serializable
    class Route(
        @Serializable(Status.Serializer::class)
        val status: Status,
        @SerialName("source_id")
        val sourceId: Long,
        @SerialName("target_id")
        val targetId: Long,
        val distance: Long,
        val duration: Long,
    ) {

        enum class Status(override val id: String) : StringId {

            /**
             * маршрут построен успешно
             */
            OK("OK"),

            /**
             * неизвестная ошибка построения маршрута
             */
            FAIL("FAIL"),

            /**
             * точки попали в зону исключения
             */
            POINT_EXCLUDED("POINT_EXCLUDED"),

            /**
             * маршрут не удалось построить на текущих данных дорожного графа
             */
            ROUTE_NOT_FOUND("ROUTE_NOT_FOUND"),

            /**
             * маршрут между точками на дорожном графе не существует
             */
            ROUTE_DOES_NOT_EXISTS("ROUTE_DOES_NOT_EXISTS"),

            /**
             * не удалось притянуть точки к дорожному графу: одна из точек маршрута удалена от дорожного графа более чем на 10 км
             */
            ATTRACT_FAIL("ATTRACT_FAIL");

            object Serializer : StringIdEnumSerializer<Status>(
                Status::class,
                entries.toTypedArray(),
                OK
            )
        }
    }
}