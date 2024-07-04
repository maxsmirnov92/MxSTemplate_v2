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
    private val callbacks: Callbacks
): InAppUpdateChecker {

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
            if (lastAppUpdateType == AppUpdateType.IMMEDIATE) {
                // при таком типе, если юзер закрыл окно или что-то пошло не так,
                // заново проверяем доступность апдейта;
                // но результат может не прийти вовсе
                doCheckUpdate()
            }
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
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                if (lastAppUpdateType == AppUpdateType.FLEXIBLE) {
                    callbacks.onUpdateDownloading()
                }
            }

            InstallStatus.DOWNLOADED -> {
                onStopChecking()
                // снек или что-то ещё + завершение по кнопке
                if (lastAppUpdateType == AppUpdateType.FLEXIBLE) {
                    onAfterUpdateDownloaded()
                }
            }

            else -> {

            }
        }
    }

    private var isChecking: Boolean = false

    private var lastAppUpdateType: Int? = null

    override fun onStartChecking() {
        if (isChecking || !availability.isGooglePlayServicesAvailable) return
        isChecking = true
        doCheckUpdate()
        appUpdateManager.registerListener(appUpdateListener)
    }

    override fun onStopChecking() {
        if (!isChecking) return
        appUpdateManager.unregisterListener(appUpdateListener)
        isChecking = false
    }

    private fun doCheckUpdate() {
        lastAppUpdateType = null
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            logger.i("Check success, availableVersionCode: ${appUpdateInfo.availableVersionCode()}, updateAvailability: ${appUpdateInfo.updateAvailability()}")
            if (fragment.requireActivity().isFinishing) {
                logger.w("Activity is finishing, not updating")
                return@addOnSuccessListener
            }
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                logger.i("App update already downloaded")
                onAfterUpdateDownloaded()
                return@addOnSuccessListener
            }
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val type = if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    AppUpdateType.FLEXIBLE
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    AppUpdateType.IMMEDIATE
                } else {
                    logger.e("No allowed update types")
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

        fun onUpdateDownloading()

        fun onUpdateDownloaded(completeAction: () -> Unit)

        fun onStartUpdateFlowException(exception: IntentSender.SendIntentException)
    }
}