package net.maxsmr.feature.download.ui

import android.Manifest
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.android.recyclerview.views.decoration.Divider
import net.maxsmr.android.recyclerview.views.decoration.DividerItemDecoration
import net.maxsmr.commonutils.gui.addSoftInputStateListener
import net.maxsmr.commonutils.gui.bindToTextNotNull
import net.maxsmr.commonutils.gui.clearFocus
import net.maxsmr.commonutils.gui.hideKeyboard
import net.maxsmr.commonutils.gui.runOnceLayoutChanges
import net.maxsmr.commonutils.gui.scrollToView
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.observeFromText
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.android.content.pick.ContentPicker
import net.maxsmr.core.android.content.pick.PickRequest
import net.maxsmr.core.android.content.pick.concrete.saf.SafPickerParams
import net.maxsmr.core.domain.entities.feature.network.Method
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.fragments.BaseMenuFragment
import net.maxsmr.core.ui.fields.bindHintError
import net.maxsmr.core.ui.fields.bindValue
import net.maxsmr.core.ui.fields.bindValueWithState
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.adapter.HeaderInfoAdapter
import net.maxsmr.feature.download.ui.adapter.HeaderListener
import net.maxsmr.feature.download.ui.databinding.FragmentDownloadsParamsBinding
import net.maxsmr.feature.preferences.ui.observePostNotificationPermissionAsked
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsParamsFragment : BaseMenuFragment<DownloadsParamsViewModel>(), HeaderListener {

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var factory: DownloadsParamsViewModel.Factory

    override val layoutId: Int = R.layout.fragment_downloads_params

    override val viewModel: DownloadsParamsViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            factory.create(it, downloadsViewModel)
        }
    }

    override val menuResId: Int = R.menu.menu_downloads_params

    private val downloadsViewModel: DownloadsViewModel by activityViewModels()

    private val binding by viewBinding(FragmentDownloadsParamsBinding::bind)

    private val headerInfoAdapter by lazy { HeaderInfoAdapter(this) }

    private val contentPicker: ContentPicker = FragmentContentPickerBuilder()
        .addRequest(
            PickRequest.BuilderDocument(REQUEST_CODE_CHOOSE_BODY)
                .addSafParams(SafPickerParams.any())
                .needPersistableUriAccess(true)
                .onSuccess {
                    viewModel.onBodyUriSelected(it.uri)
                }
                .onError {
                    viewModel.onPickerResultError(it)
                }
                .build()
        )
        .addRequest(
            PickRequest.BuilderDocument(REQUEST_CODE_CHOOSE_PARAMS_JSON)
                .addSafParams(SafPickerParams.json())
                .needPersistableUriAccess(true)
                .onSuccess {
                    downloadsViewModel.downloadFromJson(it.uri, requireContext().contentResolver)
                }
                .onError {
                    downloadsViewModel.onPickerResultError(it)
                }
                .build()
        )
        .build()

    private val fieldViewsMap: Map<Field<*>, View> by lazy {
        mutableMapOf<Field<*>, View>().apply {
            with(viewModel) {
                put(urlField, binding.tilUrl)
                put(methodField, binding.containerMethod)
                put(bodyField, binding.containerSelectRequestBody)
                put(fileNameField, binding.tilFileName)
                put(subDirNameField, binding.tilSubDirName)
                put(targetHashField, binding.tilTargetHash)
            }
        }
    }

    private var wasResumedOnce = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: DownloadsParamsViewModel,
    ) {
        super.onViewCreated(view, savedInstanceState, viewModel)

        binding.etUrl.bindToTextNotNull(viewModel.urlField)
        viewModel.urlField.observeFromText(binding.etUrl, viewLifecycleOwner)
        viewModel.urlField.bindHintError(viewLifecycleOwner, binding.tilUrl)

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.download_method_names,
            android.R.layout.simple_list_item_1
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMethod.adapter = adapter
        binding.spinnerMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.methodField.value = Method.entries[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        viewModel.methodField.valueLive.observe {
            binding.spinnerMethod.setSelection(it.ordinal)
        }

        viewModel.bodyField.valueLive.observe {
            binding.containerSelectRequestBody.isEnabled = it.isEnabled
            binding.ibSelectRequestBody.isEnabled = it.isEnabled
            binding.ibClearRequestBody.isVisible = !it.isEmpty
            binding.tvRequestBodyName.text = it.getName(requireContext()).takeIf { name -> name.isNotEmpty() }
                ?: getString(
                    if (it.isEnabled) {
                        R.string.download_field_request_body_empty_text
                    } else {
                        R.string.download_field_request_body_non_required_text
                    }
                )
        }
        viewModel.bodyField.errorLive.observe {
            binding.tvRequestBodyError.setTextOrGone(it?.get(requireContext()))
        }

        binding.etFileName.bindToTextNotNull(viewModel.fileNameField)
        viewModel.fileNameField.observeFromText(binding.etFileName, viewLifecycleOwner)
        viewModel.fileNameField.bindHintError(viewLifecycleOwner, binding.tilFileName)
        viewModel.fileNameChangeStateField.bindValueWithState(viewLifecycleOwner, binding.cbFileNameFix)

        binding.etSubDirName.bindToTextNotNull(viewModel.subDirNameField)
        viewModel.subDirNameField.observeFromText(binding.etSubDirName, viewLifecycleOwner)
        viewModel.subDirNameField.bindHintError(viewLifecycleOwner, binding.tilSubDirName)

        binding.etTargetHash.bindToTextNotNull(viewModel.targetHashField)
        viewModel.targetHashField.observeFromText(binding.etTargetHash, viewLifecycleOwner)
        viewModel.targetHashField.bindHintError(viewLifecycleOwner, binding.tilTargetHash)

        viewModel.ignoreServerErrorsField.bindValue(viewLifecycleOwner, binding.cbIgnoreServerError)
        viewModel.ignoreAttachmentStateField.bindValueWithState(viewLifecycleOwner, binding.cbIgnoreAttachment)
        viewModel.replaceFileField.bindValue(viewLifecycleOwner, binding.cbReplaceFile)
        viewModel.deleteUnfinishedField.bindValue(viewLifecycleOwner, binding.cbDeleteUnfinished)

        binding.rvHeaders.adapter = headerInfoAdapter
        binding.rvHeaders.addItemDecoration(
            DividerItemDecoration.Builder(requireContext())
                .setDivider(Divider.Space(10), DividerItemDecoration.Mode.ALL_EXCEPT_LAST)
                .build()
        )
        viewModel.headerItems.observe {
//            requireActivity().clearFocus()
            headerInfoAdapter.items = it
        }

        binding.ibSelectRequestBody.setOnClickListener {
            contentPicker.pick(REQUEST_CODE_CHOOSE_BODY, requireContext())
        }
        binding.ibClearRequestBody.setOnClickListener {
            viewModel.onClearRequestBodyUri()
        }
        binding.ibAdd.setOnClickListener {
            viewModel.onAddHeader()
        }

        binding.btStart.setOnClickListener {
            requireActivity().clearFocus()
            doOnPermissionsResult(
                BaseActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION,
                listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE) // post_notifications не является обязательным для работы сервиса
            ) {
                viewModel.onStartDownloadClick { field ->
                    requireActivity().hideKeyboard()
                    fieldViewsMap[field]?.let {
                        binding.svParams.scrollToView(it, true, activity = requireActivity())
                    }
                }
            }
        }

        binding.root.addSoftInputStateListener() {
            if (!it) {
                binding.svParams.requestLayout()
            }
        }

        viewModel.cacheRepo.observePostNotificationPermissionAsked(this)
    }

    override fun onResume() {
        super.onResume()
//        костыль для борьбы с пустым пространством на месте клавиатуры
        if (!wasResumedOnce) {
            if (requireActivity().currentFocus == null) {
//                binding.etUrl.showKeyboard()
                requireActivity().hideKeyboard(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            }
            wasResumedOnce = true
        }
        binding.svParams.runOnceLayoutChanges {
            binding.svParams.requestLayout()
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_pick_json -> {
                doOnPermissionsResult(
                    BaseActivity.REQUEST_CODE_WRITE_EXTERNAL_PERMISSION,
                    listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ) {
                    viewModel.onLoadFromJsonAction(requireContext()) {
                        contentPicker.pick(REQUEST_CODE_CHOOSE_PARAMS_JSON, requireContext())
                    }
                }
                true
            }

            else -> {
                super.onMenuItemSelected(menuItem)
            }
        }
    }

    override fun onHeaderNameChanged(id: Int, value: String) {
        viewModel.onHeaderValueChanged(id, value, true)
    }

    override fun onHeaderValueChanged(id: Int, value: String) {
        viewModel.onHeaderValueChanged(id, value, false)
    }

    override fun onRemoveHeader(id: Int) {
        viewModel.onRemoveHeader(id)
    }

    companion object {

        private const val REQUEST_CODE_CHOOSE_BODY = 1
        private const val REQUEST_CODE_CHOOSE_PARAMS_JSON = 2
    }
}