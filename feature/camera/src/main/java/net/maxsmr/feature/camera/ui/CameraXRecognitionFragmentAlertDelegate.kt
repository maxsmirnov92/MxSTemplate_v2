package net.maxsmr.feature.camera.ui

import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.feature.camera.ui.CameraXRecognitionViewModel.Companion.DIALOG_TAG_CAPTURE_RECOGNITION_RESULT

class CameraXRecognitionFragmentAlertDelegate(
    override val fragment: BaseVmFragment<CameraXRecognitionViewModel>,
    override val viewModel: CameraXRecognitionViewModel,
): BaseFragmentViewAlertDelegate<CameraXRecognitionViewModel>() {

    override fun handleAlertDialogs() {
        bindAlertDialog(DIALOG_TAG_CAPTURE_RECOGNITION_RESULT) {
            it.asOkDialog(context)
        }
    }
}