package net.maxsmr.feature.download.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.result.getOrNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.REG_EX_FILE_NAME
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.clearErrorOnChange
import net.maxsmr.commonutils.live.field.validateAndSetByRequired
import net.maxsmr.commonutils.media.name
import net.maxsmr.commonutils.media.writeFromStream
import net.maxsmr.commonutils.openRawResource
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.actions.SnackbarAction
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.base.delegates.persistableValueInitial
import net.maxsmr.core.android.baseAppName
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.network.toUrlOrNull
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.domain.entities.feature.download.REG_EX_MD5_ALGORITHM
import net.maxsmr.core.domain.entities.feature.network.Method
import net.maxsmr.core.ui.BooleanFieldState
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.adapter.HeaderInfoAdapterData
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.ui.BasePostNotificationViewModel
import java.io.Serializable

class DownloadsParamsViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val viewModel: DownloadsViewModel,
    @Dispatcher(AppDispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    repo: CacheDataStoreRepository,
) : BasePostNotificationViewModel(repo, state) {

    val urlField: Field<String> = object:  Field.Builder<String>(EMPTY_STRING) {
        override fun valueGetter(fieldValue: MutableLiveData<String>): () -> String = {
            fieldValue.value.orEmpty().trim()
        }
    }.emptyIf { it.toUrlOrNull() == null }
        .setRequired(R.string.download_url_empty_error)
        .hint(R.string.download_url_hint)
        .persist(state, KEY_FIELD_URL)
        .build()

    val methodField: Field<Method> = Field.Builder(Method.GET)
        .emptyIf { false }
        .persist(state, KEY_FIELD_METHOD)
        .build()

    val bodyField: Field<UriBodyContainer> = Field.Builder(UriBodyContainer())
        .emptyIf { it.isEmpty }
        .persist(state, KEY_FIELD_BODY)
        .build()

    val fileNameField: Field<String> = Field.Builder(EMPTY_STRING)
        .emptyIf { it.isEmpty() }
        .validators(Field.Validator(R.string.download_field_file_name_error) { Regex(REG_EX_FILE_NAME).matches(it) })
        .hint(R.string.download_field_file_name_hint)
        .persist(state, KEY_FIELD_FILE_NAME)
        .build()

    val fileNameChangeStateField: Field<BooleanFieldState> = Field.Builder(BooleanFieldState(false))
        .emptyIf { false }
        .persist(state, KEY_FIELD_FILE_NAME_CHANGE_STATE)
        .build()

    val subDirNameField: Field<String> = Field.Builder(EMPTY_STRING)
        .emptyIf { it.isEmpty() }
        .hint(R.string.download_field_sub_dir_name_hint)
        .persist(state, KEY_FIELD_SUB_DIR_NAME)
        .build()

    val targetHashField: Field<String> = Field.Builder(EMPTY_STRING)
        .emptyIf { it.isEmpty() }
        .validators(Field.Validator(R.string.download_field_target_hash_error) {
            Regex(REG_EX_MD5_ALGORITHM).matches(
                it
            )
        })
        .hint(R.string.download_field_target_hash_hint)
        .persist(state, KEY_FIELD_TARGET_HASH)
        .build()

    val ignoreServerErrorsField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_IGNORE_SERVER_ERRORS)
        .build()

    val ignoreAttachmentStateField: Field<BooleanFieldState> = Field.Builder(BooleanFieldState(false))
        .emptyIf { false }
        .persist(state, KEY_FIELD_IGNORE_ATTACHMENT_STATE)
        .build()

    val replaceFileField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_REPLACE_FILE)
        .build()

    val deleteUnfinishedField: Field<Boolean> = Field.Builder(true)
        .emptyIf { false }
        .persist(state, KEY_FIELD_DELETE_UNFINISHED)
        .build()

    /**
     * Готовые итемы для отображения в адаптере
     */
    val headerItems by persistableLiveDataInitial<List<HeaderInfoAdapterData>>(arrayListOf())

    private val headerFields = mutableListOf<HeaderInfoFields>()

    private var headerIdCounter by persistableValueInitial(0)

    private val storage: ContentStorage<Uri> by lazy {
        ContentStorage.createUriStorage(
            ContentStorage.StorageType.SHARED,
            ContentType.DOCUMENT,
            baseApplicationContext
        )
    }

    private val allFields: List<Field<*>>
        get() {
            val fields = mutableListOf(
                urlField,
                methodField,
                bodyField,
                fileNameField,
                fileNameChangeStateField,
                subDirNameField,
                targetHashField,
                ignoreServerErrorsField,
                ignoreAttachmentStateField,
                replaceFileField,
                deleteUnfinishedField
            )
            headerFields.forEach {
                fields.add(it.header.first.field)
                fields.add(it.header.second.field)
            }
            return fields
        }

    override fun onInitialized() {
        super.onInitialized()

        fun Field<BooleanFieldState>.toggleState(value: Boolean) {
            this.value = if (value) {
                BooleanFieldState(value = true, isEnabled = false)
            } else {
                this.value?.copy(isEnabled = true) ?: BooleanFieldState(value = false, isEnabled = true)
            }
        }

        urlField.clearErrorOnChange(this)
        methodField.valueLive.observe {
            var body = bodyField.value ?: UriBodyContainer()
            body = if (it == Method.POST) {
                bodyField.setRequired(R.string.download_field_request_body_empty_error)
                body.copy(isEnabled = true)
            } else {
                bodyField.setNonRequired()
                body.copy(isEnabled = false)
            }
            bodyField.value = body
        }
        bodyField.clearErrorOnChange(this)
        fileNameField.clearErrorOnChange(this)
        fileNameField.isEmptyLive.observe {
            fileNameChangeStateField.toggleState(it)
        }
        targetHashField.clearErrorOnChange(this)

        ignoreServerErrorsField.valueLive.observe {
            ignoreAttachmentStateField.toggleState(it)
        }
        headerItems.observe {
            if (it.isEmpty()) {
                headerIdCounter = 0
            }
        }
    }

    fun onBodyUriSelected(uri: Uri) {
        val body = bodyField.value
        if (body == null || !body.isEnabled) return
        bodyField.value = body.copy(bodyUri = uri.toString())
    }

    fun onClearRequestBodyUri() {
        bodyField.value = bodyField.value?.copy(bodyUri = null)
    }

    fun onAddHeader() {
        val id = headerIdCounter++

        fun Field<String>.observe(isKey: Boolean): HeaderInfoFields.Info {
            val valueObserver = Observer<String> { value ->
                updateHeaderItem(id, isKey) {
                    HeaderInfoAdapterData.Info(value, it.hint, it.error)
                }
                clearError()
            }
            valueLive.observe(this@DownloadsParamsViewModel, valueObserver)
            val hintObserver = Observer<Field.Hint?> { hint ->
                updateHeaderItem(id, isKey) {
                    HeaderInfoAdapterData.Info(it.value, hint, it.error)
                }
            }
            hintLive.observe(this@DownloadsParamsViewModel, hintObserver)
            val errorObserver = Observer<TextMessage?> { error ->
                updateHeaderItem(id, isKey) {
                    HeaderInfoAdapterData.Info(it.value, it.hint, error)
                }
            }
            errorLive.observe(this@DownloadsParamsViewModel, errorObserver)
            return HeaderInfoFields.Info(this, valueObserver, hintObserver, errorObserver)
        }

        val keyField = Field.Builder(EMPTY_STRING)
            .emptyIf { it.isEmpty() }
            .setRequired(R.string.download_field_header_key_empty_error)
            .hint(R.string.download_field_header_key_hint)
            .build()
        val keyInfo = keyField.observe(true)
        val valueField = Field.Builder(EMPTY_STRING)
            .emptyIf { it.isEmpty() }
            .setRequired(R.string.download_field_header_value_empty_error)
            .hint(R.string.download_field_header_value_hint)
            .build()
        val valueInfo = valueField.observe(false)

        headerFields.add(
            HeaderInfoFields(
                id,
                Pair(keyInfo, valueInfo)
            )
        )

        val headerItems = this.headerItems.value?.toMutableList() ?: mutableListOf()
        headerItems.add(
            HeaderInfoAdapterData(
                id,
                Pair(
                    HeaderInfoAdapterData.Info(
                        keyInfo.field.value.orEmpty(),
                        keyInfo.field.hint,
                        keyInfo.field.error
                    ),
                    HeaderInfoAdapterData.Info(
                        valueInfo.field.value.orEmpty(),
                        valueInfo.field.hint,
                        valueInfo.field.error
                    )
                )
            )
        )
        this.headerItems.value = ArrayList(headerItems)
    }

    fun onRemoveHeader(id: Int) {

        fun HeaderInfoFields.Info.removeObservers() {
            field.valueLive.removeObserver(valueObserver)
            field.hintLive.removeObserver(hintObserver)
            field.errorLive.removeObserver(errorObserver)
        }

        val iterator = headerFields.iterator()
        var targetItem: HeaderInfoFields? = null
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.id == id) {
                targetItem = item
                iterator.remove()
                break
            }
        }
        targetItem?.let {
            it.header.first.removeObservers()
            it.header.second.removeObservers()
        }

        val headerItems = this.headerItems.value?.toMutableList()
        if (headerItems?.removeIf { it.id == id } == true) {
            this.headerItems.value = headerItems
        }
    }

    fun onHeaderValueChanged(id: Int, value: String, isKey: Boolean) {
        headerFields.find { it.id == id }?.let {
            val info = if (isKey) {
                it.header.first
            } else {
                it.header.second
            }
            // изменение в филде подхватится обсервером и итемы рефрешнутся
            info.field.value = value
        }
    }

    fun onLoadFromJsonAction(context: Context, onPickAction: () -> Unit) {
        viewModelScope.launch {
            if (!cacheRepo.hasDownloadParamsModelSample()) {
                withContext(ioDispatcher) {
                    val subDir = baseAppName
                    val resource = storage.getOrCreate(RESOURCE_NAME_DOWNLOAD_PARAMS_MODEL, subDir)
                    resource.getOrNull()?.let {
                        // ассеты не допускаются в либах
                        openRawResource(context, R.raw.download_params_model_sample)?.let { assetStream ->
                            if (it.writeFromStream(context.contentResolver, assetStream)) {
                                showSnackbar(
                                    SnackbarAction(
                                        TextMessage(
                                            R.string.download_alert_params_sample_copied_format,
                                            storage.path
                                        ),
                                        SnackbarAction.SnackbarLength.LONG
                                    )
                                )
                            }
                        }
                    }
                }
                cacheRepo.setHasDownloadParamsModelSample()
            } else {
                onPickAction()
            }
        }
    }

    fun onStartDownloadClick() {
        if (!allFields.validateAndSetByRequired()) {
            return
        }
        val url = urlField.value ?: return
        val method = methodField.value ?: return
        val bodyUri = bodyField.value?.bodyUri

        val fileName = fileNameField.value
        val subDirName = subDirNameField.value.orEmpty()
        val ignoreFileName = fileNameChangeStateField.value?.value != true

        val headers = hashMapOf<String, String>()
        headerFields.forEach {
            val key = it.header.first.field.value ?: return@forEach
            val value = it.header.second.field.value ?: return@forEach
            headers[key] = value
        }

        val targetHash = targetHashField.value

        val ignoreServerErrors = ignoreServerErrorsField.value ?: false
        val ignoreAttachment = ignoreAttachmentStateField.value?.value ?: false
        val replaceFile = replaceFileField.value ?: false
        val deleteUnfinished = deleteUnfinishedField.value ?: false

        viewModel.download(
            DownloadParamsModel(
                url,
                method,
                bodyUri,
                fileName,
                ignoreFileName,
                subDirName,
                targetHash,
                ignoreServerErrors,
                ignoreAttachment,
                replaceFile,
                deleteUnfinished,
                headers
            )
        )
    }

    private fun updateHeaderItem(
        id: Int,
        isKey: Boolean,
        updateInfoFunc: (HeaderInfoAdapterData.Info) -> HeaderInfoAdapterData.Info,
    ) {
        val items = headerItems.value ?: return
        var hasChanged = false
        val newItems = mutableListOf<HeaderInfoAdapterData>().apply {
            items.forEach {
                val newHeader = if (it.id == id) {
                    // обновляем существующий итем новыми полями
                    if (isKey) {
                        val info = updateInfoFunc(it.header.first)
                        hasChanged = info != it.header.first
                        Pair(info, it.header.second)
                    } else {
                        val info = updateInfoFunc(it.header.second)
                        hasChanged = info != it.header.second
                        Pair(it.header.first, info)
                    }
                } else {
                    it.header
                }
                add(HeaderInfoAdapterData(it.id, newHeader))
            }
        }
        if (hasChanged) {
            headerItems.value = newItems
        }
    }

    class HeaderInfoFields(
        val id: Int,
        val header: Pair<Info, Info>,
    ) {

        data class Info(
            val field: Field<String>,
            val valueObserver: Observer<String>,
            val hintObserver: Observer<Field.Hint?>,
            val errorObserver: Observer<TextMessage?>,
        )
    }

    data class UriBodyContainer(
        val bodyUri: String? = null,
        val isEnabled: Boolean = false,
    ) : Serializable {

        val isEmpty = bodyUri.isNullOrEmpty()

        fun getName(context: Context): String {
            return bodyUri?.let { Uri.parse(it).name(context.contentResolver) }.orEmpty()
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            downloadsViewModel: DownloadsViewModel,
        ): DownloadsParamsViewModel
    }

    companion object {

        private const val KEY_FIELD_URL = "url"
        private const val KEY_FIELD_METHOD = "method"
        private const val KEY_FIELD_BODY = "body"
        private const val KEY_FIELD_FILE_NAME = "file_name"
        private const val KEY_FIELD_FILE_NAME_CHANGE_STATE = "file_name_change_state"
        private const val KEY_FIELD_SUB_DIR_NAME = "sub_dir_name"
        private const val KEY_FIELD_TARGET_HASH = "target_hash"
        private const val KEY_FIELD_IGNORE_SERVER_ERRORS = "ignore_server_errors"
        private const val KEY_FIELD_IGNORE_ATTACHMENT_STATE = "ignore_attachment_state"
        private const val KEY_FIELD_REPLACE_FILE = "replace_file"
        private const val KEY_FIELD_DELETE_UNFINISHED = "delete_unfinished"

        private const val RESOURCE_NAME_DOWNLOAD_PARAMS_MODEL = "download_params_model_sample.json"
    }
}