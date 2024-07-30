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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.drag.DragAndDropTouchHelperCallback
import net.maxsmr.android.recyclerview.adapters.base.drag.OnStartDragHelperListener
import net.maxsmr.commonutils.AppClickableSpan
import net.maxsmr.commonutils.RangeSpanInfo
import net.maxsmr.commonutils.conversion.SizeUnit
import net.maxsmr.commonutils.copyToClipboard
import net.maxsmr.commonutils.format.formatSizeSingle
import net.maxsmr.commonutils.gui.PopupParams
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.setSpanText
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.gui.showPopupWindowWithObserver
import net.maxsmr.commonutils.media.path
import net.maxsmr.core.android.base.connection.ConnectionHandler
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.ui.alert.representation.asSnackbar
import net.maxsmr.core.ui.components.fragments.BaseMenuFragment
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
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
        it.asSnackbar(requireView())
    }.build()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    override val layoutId: Int = R.layout.fragment_downloads_state

    override val viewModel: DownloadsStateViewModel by viewModels()

    override val menuResId: Int = R.menu.menu_downloads_state

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

        viewModel.queueNames.observe {
            binding.tvQueueCount.text = it.size.toString()
            binding.containerQueue.isVisible = it.isNotEmpty()
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

            val position = binding.svDownloads.scrollY
            infoAdapter.items = items
            binding.svDownloads.post {
                // при выставлении итемов с разным контентом скролл сбивается
                binding.svDownloads.scrollTo(0, position)
            }

            binding.ibClearFinished.isVisible = items.any { item -> !item.downloadInfo.isLoading }

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
        state?.let {
            showDetailsPopup(state, anchorView)
        }
    }

    override fun onDeleteResource(downloadId: Long, name: String) {
        viewModel.onDeleteResource(downloadId, name)
    }

    override fun onViewResource(downloadUri: Uri, mimeType: String) {
        viewModel.onViewResource(downloadUri, mimeType)
    }

    override fun onShareResource(downloadUri: Uri, mimeType: String) {
        viewModel.onShareResource(downloadUri, mimeType)
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int, item: DownloadInfoAdapterData) {
        throw UnsupportedOperationException("Move DownloadInfoAdapterData not supported")
    }

    override fun onItemRemoved(position: Int, item: DownloadInfoAdapterData) {
        viewModel.onRemoveFinishedDownload(item.id)
    }

    private fun showDetailsPopup(state: DownloadStateNotifier.DownloadState, anchorView: View) {
        val context = requireContext()
        detailsPopupWindow = showPopupWindowWithObserver(
            context,
            detailsPopupWindow,
            PopupParams(anchorView,
                onDismissed = {
                    detailsPopupWindow = null
                    false
                },
                windowConfigurator = { window, _ ->
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
                    LayoutInflater.from(context),
                    it.parent as ViewGroup,
                    false
                ).apply {
                    ibClose.setOnClickListener {
                        hideDetailsPopup()
                    }
                    val params = state.params

                    with(StringBuilder()) {
                        append(params.requestParams.url)

                        fun appendSeparator(count: Int) {
                            repeat(count) {
                                append(System.lineSeparator())
                            }
                        }

                        val localUri = state.downloadInfo.localUri
                        if (localUri != null) {
                            localUri.path(context.contentResolver).takeIf {
                                it.isNotEmpty()
                            }?.let { path ->
                                appendSeparator(2)
                                append(path)
                            }
                            if (state is DownloadStateNotifier.DownloadState.Success) {
                                formatSizeSingle(state.resourceLength, SizeUnit.BYTES, precision = 2)?.let { size ->
                                    appendSeparator(2)
                                    append(size.get(context))
                                }
                            }
                        }

                        tvDetails.setSpanText(this, RangeSpanInfo(0,
                            params.requestParams.url.length,
                            listOf(
                                AppClickableSpan(
                                    true
                                ) {
                                    requireContext().copyToClipboard("url", params.requestParams.url)
                                    viewModel.showToast(TextMessage(net.maxsmr.core.ui.R.string.toast_link_copied_to_clipboard_message))
                                }
                            )))
                    }
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