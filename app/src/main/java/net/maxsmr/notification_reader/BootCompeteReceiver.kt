package net.maxsmr.notification_reader

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.maxsmr.core.android.base.receivers.BaseBootCompleteReceiver
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.di.ApplicationScope
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import javax.inject.Inject

@AndroidEntryPoint
class BootCompeteReceiver : BaseBootCompleteReceiver() {

    override var lastBootCount: Int?
        get() = null
        set(value) {}

    @Inject
    lateinit var manager: NotificationReaderSyncManager

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun doAction(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || Settings.canDrawOverlays(baseApplicationContext)
        ) {
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