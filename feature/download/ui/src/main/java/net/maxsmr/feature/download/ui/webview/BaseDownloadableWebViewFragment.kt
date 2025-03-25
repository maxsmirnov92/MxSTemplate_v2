package net.maxsmr.feature.download.ui.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.webkit.WebSettings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import net.maxsmr.commonutils.flow.field.observeFromText
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.gui.hideKeyboard
import net.maxsmr.core.ui.alert.BaseAlertDelegate
import net.maxsmr.core.ui.components.handleAlerts
import net.maxsmr.core.ui.components.handleEvents
import net.maxsmr.core.ui.view.alert.representation.DialogViewAlertRepresentation
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation
import net.maxsmr.core.ui.view.bindHintError
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.DownloadsFragmentAlertDelegate
import net.maxsmr.feature.download.ui.databinding.DialogSaveAsBinding
import net.maxsmr.feature.webview.ui.BaseCustomizableWebViewFragment
import okhttp3.OkHttpClient

abstract class BaseDownloadableWebViewFragment<VM: BaseDownloadableWebViewModel> : BaseCustomizableWebViewFragment<VM>() {

    private val downloadsViewModel: DownloadsViewModel by activityViewModels()

    // TODO при перехватах по некоторым урлам циклические редиректы
    override var okHttpClient: OkHttpClient? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        with(downloadsViewModel) {
            // dialogQueue из downloadsViewModel
            DownloadsFragmentAlertDelegate(this@BaseDownloadableWebViewFragment, this).handleAlerts()
            handleEvents(this@BaseDownloadableWebViewFragment, navigationActor, toastActor)
        }
    }

    override fun handleAlerts(delegate: BaseAlertDelegate<VM, StandardAlertRepresentation>) {
        super.handleAlerts(delegate)
        delegate.bindAlertDialog(BaseDownloadableWebViewModel.DIALOG_TAG_SAVE_AS) {
            @Suppress("UNCHECKED_CAST")
            val modelWithType = it.extraData as? ParamsModelWithType ?: throw IllegalStateException("Extra data for this dialog not specified")
            val model = modelWithType.first

            val positiveAnswer = it.answers.getOrNull(0) ?: throw IllegalStateException("Required positive answer is missing")
            val negativeAnswer = it.answers.getOrNull(1) ?: throw IllegalStateException("Required negative answer is missing")

            val dialogBinding = DialogSaveAsBinding.inflate(LayoutInflater.from(requireContext()))

            dialogBinding.etFileName.bindToTextNotNull(viewModel.fileNameField)
            viewModel.fileNameField.observeFromText(dialogBinding.etFileName, viewLifecycleOwner)
            viewModel.fileNameField.bindHintError(viewLifecycleOwner, dialogBinding.tilFileName)

            dialogBinding.etSubDirName.bindToTextNotNull(viewModel.subDirNameField)
            viewModel.subDirNameField.observeFromText(dialogBinding.etSubDirName, viewLifecycleOwner)
            viewModel.subDirNameField.bindHintError(viewLifecycleOwner, dialogBinding.tilSubDirName)

            val onAction: () -> Unit = {
                downloadsViewModel.enqueueDownload(model.copy(
                    fileName = viewModel.fileNameField.value,
                    subDirName = viewModel.subDirNameField.value
                ), modelWithType.second)
                requireActivity().hideKeyboard()
            }

            dialogBinding.etSubDirName.setOnEditorActionListener { v, actionId, event ->
                if (actionId == IME_ACTION_DONE) {
                    positiveAnswer.select?.invoke()
                    onAction.invoke()
                    true
                } else {
                    false
                }
            }

            DialogViewAlertRepresentation.Builder(requireContext(), it)
                .setCustomView(dialogBinding.root) {
                    viewModel.canStartDownload.observeSafe {isEnabled ->
                        (this as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
                    }
                }
                .setCancelable(false)
                .setPositiveButton(positiveAnswer, onAction)
                .setNegativeButton(negativeAnswer)
                .build()
        }
    }

    override fun onSetupWebView(webSettings: WebSettings) {
        super.onSetupWebView(webSettings)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            viewModel.onDownloadStart(
                url,
                userAgent,
                contentDisposition,
                mimetype,
            )
        }
    }
}