package net.maxsmr.vk_news_client.ui.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import com.vk.id.logout.VKIDLogoutCallback
import com.vk.id.logout.VKIDLogoutFail
import com.vk.id.refresh.VKIDRefreshTokenCallback
import com.vk.id.refresh.VKIDRefreshTokenFail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.di.SessionStorageType
import net.maxsmr.core.network.exceptions.ApiException.Companion.isApiException
import net.maxsmr.core.network.exceptions.handler.CombinedCallExceptionHandler
import net.maxsmr.core.network.session.SessionStorage
import net.maxsmr.feature.vk_news_client.ui.R
import net.maxsmr.vk_news_client.data.VkApiErrorCodes
import net.maxsmr.vk_news_client.ui.presentation.login.AUTH_SCOPES
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @net.maxsmr.core.di.SessionStorage(SessionStorageType.VK)
    private val vkSessionStorage: SessionStorage,
    private val exceptionHandler: CombinedCallExceptionHandler,
    state: SavedStateHandle
) : BaseViewModel(state) {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val isAuthorized get() = vkSessionStorage.has()

    override fun onInitialized() {
        super.onInitialized()
        val token = VKID.instance.accessToken
        _authState.value = if (token != null) {
            AuthState.Authorized(token.token)
        } else {
            AuthState.NotAuthorized
        }
        viewModelScope.launch {
            exceptionHandler.exceptionsFlow.collect {
                if (it.isApiException(VkApiErrorCodes.ACCESS_TOKEN_EXPIRED.code)) {
                    showToast(TextMessage(R.string.vk_news_client_access_token_expired))
                    VKID.instance.refreshToken(callback = object: VKIDRefreshTokenCallback {
                        override fun onSuccess(token: AccessToken) {
                            showToast(TextMessage(R.string.vk_news_client_access_token_refreshed))
                        }

                        override fun onFail(fail: VKIDRefreshTokenFail) {
                            logout()
                        }
                    })
                }
            }
        }
    }

    fun onAuthSuccess(accessToken: AccessToken) {
        _authState.value = AuthState.Authorized(accessToken.token)
    }

    fun onAuthFailed(fail: VKIDAuthFail) {
        if (fail !is VKIDAuthFail.Canceled) {
            _authState.value = AuthState.AuthFailed(fail.description)
        } else {
            showSnackbar(TextMessage(R.string.vk_news_client_auth_cancelled))
        }
    }

    fun authorize() {
        if (isAuthorized) return
        viewModelScope.launch {
            VKID.instance.authorize(
                object : VKIDAuthCallback {
                    override fun onAuth(accessToken: AccessToken) {
                        onAuthSuccess(accessToken)
                    }

                    override fun onFail(fail: VKIDAuthFail) {
                        onAuthFailed(fail)
                    }
                },
                params = VKIDAuthParams {
                    scopes = AUTH_SCOPES
                })
        }
    }

    fun logout() {
        if (!isAuthorized) {
            _authState.value = AuthState.NotAuthorized
        } else {
            viewModelScope.launch {
                VKID.instance.logout(object : VKIDLogoutCallback {
                    override fun onSuccess() {
                        _authState.value = AuthState.NotAuthorized
                    }

                    override fun onFail(fail: VKIDLogoutFail) {
                        showSnackbar(TextMessage(R.string.vk_news_client_logout_failed_format, fail.description))
                    }
                })
            }
        }
    }
}