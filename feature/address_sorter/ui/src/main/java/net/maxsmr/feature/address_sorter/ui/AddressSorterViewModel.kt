package net.maxsmr.feature.address_sorter.ui

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.setValueIfNew
import net.maxsmr.commonutils.media.openInputStream
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.coroutines.usecase.asState
import net.maxsmr.core.android.coroutines.usecase.mapData
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asYesNoDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.feature.address_sorter.data.AddressSuggestUseCase
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.AddressItem.Companion.toUi
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.AddressSuggestItem.Companion.toUi
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputData
import java.io.Serializable

class AddressSorterViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val locationViewModel: LocationViewModel,
    private val repo: AddressRepo,
    private val addressSuggestUseCase: AddressSuggestUseCase,
) : BaseHandleableViewModel(state) {

    // сразу нельзя получить из
    // repo.sortedAddress.asLiveData(),
    // т.к. дальше планируется изменение итемов
    private val items by persistableLiveDataInitial<List<AddressItem>>(emptyList())

//    private val suggestsLiveData = MutableLiveData<Map<Int, LoadState<List<AddressSuggestItem>>>>(mapOf())

    private val suggestsMap = mutableMapOf<Long, LoadState<List<AddressSuggestItem>>>()

    private val suggestFlowMap = mutableMapOf<Long, FlowInfo>()

    val resultItemsState = MutableLiveData<LoadState<List<AddressInputData>>>(LoadState.success(emptyList()))

    override fun onInitialized() {
        super.onInitialized()
        viewModelScope.launch {
            repo.resultAddresses.collectLatest {
                items.postValue(it.map { address -> address.toUi() })
            }
        }
        viewModelScope.launch {
            repo.upsertCompletedEvent.collectLatest {
                resultItemsState.value = LoadState.success(resultItemsState.value?.data.orEmpty())
            }
        }
        locationViewModel.currentLocation.observe {
            viewModelScope.launch(Dispatchers.IO) {
                repo.setLastLocation(it)
            }
        }
        items.observe {
            resultItemsState.setValueIfNew(LoadState.success(it.mergeWithSuggests()))
            it.refreshFlows()
        }
    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(delegate)
        delegate.bindAlertDialog(DIALOG_TAG_CLEAR_ITEMS) {
            it.asYesNoDialog(delegate.context)
        }
    }

    fun onAddClick() {
        viewModelScope.launch {
            repo.addNewItem()
        }
    }

    fun onClearQuery(id: Long) {
        viewModelScope.launch {
            repo.updateQuery(id, EMPTY_STRING)
        }
    }

    fun onTextChanged(id: Long, value: String) {
        suggestFlowMap[id]?.let {
            it.flow.value = AddressSuggestUseCase.Parameters(id, value)
        }
    }

    fun doRefresh() {
        resultItemsState.value = LoadState.loading(resultItemsState.value?.data.orEmpty())
        viewModelScope.launch {
            repo.sortItems()
        }
    }

    fun onJsonResourceSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            uri.openInputStream(context.contentResolver)?.let {
                if (!repo.addFromStream(it)) {
                    showSnackbar(TextMessage(R.string.address_sorter_snackbar_address_add_error_message))
                }
                it.close()
            }
        }
    }

    fun onClearAction() {
        showYesNoDialog(DIALOG_TAG_CLEAR_ITEMS, TextMessage(R.string.address_sorter_dialog_clear_items_message)) {
            if (it == DialogInterface.BUTTON_POSITIVE) {
                viewModelScope.launch {
                    repo.clearItems()
                }
            }
        }
    }

    /**
     * Для адреса с данным [id] был выбран [AddressSuggestItem] из выпадающего списка
     */
    fun onSuggestSelected(id: Long, suggest: AddressSuggestItem) {
        viewModelScope.launch {
            repo.specifyItem(id, suggest.toDomain())
//        val current = suggestsLiveData.value?.toMutableMap() ?: mutableMapOf()
            onRemoveSuggests(id)
//        suggestsLiveData.value = current
        }
    }

    fun onItemRemoved(item: AddressInputData) {
        if (resultItemsState.value?.isLoading == true) return
        viewModelScope.launch {
            repo.deleteItem(item.id)
        }
    }

    fun onItemMoved(fromPosition: Int, toPosition: Int) {
        if (resultItemsState.value?.isLoading == true) return
        val currentItems = resultItemsState.value?.data.orEmpty().toMutableList()
        val item = currentItems.removeAt(fromPosition)
        val to: Int = if (fromPosition < toPosition) {
            toPosition - 1
        } else {
            toPosition
        }
        currentItems.add(to, item)
        viewModelScope.launch {
            repo.updateSortOrder(currentItems.map { it.id })
        }
    }

    private fun onNewSuggests(
        id: Long,
        state: LoadState<List<AddressSuggestItem>>,
        query: String,
    ) {
        suggestsMap[id] = state
        val resultState = resultItemsState.value
        resultState?.data?.let { current ->
            val newItems = current.map {
                if (it.id == id) {
                    return@map it.copy(item = it.item.copy(address = query), suggestsLoadState = state)
                } else {
                    it
                }
            }
            resultItemsState.postValue(resultState.copyOf(newItems))
        }
    }

    private fun onRemoveSuggests(id: Long) {
        suggestsMap.remove(id)?.let {
            val resultState = resultItemsState.value
            resultState?.data?.let { current ->
                val newItems = current.map {
                    if (it.id == id) {
                        it.copy(suggestsLoadState = LoadState.success(listOf()))
                    } else {
                        it
                    }
                }
                resultItemsState.postValue(resultState.copyOf(newItems))
            }
        }
    }

    private fun List<AddressItem>.refreshFlows() {
        this.forEach {
            // актуализация flow'ов по текущему списку итемов
            if (!suggestFlowMap.containsKey(it.id)) {
                val flow = MutableStateFlow(AddressSuggestUseCase.Parameters(it.id, it.address))
//                val resultMap = suggestsLiveData.value?.toMutableMap() ?: mutableMapOf()
                val startedJob = viewModelScope.launch {
                    addressSuggestUseCase(flow).map { result ->
                        result.mapData { addresses ->
                            addresses.map { a -> a.toUi() }
                        }
                    }.collect { result ->
                        onNewSuggests(it.id, result.asState(), flow.value.query)
//                        suggestsLiveData.value = resultMap
                    }
                }
                suggestFlowMap[it.id] = FlowInfo(flow, startedJob)
            }
        }
        suggestFlowMap.toMap().entries.forEach {
            // отмена текущих корутин по suggestion, если этого итема больше нет в списке
            if (!this.any { item -> item.id == it.key }) {
                val info = it.value
                info.startedJob.cancel()
                suggestFlowMap.remove(it.key)
                onRemoveSuggests(it.key)
            }
        }
    }

    private fun List<AddressItem>.mergeWithSuggests(): List<AddressInputData> {
        val result = mutableListOf<AddressInputData>()
        forEach { item ->
            suggestsMap[item.id]?.let {
                result.add(AddressInputData(item, it))
            } ?: result.add(AddressInputData(item, LoadState.success(listOf())))
        }
        return result
    }

    class FlowInfo(
        val flow: MutableStateFlow<AddressSuggestUseCase.Parameters>,
        val startedJob: Job,
    )

    data class AddressItem(
        val id: Long,
        val address: String,
        val location: Address.Location?,
        val isSuggested: Boolean,
    ) : Serializable {

        companion object {

            fun Address.toUi() = AddressItem(
                id, address, location, isSuggested
            )
        }
    }

    data class AddressSuggestItem(
        val address: String,
        val location: Address.Location?,
        val distance: Float?,
    ) : Serializable {

        fun toDomain() = AddressSuggest(
            location, address, distance
        )

        companion object {

            fun AddressSuggest.toUi() = AddressSuggestItem(
                address, location, distance
            )
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            locationViewModel: LocationViewModel,
        ): AddressSorterViewModel
    }

    companion object {

        const val DIALOG_TAG_CLEAR_ITEMS = "clear_items"
    }
}