package net.maxsmr.core.ui.components.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.permissionchecker.PermissionsCallbacks
import net.maxsmr.permissionchecker.PermissionsHelper

open class BaseActivity: AppCompatActivity() {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("BaseActivity")

    /**
     * При первом запросе разрешений из PermissionsHelper запоминаем [PermissionsHelper.ResultListener] по данному коду,
     * чтобы в дальнейшем отчитаться о результате из onRequestPermissionsResult или onActivityResult;
     */
    private val permissionResultListeners = mutableMapOf<Int, PermissionsHelper.ResultListener?>()

    /**
     * Разрешения запрашивать только с хостовой активити, чтобы результат прилетал сюда
     */
    fun doOnPermissionsResult(
        permissionsHelper: PermissionsHelper,
        rationale: String,
        requestCode: Int,
        perms: Collection<String>,
        callbacks: PermissionsCallbacks,
    ): PermissionsHelper.ResultListener? {
        logger.d("doOnPermissionsResult, rationale=$rationale, requestCode=$requestCode, perms=$perms")
        return permissionsHelper.doOnPermissionsResult(this, rationale, requestCode, perms, callbacks).apply {
            permissionResultListeners[requestCode] = this
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        logger.d("onRequestPermissionsResult, requestCode=$requestCode, permissions=$permissions, grantResults=$grantResults")
        permissionResultListeners.remove(requestCode)?.onRequestPermissionsResult(permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        logger.d("onActivityResult, requestCode=$requestCode, resultCode=$resultCode, data=$data")
        permissionResultListeners.remove(requestCode)?.onActivityResult()
    }

    companion object {

        // во избежание пересечений кодов разрешений
        // (в отношении остального пользовать ResultLauncher'ы)
        // все объявлять здесь

        const val REQUEST_CODE_NOTIFICATIONS_PERMISSION = 1
        const val REQUEST_CODE_GPS_PERMISSION = 2
        const val REQUEST_CODE_WRITE_EXTERNAL_PERMISSION = 3
    }
}