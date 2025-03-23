package net.maxsmr.core.network.api.vmoscloud

import kotlinx.serialization.Serializable

@Serializable
data class AutoTaskListResponse(
    val records: List<Record>,
    val total: Int,
    val size: Int,
    val current: Int,
    val pages: Int,
) {

    @Serializable
    data class Record(
        val taskId: Int,
        val userId: Int,
        val equipmentId: Int,
        val padCode: String,
        val padName: String,
        @Serializable(TaskType.Serializer::class)
        val taskType: TaskType,
    )
}