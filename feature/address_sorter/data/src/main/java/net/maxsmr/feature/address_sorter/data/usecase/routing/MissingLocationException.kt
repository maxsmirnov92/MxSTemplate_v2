package net.maxsmr.feature.address_sorter.data.usecase.routing

/**
 * @param ids идентификаторы адресов, для которых отсутствует геолокация
 */
class MissingLocationException(val ids: List<Long>) : RuntimeException() {

    init {
        if (ids.isEmpty()) {
            throw IllegalArgumentException("ids is empty")
        }
    }
}