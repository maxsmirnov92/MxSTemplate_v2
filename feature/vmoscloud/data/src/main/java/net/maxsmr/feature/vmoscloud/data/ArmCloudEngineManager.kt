package net.maxsmr.feature.vmoscloud.data

import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.arm.armcloudsdk.ArmCloudEngine
import com.arm.armcloudsdk.config.PhonePlayConfig
import com.arm.armcloudsdk.innerapi.IPlayerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.maxsmr.commonutils.getDisplayRotation
import net.maxsmr.feature.vmoscloud.data.model.ArmCloudAuthInfo
import net.maxsmr.feature.vmoscloud.data.model.ArmCloudSettings
import javax.inject.Inject

class ArmCloudEngineManager @Inject constructor() : IPlayerListener {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isRunning by lazy { _isRunning.asStateFlow() }

    private val _isRunning = MutableStateFlow(false)

    override fun networkQualityRtt(rtt: Int) {
    }

    override fun onError(code: Int, msg: String) {
    }

    override fun onMultiCloudPhoneJoin(padCode: String) {
    }

    override fun onMultiCloudPhoneLeave(padCode: String) {
    }

    override fun onNetworkChanged(var1: Int) {
    }

    override fun onPlaySuccess(videoStreamProfileId: Int) {
    }

    override fun onServiceInit(extras: Map<String, Any>?) {
    }

    override fun onWarning(code: Int, msg: String) {
    }

    fun start(
        context: FragmentActivity,
        container: ViewGroup,
        authInfo: ArmCloudAuthInfo,
        settings: ArmCloudSettings,
    ) {
        if (isRunning.value) return

        val builder: PhonePlayConfig.Builder = with(settings) {
            PhonePlayConfig.Builder().context(context)
                .userId(authInfo.userId) //必填参数 自定义客户端用户 ID
                .padCode(authInfo.padCode) // 必填参数, 云手机实例 ID
                .token(authInfo.token) // 必填参数，临时鉴权 token
//            .clientType(dto.clientType) //必填参数 客户端类型
                .container(container) // 必填参数，用来承载画面的 Container
                .enableMultiControl(true) // 选填参数 是否开启群控
//            .setPadCodes(mChosePadCodes) // 选填参数 群控设备号集合
                .let {
                    val rotation = context.getDisplayRotation()
                    if (rotation != null) {
                        it.rotation(rotation) // 选填参数 屏幕的横竖屏 默认竖屏
                    } else {
                        it
                    }
                }
//            .videoStreamProfileId(videoStreamProfileId) // 选填参数，清晰度档位ID 默认高清
                .enableGyroscopeSensor(enableGyroscopeSensor) // 选填参数 打开陀螺仪开关 默认false
                .enableVibrator(enableVibrator) // 选填参数 打开本地振动开关 默认false
                .enableLocationService(enableLocationService) // 选填参数 打开本地定位功能开关 默认false
                .enableLocalKeyboard(enableLocalKeyboard) // 选填参数 打开本地键盘开关 默认false
                .enableClipboardCloudPhoneSync(enableClipboardCloudPhoneSync) // 选填参数 打开云机剪切板同步至真机 默认true
                .enableClipboardLocalPhoneSync(enableClipboardLocalPhoneSync) // 选填参数 打开真机剪切板同步至云机 默认true
                .enableCamera(enableCamera) // 选填参数 开启相机权限 默认 true
                .enableMic(enableMic) // 选填参数 开启麦克风权限 默认 false
//            .streamType(streamType) // 选填参数 指定启动云手机时拉取音视频流类型 默认拉取音视频流
//            .videoRenderMode(renderMode) // 选填参数 指定视频流渲染模式 默认等比缩放居中模式
//            .videoRotationMode(videoRotationMode) // 选填参数 指定视频旋转模式 默认非SDK处理旋转
                .autoRecycleTime(autoRecycleTime) // 选填参数 指定无操作回收时间 单位s 默认300s
        }

        ArmCloudEngine.start(builder.build(), this)
    }

    fun stop() {
        if (!isRunning.value) return
        ArmCloudEngine.stop()
    }
}