package net.maxsmr.feature.address_sorter.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.drag.DragAndDropTouchHelperCallback
import net.maxsmr.android.recyclerview.adapters.base.drag.OnStartDragHelperListener
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.pick.ContentPicker
import net.maxsmr.core.android.content.pick.PickRequest
import net.maxsmr.core.android.content.pick.concrete.saf.SafPickerParams
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_GPS_PERMISSION
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputAdapter
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputData
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputListener
import net.maxsmr.feature.address_sorter.ui.databinding.FragmentAddressSorterBinding
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.ui.observePostNotificationPermissionAsked
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class AddressSorterFragment : BaseNavigationFragment<AddressSorterViewModel>(),
        AddressInputListener, BaseDraggableDelegationAdapter.ItemsEventsListener<AddressInputData> {

    override val layoutId: Int = R.layout.fragment_address_sorter

    override val viewModel: AddressSorterViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            factory.create(it, locationViewModel)
        }
    }

    override val menuResId: Int = R.menu.menu_address_sorter

    private val locationViewModel: LocationViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            locationFactory.create(it, null)
        }
    }

    private val adapter = AddressInputAdapter(this)

    private val touchHelper: ItemTouchHelper = ItemTouchHelper(DragAndDropTouchHelperCallback(adapter)).also {
        adapter.startDragListener = object : OnStartDragHelperListener(it) {

            override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                if (binding.swipeLayout.isRefreshing) return
                super.onStartDrag(viewHolder)
            }

            override fun onStartSwipe(viewHolder: RecyclerView.ViewHolder) {
                if (binding.swipeLayout.isRefreshing) return
                super.onStartSwipe(viewHolder)
            }
        }
    }

    private val binding by viewBinding(FragmentAddressSorterBinding::bind)

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var locationFactory: LocationViewModel.Factory

    @Inject
    lateinit var factory: AddressSorterViewModel.Factory

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

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
                        viewModel.onPickerResultError(it)
                    }
                    .build()

            ).build()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: AddressSorterViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        with(locationViewModel) {
            handleAlerts(
                // dialogQueue не из locationViewModel
                AlertFragmentDelegate(this@AddressSorterFragment, this)
            )
            handleEvents(this@AddressSorterFragment)
        }

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

        cacheRepo.observePostNotificationPermissionAsked(this)
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