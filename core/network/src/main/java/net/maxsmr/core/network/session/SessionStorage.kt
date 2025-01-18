package net.maxsmr.core.network.session

interface SessionStorage {

    val session: String?

    fun has(): Boolean = !session.isNullOrEmpty()
}