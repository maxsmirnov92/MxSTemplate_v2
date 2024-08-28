package net.maxsmr.core.domain.entities.feature.address_sorter.routing

import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.address_sorter.Address

/**
 * @param id идентификатор исходного [Address],
 * до которого измеряется расстояние
 */
@Serializable
class AddressRoute(
    val id: Long,
    val distance: Long,
    val duration: Long,
)