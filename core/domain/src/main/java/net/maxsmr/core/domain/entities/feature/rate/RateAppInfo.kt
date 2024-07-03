package net.maxsmr.core.domain.entities.feature.rate

import java.io.Serializable
import java.util.concurrent.TimeUnit

@kotlinx.serialization.Serializable
data class RateAppInfo(
    val isRated: Boolean,
    val notAskAgain: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
): Serializable