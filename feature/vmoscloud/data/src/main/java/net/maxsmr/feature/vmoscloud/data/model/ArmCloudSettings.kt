package net.maxsmr.feature.vmoscloud.data.model

data class ArmCloudSettings(
    val enableMultiControl: Boolean = true,
    val enableGyroscopeSensor: Boolean = true,
    val enableVibrator: Boolean = true,
    val enableLocationService: Boolean = true,
    val enableLocalKeyboard: Boolean = true,
    val enableClipboardCloudPhoneSync: Boolean = true,
    val enableClipboardLocalPhoneSync: Boolean = true,
    val enableCamera: Boolean = true,
    val enableMic: Boolean = true,
    val autoRecycleTime: Int = 300
)