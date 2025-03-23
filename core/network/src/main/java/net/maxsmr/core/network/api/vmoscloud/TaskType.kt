package net.maxsmr.core.network.api.vmoscloud

import net.maxsmr.core.utils.IntId
import net.maxsmr.core.utils.kotlinx.serialization.serializers.IntIdEnumSerializer

enum class TaskType(override val id: Int): IntId {

    LOGIN(1),
    EDIT(2),
    SEARCH_VIDEO(3),
    VIEW_VIDEO(4),
    PUBLISH_VIDEO(5),
    PUBLISH_PHOTO(6);

    object Serializer : IntIdEnumSerializer<TaskType>(
        TaskType::class,
        TaskType.entries.toTypedArray(),
        LOGIN
    )
}