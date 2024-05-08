package net.maxsmr.feature.download.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.ui.alert.representation.asYesNoDialog
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.DownloadsStateViewModel.Companion.DIALOG_TAG_CLEAR_QUEUE
import net.maxsmr.feature.download.ui.adapter.DownloadInfoAdapter
import net.maxsmr.feature.download.ui.adapter.DownloadListener
import net.maxsmr.feature.download.ui.databinding.FragmentDownloadsStateBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsStateFragment : BaseVmFragment<DownloadsStateViewModel>(), DownloadListener {

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var factory: DownloadsStateViewModel.Factory

    override val layoutId: Int = R.layout.fragment_downloads_state

    override val viewModel: DownloadsStateViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            factory.create(it, downloadsViewModel)
        }
    }

    private val downloadsViewModel: DownloadsViewModel by viewModels()

    private val binding by viewBinding(FragmentDownloadsStateBinding::bind)

    private val infoAdapter = DownloadInfoAdapter(this)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: DownloadsStateViewModel,
        alertHandler: AlertHandler,
    ) {
        binding.rvDownloads.adapter = infoAdapter
        // FIXME декоратор не работает?
//        binding.rvDownloads.addItemDecoration(
//            DividerItemDecoration.Builder(requireContext())
//                .setDivider(Divider.Space(16), DividerItemDecoration.Mode.ALL_EXCEPT_LAST)
//                .build()
//        )
        viewModel.queueNames.observe {
            binding.tvQueueCount.text = it.size.toString()
            val mergedNames = it.joinToString()
            binding.tvQueuedNames.setTextOrGone("[ $mergedNames ]", isEmptyFunc = { mergedNames.isEmpty() })
            binding.ibClearQueue.isVisible = it.isNotEmpty()
        }
        viewModel.downloadItems.observe {
            infoAdapter.items = it
            binding.ibCancelAll.isVisible = it.any { item -> item.downloadInfo.isLoading }
            binding.ibClearFinished.isVisible = it.any { item -> !item.downloadInfo.isLoading }
        }
        binding.ibClearQueue.setOnClickListener {
            viewModel.onClearQueue()
        }
        binding.ibClearFinished.setOnClickListener {
            viewModel.onClearFinished()
        }
        binding.ibCancelAll.setOnClickListener {
            viewModel.onCancelAllDownloads()
        }
    }

    override fun handleAlerts() {
        super.handleAlerts()
        bindAlertDialog(DIALOG_TAG_CLEAR_QUEUE) {
            it.asYesNoDialog(requireContext())
        }
    }

    override fun onCancelDownload(downloadInfo: DownloadInfo) {
        viewModel.onCancelDownload(downloadInfo.id)
    }

    override fun onRetryDownload(downloadInfo: DownloadInfo, params: DownloadService.Params) {
        viewModel.onRetryDownload(downloadInfo.id, params)
    }

    override fun onRemoveFinishedDownload(downloadInfo: DownloadInfo) {
        // TODO удаление по кнопке или смахиванием
        viewModel.onRemoveFinishedDownload(downloadInfo.id)
    }
}