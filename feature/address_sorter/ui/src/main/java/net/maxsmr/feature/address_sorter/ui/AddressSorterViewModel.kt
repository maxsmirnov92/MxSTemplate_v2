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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.format.TimePluralFormat
import net.maxsmr.commonutils.format.decomposeTimeFormatted
import net.maxsmr.commonutils.gui.message.JoinTextMessage
import net.maxsmr.commonutils.gui.message.PluralTextMessage
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.recharge
import net.maxsmr.commonutils.media.openInputStream
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.android.coroutines.usecase.asState
import net.maxsmr.core.android.coroutines.usecase.mapData
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.domain.entities.feature.address_sorter.SortPriority
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingMode
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingType
import net.maxsmr.feature.address_sorter.data.usecase.routing.RoutingFailedException
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asMultiChoiceDialog
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.alert.representation.asYesNoDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.feature.address_sorter.data.usecase.AddressSuggestUseCase
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.data.usecase.AddressSortUseCase
import net.maxsmr.feature.address_sorter.data.usecase.routing.AddressRoutingUseCase
import net.maxsmr.feature.address_sorter.data.usecase.routing.MissingLastLocationException
import net.maxsmr.feature.address_sorter.data.usecase.routing.MissingLocationException
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.AddressItem.Companion.toUi
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.AddressSuggestItem.Companion.toUi
import net.maxsmr.feature.address_sorter.ui.adapter.AddressExceptionData
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputData
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import java.io.Serializable
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class AddressSorterViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val locationViewModel: LocationViewModel,
    private val repo: AddressRepo,
    private val settingsRepo: SettingsDataStoreRepository,
    private val addressSuggestUseCase: AddressSuggestUseCase,
    private val addressSortUseCase: AddressSortUseCase,
    private val addressRoutingUseCase: AddressRoutingUseCase,
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
                resultItemsState.recharge()
            }
        }
        locationViewModel.currentLocation.observe {
            viewModelScope.launch {
                it?.let {
                    repo.setLastLocation(it)
                }
            }
        }
        items.observe {
            resultItemsState.value = LoadState.success(it.mergeWithSuggests())
            it.refreshFlows()
        }
    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(delegate)
        delegate.bindAlertDialog(DIALOG_TAG_CHANGE_ROUTING_MODE) {
            it.asMultiChoiceDialog(delegate.context, isRadioButton = true)
        }
        delegate.bindAlertDialog(DIALOG_TAG_CHANGE_ROUTING_TYPE) {
            it.asMultiChoiceDialog(delegate.context, isRadioButton = true)
        }
        delegate.bindAlertDialog(DIALOG_TAG_CHANGE_SORT_PRIORITY) {
            it.asMultiChoiceDialog(delegate.context, isRadioButton = true)
        }
        delegate.bindAlertDialog(DIALOG_TAG_CLEAR_ITEMS) {
            it.asYesNoDialog(delegate.context)
        }
        delegate.bindAlertDialog(DIALOG_TAG_ROUTING_FAILED) {
            it.asOkDialog(delegate.context)
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
        removeSnackbarsFromQueue()
        resultItemsState.value = LoadState.loading(resultItemsState.value?.data.orEmpty())
        viewModelScope.launch {
            val result = addressSortUseCase.invoke(Unit)
            val currentData = resultItemsState.value?.data.orEmpty()
            if (result is UseCaseResult.Error) {
                val e = result.exception
                resultItemsState.value = LoadState.error(e, currentData)
                showRoutingFailedMessage(e)
            } else if (result is UseCaseResult.Success && result.data.isEmpty()) {
                // поскольку не будет выставления в items.observe {}
                resultItemsState.value = LoadState.success(currentData)
            }
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

    fun onChangeRoutingModeAction(answerTexts: List<String>) {
        viewModelScope.launch {
            val settings = settingsRepo.getSettings()
            val currentIndex = RoutingMode.entries.indexOf(settings.routingMode)
            val answers = answerTexts.mapIndexed { index, s ->
                Alert.Answer(TextMessage(s), isChecked = index == currentIndex).onSelect {
                    if (index != currentIndex) {
                        val newMode = RoutingMode.entries[index]
                        viewModelScope.launch {
                            settingsRepo.updateSettings(settings.copy(routingMode = newMode))
                        }
                    }
                }
            }
            AlertDialogBuilder(DIALOG_TAG_CHANGE_ROUTING_MODE)
                .setTitle(R.string.address_sorter_dialog_change_routing_mode_title)
                .setAnswers(answers)
                .build()
        }
    }

    fun onChangeRoutingTypeAction(answerTexts: List<String>) {
        viewModelScope.launch {
            val settings = settingsRepo.getSettings()
            val currentIndex = RoutingType.entries.indexOf(settings.routingType)
            val answers = answerTexts.mapIndexed { index, s ->
                Alert.Answer(TextMessage(s), isChecked = index == currentIndex).onSelect {
                    if (index != currentIndex) {
                        val newType = RoutingType.entries[index]
                        viewModelScope.launch {
                            settingsRepo.updateSettings(settings.copy(routingType = newType))
                        }
                    }
                }
            }
            AlertDialogBuilder(DIALOG_TAG_CHANGE_ROUTING_TYPE)
                .setTitle(R.string.address_sorter_dialog_change_routing_type_title)
                .setAnswers(answers)
                .build()
        }
    }

    fun onChangeSortPriorityAction(answerTexts: List<String>) {
        viewModelScope.launch {
            val settings = settingsRepo.getSettings()
            val currentIndex = SortPriority.entries.indexOf(settings.sortPriority)
            val answers = answerTexts.mapIndexed { index, s ->
                Alert.Answer(TextMessage(s), isChecked = index == currentIndex).onSelect {
                    if (index != currentIndex) {
                        val newPriority = SortPriority.entries[index]
                        viewModelScope.launch {
                            settingsRepo.updateSettings(settings.copy(sortPriority = newPriority))
                        }
                    }
                }
            }
            AlertDialogBuilder(DIALOG_TAG_CHANGE_SORT_PRIORITY)
                .setTitle(R.string.address_sorter_dialog_change_sort_priority_title)
                .setAnswers(answers)
                .build()
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

    fun onInfoAction(item: AddressItem) {
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)

        viewModelScope.launch {
            val result = addressRoutingUseCase.invoke(AddressRoutingUseCase.Params(item.id, item.location))
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)

            val route = when (result) {
                is UseCaseResult.Error -> {
                    showRoutingFailedMessage(result.exception)
                    null
                }

                is UseCaseResult.Success -> {
                    result.data
                }

                else -> {
                    null
                }
            }

            val distanceMessage = (route?.distance?.toInt()
            // distance ранее пересчитанный в итеме или возвращшённый suggest'ом
                ?: item.distance?.roundToInt())?.let { distance ->
                TextMessage(
                    R.string.address_sorter_toast_distance_to_point_format,
                    if (distance > 1000) {
                        val distanceKm = (distance / 1000f).roundToInt()
                        PluralTextMessage(R.plurals.kilometers, distanceKm, distanceKm)
                    } else {
                        PluralTextMessage(R.plurals.meters, distance, distance)
                    }
                )

            }
            val durationMessage = route?.duration?.let { duration ->
                decomposeTimeFormatted(duration, TimeUnit.SECONDS, TimePluralFormat.NORMAL_WITH_VALUE, emptyIfZero = false).takeIf { it.isNotEmpty() }?.let {
                    TextMessage(
                        R.string.address_sorter_toast_duration_to_point_format,
                        JoinTextMessage(", ", it)
                    )
                }
            }
            val resultMessage = if (distanceMessage != null && durationMessage != null) {
                JoinTextMessage(".\n", distanceMessage, durationMessage)
            } else distanceMessage ?: durationMessage

            resultMessage?.let {
                showSnackbar(
                    it,
                    SnackbarExtraData(length = SnackbarExtraData.SnackbarLength.INDEFINITE),
                    Alert.Answer(android.R.string.ok),
                    uniqueStrategy = AlertQueueItem.UniqueStrategy.Replace
                )
            }
        }
    }

    fun onExceptionClose(id: Long, type: Address.ExceptionType) {
        viewModelScope.launch {
            repo.updateItem(id) {
                it.copy(
                    locationException = if (type == Address.ExceptionType.LOCATION) {
                        null
                    } else {
                        it.locationException
                    },
                    routingException = if (type == Address.ExceptionType.ROUTING) {
                        null
                    } else {
                        it.routingException
                    },
                ).apply {
                    this.id = id
                    this.sortOrder = it.sortOrder
                }
            }
        }
    }

    /**
     * Для адреса с данным [id] был выбран [AddressSuggestItem] из выпадающего списка
     */
    fun onSuggestSelected(id: Long, suggest: AddressSuggestItem) {
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)
        viewModelScope.launch {
            // убрать только из мапы
            onRemoveSuggests(id, true)
            // дальше должен быть mergeWithSuggests в Observer
            repo.specifyFromSuggest(id, suggest.toDomain())
//          val current = suggestsLiveData.value?.toMutableMap() ?: mutableMapOf()
//          suggestsLiveData.value = current
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
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
        val resultState = resultItemsState.value ?: LoadState.success(emptyList())
        val newItems = resultState.data?.map {
            if (it.id == id) {
                return@map it.copy(
                    item = it.item.copy(address = query, isSuggested = false),
                    suggestsLoadState = state
                )
            } else {
                it
            }
        }.orEmpty()
        resultItemsState.postValue(resultState.copyOf(newItems))
    }

    private fun onRemoveSuggests(id: Long, isFromMapOnly: Boolean) {
        suggestsMap.remove(id)?.let {
            if (isFromMapOnly) return@let
            val resultState = resultItemsState.value ?: LoadState.success(emptyList())
            val data = resultState.data.orEmpty()
            if (!data.any { it.id == id && it.suggestsLoadState.hasData() }) return
            val newItems = data.map {
                if (it.id == id) {
                    it.copy(suggestsLoadState = LoadState.success(listOf()))
                } else {
                    it
                }
            }
            resultItemsState.postValue(resultState.copyOf(newItems))
        }
    }

    private fun showRoutingFailedMessage(e: Throwable) {
        val message = when (e) {
            is MissingLastLocationException -> {
                TextMessage(
                    R.string.address_sorter_error_routing_format,
                    TextMessage(net.maxsmr.feature.address_sorter.data.R.string.address_sorter_error_missing_last_location)
                )
            }

            is MissingLocationException -> {
                val count = e.ids.size
                TextMessage(
                    R.string.address_sorter_error_routing_format,
                    if (count == 1) {
                        TextMessage(net.maxsmr.feature.address_sorter.data.R.string.address_sorter_error_missing_location)
                    } else {
                        TextMessage(net.maxsmr.feature.address_sorter.data.R.string.address_sorter_error_missing_locations_count_format, count)
                    }
                )
            }

            is RoutingFailedException -> {
                val count = e.routes.size
                if (count == 1) {
                    TextMessage(R.string.address_sorter_error_routing_count_format, 1)
                } else {
                    TextMessage(R.string.address_sorter_error_routing_format, e.routes[0].second)

                }
            }

            else -> {
                e.message?.takeIf { it.isNotEmpty() }?.let {
                    TextMessage(R.string.address_sorter_error_routing_format, it)
                } ?: TextMessage(R.string.address_sorter_error_routing)
            }
        }
        showOkDialog(DIALOG_TAG_ROUTING_FAILED, message)
    }

    private fun List<AddressItem>.refreshFlows() {
        this.forEach {
            // актуализация flow'ов по текущему списку итемов
            if (!suggestFlowMap.containsKey(it.id)) {
                val flow = MutableStateFlow(
                    if (!it.isSuggested) {
                        AddressSuggestUseCase.Parameters(it.id, it.address)
                    } else {
                        null
                    }
                )
//                val resultMap = suggestsLiveData.value?.toMutableMap() ?: mutableMapOf()
                val startedJob = viewModelScope.launch {
                    addressSuggestUseCase(flow).map { result ->
                        result.mapData { addresses ->
                            addresses.map { a -> a.toUi() }
                        }
                    }.collect { result ->
                        onNewSuggests(it.id, result.asState(), flow.value?.query.orEmpty())
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
                onRemoveSuggests(it.key, false)
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
        val flow: MutableStateFlow<AddressSuggestUseCase.Parameters?>,
        val startedJob: Job,
    )

    data class AddressItem(
        val id: Long,
        val address: String,
        val location: Address.Location?,
        val distance: Float?,
        val isSuggested: Boolean,
        val exceptionsData: ArrayList<AddressExceptionData>,
    ) : Serializable {

        companion object {

            fun Address.toUi() = AddressItem(
                id, address, location, distance, isSuggested,
                ArrayList(exceptionsMap.entries.map {
                    AddressExceptionData(it.key, it.value)
                })
            )
        }
    }

    data class AddressSuggestItem(
        val address: String,
        val location: Address.Location?,
        val distance: Float?,
    ) : Serializable {

        fun toDomain() = AddressSuggest(
            address, location, distance
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

        const val DIALOG_TAG_CHANGE_ROUTING_MODE = "change_routing_mode"
        const val DIALOG_TAG_CHANGE_ROUTING_TYPE = "change_routing_type"
        const val DIALOG_TAG_CHANGE_SORT_PRIORITY = "change_sort_priority"
        const val DIALOG_TAG_CLEAR_ITEMS = "clear_items"
        const val DIALOG_TAG_ROUTING_FAILED = "routing_failed"
    }
}