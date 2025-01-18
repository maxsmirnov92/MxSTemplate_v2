package net.maxsmr.core.network.session


interface WritableSessionStorage: SessionStorage {

    /**
     * Переменная для сохранения сессии при стандартной авторизации (вход по логину и паролю)
     */
    override var session: String?

    fun clear()
}