package net.maxsmr.feature.download.ui

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.drag.DragAndDropTouchHelperCallback
import net.maxsmr.android.recyclerview.adapters.base.drag.OnStartDragHelperListener
import net.maxsmr.commonutils.AppClickableSpan
import net.maxsmr.commonutils.Predicate.Methods.findIndexed
import net.maxsmr.commonutils.RangeSpanInfo
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.gui.PopupParams
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.setSpanText
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.gui.showPopupWindowWithObserver
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.base.connection.ConnectionHandler
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.ui.alert.representation.asIndefiniteSnackbar
import net.maxsmr.core.ui.alert.representation.asYesNoDialog
import net.maxsmr.core.ui.components.fragments.BaseMenuFragment
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadService.Companion.getShareAction
import net.maxsmr.feature.download.data.DownloadService.Companion.getViewAction
import net.maxsmr.feature.download.data.DownloadStateNotifier
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.DownloadsStateViewModel.Companion.DIALOG_TAG_CANCEL_ALL
import net.maxsmr.feature.download.ui.DownloadsStateViewModel.Companion.DIALOG_TAG_CLEAR_QUEUE
import net.maxsmr.feature.download.ui.DownloadsStateViewModel.Companion.DIALOG_TAG_RETRY_IF_SUCCESS
import net.maxsmr.feature.download.ui.adapter.DownloadInfoAdapter
import net.maxsmr.feature.download.ui.adapter.DownloadInfoAdapterData
import net.maxsmr.feature.download.ui.adapter.DownloadListener
import net.maxsmr.feature.download.ui.databinding.FragmentDownloadsStateBinding
import net.maxsmr.feature.download.ui.databinding.LayoutPopupDownloadDetailsBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsStateFragment : BaseMenuFragment<DownloadsStateViewModel>(),
        DownloadListener, BaseDraggableDelegationAdapter.ItemsEventsListener<DownloadInfoAdapterData>,
        SearchView.OnQueryTextListener {

    override val connectionHandler: ConnectionHandler = ConnectionHandler.Builder().mapAlerts {
        it.asIndefiniteSnackbar(requireView())
    }.build()

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

    override val menuResId: Int = R.menu.menu_downloads_state

    private val downloadsViewModel: DownloadsViewModel by activityViewModels()

    private val binding by viewBinding(FragmentDownloadsStateBinding::bind)

    private val infoAdapter = DownloadInfoAdapter(this)

    private val touchHelper: ItemTouchHelper = ItemTouchHelper(DragAndDropTouchHelperCallback(infoAdapter)).also {
        infoAdapter.startDragListener = OnStartDragHelperListener(it)
    }

    private var detailsPopupWindow: PopupWindow? = null

    private var filterItem: MenuItem? = null

    private var searchView: SearchView? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: DownloadsStateViewModel,
    ) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        binding.rvDownloads.adapter = infoAdapter
        touchHelper.attachToRecyclerView(binding.rvDownloads)
        infoAdapter.registerItemsEventsListener(this)

        // FIXME декоратор не работает?
