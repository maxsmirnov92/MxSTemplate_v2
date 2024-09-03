package net.maxsmr.feature.address_sorter.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.drag.DragAndDropTouchHelperCallback
import net.maxsmr.android.recyclerview.adapters.base.drag.OnStartDragHelperListener
import net.maxsmr.commonutils.getViewLocationIntent
import net.maxsmr.commonutils.gui.runAction
import net.maxsmr.commonutils.gui.scrollTo
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.pick.ContentPicker
import net.maxsmr.core.android.content.pick.PickRequest
import net.maxsmr.core.android.content.pick.concrete.saf.SafPickerParams
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingApp
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_GPS_PERMISSION
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.core.ui.openAnyIntentWithToastError
import net.maxsmr.feature.address_sorter.data.toPointF
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputAdapter
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputData
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputListener
import net.maxsmr.feature.address_sorter.ui.databinding.FragmentAddressSorterBinding

abstract class BaseAddressSorterFragment : BaseNavigationFragment<AddressSorterViewModel>(),
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

    // by lazy не подходит, т.к.
    // "Fragments must call registerForActivityResult() before they are created"
    private val contentPicker: ContentPicker = FragmentContentPickerBuilder()
        .addRequest(
            PickRequest.BuilderDocument(REQUEST_CODE_CHOOSE_JSON)
                .addSafParams(SafPickerParams.json())
                .needPersistableUriAccess(true)
                .onSuccess {
                    viewModel.onPickAddressesJson(it.uri)
                }
                .onError {
                    viewModel.onPickerResultError(it)
                }
                .build()

        ).build()

    abstract val locationFactory: LocationViewModel.Factory

    abstract val factory: AddressSorterViewModel.Factory

    private var refreshMenuItem: MenuItem? = null

    private var lastLocationInfoMenuItem: MenuItem? = null

    private var buildRouteAppMenuItem: MenuItem? = null

    private var changeRoutingModeMenuItem: MenuItem? = null

    private var changeRoutingTypeMenuItem: MenuItem? = null

    private var changeSortPriorityMenuItem: MenuItem? = null

    private var importMenuItem: MenuItem? = null

    private var exportMenuItem: MenuItem? = null

    private var clearMenuItem: MenuItem? = null

    private var shouldScrollToEnd: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: AddressSorterViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        with(locationViewModel) {
            handleAlerts(
                // dialogQueue не из locationViewModel
                AlertFragmentDelegate(this@BaseAddressSorterFragment, this)
            )
            handleEvents(this@BaseAddressSorterFragment)
        }

        with(binding) {
            // конфликтует с draggable, рефреш по кнопке меню
            swipeLayout.isEnabled = false

            rvContent.adapter = adapter
            touchHelper.attachToRecyclerView(binding.rvContent)

            fabAdd.setOnClickListener {
                viewModel.onAddClick()
                shouldScrollToEnd = true
            }
            swipeLayout.setOnRefreshListener {
                viewModel.doRefresh()
            }

            viewModel.resultItemsState.observe { state ->
                swipeLayout.isEnabled = state.isLoading
                swipeLayout.isRefreshing = state.isLoading
                adapter.isMovementEnabled = !state.isLoading
                val items = state.data.orEmpty()
                if (items.isNotEmpty()) {
                    adapter.items = items
                    rvContent.isVisible = true
                    containerEmpty.isVisible = false
                    if (shouldScrollToEnd) {
                        rvContent.runAction {
                            rvContent.scrollTo(items.size - 1, true)
                        }
                        shouldScrollToEnd = false
                    }

                } else {
                    rvContent.isVisible = false
                    containerEmpty.isVisible = true
                }
                refreshStateMenuItems(state)
            }
            viewModel.resultLocationsState.observe {
                refreshBuildRouteAppMenuItem(it)
            }
            viewModel.lastLocation.observe {
                refreshLastLocationInfoMenuItem(it)
            }
        }

        adapter.registerItemsEventsListener(this)
    }

    override fun onResume() {
        super.onResume()
        doRequestGps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.unregisterItemsEventsListener(this)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        refreshMenuItem = menu.findItem(R.id.actionRefresh)
        lastLocationInfoMenuItem = menu.findItem(R.id.actionLastLocationInfo)
        buildRouteAppMenuItem = menu.findItem(R.id.actionBuildRouteApp)
        changeRoutingModeMenuItem = menu.findItem(R.id.actionChangeRoutingMode)
        changeRoutingTypeMenuItem = menu.findItem(R.id.actionChangeRoutingType)
        changeSortPriorityMenuItem = menu.findItem(R.id.actionChangeSortPriority)
        importMenuItem = menu.findItem(R.id.actionImportFromJson)
        exportMenuItem = menu.findItem(R.id.actionExportToJson)
        clearMenuItem = menu.findItem(R.id.actionClear)
        refreshStateMenuItems(viewModel.resultItemsState.value)
        refreshLastLocationInfoMenuItem(viewModel.lastLocation.value)
        refreshBuildRouteAppMenuItem(viewModel.resultLocationsState.value)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.actionRefresh -> {
                viewModel.doRefresh()
                true
            }

            R.id.actionLastLocationInfo -> {
                viewModel.onLastLocationInfoAction()
                true
            }

            R.id.actionBuildRouteApp -> {
                viewModel.onBuildRouteInApp { intent, app ->
                    requireContext().openAnyIntentWithToastError(
                        intent, errorResId = if (app == RoutingApp.YANDEX_NAVI) {
                            R.string.address_sorter_error_build_route_yandex_navi
                        } else {
                            net.maxsmr.core.ui.R.string.error_intent_any
                        }
                    )
                }
                true
            }

            R.id.actionChangeRoutingMode -> {
                viewModel.onChangeRoutingModeAction(
                    requireContext().resources.getStringArray(
                        R.array.address_sorter_routing_mode
                    ).toList()
                )
                true
            }

            R.id.actionChangeRoutingType -> {
                viewModel.onChangeRoutingTypeAction(
                    requireContext().resources.getStringArray(
                        R.array.address_sorter_routing_type
                    ).toList()
                )
                true
            }

            R.id.actionChangeSortPriority -> {
                viewModel.onChangeSortPriorityAction(
                    requireContext().resources.getStringArray(
                        R.array.address_sorter_sort_priority
                    ).toList()
                )
                true
            }

            R.id.actionImportFromJson -> {
                contentPicker.pick(REQUEST_CODE_CHOOSE_JSON, requireContext())
                true
            }

            R.id.actionExportToJson -> {
                viewModel.onExportAddressesAction()
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

    override fun onClearAction(id: Long) {
        viewModel.onClearQuery(id)
    }

    override fun onNavigateAction(item: AddressSorterViewModel.AddressItem) {
        if (item.isEmpty) return
        getViewLocationIntent(
            item.location?.toPointF(),
            item.address/*.takeIf { item.location == null }*/
        )?.let {
            requireContext().openAnyIntentWithToastError(
                it,
                errorResId = net.maxsmr.core.ui.R.string.error_intent_open_geo
            )
        }
    }

    override fun onInfoAction(item: AddressSorterViewModel.AddressItem) {
        viewModel.onInfoAction(item)
    }

    override fun onErrorMessageClose(id: Long, type: Address.ErrorType) {
        viewModel.onItemErrorMessageClose(id, type)
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

                override fun onGpsDisabledOrNotAvailable() {
                    super.onGpsDisabledOrNotAvailable()
                    viewModel.clearLastLocation()
                }

                override fun onPermissionsDenied() {
                    super.onPermissionsDenied()
                    viewModel.clearLastLocation()
                }
            }
        )
    }

    private fun refreshStateMenuItems(state: LoadState<List<AddressInputData>>?) {
        val items = listOf(
            refreshMenuItem,
            clearMenuItem,
            changeRoutingModeMenuItem,
            changeRoutingTypeMenuItem,
            changeSortPriorityMenuItem,
            exportMenuItem
        )
        val data = state?.data.orEmpty()
        items.forEach {
            it?.let {
                if (data.isNotEmpty()) {
                    it.isVisible = state?.isLoading == false
                } else {
                    it.isVisible = false
                }
            }
        }
        importMenuItem?.isVisible = state?.isLoading != true
    }

    private fun refreshLastLocationInfoMenuItem(lastLocation: Address.Location?) {
        lastLocationInfoMenuItem?.isVisible = lastLocation != null
    }

    private fun refreshBuildRouteAppMenuItem(state: LoadState<List<Address.Location>>?) {
        val data = state?.data.orEmpty()
        buildRouteAppMenuItem?.let {
            if (data.isNotEmpty()) {
                it.isVisible = state?.isLoading == false
            } else {
                it.isVisible = false
            }
        }
    }

    companion object {

        private const val REQUEST_CODE_CHOOSE_JSON = 1
    }
}