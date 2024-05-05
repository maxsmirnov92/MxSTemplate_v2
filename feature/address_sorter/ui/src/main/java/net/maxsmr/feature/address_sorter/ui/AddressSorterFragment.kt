package net.maxsmr.feature.address_sorter.ui

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.drag.DragAndDropTouchHelperCallback
import net.maxsmr.android.recyclerview.adapters.base.drag.OnStartDragListener
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.pick.ContentPicker
import net.maxsmr.core.android.content.pick.PickRequest
import net.maxsmr.core.android.content.pick.concrete.saf.SafPickerParams
import net.maxsmr.core.ui.alert.representation.DialogRepresentation
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_DOWNLOAD_PERMISSION
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_GPS_PERMISSION
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_NOTIFICATIONS_PERMISSION
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.download.DownloadsViewModel
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputAdapter
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputData
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputListener
import net.maxsmr.feature.address_sorter.ui.databinding.FragmentAddressSorterBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import net.maxsmr.permissionchecker.PermissionsHelper.Companion.addPostNotificationsByApiVersion
import javax.inject.Inject
import kotlin.reflect.KClass

@AndroidEntryPoint
class AddressSorterFragment : BaseNavigationFragment<AddressSorterViewModel, NavArgs>(),
        AddressInputListener, OnStartDragListener, BaseDraggableDelegationAdapter.ItemsEventsListener<AddressInputData> {

    override val argsClass: KClass<NavArgs>? = null

    override val layoutId: Int = R.layout.fragment_address_sorter

    override val viewModel: AddressSorterViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            factory.create(it, locationViewModel, downloadsViewModel)
        }
    }

    override val menuResId: Int = R.menu.menu_address_sorter

    private val locationViewModel: LocationViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            locationFactory.create(it, AlertQueue(), null)
        }
    }

    private val downloadsViewModel: DownloadsViewModel by viewModels()

    private val adapter = AddressInputAdapter(this).apply {
        startDragListener = this@AddressSorterFragment
    }

    private val touchHelper: ItemTouchHelper = ItemTouchHelper(DragAndDropTouchHelperCallback(adapter))

    private val binding by viewBinding(FragmentAddressSorterBinding::bind)

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var locationFactory: LocationViewModel.Factory

    @Inject
    lateinit var factory: AddressSorterViewModel.Factory

    // by lazy не подходит, т.к.
    // "Fragments must call registerForActivityResult() before they are created"
    private val contentPicker: ContentPicker = FragmentContentPickerBuilder()
            .addRequest(
                PickRequest.BuilderDocument(REQUEST_CODE_CHOOSE_JSON)
                    .addSafParams(SafPickerParams.json())
                    .needPersistableUriAccess(true)
                    .onSuccess {
                        viewModel.onJsonResourceSelected(requireContext(), it.uri)
                    }
                    .onError {
                        viewModel.onJsonResourceSelectError(it)
                    }
                    .build()

            ).build()

    override fun handleAlerts() {
        super.handleAlerts()
        bindAlertDialog(AddressSorterViewModel.DIALOG_TAG_PICKER_ERROR) {
            it.asOkDialog(requireContext())
        }
        bindAlertDialog(AddressSorterViewModel.DIALOG_TAG_DOWNLOAD_FILE) {
            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.view_dialog_download_file, null)
            val urlEdit = customView.findViewById<EditText>(R.id.etUrl)
            val resourceNameEdit = customView.findViewById<EditText>(R.id.etName)
            DialogRepresentation.Builder(requireContext(), it)
//                .setThemeResId(com.google.android.material.R.style.Theme_MaterialComponents_DayNight)
                .setCustomView(customView)
                .setCancelable(true)
                .setPositiveButton(it.answers[0]) {
                    doOnPermissionsResult(REQUEST_CODE_DOWNLOAD_PERMISSION, addPostNotificationsByApiVersion(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                        viewModel.downloadFile(urlEdit.text.toString(), resourceNameEdit.text.toString())
                    }
                }
                .build()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: AddressSorterViewModel, alertHandler: AlertHandler) {
        super.onViewCreated(view, savedInstanceState, viewModel, alertHandler)

        with(locationViewModel) {
            handleAlerts(requireContext(), alertHandler)
            handleEvents(this@AddressSorterFragment)
        }
        downloadsViewModel.handleEvents(this)

        with(binding) {
            rvContent.adapter = adapter
            touchHelper.attachToRecyclerView(binding.rvContent)

            fabAdd.setOnClickListener {
                viewModel.onAddClick()
            }
            swipeLayout.setOnRefreshListener {
                viewModel.doRefresh()
            }

            viewModel.resultItems.observe { items ->
                swipeLayout.isRefreshing = false
                if (items.isNotEmpty()) {
                    adapter.items = items
                    rvContent.isVisible = true
                    containerEmpty.isVisible = false
                } else {
                    rvContent.isVisible = false
                    containerEmpty.isVisible = true
                }
            }
        }

        adapter.registerItemsEventsListener(this)

        viewModel.observePostNotificationPermissionAsked(this, REQUEST_CODE_NOTIFICATIONS_PERMISSION)
        doRequestGps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.unregisterItemsEventsListener(this)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.actionPickFromJson -> {
                contentPicker.pick(REQUEST_CODE_CHOOSE_JSON, requireContext())
                true
            }

            R.id.actionClear -> {
                viewModel.onClearAction()
                true
            }

            R.id.actionDownloadFile -> {
                viewModel.onDownloadFileAction()
                true
            }

            else -> {
                super.onMenuItemSelected(menuItem)
            }

        }
    }

    override fun onTextChanged(id: Long, value: String) {
        viewModel.onTextChanged(id, value)
    }

    override fun onSuggestSelect(id: Long, suggest: AddressSorterViewModel.AddressSuggestItem) {
        viewModel.onSuggestSelected(id, suggest)
    }

    override fun onRemove(id: Long) {
        viewModel.onRemoveClick(id)
    }

    override fun onItemRemoved(position: Int, item: AddressInputData) {
        viewModel.onItemRemoved(item)
    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int, item: AddressInputData) {
        viewModel.onItemMoved(fromPosition, toPosition)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        if (binding.swipeLayout.isRefreshing) return
        touchHelper.startDrag(viewHolder)
    }

    override fun onStartSwipe(viewHolder: RecyclerView.ViewHolder) {
        if (binding.swipeLayout.isRefreshing) return
        touchHelper.startSwipe(viewHolder)
    }

    private fun doRequestGpsWithRefresh() {
        doRequestGps { viewModel.doRefresh() }
    }

    private fun doRequestGps(targetAction: (() -> Unit)? = null) {
        locationViewModel.registerLocationUpdatesOnGpsCheck(this,
            REQUEST_CODE_GPS_PERMISSION,
            isGpsOnly = false,
            requireFineLocation = true,
            checkOnly = false,
            callbacks = object : LocationViewModel.GpsCheckCallbacks {
                override fun onPermissionsGranted() {
                    targetAction?.invoke()
                }
            }
        )
    }

    companion object {

        private const val REQUEST_CODE_CHOOSE_JSON = 1
    }
}