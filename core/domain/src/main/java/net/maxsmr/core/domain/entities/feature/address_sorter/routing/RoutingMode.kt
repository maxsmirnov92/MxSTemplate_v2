package net.maxsmr.core.domain.entities.feature.address_sorter.routing;

/**
 * Способ расчёта distance/duration
 */
enum class RoutingMode {

    /**
     * автомобильный
     */
    DOUBLEGIS_DRIVING,

    /**
     * автомобильный маршрут, включающий полосы общественного транспорта
     */
    DOUBLEGIS_TAXI,

    /**
     * грузовой транспорт
     */
    DOUBLEGIS_TRUCK,

    /**
     * пешеходный маршрут
     */
    DOUBLEGIS_WALKING,

    /**
     * велосипедный маршрут
     */
    DOUBLEGIS_BICYCLE,

    /**
     * Косвенно через подсказчик
     */
    SUGGEST,

    /**
     * По прямой, без API
     */
    DIRECT,

    /**
     * Не пересчитывать
     */
    NO_CHANGE;

    val isApi get() = this !in listOf(DIRECT, NO_CHANGE)
}