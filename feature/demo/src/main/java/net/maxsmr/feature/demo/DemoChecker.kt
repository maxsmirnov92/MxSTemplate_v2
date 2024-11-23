package net.maxsmr.feature.demo

import android.os.Handler
import android.os.Looper
import net.maxsmr.core.di.DI_NAME_DEMO_PERIOD
import net.maxsmr.core.di.DI_NAME_IS_DEMO_BUILD
import net.maxsmr.feature.demo.strategies.IDemoExpiredStrategy
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class DemoChecker @Inject constructor(
    @Named(DI_NAME_IS_DEMO_BUILD)
    private val isDemoBuild: Boolean,
    @Named(DI_NAME_DEMO_PERIOD)
    private val demoPeriod: Long,
    private val cacheRepo: CacheDataStoreRepository,
) {

    init {
        require(demoPeriod >= 0) { "Incorrect demoPeriod: $demoPeriod" }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val startTime = System.currentTimeMillis()

    suspend fun check(strategy: IDemoExpiredStrategy?): Boolean {
        if (!isDemoBuild || demoPeriod == 0L) return true
        val currentTime = System.currentTimeMillis()
        if (currentTime - startTime >= demoPeriod) {
            cacheRepo.setDemoPeriodExpired()
        }
        if (cacheRepo.isDemoPeriodExpired()) {
            handler.post {
                strategy?.doAction()
            }
            return false
        }
        return true
    }
}