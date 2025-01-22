package net.maxsmr.feature.camera.ui

import androidx.fragment.app.Fragment
import net.maxsmr.core.ui.view.alert.delegate.ViewFragmentAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.feature.camera.ui.CameraXRecognitionViewModel.Companion.DIALOG_TAG_CAPTURE_RECOGNITION_RESULT

class CameraXRecognitionFragmentAlertDelegate(
    fragment: Fragment,
    viewModel: CameraXRecognitionViewModel,
): ViewFragmentAlertDelegate<CameraXRecognitionViewModel>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        super.handleCommonAlertDialogs()
        bindAlertDialog(DIALOG_TAG_CAPTURE_RECOGNITION_RESULT) {
            it.asOkDialog(context)
        }
    }
}