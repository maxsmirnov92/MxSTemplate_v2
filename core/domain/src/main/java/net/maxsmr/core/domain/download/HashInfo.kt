package net.maxsmr.core.domain.download

import java.io.Serializable

@kotlinx.serialization.Serializable
data class HashInfo(
    val algorithm: String,
    // лучше в виде строки, т.к. проще сравнивать с приходящим извне
    val hash: String,
) : Serializable