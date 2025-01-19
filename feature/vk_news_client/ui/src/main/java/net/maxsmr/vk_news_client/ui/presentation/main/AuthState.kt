package net.maxsmr.vk_news_client.ui.presentation.main

sealed class AuthState {

    data class Authorized(val token: String): AuthState()

    data object NotAuthorized: AuthState()

    data class AuthFailed(val description: String): AuthState()
}