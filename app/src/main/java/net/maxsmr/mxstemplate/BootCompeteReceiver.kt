package net.maxsmr.mxstemplate

import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.receivers.BaseBootCompleteReceiver
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import javax.inject.Inject

@AndroidEntryPoint
class BootCompeteReceiver : BaseBootCompleteReceiver() {

    override var lastBootCount: Int?
        get() = null
        set(value) {}

    @Inject
    lateinit var manager: NotificationReaderSyncManager

    override fun doAction(context: Context, intent: Intent) {
        manager.doStart(context, Settings.canDrawOverlays(context))
        super.doAction(context, intent)
    }
}