//        binding.rvDownloads.addItemDecoration(
//            DividerItemDecoration.Builder(requireContext())
//                .setDivider(Divider.Space(16), DividerItemDecoration.Mode.ALL_EXCEPT_LAST)
//                .build()
//        )
        viewModel.queueNames.observe {
            binding.tvQueueCount.text = it.size.toString()
            val mergedNames = it.joinToString("; ")
            binding.tvQueuedNames.setTextOrGone("[ $mergedNames ]", isEmptyFunc = { mergedNames.isEmpty() })
            binding.ibClearQueue.isVisible = it.isNotEmpty()
        }
        viewModel.allItems.observe {
            filterItem?.isVisible = it.isNotEmpty()
        }
        viewModel.currentItems.observe { items ->
            if (items.isNotEmpty()) {
                binding.rvDownloads.isVisible = true
                binding.containerEmpty.isVisible = false
            } else {
                binding.rvDownloads.isVisible = false
                binding.containerEmpty.isVisible = true
            }
            infoAdapter.items = items

            binding.ibClearFinished.isVisible = items.any { item -> !item.downloadInfo.isLoading }
            // FIXME скролл
            if (!infoAdapter.isEmpty) {
                findIndexed(items) { it.state is DownloadStateNotifier.DownloadState.Loading }?.let {
                    binding.rvDownloads.scrollToPosition(it.first)
                }
            }
        }
        viewModel.anyCanBeCancelled.observe {
            binding.ibCancelAll.isVisible = it
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

    override fun onDestroyView() {
        super.onDestroyView()
        infoAdapter.unregisterItemsEventsListener(this)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        with(menu.findItem(R.id.action_filter)) {
            filterItem = this
            searchView = (actionView as SearchView).apply {
                queryHint = getString(R.string.download_menu_action_filter)
                setOnQueryTextListener(this@DownloadsStateFragment)
            }
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        searchView?.setQuery(viewModel.queryNameFilter.value.orEmpty(), false)
    }

    override fun onQueryTextSubmit(query: String?): Boolean = true

    override fun onQueryTextChange(newText: String?): Boolean {
        viewModel.onNameQueryFilterChanged(newText)
        return true
    }

    override fun onCancelDownload(downloadInfo: DownloadInfo) {
        viewModel.onCancelDownload(downloadInfo.id)
    }

    override fun onRetryDownload(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        state: DownloadStateNotifier.DownloadState?,
    ) {
        viewModel.onRetryDownload(downloadInfo.id, params, state)
    }

    override fun onShowDownloadDetails(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        state: DownloadStateNotifier.DownloadState?,
        anchorView: View,
    ) {
        showDetailsPopup(params, anchorView)
    }

    override fun onViewResource(downloadUri: Uri, mimeType: String) {
        startActivity(getViewAction().intent(downloadUri, mimeType))
    }

    override fun onShareResource(downloadUri: Uri, mimeType: String) {
        startActivity(getShareAction().intent(downloadUri, mimeType))
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int, item: DownloadInfoAdapterData) {
        throw UnsupportedOperationException("Move DownloadInfoAdapterData not supported")
    }

    override fun onItemRemoved(position: Int, item: DownloadInfoAdapterData) {
        viewModel.onRemoveFinishedDownload(item.id)
    }

    private fun showDetailsPopup(params: DownloadService.Params, anchorView: View) {
        detailsPopupWindow = showPopupWindowWithObserver(
            requireContext(),
            detailsPopupWindow,
            PopupParams(anchorView,
                onDismissed = {
                    detailsPopupWindow = null
                    false
                },
                windowConfigurator = { window, context ->
                    with(window) {
                        width = (context.resources.displayMetrics.widthPixels * 0.8).toInt()
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                        setBackgroundDrawable(
                            ColorDrawable(
                                ContextCompat.getColor(
                                    context,
                                    android.R.color.transparent
                                )
                            )
                        )
                        isFocusable = true
                        isTouchable = true
                        isOutsideTouchable = true
                        animationStyle = net.maxsmr.core.ui.R.style.PopupAnimationStyle
                    }
                }
            ),
            contentViewCreator = {
                val binding = LayoutPopupDownloadDetailsBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    it.parent as ViewGroup,
                    false
                ).apply {
                    ibClose.setOnClickListener {
                        hideDetailsPopup()
                    }
                    tvDetails.setSpanText(params.requestParams.url, RangeSpanInfo(0,
                        params.requestParams.url.length,
                        listOf(
                            AppClickableSpan(
                                true
                            ) {
                                copyToClipboard(requireContext(), "url", params.requestParams.url)
                                viewModel.showToast(ToastAction(TextMessage(net.maxsmr.core.android.R.string.toast_copied_to_clipboard_message)))
                            }
                        )))
                }
                return@showPopupWindowWithObserver binding.root
            }
        )
    }

    private fun hideDetailsPopup() {
        detailsPopupWindow?.let { window ->
            window.dismiss()
            detailsPopupWindow = null
        }
    }
}