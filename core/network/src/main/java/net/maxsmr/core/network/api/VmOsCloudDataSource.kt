package net.maxsmr.core.network.api

import net.maxsmr.core.network.api.vmoscloud.AutoTaskListRequest
import net.maxsmr.core.network.api.vmoscloud.AutoTaskListResponse
import net.maxsmr.core.network.api.vmoscloud.VmOsCloudDataService
import net.maxsmr.core.network.client.retrofit.VmOsCloudRetrofitClient

interface VmOsCloudDataSource {

    fun autoTaskList(request: AutoTaskListRequest): AutoTaskListResponse.Record
}

class VmOsCloudDataSourceImpl(
    private val retrofit: VmOsCloudRetrofitClient,
) : VmOsCloudDataSource {

    override fun autoTaskList(request: AutoTaskListRequest): AutoTaskListResponse.Record {
        return VmOsCloudDataService.instance(retrofit).autoTaskList(request).let {
            it.records.find {
                record -> record.taskType == request.taskType
            } ?: throw RuntimeException("Task with id ${request.taskType.id} not found")
        }
    }
}