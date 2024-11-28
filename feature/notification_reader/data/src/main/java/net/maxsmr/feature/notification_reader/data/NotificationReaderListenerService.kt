package net.maxsmr.feature.notification_reader.data

import android.app.Activity
import android.app.Notification
import android.app.Notification.EXTRA_TITLE
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.SpannableStringBuilder
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.NotificationWrapper
import net.maxsmr.commonutils.isAtLeastOreo
import net.maxsmr.commonutils.isAtLeastTiramisu
import net.maxsmr.commonutils.isAtLeastUpsideDownCake
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.service.StartResult
import net.maxsmr.commonutils.service.createServicePendingIntent
import net.maxsmr.commonutils.service.isServiceRunning
import net.maxsmr.commonutils.service.start
import net.maxsmr.commonutils.service.startForegroundCompat
import net.maxsmr.commonutils.service.startNoCheck
import net.maxsmr.commonutils.service.stop
import net.maxsmr.commonutils.service.stopForegroundCompat
import net.maxsmr.commonutils.service.withMutabilityFlag
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.di.DI_NAME_FOREGROUND_SERVICE_ID_NOTIFICATION_READER
import net.maxsmr.core.di.DI_NAME_MAIN_ACTIVITY_CLASS
import net.maxsmr.core.di.EXTRA_CALLER_CLASS_NAME
import net.maxsmr.feature.demo.DemoChecker
import net.maxsmr.feature.demo.strategies.ToastDemoExpiredStrategy
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class NotificationReaderListenerService: NotificationListenerService() {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("NotificationReaderListenerService")

    private val context: Context by lazy { this }

    private val notificationWrapper: NotificationWrapper by lazy {
        NotificationWrapper(context) {
            permissionsHelper.hasPermissions(
                context,
                PermissionsHelper.withPostNotificationsByApiVersion(emptyList())
            )
        }
    }

    private val notificationChannel by lazy {
        NotificationWrapper.ChannelParams(
            "$packageName.downloads",
            getString(R.string.notification_reader_notification_channel_name)
        ) {
            if (isAtLeastOreo()) {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
    }

    private val mainActivityClass: Class<Activity>? by lazy {
        try {
            @Suppress("UNCHECKED_CAST")
            Class.forName(mainActivityClassName) as? Class<Activity>
        } catch (e: Exception) {
            logger.e(formatException(e, "Class.forName"))
            null
        }
    }

    private val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    @Inject
    lateinit var manager : NotificationReaderSyncManager

    @Inject
    lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var demoChecker: DemoChecker

    @Inject
    @Named(DI_NAME_MAIN_ACTIVITY_CLASS)
    lateinit var mainActivityClassName: String

    @JvmField
    @Inject
    @Named(DI_NAME_FOREGROUND_SERVICE_ID_NOTIFICATION_READER)
    var foregroundNotificationId: Int = -1

    override fun onCreate() {
        super.onCreate()
        logger.d("onCreate")
        startForegroundCompat(
            foregroundNotificationId,
            foregroundNotification(),
            if (isAtLeastUpsideDownCake()) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                null
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d("onStartCommand, intent: $intent, flags: $flags, startId: $startId")
        if (intent?.getBooleanExtra(EXTRA_STOP_SELF, false) == true) {
            // нотификация уберётся
            stopForegroundCompat(true)
            // для такого сервиса остановки не будет
            stopSelf()
            return Service.START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        logger.d("onListenerConnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val summaryText = sbn.notification.getSummaryText()
        logger.d("onNotificationPosted: $sbn, text: $summaryText")
        coroutineScope.launch {
            if (demoChecker.check(ToastDemoExpiredStrategy(this@NotificationReaderListenerService,
                        getString(R.string.notification_reader_notification_handle_error)))) {
                manager.onNewNotification(summaryText.toString(), sbn.packageName)
            } else {
                stopForegroundCompat(true)
                stopSelf()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        logger.d("onNotificationRemoved: $sbn")
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.d("onDestroy")
    }

    private fun foregroundNotification(): Notification {
        return notificationWrapper.create(foregroundNotificationId, notificationChannel) {
            setDefaults(Notification.DEFAULT_ALL)
            setSmallIcon(net.maxsmr.core.ui.R.drawable.ic_info)
            setContentTitle(getString(R.string.notification_reader_notification_text))
            setOnlyAlertOnce(true)
            setSound(null)
            setSilent(true)
            // Starting in Android 13 (API level 33),
            // users can dismiss the notification associated with a foreground service by default.
            setOngoing(true)
//            addAction(
//                net.maxsmr.core.ui.R.drawable.ic_stop,
//                getString(android.R.string.cancel),
//                createPendingIntent(
//                    context,
//                    nextNotificationRequestCode(),
//                    bundleOf(EXTRA_STOP_SELF to true)
//                )
//            )
            setContentIntent()
        }
    }

    private fun NotificationCompat.Builder.setContentIntent() {
        mainActivityClass?.let {
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, it)
                        .putExtra(EXTRA_CALLER_CLASS_NAME, NotificationReaderListenerService::class.java.canonicalName),
                    withMutabilityFlag(FLAG_UPDATE_CURRENT, false)
                )
            )
        }
    }

    private fun Notification.getSummaryText(): CharSequence {
        var titleText: CharSequence? = extras.getCharSequence(EXTRA_TITLE)
        val summary = SpannableStringBuilder()
        if (titleText == null) {
            titleText = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        }

        if (titleText != null) {
            summary.append(titleText)
        }
        val contentText: CharSequence? = extras.getCharSequence(Notification.EXTRA_TEXT)
        if (titleText != null && contentText != null) {
            summary.append(" • ")
        }
        if (contentText != null) {
            summary.append(contentText)
        }
        return summary
    }

    companion object {

        private const val EXTRA_STOP_SELF = "stop_self"

        @JvmStatic
        @JvmOverloads
        fun isRunning(context: Context = baseApplicationContext): Boolean {
            return isServiceRunning(context, NotificationReaderListenerService::class.java)
        }

        @JvmStatic
        @JvmOverloads
        fun start(context: Context = baseApplicationContext): StartResult {
            // не вызываем сразу startNoCheck, т.к. не нужно несколько onStartCommand
            return start(
                context,
                NotificationReaderListenerService::class.java,
                startForeground = true
            )
        }

        @JvmStatic
        @JvmOverloads
        fun stop(context: Context = baseApplicationContext, isForeground: Boolean = true): Boolean {
            // у NotificationListenerService ни один из stop'ов не сработает
            return if (isForeground) {
                if (isRunning(context)) {
                    startNoCheck(
                        context,
                        NotificationReaderListenerService::class.java,
                        bundleOf(EXTRA_STOP_SELF to true),
                        startForeground = false
                    )
                } else {
                    true
                }
            } else {
                stop(
                    context,
                    NotificationReaderListenerService::class.java,
                )
            }
        }

        private fun createPendingIntent(
            context: Context,
            requestCode: Int,
            bundle: Bundle,
        ): PendingIntent = createServicePendingIntent(
            context,
            NotificationReaderListenerService::class.java,
            requestCode,
            PendingIntent.FLAG_CANCEL_CURRENT,
            true,
            args = bundle
        )
    }
}