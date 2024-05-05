package net.maxsmr.feature.download.ui

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.base.delegates.persistableValueInitial
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.adapter.HeaderInfoAdapterData
import java.net.MalformedURLException
import java.net.URL

class DownloadsParamsViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val viewModel: DownloadsViewModel,
) : BaseViewModel(state) {

    val urlField: Field<String> = Field.Builder(EMPTY_STRING)
        .emptyIf { it.toUrlOrNull() == null }
        .setRequired(R.string.download_file_url_empty_error)
        .hint(R.string.download_file_url_hint)
        .persist(state, KEY_FIELD_URL)
        .build()

    val fileNameField: Field<String> = Field.Builder(EMPTY_STRING)
        .emptyIf { it.isEmpty() }
        .hint(R.string.download_file_name_hint)
        .persist(state, KEY_FIELD_FILE_NAME)
        .build()

    val subDirNameField: Field<String> = Field.Builder(EMPTY_STRING)
        .emptyIf { it.isEmpty() }
        .hint(R.string.download_sub_dir_name_hint)
        .persist(state, KEY_FIELD_SUB_DIR_NAME)
        .build()

    val method by persistableLiveDataInitial(Method.GET)

    /**
     * Готовые итемы для отображения в адаптере
     */
    val headerItems by persistableLiveDataInitial<List<HeaderInfoAdapterData>>(emptyList())

    private val headerFields = mutableListOf<HeaderInfoFields>()

    private var headerIdCounter by persistableValueInitial(0)

    val allFields: List<Field<String>> get() {
        val fields = mutableListOf(urlField, fileNameField, subDirNameField)
        headerFields.forEach {
            fields.add(it.header.first.field)
            fields.add(it.header.second.field)
        }
        return fields
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
            .setRequired(R.string.download_key_name_empty_error)
            .hint(R.string.download_key_name_hint)
            .build()
        val keyInfo = keyField.observe(true)
        val valueField = Field.Builder(EMPTY_STRING)
            .emptyIf { it.isEmpty() }
            .setRequired(R.string.download_value_name_empty_error)
            .hint(R.string.download_value_name_hint)
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
            HeaderInfoAdapterData(id,
            Pair(
                HeaderInfoAdapterData.Info(keyInfo.field.value.orEmpty(), keyInfo.field.hint, keyInfo.field.error),
                HeaderInfoAdapterData.Info(valueInfo.field.value.orEmpty(), valueInfo.field.hint, valueInfo.field.error))
            )
        )
        this.headerItems.value = headerItems
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

    fun onDownloadStartClick() {
        val method = method.value ?: return

        val fields = allFields
        var hasError = false
        fields.forEach {
            if (!it.validateAndSetByRequired()) {
                hasError = true
            }
        }
        if (hasError) {
            return
        }
        val url = urlField.value?.toUrlOrNull() ?: return
        val fileName = fileNameField.value
        val subDirName = subDirNameField.value.orEmpty()
        val notificationParams = DownloadService.NotificationParams(
            successActions = DownloadsViewModel.defaultNotificationActions(
                baseApplicationContext
            )
        )
        val headers = hashMapOf<String, String>()
        headerFields.forEach {
            val key = it.header.first.field.value ?: return@forEach
            val value = it.header.second.field.value ?: return@forEach
            headers[key] = value
        }

        val params = if (method == Method.GET) {
            DownloadService.Params.defaultGETServiceParamsFor(
                url,
                fileName,
                headers,
                subDir = subDirName,
                notificationParams = notificationParams,
            )
        } else {
            DownloadService.Params.defaultPOSTServiceParamsFor(
                url,
                fileName,
                headers,
                subDir = subDirName,
                notificationParams = notificationParams,
            )
        }

        viewModel.download(params)
    }

    private fun updateHeaderItem(
        id: Int,
        isKey: Boolean,
        updateInfoFunc: (HeaderInfoAdapterData.Info) -> HeaderInfoAdapterData.Info
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

    enum class Method {
        GET,
        POST
    }

    class HeaderInfoFields(
        val id: Int,
        val header: Pair<Info, Info>
    ) {

        data class Info(
            val field: Field<String>,
            val valueObserver: Observer<String>,
            val hintObserver: Observer<Field.Hint?>,
            val errorObserver: Observer<TextMessage?>,
        )
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
        private const val KEY_FIELD_FILE_NAME = "file_name"
        private const val KEY_FIELD_SUB_DIR_NAME = "sub_dir_name"

        // TODO перенести
        fun String.toUrlOrNull() = try {
            URL(this)
        } catch (e: MalformedURLException) {
            null
        }
    }
}