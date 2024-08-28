package net.maxsmr.core.domain.entities.feature.address_sorter.routing;

enum class RoutingMode {

    /**
     * автомобильный
     */
    DRIVING,

    /**
     * автомобильный маршрут, включающий полосы общественного транспорта
     */
    TAXI,

    /**
     * грузовой транспорт
     */
    TRUCK,

    /**
     * пешеходный маршрут
     */
    WALKING,

    /**
     * велосипедный маршрут
     */
    BICYCLE,

    /**
     * по прямой, без API
     */
    DIRECT;

    val isApi get() = this != DIRECT
}