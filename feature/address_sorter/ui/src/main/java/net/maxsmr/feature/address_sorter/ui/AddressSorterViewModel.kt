package net.maxsmr.feature.address_sorter.ui

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
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
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.format.TimePluralFormat
import net.maxsmr.commonutils.format.decomposeTimeFormatted
import net.maxsmr.commonutils.gui.message.JoinTextMessage
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.media.readString
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.capFirstChar
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.android.coroutines.usecase.asState
import net.maxsmr.core.android.coroutines.usecase.data
import net.maxsmr.core.android.coroutines.usecase.mapData
import net.maxsmr.core.android.coroutines.usecase.succeeded
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.android.network.isUrlValid
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.domain.entities.feature.address_sorter.SortPriority
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingApp
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingMode
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingType
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.network.HttpErrorCode
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.RoutingFailedException
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asMultiChoiceDialog
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.alert.representation.asYesNoDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.fields.fileNameField
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.feature.address_sorter.data.getDisplayedMessageResId
import net.maxsmr.feature.address_sorter.data.getDoubleGisRouteIntent
import net.maxsmr.feature.address_sorter.data.getYandexNaviRouteIntent
import net.maxsmr.feature.address_sorter.data.usecase.AddressSuggestUseCase
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.data.usecase.AddressExportUseCase
import net.maxsmr.feature.address_sorter.data.usecase.AddressImportUseCase
import net.maxsmr.feature.address_sorter.data.usecase.AddressSuggestGeocodeUseCase
import net.maxsmr.feature.address_sorter.data.usecase.AddressSortUseCase
import net.maxsmr.feature.address_sorter.data.usecase.ReverseGeocodeUseCase
import net.maxsmr.feature.address_sorter.data.usecase.AddressRoutingUseCase
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.MissingLastLocationException
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.MissingLocationException
import net.maxsmr.feature.address_sorter.data.toAddressLocation
import net.maxsmr.feature.address_sorter.data.usecase.AddressExportUseCase.Companion.EXPORT_FILE_NAME_DEFAULT
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.AddressItem.Companion.toUi
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel.AddressSuggestItem.Companion.toUi
import net.maxsmr.feature.address_sorter.ui.adapter.AddressErrorMessageData
import net.maxsmr.feature.address_sorter.ui.adapter.AddressInputData
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.data.DownloadsViewModel.Companion.toParams
import net.maxsmr.feature.download.data.storage.DownloadServiceStorage
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import retrofit2.HttpException
import java.io.Serializable
import java.util.concurrent.TimeUnit

class AddressSorterViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val locationViewModel: LocationViewModel,
    @Assisted private val downloadsViewModel: DownloadsViewModel,
    @Assisted private val routingKeyUrl: String,
    private val repo: AddressRepo,
    private val settingsRepo: SettingsDataStoreRepository,
    private val cacheRepo: CacheDataStoreRepository,
    private val addressImportUseCase: AddressImportUseCase,
    private val addressExportUseCase: AddressExportUseCase,
    private val addressSuggestUseCase: AddressSuggestUseCase,
    private val addressSuggestGeocodeUseCase: AddressSuggestGeocodeUseCase,
    private val reverseGeocodeUseCase: ReverseGeocodeUseCase,
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

    val exportFileNameField: Field<String> =
        state.fileNameField(isRequired = true, initialValue = EXPORT_FILE_NAME_DEFAULT)

    val resultItemsState = MutableLiveData<LoadState<List<AddressInputData>>>(LoadState.success(emptyList()))

    val resultLocationsState = resultItemsState.map {
        it.copyOf(it.data?.mapNotNull { data -> data.item.location })
    }

    val lastLocation = MutableLiveData<Address.Location?>()

    override fun onInitialized() {
        super.onInitialized()
        viewModelScope.launch {
            repo.resultAddresses.collectLatest {
                items.postValue(it.map { address -> address.toUi() })
            }
        }
        viewModelScope.launch {
            repo.upsertCompletedEvent.collectLatest {
                resultItemsState.value = resultItemsState.value?.takeIf { !it.isLoading }
                    ?: LoadState.success(resultItemsState.value?.data.orEmpty())
            }
        }
        locationViewModel.currentLocation.observe {
            it?.let {
                lastLocation.value = it.toAddressLocation()
            }
        }
        items.observe {
            resultItemsState.value = LoadState.success(it.mergeWithSuggests())
            it.refreshFlows()
        }
        exportFileNameField.valueLive.observe {
            exportFileNameField.validateAndSetByRequired()
        }
    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(delegate)
        delegate.bindAlertDialog(DIALOG_TAG_IMPORT_FAILED) {
            it.asOkDialog(delegate.context)
        }
        delegate.bindAlertDialog(DIALOG_TAG_EXPORT_SUCCESS) {
            it.asOkDialog(delegate.context)
        }
        delegate.bindAlertDialog(DIALOG_TAG_EXPORT_FAILED) {
            it.asOkDialog(delegate.context)
        }
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
        delegate.bindAlertDialog(DIALOG_TAG_REVERSE_GEOCODE_FAILED) {
            it.asOkDialog(delegate.context)
        }
        delegate.bindAlertDialog(DIALOG_TAG_ROUTING_FAILED) {
            it.asOkDialog(delegate.context)
        }
        delegate.bindAlertDialog(DIALOG_TAG_DOWNLOAD_KEY_FAILED) {
            it.asOkDialog(delegate.context)
        }
    }

    fun clearLastLocation() {
        lastLocation.value = null
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
            it.flow.value = AddressSuggestUseCase.Parameters(id, value, lastLocation.value)
        }
    }

    fun doRefresh() {
        removeSnackbarsFromQueue()
        resultItemsState.value = LoadState.loading(resultItemsState.value?.data.orEmpty())

        viewModelScope.launch {

            var wasKeyDownloaded = false

            suspend fun doAddressSort() {
                val result = addressSortUseCase.invoke(lastLocation.value)
                val currentData = resultItemsState.value?.data.orEmpty()
                if (result is UseCaseResult.Error) {
                    val e = result.exception

                    fun handleBaseError(showMessage: Boolean = true) {
                        resultItemsState.value = LoadState.error(e, currentData)
                        if (showMessage) {
                            showRoutingFailedMessage(e, result.errorMessage())
                        }
                    }

                    if (shouldDownloadRoutingKey(e) && !wasKeyDownloaded) {
                        downloadsViewModel.observeDownload(enqueueDownloadRoutingKey()).observe {
                            viewModelScope.launch {

                                fun handleDownloadError(e: Throwable?) {
                                    showDownloadRoutingKeyError(e)
                                    handleBaseError(false)
                                }

                                if (it.isSuccessWithData()) {
                                    val key = withContext(Dispatchers.IO) {
                                        it.data?.downloadInfo?.localUri?.readString(baseApplicationContext.contentResolver)
                                            .orEmpty()
                                    }
                                    if (key.isNotEmpty()) {
                                        wasKeyDownloaded = true
                                        cacheRepo.setDoubleGisRoutingApiKey(key)
                                        // повтор юзкейса с актуализированным ключом
                                        doAddressSort()
                                    } else {
                                        handleDownloadError(EmptyResultException())
                                    }
                                } else if (!it.isLoading) {
                                    handleDownloadError(it.error)
                                }
                            }
                        }
                    } else {
                        handleBaseError()
                    }
                } else if (result is UseCaseResult.Success && result.data.isEmpty()) {
                    // поскольку не будет выставления в items.observe {}
                    resultItemsState.value = LoadState.success(currentData)
                }
            }

            doAddressSort()
        }
    }

    fun onPickAddressesJson(uri: Uri) {
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)
        viewModelScope.launch {
            val result = addressImportUseCase.invoke(uri)
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
            if (result is UseCaseResult.Error) {
                showOkDialog(
                    DIALOG_TAG_IMPORT_FAILED,
                    result.errorMessage()?.let {
                        TextMessage(R.string.address_sorter_dialog_address_import_error_message_format, it)
                    } ?: TextMessage(R.string.address_sorter_dialog_address_import_error_message)
                )
            }
        }
    }

    fun onExportAddressesAction() {
        showYesNoDialog(
            DIALOG_TAG_EXPORT_FILE_NAME,
            message = null,
            title = TextMessage(R.string.address_sorter_dialog_address_export_file_name_title),
            positiveAnswerResId = android.R.string.ok,
            negativeAnswerResId = android.R.string.cancel
        )
    }

    fun onExportPositiveAction() {
        if (exportFileNameField.hasError) return
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)
        viewModelScope.launch {
            val result = addressExportUseCase.invoke(exportFileNameField.value)
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
            if (result.succeeded) {
                showOkDialog(
                    DIALOG_TAG_EXPORT_SUCCESS,
                    TextMessage(R.string.address_sorter_dialog_address_export_success_message_format, result.data)
                )
            } else if (result is UseCaseResult.Error) {
                showOkDialog(
                    DIALOG_TAG_EXPORT_FAILED,
                    result.errorMessage()?.let {
                        TextMessage(R.string.address_sorter_dialog_address_export_error_message_format, it)
                    } ?: TextMessage(R.string.address_sorter_dialog_address_export_error_message)
                )
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
        removeSnackbarsFromQueue()
        showYesNoDialog(DIALOG_TAG_CLEAR_ITEMS, TextMessage(R.string.address_sorter_dialog_clear_items_message)) {
            if (it == DialogInterface.BUTTON_POSITIVE) {
                viewModelScope.launch {
                    repo.clearItems()
                }
            }
        }
    }

    fun onLastLocationInfoAction() {
        val location = locationViewModel.currentLocation.value?.let {
            Address.Location(it.latitude.toFloat(), it.longitude.toFloat())
        } ?: return
        removeSnackbarsFromQueue()
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)
        viewModelScope.launch {
            val result = reverseGeocodeUseCase.invoke(location)
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
            val geocode = when (result) {
                is UseCaseResult.Error -> {
                    showReverseGeocodeFailedMessage(result.exception, result.errorMessage())
                    null
                }

                is UseCaseResult.Success -> {
                    result.data
                }

                else -> {
                    null
                }
            }

            val message = StringBuilder(location.asReadableString(true))
            geocode?.let {
                message.append(".\n")
                message.append(geocode.name.capFirstChar())
                geocode.description?.takeIf { it.isNotEmpty() }?.let {
                    message.append(" ($it)")
                }
            }
            showSnackbar(
                TextMessage(R.string.address_sorter_toast_your_last_location_format, message),
                SnackbarExtraData(length = SnackbarExtraData.SnackbarLength.INDEFINITE, maxLines = 6),
                Alert.Answer(android.R.string.ok),
                uniqueStrategy = AlertQueueItem.UniqueStrategy.Replace
            )
        }
    }

    fun onBuildRouteInApp(navigateFunc: (Intent, RoutingApp) -> Unit) {
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)
        viewModelScope.launch {
            val locations = resultLocationsState.value?.data?.toSet().orEmpty()
            val settings = settingsRepo.getSettings()
            withContext(Dispatchers.IO) {
                when (settings.routingApp) {
                    RoutingApp.DOUBLEGIS -> {
                        getDoubleGisRouteIntent(locations, settings.routingAppFromCurrent)
                    }

                    RoutingApp.YANDEX_NAVI -> {
                        getYandexNaviRouteIntent(locations, settings.routingAppFromCurrent)
                    }
                }
            }?.let {
                dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
                navigateFunc(it, settings.routingApp)
            }
        }
    }

    fun onInfoAction(item: AddressItem) {
        removeSnackbarsFromQueue()
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)

        viewModelScope.launch {

            fun handleResult(route: AddressRoute?) {
                dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)

                val distanceMessage = (route?.distance
                // distance ранее пересчитанный в итеме или возвращшённый suggest'ом
                    ?: item.distance)?.let { distance ->
                    TextMessage(
                        R.string.address_sorter_toast_distance_to_point_format,
                        if (distance > 1000) {
                            val distanceKm = distance / 1000f
                            TextMessage(R.string.address_sorter_kilometers_format, distanceKm, distanceKm)
                        } else {
                            TextMessage(R.string.address_sorter_meters_format, distance, distance)
                        }
                    )
                }
                val durationMessage = route?.duration?.let { duration ->
                    decomposeTimeFormatted(
                        duration,
                        TimeUnit.SECONDS,
                        TimePluralFormat.NORMAL_WITH_VALUE,
                        emptyIfZero = false,
                        timeUnitsToExclude = setOf(TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS, TimeUnit.NANOSECONDS)
                    ).takeIf { it.isNotEmpty() }?.let {
                        TextMessage(
                            R.string.address_sorter_toast_duration_to_point_format,
                            JoinTextMessage(", ", it)
                        )
                    }
                }
                var resultMessage = if (distanceMessage != null && durationMessage != null) {
                    JoinTextMessage(".\n", distanceMessage, durationMessage)
                } else distanceMessage ?: durationMessage

                resultMessage?.let {
                    item.location?.let { location ->
                        resultMessage =
                            JoinTextMessage("\n\n", it, TextMessage("(${location.latitude}, ${location.longitude})"))
                    }
                }

                resultMessage?.let {
                    showSnackbar(
                        it,
                        SnackbarExtraData(length = SnackbarExtraData.SnackbarLength.INDEFINITE),
                        Alert.Answer(android.R.string.ok),
                        uniqueStrategy = AlertQueueItem.UniqueStrategy.Replace
                    )
                }
            }

            var wasKeyDownloaded = false

            suspend fun doAddressRouting() {

                val result =
                    addressRoutingUseCase.invoke(
                        AddressRoutingUseCase.Params(
                            item.id,
                            item.location,
                            lastLocation.value
                        )
                    )

                val route = when (result) {
                    is UseCaseResult.Error -> {
                        val e = result.exception
                        if (shouldDownloadRoutingKey(e) && !wasKeyDownloaded) {
                            downloadsViewModel.observeDownload(enqueueDownloadRoutingKey()).observe {
                                viewModelScope.launch {
                                    var isHandled = false
                                    if (it.isSuccessWithData()) {
                                        val key = withContext(Dispatchers.IO) {
                                            it.data?.downloadInfo?.localUri?.readString(baseApplicationContext.contentResolver)
                                                .orEmpty()
                                        }
                                        if (key.isNotEmpty()) {
                                            wasKeyDownloaded = true
                                            isHandled = true
                                            cacheRepo.setDoubleGisRoutingApiKey(key)
                                            // повтор юзкейса с актуализированным ключом
                                            doAddressRouting()
                                        } else {
                                            showDownloadRoutingKeyError(EmptyResultException())
                                        }
                                    } else if (it.isLoading) {
                                        isHandled = true
                                    } else {
                                        showDownloadRoutingKeyError(it.error)
                                    }
                                    if (!isHandled) {
                                        handleResult(null)
                                    }
                                }
                            }
                            return
                        }

                        showRoutingFailedMessage(result.exception, result.errorMessage())
                        null
                    }

                    is UseCaseResult.Success -> {
                        result.data
                    }

                    else -> {
                        null
                    }
                }

                handleResult(route)
            }

            doAddressRouting()
        }
    }

    fun onItemErrorMessageClose(id: Long, type: Address.ErrorType) {
        viewModelScope.launch {
            repo.updateItem(id) {
                it.copy(
                    locationErrorMessage = if (type == Address.ErrorType.LOCATION) {
                        null
                    } else {
                        it.locationErrorMessage
                    },
                    routingErrorMessage = if (type == Address.ErrorType.ROUTING) {
                        null
                    } else {
                        it.routingErrorMessage
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
    fun onSuggestSelected(id: Long, suggestItem: AddressSuggestItem) {
        removeSnackbarsFromQueue()
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)
        viewModelScope.launch {
            // убрать только из мапы
            onRemoveSuggests(id, true)
            val suggest = suggestItem.toDomain()
            val geocodeResult = addressSuggestGeocodeUseCase.invoke(
                AddressSuggestGeocodeUseCase.Parameters(
                    suggest,
                    lastLocation.value
                )
            )
            // дальше должен быть mergeWithSuggests в Observer
            repo.specifyFromSuggest(id, suggest, geocodeResult)
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
                    item = AddressItem(it.id, address = query),
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

    private fun showReverseGeocodeFailedMessage(e: Throwable, useCaseMessage: TextMessage?) {
        val message = useCaseMessage?.let {
            TextMessage(R.string.address_sorter_error_reverse_geocode_format, it)
        } ?: TextMessage(R.string.address_sorter_error_reverse_geocode)

        showOkDialog(DIALOG_TAG_REVERSE_GEOCODE_FAILED, message)
    }

    private fun showRoutingFailedMessage(e: Throwable, useCaseMessage: TextMessage?) {
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
                        TextMessage(
                            net.maxsmr.feature.address_sorter.data.R.string.address_sorter_error_missing_locations_count_format,
                            count
                        )
                    }
                )
            }

            is RoutingFailedException -> {
                val count = e.routes.size
                if (count == 1) {
                    TextMessage(
                        R.string.address_sorter_error_routing_format,
                        TextMessage(e.routes[0].second.getDisplayedMessageResId())
                    )
                } else {
                    TextMessage(R.string.address_sorter_error_routing_count_format, count)
                }
            }

            else -> {
                useCaseMessage?.let {
                    TextMessage(R.string.address_sorter_error_routing_format, it)
                } ?: TextMessage(R.string.address_sorter_error_routing)
            }
        }
        showOkDialog(DIALOG_TAG_ROUTING_FAILED, message)
    }

    private fun shouldDownloadRoutingKey(e: Throwable): Boolean {
        return e is HttpException && e.code() in arrayOf(
            HttpErrorCode.FORBIDDEN.code,
            HttpErrorCode.UNAUTHORIZED.code
        ) && routingKeyUrl.isUrlValid()
    }

    private fun enqueueDownloadRoutingKey(): DownloadService.Params {
        val params = DownloadParamsModel(routingKeyUrl).toParams()
        DownloadService.Params(
            params.requestParams,
            null,
            params.resourceName,
            DownloadServiceStorage.Type.INTERNAL,
            params.subDirPath,
            params.targetHashInfo,
            params.skipIfDownloaded,
            params.replaceFile,
            params.deleteUnfinished,
            params.retryWithNotifier
        ).let {
            downloadsViewModel.enqueueDownload(it)
            return it
        }
    }

    private fun showDownloadRoutingKeyError(e: Throwable?) {
        showOkDialog(DIALOG_TAG_DOWNLOAD_KEY_FAILED, e?.message?.takeIf { it.isNotEmpty() }?.let {
            TextMessage(R.string.address_sorter_download_routing_key_failed_format, it)
        } ?: TextMessage(R.string.address_sorter_download_routing_key_failed))
    }

    private fun List<AddressItem>.refreshFlows() {
        this.forEach {
            // актуализация flow'ов по текущему списку итемов
            if (!suggestFlowMap.containsKey(it.id)) {
                val flow = MutableStateFlow(
                    if (!it.isSuggested) {
                        AddressSuggestUseCase.Parameters(it.id, it.address, lastLocation.value)
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
        val location: Address.Location? = null,
        val distance: Float? = null,
        val duration: Long? = null,
        val isSuggested: Boolean = false,
        val exceptionsData: ArrayList<AddressErrorMessageData> = arrayListOf(),
    ) : Serializable {

        val isEmpty = address.isEmpty() && location == null

        companion object {

            fun Address.toUi() = AddressItem(
                id, address, location, distance, duration, isSuggested,
                ArrayList(errorMessagesMap.entries.map {
                    AddressErrorMessageData(it.key, it.value)
                })
            )
        }
    }

    data class AddressSuggestItem(
        val displayedAddress: String,
        val geocodeAddress: String,
        val location: Address.Location?,
        val distance: Float?,
    ) : Serializable {

        fun toDomain() = AddressSuggest(
            displayedAddress, geocodeAddress, location, distance
        )

        companion object {

            fun AddressSuggest.toUi() = AddressSuggestItem(
                displayedAddress, geocodeAddress, location, distance
            )
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            locationViewModel: LocationViewModel,
            downloadsViewModel: DownloadsViewModel,
            routingKeyUrl: String,
        ): AddressSorterViewModel
    }

    companion object {

        const val DIALOG_TAG_IMPORT_FAILED = "import_failed"
        const val DIALOG_TAG_EXPORT_FILE_NAME = "export_file_name"
        const val DIALOG_TAG_EXPORT_SUCCESS = "export_success"
        const val DIALOG_TAG_EXPORT_FAILED = "export_failed"
        const val DIALOG_TAG_CHANGE_ROUTING_MODE = "change_routing_mode"
        const val DIALOG_TAG_CHANGE_ROUTING_TYPE = "change_routing_type"
        const val DIALOG_TAG_CHANGE_SORT_PRIORITY = "change_sort_priority"
        const val DIALOG_TAG_CLEAR_ITEMS = "clear_items"
        const val DIALOG_TAG_REVERSE_GEOCODE_FAILED = "reverse_geocode_failed"
        const val DIALOG_TAG_ROUTING_FAILED = "routing_failed"
        const val DIALOG_TAG_DOWNLOAD_KEY_FAILED = "download_key_failed"
    }
}