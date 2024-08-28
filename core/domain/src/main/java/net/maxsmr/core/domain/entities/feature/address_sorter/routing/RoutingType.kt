package net.maxsmr.core.domain.entities.feature.address_sorter.routing

enum class RoutingType {
    /**
     * автомобильный маршрут по текущим пробкам
     */
    JAM,

    /**
     * автомобильный маршрут без учёта пробок
     */
    SHORTEST,
}