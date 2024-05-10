package net.maxsmr.core.domain.entities.feature.download

import java.io.Serializable

const val REG_EX_MD5_ALGORITHM = "^[a-f0-9]{32}$"

@kotlinx.serialization.Serializable
data class HashInfo(
    val algorithm: String,
    // лучше в виде строки, т.к. проще сравнивать с приходящим извне
    val hash: String,
) : Serializable