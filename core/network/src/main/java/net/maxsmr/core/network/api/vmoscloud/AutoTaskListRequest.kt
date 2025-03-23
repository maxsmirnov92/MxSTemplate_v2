package net.maxsmr.core.network.api.vmoscloud

import kotlinx.serialization.Serializable

@Serializable
data class AutoTaskListRequest(
    @Serializable(TaskType.Serializer::class)
    val taskType: TaskType,
    val taskIds: List<Long> = emptyList(),
    val page: Int = 1,
    val rows: Int = 10,
)