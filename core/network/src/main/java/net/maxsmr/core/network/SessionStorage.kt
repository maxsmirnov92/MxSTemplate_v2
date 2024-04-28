package net.maxsmr.core.network


interface SessionStorage {

    /**
     * Переменная для сохранения сессии при стандартной авторизации (вход по логину и паролю)
     */
    var session: String?

    fun has(): Boolean = !session.isNullOrEmpty()

    fun clear()
}