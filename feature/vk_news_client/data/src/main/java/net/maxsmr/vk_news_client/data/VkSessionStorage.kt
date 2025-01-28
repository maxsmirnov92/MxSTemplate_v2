package net.maxsmr.vk_news_client.data

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.refresh.VKIDRefreshTokenCallback
import com.vk.id.refresh.VKIDRefreshTokenFail
import com.vk.id.refresh.VKIDRefreshTokenParams
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import net.maxsmr.core.network.session.SessionStorage

class VkSessionStorage : SessionStorage {

    val refreshTokenEvents: SharedFlow<RefreshTokenEvent> by lazy {
        _refreshTokenEvents.asSharedFlow()
    }

    val successRefreshTokenEvents: Flow<RefreshTokenEvent.Success> by lazy { refreshTokenEvents.mapNotNull { it as? RefreshTokenEvent.Success } }

    val failRefreshTokenEvents: Flow<RefreshTokenEvent.Fail> by lazy { refreshTokenEvents.mapNotNull { it as? RefreshTokenEvent.Fail } }

    private val _refreshTokenEvents = MutableSharedFlow<RefreshTokenEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val session: String?
        get() = VKID.instance.accessToken?.token

    suspend fun refreshToken(
        callback: VKIDRefreshTokenCallback,
        params: VKIDRefreshTokenParams = VKIDRefreshTokenParams {},
    ) {
        VKID.instance.refreshToken(object : VKIDRefreshTokenCallback {

            override fun onSuccess(token: AccessToken) {
                callback.onSuccess(token)
                _refreshTokenEvents.tryEmit(RefreshTokenEvent.Success(token))
            }

            override fun onFail(fail: VKIDRefreshTokenFail) {
                callback.onFail(fail)
                _refreshTokenEvents.tryEmit(RefreshTokenEvent.Fail(fail))
            }
        }, params)
    }

    sealed interface RefreshTokenEvent {

        class Success(val token: AccessToken) : RefreshTokenEvent

        class Fail(val fail: VKIDRefreshTokenFail) : RefreshTokenEvent
    }
}