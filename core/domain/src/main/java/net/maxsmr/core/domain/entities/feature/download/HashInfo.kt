package net.maxsmr.core.domain.entities.feature.download

import java.io.Serializable

@kotlinx.serialization.Serializable
class HashInfo(
    val algorithm: String,
    // лучше в виде строки, т.к. проще сравнивать с приходящим извне
    val hash: String,
) : Serializable {

    val isEmpty get() = algorithm.isEmpty() || hash.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HashInfo) return false

        // data class не подходит, т.к. надо без учёта регистра
        if (!algorithm.equals(other.algorithm, true)) return false
        return hash.equals(other.hash, true)
    }

    override fun hashCode(): Int {
        var result = algorithm.lowercase().hashCode()
        result = 31 * result + hash.lowercase().hashCode()
        return result
    }

    override fun toString(): String {
        return "HashInfo(algorithm='$algorithm', hash='$hash')"
    }

    companion object {

        // FIXME все работают медленно на больших файлах

    }
}