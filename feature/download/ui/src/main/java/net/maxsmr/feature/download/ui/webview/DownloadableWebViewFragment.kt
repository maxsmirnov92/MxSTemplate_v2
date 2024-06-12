package net.maxsmr.feature.download.ui.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.DialogRepresentation
import net.maxsmr.core.ui.fields.bindHintError
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.databinding.DialogSaveAsBinding
import net.maxsmr.feature.webview.ui.BaseCustomizableWebViewFragment
import okhttp3.OkHttpClient
import java.lang.IllegalStateException

//@AndroidEntryPoint
class DownloadableWebViewFragment : BaseCustomizableWebViewFragment<DownloadableWebViewModel>() {

//    private val args by navArgs<DownloadableWebViewFragmentArgs>()

    override val viewModel: DownloadableWebViewModel by viewModels()

    private val downloadsViewModel: DownloadsViewModel by activityViewModels()

//    @Inject
//    @DownloaderOkHttpClient
    // при перехватах по некоторым урлам циклические редиректы
    override var okHttpClient: OkHttpClient? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: DownloadableWebViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        with(downloadsViewModel) {
            handleAlerts(requireContext(), AlertFragmentDelegate(this@DownloadableWebViewFragment, this))
            handleEvents(this@DownloadableWebViewFragment)
        }
    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<DownloadableWebViewModel>) {
        super.handleAlerts(delegate)
        bindAlertDialog(DownloadableWebViewModel.DIALOG_TAG_SAVE_AS) {
            @Suppress("UNCHECKED_CAST")
            val modelWithType = it.extraData as? ParamsModelWithType ?: throw IllegalStateException("Extra data for this dialog not specified")
            val model = modelWithType.first

            val dialogBinding = DialogSaveAsBinding.inflate(LayoutInflater.from(requireContext()))

            dialogBinding.etFileName.bindToTextNotNull(viewModel.fileNameField)
            viewModel.fileNameField.observeFromText(dialogBinding.etFileName, viewLifecycleOwner)
            viewModel.fileNameField.bindHintError(viewLifecycleOwner, dialogBinding.tilFileName)

            dialogBinding.etSubDirName.bindToTextNotNull(viewModel.subDirNameField)
            viewModel.subDirNameField.observeFromText(dialogBinding.etSubDirName, viewLifecycleOwner)
            viewModel.subDirNameField.bindHintError(viewLifecycleOwner, dialogBinding.tilSubDirName)

            DialogRepresentation.Builder(requireContext(), it)
                .setCustomView(dialogBinding.root) {
                    viewModel.canStartDownload.observe {isEnabled ->
                        (this as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
                    }
                }
                .setCancelable(false)
                .setPositiveButton(it.answers[0]) {
                    downloadsViewModel.enqueueDownload(model.copy(
                        fileName = viewModel.fileNameField.value,
                        subDirName = viewModel.subDirNameField.value
                    ), modelWithType.second)
                }
                .setNegativeButton(it.answers[1])
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