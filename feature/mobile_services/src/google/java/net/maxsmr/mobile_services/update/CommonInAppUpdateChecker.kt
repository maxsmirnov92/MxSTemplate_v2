package net.maxsmr.mobile_services.update

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.common.IntentSenderForResultStarter
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.mobile_services.IMobileServicesAvailability

class CommonInAppUpdateChecker(
    private val availability: IMobileServicesAvailability,
    private val fragment: Fragment,
    private val updateRequestCode: Int,
    private val callbacks: InAppUpdateChecker.Callbacks,
    private val immediateUpdatePriority: Int = 4,
) : InAppUpdateChecker {

    init {
        check(immediateUpdatePriority >= 1) {
            "immediateUpdatePriority must be >= 1"
        }
    }

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("CommonInAppUpdateChecker")

    private val context: Context by lazy { fragment.requireContext() }

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    private val updateResultLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
//        if (result.data == null) return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            val isCancelled = result.resultCode == Activity.RESULT_CANCELED
            if (isCancelled) {
                logger.w("startUpdateFlowForResult cancelled")
            } else {
                logger.e("startUpdateFlowForResult not started, unknown reason")
            }
            callbacks.onUpdateDownloadNotStarted(isCancelled)
        } else {
            logger.i("startUpdateFlowForResult success")
        }
    }

    private val updateResultStarter =
        IntentSenderForResultStarter { intent, _, fillInIntent, flagsMask, flagsValues, _, _ ->
            val request = IntentSenderRequest.Builder(intent)
                .setFillInIntent(fillInIntent)
                .setFlags(flagsValues, flagsMask)
                .build()
            updateResultLauncher.launch(request)
        }

    private val appUpdateListener = InstallStateUpdatedListener { state ->
        val status = state.installStatus()
        if (lastAppUpdateType == AppUpdateType.FLEXIBLE) {
            when (status) {
                InstallStatus.DOWNLOADING -> {
                    if (status != lastAppUpdateStatus) {
                        callbacks.onUpdateDownloadStarted()
                    }
                    callbacks.onUpdateDownloading(state.bytesDownloaded(), state.totalBytesToDownload())
                }

                InstallStatus.DOWNLOADED -> {
                    // снек или что-то ещё + завершение по кнопке
                    onAfterUpdateDownloaded()
                }

                InstallStatus.FAILED -> {
                    callbacks.onUpdateFailed()
                }

                InstallStatus.CANCELED -> {
                    callbacks.onUpdateCancelled()
                }

                else -> {

                }
            }
        }
        if (status in arrayOf(InstallStatus.DOWNLOADED, InstallStatus.FAILED, InstallStatus.CANCELED)) {
            unregisterUpdateListener()
        }
        lastAppUpdateStatus = status
    }

    private var isChecking: Boolean = false

    private var isRegistered: Boolean = false

    private var lastAppUpdateType: Int? = null

    private var lastAppUpdateStatus: Int? = null

    @Synchronized
    override fun doCheck() {
        if (!availability.isGooglePlayServicesAvailable) return
        if (isChecking) return

        isChecking = true

        lastAppUpdateType = null
        lastAppUpdateStatus = null

        logger.i("Checking updates by CommonInAppUpdateChecker...")

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                logger.i("getAppUpdateInfo success, availableVersionCode: ${appUpdateInfo.availableVersionCode()}, updateAvailability: ${appUpdateInfo.updateAvailability()}")

                val activity = fragment.activity
                if (activity == null || activity.isFinishing) {
                    logger.w("Not attached to activity or it is finishing, not updating")
                    return@addOnSuccessListener
                }

                callbacks.onUpdateCheckSuccess()

                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    // в таком статусе установки isUpdateTypeAllowed будет только IMMEDIATE
                    // независимо от исходного - нужно завершить вызовом completeUpdate()
                    logger.i("App update already downloaded")
                    onAfterUpdateDownloaded()
                    return@addOnSuccessListener
                }
                val availability = appUpdateInfo.updateAvailability()
                // If an in-app update is already running, resume the update.
                val inProgress = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                if (inProgress || availability == UpdateAvailability.UPDATE_AVAILABLE) {
                    val type = if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                            && (inProgress || appUpdateInfo.updatePriority() >= immediateUpdatePriority)
                    ) {
                        AppUpdateType.IMMEDIATE
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        AppUpdateType.FLEXIBLE
                    } else {
                        logger.e("No AppUpdateType allowed")
                        return@addOnSuccessListener
                    }
                    lastAppUpdateType = type
                    registerUpdateListener()
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            updateResultStarter,
                            AppUpdateOptions.newBuilder(type).build(),
                            updateRequestCode
                        )
                    } catch (exception: IntentSender.SendIntentException) {
                        logger.e("startUpdateFlowForResult failed", exception)
                        callbacks.onStartUpdateFlowFailed(exception)
                    }
                }
            }
            .addOnFailureListener {
                logger.w("getAppUpdateInfo failed", it)
            }
            .addOnCompleteListener(fragment.requireActivity()) {
                logger.i("getAppUpdateInfo complete")
                isChecking = false
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
        callbacks.onUpdateDownloaded {
            appUpdateManager.completeUpdate().addOnSuccessListener {
                logger.i("Update competed successfully")
            }.addOnCanceledListener {
                logger.w("Update complete cancelled")
            }.addOnFailureListener {
                logger.e("Update complete failed")
                // не рекомендуется отображать ошибку установки юзеру
            }
        }
    }
}