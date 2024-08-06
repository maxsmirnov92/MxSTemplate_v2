package net.maxsmr.mobile_services.update

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import ru.rustore.sdk.appupdate.listener.InstallStateUpdateListener
import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability
import ru.rustore.sdk.core.presentation.ActivityResult

@RequiresApi(Build.VERSION_CODES.N)
class RuStoreInAppUpdateChecker(
    private val fragment: Fragment,
    private val callbacks: InAppUpdateChecker.Callbacks,
    private val immediateUpdatePriority: Int = 4,
) : InAppUpdateChecker {

    init {
        check(immediateUpdatePriority >= 1) {
            "immediateUpdatePriority must be >= 1"
        }
    }

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("RuStoreInAppUpdateChecker")

    private val context: Context by lazy { fragment.requireContext() }

    private val appUpdateManager: RuStoreAppUpdateManager by lazy {
        RuStoreAppUpdateManagerFactory.create(context)
    }

    private val appUpdateListener = InstallStateUpdateListener { state ->
        val status = state.installStatus
        if (lastAppUpdateType == AppUpdateType.FLEXIBLE) {
            when (status) {
                InstallStatus.DOWNLOADING -> {
                    if (status != lastAppUpdateStatus) {
                        callbacks.onUpdateDownloadStarted()
                    }
                    callbacks.onUpdateDownloading(state.bytesDownloaded, state.totalBytesToDownload)
                }

                InstallStatus.DOWNLOADED -> {
                    // снек или что-то ещё + завершение по кнопке
                    onAfterUpdateDownloaded()
                }

                InstallStatus.FAILED -> {
                    callbacks.onUpdateFailed()
                }

                else -> {

                }
            }
        }
        if (status in arrayOf(InstallStatus.DOWNLOADED, InstallStatus.FAILED)) {
            unregisterUpdateListener()
        }
        lastAppUpdateStatus = status
    }

    private var isRegistered: Boolean = false

    private var lastAppUpdateType: Int? = null

    private var lastAppUpdateStatus: Int? = null

    override fun doCheck() {
        appUpdateManager
            .getAppUpdateInfo()
            .addOnSuccessListener { appUpdateInfo ->
                logger.i("getAppUpdateInfo success, availableVersionCode: ${appUpdateInfo.availableVersionCode}, updateAvailability: ${appUpdateInfo.updateAvailability}")

                val activity = fragment.activity
                if (activity == null || activity.isFinishing) {
                    logger.w("Not attached to activity or it is finishing, not updating")
                    return@addOnSuccessListener
                }

                val availability = appUpdateInfo.updateAvailability

                // If an in-app update is already running, resume the update.
                val inProgress = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                if (inProgress) {
                    logger.i("App update is in progress")
                }
                val isAlreadyDownloaded = appUpdateInfo.installStatus == InstallStatus.DOWNLOADED
                if (inProgress
                        || isAlreadyDownloaded
                        || availability == UpdateAvailability.UPDATE_AVAILABLE
                ) {
                    val type = if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                            && !isAlreadyDownloaded
                            && (inProgress || appUpdateInfo.updatePriority >= immediateUpdatePriority)
                    ) {
                        AppUpdateType.IMMEDIATE
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        AppUpdateType.FLEXIBLE
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.SILENT)) {
                        AppUpdateType.SILENT
                    } else {
                        logger.e("No AppUpdateType allowed")
                        return@addOnSuccessListener
                    }
                    lastAppUpdateType = type

                    if (isAlreadyDownloaded) {
                        // в RuStore реализации для completeUpdate требуется тип и это не должен быть IMMEDIATE
                        logger.i("App update already downloaded")
                        onAfterUpdateDownloaded()
                        return@addOnSuccessListener
                    }

                    // листенер можно зарегистрировать перед запуском flow
                    registerUpdateListener()
                    appUpdateManager.startUpdateFlow(appUpdateInfo, AppUpdateOptions.Builder().build())
                        .addOnSuccessListener { resultCode ->
                            if (resultCode != Activity.RESULT_OK) {
                                val isCancelled = resultCode == Activity.RESULT_CANCELED
                                if (isCancelled) {
                                    logger.e("startUpdateFlow cancelled")
                                } else if (resultCode == ActivityResult.ACTIVITY_NOT_FOUND) {
                                    logger.e("startUpdateFlow not started: activity not found")
                                } else {
                                    logger.e("startUpdateFlow not started, unknown reason")
                                }
                                // Пользователь отказался от скачивания
                                callbacks.onUpdateDownloadNotStarted(isCancelled)
                            } else {
                                logger.e("startUpdateFlow success")
                            }
                        }
                        .addOnFailureListener {
                            logger.e("startUpdateFlow failed", it)
                            callbacks.onStartUpdateFlowFailed(it)
                        }
                }
            }
            .addOnFailureListener {
                logger.w("getAppUpdateInfo failed", it)
            }
    }

    private fun registerUpdateListener() {
        if (!isRegistered) {
            appUpdateManager.registerListener(appUpdateListener)
            isRegistered = true
        }
    }

    private fun unregisterUpdateListener() {
        if (isRegistered) {
            appUpdateManager.unregisterListener(appUpdateListener)
            isRegistered = false
        }
    }

    private fun onAfterUpdateDownloaded() {
        val type = lastAppUpdateType.takeIf {
            it != AppUpdateType.IMMEDIATE
        } ?: throw IllegalStateException("lastAppUpdateType is not valid: $lastAppUpdateType")
        callbacks.onUpdateDownloaded {
            appUpdateManager.completeUpdate(AppUpdateOptions.Builder().appUpdateType(type).build())
                .addOnSuccessListener {
                    logger.i("Update competed successfully")
                }.addOnFailureListener {
                    logger.e("Update complete failed")
                    // не рекомендуется отображать ошибку установки юзеру
                }
        }
    }
}