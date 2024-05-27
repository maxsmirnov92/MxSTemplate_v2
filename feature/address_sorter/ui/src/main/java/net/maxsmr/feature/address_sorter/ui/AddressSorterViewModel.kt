package net.maxsmr.feature.address_sorter.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.postRecharge
import net.maxsmr.commonutils.live.recharge
import net.maxsmr.commonutils.media.openInputStream
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.android.base.actions.SnackbarAction
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.coroutines.usecase.asState
import net.maxsmr.core.android.coroutines.usecase.mapData
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.feature.address_sorter.data.AddressSuggestUseCase
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.AddressItem.Companion.toUi
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.AddressSuggestItem.Companion.toUi
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputData
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.ui.BasePostNotificationViewModel
import java.io.Serializable

class AddressSorterViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val locationViewModel: LocationViewModel,
    cacheRepo: CacheDataStoreRepository,
    @Dispatcher(AppDispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    private val repo: AddressRepo,
    private val addressSuggestUseCase: AddressSuggestUseCase,
) : BasePostNotificationViewModel(cacheRepo, state) {

    // сразу нельзя получить из
    // repo.sortedAddress.asLiveData(),
    // т.к. дальше планируется изменение итемов
    private val items by persistableLiveDataInitial<List<AddressItem>>(emptyList())

//    private val suggestsLiveData = MutableLiveData<Map<Int, LoadState<List<AddressSuggestItem>>>>(mapOf())

    private val suggestsMap = mutableMapOf<Long, LoadState<List<AddressSuggestItem>>>()

    private val suggestFlowMap = mutableMapOf<Long, FlowInfo>()

    val resultItems by persistableLiveDataInitial<List<AddressInputData>>(emptyList())

    override fun onInitialized() {
        super.onInitialized()
        viewModelScope.launch {
            repo.resultAddresses.collectLatest {
                items.postValue(it.map { address -> address.toUi() })
            }
        }
        viewModelScope.launch {
            repo.sortCompletedEvent.collectLatest {
                items.postRecharge()
            }
        }
        locationViewModel.currentLocation.observe {
            viewModelScope.launch(ioDispatcher) {
                repo.refreshLocation(it)
            }
        }
        items.observe {
            it.refreshFlows()
            resultItems.value = it.mergeWithSuggests()
        }
    }

    fun onAddClick() {
        viewModelScope.launch {
            repo.addNewItem()
        }
    }

    fun onRemoveClick(id: Long) {
        viewModelScope.launch {
            repo.deleteItem(id)
        }
    }

    fun onTextChanged(id: Long, value: String) {
        suggestFlowMap[id]?.let {
            it.flow.value = AddressSuggestUseCase.Parameters(id, value)
        }
    }

    fun doRefresh() {
        if (resultItems.value?.isEmpty() == true) {
            resultItems.recharge()
        }
        // TODO change api
        viewModelScope.launch {
            repo.sortItems()
        }
    }

    fun onJsonResourceSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            uri.openInputStream(context.contentResolver)?.let {
                if (!repo.addFromStream(it)) {
                    showSnackbar(SnackbarAction(message = TextMessage(R.string.address_sorter_snackbar_address_add_error_message)))
                }
                it.close()
            }
        }
    }

    fun onClearAction() {
        viewModelScope.launch {
            repo.clearItems()
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
        viewModelScope.launch {
            repo.deleteItem(item.id)
        }
    }

    fun onItemMoved(fromPosition: Int, toPosition: Int) {
        val currentItems = resultItems.value.orEmpty().toMutableList()
        val item = currentItems.removeAt(fromPosition)
        val to: Int = if (fromPosition < toPosition) {
            toPosition - 1
        } else {
            toPosition
        }
        currentItems.add(to, item)
        viewModelScope.launch {
            repo.refreshSortOrder(currentItems.map { it.id })
        }
    }

    private fun onNewSuggests(id: Long, state: LoadState<List<AddressSuggestItem>>) {
        suggestsMap[id] = state
        if (!state.isLoading) {
            resultItems.value?.let { current ->
                current.find { it.id == id }?.let { data ->
                    val newItems = current.map {
                        if (it.id == id) {
                            return@map it.copy(suggests = data.suggests)
                        } else {
                            it
                        }
                    }
                    resultItems.value = newItems
                }
            }
        }
    }

    private fun onRemoveSuggests(id: Long) {
        suggestsMap.remove(id)?.let {
            resultItems.value?.let { current ->
                val newItems = current.toMutableList()
                newItems.removeIf { it.id == id }
                resultItems.value = newItems
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
                        onNewSuggests(it.id, result.asState())
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
            suggestsMap[item.id]?.takeIf { it.isSuccess() }?.let {
                result.add(AddressInputData(item, it.data.orEmpty()))
            } ?: result.add(AddressInputData(item, emptyList()))
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
    ): Serializable {

        companion object {

            fun Address.toUi() = AddressItem(
                id, address, location
            )
        }
    }

    data class AddressSuggestItem(
        val address: String,
        val location: Address.Location?,
        val distance: Int?,
    ): Serializable {

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
}