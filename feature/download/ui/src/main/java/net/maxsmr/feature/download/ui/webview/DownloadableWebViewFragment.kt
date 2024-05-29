package net.maxsmr.feature.download.ui.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.DialogRepresentation
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.databinding.DialogSaveAsBinding
import net.maxsmr.feature.webview.ui.BaseCustomizableWebViewFragment
import net.maxsmr.feature.webview.ui.databinding.DialogInputUrlBinding
import okhttp3.OkHttpClient
import java.lang.IllegalStateException
import javax.inject.Inject

@AndroidEntryPoint
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
            val model = it.extraData as? DownloadParamsModel ?: throw IllegalStateException("Extra data for this dialog not specified")
            val dialogBinding = DialogSaveAsBinding.inflate(LayoutInflater.from(requireContext()))
            dialogBinding.etFileName.setText(model.fileName)
            DialogRepresentation.Builder(requireContext(), it)
                .setCustomView(dialogBinding.root)
                .setPositiveButton(it.answers[0]) {
                    val newFileName =  dialogBinding.etFileName.text.toString()
                    val newSubDirName = dialogBinding.etSubDirName.text.toString()
                    downloadsViewModel.enqueueDownload(model.copy(
                        fileName = newFileName.ifEmpty { model.fileName },
                        subDirName = newSubDirName.ifEmpty { model.subDirName }
                    ))
                }
                .setNegativeButton(it.answers[1])
                .build()
        }
    }

    override fun onSetupWebView(webSettings: WebSettings) {
        super.onSetupWebView(webSettings)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            viewModel.onDownloadStart(
                url,
                userAgent,
                contentDisposition,
                mimetype,
            )
        }
    }
}