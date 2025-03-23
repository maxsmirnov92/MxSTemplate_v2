package net.maxsmr.core.network.api.vmoscloud

import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.retrofit.VmOsCloudRetrofitClient
import retrofit2.http.Body
import retrofit2.http.POST

internal interface VmOsCloudDataService {

    @Authorization
    @POST("/vcpcloud/api/padApi/autoTaskList")
    fun autoTaskList(@Body request: AutoTaskListRequest): AutoTaskListResponse

    companion object {

        @Volatile
        private var instance: VmOsCloudDataService? = null

        @JvmStatic
        fun instance(client: VmOsCloudRetrofitClient): VmOsCloudDataService =
            instance ?: synchronized(this) {
                instance ?: client.create(VmOsCloudDataService::class.java).also { instance = it }
            }
    }
}