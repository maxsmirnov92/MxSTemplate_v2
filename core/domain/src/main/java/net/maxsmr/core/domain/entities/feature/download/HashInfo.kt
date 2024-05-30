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

        const val ALGORITHM_SHA1 = "SHA-1"
        // работает медленно!
        const val ALGORITHM_MD5 = "MD5"
        const val ALGORITHM_CRC32 = "CRC32"

        const val REG_EX_ALGORITHM_MD5 = "^[a-fA-F0-9]{32}$"
        const val REG_EX_ALGORITHM_SHA1 = "^[a-fA-F0-9]{40}$"
        const val REG_EX_ALGORITHM_CRC32 = "^[a-fA-F0-9]{8}$"
    }
}