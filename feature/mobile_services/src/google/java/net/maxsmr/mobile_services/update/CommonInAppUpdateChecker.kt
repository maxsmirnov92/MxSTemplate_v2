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
    private val callbacks: Callbacks,
) : InAppUpdateChecker {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("AppUpdateChecker")

    private val context: Context by lazy { fragment.requireContext() }

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }

    private val updateResultLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
//        if (result.data == null) return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            callbacks.onUpdateDownloadNotStarted(result.resultCode == Activity.RESULT_CANCELED)
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
        lastAppUpdateStatus = status
    }

    private var isRegistered: Boolean = false

    private var lastAppUpdateType: Int? = null

    private var lastAppUpdateStatus: Int? = null

    override fun doCheck() {
        if (!availability.isGooglePlayServicesAvailable) return
        registerUpdateListener()
        doCheckUpdate()
    }

    override fun dispose() {
        unregisterUpdateListener()
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

    private fun doCheckUpdate() {
        lastAppUpdateType = null
        lastAppUpdateStatus = null

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            logger.i("Check success, availableVersionCode: ${appUpdateInfo.availableVersionCode()}, updateAvailability: ${appUpdateInfo.updateAvailability()}")
            if (fragment.requireActivity().isFinishing) {
                logger.w("Activity is finishing, not updating")
                return@addOnSuccessListener
            }
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                // в таком статусе установки isUpdateTypeAllowed будет только IMMEDIATE
                // независимо от исходного - нужно завершить вызовом completeUpdate()
                logger.i("App update already downloaded")
                onAfterUpdateDownloaded()
                return@addOnSuccessListener
            }
            val availability = appUpdateInfo.updateAvailability()
            // If an in-app update is already running, resume the update.
            val inProgress =
                appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            if (inProgress || availability == UpdateAvailability.UPDATE_AVAILABLE) {
                val type = if (inProgress ||
                        (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) && appUpdateInfo.updatePriority() >= 4)
                ) {
                    AppUpdateType.IMMEDIATE
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    AppUpdateType.FLEXIBLE
                } else {
                    logger.e("No AppUpdateType allowed")
                    return@addOnSuccessListener
                }
                lastAppUpdateType = type
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateResultStarter,
                        AppUpdateOptions.newBuilder(type).build(),
                        updateRequestCode
                    )
                } catch (exception: IntentSender.SendIntentException) {
                    callbacks.onStartUpdateFlowException(exception)
                }
            }
        }
        appUpdateInfoTask.addOnFailureListener {
            logger.w("Check failed", it)
        }
        appUpdateInfoTask.addOnCompleteListener {

        }
        appUpdateInfoTask.addOnCanceledListener {

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
            }
        }
    }

    interface Callbacks {

        fun onUpdateDownloadNotStarted(isCancelled: Boolean)

        fun onUpdateDownloadStarted()

        fun onUpdateDownloaded(completeAction: () -> Unit)

        fun onUpdateFailed()

        fun onUpdateCancelled()

        fun onStartUpdateFlowException(exception: IntentSender.SendIntentException)
    }
}