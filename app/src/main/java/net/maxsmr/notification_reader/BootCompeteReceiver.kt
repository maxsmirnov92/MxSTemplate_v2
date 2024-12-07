package net.maxsmr.notification_reader

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.android.base.receivers.BaseBootCompleteReceiver
import net.maxsmr.commonutils.service.canStartForegroundService
import net.maxsmr.core.di.ApplicationScope
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import javax.inject.Inject

@AndroidEntryPoint
class BootCompeteReceiver : BaseBootCompleteReceiver() {

    override var lastBootCount: Int?
        get() = runBlocking { cacheRepo.getBootCount() }
        set(value) {
            value?.let {
                runBlocking {
                    cacheRepo.setBootCount(value)
                }
            }
        }

    @Inject
    lateinit var manager: NotificationReaderSyncManager

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun doAction(context: Context, intent: Intent) {
        if (canStartForegroundService(context)) {
            scope.launch {
                if (cacheRepo.shouldNotificationReaderRun()) {
                    manager.doStart(
                        context,
                        true
                    )
                }
            }
        }
        super.doAction(context, intent)
    }
}